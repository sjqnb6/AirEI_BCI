package FilterEnums_;

public enum BrainFlowFilterOrder implements FilterSettingsEnum {
    TWO (0, "2", 2),
    THREE (1, "3", 3),
    FOUR (2, "4", 4);

    private int index;
    private String name;
    private int value;

    BrainFlowFilterOrder(int index, String name, int value) {
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
