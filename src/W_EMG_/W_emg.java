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

import Globel.GUI;

import static Globel.GUI.*;
////////////////////////////////////////////////////////////////////////////////

public class W_emg extends Widget {
    // Match W_CFC / W_Connectivity dark tech palette.
    private static final int COLOR_BG_TOP = 0xFF0A1220;
    private static final int COLOR_BG_BOTTOM = 0xFF0D1830;
    private static final int COLOR_PANEL = 0xE013243F;
    private static final int COLOR_BORDER = 0x6683A2CC;
    private static final int COLOR_GRID = 0x5A9ABFE3;
    private static final int COLOR_TEXT = 0xFFEAF2FF;

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
        drawGradientBackground((int)rx, (int)ry - 1, (int)rw, (int)rh + 1);
        pApplet.noStroke();
        pApplet.fill(COLOR_PANEL);
        pApplet.rect(rx, ry - 1, rw, rh + 1);
        pApplet.stroke(COLOR_BORDER);
        pApplet.noFill();
        pApplet.rect(rx, ry - 1, rw, rh + 1);

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
                pApplet.fill(MAIN.channelColors[colorIndex], 235);
                pApplet.noStroke();
                pApplet.circle(2*colOffset/8, rowOffset / 2, scaleFactor * emgSettingsValues.getAverageuV(channel));

                //circle for outer threshold
                pApplet.noFill();
                pApplet.strokeWeight(1);
                pApplet.stroke(COLOR_GRID, 210);
                pApplet.circle(2*colOffset/8, rowOffset / 2, scaleFactor * emgSettingsValues.getUpperThreshold(channel));

                //circle for inner threshold
                pApplet.stroke(COLOR_GRID, 210);
                pApplet.circle(2*colOffset/8, rowOffset / 2, scaleFactor * emgSettingsValues.getLowerThreshold(channel));

                int _x = (int)(5*colOffset/8);
                int _y = (int)(2 * rowOffset / 8);
                int _w = (int)(5*colOffset/32);
                int _h = (int)(4*rowOffset/8);

                //draw normalized bar graph of uV w/ matching channel color
                pApplet.noStroke();
                pApplet.fill(MAIN.channelColors[colorIndex], 230);
                pApplet.rect(_x, 3*_y + 1, _w, pApplet.map(emgSettingsValues.getOutputNormalized(channel), 0, 1, 0, (-1) * (int)((4*rowOffset/8))));

                //draw background bar container for mapped uV value indication
                pApplet.strokeWeight(1);
                pApplet.stroke(COLOR_GRID, 210);
                pApplet.noFill();
                pApplet.rect(_x, _y, _w, _h);

                //draw channel number at upper left corner of row/column cell
                pApplet.pushStyle();
                pApplet.stroke(COLOR_TEXT);
                pApplet.fill(COLOR_TEXT);
                pApplet.textFont(h4, 14);
                pApplet.text((channel + 1), 10, 20);
                pApplet.popStyle();

                pApplet.popMatrix();
            }
        }

        pApplet.popStyle();
    }

    private void createEmgSettingsButton() {
        emgSettingsButton = MAIN.createButton(emgCp5, "emgSettingsButton", "EMG设置",
                (int) (x0 + w - EMG_SETTINGS_BUTTON_WIDTH - 1), (int) (y0 + navH + 1),
                EMG_SETTINGS_BUTTON_WIDTH, navH - 3, p7, 12, MAIN.colorNotPressed, COLOR_TEXT);
        emgSettingsButton.setColorBackground(0xFF1A2B46);
        emgSettingsButton.setColorForeground(0xFF25405F);
        emgSettingsButton.setColorActive(0xFF2D4F76);
        emgSettingsButton.setBorderColor(COLOR_BORDER);
        emgSettingsButton.onRelease(new CallbackListener() {
            public synchronized void controlEvent(CallbackEvent theEvent) {
                if (!MAIN.emgSettingsPopupIsOpen) {
                    EmgSettingsUI emgSettingsUI = new EmgSettingsUI(MAIN);
                }
            }
        });
        emgSettingsButton.setDescription("点击打开EMG设置界面，调整该指标的计算方式。");
    }

    private void drawGradientBackground(int x0, int y0, int w0, int h0) {
        for (int i = 0; i < h0; i++) {
            float t = i / (float) Math.max(1, h0 - 1);
            int c = lerpRgb(COLOR_BG_TOP, COLOR_BG_BOTTOM, t);
            pApplet.stroke((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF);
            pApplet.line(x0, y0 + i, x0 + w0, y0 + i);
        }
    }

    private int lerpRgb(int c1, int c2, float t) {
        int r1 = (c1 >> 16) & 0xFF;
        int g1 = (c1 >> 8) & 0xFF;
        int b1 = c1 & 0xFF;
        int r2 = (c2 >> 16) & 0xFF;
        int g2 = (c2 >> 8) & 0xFF;
        int b2 = c2 & 0xFF;
        int r = (int) PApplet.lerp(r1, r2, t);
        int g = (int) PApplet.lerp(g1, g2, t);
        int b = (int) PApplet.lerp(b1, b2, t);
        return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }
};
