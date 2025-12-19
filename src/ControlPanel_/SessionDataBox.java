package ControlPanel_;

import GUI.GUIManager;
import Globel.GUI;
import controlP5.*;
import processing.core.PApplet;

import java.util.Arrays;
import java.util.List;

import static Debugging_.GF.output;
import static Globel.GUI.*;
import static processing.core.PApplet.println;
import static processing.core.PConstants.LEFT;
import static processing.core.PConstants.TOP;

public class SessionDataBox{
    public int x, y, w, h, padding; //size and position
    private int datasource;
    private final int bdfModeHeight = 127;
    private int odfModeHeight;

    private ControlP5 sessionData_cp5;
    private int maxDurTextWidth = 82;
    private int maxDurText_x = 0;
    private Textfield sessionNameTextfield;
    private Button autoSessionName;
    private Button outputODF;
    private Button outputBDF;
    private ScrollableList maxDurationDropdown;
    private String odfMessage = "Output has been set to OpenBCI Data Format (CSV).";
    private String bdfMessage = "Output has been set to BioSemi Data Format (BDF+).";
    private GUI MAIN;
    SessionDataBox (GUI MAIN, int _x, int _y, int _w, int _h, int _padding, int _dataSource, int output, String textfieldName) {
        this.MAIN = MAIN;
        datasource = _dataSource;
        odfModeHeight = bdfModeHeight + 24 + _padding;
        x = _x;
        y = _y;
        w = _w;
        h = odfModeHeight;
        padding = _padding;
        maxDurText_x = x + padding;
        maxDurTextWidth += padding*5 + 1;

        //Instantiate local cp5 for this box
        sessionData_cp5 = new ControlP5(MAIN);
        sessionData_cp5.setGraphics(MAIN, 0,0);
        sessionData_cp5.setAutoDraw(false);

        createSessionNameTextfield(textfieldName);

        //button to autogenerate file name based on time/date
        createAutoSessionNameButton("autoSessionName", "生成会话名称", x + padding, y + 66, w-(padding*2), 24);
        createODFButton("odfButton", "OpenBCI", dataLogger.getDataLoggerOutputFormat(), x + padding, y + padding*2 + 18 + 58, (w-padding*3)/2, 24);
        createBDFButton("bdfButton", "BDF+", dataLogger.getDataLoggerOutputFormat(), x + padding*2 + (w-padding*3)/2, y + padding*2 + 18 + 58, (w-padding*3)/2, 24);

        createMaxDurationDropdown("maxFileDuration", Arrays.asList(MAIN.settings.fileDurations));

    }

    public void update() {
        copyPaste.checkForCopyPaste(sessionNameTextfield);
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
        MAIN.text("会话数据", x + padding, y + padding);
        MAIN.textFont(p7, 14);
        MAIN.text("名称", x + padding, y + padding*2 + 14);
        MAIN.popStyle();

        //Update the position of UI elements here, as this changes when user selects WiFi mode
        sessionNameTextfield.setPosition(x + 60, y + 32);
        autoSessionName.setPosition(x + padding, y + 66);
        outputODF.setPosition(x + padding, y + padding*2 + 18 + 58);
        outputBDF.setPosition(x + padding*2 + (w-padding*3)/2, y + padding*2 + 18 + 58);
        maxDurationDropdown.setPosition(x + maxDurTextWidth, (int)(outputODF.getPosition()[1]) + 24 + padding);

        boolean odfIsSelected = dataLogger.getDataLoggerOutputFormat() == dataLogger.OUTPUT_SOURCE_ODF;
        maxDurationDropdown.setVisible(odfIsSelected);

        if (odfIsSelected) {
            MAIN.pushStyle();
            //draw backgrounds to dropdown scrollableLists ... unfortunately ControlP5 doesn't have this by default, so we have to hack it to make it look nice...
            //Dropdown is drawn at the end of ControlPanel.draw()
            MAIN.fill(MAIN.OPENBCI_DARKBLUE);
            maxDurationDropdown.setPosition(x + maxDurTextWidth, (int)(outputODF.getPosition()[1]) + 24 + padding);
            //Carefully draw some text to the left of above dropdown, otherwise this text moves when changing WiFi mode
            int extraPadding = 20;
            MAIN.fill(MAIN.OPENBCI_DARKBLUE);
            MAIN.textFont(p7, 14);
            MAIN.text("最大文件时长", maxDurText_x, y + h - 24 - padding + extraPadding);
            MAIN.popStyle();
        }
        sessionData_cp5.draw();
    }

    private void createSessionNameTextfield(String name) {
        //Create textfield to allow user to type custom session folder name
        sessionNameTextfield = sessionData_cp5.addTextfield(name)
                .setPosition(x + 60, y + 32)
                .setCaptionLabel("")
                .setSize(187, 26)
                .setFont(f2)
                .setFocus(false)
                .setColor(MAIN.color(26, 26, 26))
                .setColorBackground(MAIN.color(255, 255, 255)) // text field bg color
                .setColorValueLabel(MAIN.OPENBCI_DARKBLUE)  // text color
                .setColorForeground(MAIN.OPENBCI_DARKBLUE)  // border color when not selected
                .setColorActive(MAIN.isSelected_color)  // border color when selected
                .setColorCursor(MAIN.color(26, 26, 26))
                .setText(directoryManager.getFileNameDateTime())
                .align(5, 10, 20, 40)
                .setAutoClear(false); //Don't clear textfield when pressing Enter key
        //Clear textfield on double click
        sessionNameTextfield.onDoublePress(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                output("SessionData: Enter your custom session name.");
                sessionNameTextfield.clear();
            }
        });
        //Autogenerate session name if user presses Enter key and textfield value is null
        sessionNameTextfield.addCallback(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                if (theEvent.getAction() == ControlP5.ACTION_BROADCAST && sessionNameTextfield.getText().equals("")) {
                    autogenerateSessionName();
                }
            }
        });
        //Autogenerate session name if user leaves textfield and value is null
        sessionNameTextfield.onReleaseOutside(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                if (!sessionNameTextfield.isActive() && sessionNameTextfield.getText().equals("")) {
                    autogenerateSessionName();
                }
            }
        });
    }

    private void createMaxDurationDropdown(String name, List<String> _items){
        maxDurationDropdown = sessionData_cp5.addScrollableList(name)
                .setOpen(false)
                .setColor(MAIN.settings.dropdownColors)
                .setOutlineColor(150)
                //.setColorBackground(OPENBCI_BLUE) // text field bg color
                .setColorValueLabel(MAIN.OPENBCI_DARKBLUE)       // text color
                //.setColorCaptionLabel(color(255))
                //.setColorForeground(color(125))    // border color when not selected
                //.setColorActive(BUTTON_PRESSED)       // border color when selected
                // .setColorCursor(color(26,26,26))
                .setPosition(x + maxDurTextWidth, (int)(outputODF.getPosition()[1]) + 24 + padding)
            .setSize((w-padding*3)/2, (_items.size() + 1) * 24)// + maxFreqList.size())
                .setBarHeight(24) //height of top/primary bar
                .setItemHeight(24) //height of all item/dropdown bars
                .addItems(_items) // used to be .addItems(maxFreqList)
                .setVisible(false)
        ;
        maxDurationDropdown
                .getCaptionLabel() //the caption label is the text object in the primary bar
                .toUpperCase(false) //DO NOT AUTOSET TO UPPERCASE!!!
                .setText(MAIN.settings.fileDurations[MAIN.settings.defaultOBCIMaxFileSize])
                .setFont(p4)
                .setSize(14)
                .getStyle() //need to grab style before affecting the paddingTop
                .setPaddingTop(4)
        ;
        maxDurationDropdown
                .getValueLabel() //the value label is connected to the text objects in the dropdown item bars
                .toUpperCase(false) //DO NOT AUTOSET TO UPPERCASE!!!
                .setText(MAIN.settings.fileDurations[MAIN.settings.defaultOBCIMaxFileSize])
                .setFont(h5)
                .setSize(12) //set the font size of the item bars to 14pt
                .getStyle() //need to grab style before affecting the paddingTop
                .setPaddingTop(3) //4-pixel vertical offset to center text
        ;
        maxDurationDropdown.addCallback(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                if (theEvent.getAction() == ControlP5.ACTION_BROADCAST) {
                    int n = (int)(theEvent.getController()).getValue();
                    MAIN.settings.setLogFileDurationChoice(n);
                    println("ControlPanel: Chosen Recording Duration: " + n);
                } else if (theEvent.getAction() == ControlP5.ACTION_ENTER) {
                    lockOutsideElements(true);
                } else if (theEvent.getAction() == ControlP5.ACTION_LEAVE) {
                    ScrollableList theList = (ScrollableList)(theEvent.getController());
                    lockOutsideElements(theList.isOpen());
                }
            }
        });
    }

    private Button createGUIOutputToggle(String name, String text, boolean isToggled, int _x, int _y, int _w, int _h) {
        final Button b = MAIN.createButton(sessionData_cp5, name, text, _x, _y, _w, _h);
        b.setSwitch(true); //This turns the button into a switch
        if (isToggled) {
            b.setOn();
        }
        return b;
    }

    private void createAutoSessionNameButton(String name, String text, int _x, int _y, int _w, int _h) {
        autoSessionName = MAIN.createButton(sessionData_cp5, name, text, _x, _y, _w, _h);
        autoSessionName.onClick(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                autogenerateSessionName();
            }
        });
        autoSessionName.setDescription("Autogenerate a session name based on the date and time.");
    }

    private void createODFButton(String name, String text, int dataLoggerFormat, int _x, int _y, int _w, int _h) {
        boolean formatIsODF = dataLoggerFormat == dataLogger.OUTPUT_SOURCE_ODF;
        outputODF = createGUIOutputToggle(name, text, formatIsODF, _x, _y, _w, _h);
        outputODF.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                output(odfMessage);
                dataLogger.setDataLoggerOutputFormat(dataLogger.OUTPUT_SOURCE_ODF);
                outputODF.setOn();
                outputBDF.setOff();
                setToODFHeight();
            }
        });
        outputODF.setDescription("Set GUI data output to OpenBCI Data Format (.txt). A new file will be made in the session folder when the data stream is paused or max file duration is reached.");
    }

    private void createBDFButton(String name, String text, int dataLoggerFormat, int _x, int _y, int _w, int _h) {
        boolean formatIsBDF = dataLoggerFormat == dataLogger.OUTPUT_SOURCE_BDF;
        outputBDF = createGUIOutputToggle(name, text, formatIsBDF, _x, _y, _w, _h);
        outputBDF.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                output(bdfMessage);
                dataLogger.setDataLoggerOutputFormat(dataLogger.OUTPUT_SOURCE_BDF);
                outputBDF.setOn();
                outputODF.setOff();
                setToBDFHeight();
            }
        });
        outputBDF.setDescription("Set GUI data output to BioSemi Data Format (.bdf). All session data is contained in one .bdf file. View using an EDF/BDF browser.");
    }

    private void autogenerateSessionName() {
        output("Autogenerated Session Name based on current date & time.");
        sessionNameTextfield.setText(directoryManager.getFileNameDateTime());
    }

    public void setToODFHeight() {
        h = odfModeHeight;
    }

    public void setToBDFHeight() {
        h = bdfModeHeight;
    }

    public String getSessionTextfieldString() {
        return sessionNameTextfield.getText();
    }

    public void setSessionTextfieldText(String s) {
        sessionNameTextfield.setText(s);
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
        } else {
            controlPanel.sampleRateGanglionBox.lockCp5Objects(_toggle);
        }
    }

    public void lockSessionDataBoxCp5Elements(boolean b) {
        sessionNameTextfield.setLock(b);
        autoSessionName.setLock(b);
        outputODF.setLock(b);
        outputBDF.setLock(b);
    }
};