package BoardCyton_;

public enum CytonSDMode {
    NO_WRITE("Do not write to SD...", null),
    MAX_5MIN("5 minute maximum", "A"),
    MAX_15MIN("15 minute maximum", "S"),
    MAX_30MIN("30 minute maximum", "F"),
    MAX_1HR("1 hour maximum", "G"),
    MAX_2HR("2 hour maximum", "H"),
    MAX_4HR("4 hour maximum", "J"),
    MAX_12HR("12 hour maximum", "K"),
    MAX_24HR("24 hour maximum", "L");

    private String name;
    private String command;

    CytonSDMode(String _name, String _command) {
        this.name = _name;
        this.command = _command;
    }

    public String getName() {
        return name;
    }

    public String getCommand() {
        return command;
    }
}
