package SerialParser_;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.locks.ReentrantLock;

public class CytonWifiParser implements AutoCloseable {
    public static final int NUM_CHANNELS = 8;
    public static final int NUM_TOTAL = 30;
    private static final int FRAME_SIZE = 33;
    private static final byte HEADER = (byte) 0xA0;
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

        // Bind local UDP port so board broadcast can be received.
        socket = new DatagramSocket(port);
        socket.setBroadcast(true);

        sendCommand("START");

        running = true;
        readerThread = new Thread(this::readLoop, "cyton-wifi-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    public synchronized void stop_stream() {
        running = false;
        sendCommand("STOP");
        try { if (socket != null) socket.close(); } catch (Exception ignore) {}

        if (readerThread != null) {
            try { readerThread.join(500); } catch (InterruptedException ignore) {}
        }

        readerThread = null;
        socket = null;
        targetAddress = null;
        targetPort = 0;
        framePos = 0;
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
                socket.receive(packet);
                int n = packet.getLength();
                if (n <= 0) continue;
                processIncoming(packet.getData(), n);
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
                if ((frame[FRAME_SIZE - 1] & 0xF0) == (byte) 0xC0) {
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
}
