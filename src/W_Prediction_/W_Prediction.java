package W_Prediction_;

import Widget_.Widget;
import controlP5.ControlP5;
import Globel.GUI;
import PythonIntegration_.PythonWsClient;

import java.util.Arrays;

import static Globel.GUI.p7;

public class W_Prediction extends Widget {
    private static final String[] MODEL_NAMES = {
            "EEGNet", "EEGViT", "conformer", "EEGDeformer", "LGGNet", "TSception"
    };

    private static final String[] CLASS_LABELS = {
            "清醒", // Alert
            "疲劳", // Fatigue
            "困倦"  // Drowsy
    };

    private static final int COLOR_BG = 245;
    private static final int COLOR_PANEL = 255;
    private static final int COLOR_BORDER = 220;
    private static final int COLOR_TEXT = 30;
    private static final int COLOR_TEXT_SECONDARY = 105;
    private static final int COLOR_GRID = 232;

    private static final int COLOR_SAFE = 0xFF2ECC71;
    private static final int COLOR_WARN = 0xFFF39C12;
    private static final int COLOR_RISK = 0xFFE74C3C;

    private static final int HISTORY_SIZE = 180;
    private static final int STATE_CONFIRM_POINTS = 4;

    private static final int STATE_NORMAL = 0;
    private static final int STATE_ELEVATED = 1;
    private static final int STATE_HIGH_RISK = 2;

    GUI MAIN;
    ControlP5 localCP5;

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

        localCP5 = new ControlP5(MAIN);
        localCP5.setGraphics(MAIN, 0, 0);
        localCP5.setAutoDraw(false);

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

        float rawScore = clamp((float) client.getLatestFatigueScore(), 0f, 100f);
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

        pApplet.pushStyle();
        pApplet.noStroke();
        pApplet.fill(COLOR_BG);
        pApplet.rect(x + 1, y + 1, w - 2, h - 2);

        int pad = 12;
        int panelX = x + pad;
        int panelY = y + pad;
        int panelW = w - pad * 2;
        int panelH = h - pad * 2;

        pApplet.fill(COLOR_PANEL);
        pApplet.stroke(COLOR_BORDER);
        pApplet.strokeWeight(1f);
        pApplet.rect(panelX, panelY, panelW, panelH, 8);

        int topCardH = 86;
        int gap = 10;
        int leftColW = (int) (panelW * 0.64f);
        int rightColW = panelW - leftColW - gap;

        int topX = panelX + 12;
        int topY = panelY + 12;
        int topW = panelW - 24;

        drawTopCard(topX, topY, topW, topCardH);

        int contentY = topY + topCardH + gap;
        int contentH = panelH - (contentY - panelY) - 12;

        drawTrendCard(topX, contentY, leftColW, contentH);
        drawProbCard(topX + leftColW + gap, contentY, rightColW - 12, contentH);

        pApplet.popStyle();
        localCP5.draw();
    }

    private void drawTopCard(int x, int y, int w, int h) {
        int stateColor = getStateColor(stableRiskState);

        pApplet.noStroke();
        pApplet.fill(250);
        pApplet.rect(x, y, w, h, 8);

        pApplet.stroke(COLOR_BORDER);
        pApplet.noFill();
        pApplet.rect(x, y, w, h, 8);

        pApplet.fill(COLOR_TEXT_SECONDARY);
        pApplet.textFont(p7);
        pApplet.textAlign(MAIN.LEFT, MAIN.TOP);
        pApplet.textSize(12);
        pApplet.text("疲劳风险指数", x + 14, y + 12);

        pApplet.fill(COLOR_TEXT);
        pApplet.textSize(34);
        pApplet.text(String.format("%.1f", displayedFatigueScore), x + 14, y + 30);

        pApplet.textSize(12);
        pApplet.fill(COLOR_TEXT_SECONDARY);
        pApplet.text("/ 100", x + 106, y + 48);

        pApplet.fill(stateColor);
        pApplet.textSize(15);
        pApplet.text(getStateText(stableRiskState), x + 14, y + 66);

        PythonWsClient client = PythonWsClient.getInstance();
        String modelText = "当前模型: " + client.getModelType();
        String connText = "WS: " + (client.isConnected() ? "已连接" : "离线");
        double latency = client.getLatestInferenceLatencyMs();
        String latencyText = latency >= 0 ? String.format("推理延迟: %.0f ms", latency) : "推理延迟: --";

        float rightX = x + w - 220;
        drawInfoBadge(rightX, y + 12, 198, 20, modelText, COLOR_TEXT_SECONDARY);
        drawInfoBadge(rightX, y + 38, 94, 20, connText, client.isConnected() ? COLOR_SAFE : COLOR_RISK);
        drawInfoBadge(rightX + 104, y + 38, 94, 20, latencyText, COLOR_TEXT_SECONDARY);
    }

    private void drawInfoBadge(float x, float y, float w, float h, String text, int textColor) {
        pApplet.noStroke();
        pApplet.fill(242);
        pApplet.rect(x, y, w, h, 6);

        pApplet.fill(textColor);
        pApplet.textFont(p7);
        pApplet.textAlign(MAIN.CENTER, MAIN.CENTER);
        pApplet.textSize(11);
        pApplet.text(text, x + w / 2f, y + h / 2f);
    }

    private void drawTrendCard(int x, int y, int w, int h) {
        pApplet.noStroke();
        pApplet.fill(252);
        pApplet.rect(x, y, w, h, 8);

        pApplet.stroke(COLOR_BORDER);
        pApplet.noFill();
        pApplet.rect(x, y, w, h, 8);

        pApplet.fill(COLOR_TEXT_SECONDARY);
        pApplet.textAlign(MAIN.LEFT, MAIN.TOP);
        pApplet.textFont(p7);
        pApplet.textSize(12);
        pApplet.text("趋势曲线 (当前窗口)", x + 12, y + 10);

        int chartX = x + 12;
        int chartY = y + 34;
        int chartW = w - 24;
        int chartH = h - 48;

        drawTrendGrid(chartX, chartY, chartW, chartH);
        drawTrendCurveSegmented(chartX, chartY, chartW, chartH);
    }

    private void drawTrendGrid(int x, int y, int w, int h) {
        pApplet.stroke(COLOR_GRID);
        pApplet.strokeWeight(1f);

        for (int i = 0; i <= 5; i++) {
            float yy = y + h * i / 5f;
            pApplet.line(x, yy, x + w, yy);
        }

        pApplet.stroke(200);
        pApplet.line(x, y, x, y + h);
        pApplet.line(x, y + h, x + w, y + h);

        pApplet.fill(COLOR_TEXT_SECONDARY);
        pApplet.textAlign(MAIN.RIGHT, MAIN.CENTER);
        pApplet.textSize(10);
        pApplet.text("100", x - 6, y);
        pApplet.text("50", x - 6, y + h / 2f);
        pApplet.text("0", x - 6, y + h);
    }

    private void drawTrendCurveSegmented(int x, int y, int w, int h) {
        int count = historyFilled ? HISTORY_SIZE : historyWriteIndex;
        if (count < 2) {
            return;
        }

        for (int i = 1; i < count; i++) {
            int prevIdx = historyToBufferIndex(i - 1, count);
            int currIdx = historyToBufferIndex(i, count);

            float prevVal = clamp(fatigueHistory[prevIdx], 0f, 100f);
            float currVal = clamp(fatigueHistory[currIdx], 0f, 100f);

            float x1 = x + (w * (i - 1) / (float) (count - 1));
            float y1 = y + h - (prevVal / 100f) * h;
            float x2 = x + (w * i / (float) (count - 1));
            float y2 = y + h - (currVal / 100f) * h;

            int segmentState = stateHistory[prevIdx];
            pApplet.stroke(getStateColor(segmentState));
            pApplet.strokeWeight(2f);
            pApplet.line(x1, y1, x2, y2);
        }

        int latestIdx = historyToBufferIndex(count - 1, count);
        float latestVal = clamp(fatigueHistory[latestIdx], 0f, 100f);
        float latestX = x + w;
        float latestY = y + h - (latestVal / 100f) * h;

        pApplet.noStroke();
        pApplet.fill(getStateColor(stateHistory[latestIdx]));
        pApplet.ellipse(latestX, latestY, 6, 6);
    }

    private int historyToBufferIndex(int historyPos, int count) {
        if (!historyFilled) {
            return historyPos;
        }
        return (historyWriteIndex + historyPos) % HISTORY_SIZE;
    }

    private void drawProbCard(int x, int y, int w, int h) {
        pApplet.noStroke();
        pApplet.fill(252);
        pApplet.rect(x, y, w, h, 8);

        pApplet.stroke(COLOR_BORDER);
        pApplet.noFill();
        pApplet.rect(x, y, w, h, 8);

        pApplet.fill(COLOR_TEXT_SECONDARY);
        pApplet.textAlign(MAIN.LEFT, MAIN.TOP);
        pApplet.textFont(p7);
        pApplet.textSize(12);
        pApplet.text("类别概率", x + 12, y + 10);

        int rowH = (h - 46) / 3;
        int barX = x + 12;
        int barW = w - 24;

        for (int i = 0; i < 3; i++) {
            int rowY = y + 30 + i * rowH;
            drawProbRow(i, barX, rowY, barW, rowH - 8);
        }
    }

    private void drawProbRow(int cls, int x, int y, int w, int h) {
        float prob = clamp(displayedProbs[cls], 0f, 1f);
        int c = getClassColor(cls);

        pApplet.fill(COLOR_TEXT);
        pApplet.textAlign(MAIN.LEFT, MAIN.TOP);
        pApplet.textSize(11);
        pApplet.text(CLASS_LABELS[cls], x, y);

        int trackY = y + 16;
        int trackH = Math.max(10, h - 22);

        pApplet.noStroke();
        pApplet.fill(235);
        pApplet.rect(x, trackY, w, trackH, 5);

        pApplet.fill(c);
        pApplet.rect(x, trackY, w * prob, trackH, 5);

        pApplet.fill(COLOR_TEXT_SECONDARY);
        pApplet.textAlign(MAIN.RIGHT, MAIN.TOP);
        pApplet.text(String.format("%.1f%%", prob * 100f), x + w, y);
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

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public void screenResized() {
        super.screenResized();
        localCP5.setGraphics(MAIN, 0, 0);
    }
}