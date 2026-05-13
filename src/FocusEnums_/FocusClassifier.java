package FocusEnums_;

import Widget_.IndexingInterface;
import brainflow.BrainFlowClassifiers;

import java.util.ArrayList;
import java.util.List;

public enum FocusClassifier implements IndexingInterface
{
    REGRESSION (0, "回归模型", BrainFlowClassifiers.DEFAULT_CLASSIFIER);

    private int index;
    private int value;
    private String label;
    private BrainFlowClassifiers classifier;

    private static FocusClassifier[] vals = values();

    FocusClassifier(int _index, String _label, BrainFlowClassifiers _classifier) {
        this.index = _index;
        this.label = _label;
        this.classifier = _classifier;
    }

    @Override
    public String getString() {
        return label;
    }

    @Override
    public int getIndex() {
        return index;
    }

    public BrainFlowClassifiers getClassifier() {
        return classifier;
    }

    public static List<String> getEnumStringsAsList() {
        List<String> enumStrings = new ArrayList<String>();
        for (IndexingInterface val : vals) {
            enumStrings.add(val.getString());
        }
        return enumStrings;
    }
}