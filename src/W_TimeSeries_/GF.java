package W_TimeSeries_;

import AccelerometerCapableBoard_.AccelerometerCapableBoard;
import AnalogCapableBoard_.AnalogCapableBoard;

import static GUI.GGVI.currentBoard;
import static GUI.GGVI.settings;
import static WidgetManager_.GVI.*;

public class GF {

    //These functions are activated when an item from the corresponding dropdown is selected
    void VertScale_TS(int n) {
        w_timeSeries.setTSVertScale(n);
    }

    //triggered when there is an event in the Duration Dropdown
    void Duration(int n) {
        w_timeSeries.setTSHorizScale(n);

        int newDuration = w_timeSeries.getTSHorizScale().getValue();
        //If selected by user, sync the duration of Time Series, Accelerometer, and Analog Read(Cyton Only)
        if (currentBoard instanceof AccelerometerCapableBoard) {
            if (settings.accHorizScaleSave == 0) {
                //set accelerometer x axis to the duration selected from dropdown
                w_accelerometer.accelerometerBar.adjustTimeAxis(newDuration);
            }
        }
        if (currentBoard instanceof AnalogCapableBoard) {
            if (settings.arHorizScaleSave == 0) {
                //set analog read x axis to the duration selected from dropdown
                for(int i = 0; i < w_analogRead.numAnalogReadBars; i++) {
                    w_analogRead.analogReadBars[i].adjustTimeAxis(newDuration);
                }
            }
        }
    }

}
