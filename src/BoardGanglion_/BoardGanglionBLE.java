package BoardGanglion_;

import PacketLossTracker_.PacketLossTracker;
import PacketLossTracker_.PacketLossTrackerGanglionBLE;
import PacketLossTracker_.PacketLossTrackerGanglionBLE2;
import PacketLossTracker_.PacketLossTrackerGanglionBLE3;
import PopupMessage_.PopupMessage;
import brainflow.BoardIds;

import static Debugging_.GF.output;

public class BoardGanglionBLE extends BoardGanglion {

    private int firmwareVersion = 0;
    private PacketLossTrackerGanglionBLE packetLossTrackerGanglionBLE;

    public BoardGanglionBLE() {
        super();
    }

    public BoardGanglionBLE(String deviceName, String serialPort, String macAddress, boolean showUpgradePopup) {
        super();
        this.serialPort = serialPort;
        this.macAddress = macAddress;

        if (deviceName.indexOf("Ganglion 1.3") != -1) {
            this.firmwareVersion = 3;
            output("Detected Ganglion firmware version 3");
        }
        else {
            this.firmwareVersion = 2;
            if (showUpgradePopup) {
                PopupMessage msg = new PopupMessage("Warning", "Ganglion firmware version 2 detected. Please update to version 3 for better performance. \n\nhttps://docs.openbci.com/Ganglion/GanglionProgram");
            }
            output("Detected Ganglion firmware version 2");
        }
    }

    @Override
    public BoardIds getBoardId() {
        return BoardIds.GANGLION_BOARD;
    }

    @Override
    public void setAccelerometerActive(boolean active) {
        super.setAccelerometerActive(active);

        if (packetLossTrackerGanglionBLE != null) {
            // notify the packet loss tracker, because the sample indices change based
            // on whether accel is active or not
            packetLossTrackerGanglionBLE.setAccelerometerActive(active);
        }
    }

    @Override
    protected PacketLossTracker setupPacketLossTracker() {
        if (firmwareVersion == 2) {
            packetLossTrackerGanglionBLE = new PacketLossTrackerGanglionBLE2(getSampleIndexChannel(), getTimestampChannel());
        }
        else if (firmwareVersion == 3) {
            packetLossTrackerGanglionBLE = new PacketLossTrackerGanglionBLE3(getSampleIndexChannel(), getTimestampChannel());
        }

        packetLossTrackerGanglionBLE.setAccelerometerActive(isAccelerometerActive());
        return packetLossTrackerGanglionBLE;
    }
};
