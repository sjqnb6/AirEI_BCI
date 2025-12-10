package W_TimeSeries_;

import FileBoard_.FileBoard;
import GUI.GUIManager;
import controlP5.Button;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlP5;
import processing.core.PApplet;
import processing.core.PImage;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import Globel.GUI;

import static Globel.GUI.*;

//========================== PLAYBACKSLIDER ==========================
class PlaybackScrollbar  {
    GUI MAIN;
    private final float ps_Padding = 50.0F; //used to make room for skip to start button
    private int x, y, w, h;
    private int swidth, sheight;    // width and height of bar
    private float xpos, ypos;       // x and y position of bar
    private float spos;    // x position of slider
    private float sposMin, sposMax; // max and min values of slider
    private boolean over;           // is the mouse over the slider?
    private boolean locked;
    private ControlP5 pbsb_cp5;
    private Button skipToStartButton;
    private int skipToStart_diameter;
    private String currentAbsoluteTimeToDisplay = "";
    private String currentTimeInSecondsToDisplay = "";
    private FileBoard fileBoard;

    private final DateFormat currentTimeFormatShort = new SimpleDateFormat("mm:ss");
    private final DateFormat currentTimeFormatLong = new SimpleDateFormat("HH:mm:ss");
    private final DateFormat timeStampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private PApplet applet;
    public PlaybackScrollbar (GUI MAIN, int _x, int _y, int _w, int _h, float xp, float yp, int sw, int sh) {
        this.MAIN = MAIN;
//        super(_parent);
        x = _x;
        y = _y;
        w = _w;
        h = _h;
        swidth = sw;
        sheight = sh;
        //float widthtoheight = sw - sh;
        //ratio = (float)sw / widthtoheight;
        xpos = xp + ps_Padding; //lots of padding to make room for button
        ypos = yp-sheight/2;
        spos = xpos;
        sposMin = xpos;
        sposMax = xpos + swidth - sheight/2;

        pbsb_cp5 = new ControlP5(MAIN);
        pbsb_cp5.setGraphics(MAIN, 0,0);
        pbsb_cp5.setAutoDraw(false);

        //Let's make a button to return to the start of playback!!
        skipToStart_diameter = 30;
        createSkipToStartButton("skipToStartButton", "", (int)(xp) + (int)(skipToStart_diameter*.5), (int)(yp) + (int)(sh/2) - skipToStart_diameter, skipToStart_diameter, skipToStart_diameter);

        fileBoard = (FileBoard)currentBoard;
    }

    private void createSkipToStartButton(String name, String text, int _x, int _y, int _w, int _h) {
        skipToStartButton = MAIN.createButton(pbsb_cp5, name, text, _x, _y, _w, _h, 0, p5, 12, MAIN.GREY_235, MAIN.OPENBCI_DARKBLUE, MAIN.BUTTON_HOVER, MAIN.BUTTON_PRESSED, (Integer)null, 0);
        PImage defaultImage = MAIN.loadImage("skipToStart_default-30x26.png");
        skipToStartButton.setImage(defaultImage);
        skipToStartButton.setForceDrawBackground(true);
        skipToStartButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                skipToStartButtonAction();
            }
        });
        skipToStartButton.setDescription("Click to go back to the beginning of the file.");
    }

    /////////////// Update loop for PlaybackScrollbar
    void update() {
        checkMouseOver(); // check if mouse is over

        if (MAIN.mousePressed && over) {
            locked = true;
        }
        if (!MAIN.mousePressed) {
            locked = false;
        }
        //if the slider is being used, update new position based on user mouseX
        if (locked) {
            spos = MAIN.constrain(MAIN.mouseX-sheight/2, sposMin, sposMax);
            scrubToPosition();
        }
        else {
            updateCursor();
        }

        // update timestamp
        currentAbsoluteTimeToDisplay = getAbsoluteTimeToDisplay();

        //update elapsed time to display
        currentTimeInSecondsToDisplay = getCurrentTimeToDisplaySeconds();

    } //end update loop for PlaybackScrollbar

    void updateCursor() {
        float currentSample = (float)(fileBoard.getCurrentSample());
        float totalSamples = (float)(fileBoard.getTotalSamples());
        float currentPlaybackPos = currentSample / totalSamples;

        spos =  MAIN.lerp(sposMin, sposMax, currentPlaybackPos);
    }

    void scrubToPosition() {
        int totalSamples = fileBoard.getTotalSamples();
        int newSamplePos = MAIN.floor(totalSamples * getCursorPercentage());

        fileBoard.goToIndex(newSamplePos);
    }

    float getCursorPercentage() {
        return (spos - sposMin) / (sposMax - sposMin);
    }

    String getAbsoluteTimeToDisplay() {
        List<double[]> currentData = currentBoard.getData(1);
        int timeStampChan = currentBoard.getTimestampChannel();
        long timestampMS = (long)(currentData.get(0)[timeStampChan] * 1000.0);
        if(timestampMS == 0) {
            return "";
        }

        return timeStampFormat.format(new Date(timestampMS));
    }

    String getCurrentTimeToDisplaySeconds() {
        double totalMillis = fileBoard.getTotalTimeSeconds() * 1000.0;
        double currentMillis = fileBoard.getCurrentTimeSeconds() * 1000.0;

        String totalTimeStr = formatCurrentTime(totalMillis);
        String currentTimeStr = formatCurrentTime(currentMillis);

        return currentTimeStr + " / " + totalTimeStr;
    }

    String formatCurrentTime(double millis) {
        DateFormat formatter = currentTimeFormatShort;
        if (millis >= 3600000.0) { // bigger than 60 minutes
            formatter = currentTimeFormatLong;
        }

        return formatter.format(new Date((long)millis));
    }

    //checks if mouse is over the playback scrollbar
    private void checkMouseOver() {
        if (MAIN.mouseX > xpos && MAIN.mouseX < xpos+swidth &&
                MAIN.mouseY > ypos && MAIN.mouseY < ypos+sheight) {
            if(!over) {
                onMouseEnter();
            }
        }
        else {
            if (over) {
                onMouseExit();
            }
        }
    }

    // called when the mouse enters the playback scrollbar
    private void onMouseEnter() {
        over = true;
        MAIN.cursor(MAIN.HAND); //changes cursor icon to a hand
    }

    private void onMouseExit() {
        over = false;
        MAIN.cursor(MAIN.ARROW);
    }

    public void draw() {
        MAIN.pushStyle();

        MAIN.fill(MAIN.GREY_235);
        MAIN.stroke(MAIN.OPENBCI_BLUE);
        MAIN.rect(x, y, w, h);

        //draw the playback slider inside the playback sub-widget
        MAIN.noStroke();
        MAIN.fill(MAIN.GREY_200);
        MAIN.rect(xpos, ypos, swidth, sheight);

        //select color for playback indicator
        if (over || locked) {
            MAIN.fill(MAIN.OPENBCI_DARKBLUE);
        } else {
            MAIN.fill(102, 102, 102);
        }
        //draws playback position indicator
        MAIN.rect(spos, ypos, sheight/2, sheight);

        //draw current timestamp and X of Y Seconds above scrollbar
        int fontSize = 17;
        MAIN.textFont(p2, fontSize);
        MAIN.fill(MAIN.OPENBCI_DARKBLUE);
        float tw = MAIN.textWidth(currentAbsoluteTimeToDisplay);
        MAIN.text(currentAbsoluteTimeToDisplay, xpos + swidth - tw, ypos - fontSize - 4);
        MAIN.text(currentTimeInSecondsToDisplay, xpos, ypos - fontSize - 4);

        MAIN.popStyle();

        pbsb_cp5.draw();
    }

    void screenResized(int _x, int _y, int _w, int _h, float _pbx, float _pby, float _pbw, float _pbh) {
        x = _x;
        y = _y;
        w = _w;
        h = _h;
        swidth = (int)(_pbw);
        sheight = (int)(_pbh);
        xpos = _pbx + ps_Padding; //add lots of padding for use
        ypos = _pby - sheight/2;
        sposMin = xpos;
        sposMax = xpos + swidth - sheight/2;
        //update the position of the playback indicator us
        //newspos = updatePos();

        pbsb_cp5.setGraphics(applet, 0, 0);

        skipToStartButton.setPosition(
            (int)(_pbx) + (int)(skipToStart_diameter*.5),
            (int)(_pby) - (int)(skipToStart_diameter*.5)
            );
    }

    //This function scrubs to the beginning of the playback file
    //Useful to 'reset' the scrollbar before loading a new playback file
    void skipToStartButtonAction() {
        fileBoard.goToIndex(0);
    }

};//end PlaybackScrollbar class