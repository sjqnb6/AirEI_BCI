package W_SpO2_;

import Globel.GUI;
import Widget_.Widget;
import processing.core.PApplet;

import java.util.Arrays;

import static Globel.GUI.p7;

public class W_SpO2 extends Widget {
    private static final int BG_TOP = 0xFF071120;
    private static final int BG_BOTTOM = 0xFF0E1A30;
    private static final int PANEL = 0xCC13243F;
    private static final int PANEL_STROKE = 0x6683A2CC;
    private static final int CARD = 0x2A163153;
    private static final int TEXT_MAIN = 0xFFEAF2FF;
    private static final int TEXT_SUB = 0xFF98AECE;
    private static final int GOOD = 0xFF54D59D;
    private static final int WARN = 0xFFE9B45A;
    private static final int BAD = 0xFFF06B6E;
    private static final int INFO = 0xFF56C8FF;
    private static final int RED_TRACE = 0xFFFF5C72;
    private static final int IR_TRACE = 0xFF62D6FF;
    private static final int WAVE_POINTS = 1800;
    private static final int TREND_POINTS = 180;
    private static final String[] SOURCE_LABELS = {"模拟", "硬件", "暂停"};
    private static final String[] WAVE_LABELS = {"Red + IR", "仅 Red", "仅 IR", "归一化"};
    private static final String[] WINDOW_LABELS = {"10秒", "20秒", "30秒", "60秒"};
    private static final String[] ALERT_LABELS = {"宽松", "标准", "严格"};
    private static final int[] WINDOW_POINTS = {300, 600, 900, 1800};

    private final GUI MAIN;

    private final float[] redWave = new float[WAVE_POINTS];
    private final float[] irWave = new float[WAVE_POINTS];
    private final float[] spo2Trend = new float[TREND_POINTS];
    private final float[] hrTrend = new float[TREND_POINTS];
    private final float[] qualityTrend = new float[TREND_POINTS];

    private float spo2 = 98.0f;
    private float heartRate = 76.0f;
    private float red = 52000f;
    private float ir = 62000f;
    private float signalQuality = 86.0f;
    private boolean fingerDetected = true;
    private long timestamp = 0L;
    private long lastDemoMs = 0L;
    private float demoPhase = 0f;
    private boolean usingDemoData = true;
    private boolean hardwareFrameSeen = false;
    private int sourceIndex = 0;
    private int waveModeIndex = 0;
    private int windowIndex = 0;
    private int alertIndex = 1;

    public W_SpO2(GUI MAIN) {
        super(MAIN);
        this.MAIN = MAIN;
        dropdownWidth = 82;

        addDropdown("SpO2Source", "数据源", Arrays.asList(SOURCE_LABELS), sourceIndex);
        addDropdown("SpO2Wave", "波形", Arrays.asList(WAVE_LABELS), waveModeIndex);
        addDropdown("SpO2Window", "时间窗", Arrays.asList(WINDOW_LABELS), windowIndex);
        addDropdown("SpO2Alert", "阈值", Arrays.asList(ALERT_LABELS), alertIndex);

        for (int i = 0; i < WAVE_POINTS; i++) {
            redWave[i] = red;
            irWave[i] = ir;
        }
        for (int i = 0; i < TREND_POINTS; i++) {
            spo2Trend[i] = spo2;
            hrTrend[i] = heartRate;
            qualityTrend[i] = signalQuality;
        }
    }

    public void SpO2Source(int n) {
        sourceIndex = PApplet.constrain(n, 0, SOURCE_LABELS.length - 1);
        usingDemoData = sourceIndex == 0;
    }

    public void SpO2Wave(int n) {
        waveModeIndex = PApplet.constrain(n, 0, WAVE_LABELS.length - 1);
    }

    public void SpO2Window(int n) {
        windowIndex = PApplet.constrain(n, 0, WINDOW_LABELS.length - 1);
    }

    public void SpO2Alert(int n) {
        alertIndex = PApplet.constrain(n, 0, ALERT_LABELS.length - 1);
    }

    public void updateFromHardwareFrame(
            float spo2Value,
            float heartRateValue,
            float redValue,
            float irValue,
            float signalQualityValue,
            boolean fingerDetectedValue,
            long timestampValue
    ) {
        if (sourceIndex == 2) {
            return;
        }
        sourceIndex = 1;
        usingDemoData = false;
        hardwareFrameSeen = true;
        applyFrame(spo2Value, heartRateValue, redValue, irValue, signalQualityValue, fingerDetectedValue, timestampValue);
    }

    @Override
    public void update() {
        super.update();
        if (sourceIndex == 2) {
            return;
        }
        if (sourceIndex == 0) {
            updateDemoData();
        }
    }

    @Override
    public void draw() {
        super.draw();

        MAIN.pushStyle();
        drawGradientBackground(x, y - 1, w, h + 1);

        int pad = 10;
        int panelX = x + pad;
        int panelY = y + pad;
        int panelW = w - pad * 2;
        int panelH = h - pad * 2;

        MAIN.noStroke();
        MAIN.fill(PANEL);
        MAIN.rect(panelX, panelY, panelW, panelH, 4);
        MAIN.stroke(PANEL_STROKE);
        MAIN.strokeWeight(1.1f);
        MAIN.noFill();
        MAIN.rect(panelX, panelY, panelW, panelH, 4);

        drawHeader(panelX, panelY, panelW);

        int contentY = panelY + 58;
        int contentH = panelH - 68;
        int gap = 10;
        int topH = Math.min(118, Math.max(88, contentH / 3));
        int cardW = (panelW - 20 - gap * 2) / 3;
        int cardY = contentY;
        int cardX = panelX + 10;

        drawSpO2Card(cardX, cardY, cardW, topH);
        drawHeartCard(cardX + cardW + gap, cardY, cardW, topH);
        drawQualityCard(cardX + (cardW + gap) * 2, cardY, cardW, topH);

        int lowerY = contentY + topH + gap;
        int lowerH = contentH - topH - gap;
        int leftW = Math.max(260, (int) ((panelW - 20) * 0.64f));
        int rightW = panelW - 20 - leftW - gap;
        int leftX = panelX + 10;
        int rightX = leftX + leftW + gap;

        int waveH = Math.max(150, (int) (lowerH * 0.62f));
        drawPpgWaveCard(leftX, lowerY, leftW, waveH);
        drawTrendCard(leftX, lowerY + waveH + gap, leftW, lowerH - waveH - gap);
        drawFrameCard(rightX, lowerY, rightW, lowerH);

        MAIN.popStyle();
    }

    private void updateDemoData() {
        long now = MAIN.millis();
        if (now - lastDemoMs < 33) {
            return;
        }
        lastDemoMs = now;

        demoPhase += 0.115f;
        float beat = Math.max(0f, PApplet.sin(demoPhase));
        beat = beat * beat * (1.35f - 0.35f * beat);
        float secondary = 0.20f * Math.max(0f, PApplet.sin(demoPhase - 1.05f));
        float respiration = 0.5f + 0.5f * PApplet.sin(demoPhase * 0.045f);

        float demoHr = 76f + 5.5f * PApplet.sin(demoPhase * 0.18f) + 1.2f * PApplet.sin(demoPhase * 0.055f);
        float demoSpo2 = 98.0f + 0.65f * PApplet.sin(demoPhase * 0.14f);
        float demoQuality = 85f + 8f * PApplet.sin(demoPhase * 0.12f) + 2f * PApplet.sin(demoPhase * 0.035f);
        float demoRed = 52000f + 1700f * beat + 420f * secondary + 260f * respiration;
        float demoIr = 62000f + 2300f * beat + 520f * secondary + 320f * respiration;

        applyFrame(demoSpo2, demoHr, demoRed, demoIr, demoQuality, true, System.currentTimeMillis());
    }

    private void applyFrame(float spo2Value, float hrValue, float redValue, float irValue, float qualityValue, boolean finger, long ts) {
        spo2 = PApplet.constrain(spo2Value, 0f, 100f);
        heartRate = PApplet.constrain(hrValue, 0f, 240f);
        red = Math.max(0f, redValue);
        ir = Math.max(0f, irValue);
        signalQuality = PApplet.constrain(qualityValue, 0f, 100f);
        fingerDetected = finger;
        timestamp = ts;

        push(redWave, red);
        push(irWave, ir);
        push(spo2Trend, spo2);
        push(hrTrend, heartRate);
        push(qualityTrend, signalQuality);
    }

    private void drawHeader(int px, int py, int pw) {
        MAIN.fill(TEXT_MAIN);
        MAIN.textFont(p7);
        MAIN.textSize(16);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text("血氧监测", px + 12, py + 9);

        MAIN.fill(TEXT_SUB);
        MAIN.textSize(11);
        MAIN.text("SpO2 / 心率 / Red-IR PPG 波形", px + 12, py + 32);

        int statusColor = sourceIndex == 2 ? WARN : (fingerDetected ? GOOD : BAD);
        String status = sourceIndex == 2 ? "已暂停" : (sourceIndex == 0 ? "模拟数据" : (hardwareFrameSeen ? "硬件数据" : "等待硬件"));
        drawPill(px + pw - 112, py + 11, 96, 22, status, statusColor);

        MAIN.stroke(PANEL_STROKE);
        MAIN.strokeWeight(1f);
        MAIN.line(px + 10, py + 50, px + pw - 10, py + 50);
    }

    private void drawSpO2Card(int x0, int y0, int w0, int h0) {
        int c = spo2Color(spo2);
        drawCard(x0, y0, w0, h0, "血氧饱和度");
        MAIN.fill(c);
        MAIN.textFont(p7);
        MAIN.textSize(Math.max(24, Math.min(38, w0 / 5)));
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text(PApplet.nf(spo2, 1, 0), x0 + 14, y0 + 34);
        MAIN.textSize(18);
        MAIN.text("%", x0 + 75 + Math.max(52, w0 * 0.28f), y0 + 50);
        drawRangeBar(x0 + 14, y0 + h0 - 20, w0 - 28, 8, spo2, 80f, 100f, c);
        MAIN.fill(TEXT_SUB);
        MAIN.textSize(10);
        MAIN.textAlign(PApplet.RIGHT, PApplet.TOP);
        MAIN.text(spo2Label(spo2), x0 + w0 - 14, y0 + 14);
    }

    private void drawHeartCard(int x0, int y0, int w0, int h0) {
        int c = hrColor(heartRate);
        drawCard(x0, y0, w0, h0, "心率");
        MAIN.fill(c);
        MAIN.textFont(p7);
        MAIN.textSize(Math.max(24, Math.min(38, w0 / 5)));
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text(PApplet.nf(heartRate, 1, 0), x0 + 14, y0 + 34);
        MAIN.textSize(15);
        MAIN.text("BPM", x0 + 75 + Math.max(52, w0 * 0.28f), y0 + 50);
        drawRangeBar(x0 + 14, y0 + h0 - 20, w0 - 28, 8, heartRate, 40f, 140f, c);
        drawHeartPulse(x0 + w0 - 34, y0 + 24, c);
    }

    private void drawQualityCard(int x0, int y0, int w0, int h0) {
        drawCard(x0, y0, w0, h0, "信号状态");
        int qc = qualityColor(signalQuality);

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7);
        MAIN.textSize(10);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text("信号质量", x0 + 14, y0 + 34);

        drawProgressBar(x0 + 14, y0 + 52, w0 - 28, 12, signalQuality, qc);

        MAIN.fill(qc);
        MAIN.textSize(18);
        MAIN.textAlign(PApplet.RIGHT, PApplet.TOP);
        MAIN.text(PApplet.nf(signalQuality, 1, 0) + "%", x0 + w0 - 14, y0 + 30);

        int fc = fingerDetected ? GOOD : BAD;
        drawPill(x0 + 14, y0 + h0 - 34, Math.min(120, w0 - 28), 22, fingerDetected ? "手指已检测" : "未检测到手指", fc);
    }

    private void drawPpgWaveCard(int x0, int y0, int w0, int h0) {
        drawCard(x0, y0, w0, h0, "实时PPG波形");
        int gx = x0 + 12;
        int gy = y0 + 42;
        int gw = w0 - 24;
        int gh = h0 - 62;

        if (waveModeIndex == 0 || waveModeIndex == 1 || waveModeIndex == 3) {
            drawLegend(x0 + w0 - 156, y0 + 10, waveModeIndex == 3 ? "Red(归一)" : "Red", RED_TRACE);
        }
        if (waveModeIndex == 0 || waveModeIndex == 2 || waveModeIndex == 3) {
            drawLegend(x0 + w0 - 78, y0 + 10, waveModeIndex == 3 ? "IR(归一)" : "IR", IR_TRACE);
        }

        drawGrid(gx, gy, gw, gh);
        int displayCount = getWaveDisplayCount();
        if (waveModeIndex == 0) {
            float[] range = combinedRange(redWave, irWave, displayCount);
            drawWaveWithRange(redWave, gx, gy, gw, gh, RED_TRACE, displayCount, range[0], range[1]);
            drawWaveWithRange(irWave, gx, gy, gw, gh, IR_TRACE, displayCount, range[0], range[1]);
        } else if (waveModeIndex == 1 || waveModeIndex == 3) {
            drawWave(redWave, gx, gy, gw, gh, RED_TRACE, displayCount);
        }
        if (waveModeIndex == 2 || waveModeIndex == 3) {
            drawWave(irWave, gx, gy, gw, gh, IR_TRACE, displayCount);
        }

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7);
        MAIN.textSize(10);
        MAIN.textAlign(PApplet.RIGHT, PApplet.BOTTOM);
        MAIN.text(WINDOW_LABELS[windowIndex] + "  最新 Red " + PApplet.nf(red, 1, 0) + "  IR " + PApplet.nf(ir, 1, 0), gx + gw - 4, y0 + h0 - 10);
    }

    private void drawTrendCard(int x0, int y0, int w0, int h0) {
        drawCard(x0, y0, w0, h0, "趋势概览");
        int gx = x0 + 12;
        int gy = y0 + 30;
        int gw = w0 - 24;
        int rowH = Math.max(28, (h0 - 42) / 3);
        drawMiniTrend(gx, gy, gw, rowH - 4, spo2Trend, 2f, RED_TRACE, "SpO2", "%");
        drawMiniTrend(gx, gy + rowH, gw, rowH - 4, hrTrend, 12f, INFO, "HR", " BPM");
        drawMiniTrend(gx, gy + rowH * 2, gw, rowH - 4, qualityTrend, 18f, GOOD, "信号质量", "%");
    }

    private void drawFrameCard(int x0, int y0, int w0, int h0) {
        drawCard(x0, y0, w0, h0, "数据帧预留");

        int rowY = y0 + 34;
        int rowH = 24;
        drawFieldRow(x0 + 12, rowY, w0 - 24, "血氧饱和度", PApplet.nf(spo2, 1, 1) + "%");
        drawFieldRow(x0 + 12, rowY + rowH, w0 - 24, "心率", PApplet.nf(heartRate, 1, 0) + " BPM");
        drawFieldRow(x0 + 12, rowY + rowH * 2, w0 - 24, "Red", PApplet.nf(red, 1, 0));
        drawFieldRow(x0 + 12, rowY + rowH * 3, w0 - 24, "IR", PApplet.nf(ir, 1, 0));
        drawFieldRow(x0 + 12, rowY + rowH * 4, w0 - 24, "信号质量", PApplet.nf(signalQuality, 1, 0) + "%");
        drawFieldRow(x0 + 12, rowY + rowH * 5, w0 - 24, "手指检测", fingerDetected ? "已检测" : "未检测");
        drawFieldRow(x0 + 12, rowY + rowH * 6, w0 - 24, "时间戳", timestamp > 0 ? String.valueOf(timestamp) : "--");
        drawFieldRow(x0 + 12, rowY + rowH * 7, w0 - 24, "数据源 / 阈值", SOURCE_LABELS[sourceIndex] + " / " + ALERT_LABELS[alertIndex]);
        drawFieldRow(x0 + 12, rowY + rowH * 8, w0 - 24, "波形 / 时间窗", WAVE_LABELS[waveModeIndex] + " / " + WINDOW_LABELS[windowIndex]);

        int hintY = rowY + rowH * 9 + 8;
        int piY = y0 + h0 - 54;
        if (hintY < piY - 30) {
        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7);
        MAIN.textSize(10);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        //drawWrappedText("后续解析硬件帧时调用 updateFromHardwareFrame(...) 即可刷新此界面。当前为模拟占位数据。", x0 + 12, hintY, w0 - 24, 15);
        }

        float pi = estimatePerfusionIndex();
        MAIN.fill(TEXT_MAIN);
        MAIN.textSize(12);
        MAIN.text("灌注指数", x0 + 12, piY);
        MAIN.fill(INFO);
        MAIN.textSize(24);
        MAIN.text(PApplet.nf(pi, 1, 2), x0 + 12, piY + 16);
        MAIN.fill(TEXT_SUB);
        MAIN.textSize(10);
        MAIN.text("% AC/DC", x0 + 76, piY + 25);
    }

    private void drawFieldRow(int x0, int y0, int w0, String key, String value) {
        MAIN.noStroke();
        MAIN.fill(0x20183357);
        MAIN.rect(x0, y0, w0, 23, 3);
        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7);
        MAIN.textSize(10);
        MAIN.textAlign(PApplet.LEFT, PApplet.CENTER);
        MAIN.text(key, x0 + 7, y0 + 11);
        MAIN.fill(TEXT_MAIN);
        MAIN.textAlign(PApplet.RIGHT, PApplet.CENTER);
        MAIN.text(value, x0 + w0 - 7, y0 + 11);
    }

    private void drawMiniTrend(int x0, int y0, int w0, int h0, float[] data, float minimumSpan, int color, String label, String unit) {
        int labelW = Math.min(82, Math.max(58, w0 / 4));
        int plotX = x0 + labelW;
        int plotW = Math.max(20, w0 - labelW);

        float dataMin = Float.POSITIVE_INFINITY;
        float dataMax = Float.NEGATIVE_INFINITY;
        for (float value : data) {
            if (value < dataMin) dataMin = value;
            if (value > dataMax) dataMax = value;
        }
        float span = Math.max(minimumSpan, dataMax - dataMin);
        float padding = span * 0.20f;
        float min = dataMin - padding;
        float max = dataMax + padding;

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7);
        MAIN.textSize(9);
        MAIN.textAlign(PApplet.LEFT, PApplet.CENTER);
        MAIN.text(label + " " + PApplet.nf(data[data.length - 1], 1, 0) + unit, x0 + 2, y0 + h0 * 0.5f);

        MAIN.stroke(45, 70, 105, 170);
        MAIN.strokeWeight(1f);
        MAIN.line(plotX, y0 + h0, plotX + plotW, y0 + h0);

        MAIN.noStroke();
        MAIN.fill((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 35);
        MAIN.beginShape();
        MAIN.vertex(plotX, y0 + h0);
        for (int i = 0; i < data.length; i++) {
            float xx = plotX + plotW * i / (float) (data.length - 1);
            float yy = y0 + h0 - h0 * PApplet.constrain((data[i] - min) / Math.max(1e-6f, max - min), 0f, 1f);
            MAIN.vertex(xx, yy);
        }
        MAIN.vertex(plotX + plotW, y0 + h0);
        MAIN.endShape(PApplet.CLOSE);

        MAIN.noFill();
        MAIN.stroke((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 220);
        MAIN.strokeWeight(2.0f);
        MAIN.beginShape();
        for (int i = 0; i < data.length; i++) {
            float xx = plotX + plotW * i / (float) (data.length - 1);
            float yy = y0 + h0 - h0 * PApplet.constrain((data[i] - min) / Math.max(1e-6f, max - min), 0f, 1f);
            MAIN.vertex(xx, yy);
        }
        MAIN.endShape();
    }

    private void drawWave(float[] data, int gx, int gy, int gw, int gh, int color, int count) {
        count = PApplet.constrain(count, 2, data.length);
        int start = data.length - count;
        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;
        for (int i = start; i < data.length; i++) {
            float v = data[i];
            if (v < min) min = v;
            if (v > max) max = v;
        }
        float span = Math.max(1f, max - min);

        MAIN.noFill();
        MAIN.stroke((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 220);
        MAIN.strokeWeight(1.7f);
        MAIN.beginShape();
        for (int i = 0; i < count; i++) {
            float v = data[start + i];
            float xx = gx + gw * i / (float) (count - 1);
            float yy = gy + gh - gh * ((v - min) / span);
            MAIN.vertex(xx, yy);
        }
        MAIN.endShape();
    }

    private void drawWaveWithRange(float[] data, int gx, int gy, int gw, int gh, int color, int count, float min, float max) {
        count = PApplet.constrain(count, 2, data.length);
        int start = data.length - count;
        float span = Math.max(1f, max - min);

        MAIN.noFill();
        MAIN.stroke((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 220);
        MAIN.strokeWeight(1.7f);
        MAIN.beginShape();
        for (int i = 0; i < count; i++) {
            float v = data[start + i];
            float xx = gx + gw * i / (float) (count - 1);
            float yy = gy + gh - gh * PApplet.constrain((v - min) / span, 0f, 1f);
            MAIN.vertex(xx, yy);
        }
        MAIN.endShape();
    }

    private float[] combinedRange(float[] a, float[] b, int count) {
        count = PApplet.constrain(count, 2, Math.min(a.length, b.length));
        int start = a.length - count;
        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;
        for (int i = start; i < a.length; i++) {
            float va = a[i];
            float vb = b[i];
            if (va < min) min = va;
            if (vb < min) min = vb;
            if (va > max) max = va;
            if (vb > max) max = vb;
        }
        if (!Float.isFinite(min) || !Float.isFinite(max) || min == max) {
            return new float[] {0f, 1f};
        }
        return new float[] {min, max};
    }

    private int getWaveDisplayCount() {
        return PApplet.constrain(WINDOW_POINTS[windowIndex], 2, WAVE_POINTS);
    }

    private void drawGrid(int gx, int gy, int gw, int gh) {
        MAIN.stroke(65, 92, 132, 115);
        MAIN.strokeWeight(0.8f);
        for (int i = 0; i <= 4; i++) {
            float yy = gy + gh * i / 4f;
            MAIN.line(gx, yy, gx + gw, yy);
        }
        for (int i = 0; i <= 6; i++) {
            float xx = gx + gw * i / 6f;
            MAIN.line(xx, gy, xx, gy + gh);
        }
        MAIN.stroke(PANEL_STROKE);
        MAIN.noFill();
        MAIN.rect(gx, gy, gw, gh, 3);
    }

    private void drawLegend(int x0, int y0, String label, int color) {
        MAIN.noStroke();
        MAIN.fill((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 220);
        MAIN.rect(x0, y0 + 4, 18, 3, 2);
        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7);
        MAIN.textSize(10);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text(label, x0 + 24, y0);
    }

    private void drawProgressBar(int x0, int y0, int w0, int h0, float value, int color) {
        MAIN.noStroke();
        MAIN.fill(20, 32, 52, 230);
        MAIN.rect(x0, y0, w0, h0, h0 / 2);
        MAIN.fill((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 230);
        MAIN.rect(x0, y0, w0 * PApplet.constrain(value / 100f, 0f, 1f), h0, h0 / 2);
        MAIN.stroke(PANEL_STROKE);
        MAIN.noFill();
        MAIN.rect(x0, y0, w0, h0, h0 / 2);
    }

    private void drawRangeBar(int x0, int y0, int w0, int h0, float value, float min, float max, int color) {
        float t = PApplet.constrain((value - min) / Math.max(1e-6f, max - min), 0f, 1f);
        drawProgressBar(x0, y0, w0, h0, t * 100f, color);
    }

    private void drawHeartPulse(int cx, int cy, int color) {
        float pulse = 1f + 0.10f * PApplet.sin(MAIN.frameCount * 0.18f);
        MAIN.noStroke();
        MAIN.fill((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 60);
        MAIN.ellipse(cx, cy, 30 * pulse, 30 * pulse);
        MAIN.fill((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 220);
        MAIN.ellipse(cx, cy, 12, 12);
    }

    private void drawPill(int x0, int y0, int w0, int h0, String text, int color) {
        MAIN.noStroke();
        MAIN.fill((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 42);
        MAIN.rect(x0, y0, w0, h0, h0 / 2);
        MAIN.stroke((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 190);
        MAIN.noFill();
        MAIN.rect(x0, y0, w0, h0, h0 / 2);
        MAIN.fill(color);
        MAIN.textFont(p7);
        MAIN.textSize(10);
        MAIN.textAlign(PApplet.CENTER, PApplet.CENTER);
        MAIN.text(text, x0 + w0 * 0.5f, y0 + h0 * 0.5f);
    }

    private void drawCard(int x0, int y0, int w0, int h0, String title) {
        MAIN.noStroke();
        MAIN.fill(CARD);
        MAIN.rect(x0, y0, w0, h0, 5);
        MAIN.stroke(PANEL_STROKE);
        MAIN.strokeWeight(1f);
        MAIN.noFill();
        MAIN.rect(x0, y0, w0, h0, 5);
        MAIN.fill(TEXT_MAIN);
        MAIN.textFont(p7);
        MAIN.textSize(12);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text(title, x0 + 10, y0 + 9);
    }

    private void drawGradientBackground(int x0, int y0, int w0, int h0) {
        for (int i = 0; i < h0; i++) {
            float t = i / (float) Math.max(1, h0 - 1);
            int c = lerpRgb(BG_TOP, BG_BOTTOM, t);
            MAIN.stroke((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF);
            MAIN.line(x0, y0 + i, x0 + w0, y0 + i);
        }
        float glow = 0.5f + 0.5f * PApplet.sin(MAIN.frameCount * 0.012f);
        MAIN.noStroke();
        MAIN.fill(86, 202, 255, (int) (14 * glow));
        MAIN.ellipse(x0 + w0 * 0.20f, y0 + h0 * 0.24f, w0 * 0.38f, h0 * 0.32f);
        MAIN.fill(255, 88, 112, (int) (12 * glow));
        MAIN.ellipse(x0 + w0 * 0.76f, y0 + h0 * 0.72f, w0 * 0.32f, h0 * 0.28f);
    }

    private void drawWrappedText(String text, int x0, int y0, int w0, int lineH) {
        StringBuilder line = new StringBuilder();
        int yLine = y0;
        for (int i = 0; i < text.length(); i++) {
            line.append(text.charAt(i));
            if (MAIN.textWidth(line.toString()) > w0 || i == text.length() - 1) {
                MAIN.text(line.toString(), x0, yLine);
                line.setLength(0);
                yLine += lineH;
            }
        }
    }

    private float estimatePerfusionIndex() {
        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;
        float mean = 0f;
        for (float v : irWave) {
            if (v < min) min = v;
            if (v > max) max = v;
            mean += v;
        }
        mean /= Math.max(1, irWave.length);
        return 100f * Math.max(0f, max - min) / Math.max(1f, mean);
    }

    private int spo2Color(float v) {
        float warn = alertIndex == 0 ? 92f : (alertIndex == 1 ? 95f : 96f);
        float bad = alertIndex == 0 ? 88f : (alertIndex == 1 ? 90f : 93f);
        if (v >= warn) return GOOD;
        if (v >= bad) return WARN;
        return BAD;
    }

    private int hrColor(float v) {
        float goodLow = alertIndex == 0 ? 45f : (alertIndex == 1 ? 50f : 55f);
        float goodHigh = alertIndex == 0 ? 125f : (alertIndex == 1 ? 110f : 100f);
        float warnLow = alertIndex == 0 ? 35f : (alertIndex == 1 ? 40f : 45f);
        float warnHigh = alertIndex == 0 ? 150f : (alertIndex == 1 ? 130f : 120f);
        if (v >= goodLow && v <= goodHigh) return INFO;
        if (v >= warnLow && v <= warnHigh) return WARN;
        return BAD;
    }

    private int qualityColor(float v) {
        float good = alertIndex == 0 ? 70f : (alertIndex == 1 ? 75f : 85f);
        float warn = alertIndex == 0 ? 40f : (alertIndex == 1 ? 45f : 60f);
        if (v >= good) return GOOD;
        if (v >= warn) return WARN;
        return BAD;
    }

    private String spo2Label(float v) {
        int c = spo2Color(v);
        if (c == GOOD) return "稳定";
        if (c == WARN) return "偏低";
        return "警告";
    }

    private void push(float[] arr, float v) {
        for (int i = 0; i < arr.length - 1; i++) {
            arr[i] = arr[i + 1];
        }
        arr[arr.length - 1] = v;
    }

    private static int lerpRgb(int c1, int c2, float t) {
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
