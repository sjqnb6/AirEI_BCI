package W_HeadPlot_;

import Globel.GUI;

import static GUI.GUIManager.*;
import static WidgetManager_.GVI.w_headPlot;
import static processing.core.PApplet.max;

public class GF {

    //triggered when there is an event in the Polarity Dropdown
    public static void Polarity(GUI MAIN, int n) {

        if (n==0) {
            w_headPlot.headPlot.use_polarity = true;
        } else {
            w_headPlot.headPlot.use_polarity = false;
        }
        MAIN.settings.hpPolaritySave = n;
    }

    public static void ShowContours(GUI MAIN, int n){
        if(n==0){
            //turn headplot contours on
            w_headPlot.headPlot.drawHeadAsContours = true;
        } else if(n==1){
            //turn headplot contours off
            w_headPlot.headPlot.drawHeadAsContours = false;
        }
        MAIN.settings.hpContoursSave = n;
    }

    //triggered when there is an event in the SmoothingHeadPlot Dropdown
    public static void SmoothingHeadPlot(GUI MAIN, int n) {
        w_headPlot.setSmoothFac(smoothFac[n]);
        MAIN.settings.hpSmoothingSave = n;
    }

    public static void Intensity(GUI MAIN, int n){
        vertScaleFactor_ind = n;
        updateVertScale();
        MAIN.settings.hpIntensitySave = n;
    }


    public static void setVertScaleFactor_ind(int ind) {
        vertScaleFactor_ind = max(0,ind);
        if (ind >= vertScaleFactor.length) vertScaleFactor_ind = 0;
        updateVertScale();
    }

    public static void updateVertScale() {
        vertScale_uV = default_vertScale_uV * vertScaleFactor[vertScaleFactor_ind];
        w_headPlot.headPlot.setMaxIntensity_uV(vertScale_uV);
    }

    public static void doHardCalcs() {
        if (!w_headPlot.headPlot.threadLock) {
            w_headPlot.headPlot.threadLock = true;
            w_headPlot.headPlot.setPositionSize(w_headPlot.headPlot.hp_x, w_headPlot.headPlot.hp_y, w_headPlot.headPlot.hp_w, w_headPlot.headPlot.hp_h, w_headPlot.headPlot.hp_win_x, w_headPlot.headPlot.hp_win_y);
            w_headPlot.headPlot.hardCalcsDone = true;
            w_headPlot.headPlot.threadLock = false;
        }
    }

}
