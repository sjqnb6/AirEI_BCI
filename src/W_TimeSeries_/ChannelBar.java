package W_TimeSeries_;

import ADS1299SettingsBoard_.ADS1299SettingsBoard;
import Extras_.TextBox;
import GUI.GUIManager;
import ImpedanceSettingsBoard_.ImpedanceSettingsBoard;
import controlP5.Button;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlP5;
import grafica.GPlot;
import grafica.GPoint;
import grafica.GPointsArray;
import processing.core.PApplet;
import processing.core.PImage;

import static Debugging_.GF.verbosePrint;
import static Extras_.GF.log10;
import static Globel.GUI.*;
import static WidgetManager_.GVI.w_timeSeries;

import Globel.GUI;
//========================================================================================================================
//                      CHANNEL BAR CLASS -- Implemented by Time Series Widget Class
//========================================================================================================================
//this class contains the plot and buttons for a single channel of the Time Series widget
//one of these will be created for each channel (4, 8, or 16)
class ChannelBar {
    GUI MAIN;
    int channelIndex; //duh
    String channelString;
    int x, y, w, h;
    int defaultH;
    ControlP5 cbCp5;
    Button onOffButton;
    int onOff_diameter;
    int yScaleButton_h;
    int yScaleButton_w;
    Button yScaleButton_pos;
    Button yScaleButton_neg;
    int yAxisLabel_h;
    private TextBox yAxisMax;
    private TextBox yAxisMin;

    int yAxisUpperLim;
    int yAxisLowerLim;
    int uiSpaceWidth;
    int padding_4 = 4;
    int minimumChannelHeight;
    int plotBottomWellH = 45;

    GPlot plot; //the actual grafica-based GPlot that will be rendering the Time Se ries trace
    GPointsArray channelPoints;
    int nPoints;
    int numSeconds;
    float timeBetweenPoints;

    int channelColor; //color of plot trace

    boolean isAutoscale = false; //when isAutoscale equals true, the y-axis of each channelBar will automatically update to scale to the largest visible amplitude
    float autoscaleMin;
    float autoscaleMax;
    int previousMillis = 0;

    TextBox voltageValue;
    TextBox impValue;

    boolean drawVoltageValue;

    public ChannelBar(GUI MAIN, int _channelIndex, int _x, int _y, int _w, int _h, PImage expand_default, PImage expand_hover, PImage expand_active, PImage contract_default, PImage contract_hover, PImage contract_active) {
        this.MAIN = MAIN;
        cbCp5 = new ControlP5(MAIN);
        cbCp5.setGraphics(MAIN, x, y);
        cbCp5.setAutoDraw(false); //Setting this saves code as cp5 elements will only be drawn/visible when [cp5].draw() is called

        channelIndex = _channelIndex;
        channelString = MAIN.str(channelIndex + 1);

        x = _x;
        y = _y;
        w = _w;
        h = _h;
        defaultH = h;

        onOff_diameter = h > 26 ? 26 : h - 2;
        createOnOffButton("通道开关"+channelIndex, channelString, x + 6, y + (int)(h/2) - (int)(onOff_diameter/2), onOff_diameter, onOff_diameter);

        //Create GPlot for this Channel
        uiSpaceWidth = 36 + padding_4;
        yAxisUpperLim = 200;
        yAxisLowerLim = -200;
        numSeconds = 5;
        plot = new GPlot(MAIN);
        plot.setPos(x + uiSpaceWidth, y);
        plot.setDim(w - uiSpaceWidth, h);
        plot.setMar(0f, 0f, 0f, 0f);
        plot.setLineColor((int)MAIN.channelColors[channelIndex%8]);
        plot.setXLim(-5,0);
        plot.setYLim(yAxisLowerLim, yAxisUpperLim);
        plot.setPointSize(2);
        plot.setPointColor(0);
        plot.setAllFontProperties("Microsoft YaHei", 0, 14);
        plot.getXAxis().setFontColor(MAIN.OPENBCI_DARKBLUE);
        plot.getXAxis().setLineColor(MAIN.OPENBCI_DARKBLUE);
        plot.getXAxis().getAxisLabel().setFontColor(MAIN.OPENBCI_DARKBLUE);
        if(channelIndex == nchan-1) {
            plot.getXAxis().setAxisLabelText("时间 (s)");
            plot.getXAxis().getAxisLabel().setOffset(plotBottomWellH/2 + 5f);
        }
        // plot.setBgColor(OPENBCI_BLUE);

        //Fill the GPlot with initial data
        nPoints = nPointsBasedOnDataSource();
        channelPoints = new GPointsArray(nPoints);
        timeBetweenPoints = (float)numSeconds / (float)nPoints;
        for (int i = 0; i < nPoints; i++) {
            float time = -(float)numSeconds + (float)i*timeBetweenPoints;
            float filt_uV_value = 0.0F; //0.0 for all points to start
            GPoint tempPoint = new GPoint(time, filt_uV_value);
            channelPoints.set(i, tempPoint);
        }
        plot.setPoints(channelPoints); //set the plot with 0.0 for all channelPoints to start

        //Create a UI to custom scale the Y axis for this channel
        yScaleButton_w = 18;
        yScaleButton_h = 18;
        yAxisLabel_h = 12;
        int padding = 2;
        yAxisMax = new TextBox(MAIN, "+"+yAxisUpperLim+"uV", x + uiSpaceWidth + padding, y + (int)(padding*1.5), MAIN.OPENBCI_DARKBLUE, MAIN.color(255,255,255,175), MAIN.LEFT, MAIN.TOP);
        yAxisMin = new TextBox(MAIN, yAxisLowerLim+"uV", x + uiSpaceWidth + padding, y + h - yAxisLabel_h - padding_4, MAIN.OPENBCI_DARKBLUE, MAIN.color(255,255,255,175), MAIN.LEFT, MAIN.TOP);
        customYLim(yAxisMax, yAxisUpperLim);
        customYLim(yAxisMin, yAxisLowerLim);
        yScaleButton_neg = createYScaleButton(channelIndex, false, "decreaseYscale", "-T", x + uiSpaceWidth + padding, y + w/2 - yScaleButton_h/2, yScaleButton_w, yScaleButton_h, contract_default, contract_hover, contract_active);
        yScaleButton_pos = createYScaleButton(channelIndex, true, "increaseYscale", "+T", x + uiSpaceWidth + padding*2 + yScaleButton_w, y + w/2 - yScaleButton_h/2, yScaleButton_w, yScaleButton_h, expand_default, expand_hover, expand_active);

        //Create textBoxes to display the current values
        impValue = new TextBox(MAIN, "", x + uiSpaceWidth + (int)plot.getDim()[0], y + padding, MAIN.OPENBCI_DARKBLUE, MAIN.color(255,255,255,175), MAIN.RIGHT, MAIN.TOP);
        voltageValue = new TextBox(MAIN, "", x + uiSpaceWidth + (int)plot.getDim()[0] - padding, y + h, MAIN.OPENBCI_DARKBLUE, MAIN.color(255,255,255,175), MAIN.RIGHT, MAIN.BOTTOM);
        drawVoltageValue = true;

        //Establish a minimumChannelHeight
        minimumChannelHeight = padding_4 + yAxisLabel_h*2;
    }

    void update() {
        //Reusable variables
        String fmt; float val;

        //update the voltage values
        val = dataProcessing.data_std_uV[channelIndex];
        voltageValue.string = String.format(getFmt(val),val) + " uVrms";
        if (is_railed != null) {
            voltageValue.setText(is_railed[channelIndex].notificationString + voltageValue.string);
            voltageValue.setTextColor(is_railed[channelIndex].getColor());
        }

        //update the impedance values
        val = data_elec_imp_ohm[channelIndex]/1000;
        fmt = String.format(getFmt(val),val) + " kOhm";
        if (is_railed != null && is_railed[channelIndex].is_railed == true) {
            fmt = "RAILED - " + fmt;
        }
        impValue.setText(fmt);

        // update data in plot
        updatePlotPoints();

        if(MAIN.currentBoard.isEXGChannelActive(channelIndex)) {
            onOffButton.setColorBackground(MAIN.channelColors[channelIndex%8]); // power down == false, set color to vibrant
        }
        else {
            onOffButton.setColorBackground(50); // power down == true, set to grey
        }
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

    private void updatePlotPoints() {
        autoscaleMax = -Float.MAX_VALUE;
        autoscaleMin = Float.MAX_VALUE;
        // update data in plot
        if (dataProcessingFilteredBuffer[channelIndex].length >= nPoints) {
            for (int i = dataProcessingFilteredBuffer[channelIndex].length - nPoints; i < dataProcessingFilteredBuffer[channelIndex].length; i++) {
                float time = -(float)numSeconds + (float)(i-(dataProcessingFilteredBuffer[channelIndex].length-nPoints))*timeBetweenPoints;
                float filt_uV_value = dataProcessingFilteredBuffer[channelIndex][i];

                // update channel point in place
                channelPoints.set(i-(dataProcessingFilteredBuffer[channelIndex].length-nPoints), time, filt_uV_value, "");
                autoscaleMax = Math.max(filt_uV_value, autoscaleMax);
                autoscaleMin = Math.min(filt_uV_value, autoscaleMin);
            }
            applyAutoscale();
            plot.setPoints(channelPoints); //reset the plot with updated channelPoints
        }
    }

    public void draw(boolean hardwareSettingsAreOpen) {

        plot.beginDraw();
        plot.drawBox();
        plot.drawGridLines(GPlot.VERTICAL);
        try {
            plot.drawLines();
        } catch (NullPointerException e) {
            e.printStackTrace();
            MAIN.println("PLOT ERROR ON CHANNEL " + channelIndex);

        }
        //Draw the x axis label on the bottom channel bar, hide if hardware settings are open
        if (isBottomChannel() && !hardwareSettingsAreOpen) {
            plot.drawXAxis();
            plot.getXAxis().draw();
        }
        plot.endDraw();

        //draw channel holder background
        MAIN.pushStyle();
        MAIN.stroke(MAIN.OPENBCI_BLUE_ALPHA50);
        MAIN.noFill();
        MAIN.rect(x,y,w,h);
        MAIN.popStyle();

        //draw channelBar separator line in the middle of interChannelBarSpace
        if (!isBottomChannel()) {
            MAIN.pushStyle();
            MAIN.stroke(MAIN.OPENBCI_DARKBLUE);
            MAIN.strokeWeight(1);
            int separator_y = y + h + (int)(w_timeSeries.interChannelBarSpace/2);
            MAIN.line(x, separator_y, x + w, separator_y);
            MAIN.popStyle();
        }

        //draw impedance values in time series also for each channel
        drawVoltageValue = true;
        if (MAIN.currentBoard instanceof ImpedanceSettingsBoard) {
            if(((ImpedanceSettingsBoard)MAIN.currentBoard).isCheckingImpedance(channelIndex)) {
                impValue.draw();
                drawVoltageValue = false;
            }
        }

        if (drawVoltageValue) {
            voltageValue.draw();
        }

        //Hide yAxisButtons when hardware settings are open, labels would start to overlap, or using autoscale
        boolean b = !hardwareSettingsAreOpen && (h > yScaleButton_h + yAxisLabel_h*2 + 2) && !isAutoscale;
        yScaleButton_pos.setVisible(b);
        yScaleButton_neg.setVisible(b);
        b = !hardwareSettingsAreOpen && h > minimumChannelHeight;
        yAxisMin.setVisible(b);
        yAxisMax.setVisible(b);
        yAxisMin.draw();
        yAxisMax.draw();

        try {
            cbCp5.draw();
        } catch (NullPointerException e) {
            e.printStackTrace();
            MAIN.println("CP5 ERROR ON CHANNEL " + channelIndex);
        }
    }

    private int nPointsBasedOnDataSource() {
        return numSeconds * MAIN.currentBoard.getSampleRate();
    }

    public void adjustTimeAxis(int _newTimeSize) {
        numSeconds = _newTimeSize;
        plot.setXLim(-_newTimeSize,0);

        nPoints = nPointsBasedOnDataSource();
        channelPoints = new GPointsArray(nPoints);
        if(_newTimeSize > 1) {
            plot.getXAxis().setNTicks(_newTimeSize);  //sets the number of axis divisions...
        }else{
            plot.getXAxis().setNTicks(10);
        }

        updatePlotPoints();
    }

    //Happens when user selects vert scale dropdown
    public void adjustVertScale(int _vertScaleValue) {
        //Early out if autoscale
        if (_vertScaleValue == 0) {
            isAutoscale = true;
            return;
        }
        isAutoscale = false;
        yAxisLowerLim = -_vertScaleValue;
        yAxisUpperLim = _vertScaleValue;
        plot.setYLim(yAxisLowerLim, yAxisUpperLim);
        //Update button text
        customYLim(yAxisMin, yAxisLowerLim);
        customYLim(yAxisMax, yAxisUpperLim);
    }

    public void applyAutoscale() {
        //Do this once a second for all TimeSeries ChannelBars to save on resources
        int newMillis = MAIN.millis();
        boolean doAutoscale = newMillis > previousMillis + 1000;
        if (isAutoscale && MAIN.currentBoard.isStreaming() && doAutoscale) {
            autoscaleMin = (int) Math.floor(autoscaleMin);
            autoscaleMax = (int) Math.ceil(autoscaleMax);
            previousMillis = newMillis;
            plot.setYLim(autoscaleMin, autoscaleMax); //<---- This is a very expensive method. Here is the bottleneck.
            customYLim(yAxisMin, (int)autoscaleMin);
            customYLim(yAxisMax, (int)autoscaleMax);
        }
    }

    //Update yAxis text and responsively size Textfield
    private void customYLim(TextBox tb, int limit) {
        StringBuilder s = new StringBuilder(limit > 0 ? "+" : "");
        s.append(limit);
        s.append("uV");
        tb.setText(s.toString());
    }

    public void resize(int _x, int _y, int _w, int _h) {
        x = _x;
        y = _y;
        w = _w;
        h = _h;

        //reposition & resize the plot
        int plotW = w - uiSpaceWidth;
        plot.setPos(x + uiSpaceWidth, y);
        plot.setDim(plotW, h);

        int padding = 2;
        voltageValue.setPosition(x + uiSpaceWidth + (w - uiSpaceWidth) - padding, y + h);
        impValue.setPosition(x + uiSpaceWidth + (int)plot.getDim()[0], y + padding);

        yScaleButton_neg.setPosition(x + uiSpaceWidth + padding, y + h/2 - yScaleButton_h/2);
        yScaleButton_pos.setPosition(x + uiSpaceWidth + padding*2 + yScaleButton_w, y + h/2 - yScaleButton_h/2);

        yAxisMax.setPosition(x + uiSpaceWidth + padding, y + (int)(padding*1.5) - 2);
        yAxisMin.setPosition(x + uiSpaceWidth + padding, y + h - yAxisLabel_h - padding - 1);

        onOff_diameter = h > 26 ? 26 : h - 2;
        onOffButton.setSize(onOff_diameter, onOff_diameter);
        onOffButton.setPosition(x + 6, y + (int)(h/2) - (int)(onOff_diameter/2));
    }

    public void updateCP5(PApplet _parent) {
        cbCp5.setGraphics(_parent, 0, 0);
    }

    private boolean isBottomChannel() {
        int numActiveChannels = w_timeSeries.tsChanSelect.activeChan.size();
        boolean isLastChannel = channelIndex ==  w_timeSeries.tsChanSelect.activeChan.get(numActiveChannels - 1);
        return isLastChannel;
    }

    public void mousePressed() {
    }

    public void mouseReleased() {
    }

    private void createOnOffButton(String name, String text, int _x, int _y, int _w, int _h) {
        onOffButton = MAIN.createButton(cbCp5, name, text, _x, _y, _w, _h, 0, h2, 16, MAIN.channelColors[channelIndex%8], MAIN.WHITE, MAIN.BUTTON_HOVER, MAIN.BUTTON_PRESSED, (Integer) null, -2);
        onOffButton.setCircularButton(true);
        onOffButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                boolean newState = !MAIN.currentBoard.isEXGChannelActive(channelIndex);
                MAIN.println("[" + channelString + "] onOff released - " + (newState ? "On" : "Off"));
                MAIN.currentBoard.setEXGChannelActive(channelIndex, newState);
                if (MAIN.currentBoard instanceof ADS1299SettingsBoard) {
                    w_timeSeries.adsSettingsController.updateChanSettingsDropdowns(channelIndex, MAIN.currentBoard.isEXGChannelActive(channelIndex));
                    boolean hasUnappliedChanges = MAIN.currentBoard.isEXGChannelActive(channelIndex) != newState;
                    w_timeSeries.adsSettingsController.setHasUnappliedSettings(channelIndex, hasUnappliedChanges);
                }
            }
        });
        onOffButton.setDescription("点击切换通道 " + channelString + ".");
    }

    private Button createYScaleButton(int chan, boolean shouldIncrease, String bName, String bText, int _x, int _y, int _w, int _h, PImage _default, PImage _hover, PImage _active) {
        _default.resize(_w, _h);
        _hover.resize(_w, _h);
        _active.resize(_w, _h);
        final Button myButton = cbCp5.addButton(bName)
                .setPosition(_x, _y)
                .setSize(_w, _h)
                .setColorLabel(MAIN.color(255))
                .setColorForeground(MAIN.OPENBCI_BLUE)
                .setColorBackground(MAIN.color(144, 100))
                .setImages(_default, _hover, _active)
                ;
        myButton.onClick(new yScaleButtonCallbackListener(chan, shouldIncrease));
        return myButton;
    }

    private class yScaleButtonCallbackListener implements CallbackListener {
        private int channel;
        private boolean increase;
        private final int hardLimit = 25;
        private int yLimOption = TimeSeriesYLim.UV_200.getValue();
        //private int delta = 0; //value to change limits by

        yScaleButtonCallbackListener(int theChannel, boolean isIncrease)  {
            channel = theChannel;
            increase = isIncrease;
        }
        public void controlEvent(CallbackEvent theEvent) {
            verbosePrint("A button was pressed for channel " + (channel+1) + ". Should we increase (or decrease?): " + increase);

            int inc = increase ? 1 : -1;
            int n = (int)(log10(MAIN.abs(yAxisLowerLim))) * 25 * inc;
            yAxisLowerLim -= n;
            n = (int)(log10(yAxisUpperLim)) * 25 * inc;
            yAxisUpperLim += n;

            yAxisLowerLim = yAxisLowerLim <= -hardLimit ? yAxisLowerLim : -hardLimit;
            yAxisUpperLim = yAxisUpperLim >= hardLimit ? yAxisUpperLim : hardLimit;
            plot.setYLim(yAxisLowerLim, yAxisUpperLim);
            //Update button text
            customYLim(yAxisMin, yAxisLowerLim);
            customYLim(yAxisMax, yAxisUpperLim);
        }
    }
};