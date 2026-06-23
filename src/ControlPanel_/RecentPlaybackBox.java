package ControlPanel_;

import Globel.GUI;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlP5;
import controlP5.ScrollableList;
import processing.data.JSONArray;
import processing.data.JSONObject;
import processing.data.StringList;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static Debugging_.GF.outputError;
import static Debugging_.GF.verbosePrint;
import static Extras_.GF.shortenString;
import static Globel.GUI.*;
import static W_Playback_.GF.brandPlaybackName;
import static W_Playback_.GF.playbackFileFromList;

public class RecentPlaybackBox {
    public GUI gui;
    public int x, y, w, h, padding;
    public StringList shortFileNames = new StringList();
    public StringList longFilePaths = new StringList();
    private String filePickedShort = "选择最近的回放文件";
    public ControlP5 rpb_cp5;
    private ScrollableList recentPlaybackSL;
    private int titleH = 14;
    private int buttonH = 24;

    public RecentPlaybackBox(GUI gui, int _x, int _y, int _w, int _h, int _padding) {
        this.gui = gui;
        x = _x;
        y = _y;
        w = _w;
        h = titleH + buttonH + _padding * 3;
        padding = _padding;

        rpb_cp5 = new ControlP5(gui);
        rpb_cp5.setGraphics(gui, 0, 0);
        rpb_cp5.setAutoDraw(false);

        getRecentPlaybackFiles();

        String[] temp = shortFileNames.array();
        createRecentPlaybackFilesDropdown("recentPlaybackFilesCP", Arrays.asList(temp));
    }

    public void update() {
        if (!recentPlaybackFilesHaveUpdated) {
            recentPlaybackSL.clear();
            getRecentPlaybackFiles();
            String[] temp = shortFileNames.array();
            recentPlaybackSL.addItems(temp);
            recentPlaybackSL.setSize(w - padding * 2, (temp.length + 1) * buttonH);
        }
    }

    public String getFilePickedShort() {
        return filePickedShort;
    }

    public void setFilePickedShort(String fileName) {
        filePickedShort = fileName;
    }

    public void draw() {
        this.gui.pushStyle();
        this.gui.fill(gui.boxColor);
        this.gui.stroke(gui.boxStrokeColor);
        this.gui.strokeWeight(1);
        this.gui.rect((float) x, (float) y, (float) w, (float) (h + recentPlaybackSL.getHeight() - padding * 2.5));
        this.gui.fill(gui.OPENBCI_DARKBLUE);
        this.gui.textFont(p7, 16);
        this.gui.textAlign(LEFT, TOP);
        this.gui.text("回放历史", x + padding, y + padding);
        this.gui.popStyle();
        recentPlaybackSL.setVisible(true);
        rpb_cp5.draw();
    }

    public void getRecentPlaybackFiles() {
        int numFilesToShow = 10;

        File f = new File(userPlaybackHistoryFile);
        if (!f.exists()) {
            println("AirEIBCI::Control Panel: 没有找到回放历史文件");
            recentPlaybackFilesHaveUpdated = true;
            playbackHistoryFileExists = false;
            return;
        }

        try {
            JSONObject playbackHistory = gui.loadJSONObject(userPlaybackHistoryFile);
            JSONArray recentFilesArray = playbackHistory.getJSONArray("playbackFileHistory");
            if (recentFilesArray.size() < 10) {
                println("CP: playback history size = " + recentFilesArray.size());
                numFilesToShow = recentFilesArray.size();
            }
            shortFileNames.clear();
            longFilePaths.clear();
            for (int i = 0; i < numFilesToShow; i++) {
                JSONObject playbackFile = recentFilesArray.getJSONObject(recentFilesArray.size() - i - 1);
                String shortFileName = brandPlaybackName(playbackFile.getString("id"));
                String longFilePath = playbackFile.getString("filePath");
                shortFileName = shortenString(gui, shortFileName, w - padding * 2.f, h3);
                shortFileNames.append(shortFileName);
                longFilePaths.append(longFilePath);
            }

            playbackHistoryFileExists = true;
        } catch (Exception e) {
            println("AirEIBCI::Control Panel: 读取回放历史时出现错误。");
            println(e.getMessage());
            playbackHistoryFileExists = false;
        }
        recentPlaybackFilesHaveUpdated = true;
    }

    void createRecentPlaybackFilesDropdown(String name, List<String> items) {
        recentPlaybackSL = rpb_cp5.addScrollableList(name)
            .setOpen(false)
            .setColorBackground(gui.OPENBCI_BLUE)
            .setColorValueLabel(this.gui.color(255))
            .setColorCaptionLabel(this.gui.color(255))
            .setColorForeground(this.gui.color(125))
            .setColorActive(gui.BUTTON_PRESSED)
            .setSize(w - padding * 2, (items.size() + 1) * 24)
            .setBarHeight(24)
            .setItemHeight(24)
            .addItems(items)
            .setVisible(true);
        recentPlaybackSL
            .getCaptionLabel()
            .toUpperCase(false)
            .setText(filePickedShort)
            .setFont(p7)
            .setSize(14)
            .getStyle()
            .setPaddingTop(4);
        recentPlaybackSL
            .getValueLabel()
            .toUpperCase(false)
            .setText(filePickedShort)
            .setFont(p7)
            .setSize(12)
            .getStyle()
            .setPaddingTop(3);
        recentPlaybackSL.setPosition(x + padding, y + padding * 2 + 13);
        recentPlaybackSL.setSize(w - padding * 2, (items.size() + 1) * buttonH);
        recentPlaybackSL.addCallback(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                if (theEvent.getAction() == ControlP5.ACTION_BROADCAST) {
                    int s = (int) recentPlaybackSL.getValue();
                    String filePath = longFilePaths.get(s);
                    if (new File(filePath).isFile()) {
                        playbackFileFromList(gui, filePath, s);
                    } else {
                        verbosePrint("回放历史: " + filePath);
                        outputError("回放历史中的文件不存在，请重新选择可用文件。");
                    }
                }
            }
        });
    }
}
