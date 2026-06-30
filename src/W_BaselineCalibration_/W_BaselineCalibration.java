package W_BaselineCalibration_;

import Globel.GUI;
import Widget_.Widget;
import controlP5.Button;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlP5;
import controlP5.Controller;
import processing.core.PApplet;
import processing.data.JSONArray;
import processing.data.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static Globel.GUI.*;

public class W_BaselineCalibration extends Widget {
    private static final int STAGE_COUNT = 3;
    private static final int BAND_COUNT = 5;
    private static final int HISTORY_SIZE = 180;
    private static final int UPDATE_INTERVAL_MS = 250;

    private static final String[] STAGE_NAMES = {"睁眼静息", "闭眼静息", "专注任务"};
    private static final String[] STAGE_HINTS = {
            "保持自然睁眼，注视固定点",
            "轻闭双眼，减少面部用力",
            "保持注视并进行心算或目标注意"
    };
    private static final String[] BAND_NAMES = {"Delta", "Theta", "Alpha", "Beta", "Gamma"};
    private static final String[] DURATION_LABELS = {"15秒", "30秒", "60秒"};
    private static final int[] DURATION_SECONDS = {15, 30, 60};
    private static final String[] PROTOCOL_LABELS = {"完整三段", "静息两段", "快速单段"};
    private static final int[] PROTOCOL_STAGE_COUNT = {3, 2, 1};
    private static final String[] QUALITY_LABELS = {"宽松 60", "标准 75", "严格 85"};
    private static final float[] QUALITY_GATE = {60f, 75f, 85f};

    private static final int BG_TOP = 0xFF0A1220;
    private static final int BG_BOTTOM = 0xFF0E1A30;
    private static final int PANEL = 0xCC13243F;
    private static final int PANEL_STROKE = 0x6683A2CC;
    private static final int CARD = 0x2A163153;
    private static final int TEXT_MAIN = 0xFFEAF2FF;
    private static final int TEXT_SUB = 0xFF98AECE;
    private static final int GRID = 0x2D90AED8;
    private static final int GOOD = 0xFF56D79E;
    private static final int WARN = 0xFFE9B45A;
    private static final int BAD = 0xFFEE6A74;
    private static final int ACCENT = 0xFF57D9FF;
    private static final int[] BAND_COLORS = {
            0xFF4FB4FF,
            0xFF4FD9C6,
            0xFF6BE47B,
            0xFFFFC36D,
            0xFFFF7676
    };

    private final GUI MAIN;
    private final ControlP5 localCp5;
    private final List<Controller> cp5Elements = new ArrayList<Controller>();

    private Button startPauseButton;
    private Button nextButton;
    private Button saveButton;
    private Button resetButton;

    private int durationIndex = 1;
    private int protocolIndex = 0;
    private int qualityIndex = 1;

    private int currentStage = 0;
    private boolean capturing = false;
    private boolean paused = false;
    private boolean saved = false;
    private long lastUpdateMs = 0L;
    private long noticeMs = 0L;
    private String notice = "请按顺序完成个体基线采集。";

    private final boolean[] stageDone = new boolean[STAGE_COUNT];
    private final float[][] bandSums = new float[STAGE_COUNT][BAND_COUNT];
    private final float[] rmsSums = new float[STAGE_COUNT];
    private final float[] qualitySums = new float[STAGE_COUNT];
    private final float[] thetaBetaSums = new float[STAGE_COUNT];
    private final float[] alphaBetaSums = new float[STAGE_COUNT];
    private final int[] acceptedFrames = new int[STAGE_COUNT];
    private final int[] rejectedFrames = new int[STAGE_COUNT];
    private final int[] elapsedMs = new int[STAGE_COUNT];

    private final float[] currentBands = new float[BAND_COUNT];
    private float currentTotalPower = 0f;
    private float currentQuality = 0f;
    private float currentRms = 0f;
    private float currentThetaBeta = 0f;
    private float currentAlphaBeta = 0f;
    private float currentArtifactRisk = 0f;
    private float currentSaturationRisk = 0f;
    private int activeChannels = 0;

    private final float[] qualityHistory = new float[HISTORY_SIZE];
    private final float[] alphaHistory = new float[HISTORY_SIZE];
    private int historyWrite = 0;
    private boolean historyFilled = false;

    public W_BaselineCalibration(GUI MAIN) {
        super(MAIN);
        this.MAIN = MAIN;

        addDropdown("BaselineDuration", "时长", Arrays.asList(DURATION_LABELS), durationIndex);
        addDropdown("BaselineProtocol", "流程", Arrays.asList(PROTOCOL_LABELS), protocolIndex);
        addDropdown("BaselineQualityGate", "质量门限", Arrays.asList(QUALITY_LABELS), qualityIndex);

        localCp5 = new ControlP5(MAIN);
        localCp5.setGraphics(MAIN, 0, 0);
        localCp5.setAutoDraw(false);
        createButtons();
    }

    public void BaselineDuration(int n) {
        if (!capturing) {
            durationIndex = PApplet.constrain(n, 0, DURATION_LABELS.length - 1);
        }
    }

    public void BaselineProtocol(int n) {
        if (!capturing) {
            protocolIndex = PApplet.constrain(n, 0, PROTOCOL_LABELS.length - 1);
            currentStage = Math.min(currentStage, getActiveStageCount() - 1);
        }
    }

    public void BaselineQualityGate(int n) {
        qualityIndex = PApplet.constrain(n, 0, QUALITY_LABELS.length - 1);
    }

    @Override
    public void update() {
        super.update();

        lockElementsOnOverlapCheck(cp5Elements);
        updateButtonPositions();
        updateButtonLabels();

        long now = MAIN.millis();
        if (now - lastUpdateMs < UPDATE_INTERVAL_MS) {
            return;
        }
        lastUpdateMs = now;

        computeCurrentMetrics();
        pushHistory();

        if (!capturing || paused) {
            return;
        }

        elapsedMs[currentStage] += UPDATE_INTERVAL_MS;
        if (currentQuality >= QUALITY_GATE[qualityIndex] && MAIN.currentBoard.isStreaming()) {
            accumulateFrame(currentStage);
        } else {
            rejectedFrames[currentStage]++;
        }

        if (getStageProgress(currentStage) >= 1f) {
            stageDone[currentStage] = acceptedFrames[currentStage] > 0;
            capturing = false;
            paused = false;
            saved = false;
            setNotice(STAGE_NAMES[currentStage] + " 已完成，可进入下一阶段。");
        }
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
        int leftW = (int) (panelW * 0.30f);
        int midW = (int) (panelW * 0.34f);
        int rightW = panelW - leftW - midW - 34;

        drawProtocolCard(panelX + 10, contentY, leftW, contentH);
        drawLiveCard(panelX + leftW + 18, contentY, midW, contentH);
        drawResultCard(panelX + leftW + midW + 26, contentY, rightW, contentH);

        localCp5.draw();
        MAIN.popStyle();
    }

    @Override
    public void screenResized() {
        super.screenResized();
        localCp5.setGraphics(pApplet, 0, 0);
        updateButtonPositions();
    }

    private void createButtons() {
        startPauseButton = createActionButton("baselineStartPause", "开始", 0, 0, 78);
        startPauseButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent event) {
                toggleCapture();
            }
        });

        nextButton = createActionButton("baselineNext", "下一阶段", 0, 0, 86);
        nextButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent event) {
                goNextStage();
            }
        });

        saveButton = createActionButton("baselineSave", "保存基线", 0, 0, 86);
        saveButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent event) {
                saveProfile();
            }
        });

        resetButton = createActionButton("baselineReset", "重置", 0, 0, 64);
        resetButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent event) {
                resetCalibration();
            }
        });
    }

    private Button createActionButton(String name, String text, int bx, int by, int bw) {
        Button b = MAIN.createButton(localCp5, name, text, bx, by, bw, navH - 3, p7, 12, MAIN.colorNotPressed, MAIN.OPENBCI_DARKBLUE);
        b.setColorBackground(0xFF223A5D);
        b.setColorForeground(0xFF2C4E78);
        b.setColorActive(0xFF38628E);
        b.setBorderColor(0xFF82A3CC);
        cp5Elements.add(b);
        return b;
    }

    private void updateButtonPositions() {
        int by = y0 + navH + 1;
        int bx = x0 + 6;
        startPauseButton.setPosition(bx, by);
        nextButton.setPosition(bx + 84, by);
        saveButton.setPosition(bx + 176, by);
        resetButton.setPosition(bx + 268, by);
    }

    private void updateButtonLabels() {
        startPauseButton.getCaptionLabel().setText(capturing && !paused ? "暂停" : (paused ? "继续" : "开始"));
        nextButton.getCaptionLabel().setText(currentStage >= getActiveStageCount() - 1 ? "完成" : "下一阶段");
        saveButton.setLock(!hasAnyStageDone());
        saveButton.setColorBackground(hasAnyStageDone() ? 0xFF245A67 : 0xFF4A5362);
    }

    private void toggleCapture() {
        if (!MAIN.currentBoard.isStreaming()) {
            setNotice("请先开始数据流，再进行基线校准。");
            return;
        }
        if (stageDone[currentStage]) {
            clearStage(currentStage);
        }
        if (!capturing) {
            capturing = true;
            paused = false;
            saved = false;
            setNotice("正在采集 " + STAGE_NAMES[currentStage] + "。");
            return;
        }
        paused = !paused;
        setNotice(paused ? "采集已暂停。" : "采集已继续。");
    }

    private void goNextStage() {
        capturing = false;
        paused = false;
        if (currentStage < getActiveStageCount() - 1) {
            currentStage++;
            setNotice("已切换到 " + STAGE_NAMES[currentStage] + "。");
            return;
        }
        setNotice(hasAnyStageDone() ? "校准流程已完成，可保存基线。" : "还没有可保存的有效基线。");
    }

    private void resetCalibration() {
        capturing = false;
        paused = false;
        saved = false;
        currentStage = 0;
        for (int i = 0; i < STAGE_COUNT; i++) {
            clearStage(i);
        }
        for (int i = 0; i < HISTORY_SIZE; i++) {
            qualityHistory[i] = 0f;
            alphaHistory[i] = 0f;
        }
        historyWrite = 0;
        historyFilled = false;
        setNotice("基线校准已重置。");
    }

    private void clearStage(int stage) {
        stageDone[stage] = false;
        acceptedFrames[stage] = 0;
        rejectedFrames[stage] = 0;
        elapsedMs[stage] = 0;
        rmsSums[stage] = 0f;
        qualitySums[stage] = 0f;
        thetaBetaSums[stage] = 0f;
        alphaBetaSums[stage] = 0f;
        for (int b = 0; b < BAND_COUNT; b++) {
            bandSums[stage][b] = 0f;
        }
    }

    private void computeCurrentMetrics() {
        for (int b = 0; b < BAND_COUNT; b++) {
            currentBands[b] = 0f;
        }
        currentTotalPower = 0f;
        currentRms = 0f;
        currentSaturationRisk = 0f;
        activeChannels = 0;

        if (dataProcessing == null || dataProcessing.avgPowerInBins == null || MAIN.currentBoard == null) {
            currentQuality = 0f;
            return;
        }

        int maxChannels = Math.min(nchan, dataProcessing.avgPowerInBins.length);
        float gammaRatioSum = 0f;
        for (int ch = 0; ch < maxChannels; ch++) {
            if (!MAIN.currentBoard.isEXGChannelActive(ch) || dataProcessing.avgPowerInBins[ch] == null) {
                continue;
            }
            activeChannels++;
            float chTotal = 0f;
            for (int b = 0; b < BAND_COUNT && b < dataProcessing.avgPowerInBins[ch].length; b++) {
                float v = safe(dataProcessing.avgPowerInBins[ch][b]);
                currentBands[b] += v;
                chTotal += v;
            }
            currentRms += dataProcessing.data_std_uV != null && ch < dataProcessing.data_std_uV.length ? safe(dataProcessing.data_std_uV[ch]) : 0f;
            gammaRatioSum += chTotal > 1e-9f && dataProcessing.avgPowerInBins[ch].length > 4
                    ? safe(dataProcessing.avgPowerInBins[ch][4]) / chTotal
                    : 0f;
            if (is_railed != null && ch < is_railed.length && is_railed[ch] != null) {
                if (is_railed[ch].is_railed) {
                    currentSaturationRisk += 1f;
                } else if (is_railed[ch].is_railed_warn) {
                    currentSaturationRisk += 0.55f;
                }
            }
        }

        if (activeChannels <= 0) {
            currentQuality = 0f;
            return;
        }

        for (int b = 0; b < BAND_COUNT; b++) {
            currentBands[b] /= activeChannels;
            currentTotalPower += currentBands[b];
        }
        currentRms /= activeChannels;
        currentSaturationRisk /= activeChannels;
        float gammaRatio = gammaRatioSum / activeChannels;

        currentThetaBeta = currentBands[1] / Math.max(1e-9f, currentBands[3]);
        currentAlphaBeta = currentBands[2] / Math.max(1e-9f, currentBands[3]);

        float rmsHighRisk = PApplet.constrain((currentRms - 35f) / 95f, 0f, 1f);
        float rmsLowRisk = PApplet.constrain((1.0f - currentRms) / 1.0f, 0f, 1f);
        float highFreqRisk = PApplet.constrain(gammaRatio * 3.0f, 0f, 1f);
        currentArtifactRisk = PApplet.constrain(0.45f * highFreqRisk + 0.35f * rmsHighRisk + 0.20f * rmsLowRisk, 0f, 1f);
        currentQuality = 100f * (1f - PApplet.constrain(0.55f * currentArtifactRisk + 0.45f * currentSaturationRisk, 0f, 1f));
    }

    private void accumulateFrame(int stage) {
        acceptedFrames[stage]++;
        for (int b = 0; b < BAND_COUNT; b++) {
            bandSums[stage][b] += currentBands[b];
        }
        rmsSums[stage] += currentRms;
        qualitySums[stage] += currentQuality;
        thetaBetaSums[stage] += currentThetaBeta;
        alphaBetaSums[stage] += currentAlphaBeta;
    }

    private void pushHistory() {
        qualityHistory[historyWrite] = currentQuality;
        alphaHistory[historyWrite] = getCurrentRelativeBand(2);
        historyWrite = (historyWrite + 1) % HISTORY_SIZE;
        if (historyWrite == 0) {
            historyFilled = true;
        }
    }

    private void saveProfile() {
        if (!hasAnyStageDone()) {
            setNotice("没有可保存的有效基线。");
            return;
        }

        JSONObject root = new JSONObject();
        root.setString("type", "baselineProfile");
        root.setString("version", "1");
        root.setString("createdAt", MAIN.directoryManager.getFileNameDateTime());
        root.setInt("sampleRate", MAIN.currentBoard.getSampleRate());
        root.setInt("channelCount", nchan);
        root.setString("protocol", PROTOCOL_LABELS[protocolIndex]);
        root.setString("duration", DURATION_LABELS[durationIndex]);
        root.setString("qualityGate", QUALITY_LABELS[qualityIndex]);

        JSONArray stages = new JSONArray();
        for (int s = 0; s < getActiveStageCount(); s++) {
            JSONObject st = new JSONObject();
            st.setString("name", STAGE_NAMES[s]);
            st.setBoolean("complete", stageDone[s]);
            st.setInt("acceptedFrames", acceptedFrames[s]);
            st.setInt("rejectedFrames", rejectedFrames[s]);
            st.setFloat("acceptedSeconds", getAcceptedSeconds(s));
            st.setFloat("meanQuality", getStageQuality(s));
            st.setFloat("meanRmsUv", getStageRms(s));
            st.setFloat("thetaBeta", getStageThetaBeta(s));
            st.setFloat("alphaBeta", getStageAlphaBeta(s));

            JSONArray bands = new JSONArray();
            for (int b = 0; b < BAND_COUNT; b++) {
                JSONObject band = new JSONObject();
                band.setString("name", BAND_NAMES[b]);
                band.setFloat("power", getStageBand(s, b));
                band.setFloat("relative", getStageRelativeBand(s, b));
                bands.append(band);
            }
            st.setJSONArray("bands", bands);
            stages.append(st);
        }
        root.setJSONArray("stages", stages);

        JSONObject derived = new JSONObject();
        derived.setFloat("alphaReactivity", getAlphaReactivity());
        derived.setFloat("fatigueReferenceThetaBeta", getBestThetaBetaReference());
        derived.setFloat("relaxReferenceAlphaBeta", getBestAlphaBetaReference());
        derived.setFloat("meanQuality", getMeanCompletedQuality());
        root.setJSONObject("derived", derived);

        String path = MAIN.directoryManager.getSettingsPath() + "baseline_profile.json";
        MAIN.saveJSONObject(root, path);
        saved = true;
        setNotice("已保存个体基线: " + path);
    }

    private void drawHeader(int x0, int y0, int w0) {
        MAIN.fill(TEXT_MAIN);
        MAIN.textFont(p7);
        MAIN.textSize(15);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text("个体基线校准", x0 + 12, y0 + 9);

        MAIN.fill(TEXT_SUB);
        MAIN.textSize(11);
        MAIN.text(
                "当前阶段 " + STAGE_NAMES[currentStage] + " | 质量门限 " + PApplet.nf(QUALITY_GATE[qualityIndex], 1, 0)
                        + " | 状态 " + getCaptureStatus(),
                x0 + 12, y0 + 31
        );

        int scoreColor = currentQuality >= QUALITY_GATE[qualityIndex] ? GOOD : WARN;
        if (!MAIN.currentBoard.isStreaming()) {
            scoreColor = BAD;
        }
        MAIN.fill(scoreColor);
        MAIN.textSize(24);
        MAIN.textAlign(PApplet.RIGHT, PApplet.TOP);
        MAIN.text(PApplet.nf(currentQuality, 1, 0), x0 + w0 - 10, y0 + 5);
        MAIN.fill(TEXT_SUB);
        MAIN.textSize(10);
        MAIN.text("实时质量", x0 + w0 - 14, y0 + 34);

        MAIN.stroke(PANEL_STROKE);
        MAIN.line(x0 + 10, y0 + 48, x0 + w0 - 10, y0 + 48);
    }

    private void drawProtocolCard(int x0, int y0, int w0, int h0) {
        drawCard(x0, y0, w0, h0, "校准流程");

        int stageCount = getActiveStageCount();
        int rowH = Math.max(74, (h0 - 64) / Math.max(1, stageCount));
        for (int s = 0; s < stageCount; s++) {
            int sy = y0 + 30 + s * rowH;
            boolean current = s == currentStage;
            int c = stageDone[s] ? GOOD : (current ? ACCENT : TEXT_SUB);
            MAIN.noStroke();
            MAIN.fill(current ? 0x382B5A80 : 0x211A3559);
            MAIN.rect(x0 + 10, sy, w0 - 20, rowH - 10, 4);
            MAIN.stroke(c);
            MAIN.noFill();
            MAIN.rect(x0 + 10, sy, w0 - 20, rowH - 10, 4);

            MAIN.fill(c);
            MAIN.textFont(p7);
            MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
            MAIN.textSize(12);
            MAIN.text((s + 1) + ". " + STAGE_NAMES[s], x0 + 20, sy + 9);

            MAIN.fill(TEXT_SUB);
            MAIN.textSize(10);
            drawWrappedText(STAGE_HINTS[s], x0 + 20, sy + 28, w0 - 40, 12, 2);

            drawProgressBar(x0 + 20, sy + rowH - 28, w0 - 40, 10, getStageProgress(s), c);
            MAIN.fill(TEXT_SUB);
            MAIN.textAlign(PApplet.RIGHT, PApplet.TOP);
            MAIN.text(PApplet.nf(getAcceptedSeconds(s), 1, 1) + " / " + DURATION_SECONDS[durationIndex] + "s", x0 + w0 - 20, sy + rowH - 44);
        }

        int msgY = y0 + h0 - 28;
        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7);
        MAIN.textSize(10);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        String shown = System.currentTimeMillis() - noticeMs < 5000L ? notice : getDefaultAdvice();
        drawWrappedText(shown, x0 + 12, msgY, w0 - 24, 12, 2);
    }

    private void drawLiveCard(int x0, int y0, int w0, int h0) {
        int topH = (int) (h0 * 0.38f);
        int midH = (int) (h0 * 0.34f);
        int gap = 8;

        drawQualityBlock(x0, y0, w0, topH);
        drawBandBlock(x0, y0 + topH + gap, w0, midH);
        drawTrendBlock(x0, y0 + topH + midH + gap * 2, w0, h0 - topH - midH - gap * 2);
    }

    private void drawQualityBlock(int x0, int y0, int w0, int h0) {
        drawCard(x0, y0, w0, h0, "实时质量门控");

        int scoreColor = currentQuality >= QUALITY_GATE[qualityIndex] ? GOOD : WARN;
        if (!MAIN.currentBoard.isStreaming()) {
            scoreColor = BAD;
        }

        MAIN.fill(scoreColor);
        MAIN.textFont(p7);
        MAIN.textSize(20);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text(PApplet.nf(currentQuality, 1, 1), x0 + 6, y0 + 40);
        MAIN.fill(TEXT_SUB);
        MAIN.textSize(9);
        MAIN.text("/ 100", x0 + 58, y0 + 48);

        int bx = x0 + 14;
        int by = y0 + 86;
        drawMetricLine(bx, by, w0 - 28, "活跃通道", activeChannels / (float) Math.max(1, nchan), activeChannels + " / " + nchan, ACCENT);
        drawMetricLine(bx, by + 26, w0 - 28, "伪迹风险", currentArtifactRisk, PApplet.nf(currentArtifactRisk * 100f, 1, 0) + "%", WARN);
        drawMetricLine(bx, by + 52, w0 - 28, "饱和风险", currentSaturationRisk, PApplet.nf(currentSaturationRisk * 100f, 1, 0) + "%", BAD);

        MAIN.fill(TEXT_SUB);
        MAIN.textSize(10);
        MAIN.textAlign(PApplet.RIGHT, PApplet.TOP);
        MAIN.text("RMS " + PApplet.nf(currentRms, 1, 2) + " uV", x0 + w0 - 14, y0 + 30);
        MAIN.text("采样 " + PApplet.nf(getAcceptedSeconds(currentStage), 1, 1) + "s", x0 + w0 - 14, y0 + 46);
    }

    private void drawBandBlock(int x0, int y0, int w0, int h0) {
        drawCard(x0, y0, w0, h0, "实时频带结构");

        int bx = x0 + 14;
        int by = y0 + 30;
        int rowH = Math.max(20, (h0 - 48) / BAND_COUNT);
        for (int b = 0; b < BAND_COUNT; b++) {
            drawBandBar(bx, by + b * rowH, w0 - 28, BAND_NAMES[b], getCurrentRelativeBand(b), currentBands[b], BAND_COLORS[b]);
        }

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7);
        MAIN.textSize(10);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text("θ/β " + PApplet.nf(currentThetaBeta, 1, 2), x0 + 14, y0 + h0 - 16);
        MAIN.textAlign(PApplet.RIGHT, PApplet.TOP);
        MAIN.text("α/β " + PApplet.nf(currentAlphaBeta, 1, 2), x0 + w0 - 14, y0 + h0 - 16);
    }

    private void drawTrendBlock(int x0, int y0, int w0, int h0) {
        drawCard(x0, y0, w0, h0, "质量与Alpha趋势");

        int gx = x0 + 28;
        int gy = y0 + 32;
        int gw = w0 - 42;
        int gh = h0 - 48;
        MAIN.stroke(GRID);
        for (int i = 0; i <= 4; i++) {
            float yy = gy + gh * i / 4f;
            MAIN.line(gx, yy, gx + gw, yy);
        }
        MAIN.stroke(PANEL_STROKE);
        MAIN.line(gx, gy + gh, gx + gw, gy + gh);
        MAIN.line(gx, gy, gx, gy + gh);

        drawHistoryLine(qualityHistory, gx, gy, gw, gh, 0f, 100f, GOOD);
        drawHistoryLine(alphaHistory, gx, gy, gw, gh, 0f, 1f, ACCENT);

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7);
        MAIN.textSize(9);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text("质量", gx + 4, y0 + 18);
        MAIN.fill(ACCENT);
        MAIN.text("Alpha占比", gx + 48, y0 + 18);
    }

    private void drawResultCard(int x0, int y0, int w0, int h0) {
        int topH = (int) (h0 * 0.52f);
        int gap = 8;
        drawStageSummaryBlock(x0, y0, w0, topH);
        drawDerivedBlock(x0, y0 + topH + gap, w0, h0 - topH - gap);
    }

    private void drawStageSummaryBlock(int x0, int y0, int w0, int h0) {
        drawCard(x0, y0, w0, h0, "基线结果");

        int stageCount = getActiveStageCount();
        int rowH = Math.max(52, (h0 - 40) / Math.max(1, stageCount));
        for (int s = 0; s < stageCount; s++) {
            int sy = y0 + 30 + s * rowH;
            int c = stageDone[s] ? GOOD : TEXT_SUB;
            MAIN.fill(c);
            MAIN.textFont(p7);
            MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
            MAIN.textSize(11);
            MAIN.text(STAGE_NAMES[s], x0 + 12, sy);

            MAIN.fill(TEXT_SUB);
            MAIN.textSize(9);
            MAIN.text("质量 " + PApplet.nf(getStageQuality(s), 1, 1) + "  RMS " + PApplet.nf(getStageRms(s), 1, 1), x0 + 12, sy + 16);
            MAIN.text("θ/β " + PApplet.nf(getStageThetaBeta(s), 1, 2) + "  α/β " + PApplet.nf(getStageAlphaBeta(s), 1, 2), x0 + 12, sy + 30);

            int barsX = x0 + Math.max(112, w0 / 2);
            int barsW = x0 + w0 - 14 - barsX;
            for (int b = 0; b < 4; b++) {
                MAIN.noStroke();
                MAIN.fill((BAND_COLORS[b] >> 16) & 0xFF, (BAND_COLORS[b] >> 8) & 0xFF, BAND_COLORS[b] & 0xFF, 210);
                MAIN.rect(barsX, sy + 4 + b * 9, barsW * getStageRelativeBand(s, b), 5, 2);
                MAIN.stroke(PANEL_STROKE);
                MAIN.noFill();
                MAIN.rect(barsX, sy + 4 + b * 9, barsW, 5, 2);
            }
        }
    }

    private void drawDerivedBlock(int x0, int y0, int w0, int h0) {
        drawCard(x0, y0, w0, h0, "个体化阈值与建议");

        int chipY = y0 + 28;
        drawChip(x0 + 12, chipY, (w0 - 30) / 2, 42, "Alpha反应", PApplet.nf(getAlphaReactivity(), 1, 2));
        drawChip(x0 + 18 + (w0 - 30) / 2, chipY, (w0 - 30) / 2, 42, "平均质量", PApplet.nf(getMeanCompletedQuality(), 1, 1));
        drawChip(x0 + 12, chipY + 48, (w0 - 30) / 2, 42, "疲劳参考 θ/β", PApplet.nf(getBestThetaBetaReference(), 1, 2));
        drawChip(x0 + 18 + (w0 - 30) / 2, chipY + 48, (w0 - 30) / 2, 42, "放松参考 α/β", PApplet.nf(getBestAlphaBetaReference(), 1, 2));

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7);
        MAIN.textSize(10);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        drawWrappedText(getResultAdvice(), x0 + 12, chipY + 106, w0 - 24, 15, 4);

        MAIN.fill(saved ? GOOD : TEXT_SUB);
        MAIN.textSize(9);
        drawWrappedText(
                saved ? "基线文件已保存，可供后续状态模块读取。" : "完成任一阶段后即可保存 JSON 基线文件。",
                x0 + 12,
                y0 + h0 - 22,
                w0 - 24,
                11,
                2
        );
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

    private void drawProgressBar(int x0, int y0, int w0, int h0, float v, int c) {
        MAIN.noStroke();
        MAIN.fill(22, 34, 56, 220);
        MAIN.rect(x0, y0, w0, h0, 2);
        MAIN.fill((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, 220);
        MAIN.rect(x0, y0, w0 * PApplet.constrain(v, 0f, 1f), h0, 2);
        MAIN.stroke(PANEL_STROKE);
        MAIN.noFill();
        MAIN.rect(x0, y0, w0, h0, 2);
    }

    private void drawMetricLine(int x0, int y0, int w0, String label, float v, String value, int c) {
        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.textSize(10);
        MAIN.text(label, x0, y0);
        MAIN.textAlign(PApplet.RIGHT, PApplet.TOP);
        MAIN.text(value, x0 + w0, y0);
        drawProgressBar(x0 + 76, y0 + 3, Math.max(20, w0 - 128), 8, v, c);
    }

    private void drawBandBar(int x0, int y0, int w0, String label, float pct, float value, int c) {
        int labelW = 58;
        int valueW = 62;
        int trackX = x0 + labelW;
        int trackW = Math.max(20, w0 - labelW - valueW);

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.textSize(10);
        MAIN.text(label, x0, y0);

        drawProgressBar(trackX, y0 + 3, trackW, 9, pct, c);

        MAIN.fill(TEXT_MAIN);
        MAIN.textAlign(PApplet.RIGHT, PApplet.TOP);
        MAIN.text(PApplet.nf(pct * 100f, 1, 0) + "%", x0 + w0, y0);
    }

    private void drawChip(int x0, int y0, int w0, int h0, String label, String value) {
        MAIN.noStroke();
        MAIN.fill(0x32183357);
        MAIN.rect(x0, y0, w0, h0, 4);
        MAIN.stroke(0x66A6C6EC);
        MAIN.noFill();
        MAIN.rect(x0, y0, w0, h0, 4);
        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.textSize(9);
        MAIN.text(label, x0 + 8, y0 + 6);
        MAIN.fill(TEXT_MAIN);
        MAIN.textSize(13);
        MAIN.text(value, x0 + 8, y0 + 22);
    }

    private void drawHistoryLine(float[] data, int gx, int gy, int gw, int gh, float minV, float maxV, int c) {
        int count = historyFilled ? HISTORY_SIZE : historyWrite;
        if (count < 2) {
            return;
        }
        MAIN.noFill();
        MAIN.stroke((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, 220);
        MAIN.strokeWeight(1.7f);
        MAIN.beginShape();
        for (int i = 0; i < count; i++) {
            int idx = historyFilled ? (historyWrite + i) % HISTORY_SIZE : i;
            float v = PApplet.constrain((data[idx] - minV) / Math.max(1e-6f, maxV - minV), 0f, 1f);
            float px = gx + gw * i / (float) Math.max(1, count - 1);
            float py = gy + gh - v * gh;
            MAIN.vertex(px, py);
        }
        MAIN.endShape();
    }

    private void drawWrappedText(String txt, int x0, int y0, int maxW, int lineH) {
        drawWrappedText(txt, x0, y0, maxW, lineH, Integer.MAX_VALUE);
    }

    private void drawWrappedText(String txt, int x0, int y0, int maxW, int lineH, int maxLines) {
        String[] parts = txt.split("，");
        ArrayList<String> linesToDraw = new ArrayList<String>();
        String line = "";
        int yy = y0;
        for (String part : parts) {
            String cand = line.length() == 0 ? part : line + "，" + part;
            if (MAIN.textWidth(cand) > maxW && line.length() > 0) {
                addWrappedSegment(linesToDraw, line, maxW);
                line = part;
            } else {
                line = cand;
            }
        }
        if (line.length() > 0) {
            addWrappedSegment(linesToDraw, line, maxW);
        }

        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        int count = Math.min(maxLines, linesToDraw.size());
        for (int i = 0; i < count; i++) {
            String out = linesToDraw.get(i);
            if (i == maxLines - 1 && linesToDraw.size() > maxLines) {
                out = ellipsize(out, maxW);
            }
            MAIN.text(out, x0, yy);
            yy += lineH;
        }
    }

    private void addWrappedSegment(ArrayList<String> out, String segment, int maxW) {
        String line = "";
        for (int i = 0; i < segment.length(); i++) {
            String cand = line + segment.charAt(i);
            if (MAIN.textWidth(cand) > maxW && line.length() > 0) {
                out.add(line);
                line = String.valueOf(segment.charAt(i));
            } else {
                line = cand;
            }
        }
        if (line.length() > 0) {
            out.add(line);
        }
    }

    private String ellipsize(String txt, int maxW) {
        if (MAIN.textWidth(txt) <= maxW) {
            return txt;
        }
        String suffix = "...";
        String out = txt;
        while (out.length() > 0 && MAIN.textWidth(out + suffix) > maxW) {
            out = out.substring(0, out.length() - 1);
        }
        return out + suffix;
    }

    private int getActiveStageCount() {
        return PROTOCOL_STAGE_COUNT[protocolIndex];
    }

    private float getStageProgress(int stage) {
        return PApplet.constrain(getAcceptedSeconds(stage) / Math.max(1f, DURATION_SECONDS[durationIndex]), 0f, 1f);
    }

    private float getAcceptedSeconds(int stage) {
        return acceptedFrames[stage] * UPDATE_INTERVAL_MS / 1000f;
    }

    private float getStageBand(int stage, int band) {
        return acceptedFrames[stage] > 0 ? bandSums[stage][band] / acceptedFrames[stage] : 0f;
    }

    private float getStageRelativeBand(int stage, int band) {
        float total = 0f;
        for (int b = 0; b < BAND_COUNT; b++) {
            total += getStageBand(stage, b);
        }
        return total > 1e-9f ? getStageBand(stage, band) / total : 0f;
    }

    private float getCurrentRelativeBand(int band) {
        return currentTotalPower > 1e-9f ? currentBands[band] / currentTotalPower : 0f;
    }

    private float getStageRms(int stage) {
        return acceptedFrames[stage] > 0 ? rmsSums[stage] / acceptedFrames[stage] : 0f;
    }

    private float getStageQuality(int stage) {
        return acceptedFrames[stage] > 0 ? qualitySums[stage] / acceptedFrames[stage] : 0f;
    }

    private float getStageThetaBeta(int stage) {
        return acceptedFrames[stage] > 0 ? thetaBetaSums[stage] / acceptedFrames[stage] : 0f;
    }

    private float getStageAlphaBeta(int stage) {
        return acceptedFrames[stage] > 0 ? alphaBetaSums[stage] / acceptedFrames[stage] : 0f;
    }

    private float getAlphaReactivity() {
        if (acceptedFrames[0] <= 0 || acceptedFrames[1] <= 0) {
            return 0f;
        }
        return getStageBand(1, 2) / Math.max(1e-9f, getStageBand(0, 2));
    }

    private float getBestThetaBetaReference() {
        float best = 0f;
        int count = 0;
        for (int s = 0; s < getActiveStageCount(); s++) {
            if (acceptedFrames[s] > 0) {
                best += getStageThetaBeta(s);
                count++;
            }
        }
        return count > 0 ? best / count : 0f;
    }

    private float getBestAlphaBetaReference() {
        float best = 0f;
        int count = 0;
        for (int s = 0; s < getActiveStageCount(); s++) {
            if (acceptedFrames[s] > 0) {
                best += getStageAlphaBeta(s);
                count++;
            }
        }
        return count > 0 ? best / count : 0f;
    }

    private float getMeanCompletedQuality() {
        float sum = 0f;
        int count = 0;
        for (int s = 0; s < getActiveStageCount(); s++) {
            if (acceptedFrames[s] > 0) {
                sum += getStageQuality(s);
                count++;
            }
        }
        return count > 0 ? sum / count : 0f;
    }

    private boolean hasAnyStageDone() {
        for (int s = 0; s < getActiveStageCount(); s++) {
            if (acceptedFrames[s] > 0) {
                return true;
            }
        }
        return false;
    }

    private String getCaptureStatus() {
        if (capturing && !paused) {
            return "采集中";
        }
        if (paused) {
            return "暂停";
        }
        if (hasAnyStageDone()) {
            return "待保存";
        }
        return "待开始";
    }

    private String getDefaultAdvice() {
        if (!MAIN.currentBoard.isStreaming()) {
            return "数据流未启动，无法采集个体基线。";
        }
        if (currentQuality < QUALITY_GATE[qualityIndex]) {
            return "当前质量未达标，建议检查电极接触并减少动作。";
        }
        return STAGE_HINTS[currentStage];
    }

    private String getResultAdvice() {
        if (!hasAnyStageDone()) {
            return "完成校准后，将生成个体化 Alpha、Theta/Beta、Alpha/Beta 参考值，用于后续状态评估。";
        }
        if (getMeanCompletedQuality() < 70f) {
            return "基线质量偏低，建议重新检查电极接触并重复采集。";
        }
        if (getAlphaReactivity() > 1.25f) {
            return "闭眼 Alpha 增强明显，个体基线区分度较好，适合用于放松训练和疲劳归一化。";
        }
        return "基线已可用，后续模块可使用该文件进行个人阈值归一化。";
    }

    private void setNotice(String msg) {
        notice = msg;
        noticeMs = System.currentTimeMillis();
    }

    private static float safe(float v) {
        return Float.isFinite(v) ? Math.max(0f, v) : 0f;
    }

    private void drawGradientBackground() {
        for (int i = 0; i < h; i++) {
            float t = i / (float) Math.max(1, h - 1);
            int c = lerpRgb(BG_TOP, BG_BOTTOM, t);
            MAIN.stroke((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF);
            MAIN.line(x, y + i, x + w, y + i);
        }
        float glow = 0.5f + 0.5f * (float) Math.sin(MAIN.frameCount * 0.01f);
        MAIN.noStroke();
        MAIN.fill(79, 216, 255, (int) (16 * glow));
        MAIN.ellipse(x + w * 0.18f, y + h * 0.22f, w * 0.34f, h * 0.28f);
        MAIN.fill(108, 125, 255, (int) (12 * glow));
        MAIN.ellipse(x + w * 0.84f, y + h * 0.74f, w * 0.30f, h * 0.25f);
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
