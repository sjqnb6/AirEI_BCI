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
import static Globel.GUI.*;
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

        createStartSessionButton("startSessionButton", "开始连接", x + padding, y + padding, w-padding*2, h - padding*2);
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
        if (getInitSessionButtonText().equals("开始连接")) {
            if ((eegDataSource == DATASOURCE_CYTON && MAIN.selectedProtocol == BoardProtocol.NONE) || (eegDataSource == DATASOURCE_GANGLION && MAIN.selectedProtocol == BoardProtocol.NONE)) {
                outputWarn("未选择传输协议。请选择您的传输协议并重试系统启动。");
                return;
            } else if (eegDataSource == DATASOURCE_CYTON && MAIN.selectedProtocol == BoardProtocol.SERIAL && openBCI_portName == "N/A") { //if data source == normal && if no serial port selected OR no SD setting selected
                outputWarn("未选择串口/COM端口。尝试自动连接Cyton。");
                controlPanel.comPortBox.attemptAutoConnectCyton();
                return;
            } else if (eegDataSource == DATASOURCE_CYTON && MAIN.selectedProtocol == BoardProtocol.WIFI && false && wifi_portName == "N/A" && controlPanel.getWifiSearchStyle() == controlPanel.WIFI_DYNAMIC) {
                outputWarn("没有选择Wifi。请选择您的 WiFi 并重试系统启动。");
                return;
            } else if (eegDataSource == DATASOURCE_PLAYBACKFILE && playbackData_fname == "N/A" && sdData_fname == "N/A") { //if data source == playback && playback file == 'N/A'
                outputWarn("没有选择回放文件。请选择回放文件并重试系统启动。");        // tell user that they need to select a file before the system can be started
                return;
            } else if (eegDataSource == DATASOURCE_GANGLION && (MAIN.selectedProtocol == BoardProtocol.NATIVE_BLE || MAIN.selectedProtocol == BoardProtocol.BLED112) && ganglion_portName == "N/A") {
                outputWarn("未选中BLE设备。请选择您的Ganglion设备并重试系统启动。");
                return;
            } else if (eegDataSource == DATASOURCE_GANGLION && MAIN.selectedProtocol == BoardProtocol.WIFI && false && wifi_portName == "N/A" && controlPanel.getWifiSearchStyle() == controlPanel.WIFI_DYNAMIC) {
                outputWarn("没有选择Wifi。请选择您的 WiFi 并重试系统启动。");
                return;
            } else if (eegDataSource == -1) {//if no data source selected
                outputWarn("未选择数据来源。请选择一个数据源并重试系统启动。");//tell user they must select a data source before initiating system
                return;
            } else { //otherwise, initiate system!
                //verbosePrint("ControlPanel: CPmouseReleased: init");
                setInitSessionButtonText("停止连接");
                // Global steps to START SESSION
                // Prepare the serial port

                //设置数据记录器输出以将数据保存到 BDF 或 CSV
                controlPanel.setDataLoggerOutputs();

                if (controlPanel.getWifiSearchStyle() == controlPanel.WIFI_STATIC && (MAIN.selectedProtocol == BoardProtocol.WIFI || MAIN.selectedProtocol == BoardProtocol.WIFI)) {
                    wifi_ipAddress = controlPanel.wifiBox.staticIPAddressTF.getText();
                    println("Static IP address of " + wifi_ipAddress);
                }
                if (MAIN.selectedProtocol == BoardProtocol.WIFI && (wifi_ipAddress == null || wifi_ipAddress.isEmpty() || "N/A".equals(wifi_ipAddress))) {
                    wifi_ipAddress = "192.168.4.1";
                    println("Using default WiFi IP address: " + wifi_ipAddress);
                }

                //将此标志设置为 true，并在下一个 draw() 循环之后将“正在开始会话...”绘制到屏幕上
                midInit = true;
                output("尝试建立连接..."); // Show this at the bottom of the GUI
                println("initButtonPressed: Calling initSystem() after next draw()");
            }
        } else {
            //if system is already active ... stop session and flip button state back
            setInitSessionButtonText("开始连接");
            topNav.setLockTopLeftSubNavCp5Objects(false); //Unlock top left subnav buttons
            //creates new data file name so that you don't accidentally overwrite the old one
            controlPanel.dataLogBoxCyton.setSessionTextfieldText(directoryManager.getFileNameDateTime());
            controlPanel.dataLogBoxGanglion.setSessionTextfieldText(directoryManager.getFileNameDateTime());
            controlPanel.wifiBox.setStaticIPTextfield(wifi_ipAddress);
            w_focus.killAuditoryFeedback();
            haltSystem(MAIN);
        }
    }

    public String getInitSessionButtonText() {
        return initSystemButton.getCaptionLabel().getText();
    }

    public void setInitSessionButtonText(String text) {
        initSystemButton.getCaptionLabel().setText(text);
    }
};
