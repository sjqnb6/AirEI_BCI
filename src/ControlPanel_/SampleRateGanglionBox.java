package ControlPanel_;

import GUI.GUIManager;
import Globel.GUI;
import controlP5.Button;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlP5;
import processing.core.PApplet;

import static Globel.GUI.h3;
import static processing.core.PApplet.println;
import static processing.core.PConstants.LEFT;
import static processing.core.PConstants.TOP;

public class SampleRateGanglionBox{
    public int x, y, w, h, padding; //size and position
    private ControlP5 srgb_cp5;
    private Button sampleRate200;
    private Button sampleRate1600;
    private int sr200_butX;
    private int sr1600_butX;
    private int srButton_butY;
    private GUI MAIN;
    SampleRateGanglionBox(GUI MAIN, int _x, int _y, int _w, int _h, int _padding) {
        x = _x;
        y = _y;
        w = _w;
        h = 73;
        padding = _padding;
        this.MAIN = MAIN;

        //Instantiate local cp5 for this box
        srgb_cp5 = new ControlP5(MAIN);
        srgb_cp5.setGraphics(MAIN, 0,0);
        srgb_cp5.setAutoDraw(false);

        sr200_butX = x + padding;
        sr1600_butX = x + padding*2 + (w-padding*3)/2;
        srButton_butY =  y + padding*2 + 18;
        createSR200Button("cytonSR200", "200Hz", false, sr200_butX, srButton_butY, (w-padding*3)/2, 24);
        createSR1600Button("cytonSR1600", "1600Hz", true, sr1600_butX, srButton_butY, (w-padding*3)/2, 24);
    }

    public void update() {
    }

    public void draw() {
        sr200_butX = x + padding;
        sr1600_butX = x + padding*2 + (w-padding*3)/2;
        srButton_butY =  y + padding*2 + 18;
        sampleRate200.setPosition(sr200_butX, srButton_butY);
        sampleRate1600.setPosition(sr1600_butX, srButton_butY);

        MAIN.pushStyle();
        MAIN.fill(MAIN.boxColor);
        MAIN.stroke(MAIN.boxStrokeColor);
        MAIN.strokeWeight(1);
        MAIN.rect(x, y, w, h);
        MAIN.fill(MAIN.OPENBCI_DARKBLUE);
        MAIN.textFont(h3, 16);
        MAIN.textAlign(LEFT, TOP);
        MAIN.text("SAMPLE RATE ", x + padding, y + padding);
        MAIN.fill(MAIN.OPENBCI_DARKBLUE); //set color to green
        MAIN.textFont(h3, 16);
        MAIN.textAlign(LEFT, TOP);
        MAIN.popStyle();

        srgb_cp5.draw();
    }

    private Button createSRGBButton(String name, String text, boolean isToggled, int _x, int _y, int _w, int _h) {
        final Button b = MAIN.createButton(srgb_cp5, name, text, _x, _y, _w, _h);
        b.setSwitch(true); //This turns the button into a switch
        if (isToggled) {
            b.setOn();
        }
        return b;
    }

    private void createSR200Button(String name, String text, boolean isToggled, int _x, int _y, int _w, int _h) {
        sampleRate200 = createSRGBButton(name, text, isToggled, _x, _y, _w, _h);
        sampleRate200.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                MAIN.selectedSamplingRate = 200;
                println("ControlPanel: User selected Ganglion+WiFi 200Hz");
                sampleRate200.setOn();
                sampleRate1600.setOff();
            }
        });
    }

    private void createSR1600Button(String name, String text, boolean isToggled, int _x, int _y, int _w, int _h) {
        sampleRate1600 = createSRGBButton(name, text, isToggled, _x, _y, _w, _h);
        sampleRate1600.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                MAIN.selectedSamplingRate = 1600;
                println("ControlPanel: User selected Ganglion+WiFi 1600Hz");
                sampleRate200.setOff();
                sampleRate1600.setOn();
            }
        });
    }

    public void lockCp5Objects(boolean flag) {
        sampleRate200.setLock(flag);
        sampleRate1600.setLock(flag);
    }
};