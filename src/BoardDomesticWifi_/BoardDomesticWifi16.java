package BoardDomesticWifi_;

import Board_.Board;
import Globel.GUI;
import PacketLossTracker_.PacketLossTracker;
import SerialParser_.DualCytonWifi16Parser;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.Arrays;

import static Debugging_.GF.outputError;
import static processing.core.PApplet.println;

/** Two 8-channel WiFi boards on one router exposed as one 16-channel board. */
public final class BoardDomesticWifi16 extends Board {
    private static final int DEFAULT_WIFI_PORT = 5005;
    private static final int DEFAULT_SAMPLE_RATE = 250;
    private static final int MARKER_CHANNEL = 29;
    private static final int[] EXG_CHANNELS = {
            1, 2, 3, 4, 5, 6, 7, 8,
            9, 10, 11, 12, 13, 14, 15, 16
    };

    private final String ipConfiguration;
    private final int sampleRate;
    private final boolean[] channelActive = new boolean[16];

    private String board1Ip;
    private String board2Ip;
    private DualCytonWifi16Parser parser;
    private volatile boolean streaming;
    private volatile double pendingMarker;

    /** Constructor used when a recorded session is opened for playback. */
    public BoardDomesticWifi16(GUI main) {
        this(main, null, DEFAULT_SAMPLE_RATE);
    }

    public BoardDomesticWifi16(GUI main, String ipConfiguration, int sampleRate) {
        super(main);
        this.ipConfiguration = ipConfiguration;
        this.sampleRate = sampleRate > 0 ? sampleRate : DEFAULT_SAMPLE_RATE;
        Arrays.fill(channelActive, true);
    }

    @Override
    protected boolean initializeInternal() {
        String[] addresses = parseAddresses(ipConfiguration);
        if (addresses == null) {
            outputError(
                    "16-channel WiFi mode requires two IP addresses, for example: " +
                    "192.168.1.101,192.168.1.102"
            );
            return false;
        }
        board1Ip = addresses[0];
        board2Ip = addresses[1];
        parser = new DualCytonWifi16Parser(20000);
        println("16-channel WiFi boards configured: " + board1Ip + " + " + board2Ip);
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
        // UDP reception runs on one background thread inside the parser.
    }

    @Override
    public void startStreaming() {
        if (streaming || parser == null) {
            return;
        }
        super.startStreaming();
        try {
            parser.start_stream(board1Ip, board2Ip, DEFAULT_WIFI_PORT);
            streaming = true;
            println("Dual-board 16-channel WiFi streaming started");
        } catch (Exception e) {
            streaming = false;
            outputError("Failed to start 16-channel WiFi stream: " + e.getMessage());
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
    protected double[][] getNewDataInternal() {
        if (!streaming || parser == null) {
            return emptyData;
        }
        double[][] data = parser.get_data();
        if (pendingMarker != 0.0 && data[0].length > 0) {
            data[MARKER_CHANNEL][0] = pendingMarker;
            pendingMarker = 0.0;
        }
        return data;
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
            return new ImmutablePair<Boolean, String>(false, "WiFi stream is not connected");
        }
        try {
            parser.sendCommand(command);
            return new ImmutablePair<Boolean, String>(true, "Command sent to both WiFi boards");
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
        return DualCytonWifi16Parser.TIMESTAMP_CHANNEL;
    }

    @Override
    public int getSampleIndexChannel() {
        return DualCytonWifi16Parser.SAMPLE_INDEX_CHANNEL;
    }

    @Override
    public int getTotalChannelCount() {
        return DualCytonWifi16Parser.NUM_TOTAL;
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

    public long getReceivedBoard1Packets() {
        return parser == null ? 0 : parser.getReceivedBoard1Packets();
    }

    public long getReceivedBoard2Packets() {
        return parser == null ? 0 : parser.getReceivedBoard2Packets();
    }

    public long getPairedSamples() {
        return parser == null ? 0 : parser.getPairedSamples();
    }

    private static String[] parseAddresses(String configuration) {
        if (configuration == null) {
            return null;
        }
        String normalized = configuration.replace('，', ',');
        String[] parts = normalized.split(",");
        if (parts.length != 2) {
            return null;
        }
        String first = parts[0].trim();
        String second = parts[1].trim();
        if (first.isEmpty() || second.isEmpty() || first.equalsIgnoreCase(second)) {
            return null;
        }
        return new String[]{first, second};
    }
}
