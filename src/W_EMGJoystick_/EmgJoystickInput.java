package W_EMGJoystick_;

import Widget_.IndexingInterface;

import java.util.ArrayList;
import java.util.List;

public enum EmgJoystickInput implements IndexingInterface
{
    CHANNEL_1 (0, "Channel 1", 0),
    CHANNEL_2 (1, "Channel 2", 1),
    CHANNEL_3 (2, "Channel 3", 2),
    CHANNEL_4 (3, "Channel 4", 3),
    CHANNEL_5 (4, "Channel 5", 4),
    CHANNEL_6 (5, "Channel 6", 5),
    CHANNEL_7 (6, "Channel 7", 6),
    CHANNEL_8 (7, "Channel 8", 7),
    CHANNEL_9 (8, "Channel 9", 8),
    CHANNEL_10 (9, "Channel 10", 9),
    CHANNEL_11 (10, "Channel 11", 10),
    CHANNEL_12 (11, "Channel 12", 11),
    CHANNEL_13 (12, "Channel 13", 12),
    CHANNEL_14 (13, "Channel 14", 13),
    CHANNEL_15 (14, "Channel 15", 14),
    CHANNEL_16 (15, "Channel 16", 15);

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