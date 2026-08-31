package SerialParser_;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.locks.ReentrantLock;

public class CytonWifiParser implements AutoCloseable {
    public static final int NUM_CHANNELS = 8;
    public static final int NUM_TOTAL = 30;
    private static final int FRAME_SIZE = 33;
    private static final byte HEADER = (byte) 0xA0;
    private static final int TAIL_PREFIX_MASK = 0xF0;
    private static final int TAIL_PREFIX = 0xC0;
    private static final int AUX_OFFSET = 26;
    private static final int AUX_SIZE = 6;
    private static final int AUX_TYPE_C1 = 0x01;
    private static final int AUX_TYPE_C2 = 0x02;
    private static final int AUX_TYPE_C3 = 0x03;
    private static final int INVALID_U8 = 0xFF;
    private static final int NUM_STORED = NUM_CHANNELS + 1;

    private final int capacity;
    private final int[][] ring;
    private int writePos = 0;
    private int readPos = 0;
    private int count = 0;
    private final ReentrantLock bufferLock = new ReentrantLock();

    private DatagramSocket socket;
    private InetAddress targetAddress;
    private int targetPort;
    private Thread readerThread;
    private volatile boolean running = false;

    private final byte[] frame = new byte[FRAME_SIZE];
    private int framePos = 0;
    private final int[] tmpSample = new int[NUM_STORED];
    private volatile long parsedFrames = 0;
    private volatile long recvPackets = 0;
    private volatile CytonWifiHealthFrame latestHealthFrame = null;
    private volatile long lastHealthAuxFrameTimestampMs = -1L;
    private volatile String latestHealthAuxDebugText = "";
    private volatile String latestHealthC1AuxHex = "--";
    private volatile String latestHealthC2AuxHex = "--";
    private volatile String latestHealthC3AuxHex = "--";
    private volatile int latestHealthAuxSeq = -1;
    private volatile int latestHealthAuxType = -1;
    private final int[] healthC1 = new int[AUX_SIZE];
    private final int[] healthC2 = new int[AUX_SIZE];
    private final int[] healthC3 = new int[AUX_SIZE];
    private int currentHealthSeq = -1;
    private int lastPublishedHealthSeq = -1;
    private boolean hasHealthC1 = false;
    private boolean hasHealthC2 = false;
    private boolean hasHealthC3 = false;
    private long lastStatsMs = 0;
    private long lastStatsPackets = 0;
    private long lastStatsFrames = 0;

    public CytonWifiParser(int bufferCapacitySamples) {
        if (bufferCapacitySamples <= 0) {
            throw new IllegalArgumentException("bufferCapacitySamples must be > 0");
        }
        this.capacity = bufferCapacitySamples;
        this.ring = new int[capacity][NUM_STORED];
    }

    public synchronized void start_stream(String host, int port) throws IOException {
        if (running) return;

        targetAddress = InetAddress.getByName(host);
        targetPort = port;
        resetHealthState();

        // Bind local UDP port so board broadcast can be received.
        // Use a larger receive buffer to reduce packet drop under burst traffic.
        socket = new DatagramSocket(null);
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(port));
        socket.setReceiveBufferSize(1024 * 1024);
        socket.setBroadcast(true);

        sendCommand("b");
        CytonWifiHealthRawLogger.beginSession(host, port);
        System.out.println("Cyton WiFi health raw log: " + CytonWifiHealthRawLogger.getLogPath());

        running = true;
        readerThread = new Thread(this::readLoop, "cyton-wifi-reader");
        readerThread.setDaemon(true);
        lastStatsMs = System.currentTimeMillis();
        lastStatsPackets = 0;
        lastStatsFrames = 0;
        readerThread.start();
    }

    public synchronized void stop_stream() {
        running = false;
        sendCommand("c");
        try { if (socket != null) socket.close(); } catch (Exception ignore) {}

        if (readerThread != null) {
            try { readerThread.join(500); } catch (InterruptedException ignore) {}
        }

        readerThread = null;
        socket = null;
        targetAddress = null;
        targetPort = 0;
        framePos = 0;
        resetHealthState();
        CytonWifiHealthRawLogger.endSession();
    }

    @Override
    public void close() {
        stop_stream();
    }

    public boolean isRunning() {
        return running;
    }

    public double[][] get_data() {
        bufferLock.lock();
        try {
            int n = count;
            double[][] outData = new double[NUM_TOTAL][n];
            final double scaleFactorUv = 4.5 / 8388607.0 / 24.0 * 1000000.0;

            for (int i = 0; i < n; i++) {
                int idx = (readPos + i) % capacity;
                int[] sample = ring[idx];
                outData[0][i] = sample[0];
                for (int ch = 0; ch < NUM_CHANNELS; ch++) {
                    outData[ch + 1][i] = sample[ch + 1] * scaleFactorUv;
                }
                outData[22][i] = (double) System.currentTimeMillis() / 1000.0;
            }

            count = 0;
            readPos = writePos;
            return outData;
        } finally {
            bufferLock.unlock();
        }
    }

    private void readLoop() {
        byte[] buf = new byte[2048];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        try {
            while (running) {
                // DatagramPacket length shrinks to last packet size after receive(),
                // so reset it each loop to avoid truncating subsequent EEG packets.
                packet.setLength(buf.length);
                socket.receive(packet);
                int n = packet.getLength();
                if (n <= 0) continue;
                recvPackets++;
                processIncoming(packet.getData(), n);
                maybePrintStats();
            }
        } catch (Exception e) {
            if (running) {
                e.printStackTrace();
            }
        } finally {
            running = false;
            try { if (socket != null) socket.close(); } catch (Exception ignore) {}
        }
    }

    private void sendCommand(String command) {
        if (socket == null || targetAddress == null) return;
        try {
            byte[] payload = command.getBytes("UTF-8");
            DatagramPacket packet = new DatagramPacket(payload, payload.length, targetAddress, targetPort);
            socket.send(packet);
        } catch (Exception ignore) {}
    }

    private void processIncoming(byte[] buf, int len) {
        int i = 0;
        while (i < len) {
            if (framePos == 0) {
                while (i < len && buf[i] != HEADER) i++;
                if (i >= len) return;
                frame[0] = buf[i++];
                framePos = 1;
            }

            int toCopy = Math.min(FRAME_SIZE - framePos, len - i);
            System.arraycopy(buf, i, frame, framePos, toCopy);
            framePos += toCopy;
            i += toCopy;

            if (framePos == FRAME_SIZE) {
                // Accept OpenBCI-style end byte family 0xC0~0xCF.
                if ((((int) frame[FRAME_SIZE - 1]) & TAIL_PREFIX_MASK) == TAIL_PREFIX) {
                    parseFrame(frame);
                    framePos = 0;
                } else {
                    int newStart = -1;
                    for (int j = 1; j < FRAME_SIZE; j++) {
                        if (frame[j] == HEADER) {
                            newStart = j;
                            break;
                        }
                    }
                    if (newStart >= 0) {
                        int remaining = FRAME_SIZE - newStart;
                        System.arraycopy(frame, newStart, frame, 0, remaining);
                        framePos = remaining;
                    } else {
                        framePos = 0;
                    }
                }
            }
        }
    }

    private void parseFrame(byte[] f) {
        int packetId = f[1] & 0xFF;
        tmpSample[0] = packetId;

        int off = 2;
        for (int ch = 0; ch < NUM_CHANNELS; ch++) {
            int raw = ((f[off] & 0xFF) << 16)
                    | ((f[off + 1] & 0xFF) << 8)
                    | (f[off + 2] & 0xFF);
            if ((raw & 0x800000) != 0) {
                raw |= 0xFF000000;
            }
            tmpSample[ch + 1] = raw;
            off += 3;
        }

        pushSample(tmpSample);
        parseHealthAux(f);
        parsedFrames++;
    }

    public long getParsedFrames() {
        return parsedFrames;
    }

    public CytonWifiHealthFrame getLatestHealthFrame() {
        return latestHealthFrame;
    }

    public long getLastHealthAuxFrameTimestampMs() {
        return lastHealthAuxFrameTimestampMs;
    }

    public String getLatestHealthAuxDebugText() {
        return latestHealthAuxDebugText;
    }

    private void maybePrintStats() {
        long now = System.currentTimeMillis();
        if (now - lastStatsMs < 1000) {
            return;
        }
        long p = recvPackets;
        long f = parsedFrames;
        long dp = p - lastStatsPackets;
        long df = f - lastStatsFrames;
        System.out.println("CytonWifiParser stats: packets/s=" + dp + ", frames/s=" + df + ", totalPackets=" + p + ", totalFrames=" + f);
        lastStatsMs = now;
        lastStatsPackets = p;
        lastStatsFrames = f;
    }

    private void pushSample(int[] sample) {
        bufferLock.lock();
        try {
            int[] slot = ring[writePos];
            System.arraycopy(sample, 0, slot, 0, NUM_STORED);

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

    private void parseHealthAux(byte[] f) {
        int tail = f[FRAME_SIZE - 1] & 0xFF;
        int auxType = tail & 0x0F;
        if (auxType < AUX_TYPE_C1 || auxType > AUX_TYPE_C3) {
            return;
        }

        int healthSeq = f[AUX_OFFSET] & 0xFF;
        lastHealthAuxFrameTimestampMs = System.currentTimeMillis();
        if (healthSeq != currentHealthSeq) {
            currentHealthSeq = healthSeq;
            hasHealthC1 = false;
            hasHealthC2 = false;
            hasHealthC3 = false;
            latestHealthC1AuxHex = "--";
            latestHealthC2AuxHex = "--";
            latestHealthC3AuxHex = "--";
        }
        updateHealthAuxDebug(auxType, healthSeq, f);

        CytonWifiHealthRawLogger.logHealthFrame(auxType, healthSeq, f, AUX_OFFSET, AUX_SIZE);

        if (healthSeq == lastPublishedHealthSeq) {
            return;
        }

        int[] target;
        if (auxType == AUX_TYPE_C1) {
            target = healthC1;
            hasHealthC1 = true;
        } else if (auxType == AUX_TYPE_C2) {
            target = healthC2;
            hasHealthC2 = true;
        } else {
            target = healthC3;
            hasHealthC3 = true;
        }

        for (int i = 0; i < AUX_SIZE; i++) {
            target[i] = f[AUX_OFFSET + i] & 0xFF;
        }

        if (hasHealthC1 && hasHealthC2 && hasHealthC3) {
            CytonWifiHealthFrame decodedHealthFrame = buildHealthFrame(healthSeq);
            if (decodedHealthFrame.hasMeasurement()) {
                latestHealthFrame = decodedHealthFrame;
                lastPublishedHealthSeq = healthSeq;
            }
        }
    }

    private void resetHealthState() {
        latestHealthFrame = null;
        lastHealthAuxFrameTimestampMs = -1L;
        latestHealthAuxDebugText = "";
        latestHealthC1AuxHex = "--";
        latestHealthC2AuxHex = "--";
        latestHealthC3AuxHex = "--";
        latestHealthAuxSeq = -1;
        latestHealthAuxType = -1;
        currentHealthSeq = -1;
        lastPublishedHealthSeq = -1;
        hasHealthC1 = false;
        hasHealthC2 = false;
        hasHealthC3 = false;
        for (int i = 0; i < AUX_SIZE; i++) {
            healthC1[i] = 0;
            healthC2[i] = 0;
            healthC3[i] = 0;
        }
    }

    private CytonWifiHealthFrame buildHealthFrame(int healthSeq) {
        return new CytonWifiHealthFrame(
                healthSeq,
                validU8(healthC1[1]),
                validU8(healthC1[2]),
                validU8(healthC1[3]),
                validU8(healthC1[4]),
                validU8(healthC1[5]),
                validU8(healthC2[1]),
                validU8(healthC2[2]),
                validU8(healthC2[3]),
                validU8(healthC2[4]),
                validU8(healthC2[5]),
                validTemperature(healthC3[1], healthC3[2]),
                validTemperature(healthC3[3], healthC3[4]),
                System.currentTimeMillis()
        );
    }

    private static int validU8(int value) {
        return value == INVALID_U8 ? -1 : value;
    }

    private static float validTemperature(int integerPart, int decimalPart) {
        if (integerPart == INVALID_U8 || decimalPart == INVALID_U8) {
            return Float.NaN;
        }
        return integerPart + decimalPart / 100.0f;
    }

    private void updateHealthAuxDebug(int auxType, int healthSeq, byte[] frameBytes) {
        String auxHex = formatAuxHex(frameBytes);
        if (auxType == AUX_TYPE_C1) {
            latestHealthC1AuxHex = auxHex;
        } else if (auxType == AUX_TYPE_C2) {
            latestHealthC2AuxHex = auxHex;
        } else if (auxType == AUX_TYPE_C3) {
            latestHealthC3AuxHex = auxHex;
        }
        latestHealthAuxSeq = healthSeq;
        latestHealthAuxType = auxType;
        latestHealthAuxDebugText = "AUX seq=" + healthSeq
                + " last=C" + auxType
                + " C1=" + latestHealthC1AuxHex
                + " C2=" + latestHealthC2AuxHex
                + " C3=" + latestHealthC3AuxHex;
    }

    private static String formatAuxHex(byte[] frameBytes) {
        StringBuilder sb = new StringBuilder(AUX_SIZE * 3);
        for (int i = 0; i < AUX_SIZE; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            int value = frameBytes[AUX_OFFSET + i] & 0xFF;
            if (value < 0x10) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(value).toUpperCase());
        }
        return sb.toString();
    }
}
