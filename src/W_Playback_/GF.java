package W_Playback_;

import PopupMessage_.PopupMessage;
import processing.core.PApplet;
import processing.data.JSONArray;
import processing.data.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import static Debugging_.GF.outputError;
import static Debugging_.GF.outputSuccess;
import static Debugging_.GF.verbosePrint;
import static Globel.GUI.playbackData_ShortName;
import static Globel.GUI.playbackData_fname;
import static Globel.GUI.playbackHistoryFileExists;
import static Globel.GUI.reinitRequested;
import static Globel.GUI.savePlaybackHistoryJSON;
import static Globel.GUI.sdData_fname;
import static Globel.GUI.userPlaybackHistoryFile;
import static processing.core.PApplet.loadJSONObject;
import static processing.core.PApplet.println;

public class GF {
    private static final String BRAND_NAME = "AirEIBCI";
    private static final String LEGACY_NAME = "OpenBCI";
    private static final String LEGACY_GUI_NAME = "OpenBCI_GUI";

    public static void playbackFileSelected(PApplet pApplet, File selection) {
        if (selection == null) {
            println("DataLogging: playbackSelected: dialog was closed or cancelled.");
        } else {
            println("DataLogging: playbackSelected: user selected " + selection.getAbsolutePath());
            playbackFileSelected(pApplet, selection.getAbsolutePath(), selection.getName());
        }
    }

    public static void playbackSelectedWidgetButton(PApplet pApplet, File selection) {
        if (selection == null) {
            println("W_Playback: playbackSelected: dialog was closed or cancelled.");
        } else {
            println("W_Playback: playbackSelected: user selected " + selection.getAbsolutePath());
            if (playbackFileSelected(pApplet, selection.getAbsolutePath(), selection.getName())) {
                requestReinit();
            }
        }
    }

    public static void userSelectedPlaybackMenuList(PApplet pApplet, String filePath, int listItem) {
        if (new File(filePath).isFile()) {
            playbackFileFromList(pApplet, filePath, listItem);
            requestReinit();
        } else {
            verbosePrint("Playback: " + filePath);
            outputError("Playback: 选中的文件不存在，请重新选择可用文件。");
        }
    }

    public static void playbackFileFromList(PApplet pApplet, String longName, int listItem) {
        String shortName = "";
        try {
            savePlaybackHistoryJSON = loadJSONObject(new File(userPlaybackHistoryFile));
            JSONArray recentFilesArray = savePlaybackHistoryJSON.getJSONArray("playbackFileHistory");
            JSONObject playbackFile = recentFilesArray.getJSONObject(-listItem + recentFilesArray.size() - 1);
            shortName = brandPlaybackName(playbackFile.getString("id"));
            playbackHistoryFileExists = true;
        } catch (NullPointerException e) {
            playbackHistoryFileExists = false;
        }
        playbackFileSelected(pApplet, longName, shortName);
    }

    public static boolean playbackFileSelected(PApplet pApplet, String longName, String shortName) {
        playbackData_fname = longName;
        playbackData_ShortName = brandPlaybackName(shortName);

        try (BufferedReader brTest = new BufferedReader(new FileReader(longName))) {
            String line = brTest.readLine();
            if (isPlaybackHeader(line)) {
                verbosePrint("PLAYBACK: Found playback header in file.");
                sdData_fname = "N/A";
                for (int i = 0; i < 3; i++) {
                    line = brTest.readLine();
                    verbosePrint("PLAYBACK: " + line);
                }
                if (line == null || !line.startsWith("%Board")) {
                    playbackData_fname = "N/A";
                    playbackData_ShortName = "N/A";
                    outputError("检测到过旧版本的回放文件，请先完成格式转换后再加载。");
                    new PopupMessage(
                        "旧版回放文件转换",
                        "检测到较旧版本的回放文件，请先转换后再加载。",
                        "LINK",
                        "https://github.com/OpenBCI/OpenBCI_GUI/tree/development/tools"
                    );
                    return false;
                }
            } else if ("%STOP AT".equals(line)) {
                verbosePrint("PLAYBACK: Found SD recording header in file.");
                playbackData_fname = "N/A";
                sdData_fname = longName;
            } else {
                outputError("不支持该回放文件格式，请选择有效的 AirEIBCI 数据文件。");
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

        outputSuccess("已选择 \"" + playbackData_ShortName + "\" 用于回放。");

        File f = new File(userPlaybackHistoryFile);
        if (!f.exists()) {
            println("AirEIBCI::playbackFileSelected: playback history file was not found.");
            playbackHistoryFileExists = false;
        } else {
            try {
                savePlaybackHistoryJSON = loadJSONObject(new File(userPlaybackHistoryFile));
                savePlaybackHistoryJSON.getJSONArray("playbackFileHistory");
                playbackHistoryFileExists = true;
            } catch (RuntimeException e) {
                outputError("回放历史文件损坏，请删除后重新启动软件。");
                File file = new File(userPlaybackHistoryFile);
                if (!file.isDirectory()) {
                    file.delete();
                }
            }
        }

        savePlaybackFileToHistory(pApplet, longName);
        return true;
    }

    public static void savePlaybackFileToHistory(PApplet pApplet, String fileName) {
        int maxNumHistoryFiles = 36;
        if (playbackHistoryFileExists) {
            println("Playback history file found.");
            savePlaybackHistoryJSON = loadJSONObject(new File(userPlaybackHistoryFile));
            JSONArray recentFilesArray = savePlaybackHistoryJSON.getJSONArray("playbackFileHistory");
            removePlaybackFileFromHistory(recentFilesArray, playbackData_fname);
            for (int i = 0; i < recentFilesArray.size(); i++) {
                JSONObject playbackFile = recentFilesArray.getJSONObject(i);
                playbackFile.setInt("recentFileNumber", recentFilesArray.size() - i);
                playbackFile.setString("id", brandPlaybackName(playbackFile.getString("id")));
                playbackFile.setString("filePath", playbackFile.getString("filePath"));
                recentFilesArray.setJSONObject(i, playbackFile);
            }

            JSONObject mostRecentFile = new JSONObject();
            mostRecentFile.setInt("recentFileNumber", 0);
            mostRecentFile.setString("id", playbackData_ShortName);
            mostRecentFile.setString("filePath", playbackData_fname);
            recentFilesArray.append(mostRecentFile);

            if (recentFilesArray.size() >= maxNumHistoryFiles) {
                for (int i = 0; i <= recentFilesArray.size() - maxNumHistoryFiles; i++) {
                    recentFilesArray.remove(i);
                    println("ARRAY INDEX " + i + " REMOVED----");
                }
            }

            savePlaybackHistoryJSON.setJSONArray("playbackFileHistory", recentFilesArray);
            pApplet.saveJSONObject(savePlaybackHistoryJSON, userPlaybackHistoryFile);
        } else {
            println("Playback history file not found. Creating a new one.");
            JSONObject newHistoryFile = new JSONObject();
            JSONArray newHistoryFileArray = new JSONArray();
            JSONObject mostRecentFile = new JSONObject();
            mostRecentFile.setInt("recentFileNumber", 0);
            mostRecentFile.setString("id", playbackData_ShortName);
            mostRecentFile.setString("filePath", playbackData_fname);
            newHistoryFileArray.setJSONObject(0, mostRecentFile);
            newHistoryFile.setJSONArray("playbackFileHistory", newHistoryFileArray);
            pApplet.saveJSONObject(newHistoryFile, userPlaybackHistoryFile);
            println("Playback history JSON has been created.");
            playbackHistoryFileExists = true;
        }
    }

    public static void removePlaybackFileFromHistory(JSONArray array, String filePath) {
        for (int i = 0; i < array.size(); i++) {
            JSONObject playbackFile = array.getJSONObject(i);
            if (playbackFile.getString("filePath").equals(filePath)) {
                array.remove(i);
            }
        }
    }

    public static void requestReinit() {
        reinitRequested = true;
    }

    public static String brandPlaybackName(String rawName) {
        if (rawName == null || rawName.isEmpty()) {
            return BRAND_NAME;
        }
        return rawName.replace(LEGACY_GUI_NAME, BRAND_NAME).replace(LEGACY_NAME, BRAND_NAME);
    }

    public static boolean isPlaybackHeader(String line) {
        return ("%OpenBCI Raw EEG Data".equals(line))
            || ("%OpenBCI Raw EXG Data".equals(line))
            || ("%AirEIBCI Raw EEG Data".equals(line))
            || ("%AirEIBCI Raw EXG Data".equals(line));
    }
}
