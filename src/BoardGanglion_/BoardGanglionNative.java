package BoardGanglion_;

import PacketLossTracker_.PacketLossTracker;
import PacketLossTracker_.PacketLossTrackerGanglionBLE;
import PacketLossTracker_.PacketLossTrackerGanglionBLE2;
import PacketLossTracker_.PacketLossTrackerGanglionBLE3;
import PopupMessage_.PopupMessage;
import brainflow.BoardIds;
import brainflow.BrainFlowInputParams;

import static Debugging_.GF.output;
import Globel.GUI;
public class BoardGanglionNative extends BoardGanglion {
    GUI MAIN;
    private PacketLossTrackerGanglionBLE packetLossTrackerGanglionNative;
    private String boardName;
    private int firmwareVersion = 0;

    public BoardGanglionNative(GUI MAIN) {
        super(MAIN);
    }

    public BoardGanglionNative(GUI MAIN, String name, boolean showUpgradePopup) {
        super(MAIN);
        this.MAIN = MAIN;
        this.boardName = name;

        if (name.indexOf("Ganglion 1.3") != -1) {
            this.firmwareVersion = 3;
            output("Detected Ganglion firmware version 3");
        }
        else {
            this.firmwareVersion = 2;
            output("Detected Ganglion firmware version 2");
            if (showUpgradePopup) {
                PopupMessage msg = new PopupMessage(MAIN, "Warning", "Ganglion firmware version 2 detected. Please update to version 3 for better performance. \n\nhttps://docs.openbci.com/Ganglion/GanglionProgram");
            }
        }
    }

    @Override
    protected BrainFlowInputParams getParams() {
        BrainFlowInputParams params = new BrainFlowInputParams();
        params.serial_number = boardName;
        return params;
    }

    @Override
    public BoardIds getBoardId() {
        return BoardIds.GANGLION_NATIVE_BOARD;
    }

    @Override
    public void setAccelerometerActive(boolean active) {
        super.setAccelerometerActive(active);

        if (packetLossTrackerGanglionNative != null) {
            // notify the packet loss tracker, because the sample indices change based
            // on whether accel is active or not
            packetLossTrackerGanglionNative.setAccelerometerActive(active);
        }
    }

    @Override
    protected PacketLossTracker setupPacketLossTracker() {
        if (firmwareVersion == 2) {
            packetLossTrackerGanglionNative = new PacketLossTrackerGanglionBLE2(getSampleIndexChannel(), getTimestampChannel());
        }
        else if (firmwareVersion == 3) {
            packetLossTrackerGanglionNative = new PacketLossTrackerGanglionBLE3(getSampleIndexChannel(), getTimestampChannel());
        }

        packetLossTrackerGanglionNative.setAccelerometerActive(isAccelerometerActive());
        return packetLossTrackerGanglionNative;
    }
};