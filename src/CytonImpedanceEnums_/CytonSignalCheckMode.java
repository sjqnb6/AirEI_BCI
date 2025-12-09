package CytonImpedanceEnums_;

import Widget_.IndexingInterface;

import java.util.ArrayList;
import java.util.List;

public enum CytonSignalCheckMode implements IndexingInterface
{
    LIVE (0, "Live"),
    IMPEDANCE (1, "Impedance");

    private int index;
    private String label;
    private static CytonSignalCheckMode[] vals = values();

    CytonSignalCheckMode(int _index, String _label) {
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

    public boolean getIsImpedanceMode() {
        return label.equals("Impedance");
    }

    public static List<String> getEnumStringsAsList() {
        List<String> enumStrings = new ArrayList<String>();
        for (IndexingInterface val : vals) {
            enumStrings.add(val.getString());
        }
        return enumStrings;
    }
}
