package ADS1299SettingsController_;

import ADS1299SettingsBoard_.ADS1299SettingsBoard;

import java.io.File;

import static Debugging_.GF.*;
import static GUI.GGVI.currentBoard;
import static GUI.GGVI.nchan;
import static WidgetManager_.GVI.w_timeSeries;

public class GF {

    public static void loadHardwareSettings(File selection) {
        if (selection == null) {
            output("Hardware Settings file not selected.");
        } else {
            if (currentBoard instanceof ADS1299SettingsBoard) {
                if (((ADS1299SettingsBoard)currentBoard).getADS1299Settings().loadSettingsValues(selection.getAbsolutePath())) {
                    outputSuccess("Hardware Settings Loaded!");
                    for (int i = 0; i < nchan; i++) {
                        w_timeSeries.adsSettingsController.updateChanSettingsDropdowns(i, currentBoard.isEXGChannelActive(i));
                        w_timeSeries.adsSettingsController.updateHasUnappliedSettings(i);
                    }
                } else {
                    outputError("Failed to load Hardware Settings.");
                }
            }
        }
    }

    public static void storeHardwareSettings(File selection) {
        if (selection == null) {
            output("Hardware Settings file not selected.");
        } else {
            if (currentBoard instanceof ADS1299SettingsBoard) {
                if (((ADS1299SettingsBoard)currentBoard).getADS1299Settings().saveToFile(selection.getAbsolutePath())) {
                    outputSuccess("Hardware Settings Saved!");
                } else {
                    outputError("Failed to save Hardware Settings.");
                }
            }
        }
    }
}
