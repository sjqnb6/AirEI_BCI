package BoardCyton_;

import ADS1299SettingsBoard_.*;
import Board_.Board;

import java.util.Arrays;

public class CytonDefaultSettings extends ADS1299Settings {
    public CytonDefaultSettings(Board theBoard) {
        super(theBoard);

        // the 'd' command is automatically sent by brainflow on prepare_session
        Arrays.fill(values.powerDown, PowerDown.ON);
        Arrays.fill(values.gain, Gain.X24);
        Arrays.fill(values.inputType, InputType.NORMAL);
        Arrays.fill(values.bias, Bias.INCLUDE);
        Arrays.fill(values.srb2, Srb2.CONNECT);
        Arrays.fill(values.srb1, Srb1.DISCONNECT);
    }
}
