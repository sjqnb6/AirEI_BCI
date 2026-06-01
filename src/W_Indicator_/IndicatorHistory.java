package W_Indicator_;

import processing.core.PApplet;

final class IndicatorHistory {
    private final int cols;
    private final int rows;
    private final int metrics;
    private final float[][][] data;
    private final float[] minArr;
    private final float[] maxArr;

    IndicatorHistory(int cols, int rows, int metrics) {
        this.cols = Math.max(8, cols);
        this.rows = Math.max(4, rows);
        this.metrics = Math.max(1, metrics);
        this.data = new float[this.metrics][this.rows][this.cols];
        this.minArr = new float[this.metrics];
        this.maxArr = new float[this.metrics];
        for (int m = 0; m < this.metrics; m++) {
            minArr[m] = 0f;
            maxArr[m] = 1f;
        }
    }

    void pushFrame(float[][] perChannel) {
        for (int m = 0; m < metrics; m++) {
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols - 1; c++) {
                    data[m][r][c] = data[m][r][c + 1];
                }
            }
        }

        for (int m = 0; m < metrics; m++) {
            for (int r = 0; r < rows; r++) {
                float src = sampleInterpolated(perChannel, r, m);
                float prev = data[m][r][cols - 2];
                // Keep temporal continuity while preserving sharper local peaks.
                data[m][r][cols - 1] = prev * 0.68f + src * 0.32f;
            }
        }
        recomputeMinMax();
    }

    private float sampleInterpolated(float[][] src, int row, int metric) {
        if (src == null || src.length == 0) {
            return 0f;
        }
        int srcRows = src.length;
        float pos = row * (srcRows - 1f) / Math.max(1f, rows - 1f);
        int i0 = (int) Math.floor(pos);
        int i1 = Math.min(srcRows - 1, i0 + 1);
        float t = pos - i0;
        float v0 = src[i0][metric];
        float v1 = src[i1][metric];
        return PApplet.lerp(v0, v1, t);
    }

    private void recomputeMinMax() {
        for (int m = 0; m < metrics; m++) {
            float mn = Float.POSITIVE_INFINITY;
            float mx = Float.NEGATIVE_INFINITY;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    float v = data[m][r][c];
                    if (!Float.isFinite(v)) {
                        continue;
                    }
                    if (v < mn) mn = v;
                    if (v > mx) mx = v;
                }
            }
            if (!Float.isFinite(mn) || !Float.isFinite(mx) || mn == mx) {
                mn = 0f;
                mx = 1f;
            }
            minArr[m] = minArr[m] * 0.85f + mn * 0.15f;
            maxArr[m] = maxArr[m] * 0.85f + mx * 0.15f;
            if (maxArr[m] - minArr[m] < 1e-5f) {
                maxArr[m] = minArr[m] + 1e-5f;
            }
        }
    }

    float get(int metric, int row, int col) {
        return data[metric][row][col];
    }

    float getMetricMin(int metric) {
        return minArr[metric];
    }

    float getMetricMax(int metric) {
        return maxArr[metric];
    }

    int getCols() {
        return cols;
    }

    int getRows() {
        return rows;
    }
}
