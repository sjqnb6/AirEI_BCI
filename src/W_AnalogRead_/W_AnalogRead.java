package W_AnalogRead_;

////////////////////////////////////////////////////
//
//  W_AnalogRead is used to visiualze analog voltage values
//
//  Created: AJ Keller
//
//

import AnalogCapableBoard_.AnalogCapableBoard;
import DataSourcePlayback_.DataSourcePlayback;
import Widget_.Widget;
import controlP5.Button;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.Controller;
import processing.core.PApplet;
import processing.core.PFont;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static Debugging_.GF.output;
import static Globel.GUI.navHeight;
import static Globel.GUI.p5;
import static WidgetManager_.GVI.*;
import Globel.GUI;
///////////////////////////////////////////////////,

public class W_AnalogRead extends Widget {

    //to see all core variables/methods of the Widget class, refer to Widget.pde
    //put your custom variables here...
    GUI MAIN;
    public int numAnalogReadBars;
    float xF, yF, wF, hF;
    float arPadding;
    float ar_x, ar_y, ar_h, ar_w; // values for actual time series chart (rectangle encompassing all analogReadBars)
    float plotBottomWell;
    float playbackWidgetHeight;
    int analogReadBarHeight;

    public AnalogReadBar[] analogReadBars;

    int[] xLimOptions = {0, 1, 3, 5, 10, 20}; // number of seconds (x axis of graph)
    public int[] yLimOptions = {0, 50, 100, 200, 400, 1000, 10000}; // 0 = Autoscale ... everything else is uV

    private boolean allowSpillover = false;

    //Initial dropdown settings
    private int arInitialVertScaleIndex = 5;
    private int arInitialHorizScaleIndex = 0;

    private Button analogModeButton;

    private AnalogCapableBoard analogBoard;

    public W_AnalogRead(GUI MAIN) {
        super(MAIN); //calls the parent CONSTRUCTOR method of Widget (DON'T REMOVE)
        this.MAIN = MAIN;
        analogBoard = (AnalogCapableBoard)MAIN.currentBoard;

        //Analog Read settings
        MAIN.settings.arVertScaleSave = 5; //updates in VertScale_AR()
        MAIN.settings.arHorizScaleSave = 0; //updates in Duration_AR()

        //This is the protocol for setting up dropdowns.
        //Note that these 3 dropdowns correspond to the 3 global functions below
        //You just need to make sure the "id" (the 1st String) has the same name as the corresponding function
        addDropdown("VertScale_AR", "Vert Scale", Arrays.asList(MAIN.settings.arVertScaleArray), arInitialVertScaleIndex);
        addDropdown("Duration_AR", "Window", Arrays.asList(MAIN.settings.arHorizScaleArray), arInitialHorizScaleIndex);
        // addDropdown("Spillover", "Spillover", Arrays.asList("False", "True"), 0);

        //set number of analog reads
        if (MAIN.selectedProtocol == GUI.BoardProtocol.WIFI) {
            numAnalogReadBars = 2;
        } else {
            numAnalogReadBars = 3;
        }

        xF = (float)(x); //float(int( ... is a shortcut for rounding the float down... so that it doesn't creep into the 1px margin
        yF = (float)(y);
        wF = (float)(w);
        hF = (float)(h);

        plotBottomWell = 45.0F; //this appears to be an arbitrary vertical space adds GPlot leaves at bottom, I derived it through trial and error
        arPadding = 10.0F;
        ar_x = xF + arPadding;
        ar_y = yF + (arPadding);
        ar_w = wF - arPadding*2;
        ar_h = hF - playbackWidgetHeight - plotBottomWell - (arPadding*2);
        analogReadBarHeight = (int)(ar_h/numAnalogReadBars);

        analogReadBars = new AnalogReadBar[numAnalogReadBars];

        //create our channel bars and populate our analogReadBars array!
        for(int i = 0; i < numAnalogReadBars; i++) {
            int analogReadBarY = (int)(ar_y) + i*(analogReadBarHeight); //iterate through bar locations
            AnalogReadBar tempBar = new AnalogReadBar(MAIN, i+5, (int)(ar_x), analogReadBarY, (int)(ar_w), analogReadBarHeight); //int _channelNumber, int _x, int _y, int _w, int _h
            analogReadBars[i] = tempBar;
            analogReadBars[i].adjustVertScale(yLimOptions[arInitialVertScaleIndex]);
            //sync horiz axis to Time Series by default
            analogReadBars[i].adjustTimeAxis(w_timeSeries.getTSHorizScale().getValue());
        }

        createAnalogModeButton("analogModeButton", "Turn Analog Read On", (int)(x0 + 1), (int)(y0 + navHeight + 1), 128, navHeight - 3, p5, 12, MAIN.colorNotPressed, MAIN.OPENBCI_DARKBLUE);
    }

    public int getNumAnalogReads() {
        return numAnalogReadBars;
    }

    public void update() {
        super.update(); //calls the parent update() method of Widget (DON'T REMOVE)

        if (MAIN.currentBoard instanceof DataSourcePlayback) {
            if (((DataSourcePlayback)MAIN.currentBoard) instanceof AnalogCapableBoard
                    && (!((AnalogCapableBoard)MAIN.currentBoard).isAnalogActive())) {
                return;
            }
        }

        //update channel bars ... this means feeding new EEG data into plots
        for(int i = 0; i < numAnalogReadBars; i++) {
            analogReadBars[i].update();
        }

        //ignore top left button interaction when widgetSelector dropdown is active
        List<Controller> cp5ElementsToCheck = new ArrayList<Controller>();
        cp5ElementsToCheck.add((Controller)analogModeButton);
        lockElementsOnOverlapCheck(cp5ElementsToCheck);

        if (!analogBoard.canDeactivateAnalog()) {
            analogModeButton.setLock(true);
            analogModeButton.getCaptionLabel().setText("Analog Read On");
            analogModeButton.setColorBackground(MAIN.BUTTON_LOCKED_GREY);
        }
    }

    public void draw() {
        super.draw(); //calls the parent draw() method of Widget (DON'T REMOVE)

        //remember to refer to x,y,w,h which are the positioning variables of the Widget class
        if (analogBoard.isAnalogActive()) {
            for(int i = 0; i < numAnalogReadBars; i++) {
                analogReadBars[i].draw();
            }
        }
    }

    public void screenResized() {
        super.screenResized(); //calls the parent screenResized() method of Widget (DON'T REMOVE)

        xF = (float)(x); //float(int( ... is a shortcut for rounding the float down... so that it doesn't creep into the 1px margin
        yF = (float)(y);
        wF = (float)(w);
        hF = (float)(h);

        ar_x = xF + arPadding;
        ar_y = yF + (arPadding);
        ar_w = wF - arPadding*2;
        ar_h = hF - playbackWidgetHeight - plotBottomWell - (arPadding*2);
        analogReadBarHeight = (int)(ar_h/numAnalogReadBars);

        for(int i = 0; i < numAnalogReadBars; i++) {
            int analogReadBarY = (int)(ar_y) + i*(analogReadBarHeight); //iterate through bar locations
            analogReadBars[i].screenResized((int)(ar_x), analogReadBarY, (int)(ar_w), analogReadBarHeight); //bar x, bar y, bar w, bar h
        }

        analogModeButton.setPosition((int)(x0 + 1), (int)(y0 + navHeight + 1));
    }

    public void mousePressed() {
        super.mousePressed(); //calls the parent mousePressed() method of Widget (DON'T REMOVE)
    }

    public void mouseReleased() {
        super.mouseReleased(); //calls the parent mouseReleased() method of Widget (DON'T REMOVE)
    }

    private void createAnalogModeButton(String name, String text, int _x, int _y, int _w, int _h, PFont _font, int _fontSize, int _bg, int _textColor) {
        analogModeButton = MAIN.createButton(cp5_widget, name, text, _x, _y, _w, _h, 0, _font, _fontSize, _bg, _textColor, MAIN.BUTTON_HOVER, MAIN.BUTTON_PRESSED, MAIN.OBJECT_BORDER_GREY, 0);
        analogModeButton.setSwitch(true);
        analogModeButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                if (!analogBoard.isAnalogActive()) {
                    analogBoard.setAnalogActive(true);
                    analogModeButton.getCaptionLabel().setText("Turn Analog Read Off");
                    if (MAIN.selectedProtocol == GUI.BoardProtocol.WIFI) {
                        output("Starting to read analog inputs on pin marked A5 (D11) and A6 (D12)");
                    } else {
                        output("Starting to read analog inputs on pin marked A5 (D11), A6 (D12) and A7 (D13)");
                    }
                    w_pulsesensor.toggleAnalogReadButton(true);
                    w_accelerometer.accelBoardSetActive(false);
                    w_digitalRead.toggleDigitalReadButton(false);
                } else {
                    analogBoard.setAnalogActive(false);
                    analogModeButton.getCaptionLabel().setText("Turn Analog Read On");
                    output("Starting to read accelerometer");
                    w_accelerometer.accelBoardSetActive(true);
                    w_digitalRead.toggleDigitalReadButton(false);
                    w_pulsesensor.toggleAnalogReadButton(false);
                }
            }
        });
        String _helpText = (MAIN.selectedProtocol == GUI.BoardProtocol.WIFI) ?
                "Click this button to activate/deactivate analog read on Cyton pins A5(D11) and A6(D12)." :
                "Click this button to activate/deactivate analog read on Cyton pins A5(D11), A6(D12) and A7(D13)."
                ;
        analogModeButton.setDescription(_helpText);
    }

    public void toggleAnalogReadButton(boolean _value) {
        String s = _value ? "Turn Analog Read Off" : "Turn Analog Read On";
        analogModeButton.getCaptionLabel().setText(s);
        if (_value) {
            analogModeButton.setOn();
        } else {
            analogModeButton.setOff();
        }
    }
};