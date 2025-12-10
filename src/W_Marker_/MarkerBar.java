package W_Marker_;

import DataSource_.DataSource;
import GUI.GUIManager;
import grafica.GPlot;
import grafica.GPointsArray;
import processing.core.PApplet;

import java.util.List;

import static GUI.GGVI.*;
import Globel.GUI;
//This class contains the time series plot for displaying the markers over time
class MarkerBar{
    GUI MAIN;
    //this class contains the plot for the 2d graph of marker data
    private int x, y, w, h;
    private int X_AXIS_PADDING = 22;
    private int Y_AXIS_PADDING = 30;
    private int xOffset;

    private GPlot plot; //the actual grafica-based GPlot that will be rendering the Time Series trace
    private GPointsArray markerPointsArray;
    private final String PLOT_LAYER = "layer1";

    private int nPoints;
    private int numSeconds;
    private int yAxisMax;
    private float timeBetweenPoints;
    private float[] markerTimeArray;
    private int numSamplesToProcess;

    private DataSource markerBoard;

    private boolean isAutoscale = false;
    private float autoscaleMin;
    private float autoscaleMax;
    private int previousMillis = 0;

    MarkerBar(GUI MAIN, int _yAxisMax, int markerWindow, float yLimit, int _x, int _y, int _w, int _h) {
//        super(_parent); //channel number, x/y location, height, width
        this.MAIN = MAIN;
        yAxisMax = _yAxisMax;
        numSeconds = markerWindow;

        // This widget is only instantiated when the board is accel capable, so we don't need to check
        markerBoard = (DataSource)currentBoard;

        x = _x;
        y = _y;
        w = _w;
        h = _h;
        if (eegDataSource == DATASOURCE_CYTON) {
            xOffset = 22;
        } else {
            xOffset = 0;
        }

        plot = new GPlot(MAIN);
        plot.setPos(x + 36 + 4 + xOffset, y); //match marker plot position with Time Series
        plot.setDim(w - 36 - 4 - xOffset, h);
        plot.setMar(0f, 0f, 0f, 0f);
        plot.setLineColor(MAIN.WHITE);
        plot.setXLim(-numSeconds, 0); //set the horizontal scale
        plot.setYLim((float) -0.2, (float) (yLimit + .2)); //change this to adjust vertical scale
        //plot.setPointSize(2);
        plot.setPointColor(0);
        plot.getXAxis().setAxisLabelText("Time (s)");
        plot.getYAxis().setAxisLabelText("Marker (int)");
        plot.getYAxis().setNTicks(5);
        plot.setAllFontProperties("Arial", 0, 14);
        plot.getXAxis().getAxisLabel().setOffset((float)(X_AXIS_PADDING));
        plot.getYAxis().getAxisLabel().setOffset((float)(Y_AXIS_PADDING));
        plot.getXAxis().setFontColor(MAIN.OPENBCI_DARKBLUE);
        plot.getXAxis().setLineColor(MAIN.OPENBCI_DARKBLUE);
        plot.getXAxis().getAxisLabel().setFontColor(MAIN.OPENBCI_DARKBLUE);
        plot.getYAxis().setFontColor(MAIN.OPENBCI_DARKBLUE);
        plot.getYAxis().setLineColor(MAIN.OPENBCI_DARKBLUE);
        plot.getYAxis().getAxisLabel().setFontColor(MAIN.OPENBCI_DARKBLUE);

        initArrays();


        plot.addLayer(PLOT_LAYER, markerPointsArray);
        plot.getLayer(PLOT_LAYER).setLineColor(MAIN.ACCEL_X_COLOR);

    }

    private void initArrays() {
        nPoints = nPointsBasedOnDataSource();
        timeBetweenPoints = (float)numSeconds / (float)nPoints;

        markerTimeArray = new float[nPoints];
        for (int i = 0; i < markerTimeArray.length; i++) {
            markerTimeArray[i] = -(float)numSeconds + (float)i * timeBetweenPoints;
        }

        float[] tempMarkerFloatArray = new float[nPoints];

        //make a GPoint array using float arrays x[] and y[] instead of plain index points
        markerPointsArray = new GPointsArray(markerTimeArray, tempMarkerFloatArray);
    }

    //Used to update the accelerometerBar class
    public void update() {
        updateGPlotPoints();

        if (isAutoscale) {
            adjustYAxis(-1);
        }
    }

    public void draw() {
        MAIN.pushStyle();
        plot.beginDraw();
        plot.drawBox(); //we won't draw this eventually ...
        plot.drawGridLines(GPlot.BOTH);
        plot.drawLines(); //Draw a Line graph!
        //plot.drawPoints(); //Used to draw Points instead of Lines
        plot.drawYAxis();
        plot.drawXAxis();
        plot.endDraw();
        MAIN.popStyle();
    }

    private int nPointsBasedOnDataSource() {
        return numSeconds * currentBoard.getSampleRate();
    }

    public void adjustTimeAxis(int _newTimeSize) {
        numSeconds = _newTimeSize;
        plot.setXLim(-numSeconds,0);

        initArrays();

        //Set the number of axis divisions...
        if (numSeconds > 1) {
            plot.getXAxis().setNTicks(numSeconds);
        } else {
            plot.getXAxis().setNTicks(10);
        }
    }

    public void adjustYAxis(int _yAxisMax) {
        if (_yAxisMax == -1) {
            yAxisMax = 1;
            isAutoscale = true;
            return;
        }
        isAutoscale = false;
        yAxisMax = _yAxisMax;
        plot.setYLim((float) -0.2, (float) (yAxisMax + .2));
    }

    void applyAutoscale() {
        //Do this once a second for all TimeSeries ChannelBars to save on resources
        int newMillis = MAIN.millis();
        boolean doAutoscale = newMillis > previousMillis + 1000;
        if (isAutoscale && currentBoard.isStreaming() && doAutoscale) {
            autoscaleMin = (int) Math.floor(autoscaleMin);
            autoscaleMax = (int) Math.ceil(autoscaleMax);
            previousMillis = newMillis;
            plot.setYLim(autoscaleMin, autoscaleMax); //<---- This is a very expensive method. Here is the bottleneck.
        }
    }

    //Used to update the Points within the graph
    private void updateGPlotPoints() {
        List<double[]> allData = markerBoard.getData(nPoints);
        int markerChannel = markerBoard.getMarkerChannel();

        autoscaleMax = -Float.MAX_VALUE;
        autoscaleMin = Float.MAX_VALUE;

        for (int i = 0; i < nPoints; i++) {
            markerPointsArray.set(i, markerTimeArray[i], (float)allData.get(i)[markerChannel], "");
            autoscaleMax = Math.max((float)allData.get(i)[markerChannel], autoscaleMax);
            autoscaleMin = Math.min((float)allData.get(i)[markerChannel], autoscaleMin);
        }
        applyAutoscale();
        plot.setPoints(markerPointsArray, PLOT_LAYER);
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
}; //end of class