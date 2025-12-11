package ControlPanel_;

import GUI.GUIManager;
import Globel.GUI;
import controlP5.Button;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlP5;
import processing.core.PApplet;
import processing.core.PConstants;

import static Globel.GUI.h3;
import static Globel.GUI.nchan;
import static SystemManager.GF.updateToNChan;
import static processing.core.PApplet.str;
import static processing.core.PConstants.LEFT;
import static processing.core.PConstants.TOP;

public class ChannelCountBox{
    public int x, y, w, h, padding; //size and position
    private ControlP5 ccc_cp5;
    private Button chanButton8;
    private Button chanButton16;
    private int cb8_butX;
    private int cb16_butX;
    private int cb_butY;
    private GUI MAIN;

    ChannelCountBox(GUI MAIN, int _x, int _y, int _w, int _h, int _padding) {
        x = _x;
        y = _y;
        w = _w;
        h = 73;
        padding = _padding;

        this.MAIN = MAIN;

        //Instantiate local cp5 for this box
        ccc_cp5 = new ControlP5(MAIN);
        ccc_cp5.setGraphics(MAIN, 0,0);
        ccc_cp5.setAutoDraw(false);

        cb8_butX = x + padding;
        cb16_butX = x + padding*2 + (w-padding*3)/2;
        cb_butY = y + padding*2 + 18;
        boolean is8Channels = (nchan == 8) ? true : false;
        createChan8Button("cyton8ChanButton", "8 CHANNELS", is8Channels, cb8_butX, cb_butY, (w-padding*3)/2, 24);
        createChan16Button("cyton16ChanButton", "16 CHANNELS", is8Channels, cb16_butX, cb_butY, (w-padding*3)/2, 24);
    }

    public void update() {
    }

    public void draw() {
        cb_butY = y + padding*2 + 18;
        chanButton8.setPosition(cb8_butX, cb_butY);
        chanButton16.setPosition(cb16_butX, cb_butY);

        MAIN.pushStyle();
        MAIN.fill(MAIN.boxColor);
        MAIN.stroke(MAIN.boxStrokeColor);
        MAIN.strokeWeight(1);
        MAIN.rect(x, y, w, h);
        MAIN.fill(MAIN.OPENBCI_DARKBLUE);
        MAIN.textFont(h3, 16);
        MAIN.textAlign(LEFT, TOP);
        MAIN.text("CHANNEL COUNT ", x + padding, y + padding);
        MAIN.fill(MAIN.OPENBCI_DARKBLUE); //set color to green
        MAIN.textFont(h3, 16);
        MAIN.textAlign(LEFT, TOP);
        MAIN.text("  (" + str(nchan) + ")", x + padding + 142, y + padding); // print the channel count in green next to the box title
        MAIN.popStyle();

        ccc_cp5.draw();
    }

    private Button createCCCButton(String name, String text, boolean isToggled, int _x, int _y, int _w, int _h) {
        final Button b = MAIN.createButton(ccc_cp5, name, text, _x, _y, _w, _h);
        b.setSwitch(true); //This turns the button into a switch
        if (isToggled) {
            b.setOn();
        }
        return b;
    }

    private void createChan8Button(String name, String text, boolean isToggled, int _x, int _y, int _w, int _h) {
        chanButton8 = createCCCButton(name, text, isToggled, _x, _y, _w, _h);
        chanButton8.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                updateToNChan(MAIN,8);
                chanButton8.setOn();
                chanButton16.setOff();
            }
        });
    }

    private void createChan16Button(String name, String text, boolean isToggled, int _x, int _y, int _w, int _h) {
        chanButton16 = createCCCButton(name, text, isToggled, _x, _y, _w, _h);
        chanButton16.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                updateToNChan(MAIN,16);
                chanButton8.setOff();
                chanButton16.setOn();
            }
        });
    }

    public void lockCp5Objects(boolean flag) {
        chanButton8.setLock(flag);
        chanButton16.setLock(flag);
    }

    public void set8ChanButtonActive() {
        updateToNChan(MAIN,8);
        chanButton8.setOn();
        chanButton16.setOff();
    }
};