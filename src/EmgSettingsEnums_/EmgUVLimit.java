package EmgSettingsEnums_;

public enum EmgUVLimit implements EmgSettingsEnum
{
    FIFTY_UV (0, "50 uV", 50),
    ONE_HUNDRED_UV (1, "100 uV", 100),
    TWO_HUNDRED_UV (2, "200 uV", 200),
    FOUR_HUNDRED_UV (3, "400 uV", 400);

    private int index;
    private String name;
    private int value;

    EmgUVLimit(int index, String name, int value) {
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
