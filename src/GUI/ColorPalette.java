package GUI;

import processing.core.PApplet;

public class ColorPalette {

    public int WHITE;
    public int BLACK;
    public int OPENBCI_DARKBLUE;
    public int OPENBCI_BLUE;
    public int OPENBCI_BLUE_ALPHA50;
    public int OPENBCI_BLUE_ALPHA100;
    public int boxColor;
    public int boxStrokeColor;
    public int isSelected_color;
    public int colorNotPressed;
    public int buttonsLightBlue;
    public int GREY_235;
    public int GREY_200;
    public int GREY_125;
    public int GREY_100;
    public int GREY_20;
    public int TURN_ON_GREEN;
    public int TURN_OFF_RED;
    public int BOLD_RED;
    public int BUTTON_HOVER;
    public int BUTTON_HOVER_LIGHT;
    public int BUTTON_PRESSED;
    public int BUTTON_PRESSED_LIGHT;
    public int BUTTON_LOCKED_GREY;
    public int BUTTON_PRESSED_DARKGREY;
    public int BUTTON_NOOBGREEN;
    public int BUTTON_EXPERTPURPLE;
    public int BUTTON_CAUTIONRED;
    public int OBJECT_BORDER_GREY;
    public int TOPNAV_DARKBLUE;
    public int SUBNAV_LIGHTBLUE;
    public int ACCEL_X_COLOR;
    public int ACCEL_Y_COLOR;
    public int ACCEL_Z_COLOR;
    public int SIGNAL_CHECK_YELLOW;
    public int SIGNAL_CHECK_YELLOW_LOWALPHA;
    public int SIGNAL_CHECK_RED;
    public int SIGNAL_CHECK_RED_LOWALPHA;

    public final int[] channelColors;


    public int graphStroke;
    public int graphBG;
    public int textColor;
    public int strokeColor;
    public int eggshell;


    public final int[] lineColor;

    public ColorPalette(PApplet p) {
        WHITE = p.color(255);
        BLACK = p.color(0);
        OPENBCI_DARKBLUE = p.color(1, 18, 41);
        OPENBCI_BLUE = p.color(31, 69, 110);
        OPENBCI_BLUE_ALPHA50 = p.color(31, 69, 110, 50);
        OPENBCI_BLUE_ALPHA100 = p.color(31, 69, 110, 100);
        boxColor = p.color(200);
        boxStrokeColor = OPENBCI_DARKBLUE;
        isSelected_color = p.color(184, 220, 105);
        colorNotPressed = WHITE;
        buttonsLightBlue = p.color(57, 128, 204);
        GREY_235 = p.color(235);
        GREY_200 = p.color(200);
        GREY_125 = p.color(125);
        GREY_100 = p.color(100);
        GREY_20 = p.color(20);
        TURN_ON_GREEN = p.color(195, 242, 181);
        TURN_OFF_RED = p.color(255, 210, 210);
        BOLD_RED = p.color(224, 56, 45);
        BUTTON_HOVER = p.color(177, 184, 193);
        BUTTON_HOVER_LIGHT = p.color(211, 222, 232);
        BUTTON_PRESSED = p.color(150, 170, 200);
        BUTTON_PRESSED_LIGHT = p.color(179, 187, 199);
        BUTTON_LOCKED_GREY = p.color(128);
        BUTTON_PRESSED_DARKGREY = p.color(50);
        BUTTON_NOOBGREEN = p.color(114, 204, 171);
        BUTTON_EXPERTPURPLE = p.color(135, 95, 154);
        BUTTON_CAUTIONRED = p.color(214, 100, 100);
        OBJECT_BORDER_GREY = p.color(150);
        TOPNAV_DARKBLUE = OPENBCI_BLUE;
        SUBNAV_LIGHTBLUE = buttonsLightBlue;
        ACCEL_X_COLOR = BOLD_RED;
        ACCEL_Y_COLOR = p.color(49, 113, 89);
        ACCEL_Z_COLOR = p.color(54, 87, 158);
        SIGNAL_CHECK_YELLOW = p.color(221, 178, 13);
        SIGNAL_CHECK_YELLOW_LOWALPHA = p.color(221, 178, 13, 150);
        SIGNAL_CHECK_RED = BOLD_RED;
        SIGNAL_CHECK_RED_LOWALPHA = p.color(224, 56, 45, 150);

        channelColors = new int[]{
                p.color(129, 129, 129),
                p.color(124, 75, 141),
                p.color(54, 87, 158),
                p.color(49, 113, 89),
                SIGNAL_CHECK_YELLOW,
                p.color(253, 94, 52),
                BOLD_RED,
                p.color(162, 82, 49),
        };

        graphStroke = p.color(210);
        graphBG = p.color(245);
        textColor = OPENBCI_DARKBLUE;
        strokeColor = p.color(138, 146, 153);
        eggshell = p.color(255, 253, 248);


        lineColor = new int[]{
                p.color(129, 129, 129),
                p.color(124, 75, 141),
                p.color(54, 87, 158),
                p.color(49, 113, 89),
                SIGNAL_CHECK_YELLOW,
                p.color(253, 94, 52),
                BOLD_RED,
                p.color(162, 82, 49),
                p.color(129, 129, 129),
                p.color(124, 75, 141),
                p.color(54, 87, 158),
                p.color(49, 113, 89),
                SIGNAL_CHECK_YELLOW,
                p.color(253, 94, 52),
                BOLD_RED,
                p.color(162, 82, 49)
        };
    }
}
