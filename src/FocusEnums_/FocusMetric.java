package FocusEnums_;

import Widget_.IndexingInterface;
import brainflow.BrainFlowMetrics;

import java.util.ArrayList;
import java.util.List;

public enum FocusMetric implements IndexingInterface
{
    CONCENTRATION (0, "专注", BrainFlowMetrics.MINDFULNESS, "Concentrating"),
    RELAXATION (1, "放松", BrainFlowMetrics.RESTFULNESS, "Relaxing");

    private int index;
    private String label;
    private BrainFlowMetrics metric;
    private String idealState;
    private static FocusMetric[] vals = values();

    FocusMetric(int _index, String _label, BrainFlowMetrics _metric, String _idealState) {
        this.index = _index;
        this.label = _label;
        this.metric = _metric;
        this.idealState = _idealState;
    }

    @Override
    public String getString() {
        return label;
    }

    @Override
    public int getIndex() {
        return index;
    }

    public BrainFlowMetrics getMetric() {
        return metric;
    }

    public String getIdealStateString() {
        return idealState;
    }

    public static List<String> getEnumStringsAsList() {
        List<String> enumStrings = new ArrayList<String>();
        for (IndexingInterface val : vals) {
            enumStrings.add(val.getString());
        }
        return enumStrings;
    }
}
