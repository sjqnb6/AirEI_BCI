package BoardGanglion_;

import AccelerometerCapableBoard_.AccelerometerCapableBoard;
import BoardBrainflow_.BoardBrainFlow;
import brainflow.BoardShim;
import brainflow.BrainFlowError;
import brainflow.BrainFlowInputParams;

import java.util.Arrays;
import java.util.List;

import static SystemManager.GF.stopRunning;
import static processing.core.PApplet.println;
import static processing.core.PApplet.str;

public abstract class BoardGanglion extends BoardBrainFlow implements AccelerometerCapableBoard {

    private final char[] deactivateChannelChars = {'1', '2', '3', '4', '5', '6', '7', '8', 'q', 'w', 'e', 'r', 't', 'y', 'u', 'i'};
    private final char[] activateChannelChars =  {'!', '@', '#', '$', '%', '^', '&', '*', 'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I'};

    private int[] accelChannelsCache = null;
    private int[] resistanceChannelsCache = null;

    private boolean[] exgChannelActive;

    protected String serialPort = "";
    protected String macAddress = "";
    protected String ipAddress = "";

    private boolean isCheckingImpedance = false;
    private boolean isGettingAccel = false;

    // implement mandatory abstract functions
    @Override
    protected BrainFlowInputParams getParams() {
        BrainFlowInputParams params = new BrainFlowInputParams();
        params.serial_port = serialPort;
        params.mac_address = macAddress;
        params.ip_address = ipAddress;
        params.ip_port = 6677;
        return params;
    }

    @Override
    public void setEXGChannelActive(int channelIndex, boolean active) {
        char[] charsToUse = active ? activateChannelChars : deactivateChannelChars;
        sendCommand(str(charsToUse[channelIndex]));
        exgChannelActive[channelIndex] = active;
    }

    @Override
    public boolean isEXGChannelActive(int channelIndex) {
        return exgChannelActive[channelIndex];
    }

    @Override
    public boolean initializeInternal()
    {
        // turn on accel by default, or is it handled somewhere else?
        boolean res = super.initializeInternal();

        setAccelerometerActive(true);
        exgChannelActive = new boolean[getNumEXGChannels()];
        Arrays.fill(exgChannelActive, true);

        return res;
    }

    @Override
    public boolean isAccelerometerActive() {
        return isGettingAccel;
    }

    @Override
    public void setAccelerometerActive(boolean active) {
        sendCommand(active ? "n" : "N");
        isGettingAccel = active;
    }

    @Override
    public boolean canDeactivateAccelerometer() {
        return true;
    }

    @Override
    public int[] getAccelerometerChannels() {
        if (accelChannelsCache == null) {
            try {
                accelChannelsCache = BoardShim.get_accel_channels(getBoardIdInt());
            } catch (BrainFlowError e) {
                e.printStackTrace();
            }
        }

        return accelChannelsCache;
    }

    public int[] getResistanceChannels() {
        if (resistanceChannelsCache == null) {
            try {
                resistanceChannelsCache = BoardShim.get_resistance_channels(getBoardIdInt());
            } catch (BrainFlowError e) {
                e.printStackTrace();
            }
        }

        return resistanceChannelsCache;
    }

    public void setCheckingImpedance(boolean checkImpedance) {
        if (checkImpedance) {
            if (isCheckingImpedance) {
                println("Already checking impedance.");
                return;
            }
            if (streaming) {
                stopRunning();
            }
            sendCommand("z");
            startStreaming();
            packetLossTracker = null;
        }
        else {
            if (!isCheckingImpedance) {
                println ("Impedance is not running.");
                return;
            }
            if (streaming) {
                stopStreaming();
            }
            sendCommand("Z");
            packetLossTracker = setupPacketLossTracker();
        }
        isCheckingImpedance = checkImpedance;
    }

    public boolean isCheckingImpedance() {
        return isCheckingImpedance;
    }

    @Override
    protected void addChannelNamesInternal(String[] channelNames) {
        for (int i=0; i<getAccelerometerChannels().length; i++) {
            channelNames[getAccelerometerChannels()[i]] = "Accel Channel " + i;
        }
        channelNames[getMarkerChannel()] = "Marker Channel";
    }

    @Override
    public List<double[]> getDataWithAccel(int maxSamples) {
        return getData(maxSamples);
    }

    @Override
    public int getAccelSampleRate() {
        return getSampleRate();
    }
};