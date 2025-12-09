package Debugging_;

import GUI.GUIManager;

import static GUI.GGVI.*;

public //this class is used to create the help widget that provides system feedback in response to interactivity
//it is intended to serve as a pseudo-console, allowing us to print useful information to the interface as opposed to an IDE console
class HelpWidget extends GUIManager {

    public float x, y, w, h;
    int padding;

    //current text shown in help widget, based on most recent command
    String currentOutput = "Learn how to use this application and more at docs.openbci.com";
    OutputLevel curOutputLevel = OutputLevel.INFO;
    private int colorFadeCounter;
    private int colorFadeTimeMillis = 1000;
    private boolean outputWasTriggered = false;

    public HelpWidget(float _xPos, float _yPos, float _width, float _height) {
        x = _xPos;
        y = _yPos;
        w = _width;
        h = _height;
        padding = 5;
    }

    public void update() {
    }

    public void draw() {

        pushStyle();

        if(colorScheme == COLOR_SCHEME_DEFAULT){
            // draw background of widget
            stroke(OPENBCI_DARKBLUE);
            fill(255);
            rect(-1, height-h, width+2, h);
            noStroke();

            //draw bg of text field of widget
            strokeWeight(1);
            stroke(color(0, 5, 11));
            fill(color(0, 5, 11));
            rect(x + padding, height-h + padding, width - padding*2, h - padding *2);

            textFont(p4);
            textSize(14);
            fill(255);
            textAlign(LEFT, TOP);
            text(currentOutput, padding*2, height - h + padding);
        } else if (colorScheme == COLOR_SCHEME_ALTERNATIVE_A){
            // draw background of widget
            stroke(OPENBCI_DARKBLUE);
            fill(OPENBCI_BLUE);
            rect(-1, height-h, width+2, h);
            noStroke();

            //draw bg of text field of widget
            strokeWeight(1);
            int saturationFadeValue = 0;
            if (outputWasTriggered) {
                int timeDelta = millis() - colorFadeCounter;
                saturationFadeValue = (int)map(timeDelta, 0, colorFadeTimeMillis, 100, 0);
                if (timeDelta > colorFadeTimeMillis) {
                    outputWasTriggered = false;
                }
            }
            //Colors in this method are calculated using Hue, Saturation, Brightness
            colorMode(HSB, 360, 100, 100);
            int c = getBackgroundColor(saturationFadeValue);
            stroke(c);
            fill(c);
            rect(x + padding, height-h + padding, width - padding*2, h - padding *2);

            // Revert color mode back to standard RGB here
            colorMode(RGB, 255, 255, 255);
            textFont(p4);
            textSize(14);
            fill(getTextColor());
            textAlign(LEFT, TOP);
            text(currentOutput, padding*2, height - h + padding);
        }

        popStyle();
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
        return OPENBCI_DARKBLUE;
    }

    private int getBackgroundColor(int fadeVal) {
        int sat = 0;
        int maxSat = 75;
        switch (curOutputLevel) {
            case INFO:
                //base color - #BDE5F8;
                sat = 25;
                sat = (int)map(fadeVal, 0, 100, sat, maxSat);
                return color(199, sat, 97);
            case SUCCESS:
                //base color -  #DFF2BF;
                maxSat = 25;
                sat = 0;
                sat = (int)map(fadeVal, 0, 100, sat, maxSat);
                return color(106, sat, 95);
            case WARN:
                //base color -  #FEEFB3;
                sat = 30;
                sat = (int)map(fadeVal, 0, 100, sat, maxSat);
                return color(48, sat, 100);
            case ERROR:
                //base color -  #FFD2D2;
                sat = 18;
                sat = (int)map(fadeVal, 0, 100, sat, maxSat);
                return color(0, sat, 100);
            case DEFAULT:
            default:
                colorMode(RGB, 255, 255, 255);
                return WHITE;
        }
    }

    public void output(String _output, OutputLevel level) {
        curOutputLevel = level;
        currentOutput = _output;

        String outputWithPrefix = "[" + level.name() + "]: " + _output;
        println(outputWithPrefix); // add this output to the console log
        outputWasTriggered = true;
        colorFadeCounter = millis();
    }
};