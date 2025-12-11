package FilterSettings_;

import java.io.File;
import Globel.GUI;
import static Debugging_.GF.*;
import static Globel.GUI.filterSettings;

public class GF {

    //Used by button in the Filter UI. Must be global and public.
    public static void loadFilterSettings(File selection) {
        if (selection == null) {
            output("Filters Settings file not selected.");
        } else {
            if (filterSettings.loadSettingsValues(selection.getAbsolutePath())) {
                outputSuccess("Filter Settings Loaded!");
                GVI.filterSettingsWereLoadedFromFile = true;
            } else {
                outputError("Failed to load Filter Settings. The old/broken file has been deleted.");
            }
        }
    }

    //Used by button in the Filter UI. Must be global and public.
    public static void storeFilterSettings(File selection) {
        if (selection == null) {
            output("Filter Settings file not selected.");
        } else {
            if (filterSettings.saveToFile(selection.getAbsolutePath())) {
                outputSuccess("Filter Settings Saved!");
            } else {
                outputError("Failed to save Filter Settings.");
            }
        }
    }


}
