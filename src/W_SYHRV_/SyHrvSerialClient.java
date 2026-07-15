package W_SYHRV_;

import com.fazecast.jSerialComm.SerialPort;

/** Non-blocking serial reader for a direct SY-HRV module or its Bluetooth virtual COM port. */
public final class SyHrvSerialClient {
    public interface Listener {
        void onFrame(SyHrvFrame frame);
        void onConnectionNotice(String message);
    }

    private final Listener listener;
    private final SyHrvPacketDecoder decoder = new SyHrvPacketDecoder();
    private static final byte COMMAND_START_MEASUREMENT = 0x24;
    private static final byte COMMAND_STOP_MEASUREMENT = 0x2A;
    private SerialPort serialPort;
    private String portName = "";
    private int baudRate = 9600;
    private String lastError = "";
    private long lastFrameMs = 0L;

    public SyHrvSerialClient(Listener listener) {
        this.listener = listener;
    }

    public boolean connect(String requestedPort, int requestedBaud) {
        close();
        String trimmedPort = requestedPort == null ? "" : requestedPort.trim();
        if (trimmedPort.isEmpty()) {
            setError("请输入 SY-HRV 的串口号，例如 COM3");
            return false;
        }

        portName = trimmedPort;
        baudRate = requestedBaud;
        serialPort = SerialPort.getCommPort(portName);
        serialPort.setComPortParameters(baudRate, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);
        if (!serialPort.openPort()) {
            setError("无法打开 " + portName + "，请确认端口未被占用");
            serialPort = null;
            return false;
        }

        decoder.reset();
        SyHrvRawLogger.beginSession("SERIAL", "port=" + portName + " baud=" + baudRate);
        byte[] startCommand = {COMMAND_START_MEASUREMENT};
        int written = serialPort.writeBytes(startCommand, 1);
        SyHrvRawLogger.logSent("SERIAL", startCommand, 0, 1,
                written == 1 ? "success" : "failed written=" + written);
        lastError = "";
        listener.onConnectionNotice("已连接 " + portName + "（" + baudRate + "），已发送开始测量命令");
        return true;
    }

    public void poll() {
        if (!isConnected()) {
            return;
        }
        int available = serialPort.bytesAvailable();
        if (available <= 0) {
            return;
        }

        byte[] data = new byte[Math.min(available, 256)];
        int read = serialPort.readBytes(data, data.length);
        if (read < 0) {
            setError("读取 " + portName + " 失败");
            return;
        }
        SyHrvRawLogger.logReceived("SERIAL", data, 0, read);
        for (int i = 0; i < read; i++) {
            SyHrvFrame frame = decoder.accept(data[i]);
            if (frame != null) {
                lastFrameMs = frame.timestampMs;
                listener.onFrame(frame);
            }
        }
    }

    public void close() {
        if (serialPort != null) {
            try {
                if (serialPort.isOpen()) {
                    byte[] stopCommand = {COMMAND_STOP_MEASUREMENT};
                    int written = serialPort.writeBytes(stopCommand, 1);
                    SyHrvRawLogger.logSent("SERIAL", stopCommand, 0, 1,
                            written == 1 ? "success" : "failed written=" + written);
                    serialPort.closePort();
                }
            } finally {
                serialPort = null;
                SyHrvRawLogger.endSession("SERIAL");
            }
        }
        decoder.reset();
    }

    public boolean isConnected() {
        return serialPort != null && serialPort.isOpen();
    }

    public String getPortName() {
        return portName;
    }

    public int getBaudRate() {
        return baudRate;
    }

    public String getLastError() {
        return lastError;
    }

    public long getLastFrameMs() {
        return lastFrameMs;
    }

    private void setError(String message) {
        lastError = message;
        listener.onConnectionNotice(message);
    }
}
