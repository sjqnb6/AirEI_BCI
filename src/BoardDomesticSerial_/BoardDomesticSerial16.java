package BoardDomesticSerial_;

import Board_.Board;
import Buffer_.Buffer;
import Globel.GUI;
import PacketLossTracker_.PacketLossTracker;
import SerialParser_.SingleSerial16Parser;
import SmoothingBoard_.SmoothingCapableBoard;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.Arrays;

import static Debugging_.GF.outputError;
import static processing.core.PApplet.println;

/** A single serial connection carrying one complete 16-channel data stream. */
public final class BoardDomesticSerial16 extends Board implements SmoothingCapableBoard {
    public static final String BAUD_PROPERTY = "bci.serial16.baud";
    public static final String SAMPLE_RATE_PROPERTY = "bci.serial16.sample_rate";

    private static final int DEFAULT_BAUD = 921600;
    private static final int DEFAULT_SAMPLE_RATE = 250;
    private static final int MARKER_CHANNEL = 29;
    private static final int[] EXG_CHANNELS = {
            1, 2, 3, 4, 5, 6, 7, 8,
            9, 10, 11, 12, 13, 14, 15, 16
    };

    private final GUI main;
    private final String serialPort;
    private final int baudRate;
    private final int sampleRate;
    private final boolean[] channelActive = new boolean[16];

    private SingleSerial16Parser parser;
    private Buffer<double[]> smoothingBuffer;
    private volatile boolean smoothData;
    private volatile boolean streaming;
    private volatile double pendingMarker;

    /** Constructor used when recorded data is opened for playback. */
    public BoardDomesticSerial16(GUI main) {
        this(main, null);
    }

    public BoardDomesticSerial16(GUI main, String serialPort) {
        super(main);
        this.main = main;
        this.serialPort = serialPort;
        baudRate = positiveIntProperty(BAUD_PROPERTY, DEFAULT_BAUD);
        sampleRate = positiveIntProperty(SAMPLE_RATE_PROPERTY, DEFAULT_SAMPLE_RATE);
        Arrays.fill(channelActive, true);
        setSmoothingActive(true);
    }

    @Override
    protected boolean initializeInternal() {
        if (serialPort == null || serialPort.trim().isEmpty() || "N/A".equalsIgnoreCase(serialPort.trim())) {
            outputError("16-channel serial mode requires a serial port.");
            return false;
        }
        parser = new SingleSerial16Parser(20000);
        return true;
    }

    @Override
    protected void uninitializeInternal() {
        if (parser != null) {
            parser.close();
            parser = null;
        }
        streaming = false;
    }

    @Override
    protected void updateInternal() {
        // The parser reads the single serial port on a background thread.
    }

    @Override
    public void startStreaming() {
        if (streaming || parser == null) {
            return;
        }
        super.startStreaming();
        try {
            if (smoothData) {
                smoothingBuffer = new Buffer<double[]>(main, getSampleRate());
            }
            parser.start_stream(serialPort, baudRate);
            streaming = true;
            println("Single-port 16-channel streaming started on " + serialPort);
        } catch (IOException e) {
            streaming = false;
            outputError("Failed to start 16-channel serial stream: " + e.getMessage());
        }
    }

    @Override
    public void stopStreaming() {
        if (parser != null) {
            parser.stop_stream();
        }
        streaming = false;
    }

    @Override
    protected synchronized double[][] getNewDataInternal() {
        if (!streaming || parser == null) {
            return emptyData;
        }
        double[][] data = parser.get_data();
        if (pendingMarker != 0.0 && data[0].length > 0) {
            data[MARKER_CHANNEL][0] = pendingMarker;
            pendingMarker = 0.0;
        }
        if (!smoothData) {
            return data;
        }

        for (int sample = 0; sample < data[0].length; sample++) {
            double[] entry = new double[getTotalChannelCount()];
            for (int row = 0; row < getTotalChannelCount(); row++) {
                entry[row] = data[row][sample];
            }
            smoothingBuffer.addNewEntry(entry);
        }

        int available = smoothingBuffer.getDataCount();
        if (available == 0) {
            return emptyData;
        }
        double[][] smoothed = new double[getTotalChannelCount()][available];
        for (int sample = 0; sample < available; sample++) {
            double[] entry = smoothingBuffer.popFirstEntry();
            for (int row = 0; row < getTotalChannelCount(); row++) {
                smoothed[row][sample] = entry[row];
            }
        }
        return smoothed;
    }

    @Override
    public synchronized void setSmoothingActive(boolean active) {
        if (smoothData == active) {
            return;
        }
        smoothingBuffer = active ? new Buffer<double[]>(main, getSampleRate()) : null;
        smoothData = active;
    }

    @Override
    public boolean getSmoothingActive() {
        return smoothData;
    }

    @Override
    public boolean isConnected() {
        return parser != null && parser.isRunning();
    }

    @Override
    public boolean isStreaming() {
        return streaming && parser != null && parser.isRunning();
    }

    @Override
    public Pair<Boolean, String> sendCommand(String command) {
        if (parser == null || !parser.isRunning()) {
            return new ImmutablePair<Boolean, String>(false, "Serial port is not connected");
        }
        try {
            parser.sendCommand(command);
            return new ImmutablePair<Boolean, String>(true, "Command sent");
        } catch (IOException e) {
            return new ImmutablePair<Boolean, String>(false, e.getMessage());
        }
    }

    @Override
    public void insertMarker(int value) {
        insertMarker((double) value);
    }

    @Override
    public void insertMarker(double value) {
        pendingMarker = value;
    }

    @Override
    public int getSampleRate() {
        return sampleRate;
    }

    @Override
    public void setEXGChannelActive(int channelIndex, boolean active) {
        if (channelIndex >= 0 && channelIndex < channelActive.length) {
            channelActive[channelIndex] = active;
        }
    }

    @Override
    public boolean isEXGChannelActive(int channelIndex) {
        return channelIndex >= 0 && channelIndex < channelActive.length && channelActive[channelIndex];
    }

    @Override
    public int[] getEXGChannels() {
        return Arrays.copyOf(EXG_CHANNELS, EXG_CHANNELS.length);
    }

    @Override
    public int getTimestampChannel() {
        return SingleSerial16Parser.TIMESTAMP_CHANNEL;
    }

    @Override
    public int getSampleIndexChannel() {
        return SingleSerial16Parser.SAMPLE_INDEX_CHANNEL;
    }

    @Override
    public int getTotalChannelCount() {
        return SingleSerial16Parser.NUM_TOTAL;
    }

    @Override
    public int getMarkerChannel() {
        return MARKER_CHANNEL;
    }

    @Override
    protected void addChannelNamesInternal(String[] channelNames) {
        channelNames[MARKER_CHANNEL] = "Marker Channel";
    }

    @Override
    protected PacketLossTracker setupPacketLossTracker() {
        return new PacketLossTracker(getSampleIndexChannel(), getTimestampChannel(), 0, 255);
    }

    private static int positiveIntProperty(String propertyName, int fallback) {
        try {
            int value = Integer.parseInt(System.getProperty(propertyName, Integer.toString(fallback)));
            return value > 0 ? value : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
