package W_DigitalRead_;

////////////////////////////////////////////////////
//
//  W_DigitalRead is used to visiualze digital input values
//
//  Created: AJ Keller
//
//

import DataSourcePlayback_.DataSourcePlayback;
import DigitalCapableBoard_.DigitalCapableBoard;
import Widget_.Widget;
import controlP5.Button;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.Controller;
import processing.core.PApplet;
import processing.core.PFont;

import java.util.ArrayList;
import java.util.List;

import static Debugging_.GF.output;
import static Globel.GUI.navHeight;
import static Globel.GUI.p5;
import static WidgetManager_.GVI.*;
import Globel.GUI;

///////////////////////////////////////////////////,

public class W_DigitalRead extends Widget {
    GUI MAIN;
    private int numDigitalReadDots;
    float xF, yF, wF, hF;
    int dot_padding;
    //values for actual time series chart (rectangle encompassing all digitalReadDots)
    float dot_x, dot_y, dot_h, dot_w;
    float plotBottomWell;
    float playbackWidgetHeight;
    int digitalReaddotHeight;

    DigitalReadDot[] digitalReadDots;

    private Button digitalModeButton;

    private DigitalCapableBoard digitalBoard;

    public W_DigitalRead(GUI MAIN) {
        super(MAIN); //calls the parent CONSTRUCTOR method of Widget (DON'T REMOVE)
        this.MAIN = MAIN;
        digitalBoard = (DigitalCapableBoard)MAIN.currentBoard;

        //set number of digital reads
        if (MAIN.selectedProtocol == GUI.BoardProtocol.WIFI) {
            numDigitalReadDots = 3;
        } else {
            numDigitalReadDots = 5;
        }

        xF = (float)(x); //float(int( ... is a shortcut for rounding the float down... so that it doesn't creep into the 1px margin
        yF = (float)(y);
        wF = (float)(w);
        hF = (float)(h);

        dot_padding = 10;
        dot_x = xF + dot_padding;
        dot_y = yF + (dot_padding);
        dot_w = wF - dot_padding*2;
        dot_h = hF - playbackWidgetHeight - plotBottomWell - (dot_padding*2);
        digitalReaddotHeight = (int)(dot_h/numDigitalReadDots);

        digitalReadDots = new DigitalReadDot[numDigitalReadDots];

        //create our channel bars and populate our digitalReadDots array!
        for (int i = 0; i < numDigitalReadDots; i++) {
            int digitalReaddotY = (int)(dot_y) + i*(digitalReaddotHeight); //iterate through bar locations
            int digitalReaddotX = (int)(dot_x) + i*(digitalReaddotHeight); //iterate through bar locations
            int digitalPin = 0;
            if (i == 0) {
                digitalPin = 11;
            } else if (i == 1) {
                digitalPin = 12;
            } else if (i == 2) {
                if (MAIN.selectedProtocol == GUI.BoardProtocol.WIFI) {
                    digitalPin = 17;
                } else {
                    digitalPin = 13;
                }
            } else if (i == 3) {
                digitalPin = 17;
            } else {
                digitalPin = 18;
            }
            DigitalReadDot tempDot = new DigitalReadDot(MAIN, digitalPin, digitalReaddotX, digitalReaddotY, (int)(dot_w), digitalReaddotHeight, dot_padding);
            digitalReadDots[i] = tempDot;
        }

        createDigitalModeButton("digitalModeButton", "Turn Digital Read On", (int)(x0 + 1), (int)(y0 + navHeight + 1), 128, navHeight - 3, p5, 12, MAIN.buttonsLightBlue, MAIN.WHITE);
    }

    public int getNumDigitalReads() {
        return numDigitalReadDots;
    }

    public void update() {
        super.update(); //calls the parent update() method of Widget (DON'T REMOVE)

        if (MAIN.currentBoard instanceof DataSourcePlayback) {
            if (((DataSourcePlayback)MAIN.currentBoard) instanceof DigitalCapableBoard
                    && (!((DigitalCapableBoard)MAIN.currentBoard).isDigitalActive())) {
                return;
            }
        }

        //update channel bars ... this means feeding new EEG data into plots
        for (int i = 0; i < numDigitalReadDots; i++) {
            digitalReadDots[i].update();
        }

        //ignore top left button interaction when widgetSelector dropdown is active
        List<Controller> cp5ElementsToCheck = new ArrayList<Controller>();
        cp5ElementsToCheck.add((Controller)digitalModeButton);
        lockElementsOnOverlapCheck(cp5ElementsToCheck);

        if (!digitalBoard.canDeactivateDigital()) {
            digitalModeButton.setLock(true);
            digitalModeButton.getCaptionLabel().setText("Digital Read On");
            digitalModeButton.setColorBackground(MAIN.BUTTON_LOCKED_GREY);
        }
    }

    public void draw() {
        super.draw(); //calls the parent draw() method of Widget (DON'T REMOVE)

        //draw channel bars
        if (digitalBoard.isDigitalActive()) {
            for (int i = 0; i < numDigitalReadDots; i++) {
                digitalReadDots[i].draw();
            }
        }
    }

    public void screenResized() {
        super.screenResized(); //calls the parent screenResized() method of Widget (DON'T REMOVE)

        xF = (float)(x); //float(int( ... is a shortcut for rounding the float down... so that it doesn't creep into the 1px margin
        yF = (float)(y);
        wF = (float)(w);
        hF = (float)(h);

        if (wF > hF) {
            digitalReaddotHeight = (int)(hF/(numDigitalReadDots+1));
        } else {
            digitalReaddotHeight = (int)(wF/(numDigitalReadDots+1));
        }

        if (numDigitalReadDots == 3) {
            digitalReadDots[0].screenResized(x+(int)(wF*(1.0/3.0)), y+(int)(hF*(1.0/3.0)), digitalReaddotHeight, digitalReaddotHeight); //bar x, bar y, bar w, bar h
            digitalReadDots[1].screenResized(x+(int)(wF/2), y+(int)(hF/2), digitalReaddotHeight, digitalReaddotHeight); //bar x, bar y, bar w, bar h
            digitalReadDots[2].screenResized(x+(int)(wF*(2.0/3.0)), y+(int)(hF*(2.0/3.0)), digitalReaddotHeight, digitalReaddotHeight); //bar x, bar y, bar w, bar h
        } else {
            int y_pad = y + dot_padding;
            digitalReadDots[0].screenResized(x+(int)(wF*(1.0/8.0)), y_pad+(int)(hF*(1.0/8.0)), digitalReaddotHeight, digitalReaddotHeight);
            digitalReadDots[2].screenResized(x+(int)(wF/2), y_pad+(int)(hF/2), digitalReaddotHeight, digitalReaddotHeight);
            digitalReadDots[4].screenResized(x+(int)(wF*(7.0/8.0)), y_pad+(int)(hF*(7.0/8.0)), digitalReaddotHeight, digitalReaddotHeight);
            digitalReadDots[1].screenResized((int) (digitalReadDots[0].dotX+(wF*(3.0/16.0))), (int) (digitalReadDots[0].dotY+(hF*(3.0/16.0))), digitalReaddotHeight, digitalReaddotHeight);
            digitalReadDots[3].screenResized((int) (digitalReadDots[2].dotX+(wF*(3.0/16.0))), (int) (digitalReadDots[2].dotY+(hF*(3.0/16.0))), digitalReaddotHeight, digitalReaddotHeight);

        }

        digitalModeButton.setPosition((int)(x0 + 1), (int)(y0 + navHeight + 1));
    }

    public void mousePressed() {
        super.mousePressed(); //calls the parent mousePressed() method of Widget (DON'T REMOVE)
    }

    public void mouseReleased() {
        super.mouseReleased(); //calls the parent mouseReleased() method of Widget (DON'T REMOVE)
    }

    private void createDigitalModeButton(String name, String text, int _x, int _y, int _w, int _h, PFont _font, int _fontSize, int _bg, int _textColor) {
        digitalModeButton = MAIN.createButton(cp5_widget, name, text, _x, _y, _w, _h, 0, _font, _fontSize, _bg, _textColor, MAIN.BUTTON_HOVER, MAIN.BUTTON_PRESSED, MAIN.OBJECT_BORDER_GREY, 0);
        digitalModeButton.setSwitch(true);
        digitalModeButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                if (!digitalBoard.isDigitalActive()) {
                    digitalBoard.setDigitalActive(true);
                    digitalModeButton.getCaptionLabel().setText("Turn Digital Read Off");
                    if (MAIN.selectedProtocol == GUI.BoardProtocol.WIFI) {
                        output("Starting to read digital inputs on pin marked D11, D12 and D17");
                    } else {
                        output("Starting to read digital inputs on pin marked D11, D12, D13, D17 and D18");
                    }
                    w_accelerometer.accelBoardSetActive(false);
                    w_analogRead.toggleAnalogReadButton(false);
                    w_pulsesensor.toggleAnalogReadButton(false);
                } else {
                    digitalBoard.setDigitalActive(false);
                    digitalModeButton.getCaptionLabel().setText("Turn Digital Read On");
                    output("Starting to read accelerometer");
                    w_accelerometer.accelBoardSetActive(true);
                    w_analogRead.toggleAnalogReadButton(false);
                    w_pulsesensor.toggleAnalogReadButton(false);
                }
            }
        });
        String _helpText = (MAIN.selectedProtocol == GUI.BoardProtocol.WIFI) ?
                "Click this button to activate/deactivate digital read on Cyton pins D11, D12, and D17." :
                "Click this button to activate/deactivate digital read on Cyton pins D11, D12, D13, D17 and D18."
                ;
        digitalModeButton.setDescription(_helpText);
    }

    public void toggleDigitalReadButton(boolean _value) {
        String s = _value ? "Turn Digital Read Off" : "Turn Digital Read On";
        digitalModeButton.getCaptionLabel().setText(s);
        if (_value) {
            digitalModeButton.setOn();
        } else {
            digitalModeButton.setOff();
        }
    }
};