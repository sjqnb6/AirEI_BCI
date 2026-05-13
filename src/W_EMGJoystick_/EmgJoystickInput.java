package W_EMGJoystick_;

import Widget_.IndexingInterface;

import java.util.ArrayList;
import java.util.List;

public enum EmgJoystickInput implements IndexingInterface
{
    CHANNEL_1 (0, "通道 1", 0),
    CHANNEL_2 (1, "通道 2", 1),
    CHANNEL_3 (2, "通道 3", 2),
    CHANNEL_4 (3, "通道 4", 3),
    CHANNEL_5 (4, "通道 5", 4),
    CHANNEL_6 (5, "通道 6", 5),
    CHANNEL_7 (6, "通道 7", 6),
    CHANNEL_8 (7, "通道 8", 7),
    CHANNEL_9 (8, "通道 9", 8),
    CHANNEL_10 (9, "通道 10", 9),
    CHANNEL_11 (10, "通道 11", 10),
    CHANNEL_12 (11, "通道 12", 11),
    CHANNEL_13 (12, "通道 13", 12),
    CHANNEL_14 (13, "通道 14", 13),
    CHANNEL_15 (14, "通道 15", 14),
    CHANNEL_16 (15, "通道 16", 15);

    private int index;
    private String name;
    private int value;
    private static EmgJoystickInput[] vals = values();

    EmgJoystickInput(int index, String name, int value) {
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

    private static List<String> getEnumStringsAsList() {
        List<String> enumStrings = new ArrayList<String>();
        for (IndexingInterface val : vals) {
            enumStrings.add(val.getString());
        }
        return enumStrings;
    }
}