package W_PacketLoss;

public enum CalculationWindowSize
{
    SECONDS1("1 sec", 1*1000),
    SECONDS10("10 sec", 10*1000),
    MINUTE1("1 min", 60*1000);

    private String name;
    private int milliseconds;

    CalculationWindowSize(String _name, int _millis) {
        this.name = _name;
        this.milliseconds = _millis;
    }

    public String  getName() {
        return name;
    }

    public int getMilliseconds() {
        return milliseconds;
    }
}