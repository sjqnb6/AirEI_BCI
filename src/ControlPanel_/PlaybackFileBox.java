package ControlPanel_;

import GUI.GUIManager;
import Globel.GUI;
import controlP5.Button;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlP5;
import processing.core.PApplet;

import java.io.File;

import static Debugging_.GF.output;
import static Globel.GUI.p5;
import static processing.core.PConstants.LEFT;
import static processing.core.PConstants.TOP;

public class PlaybackFileBox{
    public int x, y, w, h, padding; //size and position
    private ControlP5 pbfb_cp5;
    private Button sampleDataButton;
    private Button selectPlaybackFile;
    private int sampleDataButton_w = 100;
    private int sampleDataButton_h = 20;
    private int titleH = 14;
    private int buttonH = 24;
    private GUI MAIN;
    PlaybackFileBox(GUI MAIN, int _x, int _y, int _w, int _h, int _padding) {
        x = _x;
        y = _y;
        w = _w;
        h = buttonH + (_padding * 3) + titleH;
        padding = _padding;

        this.MAIN = MAIN;

        //Instantiate local cp5 for this box
        pbfb_cp5 = new ControlP5(MAIN);
        pbfb_cp5.setGraphics(MAIN, 0,0);
        pbfb_cp5.setAutoDraw(false);

        createSelectPlaybackFileButton("selectPlaybackFileControlPanel", "SELECT OPENBCI PLAYBACK FILE", x + padding, y + padding*2 + titleH, w - padding*2, buttonH);
        createSampleDataButton("selectSampleDataControlPanel", "Sample Data", x + w - sampleDataButton_w - padding, y + padding - 2, sampleDataButton_w, sampleDataButton_h);
    }

    public void update() {
    }

    public void draw() {
        MAIN.pushStyle();
        MAIN.fill(MAIN.boxColor);
        MAIN.stroke(MAIN.boxStrokeColor);
        MAIN.strokeWeight(1);
        MAIN.rect(x, y, w, h);
        MAIN.fill(MAIN.OPENBCI_DARKBLUE);
        MAIN.textFont(MAIN.h3, 16);
        MAIN.textAlign(LEFT, TOP);
        MAIN.text("PLAYBACK FILE", x + padding, y + padding);
        MAIN.popStyle();

        pbfb_cp5.draw();
    }

    private void createSelectPlaybackFileButton(String name, String text, int _x, int _y, int _w, int _h) {
        selectPlaybackFile = MAIN.createButton(pbfb_cp5, name, text, _x, _y, _w, _h);
        selectPlaybackFile.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                output("Select a file for playback");
                MAIN.selectInput("Select a pre-recorded file for playback:",
                        "playbackFileSelected",
                        new File(MAIN.directoryManager.getGuiDataPath() + "Recordings")
                );
            }
        });
        selectPlaybackFile.setDescription("Click to open a dialog box to select an OpenBCI playback file (.txt or .csv).");
    }

    private void createSampleDataButton(String name, String text, int _x, int _y, int _w, int _h) {
        sampleDataButton = MAIN.createButton(pbfb_cp5, name, text, _x, _y, _w, _h, p5, 12, MAIN.buttonsLightBlue, MAIN.color(255));
        sampleDataButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                output("Select a file for playback");
                MAIN.selectInput("Select a pre-recorded file for playback:",
                        "playbackFileSelected",
                        new File(MAIN.directoryManager.getGuiDataPath() + "Sample_Data" + System.getProperty("file.separator") + "OpenBCI-sampleData-2-meditation.txt")
                );
            }
        });
        //sampleDataButton.setCornerRoundness((int)(sampleDataButton_h));
        sampleDataButton.setDescription("Click to open the folder containing OpenBCI GUI Sample Data.");
    }
};