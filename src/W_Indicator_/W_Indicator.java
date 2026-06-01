package W_Indicator_;

import Widget_.Widget;
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

    private static final int GRID_ROWS = 16;
    private static final int HISTORY_COLS = 42;
    private static final long UPDATE_INTERVAL_MS = 100L;

    private static final float Z_GAIN = 0.34f;

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
        if (band == null || band.length == 0) {
            return;
        }

        channelCount = Math.max(1, Math.min(8, Math.min(nchan, band.length)));
        float[][] frame = new float[channelCount][14];

        for (int ch = 0; ch < channelCount; ch++) {
            if (band[ch] == null || band[ch].length < 4) {
                continue;
            }
            float delta = safe(band[ch][0]);
            float theta = safe(band[ch][1]);
            float alpha = safe(band[ch][2]);
            float beta = safe(band[ch][3]);
            engine.compute(delta, theta, alpha, beta, frame[ch]);
        }
        history.pushFrame(frame);
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
        MAIN.textSize(15);
        MAIN.text("14 indicators from PSD", x + 12, y + 7);

        MAIN.fill(COLOR_TEXT_SUB);
        MAIN.textSize(11);
        MAIN.text(
                "8 channels | real-time indicators | MATLAB-style surface",
                x + 12, y + 28
        );
    }

    private void drawSurfaceGrid() {
        int cols = 7;
        int rows = 2;
        int top = y + 46;
        int innerPad = 8;
        int chartW = (w - innerPad * (cols + 1)) / cols;
        int chartH = (h - 56 - innerPad * (rows + 1)) / rows;

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
        MAIN.fill(0x2A1A3559);
        MAIN.rect(x0, y0, w0, h0, 4);
        MAIN.stroke(0x6683A2CC);
        MAIN.noFill();
        MAIN.rect(x0, y0, w0, h0, 4);

        MAIN.fill(COLOR_TEXT);
        MAIN.textFont(p7);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.textSize(10);
        MAIN.text(IndicatorEngine.TITLES[metricIdx], x0 + 6, y0 + 5);

        int padL = 18;
        int padR = 8;
        int padT = 22;
        int padB = 11;
        float gx = x0 + padL;
        float gy = y0 + padT;
        float gw = Math.max(20, w0 - padL - padR);
        float gh = Math.max(20, h0 - padT - padB);

        float ox = gx + gw * 0.19f;
        float oy = gy + gh * 0.86f;
        float vxX = gw * 0.62f;
        float vxY = -gh * 0.02f;
        float vyX = -gw * 0.32f;
        float vyY = -gh * 0.46f;
        float zPix = gh * Z_GAIN;

        int rows = history.getRows();
        int cols = history.getCols();
        float vMin = history.getMetricMin(metricIdx);
        float vMax = history.getMetricMax(metricIdx);
        float span = Math.max(1e-6f, vMax - vMin);

        for (int r = rows - 2; r >= 0; r--) {
            for (int c = 0; c < cols - 1; c++) {
                float v00 = norm(history.get(metricIdx, r, c), vMin, span);
                float v10 = norm(history.get(metricIdx, r + 1, c), vMin, span);
                float v11 = norm(history.get(metricIdx, r + 1, c + 1), vMin, span);
                float v01 = norm(history.get(metricIdx, r, c + 1), vMin, span);
                float vv = 0.25f * (v00 + v10 + v11 + v01);
                int col = parula(vv);

                float u0 = c / (float) (cols - 1);
                float u1 = (c + 1) / (float) (cols - 1);
                float t0 = r / (float) (rows - 1);
                float t1 = (r + 1) / (float) (rows - 1);

                float[] p00 = project(ox, oy, vxX, vxY, vyX, vyY, zPix, u0, t0, v00);
                float[] p10 = project(ox, oy, vxX, vxY, vyX, vyY, zPix, u0, t1, v10);
                float[] p11 = project(ox, oy, vxX, vxY, vyX, vyY, zPix, u1, t1, v11);
                float[] p01 = project(ox, oy, vxX, vxY, vyX, vyY, zPix, u1, t0, v01);

                MAIN.noStroke();
                MAIN.fill((col >> 16) & 0xFF, (col >> 8) & 0xFF, col & 0xFF, 210);
                MAIN.beginShape(PApplet.QUADS);
                MAIN.vertex(p00[0], p00[1]);
                MAIN.vertex(p10[0], p10[1]);
                MAIN.vertex(p11[0], p11[1]);
                MAIN.vertex(p01[0], p01[1]);
                MAIN.endShape();
            }
        }

        MAIN.stroke(255, 255, 255, 42);
        MAIN.strokeWeight(0.6f);
        for (int r = 0; r < rows; r += 2) {
            MAIN.noFill();
            MAIN.beginShape();
            for (int c = 0; c < cols; c++) {
                float v = norm(history.get(metricIdx, r, c), vMin, span);
                float u = c / (float) (cols - 1);
                float t = r / (float) (rows - 1);
                float[] p = project(ox, oy, vxX, vxY, vyX, vyY, zPix, u, t, v);
                MAIN.vertex(p[0], p[1]);
            }
            MAIN.endShape();
        }

        MAIN.stroke(255, 255, 255, 56);
        MAIN.strokeWeight(0.7f);
        MAIN.line(ox, oy, ox + vxX, oy + vxY);
        MAIN.line(ox, oy, ox + vyX, oy + vyY);
        MAIN.line(ox + vxX, oy + vxY, ox + vxX + vyX, oy + vxY + vyY);
        MAIN.line(ox + vyX, oy + vyY, ox + vxX + vyX, oy + vxY + vyY);

        MAIN.fill(COLOR_TEXT_SUB);
        MAIN.textSize(9);
        MAIN.text("Time", ox + vxX + 2, oy + vxY + 2);
        MAIN.text("Electrode", ox + vyX - 6, oy + vyY - 2);
    }

    private float[] project(
            float ox, float oy,
            float vxX, float vxY,
            float vyX, float vyY,
            float zScale,
            float u, float v, float z
    ) {
        float px = ox + u * vxX + v * vyX;
        float py = oy + u * vxY + v * vyY - z * zScale;
        return new float[] {px, py};
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

    private static int parula(float t) {
        t = PApplet.constrain(t, 0f, 1f);
        int c1 = 0x1F2A7C;
        int c2 = 0x2E6BB5;
        int c3 = 0x1FBBA6;
        int c4 = 0x82D34A;
        int c5 = 0xF2E84C;
        if (t < 0.25f) return lerpRgb(c1, c2, t / 0.25f);
        if (t < 0.50f) return lerpRgb(c2, c3, (t - 0.25f) / 0.25f);
        if (t < 0.75f) return lerpRgb(c3, c4, (t - 0.50f) / 0.25f);
        return lerpRgb(c4, c5, (t - 0.75f) / 0.25f);
    }
}
