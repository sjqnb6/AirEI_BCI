package SerialParser_;

import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.locks.ReentrantLock;


public class CytonSerialParser implements AutoCloseable{
    public static final int NUM_CHANNELS = 8;
    public static final int NUM_TOTAL = 30;
    private static final int FRAME_SIZE = 33;
    private static final byte HEADER = (byte) 0xA0;
    private static final byte TAIL   = (byte) 0xC0;
    // ===== ring buffer: [capacity][8 channels] =====
    private final int capacity;
    private final int[][] ring; // ring[sampleIndex][channel]
    private int writePos = 0;   // next write slot
    private int readPos  = 0;   // oldest sample slot
    private int count    = 0;   // number of valid samples
    private final ReentrantLock bufferLock = new ReentrantLock();
    // ===== serial =====
    private SerialPort port;
    private InputStream in;
    private Thread readerThread;
    private volatile boolean running = false;    // ===== parser state =====
    private final byte[] frame = new byte[FRAME_SIZE];
    private int framePos = 0; // how many bytes already filled in frame[]
    private final int[] tmpSample = new int[NUM_CHANNELS]; // reused, no per-frame allocation

    // ===== optional debug stats =====
    private volatile long parsedFrames = 0;
    private volatile long badFrames = 0;
    private volatile long discontinuities = 0;
    private volatile int lastPacketId = -1;
    public CytonSerialParser(int bufferCapacitySamples) {
        if (bufferCapacitySamples <= 0) {
            throw new IllegalArgumentException("bufferCapacitySamples must be > 0");
        }
        this.capacity = bufferCapacitySamples;
        this.ring = new int[capacity][NUM_CHANNELS];
    }    /**
     * Start serial streaming + parsing in a background thread.
     *
     * @param portDescriptor e.g. "COM3" or "/dev/ttyACM0" or "/dev/cu.usbserial-xxx"
     * @param baudRate       e.g. 115200
     */
    public synchronized void start_stream(String portDescriptor, int baudRate) throws IOException {
        if (running) return;

        port = SerialPort.getCommPort(portDescriptor);
        port.setComPortParameters(baudRate, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        // Blocking read: in.read() will wait until data arrives (CPU friendly)
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);

        if (!port.openPort()) {
            throw new IOException("Failed to open serial port: " + portDescriptor);
        }

        in = port.getInputStream();
        // Send 'b' to start data stream
        try {
            port.getOutputStream().write('b');
            port.getOutputStream().flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
        running = true;

        readerThread = new Thread(this::readLoop, "cyton-serial-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    /**
     * Stop streaming and close resources.
     */
    public synchronized void stop_stream() {
        running = false;

        // Send 'c' to stop data stream
        try { if (port != null) port.getOutputStream().write('c'); port.getOutputStream().flush(); } catch (Exception ignore) {}
        // Closing port / stream should unblock a blocking read()
        try { if (in != null) in.close(); } catch (Exception ignore) {}
        try { if (port != null) port.closePort(); } catch (Exception ignore) {}

        if (readerThread != null) {
            try { readerThread.join(500); } catch (InterruptedException ignore) {}
        }

        readerThread = null;
        in = null;
        port = null;
        framePos = 0;
    }
    @Override
    public void close() {
        stop_stream();
    }
    public boolean isRunning() {
        return running;
    }
    /**
     * Drain ring buffer into int[8][N].
     * First dimension: channel (0..7)
     * Second dimension: sample index (time)
     */
    public double[][] get_data() {
        bufferLock.lock();
        try {
            int n = count;
            double[][] out = new double[NUM_TOTAL][n];

            for (int i = 0; i < n; i++) {
                int idx = (readPos + i) % capacity;
                int[] sample = ring[idx];
                for (int ch = 0; ch < NUM_CHANNELS; ch++) {
                    out[ch][i] = (double)sample[ch];
                }
            }

            // drain
            count = 0;
            readPos = writePos;
            return out;
        } finally {
            bufferLock.unlock();
        }
    }
    // ===================== internal: read + parse =====================
    private void readLoop() {
        byte[] buf = new byte[1024];

        try {
            while (running) {
                int n = in.read(buf);
                if (n < 0) break;
                if (n == 0) continue;

                processIncoming(buf, n);
            }
        } catch (Exception e) {
            if (running) {
                // 真实项目建议换成日志系统
                e.printStackTrace();
            }
        } finally {
            running = false;
            try { if (in != null) in.close(); } catch (Exception ignore) {}
            try { if (port != null) port.closePort(); } catch (Exception ignore) {}
        }
    }
    /**
     * Robust framing:
     * - find HEADER
     * - collect 33 bytes
     * - verify TAIL
     * - if tail mismatch: resync by searching next HEADER inside the frame
     */
    private void processIncoming(byte[] buf, int len) {
        int i = 0;

        while (i < len) {
            if (framePos == 0) {
                // seek header 0xA0
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
                if (frame[FRAME_SIZE - 1] == TAIL) {
                    parseFrame(frame);
                    framePos = 0;
                } else {
                    badFrames++;

                    // resync: find next HEADER within current frame[1..32]
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
    /**
     * Parse 8 channels of signed 24-bit big-endian into int (sign-extended).
     */
    private void parseFrame(byte[] f) {
        int packetId = f[1] & 0xFF;

        // optional continuity check
        int prev = lastPacketId;
        if (prev != -1) {
            int expected = (prev + 1) & 0xFF;
            if (packetId != expected) discontinuities++;
        }
        lastPacketId = packetId;

        int off = 2;
        for (int ch = 0; ch < NUM_CHANNELS; ch++) {
            int raw = ((f[off] & 0xFF) << 16)
                    | ((f[off + 1] & 0xFF) << 8)
                    | (f[off + 2] & 0xFF);

            // sign-extend 24-bit to 32-bit
            if ((raw & 0x800000) != 0) {
                raw |= 0xFF000000;
            }

            tmpSample[ch] = raw;
            off += 3;
        }

        pushSample(tmpSample);
        parsedFrames++;
    }
    /**
     * Push one sample into ring buffer. Overwrite oldest when full.
     */
    private void pushSample(int[] sample) {
        bufferLock.lock();
        try {
            int[] slot = ring[writePos];
            System.arraycopy(sample, 0, slot, 0, NUM_CHANNELS);

            if (count == capacity) {
                // overwrite oldest
                readPos = (readPos + 1) % capacity;
            } else {
                count++;
            }

            writePos = (writePos + 1) % capacity;
        } finally {
            bufferLock.unlock();
        }
    }
    // ===== optional debug getters =====
    public long getParsedFrames() { return parsedFrames; }
    public long getBadFrames() { return badFrames; }
    public long getDiscontinuities() { return discontinuities; }
    public int getLastPacketId() { return lastPacketId; }

}
