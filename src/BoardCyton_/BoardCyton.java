package BoardCyton_;

import ADS1299SettingsBoard_.*;
import AccelerometerCapableBoard_.AccelerometerCapableBoard;
import AnalogCapableBoard_.AnalogCapableBoard;
import BoardBrainflow_.BoardBrainFlow;
import DigitalCapableBoard_.DigitalCapableBoard;
import Globel.GUI;
import ImpedanceSettingsBoard_.ImpedanceSettingsBoard;
import brainflow.BoardShim;
import brainflow.BrainFlowError;
import brainflow.BrainFlowInputParams;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.List;

import static Debugging_.GF.outputError;
import static Debugging_.GF.outputWarn;
import static Globel.GUI.cyton_sdSetting;
import static processing.core.PApplet.println;

public abstract class BoardCyton extends BoardBrainFlow
        implements ImpedanceSettingsBoard, AccelerometerCapableBoard, AnalogCapableBoard, DigitalCapableBoard, ADS1299SettingsBoard {
    private final char[] channelSelectForSettings = {'1', '2', '3', '4', '5', '6', '7', '8', 'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I'};
    GUI MAIN;
    private ADS1299Settings currentADS1299Settings;
    private boolean[] localEXGChannelActive;
    private boolean[] isCheckingImpedance;
    protected boolean[] isCheckingImpedanceN;
    protected boolean[] isCheckingImpedanceP;

    private int[] accelChannelsCache = null;
    private int[] analogChannelsCache = null;

    protected String serialPort = "";
    protected String ipAddress = "";
    private CytonBoardMode currentBoardMode = CytonBoardMode.DEFAULT;

    public BoardCyton(GUI MAIN) {
        super(MAIN);

        isCheckingImpedance = new boolean[getNumEXGChannels()];
        Arrays.fill(isCheckingImpedance, false);

        localEXGChannelActive = new boolean[getNumEXGChannels()];
        Arrays.fill(localEXGChannelActive, true);

        isCheckingImpedanceN= new boolean[getNumEXGChannels()];
        isCheckingImpedanceP= new boolean[getNumEXGChannels()];
        Arrays.fill(isCheckingImpedanceN, false);
        Arrays.fill(isCheckingImpedanceP, false);

        // The command 'd' is automatically sent by brainflow on prepare_session
        currentADS1299Settings = new CytonDefaultSettings(this);
    }

    // implement mandatory abstract functions
    @Override
    protected BrainFlowInputParams getParams() {
        BrainFlowInputParams params = new BrainFlowInputParams();
        params.serial_port = serialPort;
        params.ip_address = ipAddress;
        params.ip_port = 6677;
        return params;
    }

    @Override
    public boolean initializeInternal() {
        return super.initializeInternal();
    }

    @Override
    public void uninitializeInternal() {
        closeSDFile();
        super.uninitializeInternal();
    }

    @Override
    public void setEXGChannelActive(int channelIndex, boolean active) {
        localEXGChannelActive[channelIndex] = active;
    }

    @Override
    public boolean isEXGChannelActive(int channelIndex) {
        return localEXGChannelActive[channelIndex];
    }

    @Override
    public int getAccelSampleRate() {
        return getSampleRate();
    }

    @Override
    public int getAnalogSampleRate() {
        return getSampleRate();
    }

    @Override
    public int getDigitalSampleRate() {
        return getSampleRate();
    }

    @Override
    public boolean isAccelerometerActive() {
        return getBoardMode() == CytonBoardMode.DEFAULT;
    }

    @Override
    public void setAccelerometerActive(boolean active) {
        if(active) {
            setBoardMode(CytonBoardMode.DEFAULT);
        }
        // no way of turning off accel.
    }

    @Override
    public boolean canDeactivateAccelerometer() {
        //Accelerometer is on by default for Cyton, and can not be disabled using a command.
        //Disabling another Cyton Aux mode (ex. Analog Read) will default the board back to Accelerometer mode.
        return false;
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

    @Override
    public boolean isAnalogActive() {
        return getBoardMode() == CytonBoardMode.ANALOG;
    }

    @Override
    public void setAnalogActive(boolean active) {
        if(active) {
            setBoardMode(CytonBoardMode.ANALOG);
        }
    }

    @Override
    public boolean canDeactivateAnalog() {
        //For Cyton in the GUI, you can switch to another board mode and essentially deactivate analog read mode
        return true;
    }

    @Override
    public int[] getAnalogChannels() {
        if (analogChannelsCache == null) {
            try {
                analogChannelsCache = BoardShim.get_analog_channels(getBoardIdInt());
            } catch (BrainFlowError e) {
                e.printStackTrace();
            }
        }

        return analogChannelsCache;
    }

    @Override
    public boolean isDigitalActive() {
        return getBoardMode() == CytonBoardMode.DIGITAL;
    }

    @Override
    public void setDigitalActive(boolean active) {
        if(active) {
            setBoardMode(CytonBoardMode.DIGITAL);
        }
    }

    @Override
    public boolean canDeactivateDigital() {
        //For Cyton in the GUI, you can switch to another board mode and essentially deactivate digital read mode
        return true;
    }

    @Override
    public int[] getDigitalChannels() {
        // the removeAll function will remove array indices 0 and 5.
        // remove other_channel[0] because it's the end byte
        // remove other_channels[5] because it does not contain digital data
        int[] digitalChannels = ArrayUtils.removeAll(getOtherChannels(), 0, 5); // remove non-digital channels
        return digitalChannels;
    }

    @Override
    public void setCheckingImpedance(int channel, boolean active) {
        char p = '0';
        char n = '0';

        if (active) {
            Srb2 srb2sSetting = currentADS1299Settings.values.srb2[channel];
            if (srb2sSetting == Srb2.CONNECT) {
                n = '1';
            }
            else {
                p = '1';
            }
        }

        // for example: z 4 1 0 Z
        String command = String.format("z%c%c%cZ", channelSelectForSettings[channel], p, n);
        sendCommand(command);

        isCheckingImpedance[channel] = active;
    }

    //Use this method instead of the one above!
    public Pair<Boolean, String> setCheckingImpedanceCyton(final int channel, final boolean active, final boolean _isN) {

        char p = '0';
        char n = '0';
        //Build a command string so we can send 1 command to Cyton instead of 2!
        //Hopefully, this lowers the chance of confusing the board with multiple commands sent quickly
        StringBuilder fullCommand = new StringBuilder();

        //println("CYTON_IMP_CHECK -- Attempting to change channel== " + channel + " || isActive == " + active);

        if (active) {

            currentADS1299Settings.saveLastValues(channel);

            currentADS1299Settings.values.gain[channel] = Gain.X1;
            currentADS1299Settings.values.inputType[channel] = InputType.NORMAL;
            currentADS1299Settings.values.bias[channel] = Bias.INCLUDE;
            currentADS1299Settings.values.srb2[channel] = Srb2.DISCONNECT;
            currentADS1299Settings.values.srb1[channel] = Srb1.DISCONNECT;

            fullCommand.append(currentADS1299Settings.getValuesString(channel, currentADS1299Settings.values));

            if (_isN) {
                n = '1';
            } else {
                p = '1';
            }

        } else {
            //Revert ADS channel settings to what user had before checking impedance on this channel
            currentADS1299Settings.revertToLastValues(channel);
            fullCommand.append(currentADS1299Settings.getValuesString(channel, currentADS1299Settings.values));
            //println("CYTON REVERTING TO PREVIOUS ADS SETTINGS");
        }

        // Format the impedance command string. Example: z 4 1 0 Z
        String impedanceCommandString = String.format("z%c%c%cZ", channelSelectForSettings[channel], p, n);
        fullCommand.append(impedanceCommandString);
        final String commandToSend = fullCommand.toString();

        final Pair<Boolean, String> fullResponse = sendCommand(commandToSend);
        boolean response = fullResponse.getKey().booleanValue();
        if (!response) {
            outputWarn("Cyton Impedance Check - Error sending impedance command to board.");
            if (active) {
                currentADS1299Settings.revertToLastValues(channel);
                return new ImmutablePair<Boolean, String>(false, "Error");
            }
        }

        if (_isN) {
            isCheckingImpedanceN[channel] = active;
        } else {
            isCheckingImpedanceP[channel] = active;
        }

        return fullResponse;
    }

    @Override
    //General check that is a method for all impedance boards
    public boolean isCheckingImpedance(int channel) {
        return isCheckingImpedanceN[channel] || isCheckingImpedanceP[channel];
    }

    //Specifically check the status of N or P pins
    public boolean isCheckingImpedanceNorP(int channel, boolean _isN) {
        if (_isN) {
            return isCheckingImpedanceN[channel];
        }
        return isCheckingImpedanceP[channel];
    }

    //Returns <pin, channel> if found
    //Return <null,null> if not checking on any channels
    public Pair<Boolean, Integer> isCheckingImpedanceOnAnyChannelsNorP() {
        Boolean is_n_pin = true;
        for (int i = 0; i < isCheckingImpedanceN.length; i++) {
            if (isCheckingImpedanceN[i]) {
                return new ImmutablePair<Boolean, Integer>(is_n_pin, Integer.valueOf(i));
            }
            if (isCheckingImpedanceP[i]) {
                is_n_pin = false;
                return new ImmutablePair<Boolean, Integer>(is_n_pin, Integer.valueOf(i));
            }
        }
        return new ImmutablePair<Boolean, Integer>(null, null);
    }

    //Returns the channel number where impedance check is currently active, otherwise return null
    //Less detailed than the previous method
    @Override
    public Integer isCheckingImpedanceOnChannel() {
        //printArray(isCheckingImpedance);
        for (int i = 0; i < isCheckingImpedance.length; i++) {
            if (isCheckingImpedance(i)) {
                return i;
            }
        }
        return null;
    }

    public void forceStopImpedanceFrontEnd(Integer channel, Boolean _isN) {
        if (channel == null || _isN == null) {
            outputError("OOPS! Are you sure you know what you are doing with this method? Please pass non-null values.");
            return;
        }

        if (_isN) {
            isCheckingImpedanceN[channel] = false;
        } else {
            isCheckingImpedanceP[channel] = false;
        }
    }

    @Override
    public ADS1299Settings getADS1299Settings() {
        return currentADS1299Settings;
    }

    @Override
    public char getChannelSelector(int channel) {
        return channelSelectForSettings[channel];
    }

    public CytonBoardMode getBoardMode() {
        return currentBoardMode;
    }

    private void setBoardMode(CytonBoardMode boardMode) {
        sendCommand("/" + boardMode.getValue());
        currentBoardMode = boardMode;
    }

    @Override
    public void startStreaming() {
        openSDFile();
        super.startStreaming();
    }

    @Override
    public void stopStreaming() {
        closeSDFile();
        super.stopStreaming();
    }

    public void openSDFile() {
        //If selected, send command to Cyton to enabled SD file recording for selected duration
        if (cyton_sdSetting != CytonSDMode.NO_WRITE) {
            println("Opening SD file. Writing " + cyton_sdSetting.getCommand() + " to Cyton.");
            sendCommand(cyton_sdSetting.getCommand());
        }
    }

    public void closeSDFile() {
        if (cyton_sdSetting != CytonSDMode.NO_WRITE) {
            println("Closing any open SD file. Writing 'j' to Cyton.");
            sendCommand("j"); // tell the SD file to close if one is open...
        }
    }

    public void printRegisters() {
        println("Cyton: printRegisters(): Writing ? to OpenBCI...");
        sendCommand("?");
    }

    @Override
    protected void addChannelNamesInternal(String[] channelNames) {
        for (int i = 0; i < getAccelerometerChannels().length; i++) {
            channelNames[getAccelerometerChannels()[i]] = "Accel Channel " + i;
        }
        for (int i = 0; i < getAnalogChannels().length; i++) {
            channelNames[getAnalogChannels()[i]] = "Analog Channel " + i;
        }

        channelNames[getDigitalChannels()[0]] = "Digital Channel 0 (D11)";
        channelNames[getDigitalChannels()[1]] = "Digital Channel 1 (D12)";
        channelNames[getDigitalChannels()[2]] = "Digital Channel 2 (D13)";
        channelNames[getDigitalChannels()[3]] = "Digital Channel 3 (D17)";
        channelNames[getDigitalChannels()[4]] = "Digital Channel 4 (D18)";

        channelNames[getOtherChannels()[0]] = "Not Used";
        channelNames[getOtherChannels()[5]] = "Not Used";

        channelNames[getMarkerChannel()] = "Marker Channel";
    }

    @Override
    public double getGain(int channel) {
        return getADS1299Settings().values.gain[channel].getScalar();
    }

    @Override
    public List<double[]> getDataWithAccel(int maxSamples) {
        return getData(maxSamples);
    }

    @Override
    public List<double[]> getDataWithAnalog(int maxSamples) {
        return getData(maxSamples);
    }

    @Override
    public List<double[]> getDataWithDigital(int maxSamples) {
        return getData(maxSamples);
    }
};
