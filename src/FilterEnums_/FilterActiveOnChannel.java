package FilterEnums_;

public enum FilterActiveOnChannel implements FilterSettingsEnum {
    ON (0, "Active"),
    OFF (1, "Inactive");

    private int index;
    private String name;

    FilterActiveOnChannel(int index, String name) {
        this.index = index;
        this.name = name;
    }

    public int getIndex() {
        return index;
    }

    public String getString() {
        return name;
    }

    public boolean isActive() {
        return name.equals("Active");
    }
}