package W_Head_;

import Board_.Board;
import Widget_.Widget;
import processing.core.PApplet;

import java.util.Arrays;

import static Globel.GUI.*;
import static SystemManager.GF.getNfftSafe;

import Globel.GUI;

public class W_Head extends Widget {
    private static final String[] TARGET_LABELS = {
            "Fp1", "Fp2", "C3", "C4", "P3", "P4", "O1", "O2"
    };

    // Relative scalp positions inside the head circle.
    private static final float[][] TARGET_POSITIONS = {
            {-0.34f, -0.72f}, {0.34f, -0.72f},
            {-0.48f, -0.08f}, {0.48f, -0.08f},
            {-0.33f, 0.36f}, {0.33f, 0.36f},
            {-0.24f, 0.76f}, {0.24f, 0.76f}
    };

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
    private static final int GRID_LINE = 0x2E7AA6D6;
    private static final int HEAD_FILL = 0xFF0E1828;
    private static final int HEAD_STROKE = 0x88B5D7FF;
    private static final int LOW_COLOR = 0xFF183456;
    private static final int MID_COLOR = 0xFF2EA7D8;
    private static final int HIGH_COLOR = 0xFFFFD36E;
    private static final int PEAK_COLOR = 0xFFFF835A;

    GUI MAIN;
    protected PApplet pApplet;

    private final float[] displayEnergy = new float[TARGET_LABELS.length];
    private final float[] rawEnergy = new float[TARGET_LABELS.length];
    private final boolean[] channelPresent = new boolean[TARGET_LABELS.length];
    private final int[] boardChannelByTarget = new int[TARGET_LABELS.length];

    private int bandLo = 8;
    private int bandHi = 30;
    private float energyGain = 1.0f;
    private float smoothing = 0.18f;

    public W_Head(GUI MAIN) {
        super(MAIN);
        this.MAIN = MAIN;
        this.pApplet = MAIN;

        Arrays.fill(boardChannelByTarget, -1);

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
            energyGain = 0.6f;
        } else if (n == 1) {
            energyGain = 1.0f;
        } else if (n == 2) {
            energyGain = 1.4f;
        } else {
            energyGain = 2.0f;
        }
    }

    public void update() {
        super.update();
        resolveDisplayChannels();
        updateBandEnergy();
        smoothDisplayEnergy();
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
        int mainW = (int) (panelW * 0.7f);
        int sideW = panelW - mainW - 12;

        int plotX = panelX + 8;
        int plotY = bodyY;
        int plotW = mainW - 8;
        int plotH = bodyH;

        int sideX = panelX + mainW + 8;
        int sideY = bodyY;

        drawTitle(panelX, panelY, panelW);
        drawMainCard(plotX, plotY, plotW, plotH);
        drawScalpMap(plotX + 12, plotY + 34, plotW - 24, plotH - 118);
        drawMainFooter(plotX + 10, plotY + plotH - 76, plotW - 20, 58);

        drawSideMetrics(sideX, sideY, sideW, bodyH);

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

    private void drawMainCard(int x0, int y0, int w0, int h0) {
        MAIN.noStroke();
        MAIN.fill(CARD);
        MAIN.rect(x0, y0, w0, h0, 8);
        MAIN.noFill();
        MAIN.stroke(CARD_STROKE);
        MAIN.strokeWeight(1f);
        MAIN.rect(x0, y0, w0, h0, 8);

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7, 12);
        MAIN.textAlign(MAIN.LEFT, MAIN.TOP);
        MAIN.text("头皮热力投影", x0 + 12, y0 + 8);
        MAIN.textFont(p7, 11);
        MAIN.text("八通道按真实头皮相对位置插值，颜色越暖表示该频段能量越强", x0 + 108, y0 + 9);

        MAIN.stroke(0x339EC4EF);
        MAIN.line(x0 + 12, y0 + 26, x0 + w0 - 12, y0 + 26);
    }

    private void drawScalpMap(int x0, int y0, int w0, int h0) {
        int radius = (int) (Math.min(w0, h0) * 0.37f);
        int cx = x0 + w0 / 2;
        int cy = y0 + h0 / 2 + 6;

        drawHeatField(cx, cy, radius);
        drawHeadOutline(cx, cy, radius);
        drawRegionLabels(cx, cy, radius);
        drawElectrodes(cx, cy, radius);
        drawPeakMarker(cx, cy, radius);
    }

    private void drawHeatField(int cx, int cy, int radius) {
        float maxEnergy = getDisplayMax();
        int step = Math.max(3, radius / 22);

        MAIN.pushStyle();
        MAIN.noStroke();
        for (int py = cy - radius; py <= cy + radius; py += step) {
            for (int px = cx - radius; px <= cx + radius; px += step) {
                float dx = (px - cx) / (float) radius;
                float dy = (py - cy) / (float) radius;
                float rr = dx * dx + dy * dy;
                if (rr > 1.0f) {
                    continue;
                }

                float value = interpolateField(dx, dy);
                float edgeFade = PApplet.constrain(1.0f - rr, 0f, 1f);
                int fillColor = colorForEnergy(value, maxEnergy);
                MAIN.fill(fillColor, 38 + 122 * edgeFade);
                MAIN.rect(px, py, step + 1, step + 1);
            }
        }
        MAIN.popStyle();
    }

    private void drawHeadOutline(int cx, int cy, int radius) {
        int earW = (int) (radius * 0.22f);
        int earH = (int) (radius * 0.42f);
        int noseY = cy - radius - 8;

        MAIN.pushStyle();
        MAIN.noStroke();
        MAIN.fill(HEAD_FILL, 172);
        MAIN.ellipse(cx, cy, radius * 2f, radius * 2f);

        MAIN.fill(HEAD_FILL, 125);
        MAIN.ellipse(cx - radius, cy, earW, earH);
        MAIN.ellipse(cx + radius, cy, earW, earH);

        MAIN.stroke(HEAD_STROKE);
        MAIN.strokeWeight(1.6f);
        MAIN.noFill();
        MAIN.ellipse(cx, cy, radius * 2f, radius * 2f);
        MAIN.ellipse(cx - radius, cy, earW, earH);
        MAIN.ellipse(cx + radius, cy, earW, earH);
        MAIN.line(cx - 14, cy - radius + 10, cx, noseY);
        MAIN.line(cx + 14, cy - radius + 10, cx, noseY);
        MAIN.popStyle();
    }

    private void drawRegionLabels(int cx, int cy, int radius) {
        drawRegionChip(cx, cy - radius - 30, "前额区");
        drawRegionChip(cx, cy - 12, "中央区");
        drawRegionChip(cx, cy + radius * 0.34f, "顶叶区");
        drawRegionChip(cx, cy + radius + 18, "枕区");
        drawRegionChip(cx - radius - 30, cy, "左脑");
        drawRegionChip(cx + radius + 30, cy, "右脑");
    }

    private void drawRegionChip(float anchorX, float anchorY, String label) {
        float chipW = label.length() * 12f + 20f;
        float chipH = 20f;
        float x0 = anchorX - chipW * 0.5f;
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

    private void drawElectrodes(int cx, int cy, int radius) {
        float maxEnergy = getDisplayMax();
        for (int i = 0; i < TARGET_LABELS.length; i++) {
            float px = cx + TARGET_POSITIONS[i][0] * radius;
            float py = cy + TARGET_POSITIONS[i][1] * radius;
            int dotColor = colorForEnergy(displayEnergy[i], maxEnergy);

            MAIN.pushStyle();
            MAIN.noStroke();
            MAIN.fill(8, 16, 28, 180);
            MAIN.ellipse(px, py, 18, 18);
            MAIN.fill(dotColor);
            MAIN.ellipse(px, py, 12, 12);
            MAIN.stroke(255, 235);
            MAIN.strokeWeight(1.2f);
            MAIN.noFill();
            MAIN.ellipse(px, py, 18, 18);

            MAIN.fill(TEXT_MAIN);
            MAIN.textFont(p7, 10);
            MAIN.textAlign(MAIN.CENTER, MAIN.TOP);
            MAIN.text(TARGET_LABELS[i], px, py + 11);
            MAIN.popStyle();
        }
    }

    private void drawPeakMarker(int cx, int cy, int radius) {
        int peakIndex = findPeakChannelIndex();
        if (peakIndex < 0 || peakIndex >= TARGET_LABELS.length) {
            return;
        }

        float px = cx + TARGET_POSITIONS[peakIndex][0] * radius;
        float py = cy + TARGET_POSITIONS[peakIndex][1] * radius;
        float pulse = 0.5f + 0.5f * PApplet.sin(MAIN.millis() * 0.0065f);
        float outerR = 12f + pulse * 5f;

        MAIN.pushStyle();
        MAIN.noFill();
        MAIN.stroke(255, 238, 148, 140);
        MAIN.strokeWeight(1.5f);
        MAIN.ellipse(px, py, outerR * 2f, outerR * 2f);
        MAIN.stroke(255, 131, 90, 220);
        MAIN.ellipse(px, py, 18, 18);
        MAIN.fill(TEXT_WARM);
        MAIN.textFont(p7, 11);
        MAIN.textAlign(MAIN.LEFT, MAIN.BOTTOM);
        MAIN.text("峰值 " + TARGET_LABELS[peakIndex], px + 12, py - 8);
        MAIN.popStyle();
    }

    private void drawMainFooter(int x0, int y0, int w0, int h0) {
        MAIN.noStroke();
        MAIN.fill(0x4C173050);
        MAIN.rect(x0, y0, w0, h0, 8);
        MAIN.noFill();
        MAIN.stroke(0x4A7BB4E8);
        MAIN.strokeWeight(1f);
        MAIN.rect(x0, y0, w0, h0, 8);

        MAIN.fill(TEXT_MAIN);
        MAIN.textFont(p7, 11);
        MAIN.textAlign(MAIN.LEFT, MAIN.TOP);
        MAIN.text("读图说明", x0 + 10, y0 + 9);

        float gx = x0 + 10;
        float gy = y0 + 32;
        float gw = 130;
        for (int i = 0; i < (int) gw; i++) {
            float t = gw <= 1 ? 0f : (float) i / (gw - 1f);
            MAIN.stroke(colorForGradient(t));
            MAIN.line(gx + i, gy, gx + i, gy + 10);
        }
        MAIN.noFill();
        MAIN.stroke(0x6686ACD3);
        MAIN.rect(gx, gy, gw, 10, 3);

        MAIN.fill(TEXT_SUB);
        MAIN.text("低能量", gx, gy + 14);
        MAIN.text("高能量", gx + gw - 34, gy + 14);
        MAIN.text("八通道头皮位置插值，仅显示当前选定频段的相对能量强弱", x0 + 90, y0 + 12);
    }

    private void drawSideMetrics(int x0, int y0, int w0, int h0) {
        MAIN.noStroke();
        MAIN.fill(CARD);
        MAIN.rect(x0, y0, w0, h0, 8);
        MAIN.noFill();
        MAIN.stroke(CARD_STROKE);
        MAIN.strokeWeight(1f);
        MAIN.rect(x0, y0, w0, h0, 8);

        int peakIndex = findPeakChannelIndex();
        float max = peakIndex >= 0 ? displayEnergy[peakIndex] : 0f;
        float mean = average(displayEnergy);
        float leftEnergy = displayEnergy[0] + displayEnergy[2] + displayEnergy[4] + displayEnergy[6];
        float rightEnergy = displayEnergy[1] + displayEnergy[3] + displayEnergy[5] + displayEnergy[7];
        float frontEnergy = displayEnergy[0] + displayEnergy[1];
        float backEnergy = displayEnergy[6] + displayEnergy[7];

        MAIN.fill(TEXT_MAIN);
        MAIN.textFont(p7, 12);
        MAIN.textAlign(MAIN.LEFT, MAIN.TOP);
        MAIN.text("实时解读", x0 + 10, y0 + 10);

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7, 11);
        MAIN.text(getBandDescription(), x0 + 10, y0 + 30, w0 - 20, 32);

        int cursorY = y0 + 70;
        drawMetricRow(x0 + 10, cursorY, w0 - 20, "热点通道", peakIndex >= 0 ? TARGET_LABELS[peakIndex] : "--", TEXT_ACCENT);
        cursorY += 34;
        drawMetricRow(x0 + 10, cursorY, w0 - 20, "活跃脑区", peakIndex >= 0 ? inferRegionFromLabel(TARGET_LABELS[peakIndex]) : "未知", TEXT_MAIN);
        cursorY += 34;
        drawMetricRow(x0 + 10, cursorY, w0 - 20, "平均能量", MAIN.nf(mean, 0, 3), TEXT_MAIN);
        cursorY += 34;
        drawMetricRow(x0 + 10, cursorY, w0 - 20, "左右平衡", formatBias(leftEnergy, rightEnergy, "左", "右"), TEXT_MAIN);
        cursorY += 34;
        drawMetricRow(x0 + 10, cursorY, w0 - 20, "前后对比", formatBias(frontEnergy, backEnergy, "前", "后"), TEXT_MAIN);
        cursorY += 40;

        drawMetricGauge(x0 + 10, cursorY, w0 - 20, "整体活跃度", mean / 1.2f, 0xFF53C7FF, 0xFF1E88FF);
        cursorY += 34;
        drawMetricGauge(x0 + 10, cursorY, w0 - 20, "峰值强度", max / 1.2f, 0xFFFFD07E, 0xFFED6C3E);
        cursorY += 40;

        drawTopChannelsList(x0 + 10, cursorY, w0 - 20, 84);
        cursorY += 96;

        int barH = Math.max(52, h0 - (cursorY - y0) - 18);
        drawEnergyBars(x0 + 10, cursorY + 16, w0 - 20, barH, peakIndex);
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
        MAIN.noStroke();
        MAIN.fill(0x33436789);
        MAIN.rect(x0, trackY, w0, 8, 4);

        float fillW = w0 * value;
        for (int i = 0; i < (int) fillW; i++) {
            float t = fillW <= 1f ? 0f : i / (fillW - 1f);
            MAIN.stroke(lerpColorARGB(c0, c1, t));
            MAIN.line(x0 + i, trackY, x0 + i, trackY + 8);
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
            MAIN.text(TARGET_LABELS[idx], x0 + 26, rowY);
            MAIN.textAlign(MAIN.RIGHT, MAIN.TOP);
            MAIN.text(MAIN.nf(displayEnergy[idx], 0, 3), x0 + w0 - 8, rowY);
            MAIN.textAlign(MAIN.LEFT, MAIN.TOP);
        }
    }

    private void drawEnergyBars(int bx, int by, int bw, int bh, int peakIndex) {
        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7, 11);
        MAIN.textAlign(MAIN.LEFT, MAIN.TOP);
        MAIN.text("通道能量分布", bx, by - 16);

        int count = TARGET_LABELS.length;
        float gap = 4f;
        float barW = (bw - gap * (count - 1)) / count;
        barW = PApplet.max(3f, barW);

        for (int i = 0; i < count; i++) {
            float v = displayEnergy[i];
            float hNorm = PApplet.constrain(v / 1.2f, 0f, 1f);
            float barH = hNorm * (bh - 28);
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
            MAIN.text(TARGET_LABELS[i], px + barW * 0.5f, by + bh + 2);
        }
    }

    private void resolveDisplayChannels() {
        Arrays.fill(channelPresent, false);
        Arrays.fill(boardChannelByTarget, -1);

        if (MAIN == null || MAIN.currentBoard == null) {
            return;
        }

        int[] exgChannels = MAIN.currentBoard.getEXGChannels();
        if (exgChannels == null) {
            return;
        }
        String[] names = null;
        if (MAIN.currentBoard instanceof Board) {
            names = ((Board) MAIN.currentBoard).getChannelNames();
        }

        for (int i = 0; i < TARGET_LABELS.length; i++) {
            String wanted = TARGET_LABELS[i];
            if (names != null) {
                for (int j = 0; j < exgChannels.length; j++) {
                    int boardIndex = exgChannels[j];
                    if (boardIndex >= 0 && boardIndex < names.length && wanted.equalsIgnoreCase(names[boardIndex])) {
                        boardChannelByTarget[i] = j;
                        channelPresent[i] = true;
                        break;
                    }
                }
            }
        }

        // Fallback for boards whose labels are not renamed yet.
        for (int i = 0; i < TARGET_LABELS.length && i < exgChannels.length; i++) {
            if (boardChannelByTarget[i] < 0) {
                boardChannelByTarget[i] = i;
                channelPresent[i] = true;
            }
        }
    }

    private void updateBandEnergy() {
        Arrays.fill(rawEnergy, 0f);
        if (MAIN == null || MAIN.currentBoard == null) {
            return;
        }

        int nfft = getNfftSafe(MAIN);
        float sr = MAIN.currentBoard.getSampleRate();
        int loBin = PApplet.max(1, PApplet.floor((bandLo * nfft) / sr));
        int hiBin = PApplet.max(loBin + 1, PApplet.ceil((bandHi * nfft) / sr));

        for (int i = 0; i < TARGET_LABELS.length; i++) {
            if (!channelPresent[i] || boardChannelByTarget[i] < 0 || boardChannelByTarget[i] >= nchan) {
                continue;
            }
            float acc = 0f;
            int count = 0;
            int ch = boardChannelByTarget[i];
            for (int b = loBin; b <= hiBin; b++) {
                acc += fftBuff[ch].getBand(b);
                count++;
            }
            float avg = count > 0 ? acc / count : 0f;
            rawEnergy[i] = MAIN.constrain(avg * energyGain * 0.06f, 0f, 1.2f);
        }
    }

    private void smoothDisplayEnergy() {
        for (int i = 0; i < TARGET_LABELS.length; i++) {
            displayEnergy[i] += (rawEnergy[i] - displayEnergy[i]) * smoothing;
        }
    }

    private float interpolateField(float xNorm, float yNorm) {
        float sum = 0f;
        float weightSum = 0f;
        for (int i = 0; i < TARGET_LABELS.length; i++) {
            if (!channelPresent[i]) {
                continue;
            }
            float dx = xNorm - TARGET_POSITIONS[i][0];
            float dy = yNorm - TARGET_POSITIONS[i][1];
            float dist2 = dx * dx + dy * dy;
            float weight = 1.0f / PApplet.max(0.015f, dist2);
            weight *= 1.0f + displayEnergy[i] * 0.45f;
            sum += displayEnergy[i] * weight;
            weightSum += weight;
        }
        if (weightSum <= 0f) {
            return 0f;
        }
        return sum / weightSum;
    }

    private int colorForEnergy(float value, float maxEnergy) {
        float t = maxEnergy <= 1e-6f ? 0f : PApplet.constrain(value / maxEnergy, 0f, 1f);
        return colorForGradient(t);
    }

    private int colorForGradient(float t) {
        if (t < 0.5f) {
            return lerpColorARGB(LOW_COLOR, MID_COLOR, t / 0.5f);
        }
        return lerpColorARGB(MID_COLOR, HIGH_COLOR, (t - 0.5f) / 0.5f);
    }

    private float getDisplayMax() {
        float best = 0.0001f;
        for (float v : displayEnergy) {
            best = PApplet.max(best, v);
        }
        return best;
    }

    private int findPeakChannelIndex() {
        int bestIndex = -1;
        float best = -1f;
        for (int i = 0; i < TARGET_LABELS.length; i++) {
            if (displayEnergy[i] > best) {
                best = displayEnergy[i];
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private int[] findTopChannels(int count) {
        int[] out = new int[Math.min(count, TARGET_LABELS.length)];
        boolean[] used = new boolean[TARGET_LABELS.length];
        for (int k = 0; k < out.length; k++) {
            int bestIndex = 0;
            float bestValue = -1f;
            for (int i = 0; i < TARGET_LABELS.length; i++) {
                if (!used[i] && displayEnergy[i] > bestValue) {
                    bestValue = displayEnergy[i];
                    bestIndex = i;
                }
            }
            used[bestIndex] = true;
            out[k] = bestIndex;
        }
        return out;
    }

    private float average(float[] values) {
        float sum = 0f;
        for (float v : values) {
            sum += v;
        }
        return sum / Math.max(1, values.length);
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

    private String inferRegionFromLabel(String label) {
        if (label == null) {
            return "未知区域";
        }
        if (label.startsWith("Fp")) {
            return "前额区";
        }
        if (label.startsWith("C")) {
            return "中央区";
        }
        if (label.startsWith("P")) {
            return "顶叶区";
        }
        if (label.startsWith("O")) {
            return "枕区";
        }
        return "脑区投影";
    }

    private String formatBias(float a, float b, String labelA, String labelB) {
        float total = PApplet.max(0.0001f, a + b);
        float diff = (a - b) / total;
        if (PApplet.abs(diff) < 0.08f) {
            return "平衡";
        }
        return (diff > 0 ? labelA : labelB) + "偏强";
    }

    private boolean isStreamingActive() {
        return MAIN != null && MAIN.currentBoard != null && MAIN.currentBoard.isStreaming();
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
}
