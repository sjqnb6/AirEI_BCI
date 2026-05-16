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
import static Globel.GUI.*;
import static WidgetManager_.GVI.*;
import Globel.GUI;
////////////////////////////////////////////////////

public class W_Accelerometer extends Widget {

    GUI MAIN;
    protected PApplet pApplet;
    public ColorPalette CP;
    // Match W_Prediction industrial gray palette.
    private static final int COLOR_BG = 226;
    private static final int COLOR_PANEL = 233;
    private static final int COLOR_CARD = 239;
    private static final int COLOR_BORDER = 168;
    private static final int COLOR_BORDER_DARK = 128;
    private static final int COLOR_TEXT = 36;
    private static final int COLOR_TEXT_SECONDARY = 74;
    private static final int COLOR_GRID = 184;
    private static final int COLOR_BUTTON = 239;
    private static final int COLOR_BUTTON_ACTIVE = 218;
    private static final int COLOR_BUTTON_LOCKED = 185;
    private static final int COLOR_BUTTON_HOVER = 228;
    private static final int COLOR_BUTTON_PRESSED = 212;

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

    public W_Accelerometer(GUI MAIN) {
        super(MAIN); //calls the parent CONSTRUCTOR method of Widget (DON'T REMOVE)
        this.MAIN = MAIN;
        CP = new ColorPalette(MAIN);

        pApplet = MAIN;

        accelBoard = (AccelerometerCapableBoard)MAIN.currentBoard;

        //Default dropdown settings
        MAIN.settings.accVertScaleSave = 0;
        MAIN.settings.accHorizScaleSave = 3;

        //Make dropdowns
        addDropdown("accelVertScale", "垂直刻度", Arrays.asList(MAIN.settings.accVertScaleArray), MAIN.settings.accVertScaleSave);
        addDropdown("accelDuration", "窗口", Arrays.asList(MAIN.settings.accHorizScaleArray), MAIN.settings.accHorizScaleSave);

        setGraphDimensions();
        yMaxMin = adjustYMaxMinBasedOnSource();

        //XYZ buffer for bottom graph
        lastAccelVals = new float[NUM_ACCEL_DIMS];

        //create our channel bar and populate our accelerometerBar array!
        accelerometerBar = new AccelerometerBar(MAIN, accelXyzLimit, accelGraphX, accelGraphY, accelGraphWidth, accelGraphHeight);
        accelerometerBar.adjustTimeAxis(xLimOptions[MAIN.settings.accHorizScaleSave]);
        accelerometerBar.adjustVertScale(yLimOptions[MAIN.settings.accVertScaleSave]);

        createAccelModeButton("accelModeButton", "Turn Accel. Off", (int)(x + 1), (int)(y0 + navHeight + 1), 120, navHeight - 3, p5, 12, COLOR_BUTTON, COLOR_TEXT);
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
        return accelHorizLimit * ((AccelerometerCapableBoard)MAIN.currentBoard).getAccelSampleRate();
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

        if(!accelBoard.canDeactivateAccelerometer() && !(MAIN.currentBoard instanceof BoardCyton)) {
            accelModeButton.getCaptionLabel().setText("Accel. On");
            accelModeButton.setLock(true);
        } else {
            accelModeButton.setLock(false);
        }
        accelModeButton.setVisible(false);
        refreshAccelModeButtonStyle();
    }

    public float getLastAccelVal(int val) {
        return lastAccelVals[val];
    }

    public void draw() {
        super.draw(); //calls the parent draw() method of Widget (DON'T REMOVE)

        MAIN.pushStyle();

        MAIN.noStroke();
        MAIN.fill(COLOR_BG);
        MAIN.rect(x, y - 1, w, h + 1);

        MAIN.fill(COLOR_TEXT_SECONDARY);
        MAIN.textFont(p4, 14);
        MAIN.textAlign(MAIN.CENTER,MAIN.CENTER);
        MAIN.text("z", polarWindowX, (polarWindowY-polarWindowHeight/2)-12);
        MAIN.text("x", (polarWindowX+polarWindowWidth/2)+8, polarWindowY-5);
        MAIN.text("y", (polarWindowX+polarCorner)+10, (polarWindowY-polarCorner)-10);

        MAIN.fill(COLOR_PANEL);
        MAIN.noStroke();
        MAIN.rect(accelGraphX, accelGraphY, accelGraphWidth-accPadding*2, accelGraphHeight);

        MAIN.fill(COLOR_CARD);  //pulse window background
        MAIN.stroke(COLOR_BORDER);
        MAIN.ellipse(polarWindowX,polarWindowY,polarWindowWidth,polarWindowHeight);

        MAIN.stroke(COLOR_GRID);
        MAIN.line(polarWindowX-polarWindowWidth/2, polarWindowY, polarWindowX+polarWindowWidth/2, polarWindowY);
        MAIN.line(polarWindowX, polarWindowY-polarWindowHeight/2, polarWindowX, polarWindowY+polarWindowHeight/2);
        MAIN.line(polarWindowX-polarCorner, polarWindowY+polarCorner, polarWindowX+polarCorner, polarWindowY-polarCorner);

        if (accelBoard.isAccelerometerActive()) {
            drawAccValues();
            draw3DGraph();
        }

        MAIN.popStyle();

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
        polarCorner = (MAIN.sqrt(2)*polarWindowWidth/2)/2;
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
        accelModeButton = MAIN.createButton(cp5_widget, name, text, _x, _y, _w, _h, 0, _font, _fontSize, _bg, _textColor, COLOR_BUTTON_HOVER, COLOR_BUTTON_PRESSED, COLOR_BORDER_DARK, 0);
        refreshAccelModeButtonStyle();
        accelModeButton.setSwitch(true);
        accelModeButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                if (!accelBoard.isAccelerometerActive()) {
                    accelBoard.setAccelerometerActive(true);
                    output("Starting to read accelerometer");
                    accelModeButton.getCaptionLabel().setText("Turn Accel. Off");
                    if (MAIN.currentBoard instanceof DigitalCapableBoard) {
                        w_digitalRead.toggleDigitalReadButton(false);
                    }
                    if (MAIN.currentBoard instanceof AnalogCapableBoard) {
                        w_pulsesensor.toggleAnalogReadButton(false);
                        w_analogRead.toggleAnalogReadButton(false);
                    }
                    ///Hide button when set On for Cyton board only. This is a special case for Cyton board Aux mode behavior. See BoardCyton.pde for more info.
                    if ((MAIN.currentBoard instanceof BoardCyton)) {
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
                accelModeButton.setVisible(false);
                refreshAccelModeButtonStyle();
            }
        });
        accelModeButton.setDescription("Click to activate/deactivate the accelerometer for capable boards.");
        if (accelBoard.canDeactivateAccelerometer() || (MAIN.currentBoard instanceof BoardCyton)) {
            //Set button switch to On of it can be toggled
            accelModeButton.setOn();
            //Hide button when set On for Cyton board only. This is a special case for Cyton board Aux mode behavior. See BoardCyton.pde for more info.
            if ((MAIN.currentBoard instanceof BoardCyton)) {
                accelModeButton.setVisible(false);
            }
        }
        accelModeButton.setVisible(false);
        refreshAccelModeButtonStyle();
    }

    //Draw the current accelerometer values as text
    void drawAccValues() {
        float displayX = (float)lastAccelVals[0];
        float displayY = (float)lastAccelVals[1];
        float displayZ = (float)lastAccelVals[2];
        MAIN.textAlign(MAIN.LEFT,MAIN.CENTER);
        MAIN.textFont(h1,20);
        MAIN.fill(CP.ACCEL_X_COLOR);
        MAIN.text("X = " + MAIN.nf(displayX, 1, 3) + " g", (float) (x+accPadding), (float) (y + (h/12)*1.5 - 5));
        MAIN.fill(CP.ACCEL_Y_COLOR);
        MAIN.text("Y = " + MAIN.nf(displayY, 1, 3) + " g", x+accPadding, y + (h/12)*3 - 5);
        MAIN.fill(CP.ACCEL_Z_COLOR);
        MAIN.text("Z = " + MAIN.nf(displayZ, 1, 3) + " g", (float) (x+accPadding), (float) (y + (h/12)*4.5 - 5));
    }

    //Draw the current accelerometer values as a 3D graph
    void draw3DGraph() {
        float displayX = (float)lastAccelVals[0];
        float displayY = (float)lastAccelVals[1];
        float displayZ = (float)lastAccelVals[2];

        MAIN.noFill();
        MAIN.strokeWeight(3);
        MAIN.stroke(CP.ACCEL_X_COLOR);
        MAIN.line(polarWindowX, polarWindowY, polarWindowX+MAIN.map(displayX, -yMaxMin, yMaxMin, -polarWindowWidth/2, polarWindowWidth/2), polarWindowY);
        MAIN.stroke(CP.ACCEL_Y_COLOR);
        MAIN.line(polarWindowX, polarWindowY, polarWindowX+MAIN.map((MAIN.sqrt(2)*displayY/2), -yMaxMin, yMaxMin, -polarWindowWidth/2, polarWindowWidth/2), polarWindowY+MAIN.map((MAIN.sqrt(2)*displayY/2), -yMaxMin, yMaxMin, polarWindowWidth/2, -polarWindowWidth/2));
        MAIN.stroke(CP.ACCEL_Z_COLOR);
        MAIN.line(polarWindowX, polarWindowY, polarWindowX, polarWindowY+MAIN.map(displayZ, -yMaxMin, yMaxMin, polarWindowWidth/2, -polarWindowWidth/2));
        MAIN.strokeWeight(1);
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
        if ((MAIN.currentBoard instanceof BoardCyton)) {
            accelModeButton.setVisible(!_value);
        }
        accelModeButton.setVisible(false);
        refreshAccelModeButtonStyle();
    }

    private void refreshAccelModeButtonStyle() {
        if (accelModeButton == null) {
            return;
        }
        int bg = accelModeButton.isLock() ? COLOR_BUTTON_LOCKED :
                (accelBoard.isAccelerometerActive() ? COLOR_BUTTON_ACTIVE : COLOR_BUTTON);
        accelModeButton.setColorBackground(bg);
        accelModeButton.setColorForeground(COLOR_BUTTON_HOVER);
        accelModeButton.setColorActive(COLOR_BUTTON_PRESSED);
        accelModeButton.setBorderColor(COLOR_BORDER_DARK);
        accelModeButton.getCaptionLabel().setColor(COLOR_TEXT);
    }

};//end W_Accelerometer class
