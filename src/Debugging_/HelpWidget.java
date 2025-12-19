package Debugging_;

import processing.core.PApplet;
import Globel.GUI;

import static Globel.GUI.*;

public //this class is used to create the help widget that provides system feedback in response to interactivity
//it is intended to serve as a pseudo-console, allowing us to print useful information to the interface as opposed to an IDE console
class HelpWidget {

    public float x, y, w, h;
    int padding;

    //current text shown in help widget, based on most recent command
    String currentOutput = "了解如何使用该应用程序及更多内容，请 docs.openbci.com";
    OutputLevel curOutputLevel = OutputLevel.INFO;
    private int colorFadeCounter;
    private int colorFadeTimeMillis = 1000;
    private boolean outputWasTriggered = false;
    GUI MAIN;
    public HelpWidget(GUI MAIN, float _xPos, float _yPos, float _width, float _height) {
        this.MAIN = MAIN;
        x = _xPos;
        y = _yPos;
        w = _width;
        h = _height;
        padding = 5;
    }

    public void update() {
    }

    public void draw() {

        MAIN.pushStyle();

        if(colorScheme == COLOR_SCHEME_DEFAULT){
            // draw background of widget
            MAIN.stroke(MAIN.OPENBCI_DARKBLUE);
            MAIN.fill(255);
            MAIN.rect(-1, MAIN.height-h, MAIN.width+2, h);
            MAIN.noStroke();

            //draw bg of text field of widget
            MAIN.strokeWeight(1);
            MAIN.stroke(MAIN.color(0, 5, 11));
            MAIN.fill(MAIN.color(0, 5, 11));
            MAIN.rect(x + padding, MAIN.height-h + padding, MAIN.width - padding*2, h - padding *2);

            MAIN.textFont(p7);
            MAIN.textSize(14);
            MAIN.fill(255);
            MAIN.textAlign(MAIN.LEFT, MAIN.TOP);
            MAIN.text(currentOutput, padding*2, MAIN.height - h + padding);
        } else if (colorScheme == COLOR_SCHEME_ALTERNATIVE_A){
            // draw background of widget
            MAIN.stroke(MAIN.OPENBCI_DARKBLUE);
            MAIN.fill(MAIN.OPENBCI_BLUE);
            MAIN.rect(-1, MAIN.height-h, MAIN.width+2, h);
            MAIN.noStroke();

            //draw bg of text field of widget
            MAIN.strokeWeight(1);
            int saturationFadeValue = 0;
            if (outputWasTriggered) {
                int timeDelta = MAIN.millis() - colorFadeCounter;
                saturationFadeValue = (int)MAIN.map(timeDelta, 0, colorFadeTimeMillis, 100, 0);
                if (timeDelta > colorFadeTimeMillis) {
                    outputWasTriggered = false;
                }
            }
            //Colors in this method are calculated using Hue, Saturation, Brightness
            MAIN.colorMode(MAIN.HSB, 360, 100, 100);
            int c = getBackgroundColor(saturationFadeValue);
            MAIN.stroke(c);
            MAIN.fill(c);
            MAIN.rect(x + padding, MAIN.height-h + padding, MAIN.width - padding*2, h - padding *2);

            // Revert color mode back to standard RGB here
            MAIN.colorMode(MAIN.RGB, 255, 255, 255);
            MAIN.textFont(p7);
            MAIN.textSize(14);
            MAIN.fill(getTextColor());
            MAIN.textAlign(MAIN.LEFT, MAIN.TOP);
            MAIN.text(currentOutput, padding*2, MAIN.height - h + padding);
        }

        MAIN.popStyle();
    }

    private int getTextColor() {
        /*
        switch (curOutputLevel) {
            case INFO:
                return #00529B;
            case SUCCESS:
                return #4F8A10;
            case WARN:
                return #9F6000;
            case ERROR:
                return #D8000C;
            case DEFAULT:
            default:
                return color(0, 5, 11);
        }
        */
        return MAIN.OPENBCI_DARKBLUE;
    }

    private int getBackgroundColor(int fadeVal) {
        int sat = 0;
        int maxSat = 75;
        switch (curOutputLevel) {
            case INFO:
                //base color - #BDE5F8;
                sat = 25;
                sat = (int)MAIN.map(fadeVal, 0, 100, sat, maxSat);
                return MAIN.color(199, sat, 97);
            case SUCCESS:
                //base color -  #DFF2BF;
                maxSat = 25;
                sat = 0;
                sat = (int)MAIN.map(fadeVal, 0, 100, sat, maxSat);
                return MAIN.color(106, sat, 95);
            case WARN:
                //base color -  #FEEFB3;
                sat = 30;
                sat = (int)MAIN.map(fadeVal, 0, 100, sat, maxSat);
                return MAIN.color(48, sat, 100);
            case ERROR:
                //base color -  #FFD2D2;
                sat = 18;
                sat = (int)MAIN.map(fadeVal, 0, 100, sat, maxSat);
                return MAIN.color(0, sat, 100);
            case DEFAULT:
            default:
                MAIN.colorMode(MAIN.RGB, 255, 255, 255);
                return MAIN.WHITE;
        }
    }

    public void output(String _output, OutputLevel level) {
        curOutputLevel = level;
        currentOutput = _output;

        String outputWithPrefix = "[" + level.name() + "]: " + _output;
        MAIN.println(outputWithPrefix); // add this output to the console log
        outputWasTriggered = true;
        colorFadeCounter = MAIN.millis();
    }
};