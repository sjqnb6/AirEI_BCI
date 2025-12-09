package W_TimeSeries_;

import Widget_.IndexingInterface;

import java.util.ArrayList;
import java.util.List;

public enum TimeSeriesYLim implements IndexingInterface
{
    AUTO (0, 0, "Auto"),
    UV_50 (1, 50, "50 uV"),
    UV_100 (2, 100, "100 uV"),
    UV_200 (3, 200, "200 uV"),
    UV_400 (4, 400, "400 uV"),
    UV_1000 (5, 1000, "1000 uV"),
    UV_10000 (6, 10000, "10000 uV");

    private int index;
    private int value;
    private String label;
    private static TimeSeriesYLim[] vals = values();

    TimeSeriesYLim(int _index, int _value, String _label) {
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