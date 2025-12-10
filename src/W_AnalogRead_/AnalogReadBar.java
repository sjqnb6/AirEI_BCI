package W_AnalogRead_;

import AnalogCapableBoard_.AnalogCapableBoard;
import Extras_.TextBox;
import Widget_.Widget;
import grafica.GPlot;
import grafica.GPointsArray;
import processing.core.PApplet;

import java.util.List;
import static GUI.GGVI.*;
import static GUI.GGVI.currentBoard;
import static GUI.GGVI.selectedProtocol;
import Globel.GUI;
//========================================================================================================================
//                      Analog Voltage BAR CLASS -- Implemented by Analog Read Widget Class
//========================================================================================================================
//this class contains the plot and buttons for a single channel of the Time Series widget
//one of these will be created for each channel (4, 8, or 16)
public class AnalogReadBar extends Widget {
    GUI MAIN;
    private int analogInputPin;
    private int auxValuesPosition;
    private String analogInputString;
    private int x, y, w, h;

    private GPlot plot; //the actual grafica-based GPlot that will be rendering the Time Series trace
    private GPointsArray analogReadPoints;
    private int nPoints;
    private int numSeconds;
    private float timeBetweenPoints;

    private int channelColor; //color of plot trace

    private boolean isAutoscale; //when isAutoscale equals true, the y-axis of each channelBar will automatically update to scale to the largest visible amplitude
    private int autoScaleYLim = 0;

    private TextBox analogValue;
    private TextBox analogPin;
    private TextBox digitalPin;

    private boolean drawAnalogValue;
    private int lastProcessedDataPacketInd = 0;

    private AnalogCapableBoard analogBoard;

    public AnalogReadBar(GUI MAIN, int _analogInputPin, int _x, int _y, int _w, int _h) {
        super(MAIN); // channel number, x/y location, height, width
        this.MAIN = MAIN;
        analogInputPin = _analogInputPin;
        int digitalPinNum = 0;
        if (analogInputPin == 7) {
            auxValuesPosition = 2;
            digitalPinNum = 13;
        } else if (analogInputPin == 6) {
            auxValuesPosition = 1;
            digitalPinNum = 12;
        } else {
            analogInputPin = 5;
            auxValuesPosition = 0;
            digitalPinNum = 11;
        }

        analogInputString = MAIN.str(analogInputPin);

        x = _x;
        y = _y;
        w = _w;
        h = _h;

        numSeconds = 20;
        plot = new GPlot(MAIN);
        plot.setPos(x + 36 + 4, y);
        plot.setDim(w - 36 - 4, h);
        plot.setMar(0f, 0f, 0f, 0f);
        plot.setLineColor((int)MAIN.channelColors[(auxValuesPosition)%8]);
        plot.setXLim((float) -3.2, (float) -2.9);
        plot.setYLim(-200,200);
        plot.setPointSize(2);
        plot.setPointColor(0);
        plot.setAllFontProperties("Arial", 0, 14);
        plot.getXAxis().setFontColor(MAIN.OPENBCI_DARKBLUE);
        plot.getXAxis().setLineColor(MAIN.OPENBCI_DARKBLUE);
        plot.getXAxis().getAxisLabel().setFontColor(MAIN.OPENBCI_DARKBLUE);
        if (selectedProtocol == BoardProtocol.WIFI) {
            if(auxValuesPosition == 1) {
                plot.getXAxis().setAxisLabelText("Time (s)");
            }
        } else {
            if(auxValuesPosition == 2) {
                plot.getXAxis().setAxisLabelText("Time (s)");
            }
        }

        initArrays();


        analogValue = new TextBox(MAIN, "t", x + 36 + 4 + (w - 36 - 4) - 2, y + h);
        analogValue.setTextColor(MAIN.OPENBCI_DARKBLUE);
        analogValue.alignH = MAIN.RIGHT;
        analogValue.alignV = MAIN.BOTTOM;
        analogValue.drawBackground = true;
        analogValue.setBackgroundColor(pApplet.color(255, 255, 255, 125));

        analogPin = new TextBox(MAIN, "A" + analogInputString, x+3, y + h);
        analogPin.setTextColor(MAIN.OPENBCI_DARKBLUE);
        analogPin.alignH = MAIN.CENTER;
        digitalPin = new TextBox(MAIN, "(D" + digitalPinNum + ")", x+3, y + h + 12);
        digitalPin.setTextColor(MAIN.OPENBCI_DARKBLUE);
        digitalPin.alignH = MAIN.CENTER;

        drawAnalogValue = true;
        analogBoard = (AnalogCapableBoard) currentBoard;
    }

    public void initArrays() {
        nPoints = nPointsBasedOnDataSource();
        timeBetweenPoints = (float)numSeconds / (float)nPoints;
        analogReadPoints = new GPointsArray(nPoints);

        for (int i = 0; i < nPoints; i++) {
            float time = calcTimeAxis(i);
            float analog_value = 0.0F; //0.0 for all points to start
            analogReadPoints.set(i, time, analog_value, "");
        }

        plot.setPoints(analogReadPoints); //set the plot with 0.0 for all auxReadPoints to start
    }

    public void update() {

        // early out if unactive
        if (!analogBoard.isAnalogActive()) {
            return;
        }

        // update data in plot
        updatePlotPoints();
        if(isAutoscale) {
            autoScale();
        }

        //Fetch the last value in the buffer to display on screen
        float val = analogReadPoints.getLastPoint().getY();
        analogValue.string = String.format(getFmt(val),val);
    }

    private String getFmt(float val) {
        String fmt;
        if (val > 100.0f) {
            fmt = "%.0f";
        } else if (val > 10.0f) {
            fmt = "%.1f";
        } else {
            fmt = "%.2f";
        }
        return fmt;
    }

    public float calcTimeAxis(int sampleIndex) {
        return -(float)numSeconds + (float)sampleIndex * timeBetweenPoints;
    }

    public void updatePlotPoints() {
        List<double[]> allData = analogBoard.getDataWithAnalog(nPoints);
        int[] channels = analogBoard.getAnalogChannels();

        if (channels.length == 0) {
            return;
        }

        for (int i=0; i < nPoints; i++) {
            float timey = calcTimeAxis(i);
            float value = (float)allData.get(i)[channels[auxValuesPosition]];
            analogReadPoints.set(i, timey, value, "");
        }

        plot.setPoints(analogReadPoints);
    }

    public void draw() {
        pApplet.pushStyle();

        //draw plot
        pApplet.stroke(MAIN.OPENBCI_BLUE_ALPHA50);
        pApplet.fill(pApplet.color(125,30,12,30));

        pApplet.rect(x + 36 + 4, y, w - 36 - 4, h);

        plot.beginDraw();
        plot.drawBox(); // we won't draw this eventually ...
        plot.drawGridLines(GPlot.VERTICAL);
        plot.drawLines();
        if (selectedProtocol == BoardProtocol.WIFI) {
            if(auxValuesPosition == 1) { //only draw the x axis label on the bottom channel bar
                plot.drawXAxis();
                plot.getXAxis().draw();
            }
        }
        else {
            if(auxValuesPosition == 2) { //only draw the x axis label on the bottom channel bar
                plot.drawXAxis();
                plot.getXAxis().draw();
            }
        }

        plot.endDraw();

        if(drawAnalogValue) {
            analogValue.draw();
            analogPin.draw();
            digitalPin.draw();
        }

        pApplet.popStyle();
    }

    public int nPointsBasedOnDataSource() {
        return numSeconds * ((AnalogCapableBoard)currentBoard).getAnalogSampleRate();
    }

    public void adjustTimeAxis(int _newTimeSize) {
        numSeconds = _newTimeSize;
        plot.setXLim(-_newTimeSize,0);

        nPoints = nPointsBasedOnDataSource();

        analogReadPoints = new GPointsArray(nPoints);
        if (_newTimeSize > 1) {
            plot.getXAxis().setNTicks(_newTimeSize);  //sets the number of axis divisions...
        }
        else {
            plot.getXAxis().setNTicks(10);
        }

        updatePlotPoints();
    }

    public void adjustVertScale(int _vertScaleValue) {
        if(_vertScaleValue == 0) {
            isAutoscale = true;
        } else {
            isAutoscale = false;
            plot.setYLim(-_vertScaleValue, _vertScaleValue);
        }
    }

    public void autoScale() {
        autoScaleYLim = 0;
        for(int i = 0; i < nPoints; i++) {
            if((int)(MAIN.abs(analogReadPoints.getY(i))) > autoScaleYLim) {
                autoScaleYLim = (int)(MAIN.abs(analogReadPoints.getY(i)));
            }
        }
        plot.setYLim(-autoScaleYLim, autoScaleYLim);
    }

    public void screenResized(int _x, int _y, int _w, int _h) {
        x = _x;
        y = _y;
        w = _w;
        h = _h;

        plot.setPos(x + 36 + 4, y);
        plot.setDim(w - 36 - 4, h);

        analogValue.x = x + 36 + 4 + (w - 36 - 4) - 2;
        analogValue.y = y + h;

        analogPin.x = x + 14;
        analogPin.y = y + (int)(h/2.0);
        digitalPin.x = analogPin.x;
        digitalPin.y = analogPin.y + 12;
    }
};