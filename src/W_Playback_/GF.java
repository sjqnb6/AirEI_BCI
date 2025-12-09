package W_Playback_;

import PopupMessage_.PopupMessage;
import processing.core.PApplet;
import processing.data.JSONArray;
import processing.data.JSONObject;

import java.io.*;

import static Debugging_.GF.*;
import static Globel.GUI.*;
import static processing.core.PApplet.loadJSONObject;
import static processing.core.PApplet.println;

public class GF {

    //////////////////////////////////////
// GLOBAL FUNCTIONS BELOW THIS LINE //
    //////////////////////////////////////

//Called when user selects a playback file from controlPanel dialog box
    public static void playbackFileSelected(PApplet PApplet, File selection) {
        if (selection == null) {
            println("DataLogging: playbackSelected: Window was closed or the user hit cancel.");
        } else {
            println("DataLogging: playbackSelected: User selected " + selection.getAbsolutePath());
            //Set the name of the file
            playbackFileSelected(PApplet, selection.getAbsolutePath(), selection.getName());
        }
    }


    //Activated when user selects a file using the "Select Playback File" button in PlaybackHistory
    public static void playbackSelectedWidgetButton(PApplet PApplet, File selection) {
        if (selection == null) {
            println("W_Playback: playbackSelected: Window was closed or the user hit cancel.");
        } else {
            println("W_Playback: playbackSelected: User selected " + selection.getAbsolutePath());
            if (playbackFileSelected(PApplet, selection.getAbsolutePath(), selection.getName())) {
                // restart the session with the new file
                requestReinit();
            }
        }
    }

    //Activated when user selects a file using the recent file MenuList
    public static void userSelectedPlaybackMenuList (PApplet PApplet, String filePath, int listItem) {
        if (new File(filePath).isFile()) {
            playbackFileFromList(PApplet, filePath, listItem);
            // restart the session with the new file
            requestReinit();
        } else {
            verbosePrint("Playback: " + filePath);
            outputError("Playback: Selected file does not exist. Try another file or clear settings to remove this entry.");
        }
    }

    //Called when user selects a playback file from a list
    public static void playbackFileFromList (PApplet PApplet, String longName, int listItem) {
        String shortName = "";
        //look at the JSON file to set the range menu using number of recent file entries
        try {
            savePlaybackHistoryJSON = loadJSONObject(new File(userPlaybackHistoryFile));
            JSONArray recentFilesArray = savePlaybackHistoryJSON.getJSONArray("playbackFileHistory");
            JSONObject playbackFile = recentFilesArray.getJSONObject(-listItem + recentFilesArray.size() - 1);
            shortName = playbackFile.getString("id");
            playbackHistoryFileExists = true;
        } catch (NullPointerException e) {
            //println("Playback history JSON file does not exist. Load first file to make it.");
            playbackHistoryFileExists = false;
        }
        playbackFileSelected(PApplet, longName, shortName);
    }

    //Handles the work for the above cases
    public static boolean playbackFileSelected (PApplet PApplet, String longName, String shortName) {
        playbackData_fname = longName;
        playbackData_ShortName = shortName;
        //Process the playback file, check if SD card file or something else
        try {
            BufferedReader brTest = new BufferedReader(new FileReader(longName));
            String line = brTest.readLine();
            if (line.equals("%OpenBCI Raw EEG Data") || line.equals("%OpenBCI Raw EXG Data")) {
                verbosePrint("PLAYBACK: Found OpenBCI Header in File!");
                sdData_fname = "N/A";
                for (int i = 0; i < 3; i++) {
                    line = brTest.readLine();
                    verbosePrint("PLAYBACK: " + line);
                }
                if (!line.startsWith("%Board")) {
                    playbackData_fname = "N/A";
                    playbackData_ShortName = "N/A";
                    outputError("Found GUI v4 or earlier file. Please convert this file using the provided Python script.");
                    PopupMessage msg = new PopupMessage("GUI v4 to v5 File Converter", "Found GUI v4 or earlier file. Please convert this file using the provided Python script. Press the button below to access this open-source fix.", "LINK", "https://github.com/OpenBCI/OpenBCI_GUI/tree/development/tools");
                    return false;
                }
            } else if (line.equals("%STOP AT")) {
                verbosePrint("PLAYBACK: Found SD File Header in File!");
                playbackData_fname = "N/A";
                sdData_fname = longName;
            } else {
                outputError("ERROR: Tried to load an unsupported file for playback! Please try a valid file.");
                playbackData_fname = "N/A";
                playbackData_ShortName = "N/A";
                sdData_fname = "N/A";
                return false;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        //Output new playback settings to GUI as success
        outputSuccess("You have selected \""
                + shortName + "\" for playback.");

        File f = new File(userPlaybackHistoryFile);
        if (!f.exists()) {
            println("OpenBCI_GUI::playbackFileSelected: Playback history file not found.");
            playbackHistoryFileExists = false;
        } else {
            try {
                savePlaybackHistoryJSON = loadJSONObject(new File(userPlaybackHistoryFile));
                JSONArray recentFilesArray = savePlaybackHistoryJSON.getJSONArray("playbackFileHistory");
                playbackHistoryFileExists = true;
            } catch (RuntimeException e) {
                outputError("Found an error in UserPlaybackHistory.json. Deleting this file. Please, Restart the GUI.");
                File file = new File(userPlaybackHistoryFile);
                if (!file.isDirectory()) {
                    file.delete();
                }
            }
        }

        //add playback file that was processed to the JSON history
        savePlaybackFileToHistory(PApplet, longName);
        return true;
    }

    public static void savePlaybackFileToHistory(PApplet PApplet, String fileName) {
        int maxNumHistoryFiles = 36;
        if (playbackHistoryFileExists) {
            println("Found user playback history file!");
            savePlaybackHistoryJSON = loadJSONObject(new File(userPlaybackHistoryFile));
            JSONArray recentFilesArray = savePlaybackHistoryJSON.getJSONArray("playbackFileHistory");
            //println("ARRAYSIZE-Check1: " + int(recentFilesArray.size()));
            //Recent file has recentFileNumber=0, and appears at the end of the JSON array
            //check if already in the list, if so, remove from the list
            removePlaybackFileFromHistory(recentFilesArray, playbackData_fname);
            //next, increment fileNumber of all current entries +1
            for (int i = 0; i < recentFilesArray.size(); i++) {
                JSONObject playbackFile = recentFilesArray.getJSONObject(i);
                playbackFile.setInt("recentFileNumber", recentFilesArray.size()-i);
                //println(recentFilesArray.size()-i);
                playbackFile.setString("id", playbackFile.getString("id"));
                playbackFile.setString("filePath", playbackFile.getString("filePath"));
                recentFilesArray.setJSONObject(i, playbackFile);
            }
            //println("ARRAYSIZE-Check2: " + int(recentFilesArray.size()));
            //append selected playback file to position 1 at the end of the JSONArray
            JSONObject mostRecentFile = new JSONObject();
            mostRecentFile.setInt("recentFileNumber", 0);
            mostRecentFile.setString("id", playbackData_ShortName);
            mostRecentFile.setString("filePath", playbackData_fname);
            recentFilesArray.append(mostRecentFile);
            //remove entries greater than max num files
            if (recentFilesArray.size() >= maxNumHistoryFiles) {
                for (int i = 0; i <= recentFilesArray.size()-maxNumHistoryFiles; i++) {
                    recentFilesArray.remove(i);
                    println("ARRAY INDEX " + i + " REMOVED----");
                }
            }
            //println("ARRAYSIZE-Check3: " + int(recentFilesArray.size()));
            //printArray(recentFilesArray);

            //save the JSON array and file
            savePlaybackHistoryJSON.setJSONArray("playbackFileHistory", recentFilesArray);
            PApplet.saveJSONObject(savePlaybackHistoryJSON, userPlaybackHistoryFile);

        } else if (!playbackHistoryFileExists) {
            println("Playback history file not found. making a new one.");
            //do this if the file does not exist
            JSONObject newHistoryFile;
            newHistoryFile = new JSONObject();
            JSONArray newHistoryFileArray = new JSONArray();
            //save selected playback file to position 1 in recent file history
            JSONObject mostRecentFile = new JSONObject();
            mostRecentFile.setInt("recentFileNumber", 0);
            mostRecentFile.setString("id", playbackData_ShortName);
            mostRecentFile.setString("filePath", playbackData_fname);
            newHistoryFileArray.setJSONObject(0, mostRecentFile);
            //newHistoryFile.setJSONArray("")

            //save the JSON array and file
            newHistoryFile.setJSONArray("playbackFileHistory", newHistoryFileArray);
            PApplet.saveJSONObject(newHistoryFile, userPlaybackHistoryFile);

            //now the file exists!
            println("Playback history JSON has been made!");
            playbackHistoryFileExists = true;
        }
    }

    public static void removePlaybackFileFromHistory(JSONArray array, String _filePath) {
        //check if already in the list, if so, remove from the list
        for (int i = 0; i < array.size(); i++) {
            JSONObject playbackFile = array.getJSONObject(i);
            //println("CHECKING " + i + " : " + playbackFile.getString("id") + " == " + fileName + " ?");
            if (playbackFile.getString("filePath").equals(_filePath)) {
                array.remove(i);
                //println("REMOVED: " + fileName);
            }
        }
    }


    public static void requestReinit() {
        reinitRequested = true;
    }
}
