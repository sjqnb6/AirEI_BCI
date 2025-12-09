package ControlPanel_;

import GUI.GUIManager;
import Globel.GUI;
import RadioConfig_.RadioConfig;
import controlP5.Button;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlP5;
import processing.core.PApplet;

import static GUI.GGVI.controlPanel;
import static GUI.GGVI.h3;
import static processing.core.PConstants.LEFT;
import static processing.core.PConstants.TOP;

public class RadioConfigBox{
    public int x, y, w, h, padding; //size and position
    String initial_message = "Having trouble connecting to your Cyton? Try Auto-Scan!\n\nUse this tool to get Cyton status or change settings.";
    private String last_message = initial_message;
    public boolean isShowing;
    private RadioConfig cytonRadioCfg;
    private int headerH = 15;
    private int autoscanH = 45;
    private int buttonH = 24;
    private int statusWindowH = 115;
    private ControlP5 rcb_cp5;
    private Button autoscanButton;
    private Button systemStatusButton;
    private Button setChannelButton;
    private Button ovrChannelButton;
    private GUI MAIN;
    RadioConfigBox(GUI MAIN, int _x, int _y, int _w, int _h, int _padding) {
        this.MAIN = MAIN;

        x = _x + _w;
        y = _y;
        w = _w + 10;
        h = (_padding*6) + headerH + (buttonH*2) + autoscanH + statusWindowH;
        padding = _padding;
        isShowing = false;
        cytonRadioCfg = new RadioConfig(MAIN);

        //Instantiate local cp5 for this box
        rcb_cp5 = new ControlP5(MAIN);
        rcb_cp5.setGraphics(MAIN, 0,0);
        rcb_cp5.setAutoDraw(false);

        createAutoscanButton("CytonRadioAutoscan", "AUTO-SCAN",x + padding, y + padding*2 + headerH, w-(padding*2), autoscanH);
        createSystemStatusButton("CytonSystemStatus", "SYSTEM STATUS", x + padding, y + padding*3 + headerH + autoscanH, w-(padding*2), buttonH);
        createSetChannelButton("CytonSetRadioChannel", "CHANGE CHAN.",x + padding, y + padding*4 + headerH + buttonH + autoscanH, (w-padding*3)/2, 24);
        createOverrideChannelButton("CytonOverrideDongleChannel", "OVERRIDE DONGLE", x + 2*padding + (w-padding*3)/2, y + padding*4 + headerH + buttonH + autoscanH, (w-padding*3)/2, buttonH);
    }
    public void update() {}

    public void draw() {
        MAIN.pushStyle();
        MAIN.fill(MAIN.boxColor);
        MAIN.stroke(MAIN.boxStrokeColor);
        MAIN.strokeWeight(1);
        MAIN.rect(x, y, w, h);
        MAIN.fill(MAIN.OPENBCI_DARKBLUE);
        MAIN.textFont(h3, 16);
        MAIN.textAlign(LEFT, TOP);
        MAIN.text("RADIO CONFIGURATION", x + padding, y + padding);
        MAIN.popStyle();

        rcb_cp5.draw();
        this.print_onscreen(last_message);
    }

    public void print_onscreen(String localstring){
        MAIN.pushStyle();
        MAIN.textAlign(LEFT);
        MAIN.fill(MAIN.OPENBCI_DARKBLUE);
        MAIN.rect(x + padding, y + padding*5 + headerH + buttonH*2 + autoscanH, w-(padding*2), statusWindowH);
        MAIN.fill(255);
        MAIN.textFont(h3, 15);
        MAIN.text(localstring, x + padding + 5, y + padding*6 + headerH + buttonH*2 + autoscanH, w - padding*3, statusWindowH - padding);
        MAIN.popStyle();
        this.last_message = localstring;
    }

    public void getChannel() {
        cytonRadioCfg.get_channel(RadioConfigBox.this);
    }

    public void setChannel(int val) {
        cytonRadioCfg.set_channel(RadioConfigBox.this, val);
    }

    public void setChannelOverride(int val) {
        cytonRadioCfg.set_channel_over(RadioConfigBox.this, val);
    }

    public void scanChannels() {
        cytonRadioCfg.scan_channels(RadioConfigBox.this);
    }

    public void getSystemStatus() {
        cytonRadioCfg.system_status(RadioConfigBox.this);
    }

    public void closeSerialPort() {
        print_onscreen("");
        cytonRadioCfg.closeSerialPort();
    }

    private void createAutoscanButton(String name, String text, int _x, int _y, int _w, int _h) {
        autoscanButton = MAIN.createButton(rcb_cp5, name, text, _x, _y, _w, _h);
        autoscanButton.onClick(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                scanChannels();
                controlPanel.hideChannelListCP();
            }
        });
        autoscanButton.setDescription("Scan through channels and connect to a nearby Cyton. This button solves most connection issues!");
    }

    private void createSystemStatusButton(String name, String text, int _x, int _y, int _w, int _h) {
        systemStatusButton = MAIN.createButton(rcb_cp5, name, text, _x, _y, _w, _h);
        systemStatusButton.onClick(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                getChannel();
                controlPanel.hideChannelListCP();
            }
        });
        systemStatusButton.setDescription("Get connection status and the current channel of your Cyton and USB Dongle.");
    }

    private void createSetChannelButton(String name, String text, int _x, int _y, int _w, int _h) {
        setChannelButton = MAIN.createButton(rcb_cp5, name, text, _x, _y, _w, _h);
        setChannelButton.onClick(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                controlPanel.channelPopup.setClicked(true);
                controlPanel.channelPopup.setTitleChangeChannel();
            }
        });
        setChannelButton.setDescription("Change the channel of your Cyton and USB Dongle.");
    }

    private void createOverrideChannelButton(String name, String text, int _x, int _y, int _w, int _h) {
        ovrChannelButton = MAIN.createButton(rcb_cp5, name, text, _x, _y, _w, _h);
        ovrChannelButton.onClick(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                controlPanel.channelPopup.setClicked(true);
                controlPanel.channelPopup.setTitlteOvrDongle();
            }
        });
        ovrChannelButton.setDescription("Change the channel of the USB Dongle only.");
    }
};
