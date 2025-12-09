package ControlPanel_;

import CustomCp5Classes_.MenuList;
import GUI.GUIManager;
import Globel.GUI;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlP5;
import processing.core.PApplet;

import static GUI.GGVI.*;
import static processing.core.PConstants.LEFT;
import static processing.core.PConstants.TOP;

public class ChannelPopup{
    public int x, y, w, h, padding; //size and position
    private boolean clicked;
    private final String CHANGE_CHAN = "Change Channel";
    private final String OVR_DONGLE = "Override Dongle";
    private String title = "";
    private ControlP5 cp_cp5;
    private MenuList channelList;

    private GUI MAIN;

    ChannelPopup(GUI MAIN, int _x, int _y, int _w, int _h, int _padding) {
        x = _x + _w * 2;
        y = _y;
        w = _w;
        h = 171 + _padding;
        padding = _padding;
        clicked = false;

        this.MAIN = MAIN;

        //Instantiate local cp5 for this box
        cp_cp5 = new ControlP5(MAIN);
        cp_cp5.setGraphics(MAIN, 0,0);
        cp_cp5.setAutoDraw(false);

        channelList = new MenuList(cp_cp5, "channelListCP", w - padding*2, 140, p3, MAIN);
        channelList.setPosition(x+padding, y+padding*3);
        for (int i = 1; i < 26; i++) {
            channelList.addItem(String.valueOf(i));
        }
        channelList.addCallback(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                if (theEvent.getAction() == ControlP5.ACTION_BROADCAST) {
                    int setChannelInt = (int)(theEvent.getController()).getValue() + 1;
                    setClicked(false);
                    if (title.equals(CHANGE_CHAN)) {
                        controlPanel.rcBox.setChannel(setChannelInt);
                    } else if (title.equals(OVR_DONGLE)) {
                        controlPanel.rcBox.setChannelOverride(setChannelInt);
                    }
                }
            }
        });
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
        MAIN.text(title, x + padding, y + padding);
        MAIN.popStyle();
        cp_cp5.draw();
    }

    public void setClicked(boolean click) { this.clicked = click; }
    public boolean wasClicked() { return this.clicked; }
    public void setTitleChangeChannel() { title = CHANGE_CHAN; }
    public void setTitlteOvrDongle() { title = OVR_DONGLE; }
};