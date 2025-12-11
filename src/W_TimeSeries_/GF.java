package W_TimeSeries_;

import AccelerometerCapableBoard_.AccelerometerCapableBoard;
import AnalogCapableBoard_.AnalogCapableBoard;
import static WidgetManager_.GVI.*;
import Globel.GUI;
public class GF {

    //These functions are activated when an item from the corresponding dropdown is selected
    void VertScale_TS(int n) {
        w_timeSeries.setTSVertScale(n);
    }

    //triggered when there is an event in the Duration Dropdown
    void Duration(GUI MAIN, int n) {
        w_timeSeries.setTSHorizScale(n);

        int newDuration = w_timeSeries.getTSHorizScale().getValue();
        //If selected by user, sync the duration of Time Series, Accelerometer, and Analog Read(Cyton Only)
        if (MAIN.currentBoard instanceof AccelerometerCapableBoard) {
            if (MAIN.settings.accHorizScaleSave == 0) {
                //set accelerometer x axis to the duration selected from dropdown
                w_accelerometer.accelerometerBar.adjustTimeAxis(newDuration);
            }
        }
        if (MAIN.currentBoard instanceof AnalogCapableBoard) {
            if (MAIN.settings.arHorizScaleSave == 0) {
                //set analog read x axis to the duration selected from dropdown
                for(int i = 0; i < w_analogRead.numAnalogReadBars; i++) {
                    w_analogRead.analogReadBars[i].adjustTimeAxis(newDuration);
                }
            }
        }
    }

}
