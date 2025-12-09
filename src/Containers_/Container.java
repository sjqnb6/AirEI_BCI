package Containers_;

import Globel.GUI;
import processing.core.PApplet;

import static Containers_.GVI.drawContainers;

public class Container{

    //key Container Variables
    public float x0, y0, w0, h0; //true dimensions.. without margins
    public float x, y, w, h; //dimensions with margins
    public float margin; //margin

    //constructor 1 -- comprehensive
    public Container(float _x0, float _y0, float _w0, float _h0, float _margin) {

        margin = _margin;

        x0 = _x0;
        y0 = _y0;
        w0 = _w0;
        h0 = _h0;

        x = x0 + margin;
        y = y0 + margin;
        w = w0 - margin*2;
        h = h0 - margin*2;
    }

    //constructor 2 -- recursive constructor -- for quickly building sub-containers based on a super container (aka master)
    public Container(Container master, String _type) {

        margin = master.margin;

        if(_type == "WHOLE"){
            x0 = master.x0;
            y0 = master.y0;
            w0 = master.w0;
            h0 = master.h0;
            w = master.w;
            h = master.h;
            x = master.x;
            y = master.y;
        } else if (_type == "LEFT") {
            x0 = master.x0;
            y0 = master.y0;
            w0 = master.w0/2;
            h0 = master.h0;
            w = (master.w - margin)/2;
            h = master.h;
            x = master.x;
            y = master.y;
        } else if (_type == "RIGHT") {
            x0 = master.x0 + master.w0/2;
            y0 = master.y0;
            w0 = master.w0/2;
            h0 = master.h0;
            w = (master.w - margin)/2;
            h = master.h;
            x = master.x + w + margin;
            y = master.y;
        } else if (_type == "TOP") {
            x0 = master.x0;
            y0 = master.y0;
            w0 = master.w0;
            h0 = master.h0/2;
            w = master.w;
            h = (master.h - margin)/2;
            x = master.x;
            y = master.y;
        } else if (_type == "BOTTOM") {
            x0 = master.x0;
            y0 = master.y0 + master.h0/2;
            w0 = master.w0;
            h0 = master.h0/2;
            w = master.w;
            h = (master.h - margin)/2;
            x = master.x;
            y = master.y + h + margin;
        } else if (_type == "TOP_LEFT") {
            x0 = master.x0;
            y0 = master.y0;
            w0 = master.w0/2;
            h0 = master.h0/2;
            w = (master.w - margin)/2;
            h = (master.h - margin)/2;
            x = master.x;
            y = master.y;
        } else if (_type == "TOP_RIGHT") {
            x0 = master.x0 + master.w0/2;
            y0 = master.y0;
            w0 = master.w0/2;
            h0 = master.h0/2;
            w = (master.w - margin)/2;
            h = (master.h - margin)/2;
            x = master.x + w + margin;
            y = master.y;
        } else if (_type == "BOTTOM_LEFT") {
            x0 = master.x0;
            y0 = master.y0 + master.h0/2;
            w0 = master.w0/2;
            h0 = master.h0/2;
            w = (master.w - margin)/2;
            h = (master.h - margin)/2;
            x = master.x;
            y = master.y + h + margin;
        } else if (_type == "BOTTOM_RIGHT") {
            x0 = master.x0 + master.w0/2;
            y0 = master.y0 + master.h0/2;
            w0 = master.w0/2;
            h0 = master.h0/2;
            w = (master.w - margin)/2;
            h = (master.h - margin)/2;
            x = master.x + w + margin;
            y = master.y + h + margin;
        }
    }

    public void draw(GUI MAIN) {
        if(drawContainers){
            MAIN.pushStyle();

            //draw margin area
            MAIN.fill(102, 255, 71, 100);
            MAIN.noStroke();
            MAIN.rect(x0, y0, w0, h0);

            //noFill();
            //stroke(255, 0, 0);
            //rect(x0, y0, w0, h0);

//            fill(OPENBCI_BLUE_ALPHA100);
            MAIN.noStroke();
            MAIN.rect(x, y, w, h);

            MAIN.popStyle();
        }
    }
};
