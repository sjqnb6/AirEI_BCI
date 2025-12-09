package CytonImpedanceEnums_;

import Widget_.IndexingInterface;

import java.util.ArrayList;
import java.util.List;

public enum CytonImpedanceInterval implements IndexingInterface
{
    FOUR (0, 4000, "4 sec"),
    FIVE (1, 5000, "5 sec"),
    SEVEN (2, 7000, "7 sec"),
    TEN (3, 10000, "10 sec")
    ;

    private int index;
    private int value;
    private String label;
    private boolean boolean_value;
    private static CytonImpedanceInterval[] vals = values();

    CytonImpedanceInterval(int _index, int _val, String _label) {
        this.index = _index;
        this.value = _val;
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

    public int getValue() {
        return value;
    }

    public static List<String> getEnumStringsAsList() {
        List<String> enumStrings = new ArrayList<String>();
        for (IndexingInterface val : vals) {
            enumStrings.add(val.getString());
        }
        return enumStrings;
    }
}