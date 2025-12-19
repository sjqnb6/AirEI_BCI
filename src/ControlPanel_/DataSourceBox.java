package ControlPanel_;

import CustomCp5Classes_.MenuList;
import GUI.GUIManager;
import Globel.GUI;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlP5;
import processing.core.PFont;

import java.util.Map;

//import static GUI.GGVI.*;
import static Globel.GUI.*;
import static SystemManager.GF.updateToNChan;
import static processing.core.PConstants.LEFT;
import static processing.core.PConstants.TOP;

public class DataSourceBox{
    public int x, y, w, h, padding; //size and position
    private int numItems;
    private int boxHeight = 24;
    private int spacing = 43;
    private ControlP5 datasource_cp5;
    private MenuList sourceList;

    private GUI MAIN;
    public DataSourceBox(GUI MAIN, int _x, int _y, int _w, int _h, int _padding) {
        this.MAIN = MAIN;


        numItems = 5;
        x = _x;
        y = _y;
        w = _w;
        h = spacing + (numItems * boxHeight);
        padding = _padding;

        //Instantiate local cp5 for this box
        datasource_cp5 = new ControlP5(MAIN);
        datasource_cp5.setGraphics(MAIN, 0,0);
        datasource_cp5.setAutoDraw(false);
        createDatasourceList(datasource_cp5, "sourceList", x + padding, y + padding*2 + 13, w - padding*2, numItems * boxHeight, p7);
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
        MAIN.textFont(p7, 16);
        MAIN.textAlign(LEFT, TOP);
        MAIN.text("数据库", x + padding, y + padding);
        MAIN.popStyle();

        datasource_cp5.draw();
    }

    private void createDatasourceList(ControlP5 _cp5, String name, int _x, int _y, int _w, int _h, PFont font) {
        sourceList = new MenuList(_cp5, name, _w, _h, font, MAIN);
        sourceList.setPosition(_x, _y);
        // sourceList.itemHeight = 28;
        // sourceList.padding = 9;
        sourceList.addItem("CYTON (在线采集)", DATASOURCE_CYTON);
        sourceList.addItem("GANGLION (在线采集)", DATASOURCE_GANGLION);
        sourceList.addItem("文件回放", DATASOURCE_PLAYBACKFILE);
        sourceList.addItem("仿真数据", DATASOURCE_SYNTHETIC);
        sourceList.addItem("外部流入", DATASOURCE_STREAMING);
        sourceList.scrollerLength = 10;
        sourceList.addCallback(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                if (theEvent.getAction() == ControlP5.ACTION_BROADCAST) {
                    Map bob = sourceList.getItem((int)(sourceList.getValue()));
                    String str = (String)bob.get("headline"); // Get the text displayed in the MenuList
                    int newDataSource = (int)bob.get("value");
                    MAIN.settings.controlEventDataSource = str; //Used for output message on system start
                    eegDataSource = newDataSource;

                    //Reset protocol
                    MAIN.selectedProtocol = BoardProtocol.NONE;

                    //Perform this check in a way that ignores order of items in the menulist
                    if (eegDataSource == DATASOURCE_CYTON) {
                        controlPanel.channelCountBox.set8ChanButtonActive();
                        controlPanel.interfaceBoxCyton.resetCytonSelectedProtocol();
                        controlPanel.wifiBox.setDefaultToDynamicIP();
                    } else if (eegDataSource == DATASOURCE_GANGLION) {
                        updateToNChan(MAIN, 4);
                        controlPanel.interfaceBoxGanglion.resetGanglionSelectedProtocol();
                        controlPanel.wifiBox.setDefaultToDynamicIP();
                    } else if (eegDataSource == DATASOURCE_PLAYBACKFILE) {
                        //GUI auto detects number of channels for playback when file is selected
                    } else if (eegDataSource == DATASOURCE_STREAMING) {
                        //do nothing for now
                    } else if (eegDataSource == DATASOURCE_SYNTHETIC) {
                        controlPanel.synthChannelCountBox.set8ChanButtonActive();
                    }
                }
            }
        });
    }
};
