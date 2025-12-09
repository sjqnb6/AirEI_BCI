package W_Accelerometer_;

import AccelerometerCapableBoard_.AccelerometerCapableBoard;
import GUI.ColorPalette;
import grafica.GPlot;
import grafica.GPointsArray;
import processing.core.PApplet;

import java.util.List;

import static GUI.GGVI.*;
import static processing.core.PApplet.max;
import static processing.core.PApplet.min;

//========================================================================================================================
//                     Accelerometer Graph Class -- Implemented by Accelerometer Widget Class
//========================================================================================================================
public class AccelerometerBar {

    protected PApplet pApplet;

    public ColorPalette CP;

    //this class contains the plot for the 2d graph of accelerometer data
    int x, y, w, h;
    int accBarPadding = 30;
    int xOffset;

    static GPlot plot; //the actual grafica-based GPlot that will be rendering the Time Series trace
    GPointsArray accelPointsX;
    GPointsArray accelPointsY;
    GPointsArray accelPointsZ;

    int nPoints;
    int numSeconds = 20; //default to 20 seconds
    float timeBetweenPoints;
    float[] accelTimeArray;
    int numSamplesToProcess;
    float minX, minY, minZ;
    float maxX, maxY, maxZ;
    float minVal;
    float maxVal;
    final float autoScaleSpacing = 0.1F;

    int channelColor; //color of plot trace

    static boolean isAutoscale; //when isAutoscale equals true, the y-axis will automatically update to scale to the largest visible amplitude
    int lastProcessedDataPacketInd = 0;

    private AccelerometerCapableBoard accelBoard;

    AccelerometerBar(PApplet _parent, float accelXyzLimit, int _x, int _y, int _w, int _h) { //channel number, x/y location, height, width
        CP = new ColorPalette(_parent);

        pApplet = _parent;
        // This widget is only instantiated when the board is accel capable, so we don't need to check
        accelBoard = (AccelerometerCapableBoard)currentBoard;

        x = _x;
        y = _y;
        w = _w;
        h = _h;
        if (eegDataSource == DATASOURCE_CYTON) {
            xOffset = 22;
        } else {
            xOffset = 0;
        }

        plot = new GPlot(_parent);
        plot.setPos(x + 36 + 4 + xOffset, y); //match Accelerometer plot position with Time Series
        plot.setDim(w - 36 - 4 - xOffset, h);
        plot.setMar(0f, 0f, 0f, 0f);
        plot.setLineColor((int)CP.channelColors[(NUM_ACCEL_DIMS)%8]);
        plot.setXLim(-numSeconds,0); //set the horizontal scale
        plot.setYLim(-accelXyzLimit, accelXyzLimit); //change this to adjust vertical scale
        //plot.setPointSize(2);
        plot.setPointColor(0);
        plot.getXAxis().setAxisLabelText("Time (s)");
        plot.getYAxis().setAxisLabelText("Acceleration (g)");
        plot.getYAxis().setNTicks(3);
        plot.setAllFontProperties("Arial", 0, 14);
        plot.getXAxis().getAxisLabel().setOffset((float)(accBarPadding));
        plot.getYAxis().getAxisLabel().setOffset((float)(accBarPadding));
        plot.getXAxis().setFontColor(CP.OPENBCI_DARKBLUE);
        plot.getXAxis().setLineColor(CP.OPENBCI_DARKBLUE);
        plot.getXAxis().getAxisLabel().setFontColor(CP.OPENBCI_DARKBLUE);
        plot.getYAxis().setFontColor(CP.OPENBCI_DARKBLUE);
        plot.getYAxis().setLineColor(CP.OPENBCI_DARKBLUE);
        plot.getYAxis().getAxisLabel().setFontColor(CP.OPENBCI_DARKBLUE);

        initArrays();

        //set the plot points for X, Y, and Z axes
        plot.addLayer("layer 1", accelPointsX);
        plot.getLayer("layer 1").setLineColor(CP.ACCEL_X_COLOR);
        plot.addLayer("layer 2", accelPointsY);
        plot.getLayer("layer 2").setLineColor(CP.ACCEL_Y_COLOR);
        plot.addLayer("layer 3", accelPointsZ);
        plot.getLayer("layer 3").setLineColor(CP.ACCEL_Z_COLOR);
    }

    void initArrays() {
        nPoints = nPointsBasedOnDataSource();
        timeBetweenPoints = (float)numSeconds / (float)nPoints;

        accelTimeArray = new float[nPoints];
        for (int i = 0; i < accelTimeArray.length; i++) {
            accelTimeArray[i] = -(float)numSeconds + (float)i * timeBetweenPoints;
        }

        float[] accelArrayX = new float[nPoints];
        float[] accelArrayY = new float[nPoints];
        float[] accelArrayZ = new float[nPoints];

        //make a GPoint array using float arrays x[] and y[] instead of plain index points
        accelPointsX = new GPointsArray(accelTimeArray, accelArrayX);
        accelPointsY = new GPointsArray(accelTimeArray, accelArrayY);
        accelPointsZ = new GPointsArray(accelTimeArray, accelArrayZ);
    }

    //Used to update the accelerometerBar class
    void update() {
        updateGPlotPoints();

        if (isAutoscale) {
            autoScale();
        }
    }

    void draw() {
        pApplet.pushStyle();
        plot.beginDraw();
        plot.drawBox(); //we won't draw this eventually ...
        plot.drawGridLines(GPlot.BOTH);
        plot.drawLines(); //Draw a Line graph!
        //plot.drawPoints(); //Used to draw Points instead of Lines
        plot.drawYAxis();
        plot.drawXAxis();
        plot.endDraw();
        pApplet.popStyle();
    }

    int nPointsBasedOnDataSource() {
        return numSeconds * currentBoard.getSampleRate();
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
    void updateGPlotPoints() {
        List<double[]> allData = accelBoard.getDataWithAccel(nPoints);
        int[] accelChannels = accelBoard.getAccelerometerChannels();

        for (int i = 0; i < nPoints; i++) {
            accelPointsX.set(i, accelTimeArray[i], (float)allData.get(i)[accelChannels[0]], "");
            accelPointsY.set(i, accelTimeArray[i], (float)allData.get(i)[accelChannels[1]], "");
            accelPointsZ.set(i, accelTimeArray[i], (float)allData.get(i)[accelChannels[2]], "");
        }

        plot.setPoints(accelPointsX, "layer 1");
        plot.setPoints(accelPointsY, "layer 2");
        plot.setPoints(accelPointsZ, "layer 3");
    }

    float[] getLastAccelVals() {
        float[] result = new float[NUM_ACCEL_DIMS];
        result[0] = accelPointsX.getY(nPoints-1);
        result[1] = accelPointsY.getY(nPoints-1);
        result[2] = accelPointsZ.getY(nPoints-1);

        return result;
    }

    public static void adjustVertScale(int _vertScaleValue) {
        if (_vertScaleValue == 0) {
            isAutoscale = true;
        } else {
            isAutoscale = false;
            plot.setYLim(-_vertScaleValue, _vertScaleValue);
        }
    }

    void autoScale() {
        float[] minMaxVals = minMax(accelPointsX, accelPointsY, accelPointsZ);
        plot.setYLim(minMaxVals[0] - autoScaleSpacing, minMaxVals[1] + autoScaleSpacing);
    }

    float[] minMax(GPointsArray arrX, GPointsArray arrY, GPointsArray arrZ) {
        float[] minMaxVals = {0.f, 0.f};
        for (int i = 0; i < arrX.getNPoints(); i++) { //go through the XYZ GPpointArrays for on-screen values
            float[] vals = {arrX.getY(i), arrY.getY(i), arrZ.getY(i)};
            minMaxVals[0] = min(minMaxVals[0], min(vals)); //make room to see
            minMaxVals[1] = max(minMaxVals[1], max(vals));
        }
        return minMaxVals;
    }

    void screenResized(int _x, int _y, int _w, int _h) {
        x = _x;
        y = _y;
        w = _w+100;
        h = _h;
        //reposition & resize the plot
        plot.setPos(x + 36 + 4 + xOffset, y);
        plot.setDim(w - 36 - 4 - xOffset, h);

    }
}; //end of class