package W_CFC_;

import Globel.GUI;
import Widget_.Widget;
import processing.core.PApplet;

import java.util.Arrays;

import static Globel.GUI.*;

public class W_CFC extends Widget {
    private static final int[] WINDOW_SECONDS = {2, 4, 6};
    private static final int[] UPDATE_INTERVAL_MS = {200, 400, 700};

    private static final String[] WINDOW_LABELS = {"2秒", "4秒", "6秒"};
    private static final String[] UPDATE_LABELS = {"快速 5Hz", "标准 2.5Hz", "平稳 1.4Hz"};
    private static final String[] PRESET_LABELS = {"Theta-Gamma 耦合", "Alpha-Gamma 耦合", "全频扫描"};

    private static final int PHASE_BINS = 18;
    private static final int TREND_POINTS = 180;
    private static final int SURROGATE_COUNT = 16;

    private static final float GRID_SMOOTH = 0.20f;

    private static final int BG_TOP = 0xFF0A1220;
    private static final int BG_BOTTOM = 0xFF0D1830;
    private static final int PANEL = 0xCC13243F;
    private static final int PANEL_STROKE = 0x6683A2CC;
    private static final int TEXT_MAIN = 0xFFEAF2FF;
    private static final int TEXT_SUB = 0xFF98AECE;
    private static final int GRID_LINE = 0x2D90AED8;
    private static final int ACCENT = 0xFF4FD8FF;
    private static final int ACCENT_SOFT = 0x4460E2FF;

    private final GUI MAIN;

    private String[] channelLabels;

    private int phaseChanIndex = 0;
    private int ampChanIndex = 0;
    private int windowIndex = 1;
    private int updateRateIndex = 1;
    private int presetIndex = 0;

    private int[] phaseFreqs = new int[0];
    private int[] ampFreqs = new int[0];

    private float[][] miRaw = new float[0][0];
    private float[][] miDisplay = new float[0][0];

    private float[] tempSignalPhase = new float[0];
    private float[] tempSignalAmp = new float[0];
    private float[][] phaseSeries = new float[0][0];
    private float[][] ampSeries = new float[0][0];

    private final float[] roseBins = new float[PHASE_BINS];
    private final float[] trend = new float[TREND_POINTS];
    private int trendWrite = 0;
    private boolean trendFilled = false;

    private float peakMI = 0f;
    private float peakPhaseHz = 0f;
    private float peakAmpHz = 0f;
    private float zScore = 0f;
    private float pValue = 1f;
    private float miScaleMax = 0.08f;

    private long lastComputeMs = 0L;
    private int fsCache = -1;
    private int sampleCountCache = -1;
    private int presetCache = -1;

    public W_CFC(GUI MAIN) {
        super(MAIN);
        this.MAIN = MAIN;

        channelLabels = buildChannelLabels();
        addDropdown("CFCPhaseChan", "相位通道", Arrays.asList(channelLabels), 0);
        addDropdown("CFCAmpChan", "振幅通道", Arrays.asList(channelLabels), Math.min(1, channelLabels.length - 1));
        addDropdown("CFCWindow", "窗长", Arrays.asList(WINDOW_LABELS), windowIndex);
        addDropdown("CFCRate", "刷新", Arrays.asList(UPDATE_LABELS), updateRateIndex);
        addDropdown("CFCPreset", "频段", Arrays.asList(PRESET_LABELS), presetIndex);

        rebuildFrequencyGrid();
    }

    public void CFCPhaseChan(int n) {
        phaseChanIndex = PApplet.constrain(n, 0, Math.max(0, nchan - 1));
    }

    public void CFCAmpChan(int n) {
        ampChanIndex = PApplet.constrain(n, 0, Math.max(0, nchan - 1));
    }

    public void CFCWindow(int n) {
        windowIndex = PApplet.constrain(n, 0, WINDOW_SECONDS.length - 1);
    }

    public void CFCRate(int n) {
        updateRateIndex = PApplet.constrain(n, 0, UPDATE_INTERVAL_MS.length - 1);
    }

    public void CFCPreset(int n) {
        presetIndex = PApplet.constrain(n, 0, PRESET_LABELS.length - 1);
        rebuildFrequencyGrid();
    }

    @Override
    public void update() {
        super.update();

        int sampleRate = MAIN.currentBoard.getSampleRate();
        int windowSamples = Math.max(64, sampleRate * WINDOW_SECONDS[windowIndex]);
        if (sampleRate != fsCache || sampleCountCache != windowSamples || presetIndex != presetCache) {
            rebuildFrequencyGrid();
            fsCache = sampleRate;
            sampleCountCache = windowSamples;
            presetCache = presetIndex;
        }

        long now = MAIN.millis();
        if (now - lastComputeMs < UPDATE_INTERVAL_MS[updateRateIndex]) {
            return;
        }
        lastComputeMs = now;

        computePAC(windowSamples, sampleRate);
    }

    @Override
    public void draw() {
        super.draw();

        MAIN.pushStyle();
        drawGradientBackground(x, y, w, h);

        int pad = 10;
        int panelX = x + pad;
        int panelY = y + pad;
        int panelW = w - 2 * pad;
        int panelH = h - 2 * pad;

        MAIN.noStroke();
        MAIN.fill(PANEL);
        MAIN.rect(panelX, panelY, panelW, panelH, 4);
        MAIN.stroke(PANEL_STROKE);
        MAIN.strokeWeight(1.1f);
        MAIN.noFill();
        MAIN.rect(panelX, panelY, panelW, panelH, 4);

        drawHeader(panelX, panelY, panelW);

        int contentY = panelY + 50;
        int contentH = panelH - 60;
        int leftW = (int) (panelW * 0.66f);
        int rightW = panelW - leftW - 10;

        drawComodulogram(panelX + 10, contentY, leftW - 20, contentH - 10);
        drawRightPanel(panelX + leftW + 2, contentY, rightW - 10, contentH - 10);

        MAIN.popStyle();
    }

    private void drawHeader(int panelX, int panelY, int panelW) {
        MAIN.fill(TEXT_MAIN);
        MAIN.textFont(p7);
        MAIN.textSize(15);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text("脑电跨频耦合图谱", panelX + 12, panelY + 9);

        MAIN.fill(TEXT_SUB);
        MAIN.textSize(11);
        MAIN.text(
                "相位-振幅耦合（PAC） | 相位通道 " + (phaseChanIndex + 1)
                        + " | 振幅通道 " + (ampChanIndex + 1)
                        + " | 窗长 " + WINDOW_LABELS[windowIndex],
                panelX + 12, panelY + 29
        );

        MAIN.stroke(PANEL_STROKE);
        MAIN.line(panelX + 10, panelY + 44, panelX + panelW - 10, panelY + 44);
    }

    private void drawComodulogram(int rx, int ry, int rw, int rh) {
        int titleH = 18;
        int axisL = 44;
        int axisB = 32;
        int axisT = 18;
        int axisR = 16;

        MAIN.fill(0x331A2C48);
        MAIN.noStroke();
        MAIN.rect(rx, ry, rw, rh, 3);
        MAIN.stroke(PANEL_STROKE);
        MAIN.noFill();
        MAIN.rect(rx, ry, rw, rh, 3);

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7);
        MAIN.textSize(11);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text("PAC 热力图（Comodulogram）", rx + 8, ry + 4);

        int gx = rx + axisL;
        int gy = ry + axisT + titleH;
        int gw = Math.max(40, rw - axisL - axisR);
        int gh = Math.max(40, rh - axisT - axisB - titleH);

        drawHeatGrid(gx, gy, gw, gh);
        drawHeatAxes(gx, gy, gw, gh);
    }

    private void drawHeatGrid(int gx, int gy, int gw, int gh) {
        if (phaseFreqs.length == 0 || ampFreqs.length == 0) {
            return;
        }
        float cellW = gw / (float) phaseFreqs.length;
        float cellH = gh / (float) ampFreqs.length;

        miScaleMax += (Math.max(0.02f, peakMI * 1.35f) - miScaleMax) * 0.08f;

        int peakPi = 0;
        int peakAi = 0;
        float localPeak = -1f;
        for (int pi = 0; pi < phaseFreqs.length; pi++) {
            for (int ai = 0; ai < ampFreqs.length; ai++) {
                if (miDisplay[pi][ai] > localPeak) {
                    localPeak = miDisplay[pi][ai];
                    peakPi = pi;
                    peakAi = ai;
                }
            }
        }

        MAIN.noStroke();
        for (int pi = 0; pi < phaseFreqs.length; pi++) {
            for (int ai = 0; ai < ampFreqs.length; ai++) {
                float v = miDisplay[pi][ai];
                float t = PApplet.constrain(v / Math.max(1e-4f, miScaleMax), 0f, 1f);
                int col = pacColor(t);
                int x0 = (int) (gx + pi * cellW);
                int y0 = (int) (gy + gh - (ai + 1) * cellH);
                MAIN.fill((col >> 16) & 0xFF, (col >> 8) & 0xFF, col & 0xFF, 228);
                MAIN.rect(x0 + 1, y0 + 1, Math.max(1, (int) cellW - 1), Math.max(1, (int) cellH - 1));
            }
        }

        float pulse = 0.7f + 0.3f * (float) Math.sin(MAIN.frameCount * 0.08f);
        MAIN.noFill();
        MAIN.stroke(79, 216, 255, (int) (190 * pulse));
        MAIN.strokeWeight(2f);
        float px = gx + peakPi * cellW;
        float py = gy + gh - (peakAi + 1) * cellH;
        MAIN.rect(px + 0.5f, py + 0.5f, Math.max(2f, cellW - 1f), Math.max(2f, cellH - 1f), 2);

        MAIN.stroke(GRID_LINE);
        MAIN.strokeWeight(1f);
        for (int i = 0; i <= phaseFreqs.length; i++) {
            float xx = gx + i * cellW;
            MAIN.line(xx, gy, xx, gy + gh);
        }
        for (int i = 0; i <= ampFreqs.length; i++) {
            float yy = gy + i * cellH;
            MAIN.line(gx, yy, gx + gw, yy);
        }
    }

    private void drawHeatAxes(int gx, int gy, int gw, int gh) {
        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7);
        MAIN.textSize(10);

        MAIN.stroke(PANEL_STROKE);
        MAIN.line(gx, gy + gh, gx + gw, gy + gh);
        MAIN.line(gx, gy, gx, gy + gh);

        MAIN.textAlign(PApplet.CENTER, PApplet.TOP);
        int xTicks = Math.min(phaseFreqs.length, 6);
        for (int i = 0; i < xTicks; i++) {
            int idx = Math.round(i * (phaseFreqs.length - 1f) / Math.max(1, xTicks - 1));
            float xx = gx + (idx + 0.5f) * (gw / (float) phaseFreqs.length);
            MAIN.line(xx, gy + gh, xx, gy + gh + 4);
            MAIN.text(phaseFreqs[idx] + "Hz", xx, gy + gh + 6);
        }
        MAIN.text("低频相位频率", gx + gw * 0.5f, gy + gh + 18);

        MAIN.textAlign(PApplet.RIGHT, PApplet.CENTER);
        int yTicks = Math.min(ampFreqs.length, 6);
        for (int i = 0; i < yTicks; i++) {
            int idx = Math.round(i * (ampFreqs.length - 1f) / Math.max(1, yTicks - 1));
            float yy = gy + gh - (idx + 0.5f) * (gh / (float) ampFreqs.length);
            MAIN.line(gx - 4, yy, gx, yy);
            MAIN.text(ampFreqs[idx] + "Hz", gx - 6, yy);
        }

        MAIN.pushMatrix();
        MAIN.translate(gx - 34, gy + gh * 0.5f);
        MAIN.rotate(-PApplet.HALF_PI);
        MAIN.textAlign(PApplet.CENTER, PApplet.TOP);
        MAIN.text("高频振幅频率", 0, 0);
        MAIN.popMatrix();
    }

    private void drawRightPanel(int rx, int ry, int rw, int rh) {
        int cardGap = 8;
        int topH = 94;
        int midH = (int) (rh * 0.42f);
        int botH = rh - topH - midH - cardGap * 2;

        drawTopInfoCard(rx, ry, rw, topH);
        drawRoseCard(rx, ry + topH + cardGap, rw, midH);
        drawTrendCard(rx, ry + topH + midH + cardGap * 2, rw, botH);
    }

    private void drawTopInfoCard(int x0, int y0, int w0, int h0) {
        MAIN.noStroke();
        MAIN.fill(0x2A163153);
        MAIN.rect(x0, y0, w0, h0, 3);
        MAIN.stroke(PANEL_STROKE);
        MAIN.noFill();
        MAIN.rect(x0, y0, w0, h0, 3);

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7);
        MAIN.textSize(11);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text("峰值耦合信息", x0 + 8, y0 + 6);

        MAIN.fill(TEXT_MAIN);
        MAIN.textSize(15);
        MAIN.text("相位 " + PApplet.nf(peakPhaseHz, 1, 1) + " Hz", x0 + 10, y0 + 24);
        MAIN.text("振幅 " + PApplet.nf(peakAmpHz, 1, 1) + " Hz", x0 + 10, y0 + 44);

        MAIN.fill(ACCENT);
        MAIN.textSize(16);
        MAIN.text("MI = " + PApplet.nf(peakMI, 1, 4), x0 + w0 - 138, y0 + 24);

        MAIN.fill(TEXT_SUB);
        MAIN.textSize(11);
        MAIN.text("显著性 Z = " + PApplet.nf(zScore, 1, 2), x0 + w0 - 138, y0 + 48);
        MAIN.text("经验 P = " + PApplet.nf(pValue, 1, 3), x0 + w0 - 138, y0 + 64);
    }

    private void drawRoseCard(int x0, int y0, int w0, int h0) {
        MAIN.noStroke();
        MAIN.fill(0x2A163153);
        MAIN.rect(x0, y0, w0, h0, 3);
        MAIN.stroke(PANEL_STROKE);
        MAIN.noFill();
        MAIN.rect(x0, y0, w0, h0, 3);

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7);
        MAIN.textSize(11);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text("相位分箱振幅图", x0 + 8, y0 + 6);

        float cx = x0 + w0 * 0.5f;
        float cy = y0 + h0 * 0.58f;
        float rMax = Math.min(w0, h0) * 0.33f;

        MAIN.stroke(GRID_LINE);
        MAIN.noFill();
        MAIN.ellipse(cx, cy, rMax * 2f, rMax * 2f);
        MAIN.ellipse(cx, cy, rMax * 1.4f, rMax * 1.4f);
        MAIN.ellipse(cx, cy, rMax * 0.8f, rMax * 0.8f);

        float maxVal = 0f;
        for (int i = 0; i < PHASE_BINS; i++) {
            maxVal = Math.max(maxVal, roseBins[i]);
        }
        if (maxVal <= 1e-7f) {
            maxVal = 1f;
        }

        MAIN.noStroke();
        MAIN.fill(79, 216, 255, 160);
        MAIN.beginShape();
        for (int i = 0; i < PHASE_BINS; i++) {
            float theta = PApplet.TWO_PI * i / PHASE_BINS - PApplet.HALF_PI;
            float rr = rMax * PApplet.constrain(roseBins[i] / maxVal, 0.08f, 1f);
            float px = cx + rr * PApplet.cos(theta);
            float py = cy + rr * PApplet.sin(theta);
            MAIN.vertex(px, py);
        }
        MAIN.endShape(PApplet.CLOSE);

        MAIN.stroke(ACCENT_SOFT);
        MAIN.strokeWeight(1.2f);
        for (int i = 0; i < PHASE_BINS; i += 3) {
            float theta = PApplet.TWO_PI * i / PHASE_BINS - PApplet.HALF_PI;
            MAIN.line(cx, cy, cx + rMax * PApplet.cos(theta), cy + rMax * PApplet.sin(theta));
        }

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7);
        MAIN.textSize(10);
        MAIN.textAlign(PApplet.CENTER, PApplet.CENTER);
        MAIN.text("0°", cx, cy - rMax - 10);
        MAIN.text("180°", cx, cy + rMax + 10);
        MAIN.text("-90°", cx - rMax - 15, cy);
        MAIN.text("+90°", cx + rMax + 15, cy);
    }

    private void drawTrendCard(int x0, int y0, int w0, int h0) {
        MAIN.noStroke();
        MAIN.fill(0x2A163153);
        MAIN.rect(x0, y0, w0, h0, 3);
        MAIN.stroke(PANEL_STROKE);
        MAIN.noFill();
        MAIN.rect(x0, y0, w0, h0, 3);

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7);
        MAIN.textSize(11);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text("耦合强度趋势", x0 + 8, y0 + 6);

        int gx = x0 + 10;
        int gy = y0 + 24;
        int gw = w0 - 18;
        int gh = h0 - 34;

        MAIN.stroke(GRID_LINE);
        for (int i = 0; i <= 4; i++) {
            float yy = gy + gh * i / 4f;
            MAIN.line(gx, yy, gx + gw, yy);
        }

        MAIN.stroke(PANEL_STROKE);
        MAIN.line(gx, gy + gh, gx + gw, gy + gh);
        MAIN.line(gx, gy, gx, gy + gh);

        int count = trendFilled ? TREND_POINTS : trendWrite;
        if (count < 2) {
            MAIN.fill(TEXT_SUB);
            MAIN.textFont(p7);
            MAIN.textSize(10);
            MAIN.text("等待稳定数据...", gx + 4, gy + 4);
            return;
        }

        float maxV = 0.05f;
        for (int i = 0; i < count; i++) {
            maxV = Math.max(maxV, getTrendValue(i, count));
        }

        MAIN.noFill();
        MAIN.stroke(79, 216, 255, 230);
        MAIN.strokeWeight(2f);
        MAIN.beginShape();
        for (int i = 0; i < count; i++) {
            float v = getTrendValue(i, count);
            float xx = gx + gw * i / (float) (count - 1);
            float yy = gy + gh - gh * PApplet.constrain(v / maxV, 0f, 1f);
            MAIN.vertex(xx, yy);
        }
        MAIN.endShape();

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7);
        MAIN.textSize(10);
        MAIN.textAlign(PApplet.RIGHT, PApplet.TOP);
        MAIN.text("最大 " + PApplet.nf(maxV, 1, 4), gx + gw - 2, gy + 2);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text("当前 " + PApplet.nf(peakMI, 1, 4), gx + 2, gy + 2);
    }

    private void computePAC(int windowSamples, int fs) {
        if (phaseFreqs.length == 0 || ampFreqs.length == 0) {
            return;
        }
        if (dataProcessingFilteredBuffer == null || dataProcessingFilteredBuffer.length == 0) {
            return;
        }

        phaseChanIndex = PApplet.constrain(phaseChanIndex, 0, Math.max(0, nchan - 1));
        ampChanIndex = PApplet.constrain(ampChanIndex, 0, Math.max(0, nchan - 1));

        float[] srcPhase = dataProcessingFilteredBuffer[phaseChanIndex];
        float[] srcAmp = dataProcessingFilteredBuffer[ampChanIndex];
        if (srcPhase == null || srcAmp == null) {
            return;
        }
        if (srcPhase.length < windowSamples || srcAmp.length < windowSamples) {
            return;
        }

        ensureComputeBuffers(windowSamples);
        copyTail(srcPhase, tempSignalPhase, windowSamples);
        copyTail(srcAmp, tempSignalAmp, windowSamples);
        demean(tempSignalPhase);
        demean(tempSignalAmp);

        for (int p = 0; p < phaseFreqs.length; p++) {
            computePhaseSeries(tempSignalPhase, fs, phaseFreqs[p], 2.0f, phaseSeries[p]);
        }
        for (int a = 0; a < ampFreqs.length; a++) {
            computeAmplitudeEnvelope(tempSignalAmp, fs, ampFreqs[a], 10.0f, ampSeries[a]);
        }

        float best = -1f;
        int bestP = 0;
        int bestA = 0;

        for (int p = 0; p < phaseFreqs.length; p++) {
            for (int a = 0; a < ampFreqs.length; a++) {
                float mi = computeMI(phaseSeries[p], ampSeries[a], null, 0);
                miRaw[p][a] = mi;
                miDisplay[p][a] += (mi - miDisplay[p][a]) * GRID_SMOOTH;
                if (miRaw[p][a] > best) {
                    best = miRaw[p][a];
                    bestP = p;
                    bestA = a;
                }
            }
        }

        peakMI = Math.max(0f, best);
        peakPhaseHz = phaseFreqs[bestP];
        peakAmpHz = ampFreqs[bestA];

        Arrays.fill(roseBins, 0f);
        computeMI(phaseSeries[bestP], ampSeries[bestA], roseBins, 0);

        computeSurrogateStats(phaseSeries[bestP], ampSeries[bestA]);
        pushTrend(peakMI);
    }

    private void computeSurrogateStats(float[] phase, float[] amp) {
        int n = phase.length;
        float[] surrogate = new float[SURROGATE_COUNT];

        int geCount = 0;
        for (int s = 0; s < SURROGATE_COUNT; s++) {
            int shift = (int) (((long) (s + 1) * n) / (SURROGATE_COUNT + 1L));
            float mi = computeMI(phase, amp, null, shift);
            surrogate[s] = mi;
            if (mi >= peakMI) {
                geCount++;
            }
        }

        float mean = 0f;
        for (float v : surrogate) {
            mean += v;
        }
        mean /= SURROGATE_COUNT;

        float var = 0f;
        for (float v : surrogate) {
            float d = v - mean;
            var += d * d;
        }
        var /= Math.max(1, SURROGATE_COUNT - 1);
        float sd = (float) Math.sqrt(Math.max(1e-10, var));

        zScore = (peakMI - mean) / sd;
        pValue = (geCount + 1f) / (SURROGATE_COUNT + 1f);
    }

    private float computeMI(float[] phase, float[] amp, float[] outRose, int shift) {
        int n = phase.length;
        float[] sum = new float[PHASE_BINS];
        int[] cnt = new int[PHASE_BINS];

        int start = Math.max(1, (int) (n * 0.05f));
        for (int i = start; i < n; i++) {
            int ai = i + shift;
            while (ai >= n) {
                ai -= n;
            }
            while (ai < 0) {
                ai += n;
            }

            float ph = phase[i];
            int bin = (int) ((ph + PApplet.PI) * PHASE_BINS / PApplet.TWO_PI);
            if (bin < 0) {
                bin = 0;
            } else if (bin >= PHASE_BINS) {
                bin = PHASE_BINS - 1;
            }

            float av = Math.max(0f, amp[ai]);
            sum[bin] += av;
            cnt[bin]++;
        }

        float[] meanAmp = new float[PHASE_BINS];
        float total = 0f;
        for (int b = 0; b < PHASE_BINS; b++) {
            meanAmp[b] = cnt[b] > 0 ? (sum[b] / cnt[b]) : 0f;
            total += meanAmp[b];
        }
        if (total <= 1e-12f) {
            return 0f;
        }

        float h = 0f;
        for (int b = 0; b < PHASE_BINS; b++) {
            float p = meanAmp[b] / total;
            if (p > 1e-12f) {
                h -= p * Math.log(p);
            }
        }

        float mi = (float) ((Math.log(PHASE_BINS) - h) / Math.log(PHASE_BINS));
        mi = PApplet.constrain(mi, 0f, 1f);

        if (outRose != null && outRose.length >= PHASE_BINS) {
            System.arraycopy(meanAmp, 0, outRose, 0, PHASE_BINS);
        }
        return mi;
    }

    private void computePhaseSeries(float[] x, int fs, float f0, float lpfCutHz, float[] outPhase) {
        float dt = 1f / Math.max(1f, fs);
        float w = PApplet.TWO_PI * f0;
        float a = lowpassAlpha(fs, lpfCutHz);

        float iAcc = 0f;
        float qAcc = 0f;
        for (int n = 0; n < x.length; n++) {
            float t = n * dt;
            float c = (float) Math.cos(w * t);
            float s = (float) Math.sin(w * t);

            float iMix = x[n] * c;
            float qMix = x[n] * s;
            iAcc += a * (iMix - iAcc);
            qAcc += a * (qMix - qAcc);

            outPhase[n] = (float) Math.atan2(qAcc, iAcc);
        }
    }

    private void computeAmplitudeEnvelope(float[] x, int fs, float f0, float lpfCutHz, float[] outAmp) {
        float dt = 1f / Math.max(1f, fs);
        float w = PApplet.TWO_PI * f0;
        float a = lowpassAlpha(fs, lpfCutHz);

        float iAcc = 0f;
        float qAcc = 0f;
        for (int n = 0; n < x.length; n++) {
            float t = n * dt;
            float c = (float) Math.cos(w * t);
            float s = (float) Math.sin(w * t);

            float iMix = x[n] * c;
            float qMix = x[n] * s;
            iAcc += a * (iMix - iAcc);
            qAcc += a * (qMix - qAcc);

            outAmp[n] = (float) Math.sqrt(iAcc * iAcc + qAcc * qAcc);
        }
    }

    private float lowpassAlpha(int fs, float cutoffHz) {
        float dt = 1f / Math.max(1f, fs);
        float rc = 1f / (PApplet.TWO_PI * Math.max(0.1f, cutoffHz));
        return PApplet.constrain(dt / (rc + dt), 0.001f, 0.999f);
    }

    private void rebuildFrequencyGrid() {
        switch (presetIndex) {
            case 0:
                phaseFreqs = range(4, 8, 1);
                ampFreqs = range(30, 80, 4);
                break;
            case 1:
                phaseFreqs = range(8, 13, 1);
                ampFreqs = range(30, 90, 4);
                break;
            default:
                phaseFreqs = range(2, 14, 1);
                ampFreqs = range(30, 90, 4);
                break;
        }

        miRaw = new float[phaseFreqs.length][ampFreqs.length];
        miDisplay = new float[phaseFreqs.length][ampFreqs.length];
        phaseSeries = new float[phaseFreqs.length][];
        ampSeries = new float[ampFreqs.length][];
    }

    private void ensureComputeBuffers(int n) {
        if (tempSignalPhase.length != n) {
            tempSignalPhase = new float[n];
            tempSignalAmp = new float[n];
            for (int i = 0; i < phaseSeries.length; i++) {
                phaseSeries[i] = new float[n];
            }
            for (int i = 0; i < ampSeries.length; i++) {
                ampSeries[i] = new float[n];
            }
        }
    }

    private void pushTrend(float v) {
        trend[trendWrite] = v;
        trendWrite = (trendWrite + 1) % TREND_POINTS;
        if (trendWrite == 0) {
            trendFilled = true;
        }
    }

    private float getTrendValue(int historyPos, int count) {
        if (!trendFilled) {
            return trend[historyPos];
        }
        int idx = (trendWrite + historyPos) % TREND_POINTS;
        return trend[idx];
    }

    private void copyTail(float[] src, float[] dst, int count) {
        int start = src.length - count;
        System.arraycopy(src, start, dst, 0, count);
    }

    private void demean(float[] x) {
        float m = 0f;
        for (float v : x) {
            m += v;
        }
        m /= Math.max(1, x.length);
        for (int i = 0; i < x.length; i++) {
            x[i] -= m;
        }
    }

    private String[] buildChannelLabels() {
        int n = Math.max(1, nchan);
        String[] labels = new String[n];
        for (int i = 0; i < n; i++) {
            labels[i] = "通道 " + (i + 1);
        }
        return labels;
    }

    private int[] range(int lo, int hi, int step) {
        int n = (hi - lo) / step + 1;
        int[] out = new int[Math.max(1, n)];
        int v = lo;
        for (int i = 0; i < out.length; i++) {
            out[i] = v;
            v += step;
        }
        return out;
    }

    private int pacColor(float t) {
        t = PApplet.constrain(t, 0f, 1f);
        if (t < 0.35f) {
            float u = t / 0.35f;
            return rgb(
                    (int) PApplet.lerp(18, 45, u),
                    (int) PApplet.lerp(30, 165, u),
                    (int) PApplet.lerp(65, 255, u)
            );
        } else if (t < 0.70f) {
            float u = (t - 0.35f) / 0.35f;
            return rgb(
                    (int) PApplet.lerp(45, 255, u),
                    (int) PApplet.lerp(165, 220, u),
                    (int) PApplet.lerp(255, 90, u)
            );
        } else {
            float u = (t - 0.70f) / 0.30f;
            return rgb(
                    (int) PApplet.lerp(255, 255, u),
                    (int) PApplet.lerp(220, 80, u),
                    (int) PApplet.lerp(90, 70, u)
            );
        }
    }

    private int rgb(int r, int g, int b) {
        return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    private void drawGradientBackground(int x0, int y0, int w0, int h0) {
        for (int i = 0; i < h0; i++) {
            float t = i / (float) Math.max(1, h0 - 1);
            int c = lerpRgb(BG_TOP, BG_BOTTOM, t);
            MAIN.stroke((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF);
            MAIN.line(x0, y0 + i, x0 + w0, y0 + i);
        }

        float glow = 0.5f + 0.5f * (float) Math.sin(MAIN.frameCount * 0.01f);
        MAIN.noStroke();
        MAIN.fill(79, 216, 255, (int) (18 * glow));
        MAIN.ellipse(x0 + w0 * 0.18f, y0 + h0 * 0.26f, w0 * 0.42f, h0 * 0.36f);
        MAIN.fill(103, 128, 255, (int) (14 * glow));
        MAIN.ellipse(x0 + w0 * 0.82f, y0 + h0 * 0.74f, w0 * 0.36f, h0 * 0.32f);
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
        return rgb(r, g, b);
    }
}
