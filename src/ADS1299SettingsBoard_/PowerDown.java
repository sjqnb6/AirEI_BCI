package ADS1299SettingsBoard_;

public enum PowerDown implements ADSSettingsEnum {
    ON("Active"),
    OFF("Inactive");

    private String name;

    PowerDown(String _name) {
        this.name = _name;
    }

    @Override
    public String getName() {
        return name;
    }
}
