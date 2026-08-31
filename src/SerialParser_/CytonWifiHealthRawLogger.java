package SerialParser_;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Raw WiFi AUX health logger for diagnosing C1/C2/C3 bytes from EEG frames. */
public final class CytonWifiHealthRawLogger {
    private static final Object FILE_LOCK = new Object();
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final Path LOG_PATH = resolveLogPath();
    private static volatile String lastError = "";

    private CytonWifiHealthRawLogger() {
    }

    public static void beginSession(String host, int port) {
        append(System.lineSeparator()
                + "==================== " + timestamp()
                + " EEG WIFI HEALTH RAW SESSION START ===================="
                + System.lineSeparator()
                + timestamp() + " | EVENT | host=" + clean(host) + " port=" + port
                + " log=" + LOG_PATH);
    }

    public static void endSession() {
        append(timestamp() + " | EVENT | EEG WIFI HEALTH RAW SESSION END");
    }

    public static void logHealthFrame(int auxType, int healthSeq, byte[] frame, int auxOffset, int auxSize) {
        if (frame == null || auxOffset < 0 || auxSize <= 0 || auxOffset + auxSize > frame.length) {
            return;
        }
        StringBuilder line = new StringBuilder(256);
        line.append(timestamp())
                .append(" | AUX_TYPE=C").append(auxType)
                .append(" | health_seq=").append(healthSeq)
                .append(" | aux=").append(hex(frame, auxOffset, auxSize))
                .append(" | frame=").append(hex(frame, 0, frame.length));
        append(line.toString());
    }

    public static String getLogPath() {
        return LOG_PATH.toString();
    }

    public static String getLastError() {
        return lastError;
    }

    private static void append(String line) {
        synchronized (FILE_LOCK) {
            try {
                Path parent = LOG_PATH.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.writeString(LOG_PATH, line + System.lineSeparator(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
                lastError = "";
            } catch (IOException error) {
                lastError = error.getClass().getSimpleName() + ": " + error.getMessage();
                System.err.println("Cyton WiFi health raw log write failed: " + lastError);
            }
        }
    }

    private static String hex(byte[] bytes, int offset, int length) {
        int safeLength = Math.min(length, bytes.length - offset);
        StringBuilder out = new StringBuilder(safeLength * 3);
        for (int i = 0; i < safeLength; i++) {
            if (i > 0) {
                out.append(' ');
            }
            int value = bytes[offset + i] & 0xFF;
            if (value < 0x10) {
                out.append('0');
            }
            out.append(Integer.toHexString(value).toUpperCase(java.util.Locale.ROOT));
        }
        return out.toString();
    }

    private static Path resolveLogPath() {
        Path projectSource = Path.of(System.getProperty("user.dir", "."), "src", "SerialParser_");
        if (Files.isDirectory(projectSource)) {
            return projectSource.resolve("cyton_wifi_health_raw.log").toAbsolutePath().normalize();
        }
        return Path.of(System.getProperty("user.dir", "."), "cyton_wifi_health_raw.log")
                .toAbsolutePath().normalize();
    }

    private static String timestamp() {
        return LocalDateTime.now().format(TIME_FORMAT);
    }

    private static String clean(String value) {
        return value == null ? "" : value.replace('\r', ' ').replace('\n', ' ').trim();
    }
}
