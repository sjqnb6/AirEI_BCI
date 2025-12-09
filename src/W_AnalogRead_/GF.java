package W_AnalogRead_;

import static GUI.GGVI.settings;
import static WidgetManager_.GVI.w_analogRead;
import static WidgetManager_.GVI.w_timeSeries;

public class GF {

    //These functions need to be global! These functions are activated when an item from the corresponding dropdown is selected
    public static void VertScale_AR(int n) {
        settings.arVertScaleSave = n;
        for(int i = 0; i < w_analogRead.numAnalogReadBars; i++) {
            w_analogRead.analogReadBars[i].adjustVertScale(w_analogRead.yLimOptions[n]);
        }
    }

    //triggered when there is an event in the LogLin Dropdown
    public static void Duration_AR(int n) {
        // println("adjust duration to: " + w_analogRead.analogReadBars[i].adjustTimeAxis(n));
        //set analog read x axis to the duration selected from dropdown
        settings.arHorizScaleSave = n;

        //Sync the duration of Time Series, Accelerometer, and Analog Read(Cyton Only)
        for(int i = 0; i < w_analogRead.numAnalogReadBars; i++) {
            if (n == 0) {
                w_analogRead.analogReadBars[i].adjustTimeAxis(w_timeSeries.getTSHorizScale().getValue());
            } else {
                w_analogRead.analogReadBars[i].adjustTimeAxis(w_analogRead.xLimOptions[n]);
            }
        }
    }


}
