package W_EMGJoystick_;

import Widget_.IndexingInterface;

import java.util.ArrayList;
import java.util.List;

public enum EmgJoystickSmoothing implements IndexingInterface
{
    OFF (0, "Off", 0f),
    POINT_9 (1, "0.9", .9f),
    POINT_95 (2, "0.95", .95f),
    POINT_98 (3, "0.98", .98f),
    POINT_99 (4, "0.99", .99f),
    POINT_999 (5, "0.999", .999f),
    POINT_9999 (6, "0.9999", .9999f);

    private int index;
    private String name;
    private float value;
    private static EmgJoystickSmoothing[] vals = values();

    EmgJoystickSmoothing(int index, String name, float value) {
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

    public float getValue() {
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