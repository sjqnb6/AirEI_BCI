package EmgSettingsEnums_;

public enum EmgWindow implements EmgSettingsEnum
{
    ONE_HUNDREDTH_SECOND (0, "0.01 s", .01f),
    ONE_TENTH_SECOND (1, "0.1 s", .1f),
    FIFTEEN_HUNDREDTHS_SECOND (2, "0.15 s", .15f),
    QUARTER_SECOND (3, "0.25 s", .25f),
    HALF_SECOND (4, "0.5 s", .5f),
    THREE_QUARTERS_SECOND (5, "0.75 s", .75f),
    ONE_SECOND (6, "1.0 s", 1f),
    TWO_SECONDS (7, "2.0 s", 2f);

    private int index;
    private String name;
    private float value;

    EmgWindow(int index, String name, float value) {
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
