package W_BandPower_;

////////////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                                    //
//    W_BandPowers.pde                                                                                //
//                                                                                                    //
//    This is a band power visualization widget!                                                      //
//    (Couldn't think up more)                                                                        //
//    This is for visualizing the power of each brainwave band: delta, theta, alpha, beta, gamma    //
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

import Globel.GUI;

import static Globel.GUI.*;
////////////////////////////////////////////////////////////////////////////////////////////////////////

public class W_BandPower extends Widget {
    GUI MAIN;

    private static final int COLOR_BG_TOP = 0xFF0A1220;
    private static final int COLOR_BG_BOTTOM = 0xFF0D1830;
    private static final int COLOR_BG = 0xFF0E1A2F;
    private static final int COLOR_PANEL = 0xFF142843;
    private static final int COLOR_CARD = 0xFF13243F;
    private static final int COLOR_BORDER = 0x6683A2CC;
    private static final int COLOR_TEXT = 0xFFEAF2FF;
    private static final int COLOR_TEXT_DIM = 0xFFA7B8D6;
    private static final int COLOR_GRID = 0x2D90AED8;
    private static final int COLOR_TRACK = 0x304D6B96;

    private static final String[] BAND_NAMES = {"Delta", "Theta", "Alpha", "Beta", "Gamma"};
    private static final String[] BAND_FREQ_LABELS = {"1-4 Hz", "4-8 Hz", "8-13 Hz", "13-30 Hz", "30-55 Hz"};
    private static final int[][] BAND_RGB = {
            {74, 176, 255},
            {77, 222, 209},
            {93, 225, 122},
            {255, 196, 89},
            {255, 118, 118}
    };

    private final int DELTA = 0;
    private final int THETA = 1;
    private final int ALPHA = 2;
    private final int BETA = 3;
    private final int GAMMA = 4;

    private final int NUM_BANDS = 5;
    private float[] activePower = new float[NUM_BANDS];
    private float[] normalizedBandPowers = new float[NUM_BANDS];
    private float totalActivePower = 0f;
    private int dominantBandIndex = 0;

    private GPlot bp_plot;
    public ChannelSelect bpChanSelect;
    private boolean prevChanSelectIsVisible = false;

    private List<Controller> cp5ElementsToCheck = new ArrayList<Controller>();

    public W_BandPower(GUI MAIN) {
        super(MAIN);
        this.MAIN = MAIN;

        bpChanSelect = new ChannelSelect(pApplet, this, x, y, w, navH, "BP_Channels");
        bpChanSelect.activateAllButtons();
        cp5ElementsToCheck.addAll(bpChanSelect.getCp5ElementsForOverlapCheck());

        addDropdown("Smoothing", "平滑度", Arrays.asList(MAIN.settings.fftSmoothingArray), MAIN.smoothFac_ind);
        addDropdown("UnfiltFilt", "滤波", Arrays.asList(MAIN.settings.fftFilterArray), MAIN.settings.fftFilterSave);

        bp_plot = new GPlot(MAIN, x, y - navHeight, w, h + navHeight);
        bp_plot.setDim(w, h);
        bp_plot.setLogScale("y");
        bp_plot.setYLim(0.1F, 100);
        bp_plot.setXLim(0, 5);
        bp_plot.getYAxis().setNTicks(9);
        bp_plot.getXAxis().setNTicks(0);
        bp_plot.getTitle().setTextAlignment(MAIN.LEFT);
        bp_plot.getTitle().setRelativePos(0);
        bp_plot.setAllFontProperties("Microsoft YaHei", 0, 14);
        bp_plot.getYAxis().getAxisLabel().setText("功率积分 (uV)^2 / Hz");
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
        bp_plot.getHistogram().setLineColors(new int[]{
                pApplet.color(215, 232, 255), pApplet.color(215, 232, 255), pApplet.color(215, 232, 255),
                pApplet.color(215, 232, 255), pApplet.color(215, 232, 255)
        });
        bp_plot.getHistogram().setBgColors(new int[]{
                pApplet.color(BAND_RGB[DELTA][0], BAND_RGB[DELTA][1], BAND_RGB[DELTA][2], 215),
                pApplet.color(BAND_RGB[THETA][0], BAND_RGB[THETA][1], BAND_RGB[THETA][2], 215),
                pApplet.color(BAND_RGB[ALPHA][0], BAND_RGB[ALPHA][1], BAND_RGB[ALPHA][2], 215),
                pApplet.color(BAND_RGB[BETA][0], BAND_RGB[BETA][1], BAND_RGB[BETA][2], 215),
                pApplet.color(BAND_RGB[GAMMA][0], BAND_RGB[GAMMA][1], BAND_RGB[GAMMA][2], 215)
        });
        bp_plot.getHistogram().setFontColor(COLOR_TEXT);

        flexGPlotSizeAndPosition();
    }

    public void update() {
        super.update();

        bpChanSelect.update(x, y, w);

        if (bpChanSelect.isVisible() != prevChanSelectIsVisible) {
            flexGPlotSizeAndPosition();
            prevChanSelectIsVisible = bpChanSelect.isVisible();
        }

        GPointsArray bp_points = new GPointsArray(dataProcessing.headWidePower.length);
        bp_points.add((float) (DELTA + 0.5), activePower[DELTA], "DELTA\n1-4 Hz");
        bp_points.add((float) (THETA + 0.5), activePower[THETA], "THETA\n4-8 Hz");
        bp_points.add((float) (ALPHA + 0.5), activePower[ALPHA], "ALPHA\n8-13 Hz");
        bp_points.add((float) (BETA + 0.5), activePower[BETA], "BETA\n13-30 Hz");
        bp_points.add((float) (GAMMA + 0.5), activePower[GAMMA], "GAMMA\n30-55 Hz");
        bp_plot.setPoints(bp_points);

        if (bpChanSelect.isVisible()) {
            lockElementsOnOverlapCheck(cp5ElementsToCheck);
        }
    }

    public void draw() {
        super.draw();
        pApplet.pushStyle();
        drawGradientBackground(x, y - 1, w, h + 1);
        pApplet.noStroke();
        pApplet.fill(COLOR_BG, 210);
        pApplet.rect(x, y - 1, w, h + 1);
        pApplet.stroke(COLOR_BORDER);
        pApplet.noFill();
        pApplet.rect(x, y - 1, w, h + 1);

        bp_plot.beginDraw();
        bp_plot.drawBackground();
        bp_plot.drawBox();
        bp_plot.drawXAxis();
        bp_plot.drawYAxis();
        bp_plot.drawGridLines(GPlot.HORIZONTAL);
        bp_plot.drawHistograms();
        bp_plot.endDraw();

        drawInfoPanel();

        pApplet.noStroke();
        pApplet.fill(COLOR_CARD);
        pApplet.rect(x, y - navHeight, w, navHeight);
        pApplet.stroke(COLOR_BORDER);
        pApplet.line(x + 1, y - navHeight + 1, x + w - 1, y - navHeight + 1);

        pApplet.popStyle();
        bpChanSelect.draw();
    }

    public void screenResized() {
        super.screenResized();
        flexGPlotSizeAndPosition();
        bpChanSelect.screenResized(pApplet);
    }

    public void mousePressed() {
        super.mousePressed();
        bpChanSelect.mousePressed(this.dropdownIsActive);
    }

    void flexGPlotSizeAndPosition() {
        int sidebarW = getSidebarWidth();
        int plotW = Math.max(220, w - sidebarW - 18);
        if (bpChanSelect.isVisible()) {
            bp_plot.setPos(x, y + bpChanSelect.getHeight() - navH);
            bp_plot.setOuterDim(plotW, h - bpChanSelect.getHeight() + navH);
        } else {
            bp_plot.setPos(x, y - navH);
            bp_plot.setOuterDim(plotW, h + navH);
        }
    }

    public float[] getNormalizedBPSelectedChannels() {
        return normalizedBandPowers;
    }

    public void updateBandPowerWidgetData() {
        int activeChanCount = bpChanSelect.activeChan.size();
        if (activeChanCount <= 0) {
            totalActivePower = 0f;
            dominantBandIndex = 0;
            for (int i = 0; i < NUM_BANDS; i++) {
                activePower[i] = 0f;
                normalizedBandPowers[i] = 0f;
            }
            return;
        }

        float normalizingSum = 0f;
        float maxPower = -1f;
        int maxIndex = 0;

        for (int i = 0; i < NUM_BANDS; i++) {
            float sum = 0f;
            for (int j = 0; j < activeChanCount; j++) {
                int chan = bpChanSelect.activeChan.get(j);
                sum += dataProcessing.avgPowerInBins[chan][i];
            }

            activePower[i] = sum / activeChanCount;
            normalizingSum += activePower[i];
            if (activePower[i] > maxPower) {
                maxPower = activePower[i];
                maxIndex = i;
            }
        }

        totalActivePower = normalizingSum;
        dominantBandIndex = maxIndex;

        if (normalizingSum <= 1.0e-9f) {
            for (int i = 0; i < NUM_BANDS; i++) {
                normalizedBandPowers[i] = 0f;
            }
            return;
        }

        for (int i = 0; i < NUM_BANDS; i++) {
            normalizedBandPowers[i] = activePower[i] / normalizingSum;
        }
    }

    private void drawInfoPanel() {
        int sidebarW = getSidebarWidth();
        int panelX = x + w - sidebarW - 10;
        int panelY = bpChanSelect.isVisible() ? y + bpChanSelect.getHeight() + 8 : y + 10;
        int panelH = h - (bpChanSelect.isVisible() ? bpChanSelect.getHeight() : 0) - 18;
        panelH = Math.max(180, panelH);
        int headerH = 74;
        int barTop = panelY + headerH + 14;
        int rowGap = 34;

        pApplet.noStroke();
        pApplet.fill(COLOR_CARD, 222);
        pApplet.rect(panelX, panelY, sidebarW, panelH, 10);
        pApplet.stroke(COLOR_BORDER);
        pApplet.noFill();
        pApplet.rect(panelX, panelY, sidebarW, panelH, 10);

        pApplet.textFont(p7);
        pApplet.textAlign(PApplet.LEFT, PApplet.TOP);
        pApplet.fill(COLOR_TEXT);
        pApplet.textSize(13);
        pApplet.text("频段概览", panelX + 14, panelY + 12);

        pApplet.textFont(p7);
        pApplet.textSize(11);
        pApplet.fill(COLOR_TEXT_DIM);
        pApplet.text("补充显示各频段占比与当前主导节律", panelX + 14, panelY + 34);

        drawMetricChip(panelX + 14, panelY + 54, 72, 28, "通道数", String.valueOf(bpChanSelect.activeChan.size()));
        drawMetricChip(panelX + 92, panelY + 54, 92, 28, "主导", BAND_NAMES[dominantBandIndex]);
        drawMetricChip(panelX + 190, panelY + 54, sidebarW - 204, 28, "总功率", formatCompact(totalActivePower));

        for (int i = 0; i < NUM_BANDS; i++) {
            int rowY = barTop + i * rowGap;
            float pct = normalizedBandPowers[i];
            int[] rgb = BAND_RGB[i];
            int trackX = panelX + 74;
            int trackW = Math.max(44, sidebarW - 144);
            int fillW = Math.max(0, Math.round(trackW * pct));

            pApplet.noStroke();
            pApplet.fill(rgb[0], rgb[1], rgb[2], 220);
            pApplet.ellipse(panelX + 18, rowY + 10, 9, 9);

            pApplet.fill(COLOR_TEXT);
            pApplet.textFont(p7);
            pApplet.textAlign(PApplet.LEFT, PApplet.TOP);
            pApplet.textSize(11);
            pApplet.text(BAND_NAMES[i], panelX + 30, rowY + 1);

            pApplet.fill(COLOR_TEXT_DIM);
            pApplet.textSize(10);
            pApplet.text(BAND_FREQ_LABELS[i], panelX + 30, rowY + 14);

            pApplet.noStroke();
            pApplet.fill(COLOR_TRACK);
            pApplet.rect(trackX, rowY + 6, trackW, 9, 5);

            if (fillW > 0) {
                pApplet.fill(rgb[0], rgb[1], rgb[2], 230);
                pApplet.rect(trackX, rowY + 6, fillW, 9, 5);
            }

            pApplet.fill(COLOR_TEXT);
            pApplet.textAlign(PApplet.RIGHT, PApplet.TOP);
            pApplet.textSize(11);
            pApplet.text(PApplet.nf(pct * 100f, 1, 1) + "%", panelX + sidebarW - 14, rowY + 1);
            pApplet.textSize(10);
            pApplet.fill(COLOR_TEXT_DIM);
            pApplet.text(formatCompact(activePower[i]), panelX + sidebarW - 14, rowY + 14);
        }
    }

    private void drawMetricChip(int x0, int y0, int w0, int h0, String label, String value) {
        if (w0 <= 8) {
            return;
        }
        pApplet.noStroke();
        pApplet.fill(COLOR_PANEL, 240);
        pApplet.rect(x0, y0, w0, h0, 7);
        pApplet.stroke(COLOR_BORDER);
        pApplet.noFill();
        pApplet.rect(x0, y0, w0, h0, 7);

        pApplet.textAlign(PApplet.LEFT, PApplet.TOP);
        pApplet.textFont(p7);
        pApplet.textSize(10);
        pApplet.fill(COLOR_TEXT_DIM);
        pApplet.text(label, x0 + 8, y0 + 4);

        pApplet.textFont(p7);
        pApplet.textSize(11);
        pApplet.fill(COLOR_TEXT);
        pApplet.text(value, x0 + 8, y0 + 15);
    }

    private int getSidebarWidth() {
        return PApplet.constrain((int) (w * 0.32f), 190, 250);
    }

    private String formatCompact(float value) {
        if (!Float.isFinite(value)) {
            return "0";
        }
        if (value >= 100f) {
            return PApplet.nf(value, 1, 0);
        }
        if (value >= 10f) {
            return PApplet.nf(value, 1, 1);
        }
        if (value >= 1f) {
            return PApplet.nf(value, 1, 2);
        }
        return PApplet.nf(value, 1, 3);
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
}
