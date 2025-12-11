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

public class InterfaceBoxGanglion{
    public int x, y, w, h, padding; //size and position
    private ControlP5 ifbg_cp5;
    private Button protocolGanglionNativeBLE;
    private Button protocolBLED112Ganglion;
    private Button protocolWifiGanglion;
    private GUI MAIN;
    InterfaceBoxGanglion(GUI MAIN, int _x, int _y, int _w, int _h, int _padding) {
        x = _x;
        y = _y;
        w = _w;
        padding = _padding;
        h = (24 + _padding) * 4;
        int buttonHeight = 24;

        this.MAIN = MAIN;

        //Instantiate local cp5 for this box
        ifbg_cp5 = new ControlP5(MAIN);
        ifbg_cp5.setGraphics(MAIN, 0,0);
        ifbg_cp5.setAutoDraw(false);

        createGanglionNativeBLEButton("protocolNativeBLEGanglion", "Bluetooth (Native)", false, x + padding, y + padding * 3 + 4, w - padding * 2, 24);
        createBLED112Button("protocolBLED112Ganglion", "Bluetooth (BLED112 Dongle)", false, x + padding, y + (padding * 4) + 24 + 4, w - padding * 2, 24);
        createGanglionWifiButton("protocolWifiGanglion", "Wifi (from Wifi Shield)", false, x + padding, y + (padding * 5) + (24 * 2) + 4, w - padding * 2, 24);
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

        ifbg_cp5.draw();
    }


    private Button createIFBGButton(String name, String text, boolean isToggled, int _x, int _y, int _w, int _h) {
        final Button b = MAIN.createButton(ifbg_cp5, name, text, _x, _y, _w, _h);
        b.setSwitch(true); //This turns the button into a switch
        if (isToggled) {
            b.setOn();
        }
        return b;
    }

    private void createGanglionNativeBLEButton(String name, String text, boolean isToggled, int _x, int _y, int _w, int _h) {
        protocolGanglionNativeBLE = createIFBGButton(name, text, isToggled, _x, _y, _w, _h);
        protocolGanglionNativeBLE.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                controlPanel.wifiBox.wifiList.items.clear();
                controlPanel.bleBox.bleList.items.clear();
                MAIN.selectedProtocol = GUI.BoardProtocol.NATIVE_BLE;
                controlPanel.bleBox.refreshGanglionNativeList();
                protocolGanglionNativeBLE.setOn();
                protocolBLED112Ganglion.setOff();
                protocolWifiGanglion.setOff();
            }
        });
    }

    private void createBLED112Button(String name, String text, boolean isToggled, int _x, int _y, int _w, int _h) {
        protocolBLED112Ganglion = createIFBGButton(name, text, isToggled, _x, _y, _w, _h);
        protocolBLED112Ganglion.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                controlPanel.wifiBox.wifiList.items.clear();
                controlPanel.bleBox.bleList.items.clear();
                MAIN.selectedProtocol = GUI.BoardProtocol.BLED112;
                controlPanel.bleBox.refreshGanglionBLEList();
                protocolGanglionNativeBLE.setOff();
                protocolBLED112Ganglion.setOn();
                protocolWifiGanglion.setOff();
            }
        });
    }

    private void createGanglionWifiButton(String name, String text, boolean isToggled, int _x, int _y, int _w, int _h) {
        protocolWifiGanglion = createIFBGButton(name, text, isToggled, _x, _y, _w, _h);
        protocolWifiGanglion.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                controlPanel.wifiBox.wifiList.items.clear();
                controlPanel.bleBox.bleList.items.clear();
                MAIN.selectedProtocol = GUI.BoardProtocol.WIFI;
                protocolGanglionNativeBLE.setOff();
                protocolBLED112Ganglion.setOff();
                protocolWifiGanglion.setOn();
            }
        });
    }

    public void resetGanglionSelectedProtocol() {
        protocolGanglionNativeBLE.setOff();
        protocolBLED112Ganglion.setOff();
        protocolWifiGanglion.setOff();
        MAIN.selectedProtocol = GUI.BoardProtocol.NONE;
    }
};
