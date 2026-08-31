package SerialParser_;

/**
 * One complete health measurement reconstructed from C1/C2/C3 AUX frames in
 * the custom Cyton WiFi EEG stream.
 */
public final class CytonWifiHealthFrame {
    public final int sequence;
    public final int heartRate;
    public final int spo2;
    public final int microcirculation;
    public final int systolicPressure;
    public final int diastolicPressure;
    public final int respirationRate;
    public final int fatigueIndex;
    public final int rrIntervalRaw;
    public final int sdnn;
    public final int rmssd;
    public final float bodyTemperature;
    public final float ambientTemperature;
    public final long timestampMs;

    public CytonWifiHealthFrame(
            int sequence,
            int heartRate,
            int spo2,
            int microcirculation,
            int systolicPressure,
            int diastolicPressure,
            int respirationRate,
            int fatigueIndex,
            int rrIntervalRaw,
            int sdnn,
            int rmssd,
            float bodyTemperature,
            float ambientTemperature,
            long timestampMs) {
        this.sequence = sequence;
        this.heartRate = heartRate;
        this.spo2 = spo2;
        this.microcirculation = microcirculation;
        this.systolicPressure = systolicPressure;
        this.diastolicPressure = diastolicPressure;
        this.respirationRate = respirationRate;
        this.fatigueIndex = fatigueIndex;
        this.rrIntervalRaw = rrIntervalRaw;
        this.sdnn = sdnn;
        this.rmssd = rmssd;
        this.bodyTemperature = bodyTemperature;
        this.ambientTemperature = ambientTemperature;
        this.timestampMs = timestampMs;
    }

    public boolean hasMeasurement() {
        return heartRate > 0
                || spo2 > 0
                || microcirculation > 0
                || systolicPressure > 0
                || diastolicPressure > 0
                || respirationRate > 0
                || fatigueIndex > 0
                || rrIntervalRaw > 0
                || sdnn > 0
                || rmssd > 0
                || (!Float.isNaN(bodyTemperature) && bodyTemperature > 0f);
    }
}
