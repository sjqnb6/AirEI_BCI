package W_Indicator_;

final class IndicatorEngine {
    static final String[] TITLES = {
            "\u03b4/\u2211", "\u03b8/\u2211", "\u03b1/\u2211", "\u03b2/\u2211",
            "\u03b4/\u03b2", "\u03b4/(\u03b1+\u03b2)", "\u03b4/(\u03b8+\u03b1+\u03b2)",
            "\u03b8/\u03b2", "\u03b8/(\u03b1+\u03b2)", "\u03b8/(\u03b4+\u03b1+\u03b2)",
            "\u03b1/\u03b2", "(\u03b8+\u03b1)/\u03b2", "(\u03b8+\u03b4)/(\u03b1+\u03b2)", "(\u03b8+\u03b1)/(\u03b1+\u03b2)"
    };

    private static final float EPS = 1e-9f;

    void compute(float d, float t, float a, float b, float[] out14) {
        float sum = d + t + a + b + EPS;
        float ab = a + b + EPS;
        float tab = t + a + b + EPS;
        float dab = d + a + b + EPS;

        out14[0] = d / sum;
        out14[1] = t / sum;
        out14[2] = a / sum;
        out14[3] = b / sum;
        out14[4] = d / (b + EPS);
        out14[5] = d / ab;
        out14[6] = d / tab;
        out14[7] = t / (b + EPS);
        out14[8] = t / ab;
        out14[9] = t / dab;
        out14[10] = a / (b + EPS);
        out14[11] = (t + a) / (b + EPS);
        out14[12] = (t + d) / ab;
        out14[13] = (t + a) / ab;
    }
}
