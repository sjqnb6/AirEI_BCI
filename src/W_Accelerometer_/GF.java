package W_Accelerometer_;
import static WidgetManager_.GVI.w_accelerometer;
import static WidgetManager_.GVI.w_timeSeries;
import Globel.GUI;
public class GF {

    //These functions are activated when an item from the corresponding dropdown is selected
    void accelVertScale(GUI MAIN, int n) {
        MAIN.settings.accVertScaleSave = n;
        w_accelerometer.accelerometerBar.adjustVertScale(w_accelerometer.yLimOptions[n]);
    }

    //triggered when there is an event in the Duration Dropdown
    void accelDuration(GUI MAIN, int n) {
        MAIN.settings.accHorizScaleSave = n;

        //Sync the duration of Time Series, Accelerometer, and Analog Read(Cyton Only)
        if (n == 0) {
            w_accelerometer.accelerometerBar.adjustTimeAxis(w_timeSeries.getTSHorizScale().getValue());
        } else {
            //set accelerometer x axis to the duration selected from dropdown
            w_accelerometer.accelerometerBar.adjustTimeAxis(w_accelerometer.xLimOptions[n]);
        }
    }

}
