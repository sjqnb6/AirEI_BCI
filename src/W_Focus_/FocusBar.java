package W_Focus_;

import GUI.GUIManager;
import grafica.GPlot;
import grafica.GPointsArray;
import processing.core.PApplet;

import java.util.LinkedList;

import static Globel.GUI.*;

import Globel.GUI;
//This class contains the time series plot for the focus metric over time
import Globel.GUI;
public class FocusBar{
    GUI MAIN;
    int x, y, w, h;
    int focusBarPadding = 30;
    int xOffset;
    final int nPoints = 30 * 1000;

    GPlot plot; //the actual grafica-based GPlot that will be rendering the Time Series trace
    LinkedList<Float> fifoList;
    LinkedList<Float> fifoTimeList;

    int numSeconds;
    int channelColor; //color of plot trace

    FocusBar(GUI MAIN, int xLimit, float yLimit, int _x, int _y, int _w, int _h) {
//        super(_parent); //channel number, x/y location, height, width
        this.MAIN = MAIN;
        x = _x;
        y = _y;
        w = _w;
        h = _h;
        if (eegDataSource == DATASOURCE_CYTON) {
            xOffset = 22;
        } else {
            xOffset = 0;
        }
        numSeconds = xLimit;

        plot = new GPlot(MAIN);
        plot.setPos(x + 36 + 4 + xOffset, y); //match Accelerometer plot position with Time Series
        plot.setDim(w - 36 - 4 - xOffset, h);
        plot.setMar(0f, 0f, 0f, 0f);
        plot.setLineColor((int)MAIN.channelColors[(NUM_ACCEL_DIMS)%8]);
        plot.setXLim(-numSeconds,0); //set the horizontal scale
        plot.setYLim(0, yLimit); //change this to adjust vertical scale
        //plot.setPointSize(2);
        plot.setPointColor(0);
        plot.getXAxis().setAxisLabelText("时间 (s)");
        plot.getYAxis().setAxisLabelText("专注度指标");
        plot.setAllFontProperties("Microsoft YaHei", 0, 14);
        plot.getXAxis().getAxisLabel().setOffset((float)(22));
        plot.getYAxis().getAxisLabel().setOffset((float)(focusBarPadding));
        plot.getXAxis().setFontColor(MAIN.OPENBCI_DARKBLUE);
        plot.getXAxis().setLineColor(MAIN.OPENBCI_DARKBLUE);
        plot.getXAxis().getAxisLabel().setFontColor(MAIN.OPENBCI_DARKBLUE);
        plot.getYAxis().setFontColor(MAIN.OPENBCI_DARKBLUE);
        plot.getYAxis().setLineColor(MAIN.OPENBCI_DARKBLUE);
        plot.getYAxis().getAxisLabel().setFontColor(MAIN.OPENBCI_DARKBLUE);

        adjustTimeAxis(numSeconds);

        initArrays();

        //set the plot points for X, Y, and Z axes
        plot.addLayer("layer 1", new GPointsArray(30));
        plot.getLayer("layer 1").setLineColor(MAIN.ACCEL_X_COLOR);
    }

    private void initArrays() {
        fifoList = new LinkedList<Float>();
        fifoTimeList = new LinkedList<Float>();
        for (int i = 0; i < nPoints; i++) {
            fifoList.add(0f);
            fifoTimeList.add(0f);
        }
    }

    public void update(double val) {
        updateGPlotPoints(val);
    }

    public void draw() {
        plot.beginDraw();
        plot.drawBox(); //we won't draw this eventually ...
        plot.drawGridLines(GPlot.BOTH);
        plot.drawLines(); //Draw a Line graph!
        //plot.drawPoints(); //Used to draw Points instead of Lines
        plot.drawYAxis();
        plot.drawXAxis();
        plot.getXAxis().draw();
        plot.endDraw();
    }

    public void adjustTimeAxis(int _newTimeSize) {
        numSeconds = _newTimeSize;
        plot.setXLim(-_newTimeSize,0);
        initArrays();
        //Set the number of axis divisions...
        if (_newTimeSize > 1) {
            plot.getXAxis().setNTicks(_newTimeSize);
        }else{
            plot.getXAxis().setNTicks(10);
        }
    }

    //Used to update the Points within the graph
    private void updateGPlotPoints(double val) {
        float timerVal = (float) (MAIN.millis() / 1000.0);
        fifoTimeList.removeFirst();
        fifoTimeList.addLast(timerVal);
        fifoList.removeFirst();
        fifoList.addLast((float)val);

        int stopId = 0;
        for (stopId = nPoints - 1; stopId > 0; stopId--) {
            if (timerVal - fifoTimeList.get(stopId) > numSeconds) {
                break;
            }
        }
        int size = nPoints - 1 - stopId;
        GPointsArray focusPoints = new GPointsArray(size);
        for (int i = 0; i < size; i++) {
            focusPoints.set(i, fifoTimeList.get(i + stopId) - timerVal, fifoList.get(i + stopId), "");
        }
        plot.setPoints(focusPoints, "layer 1");
    }

    public void screenResized(int _x, int _y, int _w, int _h) {
        x = _x;
        y = _y;
        w = _w;
        h = _h;
        //reposition & resize the plot
        plot.setPos(x + 36 + 4 + xOffset, y);
        plot.setDim(w - 36 - 4 - xOffset, h);

    }

    public void setPlotPosAndOuterDim(boolean chanSelectIsVisible) {
        int _y = chanSelectIsVisible ? y + 22 : y;
        int _h = chanSelectIsVisible ? h - 22 : h;
        //reposition & resize the plot
        plot.setPos(x + 36 + 4 + xOffset, _y);
        plot.setDim(w - 36 - 4 - xOffset, _h);
    }

}; //end of class