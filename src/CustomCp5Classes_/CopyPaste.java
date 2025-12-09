package CustomCp5Classes_;

import controlP5.Textfield;
import processing.core.PApplet;

import static Extras_.GF.*;
import static GClip_.GClip.copy;
import static GClip_.GClip.paste;
import static processing.core.PApplet.println;

public class CopyPaste {

    private final int CMD_CNTL_KEYCODE = (isLinux() || isWindows()) ? 17 : 157;
    private final int C_KEYCODE = 67;
    private final int V_KEYCODE = 86;
    private boolean commandControlPressed;
    private boolean copyPressed;
    private String value;

    private PApplet applet;
    public CopyPaste(PApplet applet) {
        this.applet = applet;
    }

    public boolean checkIfPressedAllOS() {
        //This logic mimics the behavior of copy/paste in Mac OS X, and applied to all.
        if (applet.keyCode == CMD_CNTL_KEYCODE) {
            commandControlPressed = true;
            //println("KEYBOARD SHORTCUT: COMMAND PRESSED");
            return true;
        }

        if (commandControlPressed && applet.keyCode == V_KEYCODE) {
            //println("KEYBOARD SHORTCUT: PASTE PRESSED");
            // Get clipboard contents
            String s = paste();
            //println("FROM CLIPBOARD ~~ " + s);
            // Assign to stored value
            value = s;
            return true;
        }

        if (commandControlPressed && applet.keyCode == C_KEYCODE) {
            //println("KEYBOARD SHORTCUT: COPY PRESSED");
            copyPressed = true;
            return true;
        }

        return false;
    }

    public void checkIfReleasedAllOS() {
        if (applet.keyCode == CMD_CNTL_KEYCODE) {
            commandControlPressed = false;
        }
    }

    //Pull stored value from this class and set to null, otherwise return null.
    private String pullValue() {
        if (value == null) {
            return value;
        }
        String s = value;
        value = null;
        return s;
    }

    private void checkForPaste(Textfield tf) {
        if (value == null) {
            return;
        }

        if (tf.isFocus()) {
            StringBuilder status = new StringBuilder("OpenBCI_GUI: User pasted text from the clipboard into ");
            status.append(tf.toString());
            println(status);
            StringBuilder sb = new StringBuilder();
            String existingText = dropNonPrintableChars(tf.getText());
            String val = pullValue();
            //println("EXISTING TEXT =="+ existingText+ "__end. VALUE ==" + val + "__end.");

            // On Mac, Remove 'v' character from the end of the existing text
            existingText = existingText.length() > 0 && isMac() ? existingText.substring(0, existingText.length() - 1) : existingText;

            sb.append(existingText);
            sb.append(val);
            //The 'v' character does make it to the textfield, but this is immediately overwritten here.
            tf.setText(sb.toString());
        }
    }

    private void checkForCopy(Textfield tf) {
        if (!copyPressed) {
            return;
        }

        if (tf.isFocus()) {
            String s = dropNonPrintableChars(tf.getText());
            if (s.length() == 0) {
                return;
            }
            StringBuilder status = new StringBuilder("OpenBCI_GUI: User copied text from ");
            status.append(tf.toString());
            status.append(" to the clipboard");
            println(status);
            //println("FOUND TEXT =="+ s+"__end.");
            if (isMac()) {
                //Remove the 'c' character that was just typed in the textfield
                s = s.substring(0, s.length() - 1);
                tf.setText(s);
                //println("MAC FIXED TEXT =="+ s+"__end.");
            }
            boolean b = copy(s);
            copyPressed = false;
        }
    }

    public void checkForCopyPaste(Textfield tf) {
        checkForPaste(tf);
        checkForCopy(tf);
    }
}