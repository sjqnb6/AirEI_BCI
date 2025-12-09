package FilterEnums_;

public enum GlobalEnvironmentalFilter implements FilterSettingsEnum {
    FIFTY (0, "50 Hz"),
    SIXTY (1, "60 Hz"),
    FIFTY_AND_SIXTY (2, "50 + 60 Hz"),
    NONE (3, "None");

    private int index;
    private String name;

    GlobalEnvironmentalFilter(int index, String name) {
        this.index = index;
        this.name = name;
    }

    public int getIndex() {
        return index;
    }

    public String getString() {
        return name;
    }
}