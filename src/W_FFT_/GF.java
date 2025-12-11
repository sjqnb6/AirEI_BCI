package W_FFT_;

import GUI.GUIManager;
import static WidgetManager_.GVI.w_bandPower;
import static WidgetManager_.GVI.w_fft;
import Globel.GUI;
public class GF extends GUIManager {
    GUI MAIN;
    //These functions need to be global! These functions are activated when an item from the corresponding dropdown is selected
//triggered when there is an event in the MaxFreq. Dropdown
    GF(GUI MAIN){
        this.MAIN = MAIN;
    }
    public void MaxFreq(int n) {
        /* request the selected item based on index n */
        w_fft.fft_plot.setXLim(0.1F, w_fft.xLimOptions[n]); //update the xLim of the FFT_Plot
        MAIN.settings.fftMaxFrqSave = n; //save the xLim to variable for save/load settings
    }

    //triggered when there is an event in the VertScale Dropdown
    public void VertScale(int n) {

        w_fft.fft_plot.setYLim(0.1F, w_fft.yLimOptions[n]); //update the yLim of the FFT_Plot
        MAIN.settings.fftMaxuVSave = n; //save the yLim to variable for save/load settings
    }

    //triggered when there is an event in the LogLin Dropdown
    public void LogLin(int n) {
        if (n==0) {
            w_fft.fft_plot.setLogScale("y");
            //store the current setting to save
            MAIN.settings.fftLogLinSave = 0;
        } else {
            w_fft.fft_plot.setLogScale("");
            //store the current setting to save
            MAIN.settings.fftLogLinSave = 1;
        }
    }

    //triggered when there is an event in the Smoothing Dropdown
    public void Smoothing(int n) {
        smoothFac_ind = n;
        MAIN.settings.fftSmoothingSave = n;
        //since this function is called by both the BandPower and FFT Widgets the dropdown needs to be updated in both
        w_fft.cp5_widget.getController("Smoothing").getCaptionLabel().setText(MAIN.settings.fftSmoothingArray[n]);
        w_bandPower.cp5_widget.getController("Smoothing").getCaptionLabel().setText(MAIN.settings.fftSmoothingArray[n]);

    }

    //triggered when there is an event in the UnfiltFilt Dropdown
    public void UnfiltFilt(int n) {
        MAIN.settings.fftFilterSave = n;
        if (n==0) {
            //have FFT use filtered data -- default
            MAIN.isFFTFiltered = true;
        } else {
            //have FFT use unfiltered data
            MAIN.isFFTFiltered = false;
        }
        //since this function is called by both the BandPower and FFT Widgets the dropdown needs to be updated in both
        w_fft.cp5_widget.getController("UnfiltFilt").getCaptionLabel().setText(MAIN.settings.fftFilterArray[n]);
        w_bandPower.cp5_widget.getController("UnfiltFilt").getCaptionLabel().setText(MAIN.settings.fftFilterArray[n]);
    }

}
