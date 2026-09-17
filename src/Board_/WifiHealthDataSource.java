package Board_;

import SerialParser_.CytonWifiHealthFrame;

/** Health metrics carried in the C1/C2/C3 AUX fields of a WiFi EEG stream. */
public interface WifiHealthDataSource {
    boolean isUsingCustomWifiParser();

    boolean isStreaming();

    CytonWifiHealthFrame getLatestWifiHealthFrame();

    long getLastWifiHealthAuxFrameTimestampMs();

    String getLatestWifiHealthAuxDebugText();
}
