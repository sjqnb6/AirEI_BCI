package BoardBrainFlowStreaming_;

import BoardBrainflow_.BoardBrainFlow;
import PacketLossTracker_.PacketLossTracker;
import PacketLossTracker_.PacketLossTrackerCytonSerialDaisy;
import brainflow.BoardIds;
import brainflow.BoardShim;
import brainflow.BrainFlowError;
import brainflow.BrainFlowInputParams;

import static Debugging_.GF.outputError;
import static GUI.GGVI.directoryManager;


public class BoardBrainFlowStreaming extends BoardBrainFlow {

    private BoardIds masterBoardId;
    private String ipAddress;
    private int ipPort;

    public BoardBrainFlowStreaming(BoardIds masterBoardId, String ipAddress, int ipPort) {
        super();
        this.masterBoardId = masterBoardId;
        this.ipAddress = ipAddress;
        this.ipPort = ipPort;
    }

    // implement mandatory abstract functions
    @Override
    protected BrainFlowInputParams getParams() {
        BrainFlowInputParams params = new BrainFlowInputParams();
        params.ip_address = ipAddress;
        params.ip_port = ipPort;
        params.master_board = masterBoardId.get_code();
        return params;
    }

    // for streaming board need to use master board id in function like  get_eeg_channels
    @Override
    public BoardIds getBoardId() {
        return masterBoardId;
    }

    @Override
    public boolean initializeInternal() {
        try {
            // here we need to provide board id of streaming board
            boardShim = new BoardShim (BoardIds.STREAMING_BOARD.get_code(), getParams());
            try {
                BoardShim.enable_dev_board_logger();
                BoardShim.set_log_file(directoryManager.getConsoleDataPath() + "Brainflow_" +
                        directoryManager.getFileNameDateTime() + ".txt");
            } catch (BrainFlowError e) {
                e.printStackTrace();
            }
            boardShim.prepare_session();
            return true;

        } catch (Exception e) {
            boardShim = null;
            outputError("ERROR: " + e + " when initializing Brainflow board. Data will not stream.");
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void setEXGChannelActive(int channelIndex, boolean active) {
        // do nothing here
    }

    @Override
    public boolean isEXGChannelActive(int channelIndex) {
        return true;
    }

    @Override
    protected void addChannelNamesInternal(String[] channelNames) {
        // do nothing here
    }

    @Override
    protected PacketLossTracker setupPacketLossTracker() {
        if (masterBoardId == BoardIds.CYTON_DAISY_BOARD) {
            return new PacketLossTrackerCytonSerialDaisy(getSampleIndexChannel(), getTimestampChannel());
        }
        final int minSampleIndex = 0;
        final int maxSampleIndex = 255;
        return new PacketLossTracker(getSampleIndexChannel(), getTimestampChannel(),
                minSampleIndex, maxSampleIndex);
    }

};
