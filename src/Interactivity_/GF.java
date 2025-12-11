package Interactivity_;

import BoardCyton_.BoardCyton;
import BoardGanglion_.BoardGanglion;
import Board_.Board;
import processing.core.PApplet;

import static Containers_.GVI.drawContainers;
import static Debugging_.GF.*;
import static Globel.GUI.*;
import static WidgetManager_.GVI.w_marker;
import static processing.core.PApplet.println;
import static processing.core.PApplet.str;
import Globel.GUI;
public class GF {

    //interpret a keypress...the key pressed comes in as "key"
    public static void keyPressed(GUI MAIN) {

        char key = MAIN.key;

        // don't allow key presses until setup is complete and the UI is initialized
        if (!MAIN.setupComplete) {
            return;
        }

        //note that the Processing variable "key" is the keypress as an ASCII character
        //note that the Processing variable "keyCode" is the keypress as a JAVA keycode.  This differs from ASCII
        //println("OpenBCI_GUI: keyPressed: key = " + key + ", int(key) = " + int(key) + ", keyCode = " + keyCode);

        //Check for Copy/Paste text keyboard shortcuts before anything else.
        if (MAIN.copyPaste.checkIfPressedAllOS()) {
            return;
        }

        boolean anyActiveTextfields = MAIN.textfieldUpdateHelper.getAnyTextfieldsActive();

        if(!MAIN.controlPanel.isOpen && !anyActiveTextfields){ //don't parse the key if the control panel is open
            if (MAIN.guiSettings.getExpertModeBoolean() || key == ' ') { //Check if Expert Mode is On or Spacebar has been pressed
                if (((int)(key) >=32) && ((int)(key) <= 126)) {  //32 through 126 represent all the usual printable ASCII characters
                    parseKey(MAIN, key);
                }
            }
        }

        if(key==27){
            key=0; //disable 'esc' quitting program
        }
    }

    public static synchronized void keyReleased(GUI MAIN) {

        MAIN.copyPaste.checkIfReleasedAllOS();
    }

    public static void parseKey(GUI MAIN, char val) {
        //assumes that val is a usual printable ASCII character (ASCII 32 through 126)
        switch (val) {
            case ' ':
                // space to start/stop the stream
                topNav.stopButtonWasPressed();
                return;
            case ',':
                drawContainers = !drawContainers;
                return;
            case '{':
                if(MAIN.colorScheme == MAIN.COLOR_SCHEME_DEFAULT){
                    MAIN.colorScheme = MAIN.COLOR_SCHEME_ALTERNATIVE_A;
                } else if(MAIN.colorScheme == MAIN.COLOR_SCHEME_ALTERNATIVE_A) {
                    MAIN.colorScheme = MAIN.COLOR_SCHEME_DEFAULT;
                }
                //topNav.updateNavButtonsBasedOnColorScheme();
                output("New Dark color scheme coming soon!");
                return;

            //deactivate channels 1-4
            case '1':
                MAIN.currentBoard.setEXGChannelActive(1-1, false);
                return;
            case '2':
                MAIN.currentBoard.setEXGChannelActive(2-1, false);
                return;
            case '3':
                MAIN.currentBoard.setEXGChannelActive(3-1, false);
                return;
            case '4':
                MAIN.currentBoard.setEXGChannelActive(4-1, false);
                return;

            //activate channels 1-4
            case '!':
                MAIN.currentBoard.setEXGChannelActive(1-1, true);
                return;
            case '@':
                MAIN.currentBoard.setEXGChannelActive(2-1, true);
                return;
            case '#':
                MAIN.currentBoard.setEXGChannelActive(3-1, true);
                return;
            case '$':
                MAIN.currentBoard.setEXGChannelActive(4-1, true);
                return;


            ///////////////////// Save User settings lowercase n
            case 'n':
                println("Interactivity: Save key pressed!");
                MAIN.settings.save(MAIN.settings.getPath("User", MAIN.eegDataSource, MAIN.nchan));
                outputSuccess("Settings Saved! Using Expert Mode, you can load these settings using 'N' key. Click \"Default\" to revert to factory settings.");
                return;

            ///////////////////// Load User settings uppercase N
            case 'N':
                println("Interactivity: Load key pressed!");
                MAIN.settings.loadKeyPressed();
                return;

            case '?':
                if(MAIN.currentBoard instanceof BoardCyton) {
                    ((BoardCyton)MAIN.currentBoard).printRegisters();
                }
                return;

            case 'm':
                String picfname = "OpenBCI-" + MAIN.directoryManager.getFileNameDateTime() + ".jpg";
                //println("OpenBCI_GUI: 'm' was pressed...taking screenshot:" + picfname);
                MAIN.saveFrame(MAIN.directoryManager.getGuiDataPath() + "Screenshots" + System.getProperty("file.separator") + picfname);    // take a shot of that!
                output("Screenshot captured! Saved to /Documents/OpenBCI_GUI/Screenshots/" + picfname);
                return;
            default:
                break;
        }

        if (MAIN.nchan > 4) {
            switch (val) {
                case '5':
                    MAIN.currentBoard.setEXGChannelActive(5-1, false);
                    return;
                case '6':
                    MAIN.currentBoard.setEXGChannelActive(6-1, false);
                    return;
                case '7':
                    MAIN.currentBoard.setEXGChannelActive(7-1, false);
                    return;
                case '8':
                    MAIN.currentBoard.setEXGChannelActive(8-1, false);
                    return;
                case '%':
                    MAIN.currentBoard.setEXGChannelActive(5-1, true);
                    return;
                case '^':
                    MAIN.currentBoard.setEXGChannelActive(6-1, true);
                    return;
                case '&':
                    MAIN.currentBoard.setEXGChannelActive(7-1, true);
                    return;
                case '*':
                    MAIN.currentBoard.setEXGChannelActive(8-1, true);
                    return;
                default:
                    break;
            }
        }

        if (MAIN.nchan > 8) {
            switch (val) {
                case 'q':
                    MAIN.currentBoard.setEXGChannelActive(9-1, false);
                    return;
                case 'w':
                    MAIN.currentBoard.setEXGChannelActive(10-1, false);
                    return;
                case 'e':
                    MAIN.currentBoard.setEXGChannelActive(11-1, false);
                    return;
                case 'r':
                    MAIN.currentBoard.setEXGChannelActive(12-1, false);
                    return;
                case 't':
                    MAIN.currentBoard.setEXGChannelActive(13-1, false);
                    return;
                case 'y':
                    MAIN.currentBoard.setEXGChannelActive(14-1, false);
                    return;
                case 'u':
                    MAIN.currentBoard.setEXGChannelActive(15-1, false);
                    return;
                case 'i':
                    MAIN.currentBoard.setEXGChannelActive(16-1, false);
                    return;
                case 'Q':
                    MAIN.currentBoard.setEXGChannelActive(9-1, true);
                    return;
                case 'W':
                    MAIN.currentBoard.setEXGChannelActive(10-1, true);
                    return;
                case 'E':
                    MAIN.currentBoard.setEXGChannelActive(11-1, true);
                    return;
                case 'R':
                    MAIN.currentBoard.setEXGChannelActive(12-1, true);
                    return;
                case 'T':
                    MAIN.currentBoard.setEXGChannelActive(13-1, true);
                    return;
                case 'Y':
                    MAIN.currentBoard.setEXGChannelActive(14-1, true);
                    return;
                case 'U':
                    MAIN.currentBoard.setEXGChannelActive(15-1, true);
                    return;
                case 'I':
                    MAIN.currentBoard.setEXGChannelActive(16-1, true);
                    return;
                default:
                    break;
            }
        }

        // Fixes #976. These keyboard shortcuts enable synthetic square waves on Ganglion and Cyton
        if (MAIN.currentBoard instanceof BoardGanglion || MAIN.currentBoard instanceof BoardCyton) {
            if (val == '[' ||  val == ']') {
                println("Expert Mode: '" + val + "' pressed. Sending to Ganglion...");
                Boolean success = ((Board)MAIN.currentBoard).sendCommand(str(val)).getKey();
                if (success) {
                    outputSuccess("Expert Mode: Success sending '" + val + "' to Ganglion!");
                } else {
                    outputWarn("Expert Mode: Error sending '" + val + "' to Ganglion. Try again with data stream stopped.");
                }
                return;
            }
        }

        // Check for software marker keyboard shortcuts
        if (w_marker.checkForMarkerKeyPress(val)) {
            return;
        }

        if (MAIN.currentBoard instanceof Board) {
            output("Expert Mode: '" + MAIN.key + "' pressed. This is not assigned or applicable to current setup.");
            //((Board)currentBoard).sendCommand(str(key));
        }
    }

    public static void mouseDragged() {

        if (systemMode >= SYSTEMMODE_POSTINIT) {

            //calling mouse dragged inly outside of Control Panel
            if (controlPanel.isOpen == false) {
                wm.mouseDragged();
            }
        }
    }
    //switch yard if a click is detected
    public static synchronized void mousePressed(PApplet applet) {
        // don't allow mouse clicks until setup is complete and the UI is initialized
        if (!setupComplete) {
            return;
        }
        // verbosePrint("OpenBCI_GUI: mousePressed: mouse pressed");
        // println("systemMode" + systemMode);

        //if not before "START SESSION" ... i.e. after initial setup
        if (systemMode >= SYSTEMMODE_POSTINIT) {

            //limit interactivity of main GUI if control panel is open
            if (controlPanel.isOpen == false) {
                //was the stopButton pressed?

                wm.mousePressed();

            }
        }

        //topNav is always clickable
        topNav.mousePressed();

        //interacting with control panel
        if (controlPanel.isOpen) {
            //close control panel if you click outside...
            if (systemMode == SYSTEMMODE_POSTINIT) {
                if (applet.mouseX > 0 && applet.mouseX < controlPanel.w && applet.mouseY > 0 && applet.mouseY < controlPanel.initBox.y+controlPanel.initBox.h) {
                    println("OpenBCI_GUI: mousePressed: clicked in CP box");
                }
                //if clicked out of panel
                else {
                    println("OpenBCI_GUI: mousePressed: outside of CP clicked");
                    controlPanel.close();
                    topNav.controlPanelCollapser.setOff();
                }
            }
        }
    }

    public static synchronized void mouseReleased() {
        // don't allow mouse clicks until setup is complete and the UI is initialized
        if (!setupComplete) {
            return;
        }

        // gui.mouseReleased();
        topNav.mouseReleased();

        if (systemMode >= SYSTEMMODE_POSTINIT) {

            // GUIWidgets_mouseReleased(); // to replace GUI_Manager version (above) soon... cdr 7/25/16
            wm.mouseReleased();
        }
    }

    //Global function used to open a url in default browser, usually after pressing a button
    public static void openURLInBrowser(String _url){
        try {
            //Set your page url in this string. For eg, I m using URL for Google Search engine
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(_url));
            output("Opening URL: " + _url);
        }
        catch (java.io.IOException e) {
            //println(e.getMessage());
            println("Error launching url in browser: " + _url);
        }
    }
}
