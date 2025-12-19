package ControlPanel_;

import DataWriterBF_.DataWriterBFEnum;
import GUI.GUIManager;
import Globel.GUI;
import controlP5.*;
import processing.core.PApplet;
import processing.core.PConstants;

import java.io.File;
import java.util.Map;

import static Debugging_.GF.output;
import static Globel.GUI.*;

public class BrainFlowStreamerBox{
    public int x, y, w, h, padding; //size and position
    private ControlP5 bfStreamerCp5;
    private int maxDurTextWidth = 82;
    private int maxDurText_x = 0;
    private Textfield ipAddress;
    private Textfield port;
    private Button autoSessionName;
    private Button outputToNetwork;
    private Button outputToFile;
    private ScrollableList bfFileSaveOption;
    private DataWriterBFEnum dataWriterBfEnum = DataWriterBFEnum.DEFAULT;
    private final int HEADER_H = 14;
    private final int OBJECT_H = 24;
    private final String DEFAULT_IP_ADDRESS = "225.1.1.1";
    private final String DEFAULT_PORT = "6677";

    private GUI MAIN;

    BrainFlowStreamerBox (GUI MAIN, int _x, int _y, int _w, int _h, int _padding, String textfieldName) {
        x = _x;
        y = _y;
        w = _w;
        h = HEADER_H + OBJECT_H*2 + _padding*4;
        padding = _padding;

        this.MAIN = MAIN;

        //Instantiate local cp5 for this box
        bfStreamerCp5 = new ControlP5(MAIN);
        bfStreamerCp5.setGraphics(MAIN, 0,0);
        bfStreamerCp5.setAutoDraw(false);

        createDropdown("bfFileSaveOption");
        createNetworkTextfields();

        //button to autogenerate file name based on time/date
        createStreamNetworkButton("networkButton", "网络流", x + padding, y + 32, (w-padding*3)/2, OBJECT_H);
        createStreamFileButton("fileButton", "文件", x + padding*2 + (w-padding*3)/2, y + 32, (w-padding*3)/2, OBJECT_H);
    }

    public void update() {
        copyPaste.checkForCopyPaste(ipAddress);
        copyPaste.checkForCopyPaste(port);
    }

    public void draw() {
        int streamerTextfieldY = y + padding*3 + HEADER_H + OBJECT_H;

        bfFileSaveOption.setVisible(outputToFile.isOn());
        ipAddress.setVisible(outputToNetwork.isOn());
        port.setVisible(outputToNetwork.isOn());

        MAIN.pushStyle();
        MAIN.fill(MAIN.boxColor);
        MAIN.stroke(MAIN.boxStrokeColor);
        MAIN.strokeWeight(1);
        MAIN.rect(x, y, w, h);
        MAIN.fill(MAIN.OPENBCI_DARKBLUE);
        MAIN.textFont(p7, 16);
        MAIN.textAlign(PConstants.LEFT, PConstants.TOP);
        MAIN.text("BRAINFLOW 数据流", x + padding, y + padding);
        MAIN.textFont(p7, 14);
        if (outputToFile.isOn()) {
            MAIN.text("文件路径", x + padding, streamerTextfieldY + 2);
        } else if (outputToNetwork.isOn()) {
            MAIN.text("IP", x + padding, streamerTextfieldY + 2);
            MAIN.text("端口", x + w - padding*2 - port.getWidth() - 14 - padding, streamerTextfieldY + 2);
        }
        MAIN.popStyle();

        //Update the position of UI elements here
        outputToNetwork.setPosition(x + padding, y + HEADER_H + padding*2);
        outputToFile.setPosition(x + padding*2 + (w-padding*3)/2, y + HEADER_H + padding*2);
        bfFileSaveOption.setPosition(x + 80, streamerTextfieldY);
        ipAddress.setPosition(x + padding * 3, streamerTextfieldY);
        port.setPosition(x + w - padding - port.getWidth(), streamerTextfieldY);

        bfStreamerCp5.draw();
    }

    private void createNetworkTextfields() {
        ipAddress = bfStreamerCp5.addTextfield("ipAddress")
                .setPosition(x + padding * 3, y + HEADER_H + padding*2)
                .setCaptionLabel("")
                .setSize(120, OBJECT_H)
                .setFont(f2)
                .setFocus(false)
                .setColor(MAIN.color(26, 26, 26))
                .setColorBackground(MAIN.color(255, 255, 255)) // text field bg color
                .setColorValueLabel(MAIN.OPENBCI_DARKBLUE)  // text color
                .setColorForeground(MAIN.OPENBCI_DARKBLUE)  // border color when not selected
                .setColorActive(MAIN.isSelected_color)  // border color when selected
                .setColorCursor(MAIN.color(26, 26, 26))
                .setText(DEFAULT_IP_ADDRESS) //default ipAddress == ""
                .align(5, 10, 20, 40)
                //.onDoublePress(cb)
                .addCallback(new CallbackListener() {
                    public void controlEvent(CallbackEvent theEvent) {
                        if (theEvent.getAction() == ControlP5.ACTION_BROADCAST && ipAddress.getText().equals("")) {
                            ipAddress.setText(DEFAULT_IP_ADDRESS);
                        }
                    }
                })
                .onReleaseOutside(new CallbackListener() {
                    public void controlEvent(CallbackEvent theEvent) {
                        if (!ipAddress.isActive() && ipAddress.getText().equals("")) {
                            ipAddress.setText(DEFAULT_IP_ADDRESS);
                        }
                    }
                })
                .onDoublePress(cb);

        port = bfStreamerCp5.addTextfield("port")
                .setPosition(x + padding*5 + w/2, y + HEADER_H + padding*2)
                .setCaptionLabel("")
                .setSize(50, OBJECT_H)
                .setFont(f2)
                .setFocus(false)
                .setColor(MAIN.color(26, 26, 26))
                .setColorBackground(MAIN.color(255, 255, 255)) // text field bg color
                .setColorValueLabel(MAIN.OPENBCI_DARKBLUE)  // text color
                .setColorForeground(MAIN.OPENBCI_DARKBLUE)  // border color when not selected
                .setColorActive(MAIN.isSelected_color)  // border color when selected
                .setColorCursor(MAIN.color(26, 26, 26))
                .setText(DEFAULT_PORT) //default port == 0
                .align(5, 10, 20, 40)
                //.onDoublePress(cb)
                .addCallback(new CallbackListener() {
                    public void controlEvent(CallbackEvent theEvent) {
                        if (theEvent.getAction() == ControlP5.ACTION_BROADCAST && port.getText().equals("")) {
                            port.setText(DEFAULT_PORT);
                        }
                    }
                })
                .onReleaseOutside(new CallbackListener() {
                    public void controlEvent(CallbackEvent theEvent) {
                        if (!port.isActive() && port.getText().equals("")) {
                            port.setText(DEFAULT_PORT);
                        }
                    }
                })
                .onDoublePress(cb);
    }

    private Button createBrainFlowOutputToggle(String name, String text, boolean isToggled, int _x, int _y, int _w, int _h) {
        final Button b = MAIN.createButton(bfStreamerCp5, name, text, _x, _y, _w, _h);
        b.setSwitch(true); //This turns the button into a switch
        if (isToggled) {
            b.setOn();
        }
        return b;
    }

    private void createStreamNetworkButton(String name, String text, int _x, int _y, int _w, int _h) {
        outputToNetwork = createBrainFlowOutputToggle(name, text, false, _x, _y, _w, _h);
        outputToNetwork.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                //output(odfMessage);
                //dataLogger.setDataLoggerOutputFormat(dataLogger.OUTPUT_SOURCE_ODF);
                outputToNetwork.setOn();
                outputToFile.setOff();
                //setToODFHeight();
            }
        });
        outputToNetwork.setDescription("通过 BrainFlow Streamer 向网络地址输出数据流。支持使用任意 BrainFlow 语言绑定的外部进程接收数据，适用于开发者进行扩展开发。");
    }

    private void createStreamFileButton(String name, String text, int _x, int _y, int _w, int _h) {
        outputToFile = createBrainFlowOutputToggle(name, text, true, _x, _y, _w, _h);
        outputToFile.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                //output(bdfMessage);
                //dataLogger.setDataLoggerOutputFormat(dataLogger.OUTPUT_SOURCE_BDF);
                outputToNetwork.setOff();
                outputToFile.setOn();
                //setToBDFHeight();
            }
        });
        outputToFile.setDescription("将 BrainFlow Streamer 设置为文件输出模式。");
    }

    private void createDropdown(String name){
        bfFileSaveOption = bfStreamerCp5.addScrollableList(name)
                .setOpen(false)
                .setColor(MAIN.settings.dropdownColors)
                .setOutlineColor(150)
                .setSize(167, (dataWriterBfEnum.values().length + 1) * 24)
                .setBarHeight(24) //height of top/primary bar
                .setItemHeight(24) //height of all item/dropdown bars
                .setVisible(true)
        ;
        for (DataWriterBFEnum value : dataWriterBfEnum.values()) {
            // this will store the *actual* enum object inside the dropdown!
            bfFileSaveOption.addItem(value.getString(), value);
        }
        bfFileSaveOption.getCaptionLabel() //the caption label is the text object in the primary bar
                .toUpperCase(false) //DO NOT AUTOSET TO UPPERCASE!!!
                .setText(dataWriterBfEnum.getString())
                .setFont(p7)
                .setSize(14)
                .getStyle() //need to grab style before affecting the paddingTop
                .setPaddingTop(4)
        ;
        bfFileSaveOption.getValueLabel() //the value label is connected to the text objects in the dropdown item bars
                .toUpperCase(false) //DO NOT AUTOSET TO UPPERCASE!!!
                .setText(dataWriterBfEnum.getString())
                .setFont(p7)
                .setSize(12) //set the font size of the item bars to 14pt
                .getStyle() //need to grab style before affecting the paddingTop
                .setPaddingTop(3) //4-pixel vertical offset to center text
        ;
        bfFileSaveOption.addCallback(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                if (theEvent.getAction() == ControlP5.ACTION_BROADCAST) {
                    int val = (int)(theEvent.getController()).getValue();
                    Map bob = ((ScrollableList)theEvent.getController()).getItem(val);
                    dataWriterBfEnum = (DataWriterBFEnum)bob.get("value");
                    StringBuilder sb = new StringBuilder("BrainFlow 文件流: 用户已选中 ");
                    sb.append(dataWriterBfEnum.getString());
                    sb.append(" file location.");
                    output(sb.toString());
                    if (dataWriterBfEnum.getIsCustomLocation()) {
                        MAIN.selectOutput("选择一个文件夹来保存 BrainFlow CSV 文件：",
                                "bfSelectedFolder",
                                new File(directoryManager.getRecordingsPath())
                        );
                    }
                } else if (theEvent.getAction() == ControlP5.ACTION_ENTER) {
                    lockOutsideElements(true);
                } else if (theEvent.getAction() == ControlP5.ACTION_LEAVE) {
                    lockOutsideElements(false);
                }
            }
        });
        bfFileSaveOption.setPosition(x + 10, y + 10); //Set arbitrary position to start, gets reset on every draw
    }

    private String getBFNetworkTextfieldsAsString() {
        StringBuilder sb = new StringBuilder("streaming_board://");
        sb.append(ipAddress.getText());
        sb.append(":");
        sb.append(port.getText());
        return sb.toString();
    }

    private String getBFFileLocationAsString() {
        if (dataLogger.getBfWriterFilePath() == null || dataWriterBfEnum.getIsTurnedOff()) {
            return null;
        }
        return dataLogger.getBfWriterFilePath();
    }

    public String getBrainFlowStreamerString() {
        String s = outputToNetwork.isOn() ? getBFNetworkTextfieldsAsString() : getBFFileLocationAsString();
        return s;
    }

    public boolean getIsBrainFlowStreamerDefaultLocation() {
        return outputToFile.isOn() && dataWriterBfEnum.getIsDefaultLocation();
    }

    // True locks elements, False unlocks elements
    private void lockOutsideElements (boolean _toggle) {
        if (eegDataSource == DATASOURCE_CYTON) {
            //Cyton for Serial and WiFi (WiFi details are drawn to the right, so no need to lock)
            controlPanel.channelCountBox.lockCp5Objects(_toggle);
            if (_toggle) {
                controlPanel.sdBox.cp5_sdBox.get(ScrollableList.class, controlPanel.sdBox.sdBoxDropdownName).lock();
            } else {
                controlPanel.sdBox.cp5_sdBox.get(ScrollableList.class, controlPanel.sdBox.sdBoxDropdownName).unlock();
            }
            controlPanel.sdBox.cp5_sdBox.get(ScrollableList.class, controlPanel.sdBox.sdBoxDropdownName).setUpdate(!_toggle);
        }
    }

    public void lockSessionDataBoxCp5Elements(boolean b) {
        ipAddress.setLock(b);
        port.setLock(b);
    }

    //Clear text field on double-click
    CallbackListener cb = new CallbackListener() {
        public void controlEvent(CallbackEvent theEvent) {
            Textfield tf = ((Textfield)theEvent.getController());
            tf.clear();
        }
    };
};
