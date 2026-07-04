package W_Indicator_;

final class IndicatorEngine {
    static final int DELTA_PCT = 0;
    static final int THETA_PCT = 1;
    static final int ALPHA_PCT = 2;
    static final int BETA_PCT = 3;
    static final int GAMMA_PCT = 4;
    static final int THETA_BETA = 5;
    static final int ALPHA_BETA = 6;
    static final int SLOW_WAVE = 7;
    static final int ENGAGEMENT = 8;
    static final int EMG_INDEX = 9;
    static final int METRIC_COUNT = 10;

    static final String[] TITLES = {
            "Delta %", "Theta %", "Alpha %", "Beta %", "Gamma %",
            "Theta/Beta", "Alpha/Beta", "Slow Wave", "Engagement", "EMG Index"
    };

    private static final float EPS = 1e-9f;

    void compute(float d, float t, float a, float b, float g, float[] out) {
        float sum = d + t + a + b + g + EPS;
        float alphaBeta = a + b + EPS;
        float alphaTheta = a + t + EPS;

        out[DELTA_PCT] = d / sum;
        out[THETA_PCT] = t / sum;
        out[ALPHA_PCT] = a / sum;
        out[BETA_PCT] = b / sum;
        out[GAMMA_PCT] = g / sum;
        out[THETA_BETA] = t / (b + EPS);
        out[ALPHA_BETA] = a / (b + EPS);
        out[SLOW_WAVE] = (d + t) / alphaBeta;
        out[ENGAGEMENT] = b / alphaTheta;
        out[EMG_INDEX] = g / alphaBeta;
    }
}
