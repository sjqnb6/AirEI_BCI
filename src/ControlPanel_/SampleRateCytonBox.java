package ControlPanel_;

import GUI.GUIManager;
import Globel.GUI;
import controlP5.Button;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlP5;
import processing.core.PApplet;

import static GUI.GGVI.h3;
import static processing.core.PApplet.println;
import static processing.core.PConstants.LEFT;
import static processing.core.PConstants.TOP;

public class SampleRateCytonBox{
    public int x, y, w, h, padding; //size and position
    private ControlP5 srcb_cp5;
    private Button sampleRate250;
    private Button sampleRate500;
    private Button sampleRate1000;
    private int sr250_butX;
    private int sr500_butX;
    private int sr1000_butX;
    private int srButton_butY;
    private GUI MAIN;
    SampleRateCytonBox(GUI MAIN, int _x, int _y, int _w, int _h, int _padding) {
        x = _x;
        y = _y;
        w = _w;
        h = 73;
        padding = _padding;
        this.MAIN = MAIN;
        //Instantiate local cp5 for this box
        srcb_cp5 = new ControlP5(MAIN);
        srcb_cp5.setGraphics(MAIN, 0,0);
        srcb_cp5.setAutoDraw(false);

        sr250_butX = x + padding;
        sr500_butX = x + padding*2 + (w-padding*4)/3;
        sr1000_butX = x + padding*3 + ((w-padding*4)/3)*2;
        srButton_butY =  y + padding*2 + 18;
        createSR250Button("cytonSR250", "250Hz", false, sr250_butX, srButton_butY, (w-padding*4)/3, 24);
        createSR500Button("cytonSR500", "500Hz", false, sr500_butX, srButton_butY, (w-padding*4)/3, 24);
        //Make 1000Hz option selected by default
        createSR1000Button("cytonSR1000", "1000Hz", true, sr1000_butX, srButton_butY, (w-padding*4)/3, 24);
    }

    public void update() {

    }

    public void draw() {

        srButton_butY =  y + padding*2 + 18;
        sampleRate250.setPosition(sr250_butX, srButton_butY);
        sampleRate500.setPosition(sr500_butX, srButton_butY);
        sampleRate1000.setPosition(sr1000_butX, srButton_butY);

        MAIN.pushStyle();
        MAIN.fill(MAIN.boxColor);
        MAIN.stroke(MAIN.boxStrokeColor);
        MAIN.strokeWeight(1);
        MAIN.rect(x, y, w, h);
        MAIN.fill(MAIN.OPENBCI_DARKBLUE);
        MAIN.textFont(h3, 16);
        MAIN.textAlign(LEFT, TOP);
        MAIN.text("SAMPLE RATE ", x + padding, y + padding);
        MAIN.fill(MAIN.OPENBCI_DARKBLUE); //set color to green
        MAIN.textFont(h3, 16);
        MAIN.textAlign(LEFT, TOP);
        MAIN.popStyle();

        srcb_cp5.draw();
    }

    private Button createSRCBButton(String name, String text, boolean isToggled, int _x, int _y, int _w, int _h) {
        final Button b = MAIN.createButton(srcb_cp5, name, text, _x, _y, _w, _h);
        b.setSwitch(true); //This turns the button into a switch
        if (isToggled) {
            b.setOn();
        }
        return b;
    }

    private void createSR250Button(String name, String text, boolean isToggled, int _x, int _y, int _w, int _h) {
        sampleRate250 = createSRCBButton(name, text, isToggled, _x, _y, _w, _h);
        sampleRate250.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                MAIN.selectedSamplingRate = 250;
                println("ControlPanel: User selected Cyton+WiFi 250Hz");
                sampleRate250.setOn();
                sampleRate500.setOff();
                sampleRate1000.setOff();
            }
        });
    }

    private void createSR500Button(String name, String text, boolean isToggled, int _x, int _y, int _w, int _h) {
        sampleRate500 = createSRCBButton(name, text, isToggled, _x, _y, _w, _h);
        sampleRate500.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                MAIN.selectedSamplingRate = 500;
                println("ControlPanel: User selected Cyton+WiFi 500Hz");
                sampleRate250.setOff();
                sampleRate500.setOn();
                sampleRate1000.setOff();
            }
        });
    }

    private void createSR1000Button(String name, String text, boolean isToggled, int _x, int _y, int _w, int _h) {
        sampleRate1000 = createSRCBButton(name, text, isToggled, _x, _y, _w, _h);
        sampleRate1000.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                MAIN.selectedSamplingRate = 1000;
                println("ControlPanel: User selected Cyton+WiFi 1000Hz");
                sampleRate250.setOff();
                sampleRate500.setOff();
                sampleRate1000.setOn();
            }
        });
    }
};