package BoardCyton_;

import GUI.GUIManager;
import Globel.GUI;
import PacketLossTracker_.PacketLossTracker;
import PacketLossTracker_.PacketLossTrackerCytonSerialDaisy;
import brainflow.BoardIds;

public class BoardCytonSerialDaisy extends BoardCytonSerialBase {
    public BoardCytonSerialDaisy(GUI gui) {
        super(gui);

    }

    public BoardCytonSerialDaisy(GUI gui, String serialPort) {
        super(gui);
        this.serialPort = serialPort;
    }

    @Override
    public BoardIds getBoardId() {
        return BoardIds.CYTON_DAISY_BOARD;
    }

    @Override
    protected PacketLossTracker setupPacketLossTracker() {
        return new PacketLossTrackerCytonSerialDaisy(getSampleIndexChannel(), getTimestampChannel());
    }
};
