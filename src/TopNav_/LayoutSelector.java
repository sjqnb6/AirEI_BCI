package TopNav_;

import GUI.GUIManager;
import controlP5.Button;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlP5;
import processing.core.PImage;

import java.util.ArrayList;

import static Debugging_.GF.output;
import static GUI.GGVI.*;
import Globel.GUI;
public class LayoutSelector{

    public int x, y, w, h, margin, b_w, b_h;
    public boolean isVisible;
    private ControlP5 layout_cp5;
    public ArrayList<Button> layoutOptions;

    GUI MAIN;

    public LayoutSelector(GUI MAIN) {
        this.MAIN = MAIN;


        w = 180;
        x = MAIN.width - w - 3;
        y = (navBarHeight * 2) - 3;
        margin = 6;
        b_w = (w - 5*margin)/4;
        b_h = b_w;
        h = margin*4 + b_h*3;

        isVisible = false;

        //Instantiate local cp5 for this box
        layout_cp5 = new ControlP5(MAIN);
        layout_cp5.setGraphics(MAIN, 0,0);
        layout_cp5.setAutoDraw(false);

        layoutOptions = new ArrayList<Button>();
        addLayoutOptionButtons();
    }

    public void update() {
        if (isVisible) { //only update if visible
            // //close dropdown when mouse leaves
            // if ((mouseX < x || mouseX > x + w || mouseY < y || mouseY > y + h) && !topNav.layoutButton.isMouseHere()){
            //   toggleVisibility();
            // }
        }

        //Update the X position of this box on every update
        x = MAIN.width - w - 3;
    }

    public void draw() {
        if (isVisible) { //only draw if visible
            MAIN.pushStyle();

            MAIN.stroke(MAIN.OPENBCI_DARKBLUE);
            // fill(229); //bg
            MAIN.fill(57, 128, 204); //bg
            MAIN.rect(x, y, w, h);

            MAIN.fill(57, 128, 204);
            // fill(177, 184, 193);
            MAIN.noStroke();
            MAIN.rect(x+w-(topNav.layoutButton.getWidth()-1), y, (topNav.layoutButton.getWidth()-1), 1);

            MAIN.popStyle();

            layout_cp5.draw();
        }
    }

    public void isMouseHere() {
    }

    public void mousePressed() {
    }

    public void mouseReleased() {
        //only allow button interactivity if isVisible==true
        if (isVisible) {
            if ((MAIN.mouseX < x || MAIN.mouseX > x + w || MAIN.mouseY < y || MAIN.mouseY > y + h) && !topNav.layoutButton.isInside()) {
                toggleVisibility();
            }

        }
    }

    void screenResized() {
        //update position of outer box and buttons
        //int oldX = x;
        x = MAIN.width - w - 3;
        //int dx = oldX - x;
        layout_cp5.setGraphics(MAIN, 0,0);

        for (int i = 0; i < layoutOptions.size(); i++) {
            int row = (i/4)%4;
            int column = i%4;
            layoutOptions.get(i).setPosition(x + (column+1)*margin + (b_w*column), y + (row+1)*margin + row*b_h);
        }
    }

    void toggleVisibility() {
        isVisible = !isVisible;
        if (isVisible) {
            //the very convoluted way of locking all controllers of a single controlP5 instance...
            for (int i = 0; i < wm.widgets.size(); i++) {
                for (int j = 0; j < wm.widgets.get(i).cp5_widget.getAll().size(); j++) {
                    wm.widgets.get(i).cp5_widget.getController(wm.widgets.get(i).cp5_widget.getAll().get(j).getAddress()).lock();
                }
            }
        } else {
            //the very convoluted way of unlocking all controllers of a single controlP5 instance...
            for (int i = 0; i < wm.widgets.size(); i++) {
                for (int j = 0; j < wm.widgets.get(i).cp5_widget.getAll().size(); j++) {
                    wm.widgets.get(i).cp5_widget.getController(wm.widgets.get(i).cp5_widget.getAll().get(j).getAddress()).unlock();
                }
            }
        }
    }

    private void addLayoutOptionButtons() {
        final int numLayouts = 12;
        for (int i = 0; i < numLayouts; i++) {
            int row = (i/4)%4;
            int column = i%4;
            final int layoutNumber = i;
            Button tempLayoutButton = MAIN.createButton(layout_cp5, "layoutButton"+i, "", x + (column+1)*margin + (b_w*column), y + (row+1)*margin + (row*b_h), b_w, b_h);
            PImage tempBackgroundImage = MAIN.loadImage("layout_buttons/layout_"+(i+1)+".png");
            tempBackgroundImage.resize(b_w, b_h);
            tempLayoutButton.setImage(tempBackgroundImage);
            tempLayoutButton.setForceDrawBackground(true);
            tempLayoutButton.onRelease(new CallbackListener() {
                public void controlEvent(CallbackEvent theEvent) {
                    output("Layout [" + (layoutNumber) + "] selected.");
                    toggleVisibility(); //shut layoutSelector if something is selected
                    wm.setNewContainerLayout(layoutNumber); //have WidgetManager update Layout and active widgets
                    settings.currentLayout = layoutNumber; //copy this value to be used when saving Layout setting
                }
            });
            layoutOptions.add(tempLayoutButton);
        }
    }
}
