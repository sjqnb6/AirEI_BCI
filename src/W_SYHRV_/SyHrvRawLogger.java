package W_SYHRV_;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Thread-safe raw byte logger used to diagnose SY-HRV UART and BLE traffic. */
public final class SyHrvRawLogger {
    private static final Object FILE_LOCK = new Object();
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final Path LOG_PATH = resolveLogPath();
    private static volatile String lastError = "";

    private SyHrvRawLogger() {
    }

    public static void beginSession(String transport, String detail) {
        append(System.lineSeparator()
                + "==================== " + timestamp() + " " + clean(transport)
                + " SESSION START ====================" + System.lineSeparator()
                + timestamp() + " | EVENT | " + clean(detail));
    }

    public static void endSession(String transport) {
        append(timestamp() + " | EVENT | " + clean(transport) + " SESSION END");
    }

    public static void logReceived(String transport, byte[] bytes, int offset, int length) {
        logBytes(transport + " RX", bytes, offset, length, "");
    }

    public static void logSent(String transport, byte[] bytes, int offset, int length, String result) {
        logBytes(transport + " TX", bytes, offset, length, result);
    }

    public static void logEvent(String message) {
        append(timestamp() + " | EVENT | " + clean(message));
    }

    public static Path getLogPath() {
        return LOG_PATH;
    }

    public static String getLastError() {
        return lastError;
    }

    private static void logBytes(String direction, byte[] bytes, int offset, int length, String suffix) {
        if (bytes == null || length <= 0 || offset < 0 || offset >= bytes.length) {
            return;
        }
        int safeLength = Math.min(length, bytes.length - offset);
        StringBuilder hex = new StringBuilder(safeLength * 3);
        for (int i = 0; i < safeLength; i++) {
            if (i > 0) {
                hex.append(' ');
            }
            int value = bytes[offset + i] & 0xFF;
            if (value < 0x10) {
                hex.append('0');
            }
            hex.append(Integer.toHexString(value).toUpperCase(java.util.Locale.ROOT));
        }
        String extra = suffix == null || suffix.trim().isEmpty() ? "" : " | " + clean(suffix);
        append(timestamp() + " | " + direction + " | len=" + safeLength + " | " + hex + extra);
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
                System.err.println("SY-HRV raw log write failed: " + lastError);
            }
        }
    }

    private static Path resolveLogPath() {
        Path projectSource = Path.of(System.getProperty("user.dir", "."), "src", "W_SYHRV_");
        if (Files.isDirectory(projectSource)) {
            return projectSource.resolve("sy_hrv_raw_bytes.log").toAbsolutePath().normalize();
        }
        return Path.of(System.getProperty("user.dir", "."), "sy_hrv_raw_bytes.log")
                .toAbsolutePath().normalize();
    }

    private static String timestamp() {
        return LocalDateTime.now().format(TIME_FORMAT);
    }

    private static String clean(String value) {
        return value == null ? "" : value.replace('\r', ' ').replace('\n', ' ').trim();
    }
}
