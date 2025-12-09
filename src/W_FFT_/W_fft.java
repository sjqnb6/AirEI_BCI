package W_FFT_;

////////////////////////////////////////////////////
//
// This class creates an FFT Plot
// It extends the Widget class
//
// Conor Russomanno, November 2016
//
// Requires the plotting library from grafica ...
// replacing the old gwoptics (which is now no longer supported)
//

import GUI.ColorPalette;
import Widget_.ChannelSelect;
import Widget_.Widget;
import controlP5.Controller;
import grafica.GPlot;
import grafica.GPoint;
import grafica.GPointsArray;
import processing.core.PApplet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static GUI.GGVI.*;
import static SystemManager.GF.getNfftSafe;

///////////////////////////////////////////////////

public class W_fft extends Widget {
    protected PApplet pApplet;
    public ColorPalette CP;

    public ChannelSelect fftChanSelect;
    boolean prevChanSelectIsVisible = false;

    public GPlot fft_plot; //create an fft plot for each active channel
    GPointsArray[] fft_points;  //create an array of points for each channel of data (4, 8, or 16)


    public int[] xLimOptions = {20, 40, 60, 100, 120, 250, 500, 800};
    public int[] yLimOptions = {10, 50, 100, 1000};

    int xLim = xLimOptions[2];  //maximum value of x axis ... in this case 20 Hz, 40 Hz, 60 Hz, 120 Hz
    int xMax = xLimOptions[xLimOptions.length-1];   //maximum possible frequency in FFT
    int FFT_indexLim = (int)(1.0*xMax*(getNfftSafe()/currentBoard.getSampleRate()));   // maxim value of FFT index
    int yLim = yLimOptions[2];  //maximum value of y axis ... 100 uV

    List<Controller> cp5ElementsToCheck = new ArrayList<Controller>();

    public W_fft(PApplet _parent){
        super(_parent); //calls the parent CONSTRUCTOR method of Widget (DON'T REMOVE)
        pApplet = _parent;
        CP = new ColorPalette(_parent);

        //Add channel select dropdown to this widget
        fftChanSelect = new ChannelSelect(pApplet, this, x, y, w, navH, "BP_Channels");
        fftChanSelect.activateAllButtons();
        cp5ElementsToCheck.addAll(fftChanSelect.getCp5ElementsForOverlapCheck());

        //Default FFT plot settings
        settings.fftMaxFrqSave = 2;
        settings.fftMaxuVSave = 2;
        settings.fftLogLinSave = 0;
        settings.fftSmoothingSave = 3;
        settings.fftFilterSave = 0;

        //This is the protocol for setting up dropdowns.
        //Note that these 3 dropdowns correspond to the 3 global functions below
        //You just need to make sure the "id" (the 1st String) has the same name as the corresponding function
        addDropdown("MaxFreq", "Max Freq", Arrays.asList(settings.fftMaxFrqArray), settings.fftMaxFrqSave);
        addDropdown("VertScale", "Max uV", Arrays.asList(settings.fftVertScaleArray), settings.fftMaxuVSave);
        addDropdown("LogLin", "Log/Lin", Arrays.asList(settings.fftLogLinArray), settings.fftLogLinSave);
        addDropdown("Smoothing", "Smooth", Arrays.asList(settings.fftSmoothingArray), smoothFac_ind); //smoothFac_ind is a global variable at the top of W_HeadPlot.pde
        addDropdown("UnfiltFilt", "Filters?", Arrays.asList(settings.fftFilterArray), settings.fftFilterSave);

        fft_points = new GPointsArray[nchan];
        // println("fft_points.length: " + fft_points.length);
        initializeFFTPlot(_parent);

    }

    void initializeFFTPlot(PApplet _parent) {
        //setup GPlot for FFT
        fft_plot = new GPlot(_parent, x, y-navHeight, w, h+navHeight); //based on container dimensions
        fft_plot.setAllFontProperties("Arial", 0, 14);
        fft_plot.getXAxis().setAxisLabelText("Frequency (Hz)");
        fft_plot.getYAxis().setAxisLabelText("Amplitude (uV)");
        fft_plot.setMar(60, 70, 40, 30); //{ bot=60, left=70, top=40, right=30 } by default
        fft_plot.setLogScale("y");

        fft_plot.setYLim(0.1F, yLim);
        int _nTicks = (int)(yLim/10 - 1); //number of axis subdivisions
        fft_plot.getYAxis().setNTicks(_nTicks);  //sets the number of axis divisions...
        fft_plot.setXLim(0.1F, xLim);
        fft_plot.getYAxis().setDrawTickLabels(true);
        fft_plot.setPointSize(2);
        fft_plot.setPointColor(0);
        fft_plot.getXAxis().setFontColor(CP.OPENBCI_DARKBLUE);
        fft_plot.getXAxis().setLineColor(CP.OPENBCI_DARKBLUE);
        fft_plot.getXAxis().getAxisLabel().setFontColor(CP.OPENBCI_DARKBLUE);
        fft_plot.getYAxis().setFontColor(CP.OPENBCI_DARKBLUE);
        fft_plot.getYAxis().setLineColor(CP.OPENBCI_DARKBLUE);
        fft_plot.getYAxis().getAxisLabel().setFontColor(CP.OPENBCI_DARKBLUE);

        //setup points of fft point arrays
        for (int i = 0; i < fft_points.length; i++) {
            fft_points[i] = new GPointsArray(FFT_indexLim);
        }

        //fill fft point arrays
        for (int i = 0; i < fft_points.length; i++) { //loop through each channel
            for (int j = 0; j < FFT_indexLim; j++) {
                GPoint temp = new GPoint(j, 0);
                fft_points[i].set(j, temp);
            }
        }

        //map fft point arrays to fft plots
        fft_plot.setPoints(fft_points[0]);
    }

    public void update(){

        super.update(); //calls the parent update() method of Widget (DON'T REMOVE)
        float sr = currentBoard.getSampleRate();
        int nfft = getNfftSafe();

        //update the points of the FFT channel arrays for all channels
        for (int i = 0; i < fft_points.length; i++) {
            for (int j = 0; j < FFT_indexLim + 2; j++) {  //loop through frequency domain data, and store into points array
                GPoint powerAtBin = new GPoint((float) ((1.0*sr/nfft)*j), fftBuff[i].getBand(j));
                fft_points[i].set(j, powerAtBin);
            }
        }

        //Update channel select checkboxes and active channels
        fftChanSelect.update(x, y, w);

        //Flex the Gplot graph when channel select dropdown is open/closed
        if (fftChanSelect.isVisible() != prevChanSelectIsVisible) {
            flexGPlotSizeAndPosition();
            prevChanSelectIsVisible = fftChanSelect.isVisible();
        }

        if (fftChanSelect.isVisible()) {
            lockElementsOnOverlapCheck(cp5ElementsToCheck);
        }
    }

    public void draw(){
        super.draw(); //calls the parent draw() method of Widget (DON'T REMOVE)

        //remember to refer to x,y,w,h which are the positioning variables of the Widget class
        pApplet.pushStyle();

        //draw FFT Graph w/ all plots
        pApplet.noStroke();
        fft_plot.beginDraw();
        fft_plot.drawBackground();
        fft_plot.drawBox();
        fft_plot.drawXAxis();
        fft_plot.drawYAxis();
        fft_plot.drawGridLines(GPlot.BOTH);
        //Update and draw active channels that have been selected via channel select for this widget
        for (int j = 0; j < fftChanSelect.activeChan.size(); j++) {
            int chan = fftChanSelect.activeChan.get(j);
            fft_plot.setLineColor(CP.lineColor[chan]);
            //remap fft point arrays to fft plots
            fft_plot.setPoints(fft_points[chan]);
            fft_plot.drawLines();
        }
        fft_plot.endDraw();

        //for this widget need to redraw the grey bar, bc the FFT plot covers it up...
        pApplet.fill(200, 200, 200);
        pApplet.rect(x, y - navHeight, w, navHeight); //button bar

        pApplet.popStyle();

        fftChanSelect.draw();
    }

    public void screenResized(){
        super.screenResized(); //calls the parent screenResized() method of Widget (DON'T REMOVE)

        //update position/size of FFT plot
        fft_plot.setPos(x, y-navHeight);//update position
        fft_plot.setOuterDim(w, h+navHeight);//update dimensions

        fftChanSelect.screenResized(pApplet);
    }

    public void mousePressed(){
        super.mousePressed(); //calls the parent mousePressed() method of Widget (DON'T REMOVE)
        fftChanSelect.mousePressed(this.dropdownIsActive); //Calls channel select mousePressed and checks if clicked
    }

    public void mouseReleased(){
        super.mouseReleased(); //calls the parent mouseReleased() method of Widget (DON'T REMOVE)
    }

    void flexGPlotSizeAndPosition() {
        if (fftChanSelect.isVisible()) {
            fft_plot.setPos(x, y);
            fft_plot.setOuterDim(w, h);
        } else {
            fft_plot.setPos(x, y - navHeight);
            fft_plot.setOuterDim(w, h + navHeight);
        }
    }
};