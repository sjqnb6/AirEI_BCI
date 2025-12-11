package W_DigitalRead_;

import DigitalCapableBoard_.DigitalCapableBoard;
import Extras_.TextBox;
import Widget_.Widget;
import processing.core.PApplet;

import java.util.List;

import Globel.GUI;
//========================================================================================================================
//                      Analog Voltage BAR CLASS -- Implemented by Analog Read Widget Class
//========================================================================================================================
//this class contains the plot and buttons for a single channel of the Time Series widget
//one of these will be created for each channel (4, 8, or 16)
class DigitalReadDot extends Widget {
    GUI MAIN;
    private int digitalInputPin;
    private int digitalInputVal;
    String digitalInputString;
    int padding;

    TextBox digitalValue;
    TextBox digitalPin;

    boolean drawDigitalValue;

    int dotStroke = 0xd2d2d2;
    int dot0Fill = 0xf5f5f5;
    int dot1Fill = 0xf5f5f5;
    int val0Fill = MAIN.OPENBCI_DARKBLUE;
    int val1Fill = MAIN.WHITE;

    int dotX;
    int dotY;
    int dotWidth;
    int dotHeight;
    float dotCorner;

    DigitalCapableBoard digitalBoard;

    public DigitalReadDot(GUI MAIN, int _digitalInputPin, int _x, int _y, int _w, int _h, int _padding) {
        super(MAIN); // channel number, x/y location, height, width
        this.MAIN = MAIN;
        digitalBoard = (DigitalCapableBoard)MAIN.currentBoard;

        digitalInputPin = _digitalInputPin;
        digitalInputString = MAIN.str(digitalInputPin);
        digitalInputVal = 0;

        if (digitalInputPin == 11) {
            dot1Fill = MAIN.channelColors[0];
        } else if (digitalInputPin == 12) {
            dot1Fill = MAIN.channelColors[1];
        } else if (digitalInputPin == 13) {
            dot1Fill = MAIN.channelColors[2];
        } else if (digitalInputPin == 17) {
            dot1Fill = MAIN.channelColors[3];
        } else { // 18
            dot1Fill = MAIN.channelColors[4];
        }

        dotX = _x;
        dotY = _y;
        dotWidth = _w;
        dotHeight = _h;
        padding = _padding;

        digitalValue = new TextBox(MAIN, "", dotX, dotY);
        digitalValue.setTextColor(val0Fill);
        digitalValue.alignH = MAIN.CENTER;
        digitalValue.alignV = MAIN.CENTER;
        drawDigitalValue = true;

        digitalPin = new TextBox(MAIN, "D" + digitalInputString, dotX, dotY - dotWidth);
        digitalPin.setTextColor(MAIN.OPENBCI_DARKBLUE);
        digitalPin.alignH = MAIN.CENTER;
    }

    public void update() {
        List<double[]> lastData = digitalBoard.getDataWithDigital(1);
        double[] lastSample = lastData.get(0);
        int[] digitalChannels = digitalBoard.getDigitalChannels();

        //update the voltage values
        if (digitalInputPin == 11) {
            digitalInputVal = (int)lastSample[digitalChannels[0]];
        } else if (digitalInputPin == 12) {
            digitalInputVal = (int)lastSample[digitalChannels[1]];
        } else if (digitalInputPin == 13) {
            digitalInputVal = (int)lastSample[digitalChannels[2]];
        } else if (digitalInputPin == 17) {
            digitalInputVal = (int)lastSample[digitalChannels[3]];
        } else {
            // 18
            digitalInputVal = (int)lastSample[digitalChannels[4]];
        }

        digitalValue.string = String.format("%d", digitalInputVal);
    }

    public void draw() {
        pApplet.pushStyle();

        if (digitalInputVal == 1) {
            pApplet.fill(dot1Fill);
            digitalValue.setTextColor(val1Fill);
        } else {
            pApplet.fill(dot0Fill);
            digitalValue.setTextColor(val0Fill);
        }
        pApplet.stroke(dotStroke);
        pApplet.ellipse(dotX, dotY, dotWidth, dotHeight);

        if (drawDigitalValue) {
            digitalValue.draw();
            digitalPin.draw();
        }

        pApplet.popStyle();
    }

    public int getDigitalReadVal() {
        return digitalInputVal;
    }

    void screenResized(int _x, int _y, int _w, int _h) {
        dotX = _x;
        dotY = _y;
        dotWidth = _w;
        dotHeight = _h;
        dotCorner = (MAIN.sqrt(2)*dotWidth/2)/2;

        digitalPin.x = dotX;
        digitalPin.y = dotY - (int)(dotWidth/2.0);

        digitalValue.x = dotX;
        digitalValue.y = dotY;
    }
};