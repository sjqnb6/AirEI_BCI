package ControlPanel_;

import GUI.GUIManager;
import Globel.GUI;
import controlP5.Button;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlP5;
import processing.core.PApplet;

import static Globel.GUI.controlPanel;
import static Globel.GUI.h3;
import static processing.core.PConstants.LEFT;
import static processing.core.PConstants.TOP;

public class InterfaceBoxCyton{
    public int x, y, w, h, padding; //size and position
    private ControlP5 ifbc_cp5;
    private Button protocolSerialCyton;
    private Button protocolWifiCyton;
    private GUI MAIN;
    InterfaceBoxCyton(GUI MAIN, int _x, int _y, int _w, int _h, int _padding) {
        x = _x;
        y = _y;
        w = _w;
        h = (24 + _padding) * 3;
        padding = _padding;
        this.MAIN = MAIN;

        //Instantiate local cp5 for this box
        ifbc_cp5 = new ControlP5(MAIN);
        ifbc_cp5.setGraphics(MAIN, 0,0);
        ifbc_cp5.setAutoDraw(false);

        //Disabled both toggles by default for this box
        createSerialCytonButton("protocolSerialCyton", "Serial (from Dongle)", false, x + padding, y + padding * 3 + 4, w - padding * 2, 24);
        createWifiCytonButton("protocolWifiCyton", "Wifi (from Wifi Shield)", false, x + padding, y + padding * 4 + 24 + 4, w - padding * 2, 24);
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
        MAIN.text("PICK TRANSFER PROTOCOL", x + padding, y + padding);
        MAIN.popStyle();

        ifbc_cp5.draw();
    }

    public Button createIFBCButton(String name, String text, boolean isToggled, int _x, int _y, int _w, int _h) {
        final Button b = MAIN.createButton(ifbc_cp5, name, text, _x, _y, _w, _h);
        b.setSwitch(true); //This turns the button into a switch
        if (isToggled) {
            b.setOn();
        }
        return b;
    }

    public void createSerialCytonButton(String name, String text, boolean isToggled, int _x, int _y, int _w, int _h) {
        protocolSerialCyton = createIFBCButton(name, text, isToggled, _x, _y, _w, _h);
        protocolSerialCyton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                controlPanel.wifiBox.wifiList.items.clear();
                controlPanel.bleBox.bleList.items.clear();
                MAIN.selectedProtocol = GUI.BoardProtocol.SERIAL;
                controlPanel.comPortBox.refreshPortListCyton();
                protocolSerialCyton.setOn();
                protocolWifiCyton.setOff();
            }
        });
    }

    public void createWifiCytonButton(String name, String text, boolean isToggled, int _x, int _y, int _w, int _h) {
        protocolWifiCyton = createIFBCButton(name, text, isToggled, _x, _y, _w, _h);
        protocolWifiCyton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                controlPanel.wifiBox.wifiList.items.clear();
                controlPanel.bleBox.bleList.items.clear();
                MAIN.selectedProtocol = GUI.BoardProtocol.WIFI;
                protocolSerialCyton.setOff();
                protocolWifiCyton.setOn();
            }
        });
    }

    public void resetCytonSelectedProtocol() {
        protocolSerialCyton.setOff();
        protocolWifiCyton.setOff();
        MAIN.selectedProtocol = GUI.BoardProtocol.NONE;
    }
};
