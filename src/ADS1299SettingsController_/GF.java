package ADS1299SettingsController_;

import ADS1299SettingsBoard_.ADS1299SettingsBoard;

import java.io.File;

import static Debugging_.GF.*;
import static WidgetManager_.GVI.w_timeSeries;
import Globel.GUI;
public class GF {

    public static void loadHardwareSettings(GUI MAIN, File selection) {
        if (selection == null) {
            output("Hardware Settings file not selected.");
        } else {
            if (MAIN.currentBoard instanceof ADS1299SettingsBoard) {
                if (((ADS1299SettingsBoard)MAIN.currentBoard).getADS1299Settings().loadSettingsValues(selection.getAbsolutePath())) {
                    outputSuccess("Hardware Settings Loaded!");
                    for (int i = 0; i < MAIN.nchan; i++) {
                        w_timeSeries.adsSettingsController.updateChanSettingsDropdowns(i, MAIN.currentBoard.isEXGChannelActive(i));
                        w_timeSeries.adsSettingsController.updateHasUnappliedSettings(i);
                    }
                } else {
                    outputError("Failed to load Hardware Settings.");
                }
            }
        }
    }

    public static void storeHardwareSettings(GUI MAIN, File selection) {
        if (selection == null) {
            output("硬件设置文件未被选中。");
        } else {
            if (MAIN.currentBoard instanceof ADS1299SettingsBoard) {
                if (((ADS1299SettingsBoard)MAIN.currentBoard).getADS1299Settings().saveToFile(selection.getAbsolutePath())) {
                    outputSuccess("硬件设置已保存！");
                } else {
                    outputError("未能保存硬件设置。");
                }
            }
        }
    }
}
