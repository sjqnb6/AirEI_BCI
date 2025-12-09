package ControlPanel_;

import GUI.GUIManager;
import Globel.GUI;
import controlP5.Button;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlP5;
import processing.core.PApplet;

import static Debugging_.GF.output;
import static Debugging_.GF.outputWarn;
import static GUI.GGVI.*;
import static SystemManager.GF.haltSystem;
import static WidgetManager_.GVI.w_focus;
import static processing.core.PApplet.println;

public class InitBox{
    public int x, y, w, h, padding; //size and position
    private ControlP5 initBox_cp5;
    public Button initSystemButton;
    private GUI MAIN;
    InitBox(GUI MAIN, int _x, int _y, int _w, int _h, int _padding) {
        this.MAIN = MAIN;
        x = _x;
        y = _y;
        w = _w;
        h = 50;
        padding = _padding;

        //Instantiate local cp5 for this box
        initBox_cp5 = new ControlP5(MAIN);
        initBox_cp5.setGraphics(MAIN, 0,0);
        initBox_cp5.setAutoDraw(false);

        createStartSessionButton("startSessionButton", "START SESSION", x + padding, y + padding, w-padding*2, h - padding*2);
    }

    public void update() {
    }

    public void draw() {
        MAIN.pushStyle();
        MAIN.fill(MAIN.boxColor);
        MAIN.stroke(MAIN.boxStrokeColor);
        MAIN.strokeWeight(1);
        MAIN.rect(x, y, w, h);
        MAIN.popStyle();

        initBox_cp5.draw();
    }

    private void createStartSessionButton(String name, String text, int _x, int _y, int _w, int _h) {
        initSystemButton = MAIN.createButton(initBox_cp5, name, text, _x, _y, _w, _h);
        initSystemButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                if (controlPanel.rcBox.isShowing) {
                    controlPanel.hideRadioPopoutBox();
                }
                //If session is not active, start session and flip button state
                initButtonPressed();
            }
        });
    }

    //This is the primary method called when Start/Stop Session Button is pressed in Control Panel
    public void initButtonPressed() {
        if (getInitSessionButtonText().equals("START SESSION")) {
            if ((eegDataSource == DATASOURCE_CYTON && selectedProtocol == BoardProtocol.NONE) || (eegDataSource == DATASOURCE_GANGLION && selectedProtocol == BoardProtocol.NONE)) {
                outputWarn("No Transfer Protocol selected. Please select your Transfer Protocol and retry system initiation.");
                return;
            } else if (eegDataSource == DATASOURCE_CYTON && selectedProtocol == BoardProtocol.SERIAL && openBCI_portName == "N/A") { //if data source == normal && if no serial port selected OR no SD setting selected
                outputWarn("No Serial/COM port selected. Attempting to AUTO-CONNECT to Cyton.");
                controlPanel.comPortBox.attemptAutoConnectCyton();
                return;
            } else if (eegDataSource == DATASOURCE_CYTON && selectedProtocol == BoardProtocol.WIFI && wifi_portName == "N/A" && controlPanel.getWifiSearchStyle() == controlPanel.WIFI_DYNAMIC) {
                outputWarn("No Wifi Shield selected. Please select your Wifi Shield and retry system initiation.");
                return;
            } else if (eegDataSource == DATASOURCE_PLAYBACKFILE && playbackData_fname == "N/A" && sdData_fname == "N/A") { //if data source == playback && playback file == 'N/A'
                outputWarn("No playback file selected. Please select a playback file and retry system initiation.");        // tell user that they need to select a file before the system can be started
                return;
            } else if (eegDataSource == DATASOURCE_GANGLION && (selectedProtocol == BoardProtocol.NATIVE_BLE || selectedProtocol == BoardProtocol.BLED112) && ganglion_portName == "N/A") {
                outputWarn("No BLE device selected. Please select your Ganglion device and retry system initiation.");
                return;
            } else if (eegDataSource == DATASOURCE_GANGLION && selectedProtocol == BoardProtocol.WIFI && wifi_portName == "N/A" && controlPanel.getWifiSearchStyle() == controlPanel.WIFI_DYNAMIC) {
                outputWarn("No Wifi Shield selected. Please select your Wifi Shield and retry system initiation.");
                return;
            } else if (eegDataSource == -1) {//if no data source selected
                outputWarn("No DATA SOURCE selected. Please select a DATA SOURCE and retry system initiation.");//tell user they must select a data source before initiating system
                return;
            } else { //otherwise, initiate system!
                //verbosePrint("ControlPanel: CPmouseReleased: init");
                setInitSessionButtonText("STOP SESSION");
                // Global steps to START SESSION
                // Prepare the serial port

                //Set data logger outputs to save data to BDF or CSV
                controlPanel.setDataLoggerOutputs();

                if (controlPanel.getWifiSearchStyle() == controlPanel.WIFI_STATIC && (selectedProtocol == BoardProtocol.WIFI || selectedProtocol == BoardProtocol.WIFI)) {
                    wifi_ipAddress = controlPanel.wifiBox.staticIPAddressTF.getText();
                    println("Static IP address of " + wifi_ipAddress);
                }

                //Set this flag to true, and draw "Starting Session..." to screen after then next draw() loop
                midInit = true;
                output("Attempting to Start Session..."); // Show this at the bottom of the GUI
                println("initButtonPressed: Calling initSystem() after next draw()");
            }
        } else {
            //if system is already active ... stop session and flip button state back
            setInitSessionButtonText("START SESSION");
            topNav.setLockTopLeftSubNavCp5Objects(false); //Unlock top left subnav buttons
            //creates new data file name so that you don't accidentally overwrite the old one
            controlPanel.dataLogBoxCyton.setSessionTextfieldText(directoryManager.getFileNameDateTime());
            controlPanel.dataLogBoxGanglion.setSessionTextfieldText(directoryManager.getFileNameDateTime());
            controlPanel.wifiBox.setStaticIPTextfield(wifi_ipAddress);
            w_focus.killAuditoryFeedback();
            haltSystem();
        }
    }

    public String getInitSessionButtonText() {
        return initSystemButton.getCaptionLabel().getText();
    }

    public void setInitSessionButtonText(String text) {
        initSystemButton.getCaptionLabel().setText(text);
    }
};
