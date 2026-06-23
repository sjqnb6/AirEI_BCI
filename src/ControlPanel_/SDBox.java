package ControlPanel_;

import BoardCyton_.CytonSDMode;
import GUI.GUIManager;
import Globel.GUI;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlP5;
import controlP5.ScrollableList;
import processing.core.PApplet;

import java.util.Map;

import static Debugging_.GF.output;
import static Debugging_.GF.verbosePrint;
import static Globel.GUI.*;
import static processing.core.PConstants.LEFT;
import static processing.core.PConstants.TOP;

public class SDBox{
    final String sdBoxDropdownName = "sdCardTimes";
    public int x, y, w, h, padding; //size and position
    ControlP5 cp5_sdBox;
    private ScrollableList sdList;
    private int prevY;
    private GUI MAIN;
    SDBox(GUI MAIN, int _x, int _y, int _w, int _h, int _padding) {
        x = _x;
        y = _y;
        w = _w;
        h = 73;
        padding = _padding;
        prevY = y;
        this.MAIN = MAIN;

        cp5_sdBox = new ControlP5(MAIN);
        cp5_sdBox.setGraphics(MAIN, 0,0);
        cp5_sdBox.setAutoDraw(false);

        createDropdown(sdBoxDropdownName);

        updatePosition();
        sdList.setSize(w - padding*2, ((int)((sdList.getItems().size()+1)/1.5)) * 24);
    }

    public void update() {
        if (y != prevY) { //When box's absolute y position changes, update cp5
            updatePosition();
            prevY = y;
        }
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
        MAIN.text("是否写入SD卡?", x + padding, y + padding);
        //draw backgrounds to dropdown scrollableLists ... unfortunately ControlP5 doesn't have this by default, so we have to hack it to make it look nice...
        MAIN.popStyle();

        MAIN.pushStyle();
        MAIN.fill(150);
        MAIN.popStyle();
        cp5_sdBox.draw();
    }

    private void createDropdown(String name){

        sdList = cp5_sdBox.addScrollableList(name)
                .setOpen(false)
                .setColor(MAIN.settings.dropdownColors)
                .setOutlineColor(150)
                .setSize(w - padding*2, 2*24)//temporary size
                .setBarHeight(24) //height of top/primary bar
                .setItemHeight(24) //height of all item/dropdown bars
                .setVisible(true)
        ;
        // for each entry in the enum, add it to the dropdown.
        for (CytonSDMode mode : CytonSDMode.values()) {
            // this will store the *actual* enum object inside the dropdown!
            sdList.addItem(mode.getName(), mode);
        }
        sdList.getCaptionLabel() //the caption label is the text object in the primary bar
                .toUpperCase(false) //DO NOT AUTOSET TO UPPERCASE!!!
                .setText(CytonSDMode.NO_WRITE.getName())
                .setFont(p7)
                .setSize(14)
                .getStyle() //need to grab style before affecting the paddingTop
                .setPaddingTop(4)
        ;
        sdList.getValueLabel() //the value label is connected to the text objects in the dropdown item bars
                .toUpperCase(false) //DO NOT AUTOSET TO UPPERCASE!!!
                .setText(CytonSDMode.NO_WRITE.getName())
                .setFont(p7)
                .setSize(12) //set the font size of the item bars to 14pt
                .getStyle() //need to grab style before affecting the paddingTop
                .setPaddingTop(3) //4-pixel vertical offset to center text
        ;
        sdList.addCallback(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                if (theEvent.getAction() == ControlP5.ACTION_BROADCAST) {
                    int val = (int)sdList.getValue();
                    Map bob = sdList.getItem(val);
                    cyton_sdSetting = (CytonSDMode)bob.get("value");
                    String outputString = "AirEIBCI microSD Setting = " + cyton_sdSetting.getName();
                    if (cyton_sdSetting != CytonSDMode.NO_WRITE) {
                        outputString += " recording time";
                    }
                    output(outputString);
                    verbosePrint("SD Command = " + cyton_sdSetting.getCommand());
                }
            }
        });
    }

    public void updatePosition() {
        sdList.setPosition(x + padding, y + padding*2 + 14);
    }
};
