package DataWriterHealthODF_;

import Board_.WifiHealthDataSource;
import Globel.GUI;
import SerialParser_.CytonWifiHealthFrame;

import java.io.File;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static Globel.GUI.directoryManager;
import static processing.core.PApplet.createWriter;

public class DataWriterHealthODF {
    private static final String BRAND_NAME = "AirEIBCI";
    private static final String SESSION_PREFIX = BRAND_NAME + "_Session_";

    private final GUI MAIN;
    private final PrintWriter output;
    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private final long sessionStartTimestampMs;
    private long lastWrittenTimestampMs = -1L;
    private int rowsWritten = 0;
    public String fname;

    public DataWriterHealthODF(GUI MAIN, String sessionName, String fileName) {
        this.MAIN = MAIN;
        this.sessionStartTimestampMs = System.currentTimeMillis();
        MAIN.settings.setSessionPath(directoryManager.getRecordingsPath() + SESSION_PREFIX + sessionName + File.separator);
        File sessionDir = new File(MAIN.settings.getSessionPath());
        if (!sessionDir.exists()) {
            sessionDir.mkdirs();
        }
        fname = MAIN.settings.getSessionPath() + BRAND_NAME + "-Health-" + fileName + ".txt";
        output = createWriter(new File(fname));
        writeHeader();
    }

    public void appendLatest(WifiHealthDataSource board) {
        if (board == null || !board.isUsingCustomWifiParser()) {
            return;
        }

        CytonWifiHealthFrame frame = board.getLatestWifiHealthFrame();
        if (frame == null || !frame.hasMeasurement() || frame.timestampMs == lastWrittenTimestampMs) {
            return;
        }

        output.print(frame.sequence);
        output.print(", ");
        output.print(validInt(frame.heartRate));
        output.print(", ");
        output.print(validInt(frame.spo2));
        output.print(", ");
        output.print(validInt(frame.microcirculation));
        output.print(", ");
        output.print(validInt(frame.systolicPressure));
        output.print(", ");
        output.print(validInt(frame.diastolicPressure));
        output.print(", ");
        output.print(validInt(frame.respirationRate));
        output.print(", ");
        output.print(validInt(frame.fatigueIndex));
        output.print(", ");
        output.print(validInt(frame.rrIntervalRaw));
        output.print(", ");
        output.print(validInt(frame.sdnn));
        output.print(", ");
        output.print(validInt(frame.rmssd));
        output.print(", ");
        output.print(validFloat(frame.bodyTemperature));
        output.print(", ");
        output.print(validFloat(frame.ambientTemperature));
        output.print(", ");
        output.print((frame.timestampMs - sessionStartTimestampMs) / 1000.0);
        output.print(", ");
        output.print(frame.timestampMs);
        output.print(", ");
        output.print(dateFormat.format(new Date(frame.timestampMs)));
        output.println();
        output.flush();

        lastWrittenTimestampMs = frame.timestampMs;
        rowsWritten++;
    }

    public void closeFile() {
        output.close();
    }

    public int getRowsWritten() {
        return rowsWritten;
    }

    private void writeHeader() {
        output.println("%" + BRAND_NAME + " WiFi Health Data");
        output.println("%Source = EEG WiFi AUX C1/C2/C3");
        output.println("%Board = " + MAIN.currentBoard.getClass().getName());
        output.println("%Invalid byte fields are written as NaN");
        output.println("%RR interval is the module raw single-byte value, not converted to milliseconds");
        output.println("health_seq, heart_rate_bpm, spo2_percent, microcirculation, systolic_mmhg, diastolic_mmhg, respiration_rate_per_min, fatigue_index, rr_interval_raw, hrv_sdnn, hrv_rmssd, body_temperature_c, ambient_temperature_c, elapsed_seconds, timestamp_ms, timestamp_formatted");
        output.flush();
    }

    private String validInt(int value) {
        return value >= 0 ? String.valueOf(value) : "NaN";
    }

    private String validFloat(float value) {
        return !Float.isNaN(value) && value >= 0f ? String.valueOf(value) : "NaN";
    }
}
