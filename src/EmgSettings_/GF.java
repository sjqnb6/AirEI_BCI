package EmgSettings_;

import DataProcessing_.DataProcessing;

import java.io.File;

import static Debugging_.GF.*;

public class GF {

    //Used by button in the EMG UI. Must be global and public. Called in above loadSettings method.
    public void loadEmgSettings(File selection, DataProcessing dataProcessing) {
        if (selection == null) {
            output("EMG Settings file not selected.");
        } else {
            if (dataProcessing.emgSettings.loadSettingsValues(selection.getAbsolutePath())) {
                outputSuccess("EMG Settings Loaded!");
                dataProcessing.emgSettings.setSettingsWereLoaded(true);
            }
        }
    }

    //Used by button in the EMG UI. Must be global and public. Called in above storeSettings method.
    public void storeEmgSettings(File selection, DataProcessing dataProcessing) {
        if (selection == null) {
            output("EMG Settings file not selected.");
        } else {
            if (dataProcessing.emgSettings.saveToFile(selection.getAbsolutePath())) {
                outputSuccess("EMG Settings Saved!");
            } else {
                outputError("Failed to save EMG Settings.");
            }
        }
    }
}
