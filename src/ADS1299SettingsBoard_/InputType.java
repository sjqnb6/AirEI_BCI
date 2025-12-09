package ADS1299SettingsBoard_;

public enum InputType implements ADSSettingsEnum {
    NORMAL("Normal"),
    SHORTED("Shorted"),
    BIAS_MEAS("Bias Meas"),
    MVDD("MVDD"),
    TEMP("Temp"),
    TEST("Test"),
    BIAS_DRP("BIAS DRP"),
    BIAS_DRN("BIAS DRN");

    private String name;

    InputType(String _name) {
        this.name = _name;
    }

    @Override
    public String getName() {
        return name;
    }
}
