package W_EMG_;

/////////////////////////////////////////////////////////////////////////////////
//
//  Emg_Widget is used to visiualze EMG data by channel, and to trip events
//
//  Created: Colin Fausnaught, December 2016 (with a lot of reworked code from Tao)
//  Modified: Richard Waltman, February 2023
//
//  Custom widget to visiualze EMG data. Features dragable thresholds, serial
//  out communication, channel configuration, digital and analog events.
//
//  KNOWN ISSUES: Cannot resize with window dragging events
//
//  TODO: Add dynamic threshold functionality

import EmgSettingsUI_.EmgSettingsUI;
import EmgSettingsValues_.EmgSettingsValues;
import Widget_.ChannelSelect;
import Widget_.Widget;
import controlP5.*;
import processing.core.PApplet;

import java.util.ArrayList;
import java.util.List;

import static GUI.GGVI.*;
import Globel.GUI;
////////////////////////////////////////////////////////////////////////////////

public class W_emg extends Widget {
    GUI MAIN;
    PApplet parent;

    private ControlP5 emgCp5;
    private Button emgSettingsButton;
    private final int EMG_SETTINGS_BUTTON_WIDTH = 125;
    private List<Controller> cp5ElementsToCheck;

    public ChannelSelect emgChannelSelect;

    public W_emg(GUI MAIN) {
        super(MAIN); //calls the parent CONSTRUCTOR method of Widget (DON'T REMOVE)
        this.MAIN = MAIN;
        parent = MAIN;

        cp5ElementsToCheck = new ArrayList<Controller>();

        //Add channel select dropdown to this widget
        emgChannelSelect = new ChannelSelect(pApplet, this, x, y, w, navH, "EMG_Channels");
        emgChannelSelect.activateAllButtons();
        cp5ElementsToCheck.addAll(emgChannelSelect.getCp5ElementsForOverlapCheck());

        emgCp5 = new ControlP5(pApplet);
        emgCp5.setGraphics(pApplet, 0,0);
        emgCp5.setAutoDraw(false);

        createEmgSettingsButton();
        cp5ElementsToCheck.add((Controller) emgSettingsButton);
    }

    public void update() {
        super.update(); //calls the parent update() method of Widget (DON'T REMOVE)
        lockElementsOnOverlapCheck(cp5ElementsToCheck);

        //Update channel checkboxes and active channels
        emgChannelSelect.update(x, y, w);

        /*
        //Flex the Gplot graph when channel select dropdown is open/closed
        if (bpChanSelect.isVisible() != prevChanSelectIsVisible) {
            flexGPlotSizeAndPosition();
            prevChanSelectIsVisible = bpChanSelect.isVisible();
        }
        */
    }

    public void draw() {
        super.draw(); //calls the parent draw() method of Widget (DON'T REMOVE)

        drawEmgVisualizations();

        emgCp5.draw();

        //Draw channel select dropdown
        emgChannelSelect.draw();
    }

    public void screenResized() {
        super.screenResized(); //calls the parent screenResized() method of Widget (DON'T REMOVE)
        emgCp5.setGraphics(pApplet, 0, 0);
        emgSettingsButton.setPosition(x0 + w - EMG_SETTINGS_BUTTON_WIDTH - 2, y0 + navH + 1);
        emgChannelSelect.screenResized(pApplet);
    }

    public void mousePressed() {
        super.mousePressed(); //calls the parent mousePressed() method of Widget (DON'T REMOVE)
        //Calls channel select mousePressed and checks if clicked
        emgChannelSelect.mousePressed(this.dropdownIsActive);
    }

    private void drawEmgVisualizations() {
        pApplet.pushStyle();

        float rx = x, ry = y, rw = w, rh = h;
        //Flex the EMG graph when channel select dropdown is open/closed
        ry = emgChannelSelect.isVisible() ? y + emgChannelSelect.getHeight() : y;
        rh = emgChannelSelect.isVisible() ? h - emgChannelSelect.getHeight() : h;
        float scaleFactor = 1.0F;
        float scaleFactorJaw = 1.5F;
        int rowCount = 4;
        int columnCount = MAIN.ceil(emgChannelSelect.activeChan.size() / (rowCount * 1f));
        float rowOffset = rh / rowCount;
        float colOffset = rw / columnCount;
        float currentX, currentY;

        EmgSettingsValues emgSettingsValues = dataProcessing.emgSettings.values;

        int channel = 0;
        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < columnCount; j++) {

                int index = i * columnCount + j;

                if (index > emgChannelSelect.activeChan.size() - 1) {
                    continue;
                }

                channel = emgChannelSelect.activeChan.get(index);

                int colorIndex = channel % 8;

                pApplet.pushMatrix();

                currentX = rx + j * colOffset;
                currentY = ry + i * rowOffset; //never name variables on an empty stomach
                pApplet.translate(currentX, currentY);

                //realtime
                pApplet.fill(MAIN.channelColors[colorIndex], 200);
                pApplet.noStroke();
                pApplet.circle(2*colOffset/8, rowOffset / 2, scaleFactor * emgSettingsValues.getAverageuV(channel));

                //circle for outer threshold
                pApplet.noFill();
                pApplet.strokeWeight(1);
                pApplet.stroke(MAIN.OPENBCI_DARKBLUE, 150);
                pApplet.circle(2*colOffset/8, rowOffset / 2, scaleFactor * emgSettingsValues.getUpperThreshold(channel));

                //circle for inner threshold
                pApplet.stroke(MAIN.OPENBCI_DARKBLUE, 150);
                pApplet.circle(2*colOffset/8, rowOffset / 2, scaleFactor * emgSettingsValues.getLowerThreshold(channel));

                int _x = (int)(5*colOffset/8);
                int _y = (int)(2 * rowOffset / 8);
                int _w = (int)(5*colOffset/32);
                int _h = (int)(4*rowOffset/8);

                //draw normalized bar graph of uV w/ matching channel color
                pApplet.noStroke();
                pApplet.fill(MAIN.channelColors[colorIndex], 200);
                pApplet.rect(_x, 3*_y + 1, _w, pApplet.map(emgSettingsValues.getOutputNormalized(channel), 0, 1, 0, (-1) * (int)((4*rowOffset/8))));

                //draw background bar container for mapped uV value indication
                pApplet.strokeWeight(1);
                pApplet.stroke(MAIN.OPENBCI_DARKBLUE, 150);
                pApplet.noFill();
                pApplet.rect(_x, _y, _w, _h);

                //draw channel number at upper left corner of row/column cell
                pApplet.pushStyle();
                pApplet.stroke(MAIN.OPENBCI_DARKBLUE);
                pApplet.fill(MAIN.OPENBCI_DARKBLUE);
                pApplet.textFont(h4, 14);
                pApplet.text((channel + 1), 10, 20);
                pApplet.popStyle();

                pApplet.popMatrix();
            }
        }

        pApplet.popStyle();
    }

    private void createEmgSettingsButton() {
        emgSettingsButton = MAIN.createButton(emgCp5, "emgSettingsButton", "EMG Settings",
                (int) (x0 + w - EMG_SETTINGS_BUTTON_WIDTH - 1), (int) (y0 + navH + 1),
                EMG_SETTINGS_BUTTON_WIDTH, navH - 3, p5, 12, MAIN.colorNotPressed, MAIN.OPENBCI_DARKBLUE);
        emgSettingsButton.setBorderColor(MAIN.OBJECT_BORDER_GREY);
        emgSettingsButton.onRelease(new CallbackListener() {
            public synchronized void controlEvent(CallbackEvent theEvent) {
                if (!MAIN.emgSettingsPopupIsOpen) {
                    EmgSettingsUI emgSettingsUI = new EmgSettingsUI(MAIN);
                }
            }
        });
        emgSettingsButton.setDescription("Click to open the EMG Settings UI to adjust how this metric is calculated.");
    }
};