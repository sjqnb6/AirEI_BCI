package W_Connectivity_;

import Widget_.Widget;
import processing.core.PApplet;

import java.util.Arrays;

import static Globel.GUI.*;

import Globel.GUI;

public class W_Connectivity extends Widget {
    private static final int BAND_COUNT = 5;
    private static final int[] BAND_LOW_HZ = {1, 4, 8, 13, 30};
    private static final int[] BAND_HIGH_HZ = {4, 8, 13, 30, 55};
    private static final String[] BAND_NAMES = {"Delta", "Theta", "Alpha", "Beta", "Gamma"};

    private static final float[] THRESHOLD_OPTIONS = {0.30f, 0.40f, 0.50f, 0.60f, 0.70f, 0.80f, 0.90f};
    private static final String[] THRESHOLD_LABELS = {"0.30", "0.40", "0.50", "0.60", "0.70", "0.80", "0.90"};

    // Reuses the default OpenBCI headplot 16-channel geometry.
    private static final float[][] ELEC_REL_XY = {
            {-0.125f, -0.416f},
            {0.125f, -0.416f},
            {-0.2f, 0.0f},
            {0.2f, 0.0f},
            {-0.3425f, 0.27f},
            {0.3425f, 0.27f},
            {-0.125f, 0.416f},
            {0.125f, 0.416f},
            {-0.3425f, -0.27f},
            {0.3425f, -0.27f},
            {-0.18f, -0.15f},
            {0.18f, -0.15f},
            {-0.416f, 0.0f},
            {0.416f, 0.0f},
            {-0.18f, 0.15f},
            {0.18f, 0.15f}
    };

    private static final String[] CH_LABELS_4 = {"Fp1", "Fp2", "C3", "C4"};
    private static final String[] CH_LABELS_8 = {"Fp1", "Fp2", "C3", "C4", "P7", "P8", "O1", "O2"};
    private static final String[] CH_LABELS_16 = {
            "Fp1", "Fp2", "C3", "C4", "P7", "P8", "O1", "O2",
            "F7", "F8", "FC3", "FC4", "T7", "T8", "CP3", "CP4"
    };

    private static final int[][] BAND_RGB = {
            {65, 115, 255},   // Delta
            {60, 210, 255},   // Theta
            {85, 255, 135},   // Alpha
            {255, 190, 70},   // Beta
            {255, 90, 70}     // Gamma
    };

    private static final int BG_COLOR = 0xFF1A1A2E;
    private static final int PANEL_COLOR = 0xFF1F263A;
    private static final int GRID_COLOR = 0x2FFFFFFF;
    private static final int TEXT_COLOR = 0xFFE7EDF8;
    private static final int DIM_TEXT_COLOR = 0xFF9AA7C0;
    private static final int NODE_RING_COLOR = 0x66FFFFFF;
    private static final int HEAD_OUTLINE_COLOR = 0x88FFFFFF;

    private static final float COH_SMOOTH = 0.10f;
    private static final int PARTICLES_PER_EDGE = 3;
    private static final float PARTICLE_TAIL_STEP = 0.04f;

    private final GUI MAIN;

    private float[][][] coherence;
    private float[][] nodeStrength;
    private float[][][] particlePhase;
    private float[][] screenXY;

    private int bandIndex = 2;      // Alpha default
    private int modeIndex = 0;      // 0 = arcs, 1 = matrix
    private int thresholdIndex = 2; // 0.50 default
    private int animIndex = 0;      // 0 = on, 1 = off

    public W_Connectivity(GUI MAIN) {
        super(MAIN);
        this.MAIN = MAIN;

        addDropdown("ConnBand", "频带", Arrays.asList(BAND_NAMES), bandIndex);
        addDropdown("ConnThreshold", "阈值", Arrays.asList(THRESHOLD_LABELS), thresholdIndex);
        addDropdown("ConnMode", "模式", Arrays.asList("弧线图", "矩阵图"), modeIndex);
        addDropdown("ConnAnim", "流动", Arrays.asList("开启", "关闭"), animIndex);

        ensureBuffers();
    }

    public void ConnBand(int n) {
        bandIndex = PApplet.constrain(n, 0, BAND_COUNT - 1);
    }

    public void ConnThreshold(int n) {
        thresholdIndex = PApplet.constrain(n, 0, THRESHOLD_OPTIONS.length - 1);
    }

    public void ConnMode(int n) {
        modeIndex = PApplet.constrain(n, 0, 1);
    }

    public void ConnAnim(int n) {
        animIndex = PApplet.constrain(n, 0, 1);
    }

    public void update() {
        super.update();
        ensureBuffers();
        computeCoherence();
        updateNodeStrength();
        updateParticles();
    }

    public void draw() {
        super.draw();

        MAIN.pushStyle();
        MAIN.noStroke();
        MAIN.fill(BG_COLOR);
        MAIN.rect(x, y - 1, w, h + 1);

        int pad = 10;
        int panelX = x + pad;
        int panelY = y + pad;
        int panelW = w - pad * 2;
        int panelH = h - pad * 2;

        MAIN.fill(PANEL_COLOR);
        MAIN.rect(panelX, panelY, panelW, panelH, 4);

        int footerH = 34;
        int headerH = 42;
        int contentX = panelX + 10;
        int contentY = panelY + headerH;
        int contentW = panelW - 20;
        int contentH = panelH - headerH - footerH - 8;

        drawHeader(panelX, panelY, panelW, headerH);
        if (modeIndex == 0) {
            drawArcsMode(contentX, contentY, contentW, contentH);
        } else {
            drawMatrixMode(contentX, contentY, contentW, contentH);
        }
        drawFooter(panelX, panelY + panelH - footerH, panelW, footerH);
        MAIN.popStyle();
    }

    private void drawHeader(int panelX, int panelY, int panelW, int headerH) {
        MAIN.fill(TEXT_COLOR);
        MAIN.textFont(p7);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.textSize(14);
        MAIN.text("脑电连接性图谱", panelX + 12, panelY + 10);

        MAIN.fill(DIM_TEXT_COLOR);
        MAIN.textSize(11);
        MAIN.text(
                BAND_NAMES[bandIndex] + "  |  阈值 " + THRESHOLD_LABELS[thresholdIndex]
                        + "  |  " + (modeIndex == 0 ? "弧线图" : "矩阵图")
                        + "  |  流动 " + (animIndex == 0 ? "开启" : "关闭"),
                panelX + 12, panelY + headerH - 14
        );

        MAIN.stroke(HEAD_OUTLINE_COLOR);
        MAIN.strokeWeight(1f);
        MAIN.line(panelX + 10, panelY + headerH, panelX + panelW - 10, panelY + headerH);
    }

    private void drawFooter(int x0, int y0, int w0, int h0) {
        MAIN.stroke(HEAD_OUTLINE_COLOR);
        MAIN.strokeWeight(1f);
        MAIN.line(x0 + 10, y0, x0 + w0 - 10, y0);

        int boxY = y0 + 9;
        int boxW = 12;
        int gap = 12;
        int cursorX = x0 + 14;

        MAIN.textFont(p5);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        for (int b = 0; b < BAND_COUNT; b++) {
            int[] rgb = BAND_RGB[b];
            MAIN.noStroke();
            MAIN.fill(rgb[0], rgb[1], rgb[2], b == bandIndex ? 255 : 105);
            MAIN.rect(cursorX, boxY, boxW, boxW, 2);
            MAIN.fill(DIM_TEXT_COLOR);
            MAIN.text(BAND_NAMES[b], cursorX + boxW + 5, boxY - 1);
            cursorX += boxW + 5 + (int) MAIN.textWidth(BAND_NAMES[b]) + gap;
        }
    }

    private void drawArcsMode(int rx, int ry, int rw, int rh) {
        int n = Math.min(nchan, ELEC_REL_XY.length);
        if (n <= 0) {
            return;
        }

        float cx = rx + rw * 0.5f;
        float cy = ry + rh * 0.52f;
        float rad = Math.min(rw, rh) * 0.40f;

        mapElectrodesToScreen(cx, cy, rad, n);

        drawHeadOutline(cx, cy, rad);
        drawConnections(cx, cy, n);
        drawNodes(n);
    }

    private void drawHeadOutline(float cx, float cy, float rad) {
        MAIN.noFill();
        MAIN.stroke(HEAD_OUTLINE_COLOR);
        MAIN.strokeWeight(1.3f);
        MAIN.ellipse(cx, cy, rad * 2f, rad * 2f);

        float nx = rad * 0.10f;
        float ny = rad * 0.90f;
        MAIN.line(cx - nx, cy - ny, cx, cy - rad * 1.07f);
        MAIN.line(cx + nx, cy - ny, cx, cy - rad * 1.07f);
    }

    private void drawConnections(float cx, float cy, int n) {
        float threshold = THRESHOLD_OPTIONS[thresholdIndex];
        int[] rgb = BAND_RGB[bandIndex];

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                float c = coherence[i][j][bandIndex];
                if (c < threshold) {
                    continue;
                }

                float x1 = screenXY[i][0];
                float y1 = screenXY[i][1];
                float x2 = screenXY[j][0];
                float y2 = screenXY[j][1];

                float mx = (x1 + x2) * 0.5f;
                float my = (y1 + y2) * 0.5f;
                float cpx = PApplet.lerp(mx, cx, 0.40f);
                float cpy = PApplet.lerp(my, cy, 0.40f);

                float t = PApplet.map(c, threshold, 1f, 0f, 1f);
                t = PApplet.constrain(t, 0f, 1f);
                float sw = PApplet.lerp(0.5f, 7.0f, t);
                int alpha = (int) PApplet.lerp(40f, 220f, t);

                MAIN.noFill();
                MAIN.stroke(rgb[0], rgb[1], rgb[2], alpha);
                MAIN.strokeWeight(sw);
                MAIN.bezier(x1, y1, cpx, cpy, cpx, cpy, x2, y2);

                if (animIndex == 0) {
                    drawFlowParticles(i, j, x1, y1, cpx, cpy, x2, y2, c, rgb);
                }
            }
        }
    }

    private void drawFlowParticles(
            int i,
            int j,
            float x1,
            float y1,
            float cpx,
            float cpy,
            float x2,
            float y2,
            float coherenceVal,
            int[] rgb
    ) {
        for (int p = 0; p < PARTICLES_PER_EDGE; p++) {
            float phase = particlePhase[i][j][p];
            drawParticleWithTail(phase, x1, y1, cpx, cpy, x2, y2, rgb, coherenceVal);
        }
    }

    private void drawParticleWithTail(
            float phase,
            float x1,
            float y1,
            float cpx,
            float cpy,
            float x2,
            float y2,
            int[] rgb,
            float coherenceVal
    ) {
        float baseSize = PApplet.lerp(2.3f, 4.4f, coherenceVal);

        for (int k = 0; k < 4; k++) {
            float t = phase - k * PARTICLE_TAIL_STEP;
            while (t < 0f) {
                t += 1f;
            }
            while (t >= 1f) {
                t -= 1f;
            }

            float px = bezierPoint(x1, cpx, cpx, x2, t);
            float py = bezierPoint(y1, cpy, cpy, y2, t);
            float a = PApplet.lerp(220f, 35f, k / 3f);
            float s = baseSize * PApplet.lerp(1f, 0.45f, k / 3f);
            MAIN.noStroke();
            MAIN.fill(Math.min(255, rgb[0] + 25), Math.min(255, rgb[1] + 25), Math.min(255, rgb[2] + 25), a);
            MAIN.ellipse(px, py, s, s);
        }
    }

    private void drawNodes(int n) {
        float pulse = 1.0f + (float) Math.sin(MAIN.frameCount * 0.05f) * 0.16f;
        String[] labels = getChannelLabels(n);

        MAIN.textFont(p6);
        MAIN.textAlign(PApplet.CENTER, PApplet.CENTER);
        for (int i = 0; i < n; i++) {
            float x = screenXY[i][0];
            float y = screenXY[i][1];
            float s = PApplet.constrain(nodeStrength[i][bandIndex], 0f, 1f);
            int col = getHeatColor(s);

            float halo = 16f + 10f * s * pulse;
            MAIN.noStroke();
            MAIN.fill((col >> 16) & 0xFF, (col >> 8) & 0xFF, col & 0xFF, 28 + (int) (70f * s));
            MAIN.ellipse(x, y, halo, halo);

            MAIN.stroke(NODE_RING_COLOR);
            MAIN.strokeWeight(1f);
            MAIN.fill(col);
            MAIN.ellipse(x, y, 16f, 16f);

            MAIN.fill(TEXT_COLOR);
            MAIN.text(labels[i], x, y - 15f);
        }
    }

    private void drawMatrixMode(int rx, int ry, int rw, int rh) {
        int n = Math.min(nchan, ELEC_REL_XY.length);
        if (n <= 0) {
            return;
        }

        String[] labels = getChannelLabels(n);
        int leftPad = 44;
        int topPad = 22;
        int rightPad = 10;
        int bottomPad = 18;

        int gx = rx + leftPad;
        int gy = ry + topPad;
        int gw = Math.max(16, rw - leftPad - rightPad);
        int gh = Math.max(16, rh - topPad - bottomPad);
        float cellW = gw / (float) n;
        float cellH = gh / (float) n;
        float threshold = THRESHOLD_OPTIONS[thresholdIndex];

        MAIN.stroke(GRID_COLOR);
        MAIN.strokeWeight(1f);
        for (int i = 0; i <= n; i++) {
            float vx = gx + i * cellW;
            float vy = gy + i * cellH;
            MAIN.line(vx, gy, vx, gy + gh);
            MAIN.line(gx, vy, gx + gw, vy);
        }

        MAIN.noStroke();
        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                float v = (r == c) ? 1f : coherence[r][c][bandIndex];
                float k = PApplet.constrain((v - threshold) / Math.max(0.001f, (1f - threshold)), 0f, 1f);
                int col = getBandColorMix(k, BAND_RGB[bandIndex]);

                int alpha = r == c ? 225 : (int) PApplet.lerp(22f, 210f, k);
                float cx = gx + c * cellW;
                float cy = gy + r * cellH;

                MAIN.fill((col >> 16) & 0xFF, (col >> 8) & 0xFF, col & 0xFF, alpha);
                MAIN.rect(cx + 1, cy + 1, Math.max(1f, cellW - 2), Math.max(1f, cellH - 2));
            }
        }

        MAIN.fill(DIM_TEXT_COLOR);
        MAIN.textFont(p6);
        MAIN.textAlign(PApplet.CENTER, PApplet.CENTER);
        for (int i = 0; i < n; i++) {
            float tx = gx + i * cellW + cellW * 0.5f;
            float ty = gy + i * cellH + cellH * 0.5f;
            MAIN.text(labels[i], tx, gy - 10);
            MAIN.text(labels[i], gx - 20, ty);
        }

        if (MAIN.mouseX >= gx && MAIN.mouseX < gx + gw && MAIN.mouseY >= gy && MAIN.mouseY < gy + gh) {
            int cc = (int) ((MAIN.mouseX - gx) / cellW);
            int rr = (int) ((MAIN.mouseY - gy) / cellH);
            rr = PApplet.constrain(rr, 0, n - 1);
            cc = PApplet.constrain(cc, 0, n - 1);
            float val = rr == cc ? 1f : coherence[rr][cc][bandIndex];

            MAIN.noStroke();
            MAIN.fill(10, 16, 28, 235);
            int tipW = 180;
            int tipH = 40;
            int tipX = PApplet.constrain(MAIN.mouseX + 14, rx + 6, rx + rw - tipW - 6);
            int tipY = PApplet.constrain(MAIN.mouseY + 14, ry + 6, ry + rh - tipH - 6);
            MAIN.rect(tipX, tipY, tipW, tipH, 3);

            MAIN.fill(TEXT_COLOR);
            MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
            MAIN.textFont(p6);
            MAIN.text(
                    labels[rr] + " <-> " + labels[cc] + "   " + PApplet.nf(val, 1, 3),
                    tipX + 8, tipY + 11
            );
        }
    }

    private void ensureBuffers() {
        int n = Math.max(1, Math.min(nchan, ELEC_REL_XY.length));
        if (coherence != null && coherence.length == n) {
            return;
        }

        coherence = new float[n][n][BAND_COUNT];
        nodeStrength = new float[n][BAND_COUNT];
        particlePhase = new float[n][n][PARTICLES_PER_EDGE];
        screenXY = new float[n][2];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                for (int p = 0; p < PARTICLES_PER_EDGE; p++) {
                    particlePhase[i][j][p] = (p / (float) PARTICLES_PER_EDGE + ((i + j) * 0.031f)) % 1f;
                }
            }
        }
    }

    private void computeCoherence() {
        int n = coherence.length;
        if (fftBuff == null || fftBuff.length < n || fftBuff[0] == null) {
            return;
        }

        int specSize = fftBuff[0].specSize();
        if (specSize <= 1) {
            return;
        }

        float[][] r = new float[n][];
        float[][] im = new float[n][];
        for (int ch = 0; ch < n; ch++) {
            if (fftBuff[ch] == null) {
                return;
            }
            r[ch] = fftBuff[ch].getSpectrumReal();
            im[ch] = fftBuff[ch].getSpectrumImaginary();
            if (r[ch] == null || im[ch] == null) {
                return;
            }
        }

        for (int i = 0; i < n; i++) {
            for (int j = i; j < n; j++) {
                for (int b = 0; b < BAND_COUNT; b++) {
                    float sumRe = 0f;
                    float sumIm = 0f;
                    float sumPxx = 0f;
                    float sumPyy = 0f;
                    int count = 0;

                    for (int k = 1; k < specSize; k++) {
                        float hz = fftBuff[i].indexToFreq(k);
                        if (hz < BAND_LOW_HZ[b] || hz >= BAND_HIGH_HZ[b]) {
                            continue;
                        }

                        float xr = r[i][k];
                        float xi = im[i][k];
                        float yr = r[j][k];
                        float yi = im[j][k];

                        // X * conj(Y)
                        float pxyRe = xr * yr + xi * yi;
                        float pxyIm = xi * yr - xr * yi;

                        sumRe += pxyRe;
                        sumIm += pxyIm;
                        sumPxx += xr * xr + xi * xi;
                        sumPyy += yr * yr + yi * yi;
                        count++;
                    }

                    float raw = 0f;
                    if (count > 0 && sumPxx > 1e-12f && sumPyy > 1e-12f) {
                        float num = sumRe * sumRe + sumIm * sumIm;
                        float den = sumPxx * sumPyy;
                        raw = PApplet.constrain(num / den, 0f, 1f);
                    }

                    float prev = coherence[i][j][b];
                    float smoothed = PApplet.lerp(prev, raw, COH_SMOOTH);
                    coherence[i][j][b] = smoothed;
                    coherence[j][i][b] = smoothed;
                }
            }
        }
    }

    private void updateNodeStrength() {
        int n = coherence.length;
        for (int i = 0; i < n; i++) {
            for (int b = 0; b < BAND_COUNT; b++) {
                float sum = 0f;
                for (int j = 0; j < n; j++) {
                    if (i != j) {
                        sum += coherence[i][j][b];
                    }
                }
                nodeStrength[i][b] = n > 1 ? (sum / (n - 1)) : 0f;
            }
        }
    }

    private void updateParticles() {
        if (animIndex != 0) {
            return;
        }
        int n = coherence.length;
        float threshold = THRESHOLD_OPTIONS[thresholdIndex];
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                float c = coherence[i][j][bandIndex];
                if (c < threshold) {
                    continue;
                }
                float speed = 0.0035f + 0.020f * c;
                for (int p = 0; p < PARTICLES_PER_EDGE; p++) {
                    float v = particlePhase[i][j][p] + speed * (1f + p * 0.16f);
                    if (v >= 1f) {
                        v -= 1f;
                    }
                    particlePhase[i][j][p] = v;
                    particlePhase[j][i][p] = v;
                }
            }
        }
    }

    private void mapElectrodesToScreen(float cx, float cy, float rad, int n) {
        for (int i = 0; i < n; i++) {
            screenXY[i][0] = cx + ELEC_REL_XY[i][0] * rad * 1.95f;
            screenXY[i][1] = cy + ELEC_REL_XY[i][1] * rad * 1.95f;
        }
    }

    private String[] getChannelLabels(int n) {
        if (n <= 4) {
            return CH_LABELS_4;
        }
        if (n <= 8) {
            return CH_LABELS_8;
        }
        if (n <= 16) {
            return CH_LABELS_16;
        }
        String[] labels = new String[n];
        for (int i = 0; i < n; i++) {
            labels[i] = "Ch" + (i + 1);
        }
        return labels;
    }

    private float bezierPoint(float a, float b, float c, float d, float t) {
        return MAIN.bezierPoint(a, b, c, d, t);
    }

    private int getHeatColor(float t) {
        t = PApplet.constrain(t, 0f, 1f);
        if (t < 0.33f) {
            float u = t / 0.33f;
            return rgb(
                    (int) PApplet.lerp(70, 45, u),
                    (int) PApplet.lerp(125, 208, u),
                    (int) PApplet.lerp(255, 220, u)
            );
        } else if (t < 0.66f) {
            float u = (t - 0.33f) / 0.33f;
            return rgb(
                    (int) PApplet.lerp(45, 245, u),
                    (int) PApplet.lerp(208, 225, u),
                    (int) PApplet.lerp(220, 78, u)
            );
        }
        float u = (t - 0.66f) / 0.34f;
        return rgb(
                (int) PApplet.lerp(245, 255, u),
                (int) PApplet.lerp(225, 80, u),
                (int) PApplet.lerp(78, 70, u)
        );
    }

    private int getBandColorMix(float t, int[] baseRgb) {
        t = PApplet.constrain(t, 0f, 1f);
        int r = (int) PApplet.lerp(24, baseRgb[0], t);
        int g = (int) PApplet.lerp(34, baseRgb[1], t);
        int b = (int) PApplet.lerp(48, baseRgb[2], t);
        return rgb(r, g, b);
    }

    private int rgb(int r, int g, int b) {
        return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }
}
