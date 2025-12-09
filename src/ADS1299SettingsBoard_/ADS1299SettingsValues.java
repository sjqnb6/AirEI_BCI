package ADS1299SettingsBoard_;

public class ADS1299SettingsValues {
    public PowerDown[] powerDown;
    public Gain[] gain;
    public InputType[] inputType;
    public Bias[] bias;
    public Srb2[] srb2;
    public Srb1[] srb1;

    //Used for Channel On/Off to reflect what happens in Firmware
    public Bias[] previousBias;
    public Srb2[] previousSrb2;
    public InputType[] previousInputType;

    public ADS1299SettingsValues() {
    }
}
