package W_HeadPlot_;

import Widget_.Widget;
import processing.core.PApplet;

import java.util.Arrays;

import static Globel.GUI.*;

import Globel.GUI;
public class W_HeadPlot extends Widget {
    public HeadPlot headPlot;
    GUI MAIN;
    public W_HeadPlot(GUI MAIN){
        super(MAIN); //calls the parent CONSTRUCTOR method of Widget (DON'T REMOVE)
        this.MAIN = MAIN;
        //Headplot settings
        MAIN.settings.hpIntensitySave = 2;
        MAIN.settings.hpPolaritySave = 0;
        MAIN.settings.hpContoursSave = 0;
        MAIN.settings.hpSmoothingSave = 3;
        //This is the protocol for setting up dropdowns.
        //Note that these 3 dropdowns correspond to the 3 global functions below
        //You just need to make sure the "id" (the 1st String) has the same name as the corresponding function
        // addDropdown("Ten20", "Layout", Arrays.asList("10-20", "5-10"), 0);
        // addDropdown("Headset", "Headset", Arrays.asList("None", "Mark II", "Mark III", "Mark IV "), 0);
        addDropdown("Intensity", "Intensity", Arrays.asList("4x", "2x", "1x", "0.5x", "0.2x", "0.02x"), MAIN.vertScaleFactor_ind);
        addDropdown("Polarity", "Polarity", Arrays.asList("+/-", " + "), MAIN.settings.hpPolaritySave);
        addDropdown("ShowContours", "Contours", Arrays.asList("ON", "OFF"), MAIN.settings.hpContoursSave);
        addDropdown("SmoothingHeadPlot", "Smooth", Arrays.asList("0.0", "0.5", "0.75", "0.9", "0.95", "0.98"), MAIN.smoothFac_ind);
        //Initialize the headplot
        updateHeadPlot(nchan);
    }

    void updateHeadPlot(int _nchan) {
        headPlot = new HeadPlot(pApplet, x, y, w, h, win_w, win_h);
        //FROM old Gui_Manager
        headPlot.setIntensityData_byRef(dataProcessing.data_std_uV, is_railed);
        headPlot.setPolarityData_byRef(dataProcessing.polarity);
        setSmoothFac(MAIN.smoothFac[MAIN.smoothFac_ind]);
    }

    public void update(){
        super.update(); //calls the parent update() method of Widget (DON'T REMOVE)
        headPlot.update();
    }

    public void draw(){
        super.draw(); //calls the parent draw() method of Widget (DON'T REMOVE)
        headPlot.draw(); //draw the actual headplot
    }

    public void screenResized(){
        super.screenResized(); //calls the parent screenResized() method of Widget (DON'T REMOVE)
        headPlot.hp_x = x;
        headPlot.hp_y = y;
        headPlot.hp_w = w;
        headPlot.hp_h = h;
        headPlot.hp_win_x = x;
        headPlot.hp_win_y = y;

        pApplet.thread("doHardCalcs");
    }

    public void mousePressed(){
        super.mousePressed(); //calls the parent mousePressed() method of Widget (DON'T REMOVE)
        headPlot.mousePressed();
    }

    public void mouseReleased(){
        super.mouseReleased(); //calls the parent mouseReleased() method of Widget (DON'T REMOVE)
        headPlot.mouseReleased();
    }

    public void mouseDragged(){
        super.mouseDragged(); //calls the parent mouseReleased() method of Widget (DON'T REMOVE)
        headPlot.mouseDragged();
    }

    //add custom class functions here
    void setSmoothFac(float fac) {
        headPlot.smooth_fac = fac;
    }
};