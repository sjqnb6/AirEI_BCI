package ControlPanel_;

import GUI.GUIManager;
import Globel.GUI;
import controlP5.Button;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlP5;
import processing.core.PApplet;

import static Globel.GUI.*;
import static processing.core.PConstants.LEFT;
import static processing.core.PConstants.TOP;

class SerialBox{
    public int x, y, w, h, padding; //size and position
    private ControlP5 cytonsb_cp5;
    private Button autoConnectButton;
    Button popOutRadioConfigButton;
    private GUI MAIN;
    SerialBox(GUI MAIN, int _x, int _y, int _w, int _h, int _padding) {
        x = _x;
        y = _y;
        w = _w;
        h = 70;
        padding = _padding;
        this.MAIN = MAIN;
        //Instantiate local cp5 for this box
        cytonsb_cp5 = new ControlP5(MAIN);
        cytonsb_cp5.setGraphics(MAIN, 0,0);
        cytonsb_cp5.setAutoDraw(false);

        createAutoConnectButton("cytonAutoConnectButton", "AUTO-CONNECT", x + padding, y + padding*3 + 4, w - padding*3 - 70, 24);
        createRadioConfigButton("cytonRadioConfigButton", "Manual >", x + w - 70 - padding, y + padding*3 + 4, 70, 24);
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
        MAIN.text("SERIAL CONNECT", x + padding, y + padding);
        MAIN.popStyle();

        if (MAIN.selectedProtocol == GUI.BoardProtocol.SERIAL) {
            cytonsb_cp5.draw();
        }
    }

    private Button createSBButton(String name, String text, int _x, int _y, int _w, int _h) {
        return MAIN.createButton(cytonsb_cp5, name, text, _x, _y, _w, _h, 0, p5, 12, MAIN.colorNotPressed, MAIN.OPENBCI_DARKBLUE, MAIN.BUTTON_HOVER, MAIN.BUTTON_PRESSED, MAIN.OPENBCI_DARKBLUE, 0);
    }

    private void createAutoConnectButton(String name, String text, int _x, int _y, int _w, int _h) {
        autoConnectButton = createSBButton(name, text, _x, _y, _w, _h);
        autoConnectButton.setColorBackground(MAIN.TURN_ON_GREEN);
        autoConnectButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                controlPanel.comPortBox.attemptAutoConnectCyton();
            }
        });
        autoConnectButton.setDescription("Attempt to auto-connect to Cyton. Try \"Manual\" if this does not work.");
    }

    private void createRadioConfigButton(String name, String text, int _x, int _y, int _w, int _h) {
        popOutRadioConfigButton = createSBButton(name, text, _x, _y, _w, _h);
        popOutRadioConfigButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                if (MAIN.selectedProtocol == GUI.BoardProtocol.SERIAL) {
                    if (controlPanel.rcBox.isShowing) {
                        controlPanel.hideRadioPopoutBox();
                    } else {
                        controlPanel.rcBox.isShowing = true;
                        controlPanel.rcBox.print_onscreen(controlPanel.rcBox.initial_message);
                        popOutRadioConfigButton.getCaptionLabel().setText("Manual <");
                    }
                }
            }
        });
        popOutRadioConfigButton.setDescription("Having trouble connecting to Cyton? Click here to access Radio Configuration tools.");
    }
};
