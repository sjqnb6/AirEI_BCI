package CustomCp5Classes_;

import controlP5.Textfield;

import static GUI.GGVI.copyPaste;

public class TextFieldUpdateHelper {

    // textFieldIsActive is used to ignore hotkeys when a textfield is active. Resets to false on every draw loop.
    private boolean textFieldIsActive = false;

    public TextFieldUpdateHelper() {

    }

    public void resetTextFieldIsActive() {
        textFieldIsActive = false;
    }

    public boolean getAnyTextfieldsActive() {
        return textFieldIsActive;
    }

    public void checkTextfield(Textfield tf) {
        if (tf.isVisible()) {
            tf.setUpdate(true);
            if (tf.isFocus()) {
                textFieldIsActive = true;
                copyPaste.checkForCopyPaste(tf);
            }
        } else {
            tf.setUpdate(false);
        }
    }
}
