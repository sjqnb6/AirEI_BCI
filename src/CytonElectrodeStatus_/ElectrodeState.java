package CytonElectrodeStatus_;

public enum ElectrodeState {
    GREYED_OUT(0, 0x717577),
    RED(1, 0xFF0000),
    YELLOW(2, 0xE6C700),
    GREEN(3, 0x00FF64),
    BLUE(4, 0x416080),
    NOT_TESTABLE(5, 0x717577); // 注意这里避免重复值，如果有意共享颜色可以复用字段而非 value

    private final int value;
    private final int color;

    ElectrodeState(int value, int color) {
        this.value = value;
        this.color = color;
    }

    public int getValue() {
        return value;
    }

    public int getColor() {
        return color;
    }

    // 可选：通过 value 查找 ElectrodeState 枚举
    public static ElectrodeState fromValue(int value) {
        for (ElectrodeState state : values()) {
            if (state.getValue() == value) {
                return state;
            }
        }
        throw new IllegalArgumentException("Invalid ElectrodeState value: " + value);
    }
}

