package BoardCyton_;

public enum CytonBoardMode {
    DEFAULT(0),
    DEBUG(1),
    ANALOG(2),
    DIGITAL(3),
    MARKER(4);

    private final int value;
    CytonBoardMode(final int newValue) {
        value = newValue;
    }
    public int getValue() { return value; }
}
