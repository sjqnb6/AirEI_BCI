package W_TimeSeries_;

////////////////////////////////////////////////////
//
// This class creates a Time Series Plot separate from the old Gui_Manager
// It extends the Widget class
//
// Conor Russomanno, November 2016
//
// Requires the plotting library from grafica ... replacing the old gwoptics (which is now no longer supported)
//
///////////////////////////////////////////////////

import Widget_.IndexingInterface;

import java.util.ArrayList;
import java.util.List;

public enum TimeSeriesXLim implements IndexingInterface
{
    ONE (0, 1, "1 sec"),
    THREE (1, 3, "3 sec"),
    FIVE (2, 5, "5 sec"),
    TEN (3, 10, "10 sec"),
    TWENTY (4, 20, "20 sec");

    private int index;
    private int value;
    private String label;
    private static TimeSeriesXLim[] vals = values();

    TimeSeriesXLim(int _index, int _value, String _label) {
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