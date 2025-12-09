package W_Focus_;

import static WidgetManager_.GVI.w_focus;

public class GF {

    //The following global functions are used by the Focus widget dropdowns. This method is the least amount of code.
    public void focusWindowDropdown(int n) {
        w_focus.setFocusHorizScale(n);
    }

    public void focusMetricDropdown(int n) {
        w_focus.setMetric(n);
    }

    public void focusClassifierDropdown(int n) {
        w_focus.setClassifier(n);
    }

    public void focusThresholdDropdown(int n) {
        w_focus.setThreshold(n);
    }

}
