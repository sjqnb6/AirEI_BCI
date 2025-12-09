package W_Marker_;

import Widget_.IndexingInterface;

import java.util.ArrayList;
import java.util.List;

//Enum for the Marker Window in W_Marker class
public enum MarkerWindow implements IndexingInterface
{
    FIVE (0, 5, "5 sec"),
    TEN (1, 10, "10 sec"),
    TWENTY (2, 20, "20 sec");

    private int index;
    private int value;
    private String label;
    private static MarkerWindow[] vals = values();

    MarkerWindow(int _index, int _value, String _label) {
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