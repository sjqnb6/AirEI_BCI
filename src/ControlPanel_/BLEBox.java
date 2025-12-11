package ControlPanel_;

import CustomCp5Classes_.MenuList;
import GUI.GUIManager;
import Globel.GUI;
import com.fazecast.jSerialComm.SerialPort;
import controlP5.Button;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlP5;
import openbci_gui_helpers.GUIHelper;
import openbci_gui_helpers.GanglionError;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PFont;

import java.util.HashMap;
import java.util.Map;

import static Debugging_.GF.output;
import static Debugging_.GF.outputError;
import static Extras_.GF.isLinux;
import static Extras_.GF.isMac;
import static Globel.GUI.*;

public class BLEBox{
    public int x, y, w, h, padding; //size and position
    private volatile boolean bleIsRefreshing = false;
    private ControlP5 bleBox_cp5;
    public MenuList bleList;
    private Button refreshBLE;
    public Map<String, String> bleMACAddrMap = new HashMap<String, String>();

    private GUI MAIN;

    public BLEBox(GUI MAIN, int _x, int _y, int _w, int _h, int _padding) {
        x = _x;
        y = _y;
        w = _w;
        h = 140 + _padding;
        padding = _padding;

        this.MAIN = MAIN;

        //Instantiate local cp5 for this box
        bleBox_cp5 = new ControlP5(MAIN);
        bleBox_cp5.setGraphics(MAIN, 0,0);
        bleBox_cp5.setAutoDraw(false);

        createRefreshBLEButton("refreshGanglionBLEButton", "START SEARCH", x + padding, y + padding*4 + 72 + 8, w - padding*5, 24);
        createGanglionBLEMenuList(bleBox_cp5, "bleList", x + padding, y + padding*3 + 8, w - padding*2, 72, p3);
    }

    public void update() {
        bleList.updateMenu();
    }

    public void draw() {
        MAIN.pushStyle();
        MAIN.fill(MAIN.boxColor);
        MAIN.stroke(MAIN.boxStrokeColor);
        MAIN.strokeWeight(1);
        MAIN.rect(x, y, w, h);
        MAIN.fill(MAIN.OPENBCI_DARKBLUE);
        MAIN.textFont(h3, 16);
        MAIN.textAlign(PConstants.LEFT, PConstants.TOP);
        MAIN.text("BLE DEVICES", x + padding, y + padding);
        MAIN.popStyle();

        if (bleIsRefreshing) {
            //Display spinning cog gif
            MAIN.image(loadingGIF_blue, w + 225,  refreshBLE.getPosition()[1] + 4, 20, 20);
        } else {
            //Draw small grey circle
            MAIN.pushStyle();
            MAIN.fill(0X999999);
            MAIN.ellipseMode(PConstants.CENTER);
            MAIN.ellipse(w + 225 + 10, refreshBLE.getPosition()[1] + 12, 12, 12);
            MAIN.popStyle();
        }

        bleBox_cp5.draw();
    }

    public void refreshGanglionNativeList() {
        if (bleIsRefreshing) {
            output("Search for Ganglions using Native Bluetooth is in progress.");
            return;
        }
        output("Refreshing available Ganglions using Native Bluetooth...");
        bleList.items.clear();

        Thread thread = new Thread(){
            public void run(){
                refreshBLE.getCaptionLabel().setText("SEARCHING...");
                bleIsRefreshing = true;

                try {
                    bleMACAddrMap = GUIHelper.scan_for_ganglions (3);
                    for (Map.Entry<String, String> entry : bleMACAddrMap.entrySet ())
                    {
                        bleList.addItem(entry.getKey(),  entry.getValue(), "");
                        bleList.updateMenu();
                        PApplet.println("Found Ganglion Board: " + entry.getKey() + " " + entry.getValue());
                    }
                } catch (GanglionError e)
                {
                    e.printStackTrace();
                }

                refreshBLE.getCaptionLabel().setText("START SEARCH");
                bleIsRefreshing = false;
            }
        };

        thread.start();
    }

    void refreshGanglionBLEList() {
        if (bleIsRefreshing) {
            output("Search for Ganglions using BLED112 Dongle is in progress.");
            return;
        }
        output("Refreshing available Ganglions using BLED112 Dongle...");
        bleList.items.clear();

        Thread thread = new Thread(){
            public void run(){
                refreshBLE.getCaptionLabel().setText("SEARCHING...");
                bleIsRefreshing = true;
                final String comPort = getBLED112Port();
                if (comPort != null) {
                    try {
                        bleMACAddrMap = GUIHelper.scan_for_ganglions (comPort, 3);
                        for (Map.Entry<String, String> entry : bleMACAddrMap.entrySet ())
                        {
                            bleList.addItem(entry.getKey(), comPort, "");
                            bleList.updateMenu();
                        }
                    } catch (GanglionError e)
                    {
                        e.printStackTrace();
                    }
                } else {
                    outputError("No BLED112 Dongle Found");
                }
                refreshBLE.getCaptionLabel().setText("START SEARCH");
                bleIsRefreshing = false;
            }
        };

        thread.start();
    }

    public String getBLED112Port() {
        String name = "Low Energy Dongle";
        SerialPort[] comPorts = SerialPort.getCommPorts();
        for (int i = 0; i < comPorts.length; i++) {
            if (comPorts[i].toString().equals(name)) {
                String found = "";
                if (isMac() || isLinux()) found += "/dev/";
                found += comPorts[i].getSystemPortName().toString();
                PApplet.println("ControlPanel: Found BLED112 Dongle on COM port: " + found);
                return found;
            }
        }
        return null;
    }

    private void createRefreshBLEButton(String name, String text, int _x, int _y, int _w, int _h) {
        refreshBLE = MAIN.createButton(bleBox_cp5, name, text, _x, _y, _w, _h);
        refreshBLE.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                if (MAIN.selectedProtocol == BoardProtocol.BLED112) {
                    refreshGanglionBLEList();
                } else {
                    refreshGanglionNativeList();
                }
            }
        });
    }

    private void createGanglionBLEMenuList(ControlP5 _cp5, String name, int _x, int _y, int _w, int _h, PFont font) {
        bleList = new MenuList(_cp5, name, _w, _h, font, MAIN);
        bleList.setPosition(_x, _y);
        bleList.addCallback(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                if (theEvent.getAction() == ControlP5.ACTION_BROADCAST) {
                    Map bob = bleList.getItem((int)(bleList.getValue()));
                    ganglion_portName = (String)bob.get("headline");
                    output("Ganglion Device Name = " + ganglion_portName);
                }
            }
        });
    }
};
