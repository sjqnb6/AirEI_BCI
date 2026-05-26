package W_TimeSeries_;

import GUI.GUIManager;
import processing.core.PApplet;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import Globel.GUI;

import static Globel.GUI.streamTimeElapsed;

//========================== TimeDisplay ==========================
public class TimeDisplay{
    private static final int COLOR_TEXT = 0xFFEAF2FF;

    int swidth, sheight;    // width and height of bar
    float xpos, ypos;       // x and y position of bar
    String currentAbsoluteTimeToDisplay = "";
    Boolean updatePosition = false;
    LocalDateTime time;
    GUI MAIN;
    TimeDisplay (GUI MAIN,float xp, float yp, int sw, int sh) {
        this.MAIN = MAIN;
//        super(pApplet);
        swidth = sw;
        sheight = sh;
        xpos = xp; //lots of padding to make room for button
        ypos = yp;
        currentAbsoluteTimeToDisplay = fetchCurrentTimeString();
    }

    /////////////// Update loop for TimeDisplay when data stream is running
    void update() {
        if (MAIN.currentBoard.isStreaming()) {
            //Fetch Local time
            try {
                currentAbsoluteTimeToDisplay = fetchCurrentTimeString();
            } catch (NullPointerException e) {
                MAIN.println("TimeDisplay: Timestamp error...");
                e.printStackTrace();
            }

        }
    } //end update loop for TimeDisplay

    public void draw() {
        MAIN.pushStyle();
        //draw current timestamp at the bottom of the Widget container
        if (!currentAbsoluteTimeToDisplay.equals(null)) {
            int fontSize = 17;
            MAIN.textFont(MAIN.p2, fontSize);
            MAIN.fill(COLOR_TEXT);
            float tw = MAIN.textWidth(currentAbsoluteTimeToDisplay);
            MAIN.text(currentAbsoluteTimeToDisplay, xpos + swidth - tw, ypos);
            MAIN.text(streamTimeElapsed.toString(), xpos + 10, ypos);
        }
        MAIN.popStyle();
    }

    void screenResized(float _x, float _y, float _w, float _h) {
        swidth = (int)(_w);
        sheight = (int)(_h);
        xpos = _x;
        ypos = _y;
    }

    String fetchCurrentTimeString() {
        time = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return time.format(formatter);
    }
};//end TimeDisplay class
