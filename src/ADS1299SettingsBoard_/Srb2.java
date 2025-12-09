package ADS1299SettingsBoard_;

public enum Srb2 implements ADSSettingsEnum {
    DISCONNECT("Off"),
    CONNECT("On");

    private String name;

    Srb2(String _name) {
        this.name = _name;
    }

    @Override
    public String getName() {
        return name;
    }
}
