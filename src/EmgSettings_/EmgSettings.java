package EmgSettings_;

import EmgSettingsValues_.EmgSettingsValues;
import Globel.GUI;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import processing.core.PApplet;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import static Debugging_.GF.outputError;

public class EmgSettings {

    GUI MAIN;
    public EmgSettingsValues values;

    private int channelCount;

    private boolean settingsWereLoaded = false;

    public EmgSettings(GUI MAIN) {
        this.MAIN = MAIN;
        channelCount = MAIN.currentBoard.getNumEXGChannels();
        values = new EmgSettingsValues(MAIN);
    }

    public boolean loadSettingsValues(String filename) {
        try {
            File file = new File(filename);
            StringBuilder fileContents = new StringBuilder((int)file.length());
            Scanner scanner = new Scanner(file);
            while(scanner.hasNextLine()) {
                fileContents.append(scanner.nextLine() + System.lineSeparator());
            }
            Gson gson = new Gson();
            EmgSettingsValues tempValues = gson.fromJson(fileContents.toString(), EmgSettingsValues.class);
            if (tempValues.window.length != channelCount) {
                outputError("Emg Settings: Loaded EMG Settings file has different number of channels than the current board.");
                return false;
            }
            //Explicitely copy values over to avoid reference issues
            //(e.g. values = tempValues "nukes" the old values object)
            values.window = tempValues.window;
            values.uvLimit = tempValues.uvLimit;
            values.creepIncreasing = tempValues.creepIncreasing;
            values.creepDecreasing = tempValues.creepDecreasing;
            values.minimumDeltaUV = tempValues.minimumDeltaUV;
            values.lowerThresholdMinimum = tempValues.lowerThresholdMinimum;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            File f = new File(filename);
            if (f.exists()) {
                if (f.delete()) {
                    outputError("Emg Settings: Could not load EMG settings from disk. Deleting this file...");
                } else {
                    outputError("Emg Settings: Error deleting old/broken EMG settings file! Please make sure the GUI has proper read/write permissions.");
                }
            }
            return false;
        }
    }

    public String getJson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(values);
    }

    public boolean saveToFile(String filename) {
        String json = getJson();
        try {
            FileWriter writer = new FileWriter(filename);
            writer.write(json);
            writer.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void revertAllChannelsToDefaultValues() {
        values = new EmgSettingsValues(MAIN);
        settingsWereLoaded = true;
    }

    //Called in UI to control number of channels. This is set from the board when this class is instantiated.
    public int getChannelCount() {
        return channelCount;
    }

    //Avoid error with popup being in another thread.
    public void storeSettings() {
        StringBuilder settingsFilename = new StringBuilder(MAIN.directoryManager.getSettingsPath());
        settingsFilename.append("EmgSettings");
        settingsFilename.append("_");
        settingsFilename.append(getChannelCount());
        settingsFilename.append("Channels.json");
        String filename = settingsFilename.toString();
        File fileToSave = new File(filename);
        MAIN.selectOutput("Save EMG settings to file", "storeEmgSettings", fileToSave);
    }

    //Avoid error with popup being in another thread.
    public void loadSettings() {
        StringBuilder settingsFilename = new StringBuilder(MAIN.directoryManager.getSettingsPath());
        settingsFilename.append("EmgSettings");
        settingsFilename.append("_");
        settingsFilename.append(getChannelCount());
        settingsFilename.append("Channels.json");
        String filename = settingsFilename.toString();
        File fileToLoad = new File(filename);
        MAIN.selectInput("Select EMG settings file to load", "loadEmgSettings", fileToLoad);
    }

    public boolean getSettingsWereLoaded() {
        return settingsWereLoaded;
    }

    public void setSettingsWereLoaded(boolean settingsWereLoaded) {
        this.settingsWereLoaded = settingsWereLoaded;
    }
}
