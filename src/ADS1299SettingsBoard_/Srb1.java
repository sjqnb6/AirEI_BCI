package ADS1299SettingsBoard_;

public enum Srb1 implements ADSSettingsEnum {
    DISCONNECT("Off"),
    CONNECT("On");

    private String name;

    Srb1(String _name) {
        this.name = _name;
    }

    @Override
    public String getName() {
        return name;
    }
}
