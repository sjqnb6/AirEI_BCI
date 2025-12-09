package Extras_;

import Widget_.Widget;
import processing.core.PApplet;
import processing.core.PFont;

import static GUI.GGVI.p5;

public class TextBox extends Widget {
    public int x;
    public int y;
    private int w, h;
    private int textColor;
    private int backgroundColor;
    private PFont font;
    private int fontSize;
    public String string;
    public boolean drawBackground = true;
    private int backgroundEdge_pixels;
    public int alignH;
    public int alignV;
    private boolean drawObject = true;

    public TextBox(PApplet _parent, String s, int x1, int y1) {
        super(_parent);
        string = s; x = x1; y = y1;
        textColor = OPENBCI_DARKBLUE;
        backgroundColor = pApplet.color(255);
        fontSize = 12;
        font = p5;
        backgroundEdge_pixels = 1;
        drawBackground = false;
        alignH = LEFT;
        alignV = BOTTOM;
    }

    public TextBox(PApplet _parent, String s, int x1, int y1, int _textColor, int _backgroundColor, int _alignH, int _alignV) {
        this(_parent, s, x1, y1);
        textColor = _textColor;
        backgroundColor = _backgroundColor;
        drawBackground = true;
        alignH = _alignH;
        alignV = _alignV;
    }

    public TextBox(PApplet _parent, String s, int x1, int y1, int _textColor, int _backgroundColor, int _fontSize, PFont _font, int _alignH, int _alignV) {
        this(_parent, s, x1, y1, _textColor, _backgroundColor, _alignH, _alignV);
        fontSize = _fontSize;
        font = _font;
    }

    public void draw() {

        if (!drawObject) {
            return;
        }

        pApplet.pushStyle();
        pApplet.noStroke();
        pApplet.textFont(font);

        //draw the box behind the text
        if (drawBackground == true) {
            w = (int)(round(pApplet.textWidth(string)));
            int xbox = x - backgroundEdge_pixels;
            switch (alignH) {
                case LEFT:
                    xbox = x - backgroundEdge_pixels;
                    break;
                case RIGHT:
                    xbox = x - w - backgroundEdge_pixels;
                    break;
                case CENTER:
                    xbox = x - (int)(round((float) (w/2.0))) - backgroundEdge_pixels;
                    break;
            }
            w = w + 2*backgroundEdge_pixels;

            h = (int)(pApplet.textAscent()) + backgroundEdge_pixels*2;
            int ybox = y;
            if (alignV == CENTER) {
                ybox -= (int) (pApplet.textAscent() / 2 - backgroundEdge_pixels);
            } else if (alignV == BOTTOM) {
                ybox -= (int) (pApplet.textAscent() + backgroundEdge_pixels*3);
            }
            pApplet.fill(backgroundColor);
            pApplet.rect(xbox,ybox,w,h);
        }
        pApplet.popStyle();

        //draw the text itself
        pApplet.pushStyle();
        pApplet.noStroke();
        pApplet.fill(textColor);
        pApplet.textAlign(alignH,alignV);
        pApplet.textFont(font);
        pApplet.text(string,x,y);
        pApplet.strokeWeight(1);
        pApplet.popStyle();
    }

    public void setPosition(int _x, int _y) {
        x = _x;
        y = _y;
    }

    public void setText(String s) {
        string = s;
    }

    public void setTextColor(int c) {
        textColor = c;
    }

    public void setBackgroundColor(int c) {
        backgroundColor = c;
    }

    public int getWidth() {
        return w;
    }

    public int getHeight() {
        return h;
    }

    public void setVisible(boolean b) {
        drawObject = b;
    }
};