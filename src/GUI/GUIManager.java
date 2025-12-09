package GUI;

import controlP5.Button;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlP5;
import processing.core.PApplet;
import processing.core.PFont;

import static GUI.GGVI.buttonHelpText;
import static GUI.GGVI.p5;

public class GUIManager extends PApplet {
    protected PApplet pApplet;
    //Starting to collect the GUI-wide color pallet here. Rename constants all caps later...
    public final int WHITE = color(255);
    public final int BLACK = color(0);
    public final int OPENBCI_DARKBLUE = color(1, 18, 41);
    public final int OPENBCI_BLUE = color(31, 69, 110);
    public final int OPENBCI_BLUE_ALPHA50 = color(31, 69, 110, 50);
    public final int OPENBCI_BLUE_ALPHA100 = color(31, 69, 110, 100);
    public final int boxColor = color(200);
    public final int boxStrokeColor = OPENBCI_DARKBLUE;
    public final int isSelected_color = color(184, 220, 105); //Used for textfield borders,
    public final int colorNotPressed = WHITE;
    public final int buttonsLightBlue = color(57,128,204);
    public final int GREY_235 = color(235);
    public final int GREY_200 = color(200);
    public final int GREY_125 = color(125);
    public final int GREY_100 = color(100);
    public final int GREY_20 = color(20);
    public final int TURN_ON_GREEN = color(195, 242, 181);
    public final int TURN_OFF_RED = color(255, 210, 210);
    public final int BOLD_RED = color(224, 56, 45);
    public final int BUTTON_HOVER = color(177, 184, 193);//color(252, 221, 198);
    public final int BUTTON_HOVER_LIGHT = color(211, 222, 232);
    public final int BUTTON_PRESSED = color(150, 170, 200); //OPENBCI_DARKBLUE;
    public final int BUTTON_PRESSED_LIGHT = color(179, 187, 199);
    public final int BUTTON_LOCKED_GREY = color(128);
    public final int BUTTON_PRESSED_DARKGREY = color(50);
    public final int BUTTON_NOOBGREEN = color(114,204,171);
    public final int BUTTON_EXPERTPURPLE = color(135,95,154);
    public final int BUTTON_CAUTIONRED = color(214,100,100);
    public final int OBJECT_BORDER_GREY = color(150);
    public final int TOPNAV_DARKBLUE = OPENBCI_BLUE;
    public final int SUBNAV_LIGHTBLUE = buttonsLightBlue;
    //Use the same colors for X,Y,Z throughout Accelerometer widget
    public final int ACCEL_X_COLOR = BOLD_RED;
    public final int ACCEL_Y_COLOR = color(49, 113, 89);
    public final int ACCEL_Z_COLOR = color(54, 87, 158);
    //Signal check colors
    public final int SIGNAL_CHECK_YELLOW = color(221, 178, 13); //Same color as yellow channel color found below
    public final int SIGNAL_CHECK_YELLOW_LOWALPHA = color(221, 178, 13, 150);
    public final int SIGNAL_CHECK_RED = BOLD_RED;
    public final int SIGNAL_CHECK_RED_LOWALPHA = color(224, 56, 45, 150);


    public final int[] channelColors = new int[]{
                color(129, 129, 129),
                color(124, 75, 141),
                color(54, 87, 158),
                color(49, 113, 89),
                SIGNAL_CHECK_YELLOW,
                color(253, 94, 52),
                BOLD_RED,
                color(162, 82, 49),
    };

    public final int graphStroke = color(210);
    public final int graphBG = color(245);
    public final int textColor = OPENBCI_DARKBLUE;
    public final int strokeColor = color(138, 146, 153);
    public final int eggshell = color(255, 253, 248);


    public final int[] lineColor = new int[]{
        color(129, 129, 129),
                color(124, 75, 141),
                color(54, 87, 158),
                color(49, 113, 89),
                SIGNAL_CHECK_YELLOW,
                color(253, 94, 52),
                BOLD_RED,
                color(162, 82, 49),
                color(129, 129, 129),
                color(124, 75, 141),
                color(54, 87, 158),
                color(49, 113, 89),
                SIGNAL_CHECK_YELLOW,
                color(253, 94, 52),
                BOLD_RED,
                color(162, 82, 49)
    };


    public boolean emgSettingsPopupIsOpen = false;

    public static float[] smoothFac = new float[]{0.0F, 0.5F, 0.75F, 0.9F, 0.95F, 0.98F, 0.99F, 0.999F}; //used by FFT & Headplot
    public static int smoothFac_ind = 3;    //initial index into the smoothFac array = 0.75 to start .. used by FFT & Head Plots

    // ----- these variable/methods are used for adjusting the intensity factor of the headplot opacity ---------------------------------------------------------------------------------------------------------
    public static float default_vertScale_uV = 200.0F; //this defines the Y-scale on the montage plots...this is the vertical space between traces
    public static float[] vertScaleFactor = { 0.25f, 0.5f, 1.0f, 2.0f, 5.0f, 50.0f};
    public static int vertScaleFactor_ind = 2;
    public static float vertScale_uV = default_vertScale_uV;

    public boolean filterUIPopupIsOpen = false;

    public static boolean filterSettingsWereLoadedFromFile = false;

    // public GUIManager(PApplet pApplet){
    //     this.pApplet = pApplet;
    // }

//    public static void main(String mainClass) {
//        PApplet.main(mainClass);
//    }

//    public void Widget(PApplet _parent){
//        pApplet = _parent;
//    }


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
    public Button createButton(ControlP5 _cp5, String name, String text, int _x, int _y, int _w, int _h) {
        return createButton(_cp5, name, text, _x, _y, _w, _h, 0, p5, 12, colorNotPressed, OPENBCI_DARKBLUE, BUTTON_HOVER, BUTTON_PRESSED, OPENBCI_DARKBLUE, 0);
    }

}
