package W_Accelerometer_;

////////////////////////////////////////////////////
//
// W_Accelerometer is used to visualize accelerometer data
//
// Created: Joel Murphy
// Modified: Colin Fausnaught, September 2016
// Modified: Wangshu Sun, November 2016
// Modified: Richard Waltman, November 2018
//
//

import AccelerometerCapableBoard_.AccelerometerCapableBoard;
import AnalogCapableBoard_.AnalogCapableBoard;
import BoardCyton_.BoardCyton;
import DigitalCapableBoard_.DigitalCapableBoard;
import GUI.ColorPalette;
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
import static GUI.GGVI.*;
import static WidgetManager_.GVI.*;

////////////////////////////////////////////////////

public class W_Accelerometer extends Widget {
    protected PApplet pApplet;
    public ColorPalette CP;

    //Graphing variables
    public int[] xLimOptions = {0, 1, 3, 5, 10, 20}; //number of seconds (x axis of graph)
    public int[] yLimOptions = {0, 1, 2, 4};
    float accelXyzLimit = 4.0F; //hard limit on all accel values
    int accelHorizLimit = 20;
    float[] lastAccelVals;
    public AccelerometerBar accelerometerBar;

    //Bottom xyz graph
    int accelGraphWidth;
    int accelGraphHeight;
    int accelGraphX;
    int accelGraphY;
    int accPadding = 30;

    //Circular 3d xyz graph
    float polarWindowX;
    float polarWindowY;
    int polarWindowWidth;
    int polarWindowHeight;
    float polarCorner;

    float yMaxMin;

    boolean accelInitHasOccured = false;
    private Button accelModeButton;

    private AccelerometerCapableBoard accelBoard;

    public W_Accelerometer(PApplet _parent) {
        super(_parent); //calls the parent CONSTRUCTOR method of Widget (DON'T REMOVE)

        CP = new ColorPalette(_parent);

        pApplet = _parent;

        accelBoard = (AccelerometerCapableBoard)currentBoard;

        //Default dropdown settings
        settings.accVertScaleSave = 0;
        settings.accHorizScaleSave = 3;

        //Make dropdowns
        addDropdown("accelVertScale", "Vert Scale", Arrays.asList(settings.accVertScaleArray), settings.accVertScaleSave);
        addDropdown("accelDuration", "Window", Arrays.asList(settings.accHorizScaleArray), settings.accHorizScaleSave);

        setGraphDimensions();
        yMaxMin = adjustYMaxMinBasedOnSource();

        //XYZ buffer for bottom graph
        lastAccelVals = new float[NUM_ACCEL_DIMS];

        //create our channel bar and populate our accelerometerBar array!
        accelerometerBar = new AccelerometerBar(_parent, accelXyzLimit, accelGraphX, accelGraphY, accelGraphWidth, accelGraphHeight);
        accelerometerBar.adjustTimeAxis(xLimOptions[settings.accHorizScaleSave]);
        accelerometerBar.adjustVertScale(yLimOptions[settings.accVertScaleSave]);

        createAccelModeButton("accelModeButton", "Turn Accel. Off", (int)(x + 1), (int)(y0 + navHeight + 1), 120, navHeight - 3, p5, 12, CP.colorNotPressed, CP.OPENBCI_DARKBLUE);
    }

    float adjustYMaxMinBasedOnSource() {
        float _yMaxMin;
        if (eegDataSource == DATASOURCE_CYTON) {
            _yMaxMin = 4.0F;
        }else if (eegDataSource == DATASOURCE_GANGLION || nchan == 4) {
            _yMaxMin = 2.0F;
            accelXyzLimit = 2.0F;
        }else{
            _yMaxMin = 4.0F;
        }
        return _yMaxMin;
    }

    int nPointsBasedOnDataSource() {
        return accelHorizLimit * ((AccelerometerCapableBoard)currentBoard).getAccelSampleRate();
    }

    public void update() {
        super.update(); //calls the parent update() method of Widget (DON'T REMOVE)

        if (accelBoard.isAccelerometerActive()) {
            //update the line graph and corresponding gplot points
            accelerometerBar.update();

            //update the current Accelerometer values
            lastAccelVals = accelerometerBar.getLastAccelVals();
        }

        //ignore top left button interaction when widgetSelector dropdown is active
        List<Controller> cp5ElementsToCheck = new ArrayList<Controller>();
        cp5ElementsToCheck.add((Controller)accelModeButton);
        lockElementsOnOverlapCheck(cp5ElementsToCheck);

        if(!accelBoard.canDeactivateAccelerometer() && !(currentBoard instanceof BoardCyton)) {
            accelModeButton.getCaptionLabel().setText("Accel. On");
            accelModeButton.setColorBackground(CP.BUTTON_LOCKED_GREY);
            accelModeButton.setLock(true);
        }
    }

    public float getLastAccelVal(int val) {
        return lastAccelVals[val];
    }

    public void draw() {
        super.draw(); //calls the parent draw() method of Widget (DON'T REMOVE)

        pApplet.pushStyle();

        pApplet.fill(50);
        pApplet.textFont(p4, 14);
        pApplet.textAlign(CENTER,CENTER);
        pApplet.text("z", polarWindowX, (polarWindowY-polarWindowHeight/2)-12);
        pApplet.text("x", (polarWindowX+polarWindowWidth/2)+8, polarWindowY-5);
        pApplet.text("y", (polarWindowX+polarCorner)+10, (polarWindowY-polarCorner)-10);

        pApplet.fill(CP.graphBG);  //pulse window background
        pApplet.stroke(CP.graphStroke);
        pApplet.ellipse(polarWindowX,polarWindowY,polarWindowWidth,polarWindowHeight);

        pApplet.stroke(180);
        pApplet.line(polarWindowX-polarWindowWidth/2, polarWindowY, polarWindowX+polarWindowWidth/2, polarWindowY);
        pApplet.line(polarWindowX, polarWindowY-polarWindowHeight/2, polarWindowX, polarWindowY+polarWindowHeight/2);
        pApplet.line(polarWindowX-polarCorner, polarWindowY+polarCorner, polarWindowX+polarCorner, polarWindowY-polarCorner);

        if (accelBoard.isAccelerometerActive()) {
            drawAccValues();
            draw3DGraph();
        }

        pApplet.popStyle();

        if (accelBoard.isAccelerometerActive()) {
            accelerometerBar.draw();
        }
    }

    void setGraphDimensions() {
        accelGraphWidth = w - accPadding*2;
        accelGraphHeight = (int)(((h) - (accPadding*3))/2.0);
        accelGraphX = x + accPadding/3;
        accelGraphY = y + h - accelGraphHeight - (int)(accPadding*2) + accPadding/6;

        polarWindowWidth = accelGraphHeight;
        polarWindowHeight = accelGraphHeight;
        polarWindowX = x + w - accPadding - polarWindowWidth/2;
        polarWindowY = y + accPadding + polarWindowHeight/2 - 10;
        polarCorner = (sqrt(2)*polarWindowWidth/2)/2;
    }

    public void screenResized() {
        int prevX = x;
        int prevY = y;
        int prevW = w;
        int prevH = h;
        super.screenResized(); //calls the parent screenResized() method of Widget (DON'T REMOVE)
        setGraphDimensions();
        //resize the accelerometer line graph
        accelerometerBar.screenResized(accelGraphX, accelGraphY, accelGraphWidth-accPadding*2, accelGraphHeight); //bar x, bar y, bar w, bar h
        //update the position of the accel mode button
        accelModeButton.setPosition((int)(x0 + 1), (int)(y0 + navHeight + 1));
    }

    public void mousePressed() {
        super.mousePressed(); //calls the parent mousePressed() method of Widget (DON'T REMOVE)
    }

    public void mouseReleased() {
        super.mouseReleased(); //calls the parent mouseReleased() method of Widget (DON'T REMOVE)
    }

    private void createAccelModeButton(String name, String text, int _x, int _y, int _w, int _h, PFont _font, int _fontSize, int _bg, int _textColor) {
        accelModeButton = createButton(cp5_widget, name, text, _x, _y, _w, _h, 0, _font, _fontSize, _bg, _textColor, CP.BUTTON_HOVER, CP.BUTTON_PRESSED, CP.OBJECT_BORDER_GREY, 0);
        accelModeButton.setSwitch(true);
        accelModeButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                if (!accelBoard.isAccelerometerActive()) {
                    accelBoard.setAccelerometerActive(true);
                    output("Starting to read accelerometer");
                    accelModeButton.getCaptionLabel().setText("Turn Accel. Off");
                    if (currentBoard instanceof DigitalCapableBoard) {
                        w_digitalRead.toggleDigitalReadButton(false);
                    }
                    if (currentBoard instanceof AnalogCapableBoard) {
                        w_pulsesensor.toggleAnalogReadButton(false);
                        w_analogRead.toggleAnalogReadButton(false);
                    }
                    ///Hide button when set On for Cyton board only. This is a special case for Cyton board Aux mode behavior. See BoardCyton.pde for more info.
                    if ((currentBoard instanceof BoardCyton)) {
                        accelModeButton.setVisible(false);
                    }
                } else {
                    if (accelBoard.canDeactivateAccelerometer()) {
                        accelBoard.setAccelerometerActive(false);
                        accelModeButton.getCaptionLabel().setText("Turn Accel. On");
                    } else {
                        accelModeButton.setOn();
                    }
                }
            }
        });
        accelModeButton.setDescription("Click to activate/deactivate the accelerometer for capable boards.");
        if (accelBoard.canDeactivateAccelerometer() || (currentBoard instanceof BoardCyton)) {
            //Set button switch to On of it can be toggled
            accelModeButton.setOn();
            //Hide button when set On for Cyton board only. This is a special case for Cyton board Aux mode behavior. See BoardCyton.pde for more info.
            if ((currentBoard instanceof BoardCyton)) {
                accelModeButton.setVisible(false);
            }
        }
    }

    //Draw the current accelerometer values as text
    void drawAccValues() {
        float displayX = (float)lastAccelVals[0];
        float displayY = (float)lastAccelVals[1];
        float displayZ = (float)lastAccelVals[2];
        pApplet.textAlign(LEFT,CENTER);
        pApplet.textFont(h1,20);
        pApplet.fill(CP.ACCEL_X_COLOR);
        pApplet.text("X = " + nf(displayX, 1, 3) + " g", (float) (x+accPadding), (float) (y + (h/12)*1.5 - 5));
        pApplet.fill(CP.ACCEL_Y_COLOR);
        pApplet.text("Y = " + nf(displayY, 1, 3) + " g", x+accPadding, y + (h/12)*3 - 5);
        pApplet.fill(CP.ACCEL_Z_COLOR);
        pApplet.text("Z = " + nf(displayZ, 1, 3) + " g", (float) (x+accPadding), (float) (y + (h/12)*4.5 - 5));
    }

    //Draw the current accelerometer values as a 3D graph
    void draw3DGraph() {
        float displayX = (float)lastAccelVals[0];
        float displayY = (float)lastAccelVals[1];
        float displayZ = (float)lastAccelVals[2];

        pApplet.noFill();
        pApplet.strokeWeight(3);
        pApplet.stroke(CP.ACCEL_X_COLOR);
        pApplet.line(polarWindowX, polarWindowY, polarWindowX+map(displayX, -yMaxMin, yMaxMin, -polarWindowWidth/2, polarWindowWidth/2), polarWindowY);
        pApplet.stroke(CP.ACCEL_Y_COLOR);
        pApplet.line(polarWindowX, polarWindowY, polarWindowX+map((sqrt(2)*displayY/2), -yMaxMin, yMaxMin, -polarWindowWidth/2, polarWindowWidth/2), polarWindowY+map((sqrt(2)*displayY/2), -yMaxMin, yMaxMin, polarWindowWidth/2, -polarWindowWidth/2));
        pApplet.stroke(CP.ACCEL_Z_COLOR);
        pApplet.line(polarWindowX, polarWindowY, polarWindowX, polarWindowY+map(displayZ, -yMaxMin, yMaxMin, polarWindowWidth/2, -polarWindowWidth/2));
        pApplet.strokeWeight(1);
    }

    //This public method allows Analog, Digital, and Pulse Widgets to turn off Accelerometer display
    //Happens only when buttons can be toggled
    public void accelBoardSetActive(boolean _value) {
        accelBoard.setAccelerometerActive(_value);
        String s = _value ? "Turn Accel. Off" : "Turn Accel. On";
        accelModeButton.getCaptionLabel().setText(s);
        if (_value) {
            accelModeButton.setOn();
        } else {
            accelModeButton.setOff();
        }
        //Hide button when set On for Cyton board only. This is a special case for Cyton board Aux mode behavior. See BoardCyton.pde for more info.
        if ((currentBoard instanceof BoardCyton)) {
            accelModeButton.setVisible(!_value);
        }
    }

};//end W_Accelerometer class