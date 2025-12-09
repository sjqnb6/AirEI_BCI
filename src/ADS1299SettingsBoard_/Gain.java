package ADS1299SettingsBoard_;

// the scalar values are actually used to scale eeg data
public enum Gain implements ADSSettingsEnum {
    X1("x1", 1.0),
    X2("x2", 2.0),
    X4("x4", 4.0),
    X6("x6", 6.0),
    X8("x8", 8.0),
    X12("x12", 12.0),
    X24("x24", 24.0);

    private String name;
    private double scalar;

    Gain(String _name, double _scalar) {
        this.name = _name;
        this.scalar = _scalar;

    }

    @Override
    public String getName() {
        return name;
    }

    public double getScalar() {
        return scalar;
    }
}
