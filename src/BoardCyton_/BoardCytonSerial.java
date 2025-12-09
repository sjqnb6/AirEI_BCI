package BoardCyton_;

import GUI.GUIManager;
import PacketLossTracker_.PacketLossTracker;
import brainflow.BoardIds;

public class BoardCytonSerial extends BoardCytonSerialBase {
    public BoardCytonSerial(GUIManager gui) {
        super(gui);
    }

    public BoardCytonSerial(GUIManager gui, String serialPort) {
        super(gui);
        this.serialPort = serialPort;
    }

    @Override
    public BoardIds getBoardId() {
        return BoardIds.CYTON_BOARD;
    }

    @Override
    protected PacketLossTracker setupPacketLossTracker() {
        final int minSampleIndex = 0;
        final int maxSampleIndex = 255;
        return new PacketLossTracker(getSampleIndexChannel(), getTimestampChannel(),
                minSampleIndex, maxSampleIndex);
    }
};
