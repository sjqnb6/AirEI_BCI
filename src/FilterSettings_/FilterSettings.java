package FilterSettings_;

import DataSource_.DataSource;
import GUI.GUIManager;
import Globel.GUI;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import static GUI.GGVI.directoryManager;

public class FilterSettings{

    public FilterSettingsValues values;
    //public FilterSettingsValues previousValues;
    public FilterSettingsValues defaultValues;

    protected DataSource board;
    public int channelCount;

    private GUI MAIN;
    public FilterSettings(GUI MAIN, DataSource theBoard) {
        this.MAIN = MAIN;
        board = theBoard;
        channelCount = board.getNumEXGChannels();

        values = new FilterSettingsValues(channelCount);
        defaultValues = new FilterSettingsValues(channelCount);
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
            values = gson.fromJson(fileContents.toString(), FilterSettingsValues.class);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            File f = new File(filename);
            if (f.exists()) {
                if (f.delete()) {
                    MAIN.println("FilterSettings: Could not load filter settings from disk. Deleting this file...");
                } else {
                    MAIN.println("FilterSettings: Error deleting old/broken filter settings file! Please make sure the GUI has proper read/write permissions.");
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
        values = new FilterSettingsValues(channelCount);
    }

    //Called in UI to control number of channels. This is set from the board when this class is instantiated.
    public int getChannelCount() {
        return channelCount;
    }

    //Avoid error with popup being in another thread.
    public void storeSettings() {
        StringBuilder settingsFilename = new StringBuilder(directoryManager.getSettingsPath());
        settingsFilename.append("FilterSettings");
        settingsFilename.append("_");
        settingsFilename.append(getChannelCount());
        settingsFilename.append("Channels.json");
        String filename = settingsFilename.toString();
        File fileToSave = new File(filename);
        MAIN.selectOutput("Save filter settings to file", "storeFilterSettings", fileToSave);
    }
    //Avoid error with popup being in another thread.
    public void loadSettings() {
        StringBuilder settingsFilename = new StringBuilder(directoryManager.getSettingsPath());
        settingsFilename.append("FilterSettings");
        settingsFilename.append("_");
        settingsFilename.append(getChannelCount());
        settingsFilename.append("Channels.json");
        String filename = settingsFilename.toString();
        File fileToLoad = new File(filename);
        MAIN.selectInput("Select settings file to load", "loadFilterSettings", fileToLoad);
    }
}
