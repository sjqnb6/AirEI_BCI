package W_Prediction_;

import Widget_.Widget;
import Globel.GUI;
import PythonIntegration_.PythonWsClient;
import processing.core.PApplet;

import java.util.Arrays;

import static Globel.GUI.*;

public class W_Prediction extends Widget {
    private static final String[] MODEL_NAMES = {
            "EEGNet", "EEGViT", "conformer", "EEGDeformer", "LGGNet", "TSception"
    };

    private static final String[] CLASS_LABELS = {
            "清醒",
            "疲劳",
            "昏睡",
    };

    private static final int BG_TOP = 0xFF0A1220;
    private static final int BG_BOTTOM = 0xFF0D1830;
    private static final int PANEL = 0xCC13243F;
    private static final int PANEL_STROKE = 0x6683A2CC;
    private static final int TEXT_MAIN = 0xFFEAF2FF;
    private static final int TEXT_SUB = 0xFF98AECE;
    private static final int GRID_LINE = 0x2D90AED8;
    private static final int ACCENT = 0xFF4FD8FF;
    private static final int ACCENT_SOFT = 0x4460E2FF;
    private static final int CARD_BG = 0x2A163153;

    private static final int COLOR_SAFE = 0xFF5AE88A;
    private static final int COLOR_WARN = 0xFFFFD866;
    private static final int COLOR_RISK = 0xFFFF6B6B;

    private static final int HISTORY_SIZE = 180;
    private static final int TREND_X_TICKS = 6;
    private static final int STATE_CONFIRM_POINTS = 4;

    private static final int STATE_NORMAL = 0;
    private static final int STATE_ELEVATED = 1;
    private static final int STATE_HIGH_RISK = 2;

    private final GUI MAIN;

    private final float[] displayedProbs = new float[]{0f, 0f, 0f};
    private float displayedFatigueScore = 0f;

    private final float[] fatigueHistory = new float[HISTORY_SIZE];
    private final int[] stateHistory = new int[HISTORY_SIZE];
    private int historyWriteIndex = 0;
    private boolean historyFilled = false;

    private int stableRiskState = STATE_NORMAL;
    private int pendingRiskState = STATE_NORMAL;
    private int pendingRiskCount = 0;

    public W_Prediction(GUI MAIN) {
        super(MAIN);
        this.MAIN = MAIN;

        addDropdown("ModelSelect", "模型", Arrays.asList(MODEL_NAMES), 0);
    }

    public void ModelSelect(int n) {
        if (n >= 0 && n < MODEL_NAMES.length) {
            PythonWsClient.getInstance().setModelType(MODEL_NAMES[n]);
        }
    }

    @Override
    public void update() {
        super.update();

        PythonWsClient client = PythonWsClient.getInstance();
        double[] probs = client.getLatestProbabilities();

        for (int i = 0; i < displayedProbs.length && i < probs.length; i++) {
            displayedProbs[i] += (((float) probs[i]) - displayedProbs[i]) * 0.18f;
        }

        float rawScore = PApplet.constrain((float) client.getLatestFatigueScore(), 0f, 100f);
        displayedFatigueScore += (rawScore - displayedFatigueScore) * 0.12f;

        updateStableRiskState(rawScore);

        fatigueHistory[historyWriteIndex] = displayedFatigueScore;
        stateHistory[historyWriteIndex] = stableRiskState;

        historyWriteIndex = (historyWriteIndex + 1) % HISTORY_SIZE;
        if (historyWriteIndex == 0) {
            historyFilled = true;
        }
    }

    private void updateStableRiskState(float score) {
        int rawState = classifyState(score);

        if (rawState == stableRiskState) {
            pendingRiskState = rawState;
            pendingRiskCount = 0;
            return;
        }

        if (rawState != pendingRiskState) {
            pendingRiskState = rawState;
            pendingRiskCount = 1;
        } else {
            pendingRiskCount++;
        }

        if (pendingRiskCount >= STATE_CONFIRM_POINTS) {
            stableRiskState = pendingRiskState;
            pendingRiskCount = 0;
        }
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

        int topCardH = 90;
        int gap = 8;
        int leftW = (int) (panelW * 0.64f);
        int rightW = panelW - leftW - gap - 20;

        drawTopCard(panelX + 10, contentY, panelW - 20, topCardH);

        int lowerY = contentY + topCardH + gap;
        int lowerH = contentH - topCardH - gap;

        drawTrendCard(panelX + 10, lowerY, leftW, lowerH);
        drawProbCard(panelX + 10 + leftW + gap, lowerY, rightW, lowerH);

        MAIN.popStyle();
    }

    private void drawHeader(int panelX, int panelY, int panelW) {
        MAIN.fill(TEXT_MAIN);
        MAIN.textFont(p7);
        MAIN.textSize(15);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text("脑电疲劳检测", panelX + 12, panelY + 9);

        PythonWsClient client = PythonWsClient.getInstance();
        MAIN.fill(TEXT_SUB);
        MAIN.textSize(11);
        MAIN.text(
                "模型 " + client.getModelType()
                        + " | WS " + (client.isConnected() ? "在线" : "离线")
                        + " | 状态 " + getStateText(stableRiskState),
                panelX + 12, panelY + 29
        );

        MAIN.stroke(PANEL_STROKE);
        MAIN.line(panelX + 10, panelY + 44, panelX + panelW - 10, panelY + 44);
    }

    private void drawTopCard(int x0, int y0, int w0, int h0) {
        MAIN.noStroke();
        MAIN.fill(CARD_BG);
        MAIN.rect(x0, y0, w0, h0, 3);
        MAIN.stroke(PANEL_STROKE);
        MAIN.noFill();
        MAIN.rect(x0, y0, w0, h0, 3);

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7);
        MAIN.textSize(11);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text("疲劳风险指数", x0 + 10, y0 + 8);

        int stateColor = getStateColor(stableRiskState);

        MAIN.fill(TEXT_MAIN);
        MAIN.textSize(32);
        MAIN.text(String.format("%.1f", displayedFatigueScore), x0 + 10, y0 + 28);

        MAIN.fill(TEXT_SUB);
        MAIN.textSize(12);
        MAIN.text("/ 100", x0 + 108, y0 + 46);

        MAIN.fill(stateColor);
        MAIN.textSize(14);
        MAIN.text(getStateText(stableRiskState), x0 + 10, y0 + 66);

        PythonWsClient client = PythonWsClient.getInstance();
        double latency = client.getLatestInferenceLatencyMs();
        String latencyText = latency >= 0 ? String.format("推理延迟: %.0f ms", latency) : "推理延迟: --";

        float rightX = x0 + w0 - 210;
        drawInfoBadge(rightX, y0 + 12, 190, 22, "当前模型: " + client.getModelType(), TEXT_SUB);
        drawInfoBadge(rightX, y0 + 40, 90, 22, "WS: " + (client.isConnected() ? "在线" : "离线"),
                client.isConnected() ? COLOR_SAFE : COLOR_RISK);
        drawInfoBadge(rightX + 100, y0 + 40, 90, 22, latencyText, TEXT_SUB);
    }

    private void drawInfoBadge(float bx, float by, float bw, float bh, String text, int textColor) {
        MAIN.noStroke();
        MAIN.fill(0x33FFFFFF);
        MAIN.rect(bx, by, bw, bh, 2);

        MAIN.stroke(PANEL_STROKE);
        MAIN.strokeWeight(1f);
        MAIN.noFill();
        MAIN.rect(bx, by, bw, bh, 2);

        MAIN.fill(textColor);
        MAIN.textFont(p7);
        MAIN.textAlign(PApplet.CENTER, PApplet.CENTER);
        MAIN.textSize(10);
        MAIN.text(text, bx + bw / 2f, by + bh / 2f);
    }

    private void drawTrendCard(int x0, int y0, int w0, int h0) {
        MAIN.noStroke();
        MAIN.fill(CARD_BG);
        MAIN.rect(x0, y0, w0, h0, 3);
        MAIN.stroke(PANEL_STROKE);
        MAIN.noFill();
        MAIN.rect(x0, y0, w0, h0, 3);

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7);
        MAIN.textSize(11);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text("疲劳趋势曲线", x0 + 8, y0 + 6);

        int chartX = x0 + 36;
        int chartY = y0 + 28;
        int chartW = w0 - 48;
        int chartH = h0 - 50;

        int count = historyFilled ? HISTORY_SIZE : historyWriteIndex;

        drawTrendGrid(chartX, chartY, chartW, chartH, count);
        drawTrendCurve(chartX, chartY, chartW, chartH);
    }

    private void drawTrendGrid(int gx, int gy, int gw, int gh, int count) {
        MAIN.stroke(GRID_LINE);
        MAIN.strokeWeight(1f);
        for (int i = 0; i <= 5; i++) {
            float yy = gy + gh * i / 5f;
            MAIN.line(gx, yy, gx + gw, yy);
        }

        MAIN.stroke(PANEL_STROKE);
        MAIN.strokeWeight(1.2f);
        MAIN.line(gx, gy, gx, gy + gh);
        MAIN.line(gx, gy + gh, gx + gw, gy + gh);

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7);
        MAIN.textSize(10);
        MAIN.textAlign(PApplet.RIGHT, PApplet.CENTER);
        for (int i = 0; i <= 5; i++) {
            int label = 100 - i * 20;
            float yy = gy + gh * i / 5f;
            MAIN.text(String.valueOf(label), gx - 6, yy);
        }

        MAIN.textAlign(PApplet.CENTER, PApplet.TOP);
        for (int i = 0; i <= TREND_X_TICKS; i++) {
            float xx = gx + gw * i / (float) TREND_X_TICKS;
            MAIN.stroke(GRID_LINE);
            MAIN.line(xx, gy + gh, xx, gy + gh + 4);
            if (i == TREND_X_TICKS) {
                MAIN.text("当前", xx - 6, gy + gh + 6);
            } else {
                int step = TREND_X_TICKS - i;
                MAIN.text("-" + step + "s", xx, gy + gh + 6);
            }
        }

        if (count < 2) {
            MAIN.fill(TEXT_SUB);
            MAIN.textFont(p7);
            MAIN.textSize(10);
            MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
            MAIN.text("等待稳定数据...", gx + 4, gy + 4);
        }
    }

    private void drawTrendCurve(int gx, int gy, int gw, int gh) {
        int count = historyFilled ? HISTORY_SIZE : historyWriteIndex;
        if (count < 2) {
            return;
        }

        for (int i = 1; i < count; i++) {
            int prevIdx = historyToBufferIndex(i - 1, count);
            int currIdx = historyToBufferIndex(i, count);

            float prevVal = PApplet.constrain(fatigueHistory[prevIdx], 0f, 100f);
            float currVal = PApplet.constrain(fatigueHistory[currIdx], 0f, 100f);

            float x1 = gx + (gw * (i - 1) / (float) (count - 1));
            float y1 = gy + gh - (prevVal / 100f) * gh;
            float x2 = gx + (gw * i / (float) (count - 1));
            float y2 = gy + gh - (currVal / 100f) * gh;

            int segColor = getStateColor(stateHistory[prevIdx]);
            MAIN.stroke(segColor);
            MAIN.strokeWeight(2.2f);
            MAIN.line(x1, y1, x2, y2);
        }

        int latestIdx = historyToBufferIndex(count - 1, count);
        float latestVal = PApplet.constrain(fatigueHistory[latestIdx], 0f, 100f);
        float latestX = gx + gw;
        float latestY = gy + gh - (latestVal / 100f) * gh;

        float pulse = 0.7f + 0.3f * (float) Math.sin(MAIN.frameCount * 0.08f);
        int dotColor = getStateColor(stateHistory[latestIdx]);
        MAIN.noStroke();
        MAIN.fill((dotColor >> 16) & 0xFF, (dotColor >> 8) & 0xFF, dotColor & 0xFF, (int) (220 * pulse));
        MAIN.ellipse(latestX, latestY, 8, 8);
    }

    private int historyToBufferIndex(int historyPos, int count) {
        if (!historyFilled) {
            return historyPos;
        }
        return (historyWriteIndex + historyPos) % HISTORY_SIZE;
    }

    private void drawProbCard(int x0, int y0, int w0, int h0) {
        MAIN.noStroke();
        MAIN.fill(CARD_BG);
        MAIN.rect(x0, y0, w0, h0, 3);
        MAIN.stroke(PANEL_STROKE);
        MAIN.noFill();
        MAIN.rect(x0, y0, w0, h0, 3);

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7);
        MAIN.textSize(11);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text("类别概率", x0 + 8, y0 + 6);

        int rowH = (h0 - 40) / 3;
        int barX = x0 + 10;
        int barW = w0 - 20;

        for (int i = 0; i < 3; i++) {
            int rowY = y0 + 28 + i * rowH;
            drawProbRow(i, barX, rowY, barW, rowH - 6);
        }
    }

    private void drawProbRow(int cls, int rx, int ry, int rw, int rh) {
        float prob = PApplet.constrain(displayedProbs[cls], 0f, 1f);
        int c = getClassColor(cls);

        MAIN.fill(TEXT_MAIN);
        MAIN.textFont(p7);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.textSize(11);
        MAIN.text(CLASS_LABELS[cls], rx, ry);

        int trackY = ry + 16;
        int trackH = Math.max(8, rh - 20);

        MAIN.noStroke();
        MAIN.fill(0x33FFFFFF);
        MAIN.rect(rx, trackY, rw, trackH, 2);

        MAIN.fill(c);
        MAIN.rect(rx, trackY, rw * prob, trackH, 2);

        MAIN.stroke(PANEL_STROKE);
        MAIN.strokeWeight(1f);
        MAIN.noFill();
        MAIN.rect(rx, trackY, rw, trackH, 2);

        MAIN.fill(TEXT_SUB);
        MAIN.textAlign(PApplet.RIGHT, PApplet.TOP);
        MAIN.textSize(10);
        MAIN.text(String.format("%.1f%%", prob * 100f), rx + rw, ry);
    }

    private int classifyState(float score) {
        if (score >= 70f) return STATE_HIGH_RISK;
        if (score >= 40f) return STATE_ELEVATED;
        return STATE_NORMAL;
    }

    private int getStateColor(int state) {
        if (state == STATE_HIGH_RISK) return COLOR_RISK;
        if (state == STATE_ELEVATED) return COLOR_WARN;
        return COLOR_SAFE;
    }

    private String getStateText(int state) {
        if (state == STATE_HIGH_RISK) return "高风险预警";
        if (state == STATE_ELEVATED) return "风险升高";
        return "正常";
    }

    private int getClassColor(int cls) {
        if (cls == 0) return COLOR_SAFE;
        if (cls == 1) return COLOR_WARN;
        return COLOR_RISK;
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
        return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }
}
