package EmgSettingsEnums_;

public enum EmgMinimumDeltaUV implements EmgSettingsEnum
{
    TWO_UV (0, "2 uV", 2),
    FOUR_UV (1, "4 uV", 4),
    SIX_UV (2, "6 uV", 6),
    EIGHT_UV (3, "8 uV", 8),
    TEN_UV (4, "10 uV", 10),
    TWENTY_UV (5, "20 uV", 20),
    FORTY_UV (6, "40 uV", 40),
    EIGHTY_UV (7, "80 uV", 80);

    private int index;
    private String name;
    private int value;

    EmgMinimumDeltaUV(int index, String name, int value) {
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

    public int getValue() {
        return value;
    }
}