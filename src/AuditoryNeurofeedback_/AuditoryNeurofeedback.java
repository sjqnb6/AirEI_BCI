package AuditoryNeurofeedback_;

import GUI.GUIManager;
import Globel.GUI;
import controlP5.Button;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlP5;
import processing.core.PApplet;

import static AuditoryNeurofeedback_.GVI.*;
import static Debugging_.GF.outputError;
import static GUI.GGVI.p5;
import static GUI.GGVI.topNav;
import static Globel.GUI.auditoryNfbFilePlayers;
import static processing.core.PApplet.map;

public class AuditoryNeurofeedback{

    private int x, y, w, h;
    private ControlP5 localCP5;
    public Button startStopButton;
    public Button modeButton;
    private boolean usingBandPowers = false;
    //There will always be 5 band powers, and 5 possible concurrent audio files for playback
    private final int NUM_SOUND_FILES = auditoryNfbFilePlayers.length;
    private final float MIN_GAIN = (float) -42.0;
    private final float MAX_GAIN = (float) -7.0;
    private final int MAX_BUTTON_W = 120;
    private int buttonW = 120;
    private int buttonH;

    private GUI MAIN;

    public AuditoryNeurofeedback(GUI MAIN, int _x, int _y, int _w, int _h) {
        localCP5 = new ControlP5(MAIN);
        localCP5.setGraphics(MAIN, 0,0);
        localCP5.setAutoDraw(false);
        buttonH = _h;
        createStartStopButton(_x, _y, buttonW, buttonH);
        createModeButton(_x, _y, buttonW, buttonH);

        this.MAIN = MAIN;
    }

    //Use band powers or prediction value to control volume of each sound file
    public void update(double[] bandPowers, float predictionVal) {
        if (!audioOutputIsAvailable) {return;}
        if (usingBandPowers) {
            for (int i = 0; i < NUM_SOUND_FILES; i++) {
                float gain = map((float)bandPowers[i], 0.1F, .7F, MIN_GAIN + 20f, MAX_GAIN);
                auditoryNfbGains[i].setValue(gain);
            }
        } else {
            float gain = map(predictionVal, 0.0F, 1.0F, MIN_GAIN, MAX_GAIN);
            for (int i = 0; i < NUM_SOUND_FILES; i++) {
                auditoryNfbGains[i].setValue(gain);
            }
        }
    }

    public void draw() {
        localCP5.draw();
    }

    public void screenResized(int _x, int _y, int _w, int _h) {
        localCP5.setGraphics(MAIN, 0, 0);
        buttonW = (_w - 6) / 2;
        buttonW = buttonW > MAX_BUTTON_W ? MAX_BUTTON_W : buttonW;
        startStopButton.setPosition(_x - buttonW - 3, _y);
        startStopButton.setSize(buttonW, _h);
        modeButton.setPosition(_x + 3, _y);
        modeButton.setSize(buttonW, _h);
    }

    public void killAudio() {
        if (!audioOutputIsAvailable) {return;}
        for (int i = 0; i < NUM_SOUND_FILES; i++) {
            auditoryNfbFilePlayers[i].pause();
            auditoryNfbFilePlayers[i].rewind();
        }
    }

    private void createStartStopButton(int _x, int _y, int _w, int _h) {
        //This is a generalized createButton method that allows us to save code by using a few patterns and method overloading
        startStopButton = MAIN.createButton(localCP5, "startStopButton", "Turn Audio On", _x, _y, _w, _h, p5, 12, MAIN.colorNotPressed, MAIN.OPENBCI_DARKBLUE);
        //Set the border color explicitely
        startStopButton.setBorderColor(MAIN.OBJECT_BORDER_GREY);
        //For this button, only call the callback listener on mouse release
        startStopButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                if (!audioOutputIsAvailable) {
                    outputError("AuditoryFeedback: Unable to load audio files. To enable this feature, please connect or turn on an audio device and restart the GUI.");
                    return;
                }
                //If using a TopNav object, ignore interaction with widget object (ex. widgetTemplateButton)
                if (!topNav.configSelector.isVisible && !topNav.layoutSelector.isVisible) {
                    if (auditoryNfbFilePlayers[0].isPlaying()) {
                        killAudio();
                        startStopButton.getCaptionLabel().setText("Turn Audio On");
                    } else {
                        for (int i = 0; i < NUM_SOUND_FILES; i++) {
                            auditoryNfbFilePlayers[i].loop();
                        }
                        startStopButton.getCaptionLabel().setText("Turn Audio Off");
                    }
                }
            }
        });
        startStopButton.setDescription("Start and Stop Auditory Feedback.");
    }

    private void createModeButton(int _x, int _y, int _w, int _h) {
        //This is a generalized createButton method that allows us to save code by using a few patterns and method overloading
        modeButton = MAIN.createButton(localCP5, "modeButton", "Use Band Powers", _x, _y, _w, _h, p5, 12, MAIN.colorNotPressed, MAIN.OPENBCI_DARKBLUE);
        //Set the border color explicitely
        modeButton.setBorderColor(MAIN.OBJECT_BORDER_GREY);
        //For this button, only call the callback listener on mouse release
        modeButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                //If using a TopNav object, ignore interaction with widget object (ex. widgetTemplateButton)
                if (!topNav.configSelector.isVisible && !topNav.layoutSelector.isVisible) {
                    String s = !usingBandPowers ? "Use Metric" : "Use Band Powers";
                    modeButton.getCaptionLabel().setText(s);
                    usingBandPowers = !usingBandPowers;
                }
            }
        });
        modeButton.setDescription("Change Auditory Feedback mode. Use the Metric to control all notes at once, or use Band Powers to control certain notes of the chord.");
    }

}
