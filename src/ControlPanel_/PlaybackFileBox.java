package ControlPanel_;

import Globel.GUI;
import controlP5.Button;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlP5;

import java.io.File;

import static Debugging_.GF.output;
import static Globel.GUI.p7;
import static processing.core.PConstants.LEFT;
import static processing.core.PConstants.TOP;

public class PlaybackFileBox {
    public int x, y, w, h, padding;
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

        pbfb_cp5 = new ControlP5(MAIN);
        pbfb_cp5.setGraphics(MAIN, 0, 0);
        pbfb_cp5.setAutoDraw(false);

        createSelectPlaybackFileButton("selectPlaybackFileControlPanel", "选择回放文件", x + padding, y + padding * 2 + titleH, w - padding * 2, buttonH);
        createSampleDataButton("selectSampleDataControlPanel", "示例数据", x + w - sampleDataButton_w - padding, y + padding - 2, sampleDataButton_w, sampleDataButton_h);
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
        MAIN.textFont(p7, 16);
        MAIN.textAlign(LEFT, TOP);
        MAIN.text("回放文件", x + padding, y + padding);
        MAIN.popStyle();

        pbfb_cp5.draw();
    }

    private void createSelectPlaybackFileButton(String name, String text, int _x, int _y, int _w, int _h) {
        selectPlaybackFile = MAIN.createButton(pbfb_cp5, name, text, _x, _y, _w, _h);
        selectPlaybackFile.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                output("选择回放文件");
                MAIN.selectInput(
                    "选择用于回放的文件:",
                    "playbackFileSelected",
                    new File(MAIN.directoryManager.getGuiDataPath() + "Recordings" + File.separator + "123.txt")
                );
            }
        });
        selectPlaybackFile.setDescription("点击打开对话框，选择 AirEIBCI 回放文件（.txt 或 .csv）。");
    }

    private void createSampleDataButton(String name, String text, int _x, int _y, int _w, int _h) {
        sampleDataButton = MAIN.createButton(pbfb_cp5, name, text, _x, _y, _w, _h, p7, 12, MAIN.buttonsLightBlue, MAIN.color(255));
        sampleDataButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                output("选择回放文件");
                MAIN.selectInput(
                    "选择示例回放文件",
                    "playbackFileSelected",
                    new File(MAIN.directoryManager.getGuiDataPath() + "Sample_Data" + System.getProperty("file.separator") + "AirEIBCI-v6-meditation.txt")
                );
            }
        });
        sampleDataButton.setDescription("点击打开内置的 AirEIBCI 示例数据文件。");
    }
}
