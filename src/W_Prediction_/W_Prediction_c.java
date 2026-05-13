package W_Prediction_;

import Widget_.Widget;
import controlP5.ControlP5;
import Globel.GUI;
import PythonIntegration_.PythonWsClient;
import java.util.Arrays;

import static Globel.GUI.p7;

public class W_Prediction_c extends Widget {


























































































































    GUI MAIN;
    ControlP5 localCP5;
    private final float[] displayedProbs = new float[]{0f, 0f, 0f};

    public W_Prediction_c(GUI MAIN) {
        super(MAIN);
        this.MAIN = MAIN;

        localCP5 = new ControlP5(MAIN);
        localCP5.setGraphics(MAIN, 0, 0);
        localCP5.setAutoDraw(false);
        
        // 增加下拉菜单以供选择模型
        addDropdown("ModelSelect", "推理模型", Arrays.asList("EEGNet", "EEGViT", "conformer", "EEGDeformer", "LGGNet", "TSception"), 0);
    }

    // 该方法名称必须与上面的下拉菜单 ID "ModelSelect" 一致，GUI 事件系统会自动利用反射调用此处
    public void ModelSelect(int n) {
        String[] models = {"EEGNet", "EEGViT", "conformer", "EEGDeformer", "LGGNet", "TSception"};
        if (n >= 0 && n < models.length) {
            PythonWsClient.getInstance().setModelType(models[n]);
        }
    }

    @Override
    public void update() {
        super.update();
    }

    @Override
    public void draw() {
        super.draw();

        pApplet.pushStyle();

        double[] probs = PythonWsClient.getInstance().getLatestProbabilities();
        int numClasses = probs.length;
        ensureDisplayBufferSize(numClasses);

        for (int i = 0; i < numClasses; i++) {
            displayedProbs[i] += ((float) probs[i] - displayedProbs[i]) * 0.18f;
        }

        int dominantIndex = 0;
        for (int i = 1; i < numClasses; i++) {
            if (displayedProbs[i] > displayedProbs[dominantIndex]) {
                dominantIndex = i;
            }
        }

        int panelX = x + 14;
        int panelY = y + 18;
        int panelW = w - 28;
        int panelH = h - 34;
        float panelRadius = 18f;

        drawPanelShadow(panelX, panelY, panelW, panelH, panelRadius);
        drawPanelBackground(panelX, panelY, panelW, panelH, panelRadius);

        float headerX = panelX + 24;
        float headerY = panelY + 20;
        float headerW = panelW - 48;
        float headerH = 74;
        drawHeader(headerX, headerY, headerW, headerH, dominantIndex);

        float chartTop = headerY + headerH + 26;
        float chartBottom = panelY + panelH - 74;
        float chartLeft = panelX + 74;
        float chartRight = panelX + panelW - 34;
        float chartHeight = chartBottom - chartTop;
        float chartWidth = chartRight - chartLeft;

        drawChartFrame(chartLeft, chartTop, chartRight, chartBottom, chartWidth, chartHeight);
        drawBars(chartLeft, chartBottom, chartWidth, chartHeight, dominantIndex);
        drawFooter(panelX, panelY, panelW, panelH, dominantIndex);

        pApplet.popStyle();
        localCP5.draw();
    }

    private void ensureDisplayBufferSize(int numClasses) {
        if (numClasses != displayedProbs.length) {
            return;
        }
    }

    private void drawPanelShadow(float panelX, float panelY, float panelW, float panelH, float radius) {
        pApplet.noStroke();
        pApplet.fill(0, 0, 0, 30);
        pApplet.rect(panelX + 8, panelY + 10, panelW, panelH, radius);
        pApplet.fill(0, 0, 0, 18);
        pApplet.rect(panelX + 4, panelY + 5, panelW, panelH, radius);
    }

    private void drawPanelBackground(float panelX, float panelY, float panelW, float panelH, float radius) {
        pApplet.noStroke();
        pApplet.fill(255);
        pApplet.rect(panelX, panelY, panelW, panelH, radius);

        pApplet.stroke(200);
        pApplet.strokeWeight(1.2f);
        pApplet.noFill();
        pApplet.rect(panelX, panelY, panelW, panelH, radius);

        pApplet.noStroke();
        pApplet.fill(240, 240, 240, 110);
        pApplet.rect(panelX + 1, panelY + 1, panelW - 2, 44, radius, radius, 8, 8);
    }

    private void drawHeader(float headerX, float headerY, float headerW, float headerH, int dominantIndex) {
        pApplet.noStroke();
        pApplet.fill(245);
        pApplet.rect(headerX, headerY, headerW, headerH, 14f);

        pApplet.textFont(p7);
        pApplet.fill(100);
        pApplet.textAlign(MAIN.LEFT, MAIN.TOP);
        pApplet.textSize(11);
        pApplet.text("实时脑电分类预测", headerX + 18, headerY + 14);

        pApplet.fill(0);
        pApplet.textSize(21);
        pApplet.text(getClassLabel(dominantIndex), headerX + 18, headerY + 32);

        float confidence = displayedProbs[dominantIndex] * 100f;
        String confidenceText = String.format("%.1f%%", confidence);
        float badgeW = 106;
        float badgeH = 34;
        float badgeX = headerX + headerW - badgeW - 18;
        float badgeY = headerY + 20;

        fillColorWithAlpha(pApplet.color(0), 34);
        pApplet.rect(badgeX, badgeY, badgeW, badgeH, 17f);
        strokeColorWithAlpha(pApplet.color(0), 90);
        pApplet.strokeWeight(1.2f);
        pApplet.noFill();
        pApplet.rect(badgeX, badgeY, badgeW, badgeH, 17f);

        pApplet.noStroke();
        pApplet.fill(0);
        pApplet.textAlign(MAIN.CENTER, MAIN.CENTER);
        pApplet.textSize(15);
        pApplet.text(confidenceText, badgeX + badgeW / 2f, badgeY + badgeH / 2f - 1);
    }

    private void drawChartFrame(float chartLeft, float chartTop, float chartRight, float chartBottom, float chartWidth, float chartHeight) {
        pApplet.textFont(p7);
        pApplet.textSize(11);

        for (int i = 0; i <= 5; i++) {
            float val = i * 0.2f;
            float tickY = chartBottom - (val * chartHeight);

            pApplet.stroke(i == 0 ? pApplet.color(150) : pApplet.color(220));
            pApplet.strokeWeight(i == 0 ? 1.5f : 1f);
            pApplet.line(chartLeft, tickY, chartRight, tickY);

            pApplet.noStroke();
            pApplet.fill(100);
            pApplet.textAlign(MAIN.RIGHT, MAIN.CENTER);
            pApplet.text(String.format("%.0f%%", val * 100), chartLeft - 12, tickY);
        }

        pApplet.stroke(100);
        pApplet.strokeWeight(1.6f);
        pApplet.line(chartLeft, chartTop, chartLeft, chartBottom);
        pApplet.line(chartLeft, chartBottom, chartRight, chartBottom);

        pApplet.noStroke();
        pApplet.fill(100);
        pApplet.textSize(12);
        pApplet.textAlign(MAIN.LEFT, MAIN.BOTTOM);
        pApplet.text("概率", chartLeft - 16, chartTop - 10);
        pApplet.textAlign(MAIN.RIGHT, MAIN.BOTTOM);
        pApplet.text("分类结果", chartRight + 15, chartBottom + 19);

        pApplet.stroke(220);
        pApplet.strokeWeight(1f);
        for (int i = 1; i < 10; i++) {
            float guideX = chartLeft + chartWidth * i / 10f;
            pApplet.line(guideX, chartTop, guideX, chartBottom);
        }
    }

    private void drawBars(float chartLeft, float chartBottom, float chartWidth, float chartHeight, int dominantIndex) {
        int numClasses = displayedProbs.length;
        float groupWidth = chartWidth / numClasses;
        float barWidth = Math.min(72f, groupWidth * 0.55f);
        float pulse = 0.88f + 0.12f * (float) Math.sin(MAIN.frameCount * 0.08f);

        for (int i = 0; i < numClasses; i++) {
            float barHeight = displayedProbs[i] * chartHeight;
            float currentX = chartLeft + i * groupWidth + (groupWidth - barWidth) / 2f;
            float barTop = chartBottom - barHeight;
            int accent = getAccentColor(i);
            boolean isDominant = i == dominantIndex;

            pApplet.noStroke();
            pApplet.fill(240);
            pApplet.rect(currentX, chartBottom - chartHeight, barWidth, chartHeight, 16f);

            pApplet.fill(accent);
            pApplet.rect(currentX, barTop, barWidth, barHeight, 16f);

            pApplet.fill(0);
            pApplet.textAlign(MAIN.CENTER, MAIN.BOTTOM);
            pApplet.textSize(isDominant ? 14 : 12);
            pApplet.text(String.format("%.1f%%", displayedProbs[i] * 100f), currentX + barWidth / 2f, barTop - 12);

            pApplet.fill(isDominant ? pApplet.color(0) : pApplet.color(100));
            pApplet.textAlign(MAIN.CENTER, MAIN.TOP);
            pApplet.textSize(12);
            pApplet.text(getClassLabel(i), currentX + barWidth / 2f, chartBottom + 5);
        }
    }

    private void drawFooter(float panelX, float panelY, float panelW, float panelH, int dominantIndex) {
        float footerY = panelY + panelH - 44;

        pApplet.stroke(220);
        pApplet.strokeWeight(1f);
        pApplet.line(panelX + 24, footerY - 12, panelX + panelW - 24, footerY - 12);

        pApplet.noStroke();
        pApplet.fill(0);
        pApplet.ellipse(panelX + 32, footerY + 3, 9, 9);

        pApplet.textFont(p7);
        pApplet.textAlign(MAIN.LEFT, MAIN.CENTER);
        pApplet.fill(100);
        pApplet.textSize(11);
        pApplet.text("当前输出", panelX + 44, footerY + 2);

        pApplet.fill(0);
        pApplet.textSize(13);
        pApplet.text(getClassLabel(dominantIndex) + "  ·  置信度 " + String.format("%.1f%%", displayedProbs[dominantIndex] * 100f), panelX + 104, footerY + 2);
    }

    private void fillColorWithAlpha(int color, int alpha) {
        pApplet.fill(pApplet.red(color), pApplet.green(color), pApplet.blue(color), alpha);
    }

    private void strokeColorWithAlpha(int color, int alpha) {
        pApplet.stroke(pApplet.red(color), pApplet.green(color), pApplet.blue(color), alpha);
    }

    private int getAccentColor(int index) {
        int[] colors = {
                pApplet.color(63, 163, 255),
                pApplet.color(132, 92, 255),
                pApplet.color(31, 201, 151)
        };
        return colors[index % colors.length];
    }

    private String getClassLabel(int index) {
        String[] labels = {"清醒", "疲劳", "困倦"};
        if (index >= 0 && index < labels.length) {
            return labels[index];
        }
        return "Class " + index;
    }

    @Override
    public void screenResized() {
        super.screenResized();
        localCP5.setGraphics(MAIN, 0, 0);
    }
}
