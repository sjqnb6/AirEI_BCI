package SessionSettings_;

import Globel.GUI;

import java.io.File;

import static Debugging_.GF.outputError;
import static Debugging_.GF.outputSuccess;
import static WidgetManager_.GVI.*;
import static processing.core.PApplet.println;

public class GF {

    //////////////////////////////////////////
//  Global Functions                    //
// Called by Buttons with the same name //
    //////////////////////////////////////////
// Select file to save custom settings using dropdown in TopNav.pde
    public static void saveConfigFile(GUI MAIN, File selection) {
        if (selection == null) {
            println("SessionSettings: saveConfigFile: Window was closed or the user hit cancel.");
        } else {
            println("SessionSettings: saveConfigFile: User selected " + selection.getAbsolutePath());
            MAIN.settings.saveDialogName = selection.getAbsolutePath();
            MAIN.settings.save(MAIN.settings.saveDialogName); //save current settings to JSON file in SavedData
            outputSuccess("Settings Saved! Using Expert Mode, you can load these settings using 'N' key. Click \"Default\" to revert to factory settings."); //print success message to screen
            MAIN.settings.saveDialogName = null; //reset this variable for future use
        }
    }
    // Select file to load custom settings using dropdown in TopNav.pde
    public static void loadConfigFile(GUI MAIN, File selection) {
        if (selection == null) {
            println("SessionSettings: loadConfigFile: Window was closed or the user hit cancel.");
        } else {
            println("SessionSettings: loadConfigFile: User selected " + selection.getAbsolutePath());
            //output("You have selected \"" + selection.getAbsolutePath() + "\" to Load custom settings.");
            MAIN.settings.loadDialogName = selection.getAbsolutePath();
            try {
                MAIN.settings.load(MAIN.settings.loadDialogName); //load settings from JSON file in /data/
                //Output success message when Loading settings is complete without errors
                if (MAIN.settings.chanNumError == false
                        && MAIN.settings.dataSourceError == false
                        && MAIN.settings.loadErrorCytonEvent == false) {
                    outputSuccess("Settings Loaded!");
                }
            } catch (Exception e) {
                println("SessionSettings: Incompatible settings file or other error");
                if (MAIN.settings.chanNumError == true) {
                    outputError("Settings Error:  Channel Number Mismatch Detected");
                } else if (MAIN.settings.dataSourceError == true) {
                    outputError("Settings Error: Data Source Mismatch Detected");
                } else {
                    outputError("Error trying to load settings file, possibly from previous GUI. Removing old settings.");
                    if (selection.exists()) selection.delete();
                }
            }
            MAIN.settings.loadDialogName = null; //reset this variable for future use
        }
    }

    //These functions need to be global! These functions are activated when an item from the corresponding dropdown is selected
//triggered when there is an event in the MaxFreq. Dropdown
    public static void MaxFreq(GUI MAIN, int n) {
        /* request the selected item based on index n */
        w_fft.fft_plot.setXLim(0.1F, w_fft.xLimOptions[n]); //update the xLim of the FFT_Plot
        MAIN.settings.fftMaxFrqSave = n; //save the xLim to variable for save/load settings
    }

    //triggered when there is an event in the VertScale Dropdown
    public static void VertScale(GUI MAIN, int n) {

        w_fft.fft_plot.setYLim(0.1F, w_fft.yLimOptions[n]); //update the yLim of the FFT_Plot
        MAIN.settings.fftMaxuVSave = n; //save the yLim to variable for save/load settings
    }

    //triggered when there is an event in the LogLin Dropdown
    public static void LogLin(GUI MAIN, int n) {
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
    public static void Smoothing(GUI MAIN, int n) {
        W_HeadPlot_.GVI.smoothFac_ind = n;
        MAIN.settings.fftSmoothingSave = n;
        //since this function is called by both the BandPower and FFT Widgets the dropdown needs to be updated in both
        w_fft.cp5_widget.getController("Smoothing").getCaptionLabel().setText(MAIN.settings.fftSmoothingArray[n]);
        w_bandPower.cp5_widget.getController("Smoothing").getCaptionLabel().setText(MAIN.settings.fftSmoothingArray[n]);

    }

    //triggered when there is an event in the UnfiltFilt Dropdown
    public static void UnfiltFilt(GUI MAIN, int n) {
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

    //These functions are activated when an item from the corresponding dropdown is selected
    public static void accelVertScale(GUI MAIN, int n) {
        MAIN.settings.accVertScaleSave = n;
        w_accelerometer.accelerometerBar.adjustVertScale(w_accelerometer.yLimOptions[n]);
    }

    //triggered when there is an event in the Duration Dropdown
    public static void accelDuration(GUI MAIN, int n) {
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
