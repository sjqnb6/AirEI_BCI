package ControlPanel_;

import Extras_.PlotFontInfo;
import GUI.GUIManager;
import Globel.GUI;

import java.io.File;

import static Debugging_.GVI.helpWidget;
import static GUI.GGVI.*;
import static processing.core.PConstants.CENTER;
import static processing.core.PConstants.TOP;


public class ControlPanel{

    public int x, y, w, h;
    public boolean isOpen;

    PlotFontInfo fontInfo;

    //various control panel elements that are unique to specific datasources
    DataSourceBox dataSourceBox;
    SerialBox serialBox;
    public ComPortBox comPortBox;
    public SessionDataBox dataLogBoxCyton;
    ChannelCountBox channelCountBox;
    public InitBox initBox;
    SyntheticChannelCountBox synthChannelCountBox;
    public RecentPlaybackBox recentPlaybackBox;
    PlaybackFileBox playbackFileBox;
    public StreamingBoardBox streamingBoardBox;
    public BLEBox bleBox;
    public SessionDataBox dataLogBoxGanglion;
    WifiBox wifiBox;
    InterfaceBoxCyton interfaceBoxCyton;
    InterfaceBoxGanglion interfaceBoxGanglion;
    SampleRateCytonBox sampleRateCytonBox;
    SampleRateGanglionBox sampleRateGanglionBox;
    SDBox sdBox;
    BrainFlowStreamerBox bfStreamerBoxCyton;
    BrainFlowStreamerBox bfStreamerBoxGanglion;
    BrainFlowStreamerBox bfStreamerBoxSynthetic;

    ChannelPopup channelPopup;
    RadioConfigBox rcBox;

    //Track Dynamic and Static WiFi mode in Control Panel
    final public String WIFI_DYNAMIC = "dynamic";
    final public String WIFI_STATIC = "static";
    private String wifiSearchStyle = WIFI_DYNAMIC;

    boolean drawStopInstructions;
    int globalPadding; //design feature: passed through to all box classes as the global spacing .. in pixels .. for all elements/subelements
    boolean convertingSD = false;
    private final int PAD_3 = 3;

    private GUI MAIN;

    public ControlPanel(GUI MAIN) {
//        super(pApplet);
        this.MAIN = MAIN;

        x = PAD_3;
        y = PAD_3 + topNav.controlPanelCollapser.getHeight();
        w = topNav.controlPanelCollapser.getWidth();
        h = MAIN.height - (int)(helpWidget.h);

        isOpen = false;
        fontInfo = new PlotFontInfo();

        globalPadding = 10;  //controls the padding of all elements on the control panel

        //boxes active when eegDataSource = Normal (OpenBCI)
        dataSourceBox = new DataSourceBox(MAIN, x, y, w, h, globalPadding);
        interfaceBoxCyton = new InterfaceBoxCyton(MAIN, x + w, dataSourceBox.y, w, h, globalPadding);
        interfaceBoxGanglion = new InterfaceBoxGanglion(MAIN, x + w, dataSourceBox.y, w, h, globalPadding);

        comPortBox = new ComPortBox(MAIN,x+w*2, y, w, h, globalPadding);
        rcBox = new RadioConfigBox(MAIN,x+w, y + comPortBox.h, w, h, globalPadding);

        serialBox = new SerialBox(MAIN,x + w, interfaceBoxCyton.y + interfaceBoxCyton.h, w, h, globalPadding);
        wifiBox = new WifiBox(MAIN, x + w + x + w - 3, interfaceBoxCyton.y, w, h, globalPadding);

        channelCountBox = new ChannelCountBox(MAIN, x + w, (serialBox.y + serialBox.h), w, h, globalPadding);
        dataLogBoxCyton = new SessionDataBox(MAIN,x + w, (channelCountBox.y + channelCountBox.h), w, h, globalPadding, DATASOURCE_CYTON, dataLogger.getDataLoggerOutputFormat(), "sessionNameCyton");
        bfStreamerBoxCyton = new BrainFlowStreamerBox(MAIN,x + w, (dataLogBoxCyton.y + dataLogBoxCyton.h), w, h, globalPadding, "bfStreamerCyton");
        sdBox = new SDBox(MAIN,x + w, (bfStreamerBoxCyton.y + bfStreamerBoxCyton.h), w, h, globalPadding);

        //Draw this to the right of the other cyton boxes
        sampleRateCytonBox = new SampleRateCytonBox(MAIN,wifiBox.x, wifiBox.y + wifiBox.h, w, h, globalPadding);

        //boxes active when eegDataSource = Playback
        int playbackWidth = (int)(w * 1.35);
        playbackFileBox = new PlaybackFileBox(MAIN,x + w, dataSourceBox.y, playbackWidth, h, globalPadding);
        recentPlaybackBox = new RecentPlaybackBox(MAIN,x + w, (playbackFileBox.y + playbackFileBox.h), playbackWidth, h, globalPadding);

        synthChannelCountBox = new SyntheticChannelCountBox(MAIN,x + w, dataSourceBox.y, w, h, globalPadding);
        bfStreamerBoxSynthetic = new BrainFlowStreamerBox(MAIN,x + w, (synthChannelCountBox.y + synthChannelCountBox.h), w, h, globalPadding, "bfStreamerSynthetic");

        streamingBoardBox = new StreamingBoardBox(MAIN,x + w, dataSourceBox.y, w, h, globalPadding);

        channelPopup = new ChannelPopup(MAIN,x+w, y, w, h, globalPadding);

        initBox = new InitBox(MAIN,x, (dataSourceBox.y + dataSourceBox.h), w, h, globalPadding);

        // Ganglion
        bleBox = new BLEBox(MAIN,x + w, interfaceBoxGanglion.y + interfaceBoxGanglion.h, w, h, globalPadding);
        dataLogBoxGanglion = new SessionDataBox(MAIN,x + w, (bleBox.y + bleBox.h), w, h, globalPadding, DATASOURCE_GANGLION, dataLogger.getDataLoggerOutputFormat(), "sessionNameGanglion");
        bfStreamerBoxGanglion = new BrainFlowStreamerBox(MAIN,x + w, (dataLogBoxGanglion.y + dataLogBoxGanglion.h), w, h, globalPadding, "bfStreamerGanglion");
        sampleRateGanglionBox = new SampleRateGanglionBox(MAIN,x + w, (bfStreamerBoxGanglion.y + bfStreamerBoxGanglion.h), w, h, globalPadding);

    }

    public void resetListItems(){
        comPortBox.serialList.activeItem = -1;
        bleBox.bleList.activeItem = -1;
        wifiBox.wifiList.activeItem = -1;
    }

    public void open(){
        isOpen = true;
        topNav.controlPanelCollapser.setOn();
        topNav.setDropdownMenuIsOpen(true);
    }

    public void close(){
        isOpen = false;
        topNav.controlPanelCollapser.setOff();
        topNav.setDropdownMenuIsOpen(false);
    }

    public String getWifiSearchStyle() {
        return wifiSearchStyle;
    }

    void setWiFiSearchStyle(String s) {
        wifiSearchStyle = s;
    }

    public void update() {
        //update all boxes if they need to be
        dataSourceBox.update();
        serialBox.update();
        bleBox.update();
        dataLogBoxCyton.update();
        channelCountBox.update();
        synthChannelCountBox.update();

        //update playback box sizes when dropdown is selected
        recentPlaybackBox.update();
        playbackFileBox.update();

        streamingBoardBox.update();

        bfStreamerBoxCyton.update();
        bfStreamerBoxGanglion.update();
        bfStreamerBoxSynthetic.update();

        sdBox.update();
        rcBox.update();
        comPortBox.update();
        initBox.update();

        channelPopup.update();

        dataLogBoxGanglion.update();

        wifiBox.update();
        interfaceBoxCyton.update();
        interfaceBoxGanglion.update();
    }

    public void draw() {

        initBox.draw();

        if (systemMode == 10) {
            drawStopInstructions = true;
        }

        if (systemMode != 10) { // only draw control panel boxes if system running is false
            dataSourceBox.draw();
            drawStopInstructions = false;

            //Carefully draw certain boxes based on UI/UX flow... let each box handle what is drawn inside with localCp5 instances
            if (eegDataSource == DATASOURCE_CYTON) {	//when data source is from OpenBCI
                interfaceBoxCyton.draw();
                if (selectedProtocol != BoardProtocol.NONE) {
                    if (selectedProtocol == BoardProtocol.SERIAL) {
                        serialBox.y = interfaceBoxCyton.y + interfaceBoxCyton.h;
                        serialBox.draw();
                        channelCountBox.y = serialBox.y + serialBox.h;
                        if (rcBox.isShowing) {
                            comPortBox.draw();
                            rcBox.draw();
                            comPortBox.serialList.setVisible(true);
                            if (channelPopup.wasClicked()) {
                                channelPopup.draw();
                            }
                        }
                    } else if (selectedProtocol == BoardProtocol.WIFI) {
                        wifiBox.y = interfaceBoxCyton.y;
                        wifiBox.x = interfaceBoxCyton.x + interfaceBoxCyton.w;
                        sampleRateCytonBox.y = wifiBox.y + wifiBox.h;
                        channelCountBox.y = interfaceBoxCyton.y + interfaceBoxCyton.h;
                        wifiBox.draw();
                        sampleRateCytonBox.draw();
                    }
                    dataLogBoxCyton.y = channelCountBox.y + channelCountBox.h;
                    bfStreamerBoxCyton.y = dataLogBoxCyton.y + dataLogBoxCyton.h;
                    sdBox.y = bfStreamerBoxCyton.y + bfStreamerBoxCyton.h;
                    channelCountBox.draw();
                    sdBox.draw();
                    bfStreamerBoxCyton.draw();
                    dataLogBoxCyton.draw(); //Drawing here allows max file size dropdown to be drawn on top
                }
            } else if (eegDataSource == DATASOURCE_PLAYBACKFILE) { //when data source is from playback file
                recentPlaybackBox.draw();
                playbackFileBox.draw();
            } else if (eegDataSource == DATASOURCE_SYNTHETIC) {  //synthetic
                synthChannelCountBox.draw();
                bfStreamerBoxSynthetic.draw();
            } else if (eegDataSource == DATASOURCE_GANGLION) {
                if (selectedProtocol == BoardProtocol.NONE) {
                    interfaceBoxGanglion.draw();
                } else {
                    interfaceBoxGanglion.draw();
                    if (selectedProtocol == BoardProtocol.BLED112 || selectedProtocol == BoardProtocol.NATIVE_BLE) {
                        bleBox.y = interfaceBoxGanglion.y + interfaceBoxGanglion.h;
                        dataLogBoxGanglion.y = bleBox.y + bleBox.h;
                        bleBox.draw();
                    } else if (selectedProtocol == BoardProtocol.WIFI) {
                        wifiBox.y = interfaceBoxGanglion.y;
                        wifiBox.x = interfaceBoxGanglion.x + interfaceBoxGanglion.w;
                        sampleRateGanglionBox.y = wifiBox.y + wifiBox.h;
                        sampleRateGanglionBox.x = wifiBox.x;
                        dataLogBoxGanglion.y = interfaceBoxGanglion.y + interfaceBoxGanglion.h;
                        wifiBox.draw();
                        sampleRateGanglionBox.draw();
                    }
                    bfStreamerBoxGanglion.y = dataLogBoxGanglion.y + dataLogBoxGanglion.h;
                    bfStreamerBoxGanglion.draw();
                    dataLogBoxGanglion.draw(); //Drawing here allows max file size dropdown to be drawn on top
                }
            } else if (eegDataSource == DATASOURCE_STREAMING) {
                streamingBoardBox.draw();
            }
        }

        //draw the box that tells you to stop the system in order to edit control settings
        if (drawStopInstructions) {
            MAIN.pushStyle();
            MAIN.fill(MAIN.boxColor);
            MAIN.strokeWeight(1);
            MAIN.stroke(MAIN.boxStrokeColor);
            MAIN.rect(x, y, w, dataSourceBox.h); //draw background of box
            String stopInstructions = "Press the \"STOP SESSION\" button to change your data source or edit system settings.";
            MAIN.textAlign(CENTER, TOP);
            MAIN.textFont(p4, 14);
            MAIN.fill(MAIN.OPENBCI_DARKBLUE);
            MAIN.text(stopInstructions, x + globalPadding*2, y + globalPadding*3, w - globalPadding*4, dataSourceBox.h - globalPadding*4);
            MAIN.popStyle();
        }
    }

    public void hideRadioPopoutBox() {
        rcBox.isShowing = false;
        comPortBox.isShowing = false;
        serialBox.popOutRadioConfigButton.getCaptionLabel().setText("Manual >");
        rcBox.closeSerialPort();
    }

    void hideChannelListCP() {
        channelPopup.setClicked(false);
    }

    public void fetchSessionNameTextfieldAllBoards() {
        String s = "";
        if (eegDataSource == DATASOURCE_CYTON) {
            // Store the current text field value of "Session Name" to be passed along to dataFiles
            s = dataLogBoxCyton.getSessionTextfieldString();
        } else if (eegDataSource == DATASOURCE_GANGLION) {
            s = dataLogBoxGanglion.getSessionTextfieldString();
        } else {
            s = directoryManager.getFileNameDateTime();
        }
        dataLogger.setSessionName(s);
        StringBuilder sb = new StringBuilder(directoryManager.getRecordingsPath());
        sb.append("OpenBCISession_");
        sb.append(dataLogger.getSessionName());
        sb.append(File.separator);
        settings.setSessionPath(sb.toString());
    }

    public void setDataLoggerOutputs() {
        if (eegDataSource == DATASOURCE_CYTON) {
            // Store the current text field value of "Session Name" to be passed along to dataFiles
            dataLogger.setSessionName(controlPanel.dataLogBoxCyton.getSessionTextfieldString());
        } else if (eegDataSource == DATASOURCE_GANGLION) {
            dataLogger.setSessionName(controlPanel.dataLogBoxGanglion.getSessionTextfieldString());
        } else if (eegDataSource == DATASOURCE_SYNTHETIC) {
            dataLogger.setSessionName(directoryManager.getFileNameDateTime());
        }
    }

    public void setBrainFlowStreamerOutput() {
        if (getIsBrainFlowSteamerDefaultFileOutput()) {
            dataLogger.setBfWriterDefaultFolder();
        }

        if (eegDataSource == DATASOURCE_CYTON) {
            brainflowStreamer = bfStreamerBoxCyton.getBrainFlowStreamerString();
        } else if (eegDataSource == DATASOURCE_GANGLION) {
            brainflowStreamer = bfStreamerBoxGanglion.getBrainFlowStreamerString();
        } else if (eegDataSource == DATASOURCE_SYNTHETIC) {
            brainflowStreamer = bfStreamerBoxSynthetic.getBrainFlowStreamerString();
        }
    }

    private boolean getIsBrainFlowSteamerDefaultFileOutput() {
        boolean b = false;
        if (eegDataSource == DATASOURCE_CYTON) {
            b = bfStreamerBoxCyton.getIsBrainFlowStreamerDefaultLocation();
        } else if (eegDataSource == DATASOURCE_GANGLION) {
            b = bfStreamerBoxGanglion.getIsBrainFlowStreamerDefaultLocation();
        } else if (eegDataSource == DATASOURCE_SYNTHETIC) {
            b = bfStreamerBoxSynthetic.getIsBrainFlowStreamerDefaultLocation();
        }
        return b;
    }

}; //end of ControlPanel class
