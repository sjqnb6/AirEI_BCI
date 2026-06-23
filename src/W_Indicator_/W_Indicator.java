package W_Indicator_;

import Widget_.Widget;
import brainflow.BrainFlowError;
import brainflow.DataFilter;
import brainflow.DetrendOperations;
import brainflow.WindowOperations;
import org.apache.commons.lang3.tuple.Pair;
import processing.core.PApplet;
import Globel.GUI;

import static Globel.GUI.*;

public class W_Indicator extends Widget {
    private static final int COLOR_BG_TOP = 0xFF071120;
    private static final int COLOR_BG_BOTTOM = 0xFF0C1B34;
    private static final int COLOR_PANEL = 0xDE13243F;
    private static final int COLOR_BORDER = 0x6683A2CC;
    private static final int COLOR_TEXT = 0xFFEAF2FF;
    private static final int COLOR_TEXT_SUB = 0xFF98AECE;

    private static final int GRID_ROWS = 8;
    private static final int HISTORY_COLS = 42;
    private static final long UPDATE_INTERVAL_MS = 100L;
    private static final int MATLAB_WINDOW_SECONDS = 2;

    private final GUI MAIN;
    private final IndicatorEngine engine = new IndicatorEngine();
    private final IndicatorHistory history = new IndicatorHistory(HISTORY_COLS, GRID_ROWS, 14);

    private long lastUpdateMs = 0L;
    private int channelCount = 8;

    public W_Indicator(GUI MAIN) {
        super(MAIN);
        this.MAIN = MAIN;
    }

        @Override
    public void update() {
        super.update();

        if (MAIN == null || MAIN.currentBoard == null || dataProcessing == null) {
            return;
        }
        if (!MAIN.currentBoard.isStreaming()) {
            return;
        }

        long now = MAIN.millis();
        if (now - lastUpdateMs < UPDATE_INTERVAL_MS) {
            return;
        }
        lastUpdateMs = now;

        float[][] band = dataProcessing.avgPowerInBins;
        if ((band == null || band.length == 0) && (dataProcessingFilteredBuffer == null || dataProcessingFilteredBuffer.length == 0)) {
            return;
        }

        int availableChannels = band != null ? band.length : dataProcessingFilteredBuffer.length;
        channelCount = Math.max(1, Math.min(8, Math.min(nchan, availableChannels)));
        float[][] frame = new float[channelCount][14];

        int sampleRate = Math.max(1, MAIN.currentBoard.getSampleRate());
        for (int ch = 0; ch < channelCount; ch++) {
            float[] matlabLike = computeBandPowersMatlabLike(ch, sampleRate);
            if (matlabLike != null) {
                engine.compute(matlabLike[0], matlabLike[1], matlabLike[2], matlabLike[3], frame[ch]);
                continue;
            }

            if (band != null && ch < band.length && band[ch] != null && band[ch].length >= 4) {
                float delta = safe(band[ch][0]);
                float theta = safe(band[ch][1]);
                float alpha = safe(band[ch][2]);
                float beta = safe(band[ch][3]);
                engine.compute(delta, theta, alpha, beta, frame[ch]);
            }
        }
        history.pushFrame(frame);
    }

    private float[] computeBandPowersMatlabLike(int ch, int sampleRate) {
        if (dataProcessingFilteredBuffer == null || ch < 0 || ch >= dataProcessingFilteredBuffer.length) {
            return null;
        }
        float[] src = dataProcessingFilteredBuffer[ch];
        if (src == null || src.length < 64) {
            return null;
        }

        int target = Math.max(128, sampleRate * MATLAB_WINDOW_SECONDS);
        int nfft = target;
        try {
            nfft = DataFilter.get_nearest_power_of_two(target);
        } catch (BrainFlowError ignore) {
            nfft = target;
        }
        nfft = Math.max(128, nfft);

        int winLen = Math.min(src.length, nfft);
        if ((winLen & 1) == 1) {
            winLen -= 1;
        }
        if (winLen < 64) {
            return null;
        }

        double[] data = new double[winLen];
        int start = src.length - winLen;
        for (int i = 0; i < winLen; i++) {
            data[i] = src[start + i];
        }

        try {
            DataFilter.detrend(data, DetrendOperations.CONSTANT.get_code());
            Pair<double[], double[]> psd = DataFilter.get_psd_welch(
                    data,
                    winLen,
                    winLen / 2,
                    sampleRate,
                    WindowOperations.NO_WINDOW.get_code()
            );

            double delta = DataFilter.get_band_power(psd, 0.0, 4.0);
            double theta = DataFilter.get_band_power(psd, 4.0, 7.0);
            double alpha = DataFilter.get_band_power(psd, 8.0, 12.0);
            double beta = DataFilter.get_band_power(psd, 13.0, 30.0);

            if (!Double.isFinite(delta + theta + alpha + beta)) {
                return null;
            }

            // Align visual value scale with MATLAB script style: pow = Fs * sum(Pxx).
            double scale = Math.max(1, sampleRate);
            return new float[] {
                    safe((float) (delta * scale)),
                    safe((float) (theta * scale)),
                    safe((float) (alpha * scale)),
                    safe((float) (beta * scale))
            };
        } catch (BrainFlowError e) {
            return null;
        }
    }
    @Override
    public void draw() {
        super.draw();

        MAIN.pushStyle();
        drawGradientBackground(x, y - 1, w, h + 1);
        MAIN.noStroke();
        MAIN.fill(COLOR_PANEL);
        MAIN.rect(x, y - 1, w, h + 1);
        MAIN.stroke(COLOR_BORDER);
        MAIN.noFill();
        MAIN.rect(x, y - 1, w, h + 1);

        drawHeader();
        drawSurfaceGrid();
        MAIN.popStyle();
    }

    private void drawHeader() {
        MAIN.fill(COLOR_TEXT);
        MAIN.textFont(p7);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.textSize(17);
        MAIN.text("14 indicators from PSD", x + 12, y + 7);

        MAIN.fill(COLOR_TEXT_SUB);
        MAIN.textSize(11);
        MAIN.text(
                "8 channels | real-time indicators | 2D heatmap + contours",
                x + 12, y + 28
        );
    }

    private void drawSurfaceGrid() {
        int cols = 7;
        int rows = 2;
        int top = y + 48;
        int innerPad = 6;
        int chartW = (w - innerPad * (cols + 1)) / cols;
        int chartH = (h - 58 - innerPad * (rows + 1)) / rows;

        for (int i = 0; i < 14; i++) {
            int r = i / cols;
            int c = i % cols;
            int cx = x + innerPad + c * (chartW + innerPad);
            int cy = top + innerPad + r * (chartH + innerPad);
            drawSingleSurface(cx, cy, chartW, chartH, i);
        }
    }

    private void drawSingleSurface(int x0, int y0, int w0, int h0, int metricIdx) {
        MAIN.noStroke();
        MAIN.fill(0x251A3559);
        MAIN.rect(x0, y0, w0, h0, 4);
        MAIN.stroke(0x88A8C8EE);
        MAIN.strokeWeight(1.0f);
        MAIN.noFill();
        MAIN.rect(x0, y0, w0, h0, 4);

        // Strong, clean metric label with subtle glow.
        MAIN.textFont(p7);
        MAIN.textAlign(PApplet.CENTER, PApplet.TOP);
        MAIN.textSize(15);
        float titleX = x0 + w0 * 0.50f;
        float titleY = y0 + 3;
        MAIN.fill(90, 170, 255, 85);
        MAIN.rect(x0 + 3, y0 + 2, w0 - 6, 18, 3);
        MAIN.fill(12, 22, 36, 200);
        MAIN.rect(x0 + 4, y0 + 3, w0 - 8, 16, 2);
        MAIN.fill(200, 230, 255, 140);
        MAIN.text(IndicatorEngine.TITLES[metricIdx], titleX + 0.8f, titleY + 0.8f);
        MAIN.fill(COLOR_TEXT);
        MAIN.text(IndicatorEngine.TITLES[metricIdx], titleX, titleY);

        float left = x0 + 14;
        float top = y0 + 24;
        float right = x0 + w0 - 12;
        float bottom = y0 + h0 - 14;
        float cbW = 5f;
        float gap = 3f;
        float gx = left;
        float gy = top;
        float gw = Math.max(24, (right - left) - cbW - gap);
        float gh = Math.max(24, bottom - top);

        int rows = history.getRows();
        int cols = history.getCols();
        float vMin = history.getMetricMin(metricIdx);
        float vMax = history.getMetricMax(metricIdx);
        float span = Math.max(1e-6f, vMax - vMin);
        float cellW = gw / Math.max(1, cols - 1);
        float cellH = gh / Math.max(1, rows - 1);

        MAIN.noStroke();
        for (int r = 0; r < rows - 1; r++) {
            for (int c = 0; c < cols - 1; c++) {
                float v00 = norm(history.get(metricIdx, r, c), vMin, span);
                float v10 = norm(history.get(metricIdx, r + 1, c), vMin, span);
                float v11 = norm(history.get(metricIdx, r + 1, c + 1), vMin, span);
                float v01 = norm(history.get(metricIdx, r, c + 1), vMin, span);
                float vv = 0.25f * (v00 + v10 + v11 + v01);
                int col = parula(vv);
                MAIN.fill((col >> 16) & 0xFF, (col >> 8) & 0xFF, col & 0xFF, 255);
                MAIN.rect(gx + c * cellW, gy + r * cellH, cellW + 0.6f, cellH + 0.6f);
            }
        }

        drawContours(metricIdx, gx, gy, gw, gh, rows, cols, vMin, span);

        MAIN.stroke(145, 175, 214, 110);
        MAIN.strokeWeight(0.85f);
        for (int t = 1; t <= 2; t++) {
            float xx = gx + (t / 2.0f) * gw;
            float yy = gy + (t / 2.0f) * gh;
            MAIN.line(xx, gy, xx, gy + gh);
            MAIN.line(gx, yy, gx + gw, yy);
        }

        MAIN.stroke(208, 228, 252, 190);
        MAIN.strokeWeight(1.2f);
        MAIN.noFill();
        MAIN.rect(gx, gy, gw, gh);

        drawColorBar(gx + gw + gap, gy, cbW, gh);
        drawAxes(gx, gy, gw, gh, rows, cols, vMin, vMax);
        drawPeakMark(metricIdx, gx, gy, cellW, cellH, rows, cols, vMin, span);
    }

    private void drawContours(int metricIdx, float gx, float gy, float gw, float gh, int rows, int cols, float vMin, float span) {
        float[] levels = {0.22f, 0.48f, 0.72f, 0.90f};
        float cellW = gw / Math.max(1, cols - 1);
        float cellH = gh / Math.max(1, rows - 1);

        MAIN.strokeWeight(1.15f);
        for (int li = 0; li < levels.length; li++) {
            float level = levels[li];
            int col = li < 2 ? 0xD8E8FF : (li < 3 ? 0xC7E0FF : 0xFFF8C5);
            MAIN.stroke((col >> 16) & 0xFF, (col >> 8) & 0xFF, col & 0xFF, 235);
            for (int r = 0; r < rows - 1; r++) {
                for (int c = 0; c < cols - 1; c++) {
                    float v00 = norm(history.get(metricIdx, r, c), vMin, span);
                    float v01 = norm(history.get(metricIdx, r, c + 1), vMin, span);
                    float v11 = norm(history.get(metricIdx, r + 1, c + 1), vMin, span);
                    float v10 = norm(history.get(metricIdx, r + 1, c), vMin, span);
                    float x = gx + c * cellW;
                    float y = gy + r * cellH;
                    drawContourCell(x, y, cellW, cellH, v00, v01, v11, v10, level);
                }
            }
        }
    }

    private void drawContourCell(
            float x, float y, float cw, float ch,
            float v00, float v01, float v11, float v10,
            float level
    ) {
        float[] xs = new float[4];
        float[] ys = new float[4];
        int n = 0;

        if (cross(v00, v01, level)) {
            float t = interp(v00, v01, level);
            xs[n] = x + t * cw; ys[n] = y; n++;
        }
        if (cross(v01, v11, level)) {
            float t = interp(v01, v11, level);
            xs[n] = x + cw; ys[n] = y + t * ch; n++;
        }
        if (cross(v10, v11, level)) {
            float t = interp(v10, v11, level);
            xs[n] = x + t * cw; ys[n] = y + ch; n++;
        }
        if (cross(v00, v10, level)) {
            float t = interp(v00, v10, level);
            xs[n] = x; ys[n] = y + t * ch; n++;
        }

        if (n == 2) {
            MAIN.line(xs[0], ys[0], xs[1], ys[1]);
        } else if (n == 4) {
            float center = 0.25f * (v00 + v01 + v11 + v10);
            if (center >= level) {
                MAIN.line(xs[0], ys[0], xs[1], ys[1]);
                MAIN.line(xs[2], ys[2], xs[3], ys[3]);
            } else {
                MAIN.line(xs[0], ys[0], xs[3], ys[3]);
                MAIN.line(xs[1], ys[1], xs[2], ys[2]);
            }
        }
    }

    private void drawColorBar(float x, float y, float w, float h) {
        int steps = 64;
        for (int i = 0; i < steps; i++) {
            float t0 = i / (float) steps;
            float t1 = (i + 1) / (float) steps;
            int col = parula(1f - t0);
            MAIN.noStroke();
            MAIN.fill((col >> 16) & 0xFF, (col >> 8) & 0xFF, col & 0xFF, 255);
            MAIN.rect(x, y + t0 * h, w, (t1 - t0) * h + 0.8f);
        }
        MAIN.stroke(205, 225, 248, 180);
        MAIN.strokeWeight(0.8f);
        MAIN.noFill();
        MAIN.rect(x, y, w, h);
    }

    private void drawAxes(float gx, float gy, float gw, float gh, int rows, int cols, float vMin, float vMax) {
        float vMid = 0.5f * (vMin + vMax);

        MAIN.fill(225, 238, 255, 225);
        MAIN.textFont(p7);
        MAIN.textSize(9);
        MAIN.textAlign(PApplet.LEFT, PApplet.CENTER);

        MAIN.textAlign(PApplet.LEFT, PApplet.CENTER);
        MAIN.text("V", gx + gw + 9, gy + 5);
        MAIN.text(formatTickShort(vMax), gx + gw + 9, gy + 4);
        MAIN.text(formatTickShort(vMid), gx + gw + 9, gy + gh * 0.5f);
        MAIN.text(formatTickShort(vMin), gx + gw + 9, gy + gh - 2);
    }

    private void drawPeakMark(int metricIdx, float gx, float gy, float cellW, float cellH, int rows, int cols, float vMin, float span) {
        int bestR = 0;
        int bestC = 0;
        float best = -1f;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float v = norm(history.get(metricIdx, r, c), vMin, span);
                if (v > best) {
                    best = v;
                    bestR = r;
                    bestC = c;
                }
            }
        }
        float px = gx + bestC * cellW;
        float py = gy + bestR * cellH;
        MAIN.stroke(255, 245, 165, 225);
        MAIN.strokeWeight(1.0f);
        MAIN.line(px - 3, py, px + 3, py);
        MAIN.line(px, py - 3, px, py + 3);
    }

    private void drawGradientBackground(int x0, int y0, int w0, int h0) {
        for (int i = 0; i < h0; i++) {
            float t = i / (float) Math.max(1, h0 - 1);
            int c = lerpRgb(COLOR_BG_TOP, COLOR_BG_BOTTOM, t);
            MAIN.stroke((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF);
            MAIN.line(x0, y0 + i, x0 + w0, y0 + i);
        }
        float glow = 0.5f + 0.5f * (float) Math.sin(MAIN.frameCount * 0.01f);
        MAIN.noStroke();
        MAIN.fill(78, 206, 255, (int) (16 * glow));
        MAIN.ellipse(x0 + w0 * 0.18f, y0 + h0 * 0.24f, w0 * 0.42f, h0 * 0.35f);
        MAIN.fill(102, 120, 255, (int) (12 * glow));
        MAIN.ellipse(x0 + w0 * 0.82f, y0 + h0 * 0.78f, w0 * 0.36f, h0 * 0.31f);
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

    private static float safe(float v) {
        return Float.isFinite(v) ? Math.max(0f, v) : 0f;
    }

    private static float norm(float v, float vMin, float span) {
        return PApplet.constrain((v - vMin) / span, 0f, 1f);
    }

    private static boolean cross(float a, float b, float level) {
        return (a < level && b >= level) || (a >= level && b < level);
    }

    private static float interp(float a, float b, float level) {
        float d = (b - a);
        if (Math.abs(d) < 1e-9f) {
            return 0.5f;
        }
        return PApplet.constrain((level - a) / d, 0f, 1f);
    }

    private static String formatTickShort(float v) {
        float av = Math.abs(v);
        if (av >= 1000f) return String.format("%.0f", v);
        if (av >= 100f) return String.format("%.1f", v);
        if (av >= 10f) return String.format("%.1f", v);
        if (av >= 1f) return String.format("%.2f", v);
        return String.format("%.3f", v);
    }

    private static int parula(float t) {
        t = PApplet.constrain(t, 0f, 1f);
        // Darker, punchier palette for stronger contrast.
        int c1 = 0x0D1B72;
        int c2 = 0x1D4FB0;
        int c3 = 0x0B8EA2;
        int c4 = 0x4F9715;
        int c5 = 0xC8AB10;
        if (t < 0.25f) return lerpRgb(c1, c2, t / 0.25f);
        if (t < 0.50f) return lerpRgb(c2, c3, (t - 0.25f) / 0.25f);
        if (t < 0.75f) return lerpRgb(c3, c4, (t - 0.50f) / 0.25f);
        return lerpRgb(c4, c5, (t - 0.75f) / 0.25f);
    }
}







