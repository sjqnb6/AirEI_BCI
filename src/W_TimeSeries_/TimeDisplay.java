package W_TimeSeries_;

import GUI.GUIManager;
import processing.core.PApplet;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static GUI.GGVI.*;

//========================== TimeDisplay ==========================
public class TimeDisplay extends GUIManager {
    int swidth, sheight;    // width and height of bar
    float xpos, ypos;       // x and y position of bar
    String currentAbsoluteTimeToDisplay = "";
    Boolean updatePosition = false;
    LocalDateTime time;

    TimeDisplay (PApplet pApplet,float xp, float yp, int sw, int sh) {
//        super(pApplet);
        swidth = sw;
        sheight = sh;
        xpos = xp; //lots of padding to make room for button
        ypos = yp;
        currentAbsoluteTimeToDisplay = fetchCurrentTimeString();
    }

    /////////////// Update loop for TimeDisplay when data stream is running
    void update() {
        if (currentBoard.isStreaming()) {
            //Fetch Local time
            try {
                currentAbsoluteTimeToDisplay = fetchCurrentTimeString();
            } catch (NullPointerException e) {
                println("TimeDisplay: Timestamp error...");
                e.printStackTrace();
            }

        }
    } //end update loop for TimeDisplay

    public void draw() {
        pushStyle();
        //draw current timestamp at the bottom of the Widget container
        if (!currentAbsoluteTimeToDisplay.equals(null)) {
            int fontSize = 17;
            textFont(p2, fontSize);
            fill(OPENBCI_DARKBLUE);
            float tw = textWidth(currentAbsoluteTimeToDisplay);
            text(currentAbsoluteTimeToDisplay, xpos + swidth - tw, ypos);
            text(streamTimeElapsed.toString(), xpos + 10, ypos);
        }
        popStyle();
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