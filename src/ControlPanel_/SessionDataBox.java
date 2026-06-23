package ControlPanel_;

import Globel.GUI;
import controlP5.Button;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlP5;
import controlP5.ScrollableList;
import controlP5.Textfield;

import java.util.Arrays;
import java.util.List;

import static Debugging_.GF.output;
import static Globel.GUI.*;
import static processing.core.PApplet.println;
import static processing.core.PConstants.LEFT;
import static processing.core.PConstants.TOP;

public class SessionDataBox {
    public int x, y, w, h, padding;
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
    private String odfMessage = "输出设置为 AirEIBCI 数据格式（CSV）。";
    private String bdfMessage = "输出设置为 BioSemi 数据格式（BDF+）。";
    private GUI MAIN;

    SessionDataBox(GUI MAIN, int _x, int _y, int _w, int _h, int _padding, int _dataSource, int output, String textfieldName) {
        this.MAIN = MAIN;
        datasource = _dataSource;
        odfModeHeight = bdfModeHeight + 24 + _padding;
        x = _x;
        y = _y;
        w = _w;
        h = odfModeHeight;
        padding = _padding;
        maxDurText_x = x + padding;
        maxDurTextWidth += padding * 5 + 1;

        sessionData_cp5 = new ControlP5(MAIN);
        sessionData_cp5.setGraphics(MAIN, 0, 0);
        sessionData_cp5.setAutoDraw(false);

        createSessionNameTextfield(textfieldName);
        createAutoSessionNameButton("autoSessionName", "生成会话名称", x + padding, y + 66, w - (padding * 2), 24);
        createODFButton("odfButton", "AirEIBCI", dataLogger.getDataLoggerOutputFormat(), x + padding, y + padding * 2 + 18 + 58, (w - padding * 3) / 2, 24);
        createBDFButton("bdfButton", "BDF+", dataLogger.getDataLoggerOutputFormat(), x + padding * 2 + (w - padding * 3) / 2, y + padding * 2 + 18 + 58, (w - padding * 3) / 2, 24);
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
        MAIN.text("名称", x + padding, y + padding * 2 + 14);
        MAIN.popStyle();

        sessionNameTextfield.setPosition(x + 60, y + 32);
        autoSessionName.setPosition(x + padding, y + 66);
        outputODF.setPosition(x + padding, y + padding * 2 + 18 + 58);
        outputBDF.setPosition(x + padding * 2 + (w - padding * 3) / 2, y + padding * 2 + 18 + 58);
        maxDurationDropdown.setPosition(x + maxDurTextWidth, (int) (outputODF.getPosition()[1]) + 24 + padding);

        boolean odfIsSelected = dataLogger.getDataLoggerOutputFormat() == dataLogger.OUTPUT_SOURCE_ODF;
        maxDurationDropdown.setVisible(odfIsSelected);

        if (odfIsSelected) {
            MAIN.pushStyle();
            MAIN.fill(MAIN.OPENBCI_DARKBLUE);
            maxDurationDropdown.setPosition(x + maxDurTextWidth, (int) (outputODF.getPosition()[1]) + 24 + padding);
            int extraPadding = 20;
            MAIN.fill(MAIN.OPENBCI_DARKBLUE);
            MAIN.textFont(p7, 14);
            MAIN.text("最大文件时长", maxDurText_x, y + h - 24 - padding + extraPadding);
            MAIN.popStyle();
        }
        sessionData_cp5.draw();
    }

    private void createSessionNameTextfield(String name) {
        sessionNameTextfield = sessionData_cp5.addTextfield(name)
            .setPosition(x + 60, y + 32)
            .setCaptionLabel("")
            .setSize(187, 26)
            .setFont(f2)
            .setFocus(false)
            .setColor(MAIN.color(26, 26, 26))
            .setColorBackground(MAIN.color(255, 255, 255))
            .setColorValueLabel(MAIN.OPENBCI_DARKBLUE)
            .setColorForeground(MAIN.OPENBCI_DARKBLUE)
            .setColorActive(MAIN.isSelected_color)
            .setColorCursor(MAIN.color(26, 26, 26))
            .setText(directoryManager.getFileNameDateTime())
            .align(5, 10, 20, 40)
            .setAutoClear(false);
        sessionNameTextfield.onDoublePress(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                output("SessionData：请输入你的自定义会话名称。");
                sessionNameTextfield.clear();
            }
        });
        sessionNameTextfield.addCallback(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                if (theEvent.getAction() == ControlP5.ACTION_BROADCAST && sessionNameTextfield.getText().equals("")) {
                    autogenerateSessionName();
                }
            }
        });
        sessionNameTextfield.onReleaseOutside(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                if (!sessionNameTextfield.isActive() && sessionNameTextfield.getText().equals("")) {
                    autogenerateSessionName();
                }
            }
        });
    }

    private void createMaxDurationDropdown(String name, List<String> items) {
        maxDurationDropdown = sessionData_cp5.addScrollableList(name)
            .setOpen(false)
            .setColor(MAIN.settings.dropdownColors)
            .setOutlineColor(150)
            .setColorValueLabel(MAIN.OPENBCI_DARKBLUE)
            .setPosition(x + maxDurTextWidth, (int) (outputODF.getPosition()[1]) + 24 + padding)
            .setSize((w - padding * 3) / 2, (items.size() + 1) * 24)
            .setBarHeight(24)
            .setItemHeight(24)
            .addItems(items)
            .setVisible(false);
        maxDurationDropdown
            .getCaptionLabel()
            .toUpperCase(false)
            .setText(MAIN.settings.fileDurations[MAIN.settings.defaultOBCIMaxFileSize])
            .setFont(p7)
            .setSize(14)
            .getStyle()
            .setPaddingTop(4);
        maxDurationDropdown
            .getValueLabel()
            .toUpperCase(false)
            .setText(MAIN.settings.fileDurations[MAIN.settings.defaultOBCIMaxFileSize])
            .setFont(p7)
            .setSize(12)
            .getStyle()
            .setPaddingTop(3);
        maxDurationDropdown.addCallback(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                if (theEvent.getAction() == ControlP5.ACTION_BROADCAST) {
                    int n = (int) (theEvent.getController()).getValue();
                    MAIN.settings.setLogFileDurationChoice(n);
                    println("ControlPanel: Chosen recording duration: " + n);
                } else if (theEvent.getAction() == ControlP5.ACTION_ENTER) {
                    lockOutsideElements(true);
                } else if (theEvent.getAction() == ControlP5.ACTION_LEAVE) {
                    ScrollableList theList = (ScrollableList) (theEvent.getController());
                    lockOutsideElements(theList.isOpen());
                }
            }
        });
    }

    private Button createGUIOutputToggle(String name, String text, boolean isToggled, int _x, int _y, int _w, int _h) {
        final Button b = MAIN.createButton(sessionData_cp5, name, text, _x, _y, _w, _h);
        b.setSwitch(true);
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
        autoSessionName.setDescription("根据日期和时间自动生成会话名称。");
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
        outputODF.setDescription("将 GUI 数据输出设置为 AirEIBCI 数据格式（.txt）。当数据流暂停或文件时长达到最大值时，会在会话文件夹中创建新文件。");
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
        outputBDF.setDescription("将 GUI 数据输出设置为 BioSemi 数据格式（.bdf）。所有会话数据都会保存在一个 .bdf 文件中。");
    }

    private void autogenerateSessionName() {
        output("基于当前日期和时间自动生成会话名称");
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

    public void setSessionTextfieldString(String val) {
        sessionNameTextfield.setText(val);
    }

    public void setSessionTextfieldText(String val) {
        setSessionTextfieldString(val);
    }

    private void lockOutsideElements(boolean dropdownIsOpen) {
        outputODF.setLock(dropdownIsOpen);
        outputBDF.setLock(dropdownIsOpen);
        autoSessionName.setLock(dropdownIsOpen);
        sessionNameTextfield.setLock(dropdownIsOpen);
    }
}
