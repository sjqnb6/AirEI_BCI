package W_BandPower_;

////////////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                                    //
//    W_BandPowers.pde                                                                                //
//                                                                                                    //
//    This is a band power visualization widget!                                                      //
//    (Couldn't think up more)                                                                        //
//    This is for visualizing the power of each brainwave band: delta, theta, alpha, beta, gamma      //
//    Averaged over all channels                                                                      //
//                                                                                                    //
//    Created by: Wangshu Sun, May 2017                                                               //
//    Modified by: Richard Waltman, March 2022                                                        //
//                                                                                                    //

import Widget_.ChannelSelect;
import Widget_.Widget;
import controlP5.Controller;
import grafica.GPlot;
import grafica.GPointsArray;
import processing.core.PApplet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static Globel.GUI.dataProcessing;
import static Globel.GUI.navHeight;

import Globel.GUI;
////////////////////////////////////////////////////////////////////////////////////////////////////////

public class W_BandPower extends Widget {
    GUI MAIN;
    // Match W_CFC / W_Connectivity dark tech palette.
    private static final int COLOR_BG_TOP = 0xFF0A1220;
    private static final int COLOR_BG_BOTTOM = 0xFF0D1830;
    private static final int COLOR_BG = 0xFF0E1A2F;
    private static final int COLOR_PANEL = 0xFF142843;
    private static final int COLOR_CARD = 0xFF13243F;
    private static final int COLOR_BORDER = 0x6683A2CC;
    private static final int COLOR_TEXT = 0xFFEAF2FF;
    private static final int COLOR_GRID = 0x2D90AED8;

    // indexes
    private final int DELTA = 0; // 1-4 Hz
    private final int THETA = 1; // 4-8 Hz
    private final int ALPHA = 2; // 8-13 Hz
    private final int BETA = 3; // 13-30 Hz
    private final int GAMMA = 4; // 30-55 Hz

    private final int NUM_BANDS = 5;
    private float[] activePower = new float[NUM_BANDS];
    private float[] normalizedBandPowers = new float[NUM_BANDS];

    private GPlot bp_plot;
    public ChannelSelect bpChanSelect;
    private boolean prevChanSelectIsVisible = false;

    private List<Controller> cp5ElementsToCheck = new ArrayList<Controller>();

    public W_BandPower(GUI MAIN) {
        super(MAIN); //calls the parent CONSTRUCTOR method of Widget (DON'T REMOVE)
        this.MAIN = MAIN;
        //Add channel select dropdown to this widget
        bpChanSelect = new ChannelSelect(pApplet, this, x, y, w, navH, "BP_Channels");
        bpChanSelect.activateAllButtons();
        cp5ElementsToCheck.addAll(bpChanSelect.getCp5ElementsForOverlapCheck());

        //Add settings dropdowns
        addDropdown("Smoothing", "平滑度", Arrays.asList(MAIN.settings.fftSmoothingArray), MAIN.smoothFac_ind); //smoothFac_ind is a global variable at the top of W_HeadPlot.pde
        addDropdown("UnfiltFilt", "滤波", Arrays.asList(MAIN.settings.fftFilterArray), MAIN.settings.fftFilterSave);

        // Setup for the BandPower plot
        bp_plot = new GPlot(MAIN, x, y-navHeight, w, h+navHeight);
        // bp_plot.setPos(x, y+navHeight);
        bp_plot.setDim(w, h);
        bp_plot.setLogScale("y");
        bp_plot.setYLim(0.1F, 100);
        bp_plot.setXLim(0, 5);
        bp_plot.getYAxis().setNTicks(9);
        bp_plot.getXAxis().setNTicks(0);
        bp_plot.getTitle().setTextAlignment(MAIN.LEFT);
        bp_plot.getTitle().setRelativePos(0);
        bp_plot.setAllFontProperties("Microsoft YaHei", 0, 14);
        bp_plot.getYAxis().getAxisLabel().setText("功率 — (uV)^2 / Hz");
        bp_plot.getXAxis().setAxisLabelText("脑电频段");
        bp_plot.getXAxis().getAxisLabel().setOffset(42f);
        bp_plot.startHistograms(GPlot.VERTICAL);
        bp_plot.getHistogram().setDrawLabels(true);
        bp_plot.setBgColor(COLOR_BG);
        bp_plot.setBoxBgColor(COLOR_PANEL);
        bp_plot.setBoxLineColor(COLOR_BORDER);
        bp_plot.setGridLineColor(COLOR_GRID);
        bp_plot.getXAxis().setFontColor(COLOR_TEXT);
        bp_plot.getXAxis().setLineColor(COLOR_TEXT);
        bp_plot.getXAxis().getAxisLabel().setFontColor(COLOR_TEXT);
        bp_plot.getYAxis().setFontColor(COLOR_TEXT);
        bp_plot.getYAxis().setLineColor(COLOR_TEXT);
        bp_plot.getYAxis().getAxisLabel().setFontColor(COLOR_TEXT);

        //setting border of histograms to match BG
        bp_plot.getHistogram().setLineColors(new int[]{
                pApplet.color(215, 232, 255), pApplet.color(215, 232, 255), pApplet.color(215, 232, 255),
                pApplet.color(215, 232, 255), pApplet.color(215, 232, 255)
                }
        );
        // High-contrast band colors for dark background.
        bp_plot.getHistogram().setBgColors(new int[] {
                        pApplet.color(74, 176, 255, 215),   // Delta
                        pApplet.color(77, 222, 209, 215),   // Theta
                        pApplet.color(93, 225, 122, 215),   // Alpha
                        pApplet.color(255, 196, 89, 215),   // Beta
                        pApplet.color(255, 118, 118, 215),  // Gamma
                }
        );
        //setting color of text label for each histogram bar on the x axis
        bp_plot.getHistogram().setFontColor(COLOR_TEXT);
    }

    public void update() {
        super.update(); //calls the parent update() method of Widget (DON'T REMOVE)

        //Update channel checkboxes and active channels
        bpChanSelect.update(x, y, w);

        //Flex the Gplot graph when channel select dropdown is open/closed
        if (bpChanSelect.isVisible() != prevChanSelectIsVisible) {
            flexGPlotSizeAndPosition();
            prevChanSelectIsVisible = bpChanSelect.isVisible();
        }

        GPointsArray bp_points = new GPointsArray(dataProcessing.headWidePower.length);
        bp_points.add((float) (DELTA + 0.5), activePower[DELTA], "DELTA\n0.5-4Hz");
        bp_points.add((float) (THETA + 0.5), activePower[THETA], "THETA\n4-8Hz");
        bp_points.add((float) (ALPHA + 0.5), activePower[ALPHA], "ALPHA\n8-13Hz");
        bp_points.add((float) (BETA + 0.5), activePower[BETA], "BETA\n13-32Hz");
        bp_points.add((float) (GAMMA + 0.5), activePower[GAMMA], "GAMMA\n32-100Hz");
        bp_plot.setPoints(bp_points);

        if (bpChanSelect.isVisible()) {
            lockElementsOnOverlapCheck(cp5ElementsToCheck);
        }
    }

    public void draw() {
        super.draw(); //calls the parent draw() method of Widget (DON'T REMOVE)
        pApplet.pushStyle();
        drawGradientBackground(x, y - 1, w, h + 1);
        pApplet.noStroke();
        pApplet.fill(COLOR_BG, 210);
        pApplet.rect(x, y - 1, w, h + 1);
        pApplet.stroke(COLOR_BORDER);
        pApplet.noFill();
        pApplet.rect(x, y - 1, w, h + 1);

        //remember to refer to x,y,w,h which are the positioning variables of the Widget class
        // Draw the third plot
        bp_plot.beginDraw();
        bp_plot.drawBackground();
        bp_plot.drawBox();
        bp_plot.drawXAxis();
        bp_plot.drawYAxis();
        bp_plot.drawGridLines(GPlot.HORIZONTAL);
        bp_plot.drawHistograms();
        bp_plot.endDraw();

        //for this widget need to redraw the top bar because the plot covers it up
        pApplet.noStroke();
        pApplet.fill(COLOR_CARD);
        pApplet.rect(x, y - navHeight, w, navHeight); //button bar
        pApplet.stroke(COLOR_BORDER);
        pApplet.line(x + 1, y - navHeight + 1, x + w - 1, y - navHeight + 1);

        pApplet.popStyle();
        bpChanSelect.draw();
    }

    public void screenResized() {
        super.screenResized(); //calls the parent screenResized() method of Widget (DON'T REMOVE)

        flexGPlotSizeAndPosition();

        bpChanSelect.screenResized(pApplet);
    }

    public void mousePressed() {
        super.mousePressed(); //calls the parent mousePressed() method of Widget (DON'T REMOVE)
        bpChanSelect.mousePressed(this.dropdownIsActive); //Calls channel select mousePressed and checks if clicked
    }

    void flexGPlotSizeAndPosition() {
        if (bpChanSelect.isVisible()) {
            bp_plot.setPos(x, y + bpChanSelect.getHeight() - navH);
            bp_plot.setOuterDim(w, h - bpChanSelect.getHeight() + navH);
        } else {
            bp_plot.setPos(x, y - navH);
            bp_plot.setOuterDim(w, h + navH);
        }
    }

    public float[] getNormalizedBPSelectedChannels() {
        return normalizedBandPowers;
    }

    //Called in DataProcessing.pde to update data even if widget is closed
    public void updateBandPowerWidgetData() {
        float normalizingSum = 0;

        for (int i = 0; i < NUM_BANDS; i++) {
            float sum = 0;

            for (int j = 0; j < bpChanSelect.activeChan.size(); j++) {
                int chan = bpChanSelect.activeChan.get(j);
                sum += dataProcessing.avgPowerInBins[chan][i];
            }

            activePower[i] = sum / bpChanSelect.activeChan.size();

            normalizingSum += activePower[i];
        }

        for (int i = 0; i < NUM_BANDS; i++) {
            normalizedBandPowers[i] = activePower[i] / normalizingSum;
        }
    }

    private void drawGradientBackground(int x0, int y0, int w0, int h0) {
        for (int i = 0; i < h0; i++) {
            float t = i / (float) Math.max(1, h0 - 1);
            int c = lerpRgb(COLOR_BG_TOP, COLOR_BG_BOTTOM, t);
            pApplet.stroke((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF);
            pApplet.line(x0, y0 + i, x0 + w0, y0 + i);
        }

        float glow = 0.5f + 0.5f * (float) Math.sin(MAIN.frameCount * 0.01f);
        pApplet.noStroke();
        pApplet.fill(79, 216, 255, (int) (16 * glow));
        pApplet.ellipse(x0 + w0 * 0.20f, y0 + h0 * 0.28f, w0 * 0.40f, h0 * 0.34f);
        pApplet.fill(103, 128, 255, (int) (12 * glow));
        pApplet.ellipse(x0 + w0 * 0.82f, y0 + h0 * 0.74f, w0 * 0.34f, h0 * 0.30f);
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
};
