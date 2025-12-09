package ADS1299SettingsBoard_;

public interface ADS1299SettingsBoard {

    // Interface methods
    public ADS1299Settings getADS1299Settings();
    public char getChannelSelector(int channel);
    public double getGain(int channel);
};
