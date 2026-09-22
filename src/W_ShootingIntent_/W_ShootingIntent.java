package W_ShootingIntent_;

import Globel.GUI;
import Widget_.Widget;
import processing.core.PApplet;

import java.util.Arrays;
import java.util.Random;

import static Globel.GUI.p7;

/** A single-curve visualization for real-time shooting-intent detection. */
public class W_ShootingIntent extends Widget {
    private static final int BG_TOP = 0xFF071120;
    private static final int BG_BOTTOM = 0xFF0E1A30;
    private static final int PANEL = 0xE013243F;
    private static final int PANEL_STROKE = 0x6683A2CC;
    private static final int GRID_LINE = 0x2D90AED8;
    private static final int TEXT_MAIN = 0xFFEAF2FF;
    private static final int TEXT_SUB = 0xFF98AECE;
    private static final int TRACE = 0xFF58D7FF;
    private static final int TRACE_GLOW = 0x4058D7FF;
    private static final int ALERT = 0xFFFF5F68;
    private static final int SAFE = 0xFF54D59D;

    private static final int HISTORY_SIZE = 360;
    private static final int UPDATE_INTERVAL_MS = 33;
    private static final int EXTERNAL_DATA_TIMEOUT_MS = 1000;
    private static final int ALERT_HOLD_MS = 900;
    private static final int TRIGGER_COOLDOWN_MS = 1200;
    private static final float[] THRESHOLDS = {0.60f, 0.70f, 0.80f};
    private static final String[] THRESHOLD_LABELS = {"60%", "70%", "80%"};

    private final GUI MAIN;
    private final float[] history = new float[HISTORY_SIZE];
    private final boolean[] triggerHistory = new boolean[HISTORY_SIZE];
    private final Random demoRandom = new Random();

    private int writeIndex;
    private int historyCount;
    private int thresholdIndex = 1;
    private float threshold = THRESHOLDS[thresholdIndex];
    private float displayedScore = 0.22f;
    private volatile float incomingScore = 0.22f;
    private float demoBaseline = 0.23f;
    private float demoSlowDrift;
    private float demoColoredNoise;
    private float demoTransient;
    private long nextDemoEventMs;
    private long demoEventStartMs = -1L;
    private long demoEventDurationMs;
    private float demoEventAmplitude;
    private float demoEventPeakPosition = 0.58f;
    private float demoEventDecayRate = 2.4f;
    private long lastUpdateMs;
    private volatile long lastExternalDataMs = -1L;
    private long lastTriggerMs = -TRIGGER_COOLDOWN_MS;
    private long alertUntilMs;
    private int detectionCount;
    private boolean previousAboveThreshold;
    private boolean wasUsingExternalData;

    public W_ShootingIntent(GUI main) {
        super(main);
        MAIN = main;
        setTitle("射击意图检测");
        dropdownWidth = 70;
        addDropdown(
                "ShootingIntentThreshold",
                "阈值",
                Arrays.asList(THRESHOLD_LABELS),
                thresholdIndex
        );

        Arrays.fill(history, displayedScore);
    }

    /** ControlP5 callback for the threshold selector. */
    public void ShootingIntentThreshold(int value) {
        thresholdIndex = PApplet.constrain(value, 0, THRESHOLDS.length - 1);
        threshold = THRESHOLDS[thresholdIndex];
        previousAboveThreshold = displayedScore >= threshold;
    }

    /** Supplies a normalized model score in the range [0, 1]. */
    public void updateIntentScore(float score) {
        incomingScore = PApplet.constrain(score, 0f, 1f);
        lastExternalDataMs = System.currentTimeMillis();
    }

    public void setThreshold(float value) {
        threshold = PApplet.constrain(value, 0.05f, 0.95f);
        previousAboveThreshold = displayedScore >= threshold;
    }

    public float getThreshold() {
        return threshold;
    }

    public boolean isShootingIntentDetected() {
        return displayedScore >= threshold || System.currentTimeMillis() < alertUntilMs;
    }

    public int getDetectionCount() {
        return detectionCount;
    }

    @Override
    public void update() {
        super.update();

        long now = System.currentTimeMillis();
        if (now - lastUpdateMs < UPDATE_INTERVAL_MS) {
            return;
        }
        lastUpdateMs = now;

        boolean usingExternalData = lastExternalDataMs > 0L
                && now - lastExternalDataMs <= EXTERNAL_DATA_TIMEOUT_MS;
        if (!usingExternalData && wasUsingExternalData) {
            resetDemoTimeline(now);
        }
        wasUsingExternalData = usingExternalData;

        float targetScore = usingExternalData ? incomingScore : createDemoScore(now);
        displayedScore += (targetScore - displayedScore) * 0.28f;
        displayedScore = PApplet.constrain(displayedScore, 0f, 1f);

        boolean aboveThreshold = displayedScore >= threshold;
        boolean risingEdge = aboveThreshold && !previousAboveThreshold;
        boolean triggered = risingEdge && now - lastTriggerMs >= TRIGGER_COOLDOWN_MS;
        if (triggered) {
            lastTriggerMs = now;
            alertUntilMs = now + ALERT_HOLD_MS;
            detectionCount++;
        }
        previousAboveThreshold = aboveThreshold;

        history[writeIndex] = displayedScore;
        triggerHistory[writeIndex] = triggered;
        writeIndex = (writeIndex + 1) % HISTORY_SIZE;
        historyCount = Math.min(historyCount + 1, HISTORY_SIZE);
    }

    @Override
    public void draw() {
        super.draw();
        MAIN.pushStyle();

        drawGradientBackground(x, y - 1, w, h + 1);

        int padding = 10;
        int panelX = x + padding;
        int panelY = y + padding;
        int panelW = Math.max(1, w - padding * 2);
        int panelH = Math.max(1, h - padding * 2);

        MAIN.noStroke();
        MAIN.fill(PANEL);
        MAIN.rect(panelX, panelY, panelW, panelH, 5);
        MAIN.stroke(PANEL_STROKE);
        MAIN.strokeWeight(1.1f);
        MAIN.noFill();
        MAIN.rect(panelX, panelY, panelW, panelH, 5);

        drawHeader(panelX, panelY, panelW);

        int chartX = panelX + 48;
        int chartY = panelY + 72;
        int chartW = Math.max(1, panelW - 66);
        int chartH = Math.max(1, panelH - 106);
        drawChart(chartX, chartY, chartW, chartH);

        MAIN.popStyle();
    }

    private float createDemoScore(long now) {
        // A bounded random walk models slow changes in attention and electrode conditions.
        demoBaseline = PApplet.constrain(
                demoBaseline + gaussian(0.00055f),
                0.16f,
                0.34f
        );
        demoSlowDrift = demoSlowDrift * 0.995f + gaussian(0.0015f);
        demoSlowDrift = PApplet.constrain(demoSlowDrift, -0.07f, 0.07f);

        // Correlated noise avoids the white-noise look of independent random points.
        demoColoredNoise = demoColoredNoise * 0.78f + gaussian(0.018f);
        demoTransient *= 0.84f;
        if (demoRandom.nextFloat() < 0.0035f) {
            demoTransient += 0.035f + demoRandom.nextFloat() * 0.11f;
        }

        if (nextDemoEventMs == 0L) {
            scheduleNextDemoEvent(now, true);
        }

        float eventContribution = 0f;
        if (demoEventStartMs < 0L && now >= nextDemoEventMs) {
            beginDemoEvent(now);
        }
        if (demoEventStartMs >= 0L) {
            float progress = (now - demoEventStartMs) / (float) demoEventDurationMs;
            if (progress >= 1f) {
                demoEventStartMs = -1L;
                scheduleNextDemoEvent(now, false);
            } else {
                eventContribution = demoEventAmplitude * eventEnvelope(progress);
            }
        }

        float score = demoBaseline
                + demoSlowDrift
                + demoColoredNoise
                + demoTransient
                + eventContribution;
        return PApplet.constrain(score, 0.04f, 0.98f);
    }

    private void beginDemoEvent(long now) {
        demoEventStartMs = now;
        demoEventDurationMs = 1050L + demoRandom.nextInt(1550);
        demoEventPeakPosition = 0.46f + demoRandom.nextFloat() * 0.22f;
        demoEventDecayRate = 1.7f + demoRandom.nextFloat() * 1.8f;

        // Some preparations remain below threshold; stronger events become detections.
        if (demoRandom.nextFloat() < 0.22f) {
            demoEventAmplitude = 0.25f + demoRandom.nextFloat() * 0.16f;
        } else {
            demoEventAmplitude = 0.52f + demoRandom.nextFloat() * 0.27f;
        }
    }

    private void scheduleNextDemoEvent(long now, boolean initial) {
        long delay;
        if (initial) {
            delay = 2200L + demoRandom.nextInt(5200);
        } else {
            // Exponential-like spacing produces irregular clusters and long quiet periods.
            double randomUnit = Math.max(0.0001, 1.0 - demoRandom.nextDouble());
            delay = 3200L + (long) (-Math.log(randomUnit) * 4600.0);
            delay = Math.min(delay, 16500L);
            if (demoRandom.nextFloat() < 0.16f) {
                delay = 1700L + demoRandom.nextInt(2300);
            }
        }
        nextDemoEventMs = now + delay;
    }

    private float eventEnvelope(float progress) {
        if (progress < demoEventPeakPosition) {
            float rising = progress / demoEventPeakPosition;
            return rising * rising * (3f - 2f * rising);
        }
        float falling = (progress - demoEventPeakPosition) / (1f - demoEventPeakPosition);
        return (float) Math.exp(-demoEventDecayRate * falling) * (1f - falling);
    }

    private float gaussian(float standardDeviation) {
        return (float) demoRandom.nextGaussian() * standardDeviation;
    }

    private void resetDemoTimeline(long now) {
        demoBaseline = PApplet.constrain(displayedScore, 0.16f, 0.34f);
        demoSlowDrift = 0f;
        demoColoredNoise = 0f;
        demoTransient = 0f;
        demoEventStartMs = -1L;
        nextDemoEventMs = 0L;
        scheduleNextDemoEvent(now, true);
    }

    private void drawHeader(int panelX, int panelY, int panelW) {
        boolean alert = isShootingIntentDetected();

        MAIN.fill(TEXT_MAIN);
        MAIN.textFont(p7);
        MAIN.textSize(16);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text("射击意图检测", panelX + 14, panelY + 10);

        MAIN.fill(TEXT_SUB);
        MAIN.textSize(11);
        MAIN.text(
                "实时意图概率  ·  阈值 " + Math.round(threshold * 100f) + "%  ·  触发 " + detectionCount + " 次",
                panelX + 14,
                panelY + 34
        );

        String scoreText = Math.round(displayedScore * 100f) + "%";
        int pillW = Math.max(138, Math.min(190, panelW / 3));
        int pillX = panelX + panelW - pillW - 14;
        int pillY = panelY + 12;
        int stateColor = alert ? ALERT : SAFE;

        MAIN.noStroke();
        MAIN.fill(alert ? 0x38FF5F68 : 0x2854D59D);
        MAIN.rect(pillX, pillY, pillW, 32, 16);
        MAIN.stroke(stateColor);
        MAIN.strokeWeight(1f);
        MAIN.noFill();
        MAIN.rect(pillX, pillY, pillW, 32, 16);

        MAIN.fill(stateColor);
        MAIN.noStroke();
        MAIN.ellipse(pillX + 15, pillY + 16, 8, 8);
        MAIN.textFont(p7);
        MAIN.textSize(11);
        MAIN.textAlign(PApplet.LEFT, PApplet.CENTER);
        MAIN.text(alert ? "检测到射击  " + scoreText : "监测中  " + scoreText, pillX + 27, pillY + 16);

        MAIN.stroke(PANEL_STROKE);
        MAIN.line(panelX + 12, panelY + 58, panelX + panelW - 12, panelY + 58);
    }

    private void drawChart(int chartX, int chartY, int chartW, int chartH) {
        drawGrid(chartX, chartY, chartW, chartH);
        drawThreshold(chartX, chartY, chartW, chartH);
        drawCurve(chartX, chartY, chartW, chartH);
        drawAxesLabels(chartX, chartY, chartW, chartH);
    }

    private void drawGrid(int chartX, int chartY, int chartW, int chartH) {
        MAIN.stroke(GRID_LINE);
        MAIN.strokeWeight(1f);
        for (int i = 0; i <= 5; i++) {
            float yy = chartY + chartH * i / 5f;
            MAIN.line(chartX, yy, chartX + chartW, yy);
        }
        for (int i = 0; i <= 6; i++) {
            float xx = chartX + chartW * i / 6f;
            MAIN.line(xx, chartY, xx, chartY + chartH);
        }

        MAIN.stroke(PANEL_STROKE);
        MAIN.strokeWeight(1.2f);
        MAIN.line(chartX, chartY, chartX, chartY + chartH);
        MAIN.line(chartX, chartY + chartH, chartX + chartW, chartY + chartH);
    }

    private void drawThreshold(int chartX, int chartY, int chartW, int chartH) {
        float yy = scoreToY(threshold, chartY, chartH);
        MAIN.stroke(ALERT);
        MAIN.strokeWeight(1.2f);
        for (int xx = chartX; xx < chartX + chartW; xx += 12) {
            MAIN.line(xx, yy, Math.min(xx + 6, chartX + chartW), yy);
        }

        String label = "射击阈值 " + Math.round(threshold * 100f) + "%";
        MAIN.textFont(p7);
        MAIN.textSize(10);
        float labelW = MAIN.textWidth(label) + 12f;
        MAIN.noStroke();
        MAIN.fill(0xCC351F30);
        MAIN.rect(chartX + chartW - labelW, yy - 18, labelW, 16, 3);
        MAIN.fill(ALERT);
        MAIN.textAlign(PApplet.CENTER, PApplet.CENTER);
        MAIN.text(label, chartX + chartW - labelW / 2f, yy - 10);
    }

    private void drawCurve(int chartX, int chartY, int chartW, int chartH) {
        if (historyCount < 2) {
            return;
        }

        for (int pass = 0; pass < 2; pass++) {
            for (int i = 1; i < historyCount; i++) {
                int previousIndex = historyIndex(i - 1);
                int currentIndex = historyIndex(i);
                float previousValue = history[previousIndex];
                float currentValue = history[currentIndex];
                float x1 = historyX(i - 1, chartX, chartW);
                float x2 = historyX(i, chartX, chartW);
                float y1 = scoreToY(previousValue, chartY, chartH);
                float y2 = scoreToY(currentValue, chartY, chartH);
                boolean overThreshold = previousValue >= threshold || currentValue >= threshold;

                if (pass == 0) {
                    MAIN.stroke(overThreshold ? 0x55FF5F68 : TRACE_GLOW);
                    MAIN.strokeWeight(6f);
                } else {
                    MAIN.stroke(overThreshold ? ALERT : TRACE);
                    MAIN.strokeWeight(overThreshold ? 2.8f : 2.2f);
                }
                MAIN.line(x1, y1, x2, y2);
            }
        }

        drawTriggerMarkers(chartX, chartY, chartW, chartH);

        int latestIndex = historyIndex(historyCount - 1);
        float latestX = historyX(historyCount - 1, chartX, chartW);
        float latestY = scoreToY(history[latestIndex], chartY, chartH);
        int pointColor = history[latestIndex] >= threshold ? ALERT : TRACE;
        float pulse = 0.78f + 0.22f * PApplet.sin(MAIN.frameCount * 0.12f);
        MAIN.noStroke();
        MAIN.fill(pointColor);
        MAIN.ellipse(latestX, latestY, 7f + pulse * 3f, 7f + pulse * 3f);
    }

    private void drawTriggerMarkers(int chartX, int chartY, int chartW, int chartH) {
        for (int i = 0; i < historyCount; i++) {
            int index = historyIndex(i);
            if (!triggerHistory[index]) {
                continue;
            }
            float xx = historyX(i, chartX, chartW);
            float yy = scoreToY(history[index], chartY, chartH);
            MAIN.stroke(0x66FF5F68);
            MAIN.strokeWeight(1f);
            MAIN.line(xx, yy, xx, chartY + chartH);
            MAIN.noStroke();
            MAIN.fill(ALERT);
            MAIN.ellipse(xx, yy, 8, 8);
        }
    }

    private void drawAxesLabels(int chartX, int chartY, int chartW, int chartH) {
        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7);
        MAIN.textSize(10);
        MAIN.textAlign(PApplet.RIGHT, PApplet.CENTER);
        for (int i = 0; i <= 5; i++) {
            MAIN.text((100 - i * 20) + "%", chartX - 7, chartY + chartH * i / 5f);
        }

        MAIN.textAlign(PApplet.CENTER, PApplet.TOP);
        for (int i = 0; i <= 6; i++) {
            float xx = chartX + chartW * i / 6f;
            int seconds = (6 - i) * 2;
            MAIN.text(i == 6 ? "现在" : "-" + seconds + "s", xx, chartY + chartH + 7);
        }

        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text("意图概率", chartX, chartY - 18);
    }

    private int historyIndex(int chronologicalPosition) {
        int oldestIndex = historyCount < HISTORY_SIZE ? 0 : writeIndex;
        return (oldestIndex + chronologicalPosition) % HISTORY_SIZE;
    }

    private float historyX(int chronologicalPosition, int chartX, int chartW) {
        int emptyPoints = HISTORY_SIZE - historyCount;
        return chartX + chartW * (emptyPoints + chronologicalPosition) / (float) (HISTORY_SIZE - 1);
    }

    private float scoreToY(float score, int chartY, int chartH) {
        return chartY + chartH - PApplet.constrain(score, 0f, 1f) * chartH;
    }

    private void drawGradientBackground(int bx, int by, int bw, int bh) {
        int safeHeight = Math.max(1, bh);
        MAIN.noStroke();
        for (int row = 0; row < safeHeight; row += 2) {
            float amount = row / (float) safeHeight;
            MAIN.fill(MAIN.lerpColor(BG_TOP, BG_BOTTOM, amount));
            MAIN.rect(bx, by + row, bw, 2);
        }
    }
}
