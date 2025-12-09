package ADS1299SettingsBoard_;

public enum Bias implements ADSSettingsEnum {
    NO_INCLUDE("No"),
    INCLUDE("Yes");

    private String name;

    Bias(String _name) {
        this.name = _name;
    }

    @Override
    public String getName() {
        return name;
    }
}