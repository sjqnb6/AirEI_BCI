package ControlPanel_;

import BoardBrainFlowStreaming_.BrainFlowStreaming_Boards;
import GUI.GUIManager;
import Globel.GUI;
import controlP5.*;
import processing.core.PApplet;

import java.util.Map;

import static Globel.GUI.*;
import static processing.core.PConstants.LEFT;
import static processing.core.PConstants.TOP;

public class StreamingBoardBox{
    public int x, y, w, h, padding; //size and position
    private final String boxLabel = "STREAMING BOARD CONFIG";
    private final String ipLabel = "IP";
    private final String portLabel = "PORT";
    private final String boardLabel = "BOARD";
    private ControlP5 localCP5;
    private ScrollableList boardIdList;
    private Textfield ipAddress;
    private Textfield port;
    private final int headerH = 14;
    private final int objectH = 24;
    private GUI MAIN;
    StreamingBoardBox(GUI MAIN, int _x, int _y, int _w, int _h, int _padding) {
        this.MAIN = MAIN;
        x = _x;
        y = _y;
        w = _w - _padding;
        h = headerH + objectH*2 + _padding*4;
        padding = _padding;
        localCP5 = new ControlP5(MAIN);
        localCP5.setGraphics(MAIN, 0,0);
        localCP5.setAutoDraw(false); //Setting this saves code as cp5 elements will only be drawn/visible when [cp5].draw() is called

        ipAddress = localCP5.addTextfield("ipAddress")
                .setPosition(x + padding * 3, y + headerH + padding*2)
                .setCaptionLabel("")
                .setSize(w / 3, objectH)
                .setFont(f2)
                .setFocus(false)
                .setColor(MAIN.color(26, 26, 26))
                .setColorBackground(MAIN.color(255, 255, 255)) // text field bg color
                .setColorValueLabel(MAIN.OPENBCI_DARKBLUE)  // text color
                .setColorForeground(MAIN.OPENBCI_DARKBLUE)  // border color when not selected
                .setColorActive(MAIN.isSelected_color)  // border color when selected
                .setColorCursor(MAIN.color(26, 26, 26))
                .setText("") //default ipAddress == ""
                .align(5, 10, 20, 40)
                .onDoublePress(cb)
                .setAutoClear(true);

        port = localCP5.addTextfield("port")
                .setPosition(x + padding*5 + w/2, y + headerH + padding*2)
                .setCaptionLabel("")
                .setSize(w / 5 + padding, objectH)
                .setFont(f2)
                .setFocus(false)
                .setColor(MAIN.color(26, 26, 26))
                .setColorBackground(MAIN.color(255, 255, 255)) // text field bg color
                .setColorValueLabel(MAIN.OPENBCI_DARKBLUE)  // text color
                .setColorForeground(MAIN.OPENBCI_DARKBLUE)  // border color when not selected
                .setColorActive(MAIN.isSelected_color)  // border color when selected
                .setColorCursor(MAIN.color(26, 26, 26))
                .setText(Integer.toString(0)) //default port == 0
                .align(5, 10, 20, 40)
                .onDoublePress(cb)
                .setAutoClear(true);

        boardIdList = createDropdown("streamingBoard_IDs", BrainFlowStreaming_Boards.values());
        boardIdList.setPosition(x + 48 + padding*2, y + headerH + padding*3 + objectH);
        boardIdList.setSize(170, (boardIdList.getItems().size()+1)*objectH);
    }

    public void update() {
        copyPaste.checkForCopyPaste(ipAddress);
        copyPaste.checkForCopyPaste(port);
    }

    public void draw() {
        MAIN.pushStyle();
        MAIN.fill(MAIN.boxColor);
        MAIN.stroke(MAIN.boxStrokeColor);
        MAIN.strokeWeight(1);
        MAIN.rect(x, y, w, h);
        MAIN.popStyle();

        MAIN.pushStyle();
        MAIN.fill(MAIN.OPENBCI_DARKBLUE);
        MAIN.textFont(h3, 16);
        MAIN.textAlign(LEFT, TOP);
        //draw text labels
        MAIN.text(boxLabel, x + padding, y + padding);
        MAIN.textAlign(LEFT, TOP);
        MAIN.textFont(p4, 14);
        MAIN.text(ipLabel, x + padding, y + padding*2 + headerH + 4);
        MAIN.text(portLabel, x + w/2, y + padding*2 + headerH + 4);
        MAIN.text(boardLabel, x + padding, y + padding*3 + objectH + headerH + 4);
        MAIN.popStyle();

        //draw cp5 last, on top of everything in this box
        localCP5.draw();
    }

    private ScrollableList createDropdown(String name, BrainFlowStreaming_Boards[] enumValues){
        ScrollableList list = localCP5.addScrollableList(name)
                .setOpen(false)
                .setColorBackground(MAIN.OPENBCI_BLUE) // text field bg color
                .setColorValueLabel(MAIN.color(255))       // text color
                .setColorCaptionLabel(MAIN.color(255))
                .setColorForeground(MAIN.color(125))    // border color when not selected
                .setColorActive(MAIN.BUTTON_PRESSED)       // border color when selected
                .setOutlineColor(150)
                .setSize(w - padding*2, objectH)//temporary size
                .setBarHeight(objectH) //height of top/primary bar
                .setItemHeight(objectH) //height of all item/dropdown bars
                .setVisible(true)
                ;
        // for each entry in the enum, add it to the dropdown.
        for (BrainFlowStreaming_Boards value : enumValues) {
            // this will store the *actual* enum object inside the dropdown!
            list.addItem(value.getName(), value);
        }
        //Style the text in the ScrollableList
        list.getCaptionLabel() //the caption label is the text object in the primary bar
                .toUpperCase(false) //DO NOT AUTOSET TO UPPERCASE!!!
                .setText(enumValues[0].getName())
                .setFont(h4)
                .setSize(14)
                .getStyle() //need to grab style before affecting the paddingTop
                .setPaddingTop(4)
        ;
        list.getValueLabel() //the value label is connected to the text objects in the dropdown item bars
                .toUpperCase(false) //DO NOT AUTOSET TO UPPERCASE!!!
                .setText(enumValues[0].getName())
                .setFont(h5)
                .setSize(12) //set the font size of the item bars to 14pt
                .getStyle() //need to grab style before affecting the paddingTop
                .setPaddingTop(3) //4-pixel vertical offset to center text
        ;
        return list;
    }

    public BrainFlowStreaming_Boards getBoard() {
        int val = (int)boardIdList.getValue();
        Map bob = boardIdList.getItem(val);
        // this will retrieve the enum object stored in the dropdown!
        return (BrainFlowStreaming_Boards)bob.get("value");
    }

    public String getIP() {
        return ipAddress.getText();
    }

    public int getPort() {
        return Integer.parseInt(port.getText());
    }

    //Clear text field on double-click
    CallbackListener cb = new CallbackListener() {
        public void controlEvent(CallbackEvent theEvent) {
            ((Textfield)(theEvent.getController())).clear();
        }
    };
};
