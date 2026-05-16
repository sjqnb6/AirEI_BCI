package W_Head_;

import Widget_.Widget;
import processing.core.PApplet;

import java.util.Arrays;

import static Globel.GUI.*;
import static SystemManager.GF.getNfftSafe;

import Globel.GUI;

public class W_Head extends Widget {
    GUI MAIN;
    protected PApplet pApplet;

    // Industrial gray palette aligned with W_Prediction.
    private static final int COLOR_BG = 226;
    private static final int COLOR_PANEL = 233;
    private static final int COLOR_CARD = 239;
    private static final int COLOR_BORDER = 168;
    private static final int COLOR_BORDER_DARK = 128;
    private static final int COLOR_TEXT = 36;
    private static final int COLOR_GRID = 184;

    private static final int GRID_COLS = 28;
    private static final int GRID_ROWS = 18;

    private final float[][] terrain = new float[GRID_ROWS][GRID_COLS];
    private final float[] channelEnergy = new float[Math.max(1, nchan)];

    private int bandLo = 8;   // Hz
    private int bandHi = 30;  // Hz
    private float terrainGain = 1.0f;
    private float smoothing = 0.16f;

    public W_Head(GUI MAIN) {
        super(MAIN);
        this.MAIN = MAIN;
        this.pApplet = MAIN;

        addDropdown("TerrainBand", "频段", Arrays.asList("Theta/Beta", "Alpha/Beta", "Beta", "Gamma"), 1);
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
        updateChannelEnergyFromFFT();
        updateTerrain();
    }

    public void draw() {
        super.draw();

        MAIN.pushStyle();
        MAIN.noStroke();
        MAIN.fill(COLOR_BG);
        MAIN.rect(x, y - 1, w, h + 1);

        int pad = 10;
        int panelX = x + pad;
        int panelY = y + pad;
        int panelW = w - pad * 2;
        int panelH = h - pad * 2;

        MAIN.fill(COLOR_PANEL);
        MAIN.stroke(COLOR_BORDER);
        MAIN.strokeWeight(1.2f);
        MAIN.rect(panelX, panelY, panelW, panelH, 2);

        MAIN.noStroke();
        MAIN.fill(COLOR_CARD);
        MAIN.rect(panelX + 10, panelY + 10, panelW - 20, panelH - 20, 2);
        MAIN.stroke(COLOR_BORDER);
        MAIN.noFill();
        MAIN.rect(panelX + 10, panelY + 10, panelW - 20, panelH - 20, 2);

        drawTerrainMesh(panelX + 14, panelY + 16, panelW - 28, panelH - 32);

        MAIN.fill(COLOR_TEXT);
        MAIN.textFont(p7);
        MAIN.textSize(12);
        MAIN.textAlign(MAIN.LEFT, MAIN.TOP);
        MAIN.text("3D脑状态地形", panelX + 16, panelY + 14);
        MAIN.text("频段: " + bandLo + "-" + bandHi + "Hz", panelX + 16, panelY + 30);
        MAIN.popStyle();
    }

    private void drawTerrainMesh(int rx, int ry, int rw, int rh) {
        float horizonY = ry + rh * 0.18f;
        float nearY = ry + rh * 0.88f;
        float leftNear = rx + rw * 0.08f;
        float rightNear = rx + rw * 0.92f;
        float leftFar = rx + rw * 0.32f;
        float rightFar = rx + rw * 0.68f;
        float amp = rh * 0.26f;

        // Draw horizontal mesh lines from far to near.
        for (int r = 0; r < GRID_ROWS; r++) {
            float zr = (float) r / (GRID_ROWS - 1);
            float baseY = PApplet.lerp(horizonY, nearY, zr);
            float leftX = PApplet.lerp(leftFar, leftNear, zr);
            float rightX = PApplet.lerp(rightFar, rightNear, zr);

            int rowGray = (int) PApplet.lerp(COLOR_BORDER_DARK, COLOR_GRID, zr);
            MAIN.stroke(rowGray);
            MAIN.strokeWeight(1.0f);
            MAIN.noFill();
            MAIN.beginShape();
            for (int c = 0; c < GRID_COLS; c++) {
                float xc = (float) c / (GRID_COLS - 1);
                float px = PApplet.lerp(leftX, rightX, xc);
                float py = baseY - terrain[r][c] * amp;
                MAIN.vertex(px, py);
            }
            MAIN.endShape();
        }

        // Vertical mesh hints
        for (int c = 0; c < GRID_COLS; c += 2) {
            float xc = (float) c / (GRID_COLS - 1);
            MAIN.stroke(COLOR_BORDER);
            MAIN.strokeWeight(0.8f);
            MAIN.noFill();
            MAIN.beginShape();
            for (int r = 0; r < GRID_ROWS; r++) {
                float zr = (float) r / (GRID_ROWS - 1);
                float baseY = PApplet.lerp(horizonY, nearY, zr);
                float leftX = PApplet.lerp(leftFar, leftNear, zr);
                float rightX = PApplet.lerp(rightFar, rightNear, zr);
                float px = PApplet.lerp(leftX, rightX, xc);
                float py = baseY - terrain[r][c] * amp;
                MAIN.vertex(px, py);
            }
            MAIN.endShape();
        }
    }

    private void updateChannelEnergyFromFFT() {
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
        for (int r = 0; r < GRID_ROWS; r++) {
            float zr = (float) r / (GRID_ROWS - 1);
            float roll = (float) Math.sin((MAIN.millis() * 0.0016f) + r * 0.28f) * 0.08f;
            for (int c = 0; c < GRID_COLS; c++) {
                float chPos = ((float) c / (GRID_COLS - 1)) * (Math.max(1, nchan) - 1);
                int ch0 = PApplet.floor(chPos);
                int ch1 = PApplet.min(nchan - 1, ch0 + 1);
                float t = chPos - ch0;
                float e = PApplet.lerp(channelEnergy[ch0], channelEnergy[ch1], t);
                float ridge = (float) Math.sin((c * 0.35f) + (MAIN.millis() * 0.0011f)) * 0.05f;
                float target = MAIN.constrain(e * (0.65f + 0.55f * zr) + ridge + roll, 0f, 1.4f);
                terrain[r][c] += (target - terrain[r][c]) * smoothing;
            }
        }
    }
}
