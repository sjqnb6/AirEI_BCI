package FilterEnums_;

public enum FilterChannelSelect implements FilterSettingsEnum
{
    ALL_CHANNELS (0, "All Channels"),
    CUSTOM_CHANNELS (1, "Per Channel");

    private int index;
    private String name;

    FilterChannelSelect(int index, String name) {
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