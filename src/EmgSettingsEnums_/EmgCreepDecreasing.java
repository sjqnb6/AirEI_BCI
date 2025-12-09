package EmgSettingsEnums_;

public enum EmgCreepDecreasing implements EmgSettingsEnum
{
    POINT_9 (0, "0.9", .9f),
    POINT_95 (1, "0.95", .95f),
    POINT_98 (2, "0.98", .98f),
    POINT_99 (3, "0.99", .99f),
    POINT_999 (4, "0.999", .999f),
    POINT_9999 (5, "0.9999", .9999f),
    POINT_99999 (6, "0.99999", .99999f);

    private int index;
    private String name;
    private float value;

    EmgCreepDecreasing(int index, String name, float value) {
        this.index = index;
        this.name = name;
        this.value = value;
    }

    public int getIndex() {
        return index;
    }

    public String getString() {
        return name;
    }

    public float getValue() {
        return value;
    }
}
