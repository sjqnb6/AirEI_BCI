package W_Accelerometer_;

import static GUI.GGVI.settings;
import static WidgetManager_.GVI.w_accelerometer;
import static WidgetManager_.GVI.w_timeSeries;

public class GF {

    //These functions are activated when an item from the corresponding dropdown is selected
    void accelVertScale(int n) {
        settings.accVertScaleSave = n;
        w_accelerometer.accelerometerBar.adjustVertScale(w_accelerometer.yLimOptions[n]);
    }

    //triggered when there is an event in the Duration Dropdown
    void accelDuration(int n) {
        settings.accHorizScaleSave = n;

        //Sync the duration of Time Series, Accelerometer, and Analog Read(Cyton Only)
        if (n == 0) {
            w_accelerometer.accelerometerBar.adjustTimeAxis(w_timeSeries.getTSHorizScale().getValue());
        } else {
            //set accelerometer x axis to the duration selected from dropdown
            w_accelerometer.accelerometerBar.adjustTimeAxis(w_accelerometer.xLimOptions[n]);
        }
    }

}
