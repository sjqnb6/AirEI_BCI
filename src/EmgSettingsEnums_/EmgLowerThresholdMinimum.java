package EmgSettingsEnums_;

public enum EmgLowerThresholdMinimum implements EmgSettingsEnum
{
    ZERO_UV (0, "0 uV", 0),
    TWO_UV (1, "2 uV", 2),
    FOUR_UV (2, "4 uV", 4),
    SIX_UV (3, "6 uV", 6),
    EIGHT_UV (4, "8 uV", 8),
    TEN_UV (5, "10 uV", 10),
    FIFTEEN_UV (6, "15 uV", 15),
    TWENTY_UV (7, "20 uV", 20),
    THIRTY_UV (8, "30 uV", 30),
    FORTY_UV (9, "40 uV", 40);

    private int index;
    private String name;
    private int value;

    EmgLowerThresholdMinimum(int index, String name, int value) {
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
