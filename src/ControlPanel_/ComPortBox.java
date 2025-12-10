package ControlPanel_;

import CustomCp5Classes_.MenuList;
import GUI.GUIManager;
import Globel.GUI;
import RadioConfig_.RadioConfig;
import com.fazecast.jSerialComm.SerialPort;
import controlP5.Button;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlP5;
import processing.core.PApplet;
import processing.core.PFont;

import java.util.LinkedList;
import java.util.Map;

import static Debugging_.GF.*;
import static Extras_.GF.isLinux;
import static Extras_.GF.isMac;
import static Globel.GUI.*;
import static processing.core.PApplet.println;
import static processing.core.PConstants.LEFT;
import static processing.core.PConstants.TOP;

public class ComPortBox{
    public int x, y, w, h, padding; //size and position
    public boolean isShowing;
    private ControlP5 cytoncpb_cp5;
    private Button refreshCytonDongles;
    public MenuList serialList;
    public RadioConfig cytonRadioCfg;
    private boolean midAutoScan = false;
    private boolean midAutoScanCheck2 = false;

    private GUI MAIN;

    public ComPortBox(GUI MAIN, int _x, int _y, int _w, int _h, int _padding) {
//        super(papplet);

        this.MAIN = MAIN;
        x = _x;
        y = _y;
        w = _w + 10;
        h = 140 + _padding;
        padding = _padding;
        isShowing = false;
        cytonRadioCfg = new RadioConfig(MAIN);

        //Instantiate local cp5 for this box
        cytoncpb_cp5 = new ControlP5(MAIN);
        cytoncpb_cp5.setGraphics(MAIN, 0,0);
        cytoncpb_cp5.setAutoDraw(false);

        createRefreshCytonDonglesButton("refreshCytonDonglesButton", "REFRESH LIST", x + padding, y + padding*4 + 72 + 8, w - padding*2, 24);
        createCytonDongleList(cytoncpb_cp5, "cytonDongleList", x + padding, y + padding*3 + 8,  w - padding*2, 72, p3);
    }

    public void update() {
        serialList.updateMenu();
        //Allow two drawing/update cycles to pass so that overlay can be drawn
        //This lets users know that auto-scan is working and GUI is not frozen
        if (midAutoScan) {
            if (midAutoScanCheck2) {
                cytonAutoConnect_AutoScan();
                midAutoScanCheck2 = false;
                midAutoScan = midAutoScanCheck2;
            }
            midAutoScanCheck2 = midAutoScan;
        }
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
        MAIN.text("SERIAL/COM PORT", x + padding, y + padding);
        MAIN.popStyle();

        cytoncpb_cp5.draw();
    }

    private void createRefreshCytonDonglesButton(String name, String text, int _x, int _y, int _w, int _h) {
        refreshCytonDongles = MAIN.createButton(cytoncpb_cp5, name, text, _x, _y, _w, _h);
        refreshCytonDongles.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                refreshPortListCyton();
            }
        });
    }

    private void createCytonDongleList(ControlP5 _cp5, String name, int _x, int _y, int _w, int _h, PFont font) {
        serialList = new MenuList(_cp5, name, _w, _h, font, MAIN);
        serialList.setPosition(_x, _y);
        serialList.addCallback(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                if (theEvent.getAction() == ControlP5.ACTION_BROADCAST) {
                    Map bob = serialList.getItem((int)(serialList.getValue()));
                    openBCI_portName = (String)bob.get("subline");
                    output("ControlPanel: Selected OpenBCI Port " + openBCI_portName);
                }
            }
        });
    }

    //This is called when the Auto-Connect button is pressed in another Control Panel Box
    public void attemptAutoConnectCyton() {
        println("\n-------------------------------------------------\nControlPanel: Attempting to Auto-Connect to Cyton\n-------------------------------------------------\n");
        LinkedList<String> comPorts = getCytonComPorts();
        if (!comPorts.isEmpty()) {
            openBCI_portName = comPorts.getFirst();
            if (cytonRadioCfg.get_channel()) {
                controlPanel.initBox.initButtonPressed();
            } else {
                outputWarn("Found a Cyton dongle, but could not connect to the board. Auto-Scanning now...");
                midAutoScan = true;
            }
        } else {
            outputWarn("No Cyton dongles were found.");
        }
    }

    //If Cyton dongle exists, and fails to connect, try to Auto-Scan in the background to align Cyton/Dongle Channel
    //This is called after overlay has a chance to draw on top to inform users the GUI is working and not crashed
    private void cytonAutoConnect_AutoScan() {
        if (cytonRadioCfg.scan_channels()) {
            println("Successfully connected to Cyton using " + openBCI_portName);
            controlPanel.initBox.initButtonPressed();
        } else {
            outputError("Unable to connect to Cyton. Please check hardware and power source.");
        }
    }

    //Refresh the Cyton Dongle list
    public void refreshPortListCyton(){
        serialList.items.clear();

        Thread thread = new Thread(){
            public void run(){
                refreshCytonDongles.getCaptionLabel().setText("SEARCHING...");

                LinkedList<String> comPorts = getCytonComPorts();
                for (String comPort : comPorts) {
                    serialList.addItem("(Cyton) " + comPort, comPort, "");
                }
                serialList.updateMenu();
                refreshCytonDongles.getCaptionLabel().setText("REFRESH LIST");
            }
        };

        thread.start();
    }

    private LinkedList<String> getCytonComPorts() {
        final String[] names = {"FT231X USB UART", "VCP"};
        final SerialPort[] comPorts = SerialPort.getCommPorts();
        LinkedList<String> results = new LinkedList<String>();
        for (SerialPort comPort : comPorts) {
            for (String name : names) {
                if (comPort.toString().startsWith(name)) {
                    // on macos need to drop tty ports
                    if (isMac() && comPort.getSystemPortName().startsWith("tty")) {
                        continue;
                    }
                    String found = "";
                    if (isMac() || isLinux()) found += "/dev/";
                    found += comPort.getSystemPortName();
                    println("ControlPanel: Found Cyton Dongle on COM port: " + found);
                    results.add(found);
                }
            }
        }

        return results;
    }

    public boolean isAutoScanningForCytonSerial() {
        return midAutoScan;
    }
};
