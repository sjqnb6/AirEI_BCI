package CytonImpedanceEnums_;

import Widget_.IndexingInterface;

import java.util.ArrayList;
import java.util.List;

public enum CytonImpedanceLabels implements IndexingInterface
{
    ADS_CHANNEL (0, "Channel"),
    ANATOMICAL (1, "Anatomical")
    ;

    private int index;
    private String label;
    private boolean boolean_value;
    private static CytonImpedanceLabels[] vals = values();

    CytonImpedanceLabels(int _index, String _label) {
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

    public boolean getIsAnatomicalName() {
        return label.equals("Anatomical");
    }

    public static List<String> getEnumStringsAsList() {
        List<String> enumStrings = new ArrayList<String>();
        for (IndexingInterface val : vals) {
            enumStrings.add(val.getString());
        }
        return enumStrings;
    }
}