package W_SignalQuality_;

import Globel.GUI;
import Widget_.Widget;
import processing.core.PApplet;

import java.util.Arrays;

import static Globel.GUI.*;

public class W_SignalQuality extends Widget {
    private static final int[] WINDOW_SECONDS = {2, 4, 6};
    private static final String[] STRICTNESS_LABELS = {"宽松", "标准", "严格"};
    private static final String[] LINE_LABELS = {"自动", "50Hz", "60Hz"};
    private static final String[] WINDOW_LABELS = {"2秒", "4秒", "6秒"};

    private static final int HISTORY_SIZE = 180;
    private static final int RADAR_DIM = 5;

    private static final int BG_TOP = 0xFF0A1220;
    private static final int BG_BOTTOM = 0xFF0E1A30;
    private static final int PANEL = 0xCC13243F;
    private static final int PANEL_STROKE = 0x6683A2CC;
    private static final int CARD = 0x2A163153;
    private static final int TEXT_MAIN = 0xFFEAF2FF;
    private static final int TEXT_SUB = 0xFF98AECE;
    private static final int GRID = 0x2D90AED8;
    private static final int GOOD = 0xFF54D59D;
    private static final int WARN = 0xFFE9B45A;
    private static final int BAD = 0xFFF06B6E;
    private static final int INFO = 0xFF56C8FF;

    private static final String[] RADAR_NAMES = {"肌电污染", "工频干扰", "基线漂移", "饱和风险", "突发伪迹"};

    private final GUI MAIN;

    private int strictnessIndex = 1;
    private int lineFreqIndex = 0;
    private int windowIndex = 1;

    private float[] chQuality = new float[0];
    private float[] chEmg = new float[0];
    private float[] chLine = new float[0];
    private float[] chDrift = new float[0];
    private float[] chSaturate = new float[0];
    private float[] chBurst = new float[0];

    private float globalQuality = 100f;
    private final float[] radarVals = new float[RADAR_DIM];
    private final float[] trend = new float[HISTORY_SIZE];
    private int trendWrite = 0;
    private boolean trendFilled = false;

    private int alertLevel = 0; // 0 稳定 1 注意 2 警告
    private String alertTitle = "信号稳定";
    private String alertMsg = "当前信号质量良好，可用于实时分析。";
    private long lastUpdateMs = 0;

    public W_SignalQuality(GUI MAIN) {
        super(MAIN);
        this.MAIN = MAIN;

        addDropdown("SQStrictness", "阈值策略", Arrays.asList(STRICTNESS_LABELS), strictnessIndex);
        addDropdown("SQLine", "工频制式", Arrays.asList(LINE_LABELS), lineFreqIndex);
        addDropdown("SQWindow", "评估窗长", Arrays.asList(WINDOW_LABELS), windowIndex);

        ensureChannelBuffers();
    }

    public void SQStrictness(int n) {
        strictnessIndex = PApplet.constrain(n, 0, STRICTNESS_LABELS.length - 1);
    }

    public void SQLine(int n) {
        lineFreqIndex = PApplet.constrain(n, 0, LINE_LABELS.length - 1);
    }

    public void SQWindow(int n) {
        windowIndex = PApplet.constrain(n, 0, WINDOW_LABELS.length - 1);
    }

    @Override
    public void update() {
        super.update();

        ensureChannelBuffers();
        long now = MAIN.millis();
        if (now - lastUpdateMs < 160) {
            return;
        }
        lastUpdateMs = now;

        computeQuality();
        pushTrend(globalQuality);
        updateAlert();
    }

    @Override
    public void draw() {
        super.draw();

        MAIN.pushStyle();
        drawGradientBackground();

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

        int contentY = panelY + 56;
        int contentH = panelH - 66;
        int leftW = (int) (panelW * 0.62f);
        int rightW = panelW - leftW - 12;

        drawChannelCard(panelX + 10, contentY, leftW - 14, contentH * 3 / 5);
        drawTrendCard(panelX + 10, contentY + contentH * 3 / 5 + 8, leftW - 14, contentH * 2 / 5 - 8);

        int rightX = panelX + leftW + 8;
        int radarH = contentH * 3 / 5;
        drawRadarCard(rightX, contentY, rightW - 10, radarH);
        drawAlertCard(rightX, contentY + radarH + 8, rightW - 10, contentH - radarH - 8);

        MAIN.popStyle();
    }

    private void drawHeader(int px, int py, int pw) {
        MAIN.fill(TEXT_MAIN);
        MAIN.textFont(p7);
        MAIN.textSize(15);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text("伪迹与信号质量中心", px + 12, py + 9);

        int scoreColor = qualityColor(globalQuality);
        MAIN.fill(scoreColor);
        MAIN.textSize(26);
        MAIN.textAlign(PApplet.RIGHT, PApplet.TOP);
        MAIN.text(PApplet.nf(globalQuality, 1, 1), px + pw - 14, py + 6);

        MAIN.fill(TEXT_SUB);
        MAIN.textSize(11);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text(
                "评估窗长 " + WINDOW_LABELS[windowIndex] + " | 阈值策略 " + STRICTNESS_LABELS[strictnessIndex]
                        + " | 工频制式 " + LINE_LABELS[lineFreqIndex],
                px + 12, py + 32
        );

        MAIN.textAlign(PApplet.RIGHT, PApplet.TOP);
        MAIN.text("综合质量分 / 100", px + pw - 14, py + 35);

        MAIN.stroke(PANEL_STROKE);
        MAIN.strokeWeight(1f);
        MAIN.line(px + 10, py + 48, px + pw - 10, py + 48);
    }

    private void drawChannelCard(int x0, int y0, int w0, int h0) {
        drawCard(x0, y0, w0, h0, "通道质量矩阵");

        int n = chQuality.length;
        if (n <= 0) {
            return;
        }

        int tx = x0 + 10;
        int ty = y0 + 28;
        int tw = w0 - 20;
        int th = h0 - 36;
        int rowH = Math.max(18, th / n);

        MAIN.textFont(p7);
        MAIN.textSize(10);
        MAIN.textAlign(PApplet.LEFT, PApplet.CENTER);
        int hoverChannel = -1;

        for (int i = 0; i < n; i++) {
            int yRow = ty + i * rowH;
            if (yRow + rowH > y0 + h0 - 8) {
                break;
            }

            String ch = "Ch" + (i + 1);
            MAIN.fill(TEXT_SUB);
            MAIN.text(ch, tx, yRow + rowH * 0.5f);

            int barX = tx + 34;
            int barW = Math.max(60, tw - 170);
            int barH = Math.max(8, rowH - 8);
            int barY = yRow + (rowH - barH) / 2;

            MAIN.noStroke();
            MAIN.fill(26, 42, 68, 210);
            MAIN.rect(barX, barY, barW, barH, 2);

            float q = PApplet.constrain(chQuality[i], 0f, 100f);
            int qc = qualityColor(q);
            MAIN.fill((qc >> 16) & 0xFF, (qc >> 8) & 0xFF, qc & 0xFF, 230);
            MAIN.rect(barX, barY, barW * q / 100f, barH, 2);

            MAIN.stroke(PANEL_STROKE);
            MAIN.noFill();
            MAIN.rect(barX, barY, barW, barH, 2);

            int pillX = barX + barW + 8;
            drawMiniMetric(pillX, barY, "E", chEmg[i], 0xFF5FC1FF);
            drawMiniMetric(pillX + 24, barY, "L", chLine[i], 0xFFB08CFF);
            drawMiniMetric(pillX + 48, barY, "D", chDrift[i], 0xFF7DE7C3);
            drawMiniMetric(pillX + 72, barY, "S", chSaturate[i], 0xFFFF8C8C);

            MAIN.fill(TEXT_MAIN);
            MAIN.textAlign(PApplet.RIGHT, PApplet.CENTER);
            MAIN.text(PApplet.nf(q, 1, 0), x0 + w0 - 8, yRow + rowH * 0.5f);
            MAIN.textAlign(PApplet.LEFT, PApplet.CENTER);

            if (isInsideRect(MAIN.mouseX, MAIN.mouseY, tx, yRow, tw, rowH)) {
                hoverChannel = i;
            }
        }

        if (hoverChannel >= 0) {
            drawChannelHoverDetail(x0 + w0 - 192, y0 + 28, 180, 74, hoverChannel);
        }
    }

    private void drawMiniMetric(int x, int y, String tag, float v, int c) {
        int h = 10;
        int w = 20;
        float t = PApplet.constrain(v, 0f, 1f);
        MAIN.noStroke();
        MAIN.fill(20, 30, 50, 220);
        MAIN.rect(x, y, w, h, 2);
        MAIN.fill((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, 220);
        MAIN.rect(x, y, w * t, h, 2);
        MAIN.stroke(PANEL_STROKE);
        MAIN.noFill();
        MAIN.rect(x, y, w, h, 2);
        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7);
        MAIN.textSize(8);
        MAIN.textAlign(PApplet.CENTER, PApplet.BOTTOM);
        MAIN.text(tag, x + w * 0.5f, y - 1);
    }

    private void drawRadarCard(int x0, int y0, int w0, int h0) {
        drawCard(x0, y0, w0, h0, "伪迹分类雷达");

        float cx = x0 + w0 * 0.50f;
        float cy = y0 + h0 * 0.56f;
        float r = Math.min(w0, h0) * 0.29f;

        MAIN.stroke(GRID);
        MAIN.noFill();
        for (int k = 1; k <= 4; k++) {
            float rr = r * k / 4f;
            MAIN.beginShape();
            for (int i = 0; i < RADAR_DIM; i++) {
                float a = -PApplet.HALF_PI + PApplet.TWO_PI * i / RADAR_DIM;
                MAIN.vertex(cx + rr * PApplet.cos(a), cy + rr * PApplet.sin(a));
            }
            MAIN.endShape(PApplet.CLOSE);
        }

        for (int i = 0; i < RADAR_DIM; i++) {
            float a = -PApplet.HALF_PI + PApplet.TWO_PI * i / RADAR_DIM;
            MAIN.line(cx, cy, cx + r * PApplet.cos(a), cy + r * PApplet.sin(a));
        }

        float baseThreshold = strictnessIndex == 0 ? 0.58f : (strictnessIndex == 1 ? 0.48f : 0.40f);
        float ringPulse = 0.03f * (float) Math.sin(MAIN.frameCount * 0.07f);
        float thresholdR = r * PApplet.constrain(baseThreshold + ringPulse, 0.25f, 0.78f);
        int tc = alertLevel == 2 ? BAD : (alertLevel == 1 ? WARN : INFO);
        MAIN.noStroke();
        MAIN.fill((tc >> 16) & 0xFF, (tc >> 8) & 0xFF, tc & 0xFF, 18);
        MAIN.beginShape();
        for (int i = 0; i < RADAR_DIM; i++) {
            float a = -PApplet.HALF_PI + PApplet.TWO_PI * i / RADAR_DIM;
            MAIN.vertex(cx + thresholdR * PApplet.cos(a), cy + thresholdR * PApplet.sin(a));
        }
        MAIN.endShape(PApplet.CLOSE);
        MAIN.stroke((tc >> 16) & 0xFF, (tc >> 8) & 0xFF, tc & 0xFF, 180);
        MAIN.strokeWeight(1.2f);
        MAIN.noFill();
        MAIN.beginShape();
        for (int i = 0; i < RADAR_DIM; i++) {
            float a = -PApplet.HALF_PI + PApplet.TWO_PI * i / RADAR_DIM;
            MAIN.vertex(cx + thresholdR * PApplet.cos(a), cy + thresholdR * PApplet.sin(a));
        }
        MAIN.endShape(PApplet.CLOSE);

        MAIN.noStroke();
        MAIN.fill(79, 216, 255, 120);
        MAIN.beginShape();
        for (int i = 0; i < RADAR_DIM; i++) {
            float a = -PApplet.HALF_PI + PApplet.TWO_PI * i / RADAR_DIM;
            float rr = r * PApplet.constrain(radarVals[i], 0f, 1f);
            MAIN.vertex(cx + rr * PApplet.cos(a), cy + rr * PApplet.sin(a));
        }
        MAIN.endShape(PApplet.CLOSE);

        MAIN.stroke(79, 216, 255, 220);
        MAIN.strokeWeight(1.6f);
        MAIN.noFill();
        MAIN.beginShape();
        for (int i = 0; i < RADAR_DIM; i++) {
            float a = -PApplet.HALF_PI + PApplet.TWO_PI * i / RADAR_DIM;
            float rr = r * PApplet.constrain(radarVals[i], 0f, 1f);
            MAIN.vertex(cx + rr * PApplet.cos(a), cy + rr * PApplet.sin(a));
        }
        MAIN.endShape(PApplet.CLOSE);

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7);
        MAIN.textSize(10);
        MAIN.textAlign(PApplet.CENTER, PApplet.CENTER);
        for (int i = 0; i < RADAR_DIM; i++) {
            float a = -PApplet.HALF_PI + PApplet.TWO_PI * i / RADAR_DIM;
            float rr = r + 16;
            MAIN.text(RADAR_NAMES[i], cx + rr * PApplet.cos(a), cy + rr * PApplet.sin(a));
        }
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.textSize(9);
        MAIN.fill(TEXT_SUB);
        MAIN.text("动态阈值环", x0 + 10, y0 + h0 - 16);
    }

    private void drawAlertCard(int x0, int y0, int w0, int h0) {
        drawCard(x0, y0, w0, h0, "实时告警与建议");

        int c = alertLevel == 0 ? GOOD : (alertLevel == 1 ? WARN : BAD);
        MAIN.noStroke();
        MAIN.fill((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, 42);
        MAIN.rect(x0 + 10, y0 + 28, w0 - 20, 26, 3);
        MAIN.stroke((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, 200);
        MAIN.noFill();
        MAIN.rect(x0 + 10, y0 + 28, w0 - 20, 26, 3);

        MAIN.fill(c);
        MAIN.textFont(p7);
        MAIN.textSize(12);
        MAIN.textAlign(PApplet.LEFT, PApplet.CENTER);
        MAIN.text(alertTitle, x0 + 18, y0 + 41);

        drawAlertLevelBar(x0 + 14, y0 + 62, w0 - 28, 14);

        MAIN.fill(TEXT_SUB);
        MAIN.textSize(11);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        drawWrappedText(alertMsg, x0 + 14, y0 + 82, w0 - 28, 16);

        MAIN.fill(TEXT_SUB);
        MAIN.textSize(10);
        MAIN.text("E=肌电  L=工频  D=漂移  S=饱和", x0 + 14, y0 + h0 - 16);
    }

    private void drawAlertLevelBar(int x0, int y0, int w0, int h0) {
        int segW = Math.max(6, w0 / 3);
        int[] cols = {GOOD, WARN, BAD};
        String[] labels = {"稳定", "注意", "警告"};
        for (int i = 0; i < 3; i++) {
            int xx = x0 + i * segW;
            int ww = (i == 2) ? (w0 - segW * 2) : segW;
            int c = cols[i];
            MAIN.noStroke();
            MAIN.fill((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, 78);
            MAIN.rect(xx, y0, ww, h0, 2);
            MAIN.fill(TEXT_MAIN);
            MAIN.textFont(p7);
            MAIN.textSize(9);
            MAIN.textAlign(PApplet.CENTER, PApplet.CENTER);
            MAIN.text(labels[i], xx + ww * 0.5f, y0 + h0 * 0.5f + 0.5f);
        }
        MAIN.stroke(PANEL_STROKE);
        MAIN.noFill();
        MAIN.rect(x0, y0, w0, h0, 2);

        float levelX = x0 + (alertLevel + 0.5f) * (w0 / 3f);
        MAIN.stroke(235, 245, 255, 230);
        MAIN.strokeWeight(2f);
        MAIN.line(levelX, y0 - 3, levelX, y0 + h0 + 3);
        MAIN.noStroke();
        MAIN.fill(235, 245, 255, 230);
        MAIN.triangle(levelX - 4, y0 - 4, levelX + 4, y0 - 4, levelX, y0 - 9);
    }

    private void drawChannelHoverDetail(int x0, int y0, int w0, int h0, int ch) {
        if (ch < 0 || ch >= chQuality.length) {
            return;
        }
        MAIN.noStroke();
        MAIN.fill(12, 22, 36, 238);
        MAIN.rect(x0, y0, w0, h0, 3);
        MAIN.stroke(130, 178, 235, 170);
        MAIN.noFill();
        MAIN.rect(x0, y0, w0, h0, 3);

        MAIN.textFont(p7);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.fill(TEXT_MAIN);
        MAIN.textSize(10);
        MAIN.text("通道 Ch" + (ch + 1) + " 详细分项", x0 + 8, y0 + 6);

        MAIN.fill(TEXT_SUB);
        MAIN.textSize(9);
        MAIN.text("质量分 " + PApplet.nf(chQuality[ch], 1, 1), x0 + 8, y0 + 22);
        MAIN.text("E 肌电: " + PApplet.nf(chEmg[ch] * 100f, 1, 1) + "%", x0 + 8, y0 + 36);
        MAIN.text("L 工频: " + PApplet.nf(chLine[ch] * 100f, 1, 1) + "%", x0 + 8, y0 + 48);
        MAIN.text("D 漂移: " + PApplet.nf(chDrift[ch] * 100f, 1, 1) + "%", x0 + 94, y0 + 36);
        MAIN.text("S 饱和: " + PApplet.nf(chSaturate[ch] * 100f, 1, 1) + "%", x0 + 94, y0 + 48);
        MAIN.text("B 突发: " + PApplet.nf(chBurst[ch] * 100f, 1, 1) + "%", x0 + 94, y0 + 60);
    }

    private boolean isInsideRect(float mx, float my, float x0, float y0, float w0, float h0) {
        return mx >= x0 && mx <= (x0 + w0) && my >= y0 && my <= (y0 + h0);
    }

    private void drawTrendCard(int x0, int y0, int w0, int h0) {
        drawCard(x0, y0, w0, h0, "质量趋势（最近约60秒）");

        int gx = x0 + 10;
        int gy = y0 + 28;
        int gw = w0 - 18;
        int gh = h0 - 38;

        MAIN.stroke(GRID);
        for (int i = 0; i <= 4; i++) {
            float yy = gy + gh * i / 4f;
            MAIN.line(gx, yy, gx + gw, yy);
        }

        MAIN.stroke(PANEL_STROKE);
        MAIN.line(gx, gy + gh, gx + gw, gy + gh);
        MAIN.line(gx, gy, gx, gy + gh);

        int count = trendFilled ? HISTORY_SIZE : trendWrite;
        if (count < 2) {
            MAIN.fill(TEXT_SUB);
            MAIN.textFont(p7);
            MAIN.textSize(10);
            MAIN.text("等待稳定数据...", gx + 4, gy + 4);
            return;
        }

        MAIN.noFill();
        MAIN.stroke(79, 216, 255, 230);
        MAIN.strokeWeight(2f);
        MAIN.beginShape();
        for (int i = 0; i < count; i++) {
            float v = getTrendValue(i, count);
            float xx = gx + gw * i / (float) (count - 1);
            float yy = gy + gh - gh * PApplet.constrain(v / 100f, 0f, 1f);
            MAIN.vertex(xx, yy);
        }
        MAIN.endShape();

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7);
        MAIN.textSize(10);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text("当前值 " + PApplet.nf(globalQuality, 1, 1), gx + 2, gy + 2);
    }

    private void computeQuality() {
        int n = chQuality.length;
        if (n <= 0 || fftBuff == null || fftBuff.length < n || dataProcessingFilteredBuffer == null) {
            return;
        }

        float[] ws = weightsByStrictness();
        int lineFreq = resolveLineFreq();

        float sumQ = 0f;
        float sumE = 0f;
        float sumL = 0f;
        float sumD = 0f;
        float sumS = 0f;
        float sumB = 0f;

        int sampleRate = MAIN.currentBoard.getSampleRate();
        int winSec = WINDOW_SECONDS[windowIndex];
        int winLen = Math.max(16, sampleRate * winSec);

        for (int ch = 0; ch < n; ch++) {
            if (fftBuff[ch] == null || dataProcessingFilteredBuffer[ch] == null) {
                continue;
            }

            float total = 1e-9f;
            float emg = 0f;
            float line = 0f;
            float drift = 0f;

            int spec = fftBuff[ch].specSize();
            for (int k = 1; k < spec; k++) {
                float hz = fftBuff[ch].indexToFreq(k);
                float a = fftBuff[ch].getBand(k);
                float p = a * a;
                total += p;

                if (hz >= 25f && hz <= 45f) {
                    emg += p;
                }
                if (hz >= (lineFreq - 1.0f) && hz <= (lineFreq + 1.0f)) {
                    line += p;
                }
                if (hz >= 0.5f && hz <= 2.0f) {
                    drift += p;
                }
            }

            float emgRatio = PApplet.constrain(emg / total * 2.0f, 0f, 1f);
            float lineRatio = PApplet.constrain(line / total * 6.0f, 0f, 1f);
            float driftRatio = PApplet.constrain(drift / total * 3.0f, 0f, 1f);

            float sat = 0f;
            if (is_railed != null && ch < is_railed.length && is_railed[ch] != null) {
                if (is_railed[ch].is_railed) {
                    sat = 1.0f;
                } else if (is_railed[ch].is_railed_warn) {
                    sat = 0.6f;
                }
            }

            float burst = computeBurstIndex(dataProcessingFilteredBuffer[ch], winLen);

            chEmg[ch] = emgRatio;
            chLine[ch] = lineRatio;
            chDrift[ch] = driftRatio;
            chSaturate[ch] = sat;
            chBurst[ch] = burst;

            float penalty = ws[0] * emgRatio + ws[1] * lineRatio + ws[2] * driftRatio + ws[3] * sat + ws[4] * burst;
            float q = 100f * (1f - PApplet.constrain(penalty, 0f, 1f));
            chQuality[ch] += (q - chQuality[ch]) * 0.18f;

            sumQ += chQuality[ch];
            sumE += emgRatio;
            sumL += lineRatio;
            sumD += driftRatio;
            sumS += sat;
            sumB += burst;
        }

        globalQuality = n > 0 ? sumQ / n : 100f;
        radarVals[0] = n > 0 ? sumE / n : 0f;
        radarVals[1] = n > 0 ? sumL / n : 0f;
        radarVals[2] = n > 0 ? sumD / n : 0f;
        radarVals[3] = n > 0 ? sumS / n : 0f;
        radarVals[4] = n > 0 ? sumB / n : 0f;
    }

    private float computeBurstIndex(float[] data, int winLen) {
        if (data == null || data.length < 3) {
            return 0f;
        }
        int start = Math.max(1, data.length - winLen);
        float mad = 0f;
        float peak = 0f;
        int count = 0;
        for (int i = start; i < data.length; i++) {
            float d = Math.abs(data[i] - data[i - 1]);
            mad += d;
            peak = Math.max(peak, d);
            count++;
        }
        if (count <= 0) {
            return 0f;
        }
        mad /= count;
        if (mad < 1e-9f) {
            return 0f;
        }
        float ratio = peak / (mad * 6.0f);
        return PApplet.constrain(ratio, 0f, 1f);
    }

    private void updateAlert() {
        float maxArtifact = radarVals[0];
        int maxIdx = 0;
        for (int i = 1; i < RADAR_DIM; i++) {
            if (radarVals[i] > maxArtifact) {
                maxArtifact = radarVals[i];
                maxIdx = i;
            }
        }

        if (globalQuality < 55f || radarVals[3] > 0.45f) {
            alertLevel = 2;
        } else if (globalQuality < 75f || maxArtifact > 0.38f) {
            alertLevel = 1;
        } else {
            alertLevel = 0;
        }

        if (alertLevel == 0) {
            alertTitle = "信号稳定";
            alertMsg = "当前信号质量良好，可继续进行连接性和跨频耦合分析。";
            return;
        }

        String[] suggestions = {
                "肌电污染偏高，建议放松面部和颈部肌肉，减少咬牙与皱眉等动作。",
                "工频干扰偏高，建议检查电源环境与导线走向，尽量远离强电设备。",
                "基线漂移偏高，建议保持头部稳定并复查电极贴合状态。",
                "饱和风险偏高，建议降低电极阻抗并检查导联接触是否可靠。",
                "突发伪迹偏高，建议减少突发动作并避免触碰导线。"
        };

        alertTitle = alertLevel == 2 ? "质量警告" : "质量注意";
        alertMsg = suggestions[maxIdx];
    }

    private void ensureChannelBuffers() {
        int n = Math.max(1, nchan);
        if (chQuality.length == n) {
            return;
        }
        chQuality = new float[n];
        chEmg = new float[n];
        chLine = new float[n];
        chDrift = new float[n];
        chSaturate = new float[n];
        chBurst = new float[n];
        Arrays.fill(chQuality, 85f);
    }

    private int resolveLineFreq() {
        if (lineFreqIndex == 1) {
            return 50;
        }
        if (lineFreqIndex == 2) {
            return 60;
        }
        return 50;
    }

    private float[] weightsByStrictness() {
        // 权重: EMG, Line, Drift, Saturate, Burst
        if (strictnessIndex == 0) {
            return new float[]{0.28f, 0.20f, 0.18f, 0.20f, 0.14f};
        }
        if (strictnessIndex == 2) {
            return new float[]{0.22f, 0.18f, 0.16f, 0.28f, 0.16f};
        }
        return new float[]{0.25f, 0.19f, 0.17f, 0.24f, 0.15f};
    }

    private void pushTrend(float v) {
        trend[trendWrite] = v;
        trendWrite = (trendWrite + 1) % HISTORY_SIZE;
        if (trendWrite == 0) {
            trendFilled = true;
        }
    }

    private float getTrendValue(int pos, int count) {
        if (!trendFilled) {
            return trend[pos];
        }
        int idx = (trendWrite + pos) % HISTORY_SIZE;
        return trend[idx];
    }

    private int qualityColor(float score) {
        if (score >= 80f) {
            return GOOD;
        }
        if (score >= 60f) {
            return WARN;
        }
        return BAD;
    }

    private void drawCard(int x0, int y0, int w0, int h0, String title) {
        MAIN.noStroke();
        MAIN.fill(CARD);
        MAIN.rect(x0, y0, w0, h0, 3);
        MAIN.stroke(PANEL_STROKE);
        MAIN.noFill();
        MAIN.rect(x0, y0, w0, h0, 3);

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7);
        MAIN.textSize(11);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text(title, x0 + 8, y0 + 6);
    }

    private void drawWrappedText(String txt, int x0, int y0, int maxW, int lineH) {
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        String[] seg = txt.split("，");
        String line = "";
        int yy = y0;
        for (String s : seg) {
            String cand = line.isEmpty() ? s : (line + "，" + s);
            if (MAIN.textWidth(cand) > maxW && !line.isEmpty()) {
                MAIN.text(line, x0, yy);
                yy += lineH;
                line = s;
            } else {
                line = cand;
            }
        }
        if (!line.isEmpty()) {
            MAIN.text(line, x0, yy);
        }
    }

    private void drawGradientBackground() {
        for (int i = 0; i < h; i++) {
            float t = i / (float) Math.max(1, h - 1);
            int c = lerpRgb(BG_TOP, BG_BOTTOM, t);
            MAIN.stroke((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF);
            MAIN.line(x, y + i, x + w, y + i);
        }

        float pulse = 0.5f + 0.5f * (float) Math.sin(MAIN.frameCount * 0.01f);
        MAIN.noStroke();
        MAIN.fill(79, 216, 255, (int) (16 + 12 * pulse));
        MAIN.ellipse(x + w * 0.18f, y + h * 0.22f, w * 0.36f, h * 0.30f);
        MAIN.fill(108, 125, 255, (int) (12 + 9 * pulse));
        MAIN.ellipse(x + w * 0.84f, y + h * 0.74f, w * 0.30f, h * 0.26f);
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
