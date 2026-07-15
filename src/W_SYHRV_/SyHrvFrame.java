package W_SYHRV_;

/** Immutable measurement decoded from one SY-HRV direct-module packet. */
public final class SyHrvFrame {
    public final int heartRate;
    public final int spo2;
    public final int microcirculation;
    public final int systolicPressure;
    public final int diastolicPressure;
    public final int respirationRate;
    public final int fatigueIndex;
    public final int rrIntervalMs;
    public final int sdnn;
    public final int rmssd;
    public final float bodyTemperature;
    public final float predictedTemperature;
    public final long timestampMs;

    SyHrvFrame(int heartRate, int spo2, int microcirculation,
               int systolicPressure, int diastolicPressure, int respirationRate,
               int fatigueIndex, int rrIntervalMs, int sdnn, int rmssd,
               float bodyTemperature, float predictedTemperature, long timestampMs) {
        this.heartRate = heartRate;
        this.spo2 = spo2;
        this.microcirculation = microcirculation;
        this.systolicPressure = systolicPressure;
        this.diastolicPressure = diastolicPressure;
        this.respirationRate = respirationRate;
        this.fatigueIndex = fatigueIndex;
        this.rrIntervalMs = rrIntervalMs;
        this.sdnn = sdnn;
        this.rmssd = rmssd;
        this.bodyTemperature = bodyTemperature;
        this.predictedTemperature = predictedTemperature;
        this.timestampMs = timestampMs;
    }

    public boolean hasMeasurement() {
        return heartRate > 0 || spo2 > 0 || systolicPressure > 0 || bodyTemperature > 0f;
    }
}
