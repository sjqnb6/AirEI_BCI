package SerialParser_;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Receives two independent 8-channel UDP streams on one local port and exposes
 * them as a single 16-channel stream. Source IP determines channel ownership:
 * board 1 -> CH1..CH8, board 2 -> CH9..CH16.
 */
public final class DualCytonWifi16Parser implements AutoCloseable {
    public static final int NUM_CHANNELS = 16;
    public static final int NUM_TOTAL = 30;
    public static final int SAMPLE_INDEX_CHANNEL = 0;
    public static final int FIRST_EXG_CHANNEL = 1;
    public static final int TIMESTAMP_CHANNEL = 22;

    private static final int FRAME_SIZE = 33;
    private static final int BOARD_COMMAND_PORT = 5005;
    private static final byte HEADER = (byte) 0xA0;
    private static final int TAIL_PREFIX_MASK = 0xF0;
    private static final int TAIL_PREFIX = 0xC0;
    private static final long PAIR_TIMEOUT_NANOS = 500_000_000L;
    private static final double SCALE_FACTOR_UV = 4.5 / 8388607.0 / 24.0 * 1_000_000.0;

    private final int capacity;
    private final CombinedSample[] ring;
    private final ReentrantLock bufferLock = new ReentrantLock();
    private final Object pairLock = new Object();
    private final Sample8[] pendingBoard1 = new Sample8[256];
    private final Sample8[] pendingBoard2 = new Sample8[256];
    private final DeviceState board1State = new DeviceState();
    private final DeviceState board2State = new DeviceState();

    private int writePos;
    private int readPos;
    private int count;

    private DatagramSocket socket;
    private InetAddress board1Address;
    private InetAddress board2Address;
    private Thread readerThread;
    private volatile boolean running;
    private volatile long receivedBoard1Packets;
    private volatile long receivedBoard2Packets;
    private volatile long unknownSourcePackets;
    private volatile long pairedSamples;
    private volatile long expiredBoard1Samples;
    private volatile long expiredBoard2Samples;
    private volatile long lastBoard1PacketAtMs = -1L;
    private volatile long lastBoard2PacketAtMs = -1L;

    public DualCytonWifi16Parser(int bufferCapacitySamples) {
        if (bufferCapacitySamples <= 0) {
            throw new IllegalArgumentException("bufferCapacitySamples must be > 0");
        }
        capacity = bufferCapacitySamples;
        ring = new CombinedSample[capacity];
    }

    public synchronized void start_stream(
            String board1Host,
            String board2Host,
            int port
    ) throws IOException {
        if (running) {
            return;
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid UDP port: " + port);
        }

        board1Address = InetAddress.getByName(board1Host);
        board2Address = InetAddress.getByName(board2Host);
        if (board1Address.equals(board2Address)) {
            board1Address = null;
            board2Address = null;
            throw new IllegalArgumentException("The two WiFi boards must use different IP addresses");
        }
        resetState();

        try {
            socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(port));
            socket.setReceiveBufferSize(1024 * 1024);
            socket.setBroadcast(true);
        } catch (IOException e) {
            if (socket != null) {
                socket.close();
            }
            socket = null;
            board1Address = null;
            board2Address = null;
            throw e;
        }

        running = true;
        readerThread = new Thread(this::readLoop, "dual-wifi-16-reader");
        readerThread.setDaemon(true);
        readerThread.start();

        try {
            sendCommand("b");
        } catch (IOException e) {
            stop_stream();
            throw e;
        }
    }

    public synchronized void stop_stream() {
        if (socket == null && !running) {
            return;
        }
        try {
            sendCommand("c");
        } catch (Exception ignored) {
            // Continue closing even if one board has already disappeared.
        }
        running = false;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (Exception ignored) {
        }

        Thread thread = readerThread;
        if (thread != null && thread != Thread.currentThread()) {
            try {
                thread.join(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        readerThread = null;
        socket = null;
        board1Address = null;
        board2Address = null;
        board1State.reset();
        board2State.reset();
        clearPendingSamples();
    }

    public boolean isRunning() {
        return running && socket != null && !socket.isClosed();
    }

    public synchronized void sendCommand(String command) throws IOException {
        if (command == null || command.isEmpty()) {
            return;
        }
        if (socket == null || socket.isClosed() || board1Address == null || board2Address == null) {
            throw new IOException("Dual WiFi socket is not open");
        }
        byte[] payload = command.getBytes(StandardCharsets.US_ASCII);
        socket.send(new DatagramPacket(payload, payload.length, board1Address, BOARD_COMMAND_PORT));
        socket.send(new DatagramPacket(payload, payload.length, board2Address, BOARD_COMMAND_PORT));
    }

    /** Drains paired samples into the project's [row][sample] matrix. */
    public double[][] get_data() {
        bufferLock.lock();
        try {
            int sampleCount = count;
            double[][] output = new double[NUM_TOTAL][sampleCount];
            for (int i = 0; i < sampleCount; i++) {
                int index = (readPos + i) % capacity;
                CombinedSample sample = ring[index];
                output[SAMPLE_INDEX_CHANNEL][i] = sample.packetId;
                for (int channel = 0; channel < NUM_CHANNELS; channel++) {
                    output[FIRST_EXG_CHANNEL + channel][i] = sample.channelsUv[channel];
                }
                output[TIMESTAMP_CHANNEL][i] = sample.receivedAtEpochSeconds;
                ring[index] = null;
            }
            count = 0;
            readPos = writePos;
            return output;
        } finally {
            bufferLock.unlock();
        }
    }

    public long getReceivedBoard1Packets() {
        return receivedBoard1Packets;
    }

    public long getReceivedBoard2Packets() {
        return receivedBoard2Packets;
    }

    public long getUnknownSourcePackets() {
        return unknownSourcePackets;
    }

    public long getPairedSamples() {
        return pairedSamples;
    }

    public long getExpiredBoard1Samples() {
        return expiredBoard1Samples;
    }

    public long getExpiredBoard2Samples() {
        return expiredBoard2Samples;
    }

    public long getLastBoard1PacketAtMs() {
        return lastBoard1PacketAtMs;
    }

    public long getLastBoard2PacketAtMs() {
        return lastBoard2PacketAtMs;
    }

    @Override
    public void close() {
        stop_stream();
    }

    private void readLoop() {
        byte[] buffer = new byte[4096];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try {
            while (running) {
                packet.setLength(buffer.length);
                socket.receive(packet);
                int length = packet.getLength();
                if (length <= 0) {
                    continue;
                }

                handleDatagram(packet.getAddress(), packet.getData(), packet.getOffset(), length);
            }
        } catch (Exception e) {
            if (running) {
                System.err.println("Dual 16-channel WiFi receive failed: " + e.getMessage());
            }
        } finally {
            running = false;
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void handleDatagram(InetAddress source, byte[] data, int offset, int length) {
        if (source.equals(board1Address)) {
            receivedBoard1Packets++;
            lastBoard1PacketAtMs = System.currentTimeMillis();
            board1State.accept(data, offset, length, true);
        } else if (source.equals(board2Address)) {
            receivedBoard2Packets++;
            lastBoard2PacketAtMs = System.currentTimeMillis();
            board2State.accept(data, offset, length, false);
        } else {
            unknownSourcePackets++;
        }
    }

    private void onDecodedSample(boolean fromBoard1, Sample8 sample) {
        synchronized (pairLock) {
            Sample8[] own = fromBoard1 ? pendingBoard1 : pendingBoard2;
            Sample8[] other = fromBoard1 ? pendingBoard2 : pendingBoard1;
            int packetId = sample.packetId;
            own[packetId] = sample;

            Sample8 counterpart = other[packetId];
            if (counterpart != null) {
                long arrivalDifference = Math.abs(sample.receivedAtNanos - counterpart.receivedAtNanos);
                if (arrivalDifference <= PAIR_TIMEOUT_NANOS) {
                    pendingBoard1[packetId] = null;
                    pendingBoard2[packetId] = null;
                    Sample8 first = fromBoard1 ? sample : counterpart;
                    Sample8 second = fromBoard1 ? counterpart : sample;
                    pushCombinedSample(new CombinedSample(first, second));
                    pairedSamples++;
                } else if (sample.receivedAtNanos > counterpart.receivedAtNanos) {
                    other[packetId] = null;
                    if (fromBoard1) {
                        expiredBoard2Samples++;
                    } else {
                        expiredBoard1Samples++;
                    }
                }
            }
            expireOldPendingSamples(sample.receivedAtNanos);
        }
    }

    private void expireOldPendingSamples(long nowNanos) {
        for (int i = 0; i < 256; i++) {
            Sample8 first = pendingBoard1[i];
            if (first != null && nowNanos - first.receivedAtNanos > PAIR_TIMEOUT_NANOS) {
                pendingBoard1[i] = null;
                expiredBoard1Samples++;
            }
            Sample8 second = pendingBoard2[i];
            if (second != null && nowNanos - second.receivedAtNanos > PAIR_TIMEOUT_NANOS) {
                pendingBoard2[i] = null;
                expiredBoard2Samples++;
            }
        }
    }

    private void pushCombinedSample(CombinedSample sample) {
        bufferLock.lock();
        try {
            ring[writePos] = sample;
            if (count == capacity) {
                readPos = (readPos + 1) % capacity;
            } else {
                count++;
            }
            writePos = (writePos + 1) % capacity;
        } finally {
            bufferLock.unlock();
        }
    }

    private void resetState() {
        board1State.reset();
        board2State.reset();
        clearPendingSamples();
        bufferLock.lock();
        try {
            for (int i = 0; i < ring.length; i++) {
                ring[i] = null;
            }
            writePos = 0;
            readPos = 0;
            count = 0;
        } finally {
            bufferLock.unlock();
        }
        receivedBoard1Packets = 0;
        receivedBoard2Packets = 0;
        unknownSourcePackets = 0;
        pairedSamples = 0;
        expiredBoard1Samples = 0;
        expiredBoard2Samples = 0;
        lastBoard1PacketAtMs = -1L;
        lastBoard2PacketAtMs = -1L;
    }

    private void clearPendingSamples() {
        synchronized (pairLock) {
            for (int i = 0; i < 256; i++) {
                pendingBoard1[i] = null;
                pendingBoard2[i] = null;
            }
        }
    }

    private final class DeviceState {
        private final byte[] frame = new byte[FRAME_SIZE];
        private int framePos;
        private long parsedFrames;
        private long badFrames;

        private void reset() {
            framePos = 0;
            parsedFrames = 0;
            badFrames = 0;
        }

        private void accept(byte[] bytes, int offset, int length, boolean fromBoard1) {
            int index = offset;
            int end = offset + length;
            while (index < end) {
                if (framePos == 0) {
                    while (index < end && bytes[index] != HEADER) {
                        index++;
                    }
                    if (index >= end) {
                        return;
                    }
                    frame[0] = bytes[index++];
                    framePos = 1;
                }

                int toCopy = Math.min(FRAME_SIZE - framePos, end - index);
                System.arraycopy(bytes, index, frame, framePos, toCopy);
                framePos += toCopy;
                index += toCopy;

                if (framePos == FRAME_SIZE) {
                    int tail = frame[FRAME_SIZE - 1] & 0xFF;
                    if ((tail & TAIL_PREFIX_MASK) == TAIL_PREFIX) {
                        parseFrame(fromBoard1);
                        framePos = 0;
                    } else {
                        badFrames++;
                        resynchronize();
                    }
                }
            }
        }

        /**
         * Provisional hardware boundary: currently compatible with the existing
         * 33-byte OpenBCI-style 8-channel frame. Replace only this method and the
         * framing constants when the domestic-board WiFi protocol is finalized.
         */
        private void parseFrame(boolean fromBoard1) {
            int packetId = frame[1] & 0xFF;
            double[] channelsUv = new double[8];
            int dataOffset = 2;
            for (int channel = 0; channel < 8; channel++) {
                int raw = ((frame[dataOffset] & 0xFF) << 16)
                        | ((frame[dataOffset + 1] & 0xFF) << 8)
                        | (frame[dataOffset + 2] & 0xFF);
                if ((raw & 0x800000) != 0) {
                    raw |= 0xFF000000;
                }
                channelsUv[channel] = raw * SCALE_FACTOR_UV;
                dataOffset += 3;
            }
            onDecodedSample(
                    fromBoard1,
                    new Sample8(packetId, System.nanoTime(), System.currentTimeMillis() / 1000.0, channelsUv)
            );
            parsedFrames++;
        }

        private void resynchronize() {
            int newStart = -1;
            for (int i = 1; i < FRAME_SIZE; i++) {
                if (frame[i] == HEADER) {
                    newStart = i;
                    break;
                }
            }
            if (newStart < 0) {
                framePos = 0;
                return;
            }
            int remaining = FRAME_SIZE - newStart;
            System.arraycopy(frame, newStart, frame, 0, remaining);
            framePos = remaining;
        }
    }

    private static final class Sample8 {
        private final int packetId;
        private final long receivedAtNanos;
        private final double receivedAtEpochSeconds;
        private final double[] channelsUv;

        private Sample8(
                int packetId,
                long receivedAtNanos,
                double receivedAtEpochSeconds,
                double[] channelsUv
        ) {
            this.packetId = packetId;
            this.receivedAtNanos = receivedAtNanos;
            this.receivedAtEpochSeconds = receivedAtEpochSeconds;
            this.channelsUv = channelsUv;
        }
    }

    private static final class CombinedSample {
        private final int packetId;
        private final double receivedAtEpochSeconds;
        private final double[] channelsUv = new double[NUM_CHANNELS];

        private CombinedSample(Sample8 board1, Sample8 board2) {
            packetId = board1.packetId;
            receivedAtEpochSeconds = Math.max(
                    board1.receivedAtEpochSeconds,
                    board2.receivedAtEpochSeconds
            );
            System.arraycopy(board1.channelsUv, 0, channelsUv, 0, 8);
            System.arraycopy(board2.channelsUv, 0, channelsUv, 8, 8);
        }
    }
}
