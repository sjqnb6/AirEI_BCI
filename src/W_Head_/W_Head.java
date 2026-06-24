package W_Head_;

import Board_.Board;
import Widget_.Widget;
import processing.core.PApplet;

import java.util.Arrays;

import static Globel.GUI.*;
import static SystemManager.GF.getNfftSafe;

import Globel.GUI;

public class W_Head extends Widget {
    GUI MAIN;
    protected PApplet pApplet;

    private static final int BG_TOP = 0xFF0A1220;
    private static final int BG_BOTTOM = 0xFF101E36;
    private static final int PANEL = 0xCC13243F;
    private static final int PANEL_STROKE = 0x6683A2CC;
    private static final int CARD = 0xC61A2F50;
    private static final int CARD_STROKE = 0x66A7C6F5;
    private static final int TEXT_MAIN = 0xFFEAF2FF;
    private static final int TEXT_SUB = 0xFF9CB4D6;
    private static final int TEXT_ACCENT = 0xFF7DE8FF;
    private static final int TEXT_WARM = 0xFFFFE486;
    private static final int GRID_FAR = 0x335684A8;
    private static final int GRID_NEAR = 0xCC8FC2FF;
    private static final int GRID_LINE = 0x4A76A9D8;
    private static final int CONTOUR_LOW = 0x4495D7FF;
    private static final int CONTOUR_HIGH = 0xAAFFE08A;

    private static final int GRID_COLS = 28;
    private static final int GRID_ROWS = 18;

    private final float[][] terrain = new float[GRID_ROWS][GRID_COLS];
    private float[] channelEnergy = new float[Math.max(1, nchan)];

    private int bandLo = 8;
    private int bandHi = 30;
    private float terrainGain = 1.0f;
    private float smoothing = 0.16f;
    private int peakMarkerR = GRID_ROWS / 2;
    private int peakMarkerC = GRID_COLS / 2;
    private float peakMarkerValue = 0f;

    public W_Head(GUI MAIN) {
        super(MAIN);
        this.MAIN = MAIN;
        this.pApplet = MAIN;

        addDropdown("TerrainBand", "频段", Arrays.asList(
                "Theta-Beta 4-25Hz",
                "Alpha-Beta 8-30Hz",
                "Beta 14-35Hz",
                "Gamma 30-45Hz"
        ), 1);
        addDropdown("TerrainGain", "强度", Arrays.asList("0.6x", "1.0x", "1.4x", "2.0x"), 1);
    }

    public void TerrainBand(int n) {
        if (n == 0) {
            bandLo = 4;
            bandHi = 25;
        } else if (n == 1) {
            bandLo = 8;
            bandHi = 30;
        } else if (n == 2) {
            bandLo = 14;
            bandHi = 35;
        } else {
            bandLo = 30;
            bandHi = 45;
        }
    }

    public void TerrainGain(int n) {
        if (n == 0) {
            terrainGain = 0.6f;
        } else if (n == 1) {
            terrainGain = 1.0f;
        } else if (n == 2) {
            terrainGain = 1.4f;
        } else {
            terrainGain = 2.0f;
        }
    }

    public void update() {
        super.update();
        ensureEnergyBuffer();
        updateChannelEnergyFromFFT();
        updateTerrain();
    }

    public void draw() {
        super.draw();

        MAIN.pushStyle();
        drawGradientBackground(x, y, w, h);

        int pad = 10;
        int panelX = x + pad;
        int panelY = y + pad;
        int panelW = w - pad * 2;
        int panelH = h - pad * 2;

        MAIN.noStroke();
        MAIN.fill(PANEL);
        MAIN.rect(panelX, panelY, panelW, panelH, 9);
        MAIN.noFill();
        MAIN.stroke(PANEL_STROKE);
        MAIN.strokeWeight(1.2f);
        MAIN.rect(panelX, panelY, panelW, panelH, 9);

        int titleH = 50;
        int bodyY = panelY + titleH;
        int bodyH = panelH - titleH - 10;
        int mainW = (int) (panelW * 0.72f);
        int sideW = panelW - mainW - 12;

        int terrainX = panelX + 8;
        int terrainY = bodyY;
        int terrainW = mainW - 8;
        int terrainH = bodyH;

        int sideX = panelX + mainW + 8;
        int sideY = bodyY;
        int sideH = bodyH;

        drawMainCard(terrainX, terrainY, terrainW, terrainH);
        int meshX = terrainX + 10;
        int meshY = terrainY + 34;
        int meshW = terrainW - 20;
        int meshH = terrainH - 124;
        drawTerrainMesh(meshX, meshY, meshW, meshH);
        drawMainFooter(terrainX + 10, terrainY + terrainH - 82, terrainW - 20, 62);

        drawSideMetrics(sideX, sideY, sideW, sideH);
        drawTitle(panelX, panelY, panelW);

        MAIN.popStyle();
    }

    private void drawTitle(int panelX, int panelY, int panelW) {
        MAIN.fill(TEXT_MAIN);
        MAIN.textFont(p7, 14);
        MAIN.textAlign(MAIN.LEFT, MAIN.TOP);
        MAIN.text("脑区能量地形图", panelX + 12, panelY + 10);

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7, 12);
        MAIN.text(getBandLabel() + "  |  " + bandLo + "-" + bandHi + "Hz", panelX + 12, panelY + 29);

        boolean streaming = isStreamingActive();
        float pulse = 0.5f + 0.5f * PApplet.sin(MAIN.millis() * 0.004f);
        int statusColor = streaming
                ? lerpColorARGB(0xFF2C5474, 0xFF5CD9FF, pulse)
                : lerpColorARGB(0xFF5E536A, 0xFFB8A7C9, pulse);
        MAIN.fill(statusColor);
        MAIN.textAlign(MAIN.RIGHT, MAIN.TOP);
        MAIN.text(streaming ? "实时更新" : "等待数据", panelX + panelW - 12, panelY + 12);
    }

    private void drawMainCard(int cx, int cy, int cw, int ch) {
        MAIN.noStroke();
        MAIN.fill(CARD);
        MAIN.rect(cx, cy, cw, ch, 8);
        MAIN.noFill();
        MAIN.stroke(CARD_STROKE);
        MAIN.strokeWeight(1f);
        MAIN.rect(cx, cy, cw, ch, 8);

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7, 12);
        MAIN.textAlign(MAIN.LEFT, MAIN.TOP);
        MAIN.text("脑区能量透视", cx + 12, cy + 8);

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7, 11);
        MAIN.text("山脊越高、等值线越密，表示该频段能量越强", cx + 118, cy + 9);

        MAIN.stroke(0x339EC4EF);
        MAIN.line(cx + 12, cy + 26, cx + cw - 12, cy + 26);
    }

    private void drawMainFooter(int fx, int fy, int fw, int fh) {
        MAIN.noStroke();
        MAIN.fill(0x4C173050);
        MAIN.rect(fx, fy, fw, fh, 8);
        MAIN.noFill();
        MAIN.stroke(0x4A7BB4E8);
        MAIN.strokeWeight(1f);
        MAIN.rect(fx, fy, fw, fh, 8);

        MAIN.fill(TEXT_MAIN);
        MAIN.textFont(p7, 11);
        MAIN.textAlign(MAIN.LEFT, MAIN.TOP);
        MAIN.text("读图说明", fx + 10, fy + 9);

        float gx = fx + 10;
        float gy = fy + 34;
        float gw = 112;
        float gh = 10;
        for (int i = 0; i < (int) gw; i++) {
            float t = gw <= 1 ? 0f : (float) i / (gw - 1f);
            MAIN.stroke(lerpColorARGB(CONTOUR_LOW, CONTOUR_HIGH, t));
            MAIN.line(gx + i, gy, gx + i, gy + gh);
        }
        MAIN.noFill();
        MAIN.stroke(0x6686ACD3);
        MAIN.rect(gx, gy, gw, gh, 3);

        MAIN.fill(TEXT_SUB);
        MAIN.text("低能量", gx, gy + 14);
        MAIN.text("高能量", gx + gw - 34, gy + 14);

        float sx = fx + 160;
        float sy = fy + 18;
        MAIN.noFill();
        MAIN.stroke(TEXT_WARM);
        MAIN.strokeWeight(1.4f);
        MAIN.ellipse(sx, sy + 10, 18, 18);
        MAIN.ellipse(sx, sy + 10, 8, 8);
        MAIN.fill(TEXT_SUB);
        MAIN.text("峰值标记", sx - 15, fy + 9);

        MAIN.noStroke();
        MAIN.fill(TEXT_SUB);
        MAIN.text("左侧用于文字解读，主图用于观察能量起伏", fx + fw - 236, fy + 9);
        MAIN.text("区域标签为近似脑区投影", fx + fw - 236, fy + 26);
    }

    private void drawSideMetrics(int x0, int y0, int w0, int h0) {
        MAIN.noStroke();
        MAIN.fill(CARD);
        MAIN.rect(x0, y0, w0, h0, 8);
        MAIN.noFill();
        MAIN.stroke(CARD_STROKE);
        MAIN.strokeWeight(1f);
        MAIN.rect(x0, y0, w0, h0, 8);

        int maxIdx = findPeakChannelIndex();
        float max = maxIdx >= 0 ? channelEnergy[maxIdx] : 0f;

        float mean = 0f;
        for (int i = 0; i < nchan; i++) {
            mean += channelEnergy[i];
        }
        mean /= Math.max(1, nchan);

        float var = 0f;
        for (int i = 0; i < nchan; i++) {
            float d = channelEnergy[i] - mean;
            var += d * d;
        }
        float std = PApplet.sqrt(var / Math.max(1, nchan));

        MAIN.fill(TEXT_MAIN);
        MAIN.textFont(p7, 12);
        MAIN.textAlign(MAIN.LEFT, MAIN.TOP);
        MAIN.text("实时解读", x0 + 10, y0 + 10);

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7, 11);
        MAIN.text(getBandDescription(), x0 + 10, y0 + 30, w0 - 20, 32);

        int cursorY = y0 + 70;
        drawMetricRow(x0 + 10, cursorY, w0 - 20, "热点通道", maxIdx >= 0 ? getChannelLabel(maxIdx) : "--", TEXT_ACCENT);
        cursorY += 36;
        drawMetricRow(x0 + 10, cursorY, w0 - 20, "活跃脑区", maxIdx >= 0 ? getDominantRegionLabel(maxIdx) : "未知", TEXT_MAIN);
        cursorY += 36;
        drawMetricRow(x0 + 10, cursorY, w0 - 20, "平均能量", MAIN.nf(mean, 0, 3), TEXT_MAIN);
        cursorY += 36;
        drawMetricRow(x0 + 10, cursorY, w0 - 20, "空间起伏", MAIN.nf(std, 0, 3), TEXT_MAIN);
        cursorY += 42;

        drawMetricGauge(x0 + 10, cursorY, w0 - 20, "整体活跃度", mean / 1.2f, 0xFF53C7FF, 0xFF1E88FF);
        cursorY += 34;
        drawMetricGauge(x0 + 10, cursorY, w0 - 20, "峰值对比", max / 1.2f, 0xFFFFD07E, 0xFFED6C3E);
        cursorY += 40;

        int listH = 84;
        drawTopChannelsList(x0 + 10, cursorY, w0 - 20, listH);
        cursorY += listH + 10;

        int barH = Math.max(44, h0 - (cursorY - y0) - 16);
        drawEnergyBars(x0 + 10, cursorY + 16, w0 - 20, barH, maxIdx);
    }

    private void drawMetricRow(int x0, int y0, int w0, String label, String value, int valueColor) {
        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7, 11);
        MAIN.textAlign(MAIN.LEFT, MAIN.TOP);
        MAIN.text(label, x0, y0);

        MAIN.fill(valueColor);
        MAIN.textFont(p7, 12);
        MAIN.textAlign(MAIN.RIGHT, MAIN.TOP);
        MAIN.text(value, x0 + w0, y0 - 1);
    }

    private void drawMetricGauge(int x0, int y0, int w0, String label, float value, int c0, int c1) {
        value = PApplet.constrain(value, 0f, 1f);
        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7, 11);
        MAIN.textAlign(MAIN.LEFT, MAIN.TOP);
        MAIN.text(label, x0, y0);

        float trackY = y0 + 16;
        float trackH = 8f;
        MAIN.noStroke();
        MAIN.fill(0x33436789);
        MAIN.rect(x0, trackY, w0, trackH, 4);

        float fillW = w0 * value;
        for (int i = 0; i < (int) fillW; i++) {
            float t = fillW <= 1f ? 0f : i / (fillW - 1f);
            MAIN.stroke(lerpColorARGB(c0, c1, t));
            MAIN.line(x0 + i, trackY, x0 + i, trackY + trackH);
        }

        MAIN.fill(TEXT_MAIN);
        MAIN.textAlign(MAIN.RIGHT, MAIN.TOP);
        MAIN.text(MAIN.nf(value * 100f, 0, 0) + "%", x0 + w0, y0);
    }

    private void drawTopChannelsList(int x0, int y0, int w0, int h0) {
        MAIN.noStroke();
        MAIN.fill(0x2F1A3254);
        MAIN.rect(x0, y0, w0, h0, 7);
        MAIN.noFill();
        MAIN.stroke(0x4A7AAADB);
        MAIN.rect(x0, y0, w0, h0, 7);

        MAIN.fill(TEXT_MAIN);
        MAIN.textFont(p7, 11);
        MAIN.textAlign(MAIN.LEFT, MAIN.TOP);
        MAIN.text("热点排名", x0 + 8, y0 + 8);

        int[] top = findTopChannels(3);
        for (int i = 0; i < top.length; i++) {
            int idx = top[i];
            int rowY = y0 + 28 + i * 18;
            MAIN.fill(i == 0 ? TEXT_WARM : TEXT_SUB);
            MAIN.text((i + 1) + ".", x0 + 8, rowY);
            MAIN.text(getChannelLabel(idx), x0 + 26, rowY);
            MAIN.textAlign(MAIN.RIGHT, MAIN.TOP);
            MAIN.text(MAIN.nf(channelEnergy[idx], 0, 3), x0 + w0 - 8, rowY);
            MAIN.textAlign(MAIN.LEFT, MAIN.TOP);
        }
    }

    private void drawEnergyBars(int bx, int by, int bw, int bh, int peakIndex) {
        if (bh < 36) {
            return;
        }

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7, 11);
        MAIN.textAlign(MAIN.LEFT, MAIN.TOP);
        MAIN.text("通道能量分布", bx, by - 16);

        int count = Math.max(1, nchan);
        float gap = 4f;
        float barW = (bw - gap * (count - 1)) / count;
        barW = PApplet.max(3f, barW);

        for (int i = 0; i < count; i++) {
            float v = channelEnergy[i];
            float hNorm = PApplet.constrain(v / 1.2f, 0f, 1f);
            float barH = hNorm * (bh - 18);
            float px = bx + i * (barW + gap);
            float py = by + (bh - barH);

            MAIN.noStroke();
            MAIN.fill(0x33436789);
            MAIN.rect(px, by + 4, barW, bh - 4, 2);

            int c0 = i == peakIndex ? 0xFFFFB86A : 0xFF57C7FF;
            int c1 = i == peakIndex ? 0xFFED6C3E : 0xFF2B9DFF;
            for (int yy = 0; yy < (int) barH; yy++) {
                float t = barH <= 1f ? 0f : yy / (barH - 1f);
                MAIN.stroke(lerpColorARGB(c0, c1, t));
                MAIN.line(px, py + yy, px + barW, py + yy);
            }

            MAIN.noStroke();
            MAIN.fill(TEXT_SUB);
            MAIN.textFont(p7, 9);
            MAIN.textAlign(MAIN.CENTER, MAIN.TOP);
            MAIN.text(i + 1, px + barW * 0.5f, by + bh + 2);
        }
    }

    private void drawTerrainMesh(int rx, int ry, int rw, int rh) {
        float horizonY = ry + rh * 0.18f;
        float nearY = ry + rh * 0.9f;
        float leftNear = rx + rw * 0.08f;
        float rightNear = rx + rw * 0.92f;
        float leftFar = rx + rw * 0.3f;
        float rightFar = rx + rw * 0.7f;
        float amp = rh * 0.28f;

        float maxTerrain = -999f;
        int maxR = 0;
        int maxC = 0;

        for (int r = 0; r < GRID_ROWS; r++) {
            float zr = (float) r / (GRID_ROWS - 1);
            float baseY = PApplet.lerp(horizonY, nearY, zr);
            float leftX = PApplet.lerp(leftFar, leftNear, zr);
            float rightX = PApplet.lerp(rightFar, rightNear, zr);

            int rowColor = lerpColorARGB(GRID_FAR, GRID_NEAR, zr);
            MAIN.stroke(rowColor);
            MAIN.strokeWeight(PApplet.lerp(0.8f, 1.45f, zr));
            MAIN.noFill();
            MAIN.beginShape();
            for (int c = 0; c < GRID_COLS; c++) {
                float xc = (float) c / (GRID_COLS - 1);
                float px = PApplet.lerp(leftX, rightX, xc);
                float tv = terrain[r][c];
                float py = baseY - tv * amp;
                MAIN.vertex(px, py);

                if (tv > maxTerrain) {
                    maxTerrain = tv;
                    maxR = r;
                    maxC = c;
                }
            }
            MAIN.endShape();
        }

        for (int c = 0; c < GRID_COLS; c += 2) {
            float xc = (float) c / (GRID_COLS - 1);
            MAIN.noFill();
            MAIN.beginShape();
            for (int r = 0; r < GRID_ROWS; r++) {
                float zr = (float) r / (GRID_ROWS - 1);
                float baseY = PApplet.lerp(horizonY, nearY, zr);
                float leftX = PApplet.lerp(leftFar, leftNear, zr);
                float rightX = PApplet.lerp(rightFar, rightNear, zr);
                float px = PApplet.lerp(leftX, rightX, xc);
                float py = baseY - terrain[r][c] * amp;
                MAIN.stroke(lerpColorARGB(0x2D5E84B0, GRID_LINE, zr));
                MAIN.strokeWeight(PApplet.lerp(0.55f, 0.95f, zr));
                MAIN.vertex(px, py);
            }
            MAIN.endShape();
        }

        drawContourLevels(horizonY, nearY, leftNear, rightNear, leftFar, rightFar, amp);

        boolean streaming = isStreamingActive();
        if (streaming) {
            peakMarkerR = maxR;
            peakMarkerC = maxC;
            peakMarkerValue = maxTerrain;
        }

        float peakZ = (float) peakMarkerR / (GRID_ROWS - 1);
        float peakXNorm = (float) peakMarkerC / (GRID_COLS - 1);
        float peakX = projectX(peakZ, peakXNorm, leftNear, rightNear, leftFar, rightFar);
        float peakY = projectY(peakZ, peakXNorm, horizonY, nearY, amp);
        float pulse = 0.5f + 0.5f * PApplet.sin(MAIN.millis() * 0.0065f);
        float outerR = 9f + pulse * 5f;

        MAIN.noFill();
        MAIN.stroke(lerpColorARGB(0x66FFDFA0, 0xE6FFF29A, pulse));
        MAIN.strokeWeight(1.5f);
        MAIN.ellipse(peakX, peakY, outerR * 2f, outerR * 2f);
        MAIN.stroke(0xE6FFE486);
        MAIN.strokeWeight(1.0f);
        MAIN.ellipse(peakX, peakY, 8f, 8f);

        MAIN.fill(0xFFFFF0B5);
        MAIN.textAlign(MAIN.LEFT, MAIN.CENTER);
        MAIN.textFont(p7, 11);
        MAIN.text("峰值 " + MAIN.nf(peakMarkerValue, 0, 3), peakX + 10f, peakY - 2f);

        float glow = 0.5f + 0.5f * PApplet.sin(MAIN.millis() * 0.0035f);
        MAIN.stroke(lerpColorARGB(0x443D89CC, 0xCC6EE7FF, glow));
        MAIN.strokeWeight(1.6f);
        MAIN.line(leftFar, horizonY, rightFar, horizonY);

        drawTerrainAnnotations(rx, ry, rw, rh, horizonY, nearY, peakX, peakY);
        drawBrainRegionGuide(rx, ry, rw, rh, horizonY, nearY, leftNear, rightNear, leftFar, rightFar);
    }

    private void drawTerrainAnnotations(int rx, int ry, int rw, int rh, float horizonY, float nearY, float peakX, float peakY) {
        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7, 11);
        MAIN.textAlign(MAIN.LEFT, MAIN.TOP);
        MAIN.text("等值线", rx + 8, ry + 6);

        MAIN.textAlign(MAIN.RIGHT, MAIN.TOP);
        MAIN.text("能量起伏", rx + rw - 8, ry + 6);

        MAIN.textAlign(MAIN.LEFT, MAIN.CENTER);
        MAIN.text("近场", rx + 8, nearY - 4);
        MAIN.textAlign(MAIN.RIGHT, MAIN.CENTER);
        MAIN.text("远场", rx + rw - 8, horizonY + 4);

        MAIN.fill(TEXT_WARM);
        MAIN.textAlign(MAIN.LEFT, MAIN.BOTTOM);
        MAIN.text("高能热点", peakX + 10, peakY - 10);
    }

    private void drawBrainRegionGuide(int rx, int ry, int rw, int rh,
                                      float horizonY, float nearY,
                                      float leftNear, float rightNear,
                                      float leftFar, float rightFar) {
        float frontalY = horizonY + rh * 0.06f;
        float centralY = PApplet.lerp(horizonY, nearY, 0.42f);
        float parietalY = PApplet.lerp(horizonY, nearY, 0.66f);
        float occipitalY = nearY + rh * 0.035f;
        float centerX = rx + rw * 0.5f;

        drawRegionChip(centerX, frontalY, "前额区", MAIN.CENTER);
        drawRegionChip(centerX, centralY, "中央区", MAIN.CENTER);
        drawRegionChip(centerX, parietalY, "顶叶区", MAIN.CENTER);
        drawRegionChip(centerX, occipitalY, "枕区", MAIN.CENTER);

        float midY = PApplet.lerp(horizonY, nearY, 0.5f);
        float leftX = PApplet.lerp(leftFar, leftNear, 0.5f) - 18f;
        float rightX = PApplet.lerp(rightFar, rightNear, 0.5f) + 18f;
        drawRegionChip(leftX, midY, "左脑", MAIN.RIGHT);
        drawRegionChip(rightX, midY, "右脑", MAIN.LEFT);

        MAIN.pushStyle();
        MAIN.stroke(0x2E8AB8E5);
        MAIN.strokeWeight(1f);
        MAIN.line(centerX - 34, frontalY + 12, centerX + 34, frontalY + 12);
        MAIN.line(centerX - 28, centralY + 12, centerX + 28, centralY + 12);
        MAIN.line(centerX - 24, parietalY + 12, centerX + 24, parietalY + 12);
        MAIN.line(centerX - 18, occipitalY - 5, centerX + 18, occipitalY - 5);
        MAIN.popStyle();
    }

    private void drawRegionChip(float anchorX, float anchorY, String label, int align) {
        float chipW = label.length() * 13f + 20f;
        float chipH = 20f;
        float x0;
        if (align == MAIN.RIGHT) {
            x0 = anchorX - chipW;
        } else if (align == MAIN.CENTER) {
            x0 = anchorX - chipW * 0.5f;
        } else {
            x0 = anchorX;
        }
        float y0 = anchorY - chipH * 0.5f;

        MAIN.pushStyle();
        MAIN.noStroke();
        MAIN.fill(0x8A143053);
        MAIN.rect(x0, y0, chipW, chipH, 10);
        MAIN.stroke(0x6695C8F5);
        MAIN.strokeWeight(1f);
        MAIN.noFill();
        MAIN.rect(x0, y0, chipW, chipH, 10);
        MAIN.fill(TEXT_MAIN);
        MAIN.textFont(p7, 10);
        MAIN.textAlign(MAIN.CENTER, MAIN.CENTER);
        MAIN.text(label, x0 + chipW * 0.5f, y0 + chipH * 0.5f - 1f);
        MAIN.popStyle();
    }

    private void drawContourLevels(float horizonY, float nearY, float leftNear, float rightNear, float leftFar, float rightFar, float amp) {
        float[] levels = {0.28f, 0.46f, 0.68f, 0.92f};
        for (int i = 0; i < levels.length; i++) {
            float level = levels[i];
            float t = (float) i / (levels.length - 1);
            int contourColor = lerpColorARGB(CONTOUR_LOW, CONTOUR_HIGH, t);
            MAIN.stroke(contourColor);
            MAIN.strokeWeight(1.05f + t * 0.35f);
            drawContourLevel(level, horizonY, nearY, leftNear, rightNear, leftFar, rightFar, amp);
        }
    }

    private void drawContourLevel(float level, float horizonY, float nearY, float leftNear, float rightNear, float leftFar, float rightFar, float amp) {
        for (int r = 0; r < GRID_ROWS - 1; r++) {
            for (int c = 0; c < GRID_COLS - 1; c++) {
                float v00 = terrain[r][c];
                float v10 = terrain[r][c + 1];
                float v01 = terrain[r + 1][c];
                float v11 = terrain[r + 1][c + 1];

                float[][] pts = new float[4][2];
                int count = 0;

                count = collectEdgePoint(pts, count, level, r, c, r, c + 1, v00, v10, horizonY, nearY, leftNear, rightNear, leftFar, rightFar, amp);
                count = collectEdgePoint(pts, count, level, r, c + 1, r + 1, c + 1, v10, v11, horizonY, nearY, leftNear, rightNear, leftFar, rightFar, amp);
                count = collectEdgePoint(pts, count, level, r + 1, c + 1, r + 1, c, v11, v01, horizonY, nearY, leftNear, rightNear, leftFar, rightFar, amp);
                count = collectEdgePoint(pts, count, level, r + 1, c, r, c, v01, v00, horizonY, nearY, leftNear, rightNear, leftFar, rightFar, amp);

                if (count == 2) {
                    MAIN.line(pts[0][0], pts[0][1], pts[1][0], pts[1][1]);
                } else if (count == 4) {
                    MAIN.line(pts[0][0], pts[0][1], pts[1][0], pts[1][1]);
                    MAIN.line(pts[2][0], pts[2][1], pts[3][0], pts[3][1]);
                }
            }
        }
    }

    private int collectEdgePoint(float[][] pts, int count, float level,
                                 int r0, int c0, int r1, int c1, float v0, float v1,
                                 float horizonY, float nearY, float leftNear, float rightNear, float leftFar, float rightFar, float amp) {
        if (count >= pts.length) {
            return count;
        }
        if (!crossesLevel(level, v0, v1)) {
            return count;
        }

        float denom = (v1 - v0);
        float t = PApplet.abs(denom) < 1e-6f ? 0.5f : (level - v0) / denom;
        t = PApplet.constrain(t, 0f, 1f);

        float rr = PApplet.lerp((float) r0, (float) r1, t);
        float cc = PApplet.lerp((float) c0, (float) c1, t);
        float zNorm = rr / (GRID_ROWS - 1);
        float xNorm = cc / (GRID_COLS - 1);

        pts[count][0] = projectX(zNorm, xNorm, leftNear, rightNear, leftFar, rightFar);
        pts[count][1] = projectY(zNorm, xNorm, horizonY, nearY, amp);
        return count + 1;
    }

    private boolean crossesLevel(float level, float a, float b) {
        return (a <= level && b >= level) || (a >= level && b <= level);
    }

    private float projectX(float zNorm, float xNorm, float leftNear, float rightNear, float leftFar, float rightFar) {
        float leftX = PApplet.lerp(leftFar, leftNear, zNorm);
        float rightX = PApplet.lerp(rightFar, rightNear, zNorm);
        return PApplet.lerp(leftX, rightX, xNorm);
    }

    private float projectY(float zNorm, float xNorm, float horizonY, float nearY, float amp) {
        float baseY = PApplet.lerp(horizonY, nearY, zNorm);
        float rr = zNorm * (GRID_ROWS - 1);
        float cc = xNorm * (GRID_COLS - 1);
        int r0 = PApplet.constrain(PApplet.floor(rr), 0, GRID_ROWS - 1);
        int c0 = PApplet.constrain(PApplet.floor(cc), 0, GRID_COLS - 1);
        int r1 = PApplet.min(GRID_ROWS - 1, r0 + 1);
        int c1 = PApplet.min(GRID_COLS - 1, c0 + 1);
        float tr = rr - r0;
        float tc = cc - c0;

        float v0 = PApplet.lerp(terrain[r0][c0], terrain[r0][c1], tc);
        float v1 = PApplet.lerp(terrain[r1][c0], terrain[r1][c1], tc);
        float v = PApplet.lerp(v0, v1, tr);

        return baseY - v * amp;
    }

    private void updateChannelEnergyFromFFT() {
        if (MAIN == null || MAIN.currentBoard == null) {
            Arrays.fill(channelEnergy, 0f);
            return;
        }

        int nfft = getNfftSafe(MAIN);
        float sr = MAIN.currentBoard.getSampleRate();
        int loBin = PApplet.max(1, PApplet.floor((bandLo * nfft) / sr));
        int hiBin = PApplet.max(loBin + 1, PApplet.ceil((bandHi * nfft) / sr));

        for (int ch = 0; ch < nchan; ch++) {
            float acc = 0f;
            int count = 0;
            for (int b = loBin; b <= hiBin; b++) {
                acc += fftBuff[ch].getBand(b);
                count++;
            }
            float avg = count > 0 ? acc / count : 0f;
            channelEnergy[ch] = MAIN.constrain(avg * terrainGain * 0.06f, 0f, 1.2f);
        }
    }

    private void updateTerrain() {
        float motionScale = isStreamingActive() ? 1.0f : 0.12f;
        for (int r = 0; r < GRID_ROWS; r++) {
            float zr = (float) r / (GRID_ROWS - 1);
            float roll = (float) Math.sin((MAIN.millis() * 0.0016f) + r * 0.28f) * 0.08f * motionScale;
            for (int c = 0; c < GRID_COLS; c++) {
                float chPos = ((float) c / (GRID_COLS - 1)) * (Math.max(1, nchan) - 1);
                int ch0 = PApplet.floor(chPos);
                int ch1 = PApplet.min(nchan - 1, ch0 + 1);
                float t = chPos - ch0;
                float e = PApplet.lerp(channelEnergy[ch0], channelEnergy[ch1], t);
                float ridge = (float) Math.sin((c * 0.35f) + (MAIN.millis() * 0.0011f)) * 0.05f * motionScale;
                float target = MAIN.constrain(e * (0.65f + 0.55f * zr) + ridge + roll, 0f, 1.4f);
                terrain[r][c] += (target - terrain[r][c]) * smoothing;
            }
        }
    }

    private void drawGradientBackground(int gx, int gy, int gw, int gh) {
        MAIN.pushStyle();
        MAIN.noFill();
        for (int i = 0; i < gh; i++) {
            float t = gh <= 1 ? 0f : (float) i / (float) (gh - 1);
            MAIN.stroke(lerpColorARGB(BG_TOP, BG_BOTTOM, t));
            MAIN.line(gx, gy + i, gx + gw, gy + i);
        }
        MAIN.popStyle();
    }

    private int lerpColorARGB(int c1, int c2, float t) {
        int a1 = (c1 >>> 24) & 0xFF;
        int r1 = (c1 >>> 16) & 0xFF;
        int g1 = (c1 >>> 8) & 0xFF;
        int b1 = c1 & 0xFF;
        int a2 = (c2 >>> 24) & 0xFF;
        int r2 = (c2 >>> 16) & 0xFF;
        int g2 = (c2 >>> 8) & 0xFF;
        int b2 = c2 & 0xFF;
        int a = (int) PApplet.lerp(a1, a2, t);
        int r = (int) PApplet.lerp(r1, r2, t);
        int g = (int) PApplet.lerp(g1, g2, t);
        int b = (int) PApplet.lerp(b1, b2, t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int findPeakChannelIndex() {
        if (nchan <= 0 || channelEnergy.length == 0) {
            return -1;
        }
        int bestIndex = 0;
        float bestValue = channelEnergy[0];
        for (int i = 1; i < Math.min(nchan, channelEnergy.length); i++) {
            if (channelEnergy[i] > bestValue) {
                bestValue = channelEnergy[i];
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private int[] findTopChannels(int count) {
        int safeCount = Math.min(Math.max(1, count), Math.max(1, nchan));
        int[] indices = new int[safeCount];
        boolean[] used = new boolean[Math.max(1, nchan)];
        for (int k = 0; k < safeCount; k++) {
            int best = 0;
            float bestValue = -1f;
            for (int i = 0; i < nchan; i++) {
                if (!used[i] && channelEnergy[i] > bestValue) {
                    bestValue = channelEnergy[i];
                    best = i;
                }
            }
            indices[k] = best;
            used[best] = true;
        }
        return indices;
    }

    private String getBandLabel() {
        if (bandLo == 4 && bandHi == 25) {
            return "宽频段观察";
        }
        if (bandLo == 8 && bandHi == 30) {
            return "注意力频段";
        }
        if (bandLo == 14 && bandHi == 35) {
            return "运动激活频段";
        }
        return "高频激活";
    }

    private String getBandDescription() {
        if (bandLo == 4 && bandHi == 25) {
            return "用于观察从低频到中频的整体能量变化。";
        }
        if (bandLo == 8 && bandHi == 30) {
            return "更适合观察注意与觉醒度的能量起伏。";
        }
        if (bandLo == 14 && bandHi == 35) {
            return "更容易看到运动相关的活跃能量区域。";
        }
        return "用于观察偏高频能量的活跃热点。";
    }

    private String getDominantRegionLabel(int index) {
        return inferRegionFromChannelLabel(getChannelLabel(index));
    }

    private String inferRegionFromChannelLabel(String channelLabel) {
        if (channelLabel == null) {
            return "未知区域";
        }
        String upper = channelLabel.toUpperCase();
        if (upper.startsWith("FP") || upper.startsWith("AF") || upper.startsWith("F")) {
            return "前额区";
        }
        if (upper.startsWith("C")) {
            return "中央区";
        }
        if (upper.startsWith("P")) {
            return "顶叶区";
        }
        if (upper.startsWith("O")) {
            return "枕区";
        }
        if (upper.startsWith("T")) {
            return "颞侧区";
        }
        return "脑区投影";
    }

    private boolean isStreamingActive() {
        return MAIN != null && MAIN.currentBoard != null && MAIN.currentBoard.isStreaming();
    }

    private String getChannelLabel(int index) {
        if (MAIN != null && MAIN.currentBoard != null) {
            int[] exgChannels = MAIN.currentBoard.getEXGChannels();
            if (exgChannels != null && index >= 0 && index < exgChannels.length) {
                int boardChannelIndex = exgChannels[index];
                if (MAIN.currentBoard instanceof Board) {
                    String[] channelNames = ((Board) MAIN.currentBoard).getChannelNames();
                    if (channelNames != null && boardChannelIndex >= 0 && boardChannelIndex < channelNames.length) {
                        return channelNames[boardChannelIndex];
                    }
                }
                return "CH" + (index + 1);
            }
        }
        return "CH" + (index + 1);
    }

    private void ensureEnergyBuffer() {
        if (channelEnergy.length != Math.max(1, nchan)) {
            channelEnergy = new float[Math.max(1, nchan)];
        }
    }
}

