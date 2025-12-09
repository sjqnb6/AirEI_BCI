package W_Marker_;

import Widget_.IndexingInterface;

import java.util.ArrayList;
import java.util.List;

//Enum for the Marker Vertical Scale in W_Marker class
public enum MarkerVertScale implements IndexingInterface
{
    AUTO (0, -1, "Auto"),
    TWO (1, 2, "2"),
    FOUR (2, 4, "4"),
    EIGHT (3, 8, "8"),
    TEN (4, 10, "10"),
    TWENTY (6, 20, "20");

    private int index;
    private int value;
    private String label;
    private static MarkerVertScale[] vals = values();

    MarkerVertScale(int _index, int _value, String _label) {
        this.index = _index;
        this.value = _value;
        this.label = _label;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String getString() {
        return label;
    }

    @Override
    public int getIndex() {
        return index;
    }

    public static List<String> getEnumStringsAsList() {
        List<String> enumStrings = new ArrayList<String>();
        for (IndexingInterface val : vals) {
            enumStrings.add(val.getString());
        }
        return enumStrings;
    }
}