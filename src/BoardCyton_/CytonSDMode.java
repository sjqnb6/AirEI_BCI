package BoardCyton_;

public enum CytonSDMode {
    NO_WRITE("不写入SD...", null),
    MAX_5MIN("最长5分钟", "A"),
    MAX_15MIN("最长15分钟", "S"),
    MAX_30MIN("最长30分钟", "F"),
    MAX_1HR("最长1小时", "G"),
    MAX_2HR("最长2小时", "H"),
    MAX_4HR("最长4小时", "J"),
    MAX_12HR("最长12小时", "K"),
    MAX_24HR("最长24小时", "L");

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
