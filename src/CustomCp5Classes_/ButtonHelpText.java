package CustomCp5Classes_;

import GUI.GUIManager;
import Globel.GUI;

import static Globel.GUI.*;
import static processing.core.PConstants.CENTER;
import static processing.core.PConstants.TOP;

public class ButtonHelpText {
    private int x, y, w, h;
    private String myText = "";
    private boolean isVisible;
    private int numLines;
    private int lineSpacing = 14;
    private int padding = 10;
    private int timeUserEnteredUIObject;
    private final int delay = 1000;
    private final int fadeInTime = 500;
    private float masterOpacity;
    private GUI MAIN;
    public ButtonHelpText(GUI MAIN){
        this.MAIN = MAIN;
    }

    public void setTimeUserEnteredUIObject() {
        timeUserEnteredUIObject = MAIN.millis();
        isVisible = true;
    }

    public void setVisible(boolean _isVisible){
        isVisible = _isVisible;
    }

    public void setButtonHelpText(String _myText, int _x, int _y){
        myText = _myText;
        x = _x;
        y = _y;
    }

    public void draw(){
        //When using expert mode, disable help text over UI objects
        if (!isVisible || guiSettings.getExpertModeBoolean()) {
            return;
        }

        int delta = MAIN.millis() - timeUserEnteredUIObject;
        boolean timeToShowHelpText =  delta > delay;

        if (timeToShowHelpText) {

            //Fade in the help text
            masterOpacity = (delta < delay + fadeInTime) ? MAIN.map(delta, delay, delay + fadeInTime, 0, 255) : 255f;

            MAIN.pushStyle();
            MAIN.textAlign(CENTER, TOP);

            MAIN.textFont(p7,12);
            MAIN.textLeading(lineSpacing); //line spacing
            MAIN.stroke(31,69,110, masterOpacity);
            MAIN.fill(255, masterOpacity);
            numLines = (int)((float)myText.length()/30.0) + 1; //add 1 to round up
            // println("numLines: " + numLines);
            //if on left side of screen, draw box brightness to prevent box off screen
            if(x <= MAIN.width/2){
                MAIN.rect(x, y, 200, 2*padding + numLines*lineSpacing + 4);
                MAIN.fill(31,69,110, masterOpacity); //text color
                MAIN.text(myText, x + padding, y + padding, 180, (numLines*lineSpacing + 4));
            } else{ //if on right side of screen, draw box left to prevent box off screen
                MAIN.rect(x - 200, y, 200, 2*padding + numLines*lineSpacing + 4);
                MAIN.fill(MAIN.OPENBCI_BLUE); //text color
                MAIN.text(myText, x + padding - 200, y + padding, 180, (numLines*lineSpacing + 4));
            }
            MAIN.popStyle();
        }
    }
};