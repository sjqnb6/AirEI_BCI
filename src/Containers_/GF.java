package Containers_;

import GUI.GUIManager;
import Globel.GUI;
import processing.core.PApplet;

import static Containers_.GVI.*;
import static GUI.GGVI.settings;

public class GF {

    public static void setupContainers(PApplet _parent) {
        int width = _parent.width;
        int height = _parent.height;

        widthOfLastScreen_C = _parent.width;
        heightOfLastScreen_C = _parent.height;

        GVI.container[0] = new Container(0, 0, width, topNav_h, 0);
        GVI.container[5] = new Container(0, topNav_h, width, height - (topNav_h + bottomNav_h), 1);
        GVI.container[1] = new Container(GVI.container[5], "TOP_LEFT");
        GVI.container[2] = new Container(GVI.container[5], "TOP");
        GVI.container[3] = new Container(GVI.container[5], "TOP_RIGHT");
        GVI.container[4] = new Container(GVI.container[5], "LEFT");
        GVI.container[6] = new Container(GVI.container[5], "RIGHT");
        GVI.container[7] = new Container(GVI.container[5], "BOTTOM_LEFT");
        GVI.container[8] = new Container(GVI.container[5], "BOTTOM");
        GVI.container[9] = new Container(GVI.container[5], "BOTTOM_RIGHT");
//        GVI.container[10] = new Container(_parent,0, height - bottomNav_h, width, 50, 0);
        GVI.container[11] = new Container(GVI.container[3], "TOP");
        GVI.container[12] = new Container(GVI.container[3], "BOTTOM");
        GVI.container[13] = new Container(GVI.container[9], "TOP");
        GVI.container[14] = new Container(GVI.container[9], "BOTTOM");
        GVI.container[15] = new Container(GVI.container[6], "TOP_LEFT");
        GVI.container[16] = new Container(GVI.container[6], "TOP_RIGHT");
        GVI.container[17] = new Container(GVI.container[6], "BOTTOM_LEFT");
        GVI.container[18] = new Container(GVI.container[6], "BOTTOM_RIGHT");

        //setup viz objects... example of container extension (more below)
        //setupVizs();
    }

    public static void drawContainers(GUI MAIN) {
        for(int i = 0; i < GVI.container.length; i++){
            GVI.container[i].draw(MAIN);
        }

        //Draw viz objects.. example extension of container class (more below)
        //viz1.draw();
        //viz2.draw();

        //alternative component listener function (line 177 - 187 frame.addComponentListener) for processing 3,
        if (widthOfLastScreen_C != MAIN.width || heightOfLastScreen_C != MAIN.height) {
            setupContainers(MAIN);
            //setupVizs(); //container extension example (more below)
            MAIN.settings.widthOfLastScreen = MAIN.width;
            settings.heightOfLastScreen = MAIN.height;
        }
    }

}
