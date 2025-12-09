package GuiSettings_;

public enum ExpertModeEnum implements GuiSettingsEnum {
    ON("Active", true),
    OFF("Inactive", false);

    private String name;
    private boolean val;

    ExpertModeEnum(String _name, boolean _val) {
        this.name = _name;
        this.val = _val;
    }

    @Override
    public String getName() {
        return name;
    }

    public boolean getBooleanValue() {
        return val;
    }
}