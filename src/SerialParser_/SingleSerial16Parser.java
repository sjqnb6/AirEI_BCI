package SerialParser_;

import com.fazecast.jSerialComm.SerialPort;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.ReentrantLock;

/** One-port serial receiver and buffer for complete 16-channel samples. */
public final class SingleSerial16Parser implements AutoCloseable {
    public static final int NUM_CHANNELS = 16;
    public static final int NUM_TOTAL = 30;
    public static final int SAMPLE_INDEX_CHANNEL = 0;
    public static final int FIRST_EXG_CHANNEL = 1;
    public static final int TIMESTAMP_CHANNEL = 22;

    private final int capacity;
    private final Decoded16ChannelSample[] ring;
    private final Serial16FrameDecoder decoder;
    private final ReentrantLock bufferLock = new ReentrantLock();
    private final Object resourceLock = new Object();

    private int writePos;
    private int readPos;
    private int count;
    private int generatedSequence;

    private SerialPort port;
    private InputStream input;
    private Thread readerThread;
    private volatile boolean running;

    public SingleSerial16Parser(int bufferCapacitySamples) {
        this(bufferCapacitySamples, new DomesticSerial16FrameDecoder());
    }

    SingleSerial16Parser(int bufferCapacitySamples, Serial16FrameDecoder decoder) {
        if (bufferCapacitySamples <= 0) {
            throw new IllegalArgumentException("bufferCapacitySamples must be > 0");
        }
        if (decoder == null) {
            throw new IllegalArgumentException("decoder must not be null");
        }
        capacity = bufferCapacitySamples;
        ring = new Decoded16ChannelSample[capacity];
        this.decoder = decoder;
    }

    public synchronized void start_stream(String portDescriptor, int baudRate) throws IOException {
        if (running) {
            return;
        }

        decoder.reset();
        clearBuffer();
        port = SerialPort.getCommPort(portDescriptor);
        port.setComPortParameters(baudRate, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);
        if (!port.openPort()) {
            port = null;
            throw new IOException("Failed to open serial port: " + portDescriptor);
        }

        try {
            input = port.getInputStream();
            writeBytes(decoder.getStartCommand());
        } catch (Exception e) {
            closeResources();
            throw new IOException("Failed to start serial stream on: " + portDescriptor, e);
        }

        running = true;
        readerThread = new Thread(this::readLoop, "domestic-serial-16-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    public synchronized void stop_stream() {
        running = false;
        try {
            writeBytes(decoder.getStopCommand());
        } catch (Exception ignored) {
            // Closing must continue even when the board has already disconnected.
        }
        closeResources();

        if (readerThread != null && readerThread != Thread.currentThread()) {
            try {
                readerThread.join(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        readerThread = null;
        decoder.reset();
    }

    public boolean isRunning() {
        return running && port != null && port.isOpen();
    }

    public synchronized void sendCommand(String command) throws IOException {
        if (command == null || command.isEmpty()) {
            return;
        }
        writeBytes(command.getBytes(StandardCharsets.US_ASCII));
    }

    /** Drains buffered samples into the project's [row][sample] matrix. */
    public double[][] get_data() {
        bufferLock.lock();
        try {
            int sampleCount = count;
            double[][] output = new double[NUM_TOTAL][sampleCount];
            long nowNanos = System.nanoTime();
            double nowEpochSeconds = System.currentTimeMillis() / 1000.0;

            for (int i = 0; i < sampleCount; i++) {
                int index = (readPos + i) % capacity;
                Decoded16ChannelSample sample = ring[index];
                output[SAMPLE_INDEX_CHANNEL][i] = normalizedSequence(sample.getSequence());
                for (int channel = 0; channel < NUM_CHANNELS; channel++) {
                    output[FIRST_EXG_CHANNEL + channel][i] = sample.getChannelUv(channel);
                }
                long receivedAtNanos = sample.getReceivedAtNanos() > 0
                        ? sample.getReceivedAtNanos()
                        : nowNanos;
                output[TIMESTAMP_CHANNEL][i] = nowEpochSeconds +
                        (receivedAtNanos - nowNanos) / 1_000_000_000.0;
                ring[index] = null;
            }

            count = 0;
            readPos = writePos;
            return output;
        } finally {
            bufferLock.unlock();
        }
    }

    @Override
    public void close() {
        stop_stream();
    }

    private void readLoop() {
        byte[] buffer = new byte[4096];
        try {
            while (running) {
                int bytesRead = input.read(buffer);
                if (bytesRead < 0) {
                    break;
                }
                if (bytesRead > 0) {
                    decoder.accept(buffer, 0, bytesRead, this::pushSample);
                }
            }
        } catch (Exception e) {
            if (running) {
                System.err.println("16-channel serial read failed: " + e.getMessage());
            }
        } finally {
            running = false;
            closeResources();
        }
    }

    private void pushSample(Decoded16ChannelSample sample) {
        if (sample == null) {
            return;
        }
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

    private int normalizedSequence(int sequence) {
        if (sequence >= 0) {
            int result = sequence & 0xFF;
            generatedSequence = (result + 1) & 0xFF;
            return result;
        }
        int result = generatedSequence & 0xFF;
        generatedSequence = (generatedSequence + 1) & 0xFF;
        return result;
    }

    private void clearBuffer() {
        bufferLock.lock();
        try {
            for (int i = 0; i < ring.length; i++) {
                ring[i] = null;
            }
            writePos = 0;
            readPos = 0;
            count = 0;
            generatedSequence = 0;
        } finally {
            bufferLock.unlock();
        }
    }

    private void writeBytes(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length == 0) {
            return;
        }
        if (port == null || !port.isOpen()) {
            throw new IOException("Serial port is not open");
        }
        int written = port.writeBytes(bytes, bytes.length);
        if (written != bytes.length) {
            throw new IOException("Incomplete serial write");
        }
    }

    private void closeResources() {
        synchronized (resourceLock) {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (Exception ignored) {
            }
            try {
                if (port != null) {
                    port.closePort();
                }
            } catch (Exception ignored) {
            }
            input = null;
            port = null;
        }
    }
}
