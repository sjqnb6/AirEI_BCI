package Widget_;

import GUI.ColorPalette;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlP5;
import controlP5.Toggle;
import processing.core.PApplet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static Globel.GUI.nchan;
import static Globel.GUI.p5;
import static processing.core.PConstants.CENTER;
import static processing.core.PConstants.TOP;
import Globel.GUI;
// This is a helpful class that will add a channel select feature to a Widget
public class ChannelSelect {
    GUI MAIN;
    public PApplet applet;
    public ColorPalette CP;

    protected Widget widget;
    private List<controlP5.Controller> cp5ElementsToCheck = new ArrayList<controlP5.Controller>();
    protected int x, y, w, h, navH, butToggleY;
    public float tri_xpos = 0;
    protected float chanSelectXPos = 0;
    protected final int button_spacer = 10;
    public ControlP5 cp5_chanSelect;   //ControlP5 to contain our checkboxes
    protected List<Toggle> channelButtons;
    protected int offset;  //offset on nav bar of checkboxes
    protected int buttonW;
    protected int buttonH;
    protected boolean channelSelectHover;
    protected boolean isVisible;
    public List<Integer> activeChan;
    public String chanDropdownName;
    protected boolean isFirstRowChannelSelect = true;
    protected boolean isDualChannelSelect = false;

    private int labelWidth = 0;
    private int labelSpacer = 0;
    private String firstRowLabel = "Top";
    private String secondRowLabel = "Bot";

    public ChannelSelect(PApplet _parent, Widget _widget, int _x, int _y, int _w, int _navH, String checkBoxName) {
        applet = _parent;
        this.MAIN = (GUI) _parent;
        CP = new ColorPalette(_parent);

        widget = _widget;
        x = _x;
        y = _y;
        w = _w;
        h = _navH;
        navH = _navH;
        activeChan = new ArrayList<Integer>();
        chanDropdownName = checkBoxName;

        //setup for checkboxes
        cp5_chanSelect = new ControlP5(_parent);
        cp5_chanSelect.setGraphics(_parent, 0, 0);
        cp5_chanSelect.setAutoDraw(false); //draw only when specified
        createButtons(nchan);
    }

    public void update(int _x, int _y, int _w) {
        //update the x,y,w for this class using the parent class
        x = _x;
        y = _y;
        w = _w;
        //Toggle open/closed the channel menu
        if (applet.mouseX > (chanSelectXPos) && applet.mouseX < (tri_xpos + 10) && applet.mouseY < (y - navH*0.25) && applet.mouseY > (y - navH*0.65)) {
            channelSelectHover = true;
        } else {
            channelSelectHover = false;
        }
        //Update position of buttons on every update and check for UI overlap
        for (int i = 0; i < nchan; i++) {
            channelButtons.get(i).setPosition(x + labelWidth + labelSpacer + (button_spacer*(i+1)) + (buttonW*i), y + offset);
        }
    }

    public void draw() {
        chanSelectXPos = x + 2;
        applet.pushStyle();
        applet.noStroke();
        if (isFirstRowChannelSelect) {
            //change "Channels" text color and triangle color on hover
            if (channelSelectHover) {
                applet.fill(CP.OPENBCI_BLUE);
            } else {
                applet.fill(CP.OPENBCI_DARKBLUE);
            }
            applet.textFont(p5, 12);

            applet.text("Channels", chanSelectXPos, y - 6);
            tri_xpos = x + applet.textWidth("Channels") + 7;

            //draw triangle as pointing up or down, depending on if channel Select is active or closed
            if (!isVisible) {
                applet.triangle(tri_xpos, y - 7, tri_xpos + 6, y - 13, tri_xpos + 12, y - 7);
            } else {
                applet.triangle(tri_xpos, y - 13, tri_xpos + 6, y - 7, tri_xpos + 12, y - 13);
                //if active, draw a grey background for the channel select checkboxes
                applet.fill(200);
                applet.rect(x,y,w,navH);
            }
        } else { //This is the case in Spectrogram where we need a second channel selector
            //this draws extra grey space behind the checklist buttons
            if (isVisible) {
                applet.fill(200);
                applet.rect(x,y,w,navH);
            }
        }
        applet.popStyle();

        if (isVisible) {
            //Draw channel select buttons
            cp5_chanSelect.draw();
            //Draw a border around toggle buttons to indicate if channel is on or off
            applet.pushStyle();
            int weight = 1;
            applet.strokeWeight(weight);
            applet.noFill();
            for (int i = 0; i < nchan; i++) {
                int c = MAIN.currentBoard.isEXGChannelActive(i) ? applet.color(0,255,0,255) : applet.color(255,0,0,255);
                applet.stroke(c);
                applet.rect(x + labelWidth + labelSpacer + (button_spacer*(i+1)) + (buttonW*i) - weight, y + offset - weight, channelButtons.get(i).getWidth() + weight, channelButtons.get(i).getHeight() + weight);
            }
            applet.popStyle();
            //Draw label
            if (isDualChannelSelect) {
                applet.pushStyle();
                applet.fill(0);
                applet.textFont(p5, 12);
                applet.textAlign(CENTER, TOP);
                String label = isFirstRowChannelSelect ? firstRowLabel : secondRowLabel;
                applet.text(label, x + labelSpacer + labelWidth/2, y + offset);
                applet.popStyle();
            }
        }
    }

    public void screenResized(PApplet _parent) {
        cp5_chanSelect.setGraphics(_parent, 0, 0);
    }

    public void mousePressed(boolean dropdownIsActive) {
        if (!dropdownIsActive) {
            if (applet.mouseX > (chanSelectXPos) && applet.mouseX < (tri_xpos + 10) && applet.mouseY < (y - navH*0.25) && applet.mouseY > (y - navH*0.65)) {
                isVisible = !isVisible;
            }
        }
    }

    private void createButtons(int _nchan) {
        channelButtons = new ArrayList<Toggle>();

        int checkSize = navH - 6;
        offset = (navH - checkSize)/2;

        channelSelectHover = false;
        isVisible = false;

        buttonW = checkSize;
        buttonH = buttonW;

        for (int i = 0; i < _nchan; i++) {
            //start all items as invisible until user clicks dropdown to show checkboxes
            channelButtons.add(
                    createButton("ch"+(i+1), (i+1), true, x + (button_spacer*(i+2)) + (buttonW*i), y + offset, buttonW, buttonH)
            );
            cp5ElementsToCheck.add((controlP5.Controller)channelButtons.get(i));
        }
    }

    private Toggle createButton(String name, int chan, boolean _isVisible, int _x, int _y, int _w, int _h) {
        int _fontSize = 12;
        int marginLeftOffset = chan > 9 ? -9 : -6;
        Toggle myButton = cp5_chanSelect.addToggle(name)
                .setPosition(_x, _y)
                .setSize(_w, _h)
                .setColorLabel(CP.OPENBCI_DARKBLUE)
                .setColorForeground(applet.color(120))
                .setColorBackground(applet.color(150))
                .setColorActive(applet.color(57, 128, 204))
                .setVisible(_isVisible)
                ;
        myButton
                .getCaptionLabel()
                .setFont(applet.createFont("Arial", _fontSize, true))
                .toUpperCase(false)
                .setSize(_fontSize)
                .setText(String.valueOf(chan))
                .getStyle() //need to grab style before affecting margin and padding
                .setMargin(-_h - 3, 0, 0, marginLeftOffset)
                .setPaddingLeft(10)
        ;
        myButton.onPress(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                int chan = Integer.parseInt(((Toggle)theEvent.getController()).getCaptionLabel().getText()) - 1;
                boolean b = ((Toggle)theEvent.getController()).getBooleanValue();
                setToggleState(chan, b);
                //println(widget + " || " + activeChan);
            }
        });
        return myButton;
    }

    public void setIsFirstRowChannelSelect(boolean b) {
        isFirstRowChannelSelect = b;
    }

    public void setIsDualChannelSelect(boolean b) {
        isDualChannelSelect = b;
        if (isDualChannelSelect) {
            labelWidth = 28;
            labelSpacer = 4;
        }
    }

    public List<controlP5.Controller> getCp5ElementsForOverlapCheck() {
        return cp5ElementsToCheck;
    }

    public void setFirstRowLabel(String s) {
        firstRowLabel = s;
    }

    public void setSecondRowLabel(String s) {
        secondRowLabel = s;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setIsVisible(boolean b) {
        isVisible = b;
    }

    public void deactivateAllButtons() {
        for (int i = 0; i < nchan; i++) {
            channelButtons.get(i).setState(false);
        }
        activeChan.clear();
    }

    public void activateAllButtons() {
        for (int i = 0; i < nchan; i++) {
            channelButtons.get(i).setState(true);
            activeChan.add(i);
        }
        Collections.sort(activeChan);
    }

    public void setToggleState(Integer chan, boolean b) {
        channelButtons.get(chan).setState(b);
        if (b && !activeChan.contains(chan)) {
            activeChan.add(chan);
        } else if (!b && activeChan.contains(chan)) {
            activeChan.remove(chan);
        }
        Collections.sort(activeChan);
        //println(activeChan.toArray());
    }

    public int getHeight() {
        return h;
    }

} //end of ChannelSelect class