package FilterEnums_;

import brainflow.FilterTypes;

public enum BrainFlowFilterType implements FilterSettingsEnum {
    BUTTERWORTH (0, "Butterworth", FilterTypes.BUTTERWORTH.get_code()),
    CHEBYSHEV (1, "Chebyshev", FilterTypes.CHEBYSHEV_TYPE_1.get_code()),
    BESSEL (2, "Bessel", FilterTypes.BESSEL.get_code());

    private int index;
    private String name;
    private int value;

    BrainFlowFilterType(int index, String name, int value) {
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
