package FocusEnums_;

import Widget_.IndexingInterface;

import java.util.ArrayList;
import java.util.List;

public enum FocusThreshold implements IndexingInterface
{
    FIVE_TENTHS (0, .5F, "0.5"),
    SIX_TENTHS (1, .6F, "0.6"),
    SEVEN_TENTHS (2, .7F, "0.7"),
    EIGHT_TENTHS (3, .8F, "0.8"),
    NINE_TENTHS (4, .9F, "0.9");

    private int index;
    private float value;
    private String label;

    private static FocusThreshold[] vals = values();

    FocusThreshold(int _index, float _value, String _label) {
        this.index = _index;
        this.value = _value;
        this.label = _label;
    }

    public float getValue() {
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