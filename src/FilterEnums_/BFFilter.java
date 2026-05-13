package FilterEnums_;

public enum BFFilter implements FilterSettingsEnum
{
    BANDSTOP (0, "带阻滤波"),
    BANDPASS (1, "带通滤波");

    private int index;
    private String name;

    BFFilter(int index, String name) {
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
