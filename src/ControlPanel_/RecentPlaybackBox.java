package ControlPanel_;

import Globel.GUI;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlP5;
import controlP5.ScrollableList;
import processing.core.PApplet;
import processing.data.JSONArray;
import processing.data.JSONObject;
import processing.data.StringList;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static Debugging_.GF.outputError;
import static Debugging_.GF.verbosePrint;
import static Extras_.GF.shortenString;
//import static GUI.GGVI.*;
import static Globel.GUI.*;
import static W_Playback_.GF.playbackFileFromList;

public class RecentPlaybackBox{
    public GUI gui;
    public int x, y, w, h, padding; //size and position
    public StringList shortFileNames = new StringList();
    public StringList longFilePaths = new StringList();
    private String filePickedShort = "Select Recent Playback File";
    public ControlP5 rpb_cp5;
    private ScrollableList recentPlaybackSL;
    private int titleH = 14;
    private int buttonH = 24;

    public RecentPlaybackBox(GUI gui, int _x, int _y, int _w, int _h, int _padding) {
        this.gui = gui;
        x = _x;
        y = _y;
        w = _w;
        h = titleH + buttonH + _padding*3;
        padding = _padding;

        rpb_cp5 = new ControlP5(gui);
        rpb_cp5.setGraphics(gui, 0,0);
        rpb_cp5.setAutoDraw(false);

        getRecentPlaybackFiles();

        String[] temp = shortFileNames.array();
        createRecentPlaybackFilesDropdown("recentPlaybackFilesCP", Arrays.asList(temp));
    }

    public void update() {
        //Update the dropdown list if it has not already been done
        if (!recentPlaybackFilesHaveUpdated) {
            recentPlaybackSL.clear();
            getRecentPlaybackFiles();
            String[] temp = shortFileNames.array();
            recentPlaybackSL.addItems(temp);
            recentPlaybackSL.setSize(w - padding*2, (temp.length + 1) * buttonH);
        }
    }

    public String getFilePickedShort() {
        return filePickedShort;
    }

    public void setFilePickedShort(String _fileName) {
        filePickedShort = _fileName;
    }

    public void draw() {
        this.gui.pushStyle();
        this.gui.fill(gui.boxColor);
        this.gui.stroke(gui.boxStrokeColor);
        this.gui.strokeWeight(1);
        this.gui.rect((float) x, (float) y, (float) w, (float) (h + recentPlaybackSL.getHeight() - padding*2.5));
        this.gui.fill(gui.OPENBCI_DARKBLUE);
        this.gui.textFont(h3, 16);
        this.gui.textAlign(LEFT, TOP);
        this.gui.text("PLAYBACK HISTORY", x + padding, y + padding);
        this.gui.popStyle();
        recentPlaybackSL.setVisible(true);
        rpb_cp5.draw();
    }

    public void getRecentPlaybackFiles() {
        int numFilesToShow = 10;

        File f = new File(userPlaybackHistoryFile);
        if (!f.exists()) {
            println("OpenBCI_GUI::Control Panel: Playback history file not found.");
            recentPlaybackFilesHaveUpdated = true;
            playbackHistoryFileExists = false;
            return;
        }

        try {
            JSONObject playbackHistory = gui.loadJSONObject(userPlaybackHistoryFile);
            JSONArray recentFilesArray = playbackHistory.getJSONArray("playbackFileHistory");
            if (recentFilesArray.size() < 10) {
                println("CP: Playback History Size = " + recentFilesArray.size());
                numFilesToShow = recentFilesArray.size();
            }
            shortFileNames.clear();
            longFilePaths.clear();
            for (int i = 0; i < numFilesToShow; i++) {
                JSONObject playbackFile = recentFilesArray.getJSONObject(recentFilesArray.size()-i-1);
                String shortFileName = playbackFile.getString("id");
                String longFilePath = playbackFile.getString("filePath");
                //truncate display name, if needed
                shortFileName = shortenString(gui, shortFileName, w-padding*2.f, h3);
                //store to arrays to set recent playback buttons text and function
                shortFileNames.append(shortFileName);
                longFilePaths.append(longFilePath);
                //println(shortFileName + " " + longFilePath);
            }

            playbackHistoryFileExists = true;
        } catch (Exception e) {
            println("OpenBCI_GUI::Control Panel: Other error! Please submit an issue on Github and share this console log.");
            println(e.getMessage());
            playbackHistoryFileExists = false;
        }
        recentPlaybackFilesHaveUpdated = true;
    }

    void createRecentPlaybackFilesDropdown(String name, List<String> _items){
        recentPlaybackSL = rpb_cp5.addScrollableList(name)
                .setOpen(false)
                .setColorBackground(gui.OPENBCI_BLUE) // text field bg color
                .setColorValueLabel(this.gui.color(255))       // text color
                .setColorCaptionLabel(this.gui.color(255))
                .setColorForeground(this.gui.color(125))    // border color when not selected
                .setColorActive(gui.BUTTON_PRESSED)       // border color when selected
                // .setColorCursor(color(26,26,26))

                .setSize(w - padding*2,(_items.size()+1)*24)// + maxFreqList.size())
                .setBarHeight(24) //height of top/primary bar
                .setItemHeight(24) //height of all item/dropdown bars
                .addItems(_items) // used to be .addItems(maxFreqList)
                .setVisible(true)
        ;
        recentPlaybackSL
                .getCaptionLabel() //the caption label is the text object in the primary bar
                .toUpperCase(false) //DO NOT AUTOSET TO UPPERCASE!!!
                .setText(filePickedShort)
                .setFont(h4)
                .setSize(14)
                .getStyle() //need to grab style before affecting the paddingTop
                .setPaddingTop(4)
        ;
        recentPlaybackSL
                .getValueLabel() //the value label is connected to the text objects in the dropdown item bars
                .toUpperCase(false) //DO NOT AUTOSET TO UPPERCASE!!!
                .setText(filePickedShort)
                .setFont(h5)
                .setSize(12) //set the font size of the item bars to 14pt
                .getStyle() //need to grab style before affecting the paddingTop
                .setPaddingTop(3) //4-pixel vertical offset to center text
        ;
        recentPlaybackSL.setPosition(x + padding, y + padding*2 + 13);
        recentPlaybackSL.setSize(w - padding*2, (_items.size() + 1) * buttonH);
        recentPlaybackSL.addCallback(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                if (theEvent.getAction() == ControlP5.ACTION_BROADCAST) {
                    int s = (int)recentPlaybackSL.getValue();
                    //println("got a menu event from item " + s);
                    String filePath = longFilePaths.get(s);
                    if (new File(filePath).isFile()) {
                        playbackFileFromList(gui, filePath, s);
                    } else {
                        verbosePrint("Playback History: " + filePath);
                        outputError("Playback History: Selected file does not exist. Try another file or clear settings to remove this entry.");
                    }
                }
            }
        });
    }
};
