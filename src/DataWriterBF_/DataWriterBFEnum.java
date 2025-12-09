package DataWriterBF_;

import Widget_.IndexingInterface;

public enum DataWriterBFEnum implements IndexingInterface
{
    DEFAULT (0, "Default"),
    CUSTOM (1, "Custom"),
    NONE (2, "None");

    private int index;
    private String label;

    DataWriterBFEnum(int _index, String _label) {
        this.index = _index;
        this.label = _label;
    }

    @Override
    public String getString() {
        return label;
    }

    @Override
    public int getIndex() {
        return index;
    }

    public boolean getIsDefaultLocation() {
        return label.equals("Default");
    }

    public boolean getIsCustomLocation() {
        return label.equals("Custom");
    }

    public boolean getIsTurnedOff() {
        return label.equals("None");
    }
}
