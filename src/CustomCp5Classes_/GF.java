package CustomCp5Classes_;

import GUI.ColorPalette;
import GUI.GUIManager;
import controlP5.Button;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlP5;
import processing.core.PFont;

import static GUI.GGVI.buttonHelpText;
import static GUI.GGVI.p5;

public class GF extends GUIManager {

    //Reusable method for creating CP5 buttons throughout the GUI
    public Button createButton(ControlP5 _cp5, String name, String text, int _x, int _y, int _w, int _h, int _roundness, PFont _font, int _fontSize, int _bgColor, int _textColor, int _colorHover, int _colorPressed, Integer _strokeColor, int _marginTop) {
        final Button b = _cp5.addButton(name)
                .setPosition(_x, _y)
                .setSize(_w, _h)
                .setColorLabel(_textColor)
                .setCornerRoundness(_roundness) //From Processing rect(): To draw a rounded rectangle, add a fifth parameter, which is used as the radius value for all four corners.
                .setColorForeground(_colorHover)
                .setColorBackground(_bgColor)
                .setColorActive(_colorPressed)
                .setBorderColor(_strokeColor)
                ;
        b.getCaptionLabel()
                .setFont(_font)
                .toUpperCase(false)
                .setSize(_fontSize)
                .setText(text)
                .setColor(_textColor) //This sets the color of the button label
                .getStyle()
                .setMarginTop(_marginTop)
        ;
        //Add Help Text to all Buttons. If description is null or object is locked, take no action.
        b.addCallback(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                if (theEvent.getAction() == ControlP5.ACTION_ENTER && !b.isLock() && b.getDescription() != null) {
                    //Show helpt text if object is not locked and has a description
                    buttonHelpText.setButtonHelpText(b.getDescription(), (int)b.getPosition()[0] + b.getWidth()/2, (int)b.getPosition()[1] + (3*b.getHeight())/4);
                    buttonHelpText.setTimeUserEnteredUIObject();
                } else if (theEvent.getAction() == ControlP5.ACTION_LEAVE || theEvent.getAction() == ControlP5.ACTION_BROADCAST) {
                    //Hide help text if clicked or user's mouse leaves object
                    buttonHelpText.setVisible(false);
                }
            }
        });
        return b;
    }

    //Square corners and no text label adjustment w/ default hover and press colors
    public Button createButton(ControlP5 _cp5, String name, String text, int _x, int _y, int _w, int _h, PFont _font, int _fontSize, int _bgColor, int _textColor) {
        return createButton(_cp5, name, text, _x, _y, _w, _h, 0, _font, _fontSize, _bgColor, _textColor, BUTTON_HOVER, BUTTON_PRESSED, OPENBCI_DARKBLUE, 0);
    }

    //Default button colors and fonts
    public Button createButton(ControlP5 _cp5, String name, String text, int _x, int _y, int _w, int _h, ColorPalette CP) {
        return createButton(_cp5, name, text, _x, _y, _w, _h, 0, p5, 12, colorNotPressed, OPENBCI_DARKBLUE, BUTTON_HOVER, BUTTON_PRESSED, OPENBCI_DARKBLUE, 0);
    }

}
