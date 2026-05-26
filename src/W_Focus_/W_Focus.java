package W_Focus_;

////////////////////////////////////////////////////
//                                                //
//    W_focus.pde (ie "Focus Widget")             //
//    Enums can be found in FocusEnums.pde        //
//                                                //
//                                                //
//    Created by: Richard Waltman, March 2021     //
//                                                //
////////////////////////////////////////////////////

import AuditoryNeurofeedback_.AuditoryNeurofeedback;
import FocusEnums_.*;
import Grid_.Grid;
import Widget_.ChannelSelect;
import Widget_.Widget;
import brainflow.BrainFlowError;
import brainflow.BrainFlowModelParams;
import brainflow.DataFilter;
import brainflow.MLModel;
import controlP5.Controller;
import controlP5.ScrollableList;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import processing.core.PApplet;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import Globel.GUI;

import static Globel.GUI.*;

public class W_Focus extends Widget {
    // Match W_CFC / W_Connectivity dark tech palette.
    private static final int COLOR_BG_TOP = 0xFF0A1220;
    private static final int COLOR_BG_BOTTOM = 0xFF0D1830;
    private static final int COLOR_PANEL = 0xE013243F;
    private static final int COLOR_BORDER = 0x6683A2CC;
    private static final int COLOR_TEXT = 0xFFEAF2FF;
    private static final int COLOR_TEXT_SUB = 0xFF98AECE;
    private static final int COLOR_FOCUS = 0xFF6CE39A;
    private static final int COLOR_IDLE = 0xFF2A405E;

    GUI MAIN;
    //to see all core variables/methods of the Widget class, refer to Widget.pde
    //put your custom variables here...
    //private ControlP5 focus_cp5;
    //private Button widgetTemplateButton;
    private ChannelSelect focusChanSelect;
    private boolean prevChanSelectIsVisible = false;
    private AuditoryNeurofeedback auditoryNeurofeedback;


    private Grid dataGrid;
    private final int NUM_TABLE_ROWS = 6;
    private final int NUM_TABLE_COLUMNS = 2;
    //private final int TABLE_WIDTH = 142;
    private int tableHeight = 0;
    private int cellHeight = 10;
    private DecimalFormat df = new DecimalFormat("#.0000");

    private final int PAD_FIVE = 5;
    private final int PAD_TWO = 2;
    private final int METRIC_DROPDOWN_W = 100;
    private final int CLASSIFIER_DROPDOWN_W = 80;

    private FocusBar focusBar;
    private float focusBarHardYAxisLimit = 1.05f; //Provide slight "breathing room" to avoid GPlot error when metric value == 1.0
    private FocusXLim xLimit = FocusXLim.TEN;
    private FocusMetric focusMetric = FocusMetric.RELAXATION;
    private FocusClassifier focusClassifier = FocusClassifier.REGRESSION;
    private FocusThreshold focusThreshold = FocusThreshold.EIGHT_TENTHS;
    private FocusColors focusColors = FocusColors.GREEN;

    private int[] exgChannels;
    private int channelCount;
    private double[][] dataArray;

    private MLModel mlModel;
    private double metricPrediction = 0d;
    private boolean predictionExceedsThreshold = false;

    private float xc, yc, wc, hc; // status circle center xy, width and height
    private int graphX, graphY, graphW, graphH;
    private final int GRAPH_PADDING = 30;
    private int cBack, cDark, cMark, cFocus, cWave, cPanel;

    List<Controller> cp5ElementsToCheck = new ArrayList<Controller>();

    public W_Focus(GUI MAIN) {
        super(MAIN); //calls the parent CONSTRUCTOR method of Widget (DON'T REMOVE)
        this.MAIN = MAIN;
        //Add channel select dropdown to this widget
        focusChanSelect = new ChannelSelect(pApplet, this, x, y, w, navH, "FocusChannelSelect");
        focusChanSelect.activateAllButtons();
        cp5ElementsToCheck.addAll(focusChanSelect.getCp5ElementsForOverlapCheck());

        auditoryNeurofeedback = new AuditoryNeurofeedback(MAIN, x + PAD_FIVE, y + PAD_FIVE, w/2 - PAD_FIVE*2, navBarHeight/2);
        cp5ElementsToCheck.add((Controller)auditoryNeurofeedback.startStopButton);
        cp5ElementsToCheck.add((Controller)auditoryNeurofeedback.modeButton);

        exgChannels = MAIN.currentBoard.getEXGChannels();
        channelCount = MAIN.currentBoard.getNumEXGChannels();
        dataArray = new double[channelCount][];

        // initialize graphics parameters
        onColorChange();

        //This is the protocol for setting up dropdowns.
        dropdownWidth = 60; //Override the default dropdown width for this widget
        addDropdown("focusMetricDropdown", "监测指标", focusMetric.getEnumStringsAsList(), focusMetric.getIndex());
        addDropdown("focusClassifierDropdown", "分类器", focusClassifier.getEnumStringsAsList(), focusClassifier.getIndex());
        addDropdown("focusThresholdDropdown", "阈值", focusThreshold.getEnumStringsAsList(), focusThreshold.getIndex());
        addDropdown("focusWindowDropdown", "窗口", xLimit.getEnumStringsAsList(), xLimit.getIndex());


        //Create data table
        dataGrid = new Grid(MAIN, NUM_TABLE_ROWS, NUM_TABLE_COLUMNS, cellHeight);
        dataGrid.setTableFontAndSize(p7, 12);
        dataGrid.setDrawTableBorder(true);
        dataGrid.setString("量化值", 0, 0);
        dataGrid.setString("Delta (1.5-4Hz)", 1, 0);
        dataGrid.setString("Theta (4-8Hz)", 2, 0);
        dataGrid.setString("Alpha (7.5-13Hz)", 3, 0);
        dataGrid.setString("Beta (13-30Hz)", 4, 0);
        dataGrid.setString("Gamma (30-45Hz)", 5, 0);
        // Improve table readability on dark background.
        for (int r = 0; r < NUM_TABLE_ROWS; r++) {
            dataGrid.setTextColor(COLOR_TEXT_SUB, r, 0); // labels
            dataGrid.setTextColor(COLOR_TEXT, r, 1);     // values
        }

        //Instantiate local cp5 for this box. This allows extra control of drawing cp5 elements specifically inside this class.
        //focus_cp5 = new ControlP5(ourApplet);
        //focus_cp5.setGraphics(ourApplet, 0,0);
        //focus_cp5.setAutoDraw(false);

        //create our focus graph
        updateGraphDims();
        focusBar = new FocusBar(MAIN, xLimit.getValue(), focusBarHardYAxisLimit, graphX, graphY, graphW, graphH);

        initBrainFlowMetric();
    }

    public void update() {
        super.update(); //calls the parent update() method of Widget (DON'T REMOVE)

        //Update channel checkboxes and active channels
        focusChanSelect.update(x, y, w);

        //Flex the Gplot graph when channel select dropdown is open/closed
        if (focusChanSelect.isVisible() != prevChanSelectIsVisible) {
            channelSelectFlexWidgetUI();
            prevChanSelectIsVisible = focusChanSelect.isVisible();
        }

        if (MAIN.currentBoard.isStreaming()) {
            dataGrid.setString(df.format(metricPrediction), 0, 1);
            focusBar.update(metricPrediction);
        }

        lockElementsOnOverlapCheck(cp5ElementsToCheck);
    }

    public void draw() {
        super.draw(); //calls the parent draw() method of Widget (DON'T REMOVE)
        //remember to refer to x,y,w,h which are the positioning variables of the Widget class
        MAIN.pushStyle();
        drawGradientBackground(x, y - 1, w, h + 1);
        MAIN.noStroke();
        MAIN.fill(COLOR_PANEL);
        MAIN.rect(x, y - 1, w, h + 1);
        MAIN.stroke(COLOR_BORDER);
        MAIN.noFill();
        MAIN.rect(x, y - 1, w, h + 1);
        MAIN.popStyle();

        //Draw data table
        dataGrid.draw();

        drawStatusCircle();

        if (false) {
            //Draw some guides to help develop this widget faster
            MAIN.pushStyle();
            MAIN.stroke(MAIN.OPENBCI_DARKBLUE);
            //Main guides
            MAIN.line(x, y+(h/2), x+w, y+(h/2));
            MAIN.line(x+(w/2), y, x+(w/2), y+(h/2));
            //Top left container center
            MAIN.line(x+(w/4), y, x+(w/4), y+(h/2));
            MAIN.line(x, y+(h/4), x+(w/2), y+(h/4));
            MAIN.popStyle();
        }

        //This draws all cp5 objects in the local instance
        //focus_cp5.draw();
        auditoryNeurofeedback.draw();

        //Draw the graph
        focusBar.draw();

        focusChanSelect.draw();
    }

    public void screenResized() {
        super.screenResized(); //calls the parent screenResized() method of Widget (DON'T REMOVE)

        //Very important to allow users to interact with objects after app resize
        //focus_cp5.setGraphics(ourApplet, 0, 0);

        resizeTable();

        //We need to set the position of our Cp5 object after the screen is resized
        //widgetTemplateButton.setPosition(x + w/2 - widgetTemplateButton.getWidth()/2, y + h/2 - widgetTemplateButton.getHeight()/2);

        updateStatusCircle();
        updateAuditoryNeurofeedbackPosition();

        updateGraphDims();
        focusBar.screenResized(graphX, graphY, graphW, graphH);
        focusChanSelect.screenResized(pApplet);

        //Custom resize these dropdowns due to longer text strings as options
        cp5_widget.get(ScrollableList.class, "focusMetricDropdown").setWidth(METRIC_DROPDOWN_W);
        cp5_widget.get(ScrollableList.class, "focusMetricDropdown").setPosition(
                x0 + w0 - (dropdownWidth*2) - METRIC_DROPDOWN_W - CLASSIFIER_DROPDOWN_W - (PAD_TWO*4),
                navH + y0 + PAD_TWO
        );
        cp5_widget.get(ScrollableList.class, "focusClassifierDropdown").setWidth(CLASSIFIER_DROPDOWN_W);
        cp5_widget.get(ScrollableList.class, "focusClassifierDropdown").setPosition(
                x0 + w0 - (dropdownWidth*2) - CLASSIFIER_DROPDOWN_W - (PAD_TWO*3),
                navH + y0 + PAD_TWO
        );
    }

    public void mousePressed() {
        super.mousePressed(); //calls the parent mousePressed() method of Widget (DON'T REMOVE)
        focusChanSelect.mousePressed(this.dropdownIsActive); //Calls channel select mousePressed and checks if clicked
    }

    private void resizeTable() {
        int extraPadding = focusChanSelect.isVisible() ? navHeight : 0;
        float upperLeftContainerW = w/2;
        float upperLeftContainerH = h/2;
        //float min = min(upperLeftContainerW, upperLeftContainerH);
        int tx = x + (int)(upperLeftContainerW);
        int ty = y + PAD_FIVE + extraPadding;
        int tw = (int)(upperLeftContainerW) - PAD_FIVE*2;
        //tableHeight = tw;
        dataGrid.setDim(tx, ty, tw);
        dataGrid.setTableHeight((int)(upperLeftContainerH - PAD_FIVE*2));
        dataGrid.dynamicallySetTextVerticalPadding(0, 0);
        dataGrid.setHorizontalCenterTextInCells(true);
    }

    private void updateAuditoryNeurofeedbackPosition() {
        int extraPadding = focusChanSelect.isVisible() ? navHeight : 0;
        int subContainerMiddleX = x + w/4;
        auditoryNeurofeedback.screenResized(subContainerMiddleX, (int)(y + h/2 - navHeight + extraPadding), w/2 - PAD_FIVE*2, navBarHeight/2);
    }

    private void updateStatusCircle() {
        float upperLeftContainerW = w/2;
        float upperLeftContainerH = h/2;
        float min = MAIN.min(upperLeftContainerW, upperLeftContainerH);
        xc = x + w/4;
        yc = y + h/4 - navHeight;
        wc = min * (3f/5);
        hc = wc;
    }

    private void updateGraphDims() {
        graphW = (int)(w - PAD_FIVE*4);
        graphH = (int)(h/2 - GRAPH_PADDING - PAD_FIVE*2);
        graphX = x + PAD_FIVE*2;
        graphY = (int)(y + h/2);
    }

    //Core method to fetch and process data
    //Returns a metric value from 0. to 1. When there is an error, returns -1.
    private double updateFocusState() {
        try {
            int windowSize = MAIN.currentBoard.getSampleRate() * xLimit.getValue();
            // getData in GUI returns data in shape ndatapoints x nchannels, in BrainFlow its transposed
            List<double[]> currentData = MAIN.currentBoard.getData(windowSize);

            if (currentData.size() != windowSize || focusChanSelect.activeChan.size() <= 0) {
                return -1.0;
            }

            for (int i = 0; i < channelCount; i++) {
                dataArray[i] = new double[windowSize];
                for (int j = 0; j < currentData.size(); j++) {
                    dataArray[i][j] = currentData.get(j)[exgChannels[i]];
                }
            }

            int[] channelsInDataArray = ArrayUtils.toPrimitive(
                    focusChanSelect.activeChan.toArray(
                            new Integer[focusChanSelect.activeChan.size()]
                    ));

            //Full Source Code for this method: https://github.com/brainflow-dev/brainflow/blob/c5f0ad86683e6eab556e30965befb7c93e389a3b/src/data_handler/data_handler.cpp#L1115
            Pair<double[], double[]> bands = DataFilter.get_avg_band_powers (dataArray, channelsInDataArray, MAIN.currentBoard.getSampleRate(), true);
            double[] featureVector = bands.getLeft ();

            //Left array is Averages, right array is Standard Deviations. Update values using Averages.
            updateBandPowerTableValues(bands.getLeft());

            //Keep this here
            double prediction = mlModel.predict(featureVector)[0];
            //println("Concentration: " + prediction);

            //Send band power and prediction data to AuditoryNeurofeedback class
            auditoryNeurofeedback.update(bands.getLeft(), (float)prediction);

            return prediction;

        } catch (BrainFlowError e) {
            e.printStackTrace();
            MAIN.println("Error updating focus state!");
            return -1d;
        }
    }

    private void updateBandPowerTableValues(double[] bandPowers) {
        for (int i = 0; i < bandPowers.length; i++) {
            dataGrid.setString(df.format(bandPowers[i]), 1 + i, 1);
        }
    }

    private void drawStatusCircle() {
        int fillColor;
        int strokeColor;
        StringBuilder sb = new StringBuilder("");
        String targetLabel = focusMetric.getString();
        if (predictionExceedsThreshold) {
            fillColor = COLOR_FOCUS;
            strokeColor = COLOR_FOCUS;
            sb.append("已达");
            sb.append(targetLabel);
            sb.append("状态");
        } else {
            fillColor = COLOR_IDLE;
            strokeColor = COLOR_IDLE;
            sb.append("未达");
            sb.append(targetLabel);
            sb.append("阈值");
        }
        //Draw status graphic
        MAIN.pushStyle();
        MAIN.noStroke();
        MAIN.fill(fillColor);
        MAIN.stroke(strokeColor);
        MAIN.ellipseMode(MAIN.CENTER);
        MAIN.ellipse(xc, yc, wc, hc);
        MAIN.noStroke();
        MAIN.fill(COLOR_TEXT);
        MAIN.textAlign(MAIN.CENTER);
        MAIN.text(sb.toString(), xc, yc + hc/2 + 16);
        MAIN.popStyle();
    }

    private void initBrainFlowMetric() {
        BrainFlowModelParams modelParams = new BrainFlowModelParams(
                focusMetric.getMetric().get_code(),
                focusClassifier.getClassifier().get_code()
        );
        mlModel = new MLModel (modelParams);
        try {
            mlModel.prepare();
        } catch (BrainFlowError e) {
            e.printStackTrace();
        }
    }

    //Called on haltSystem() when GUI exits or session stops
    public void endSession() {
        try {
            mlModel.release();
        } catch (BrainFlowError e) {
            e.printStackTrace();
        }
    }

    private void onColorChange() {
        switch(focusColors) {
            case GREEN:
                cBack = 0xffffff;   //white
                cDark = 0x3068a6;   //medium/dark blue
                cMark = 0x4d91d9;    //lighter blue
                cFocus = 0xb8dc69;   //theme green
                cWave = 0xffdd3a;    //yellow
                cPanel = 0xf5f5f5;   //little grey
                break;
            case ORANGE:
                cBack = 0xffffff;   //white
                cDark = 0x377bc4;   //medium/dark blue
                cMark = 0x5e9ee2;    //lighter blue
                cFocus = 0xfcce51;   //orange
                cWave = 0xffdd3a;    //yellow
                cPanel = 0xf5f5f5;   //little grey
                break;
            case CYAN:
                cBack = 0xffffff;   //white
                cDark = 0x377bc4;   //medium/dark blue
                cMark = 0x5e9ee2;    //lighter blue
                cFocus = 0x91f4fc;   //cyan
                cWave = 0xffdd3a;    //yellow
                cPanel = 0xf5f5f5;   //little grey
                break;
        }
    }

    private void drawGradientBackground(int x0, int y0, int w0, int h0) {
        for (int i = 0; i < h0; i++) {
            float t = i / (float) Math.max(1, h0 - 1);
            int c = lerpRgb(COLOR_BG_TOP, COLOR_BG_BOTTOM, t);
            MAIN.stroke((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF);
            MAIN.line(x0, y0 + i, x0 + w0, y0 + i);
        }
    }

    private int lerpRgb(int c1, int c2, float t) {
        int r1 = (c1 >> 16) & 0xFF;
        int g1 = (c1 >> 8) & 0xFF;
        int b1 = c1 & 0xFF;
        int r2 = (c2 >> 16) & 0xFF;
        int g2 = (c2 >> 8) & 0xFF;
        int b2 = c2 & 0xFF;
        int r = (int) PApplet.lerp(r1, r2, t);
        int g = (int) PApplet.lerp(g1, g2, t);
        int b = (int) PApplet.lerp(b1, b2, t);
        return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    void channelSelectFlexWidgetUI() {
        focusBar.setPlotPosAndOuterDim(focusChanSelect.isVisible());
        int factor = focusChanSelect.isVisible() ? 1 : -1;
        yc += navHeight * factor;
        resizeTable();
        updateAuditoryNeurofeedbackPosition();
    }

    public void setFocusHorizScale(int n) {
        xLimit = xLimit.values()[n];
        focusBar.adjustTimeAxis(xLimit.getValue());
    }

    public void setMetric(int n) {
        focusMetric = focusMetric.values()[n];
        endSession();
        initBrainFlowMetric();
    }

    public void setClassifier(int n) {
        focusClassifier = focusClassifier.values()[n];
        endSession();
        initBrainFlowMetric();
    }

    public void setThreshold(int n) {
        focusThreshold = focusThreshold.values()[n];
    }

    public int getMetricExceedsThreshold() {
        return predictionExceedsThreshold ? 1 : 0;
    }

    public void killAuditoryFeedback() {
        auditoryNeurofeedback.killAudio();
    }

    //Called in DataProcessing.pde to update data even if widget is closed
    public void updateFocusWidgetData() {
        metricPrediction = updateFocusState();
        predictionExceedsThreshold = metricPrediction > focusThreshold.getValue();
    }
}; //end of class
