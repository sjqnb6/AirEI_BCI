package ControlPanel_;

import GUI.GUIManager;
import Globel.GUI;
import controlP5.Button;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlP5;
import processing.core.PApplet;

import static Globel.GUI.h3;
import static Globel.GUI.nchan;
import static SystemManager.GF.updateToNChan;
import static processing.core.PApplet.str;
import static processing.core.PConstants.LEFT;
import static processing.core.PConstants.TOP;

public class SyntheticChannelCountBox{
    public int x, y, w, h, padding; //size and position
    private ControlP5 sccb_cp5;
    private Button synthChanButton4;
    private Button synthChanButton8;
    private Button synthChanButton16;
    private GUI MAIN;
    SyntheticChannelCountBox(GUI MAIN, int _x, int _y, int _w, int _h, int _padding) {
        this.MAIN = MAIN;
        x = _x;
        y = _y;
        w = _w;
        h = 73;
        padding = _padding;

        //Instantiate local cp5 for this box
        sccb_cp5 = new ControlP5(MAIN);
        sccb_cp5.setGraphics(MAIN, 0,0);
        sccb_cp5.setAutoDraw(false);

        createSynthChan4Button("synthChan4Button", "4 chan", x + padding, y + padding*2 + 18, (w-padding*4)/3, 24);
        createSynthChan8Button("synthChan8Button", "8 chan", x + padding*2 + (w-padding*4)/3, y + padding*2 + 18, (w-padding*4)/3, 24);
        createSynthChan16Button("synthChan16Button", "16 chan", x + padding*3 + ((w-padding*4)/3)*2, y + padding*2 + 18, (w-padding*4)/3, 24);
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
        MAIN.textFont(h3, 16);
        MAIN.textAlign(LEFT, TOP);
        MAIN.text("CHANNEL COUNT", x + padding, y + padding);
        MAIN.fill(MAIN.OPENBCI_DARKBLUE); //set color to green
        MAIN.textFont(h3, 16);
        MAIN.textAlign(LEFT, TOP);
        MAIN.text("  (" + str(nchan) + ")", x + padding + 142, y + padding); // print the channel count in green next to the box title
        MAIN.popStyle();

        sccb_cp5.draw();
    }

    private Button createSCCBButton(String name, String text, boolean isToggled, int _x, int _y, int _w, int _h) {
        final Button b = MAIN.createButton(sccb_cp5, name, text, _x, _y, _w, _h);
        b.setSwitch(true); //This turns the button into a switch
        if (isToggled) {
            b.setOn();
        }
        return b;
    }

    private void createSynthChan4Button(String name, String text, int _x, int _y, int _w, int _h) {
        synthChanButton4 = createSCCBButton(name, text, false,_x, _y, _w, _h);
        synthChanButton4.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                updateToNChan(MAIN,4);
                synthChanButton4.setOn();
                synthChanButton8.setOff();
                synthChanButton16.setOff();
            }
        });
    }

    private void createSynthChan8Button(String name, String text, int _x, int _y, int _w, int _h) {
        //Default is 8 channels when app starts
        synthChanButton8 = createSCCBButton(name, text, true, _x, _y, _w, _h);
        synthChanButton8.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                updateToNChan(MAIN,8);
                synthChanButton4.setOff();
                synthChanButton8.setOn();
                synthChanButton16.setOff();
            }
        });
    }

    private void createSynthChan16Button(String name, String text, int _x, int _y, int _w, int _h) {
        synthChanButton16 = createSCCBButton(name, text, false, _x, _y, _w, _h);
        synthChanButton16.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                updateToNChan(MAIN,16);
                synthChanButton4.setOff();
                synthChanButton8.setOff();
                synthChanButton16.setOn();
            }
        });
    }

    public void set8ChanButtonActive() {
        updateToNChan(MAIN,8);
        synthChanButton4.setOff();
        synthChanButton8.setOn();
        synthChanButton16.setOff();
    }
};