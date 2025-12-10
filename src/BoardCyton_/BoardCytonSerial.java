package BoardCyton_;

import GUI.GUIManager;
import PacketLossTracker_.PacketLossTracker;
import brainflow.BoardIds;
import Globel.GUI;
    public class BoardCytonSerial extends BoardCytonSerialBase {
    public BoardCytonSerial(GUI MAIN) {
        super(MAIN);
    }

    public BoardCytonSerial(GUI MAIN, String serialPort) {
        super(MAIN);
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
