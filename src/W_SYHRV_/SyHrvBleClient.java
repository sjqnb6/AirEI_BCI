package W_SYHRV_;

import com.sun.jna.Callback;
import com.sun.jna.Function;
import com.sun.jna.Memory;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BLE GATT client for the SY-HRV module. The UUIDs and packet format come from
 * {@code 蓝牙服务协议说明.docx}: UUID2 receives commands and UUID1 notifies 24-byte frames.
 */
public final class SyHrvBleClient {
    public interface Listener {
        void onFrame(SyHrvFrame frame);
        void onConnectionNotice(String message);
    }

    public static final class Device {
        public final String name;
        public final String address;
        public final int rssi;

        Device(String name, String address, int rssi) {
            this.name = name == null ? "" : name;
            this.address = address == null ? "" : address;
            this.rssi = rssi;
        }

        @Override
        public String toString() {
            return name.isEmpty() ? address : name + " [" + address + "]";
        }
    }

    // The document lists little-endian firmware byte arrays. GATT APIs use canonical UUID order.
    // UUID0 = Nordic UART Service, UUID1 = module -> app notify, UUID2 = app -> module write.
    private static final String SERVICE_UUID = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    private static final String NOTIFY_UUID = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
    private static final String WRITE_UUID = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";
    private static final byte COMMAND_START_MEASUREMENT = 0x24;
    private static final byte COMMAND_STOP_MEASUREMENT = 0x2A;
    private static final int SIMPLEBLE_SUCCESS = 0;
    private static final int SIMPLEBLE_FAILURE = 1;

    private final Listener listener;
    private final SyHrvPacketDecoder decoder = new SyHrvPacketDecoder();
    private final Object nativeLock = new Object();
    private NativeLibrary library;
    private Pointer adapter;
    private Pointer peripheral;
    private final Map<String, Pointer> scannedPeripherals = new HashMap<String, Pointer>();
    private ScanPeripheralCallback scanPeripheralCallback;
    private NotificationCallback notificationCallback;
    private volatile String deviceName = "";
    private volatile String deviceAddress = "";
    private volatile String lastError = "";
    private volatile long lastFrameMs = 0L;
    private volatile boolean connected = false;
    private volatile boolean protocolVerified = false;

    public SyHrvBleClient(Listener listener) {
        this.listener = listener;
    }

    /** Scans with the system BLE adapter. It is blocking and must be run off the draw thread. */
    public List<Device> scan(int durationMs) {
        synchronized (nativeLock) {
            if (connected) {
                setError("模块已连接；请先断开后再扫描");
                return Collections.emptyList();
            }
            if (!loadLibrary()) {
                return Collections.emptyList();
            }
            releaseScannedPeripherals();
            releaseAdapter();
            long adapterCount = invokeLong("simpleble_adapter_get_count");
            if (adapterCount < 1) {
                setError("未找到可用的 BLE 适配器");
                return Collections.emptyList();
            }
            adapter = invokePointer("simpleble_adapter_get_handle", 0L);
            if (isNull(adapter)) {
                setError("无法打开系统 BLE 适配器");
                return Collections.emptyList();
            }

            // Active scanning can first report a device without a local name and then deliver the
            // name in a later scan-response update. Capture both events instead of only reading the
            // final SimpleBLE snapshot after scan_for() returns.
            final List<Device> result = new ArrayList<Device>();
            final Object callbackCaptureLock = new Object();
            final boolean[] acceptCallbackResults = {true};
            scanPeripheralCallback = new ScanPeripheralCallback() {
                @Override
                public void invoke(Pointer adapterHandle, Pointer found, Pointer userData) {
                    if (isNull(found)) {
                        return;
                    }
                    boolean retained = false;
                    synchronized (callbackCaptureLock) {
                        try {
                            if (acceptCallbackResults[0]) {
                                retained = retainIfSimpleDevice(found, result);
                            }
                        } finally {
                            // The C callback creates a new peripheral handle for every event.
                            // Handles not transferred to scannedPeripherals must be released here.
                            if (!retained) {
                                invokeVoid("simpleble_peripheral_release_handle", found);
                            }
                        }
                    }
                }
            };
            invokeVoid("simpleble_adapter_set_callback_on_scan_found", adapter, scanPeripheralCallback, null);
            invokeVoid("simpleble_adapter_set_callback_on_scan_updated", adapter, scanPeripheralCallback, null);

            int scanStatus = invokeInt("simpleble_adapter_scan_for", adapter, Math.max(1000, durationMs));
            synchronized (callbackCaptureLock) {
                acceptCallbackResults[0] = false;
            }
            if (scanStatus != SIMPLEBLE_SUCCESS) {
                setError("蓝牙设备扫描失败");
                releaseScannedPeripherals();
                releaseAdapter();
                return Collections.emptyList();
            }
            long count = invokeLong("simpleble_adapter_scan_get_results_count", adapter);
            for (long i = 0; i < count; i++) {
                Pointer found = invokePointer("simpleble_adapter_scan_get_results_handle", adapter, i);
                if (isNull(found)) {
                    continue;
                }
                boolean retained = false;
                try {
                    retained = retainIfSimpleDevice(found, result);
                } finally {
                    if (!retained) {
                        invokeVoid("simpleble_peripheral_release_handle", found);
                    }
                }
            }

            // Windows may show a cached/paired friendly name even when the live advertisement omits its name.
            long pairedCount = invokeLong("simpleble_adapter_get_paired_peripherals_count", adapter);
            for (long i = 0; i < pairedCount; i++) {
                Pointer paired = invokePointer("simpleble_adapter_get_paired_peripherals_handle", adapter, i);
                if (isNull(paired)) {
                    continue;
                }
                boolean retained = false;
                try {
                    retained = retainIfSimpleDevice(paired, result);
                } finally {
                    if (!retained) {
                        invokeVoid("simpleble_peripheral_release_handle", paired);
                    }
                }
            }

            // If more than one matching module is nearby, automatically choose the strongest/nearest signal first.
            Collections.sort(result, (left, right) -> Integer.compare(right.rssi, left.rssi));
            if (result.isEmpty()) {
                long visibleCount = count + pairedCount;
                setError(visibleCount == 0
                        ? "未扫描到附近的 BLE 设备"
                        : "实时及已配对列表共发现 " + visibleCount + " 台 BLE 设备，但没有名称以 simple 开头的设备");
                releaseAdapter();
            }
            return result;
        }
    }

    /** Connects to an item returned by {@link #scan(int)} and starts notifications. */
    public boolean connect(Device device) {
        synchronized (nativeLock) {
            if (device == null || device.address.isEmpty()) {
                setError("请先扫描并选择 SY-HRV 蓝牙设备");
                return false;
            }
            if (!loadLibrary()) {
                return false;
            }
            if (peripheral != null || connected) {
                setError("已有 BLE 连接，请先断开");
                return false;
            }
            peripheral = scannedPeripherals.remove(normalizeAddress(device.address));
            releaseScannedPeripherals();
            if (isNull(peripheral)) {
                peripheral = null;
                setError("设备扫描结果已失效，请重新点击“自动查找”");
                releaseAdapter();
                return false;
            }
            boolean nativeConnected = readNativeFlag("simpleble_peripheral_is_connected", peripheral);
            boolean nativeConnectable = readNativeFlag("simpleble_peripheral_is_connectable", peripheral);
            if (!nativeConnected) {
                int connectResult = SIMPLEBLE_FAILURE;
                for (int attempt = 0; attempt < 3 && connectResult != SIMPLEBLE_SUCCESS; attempt++) {
                    connectResult = invokeInt("simpleble_peripheral_connect", peripheral);
                    if (connectResult != SIMPLEBLE_SUCCESS && attempt < 2) {
                        try {
                            Thread.sleep(350L);
                        } catch (InterruptedException interrupted) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
                if (connectResult != SIMPLEBLE_SUCCESS
                        && !readNativeFlag("simpleble_peripheral_is_connected", peripheral)) {
                    setError("无法连接 " + device + (nativeConnectable
                            ? "；请确认模块未连接手机或其他程序后重试"
                            : "；设备当前不可连接，可能已被手机或其他程序占用"));
                    closeLocked();
                    return false;
                }
            }
            // Validate again after GATT service discovery. This prevents a stale or forged scan result being used.
            if (!hasSyHrvService(peripheral)) {
                setError("连接设备未提供说明书指定的 SY-HRV 服务，已拒绝连接");
                closeLocked();
                return false;
            }

            SyHrvRawLogger.beginSession("BLE", "device=" + device.name + " address=" + device.address);
            decoder.reset();
            protocolVerified = false;
            notificationCallback = new NotificationCallback() {
                @Override
                public void invoke(Uuid service, Uuid characteristic, Pointer data, long dataLength, Pointer userData) {
                    if (data == null || dataLength <= 0 || dataLength > 1024) {
                        return;
                    }
                    byte[] bytes = data.getByteArray(0, (int) dataLength);
                    SyHrvRawLogger.logReceived("BLE", bytes, 0, bytes.length);
                    for (byte value : bytes) {
                        SyHrvFrame decoded = decoder.accept(value);
                        if (decoded != null) {
                            lastFrameMs = decoded.timestampMs;
                            if (!protocolVerified) {
                                protocolVerified = true;
                                listener.onConnectionNotice("SY-HRV 的 24 字节数据协议校验通过");
                            }
                            listener.onFrame(decoded);
                        }
                    }
                }
            };
            Uuid service = new Uuid(SERVICE_UUID);
            Uuid notify = new Uuid(NOTIFY_UUID);
            if (invokeInt("simpleble_peripheral_notify", peripheral, service, notify, notificationCallback, null)
                    != SIMPLEBLE_SUCCESS) {
                setError("模块不支持协议指定的通知特征（UUID1）");
                closeLocked();
                return false;
            }
            if (!writeCommand(COMMAND_START_MEASUREMENT)) {
                setError("已连接，但无法向 UUID2 发送开始测量命令");
                closeLocked();
                return false;
            }
            String connectedName = readAndFree("simpleble_peripheral_identifier", peripheral);
            String connectedAddress = readAndFree("simpleble_peripheral_address", peripheral);
            deviceName = connectedName.isEmpty() ? device.name : connectedName;
            deviceAddress = connectedAddress.isEmpty() ? device.address : connectedAddress;
            lastError = "";
            connected = true;
            listener.onConnectionNotice("BLE 已连接 " + displayName() + "，已自动发送 0x24 开始测量指令，等待数据");
            SyHrvRawLogger.logEvent("BLE connected; raw log=" + SyHrvRawLogger.getLogPath());
            return true;
        }
    }

    public void close() {
        synchronized (nativeLock) {
            closeLocked();
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public String getLastError() {
        return lastError;
    }

    public long getLastFrameMs() {
        return lastFrameMs;
    }

    public boolean isProtocolVerified() {
        return protocolVerified;
    }

    private void closeLocked() {
        connected = false;
        if (peripheral != null && library != null) {
            try {
                writeCommand(COMMAND_STOP_MEASUREMENT);
                invokeInt("simpleble_peripheral_unsubscribe", peripheral, new Uuid(SERVICE_UUID), new Uuid(NOTIFY_UUID));
                invokeInt("simpleble_peripheral_disconnect", peripheral);
            } catch (Exception ignored) {
                // The device may already be disconnected; native handles still need releasing.
            } finally {
                invokeVoid("simpleble_peripheral_release_handle", peripheral);
                peripheral = null;
            }
        }
        releaseScannedPeripherals();
        releaseAdapter();
        notificationCallback = null;
        protocolVerified = false;
        decoder.reset();
        SyHrvRawLogger.endSession("BLE");
    }

    private String displayName() {
        return deviceName == null || deviceName.trim().isEmpty() ? deviceAddress : deviceName;
    }

    private void releaseAdapter() {
        if (adapter != null && library != null) {
            invokeVoid("simpleble_adapter_release_handle", adapter);
            adapter = null;
        }
        scanPeripheralCallback = null;
    }

    private void releaseScannedPeripherals() {
        if (library != null) {
            for (Pointer handle : scannedPeripherals.values()) {
                if (!isNull(handle) && handle != peripheral) {
                    invokeVoid("simpleble_peripheral_release_handle", handle);
                }
            }
        }
        scannedPeripherals.clear();
    }

    private String normalizeAddress(String address) {
        return address == null ? "" : address.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private boolean retainIfSimpleDevice(Pointer handle, List<Device> result) {
        String name = readAndFree("simpleble_peripheral_identifier", handle);
        String address = readAndFree("simpleble_peripheral_address", handle);
        if (!name.trim().toLowerCase(java.util.Locale.ROOT).startsWith("simple")) {
            return false;
        }
        String key = normalizeAddress(address);
        if (key.isEmpty() || scannedPeripherals.containsKey(key)) {
            return false;
        }
        result.add(new Device(name, address, invokeShort("simpleble_peripheral_rssi", handle)));
        scannedPeripherals.put(key, handle);
        return true;
    }

    private boolean writeCommand(byte command) {
        if (peripheral == null) {
            return false;
        }
        byte[] bytes = {command};
        Uuid service = new Uuid(SERVICE_UUID);
        Uuid write = new Uuid(WRITE_UUID);
        int result = invokeInt("simpleble_peripheral_write_request", peripheral, service, write, bytes, 1L);
        // Some firmware exposes the same characteristic as write-without-response.
        boolean success = result == SIMPLEBLE_SUCCESS;
        String method = "write-request";
        if (!success) {
            success = invokeInt("simpleble_peripheral_write_command", peripheral, service, write, bytes, 1L)
                    == SIMPLEBLE_SUCCESS;
            method = "write-command fallback";
        }
        SyHrvRawLogger.logSent("BLE", bytes, 0, bytes.length,
                method + (success ? " success" : " failed"));
        return success;
    }

    /** Reads the first field of each simpleble_service_t; it is the canonical service UUID string. */
    private boolean hasSyHrvService(Pointer device) {
        long serviceCount = invokeLong("simpleble_peripheral_services_count", device);
        if (serviceCount <= 0 || serviceCount > 256) {
            return false;
        }
        // simpleble_service_t 0.6.1 is about 10.5 KiB; extra capacity protects against ABI padding.
        Memory service = new Memory(16384);
        for (long i = 0; i < serviceCount; i++) {
            service.clear();
            if (invokeInt("simpleble_peripheral_services_get", device, i, service) == SIMPLEBLE_SUCCESS) {
                String uuid = service.getString(0);
                if (SERVICE_UUID.equalsIgnoreCase(uuid)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean readNativeFlag(String functionName, Pointer device) {
        Memory value = new Memory(1);
        value.setByte(0, (byte) 0);
        return invokeInt(functionName, device, value) == SIMPLEBLE_SUCCESS && value.getByte(0) != 0;
    }

    private boolean loadLibrary() {
        if (library != null) {
            return true;
        }
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            setError("当前内置 BLE 通讯实现仅支持 Windows");
            return false;
        }
        try (InputStream source = SyHrvBleClient.class.getClassLoader().getResourceAsStream("brainflow/simpleble-c.dll")) {
            if (source == null) {
                setError("未找到项目内置的 SimpleBLE 运行库");
                return false;
            }
            // Keep the original DLL name: Windows resolves its dependencies relative to it.
            Path target = Path.of(System.getProperty("java.io.tmpdir"), "simpleble-c.dll");
            if (!Files.exists(target) || Files.size(target) == 0L) {
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            }
            library = NativeLibrary.getInstance(target.toAbsolutePath().toString());
            return true;
        } catch (Exception error) {
            setError("加载 Windows BLE 运行库失败：" + error.getMessage());
            return false;
        }
    }

    private String readAndFree(String functionName, Pointer handle) {
        Pointer value = invokePointer(functionName, handle);
        if (isNull(value)) {
            return "";
        }
        try {
            return value.getString(0);
        } finally {
            invokeVoid("simpleble_free", value);
        }
    }

    private int invokeInt(String name, Object... arguments) {
        return (Integer) library.getFunction(name).invoke(Integer.class, arguments);
    }

    private long invokeLong(String name, Object... arguments) {
        return (Long) library.getFunction(name).invoke(Long.class, arguments);
    }

    private short invokeShort(String name, Object... arguments) {
        return (Short) library.getFunction(name).invoke(Short.class, arguments);
    }

    private Pointer invokePointer(String name, Object... arguments) {
        return (Pointer) library.getFunction(name).invoke(Pointer.class, arguments);
    }

    private void invokeVoid(String name, Object... arguments) {
        library.getFunction(name).invokeVoid(arguments);
    }

    private void setError(String message) {
        lastError = message;
        listener.onConnectionNotice(message);
    }

    private boolean isNull(Pointer pointer) {
        return pointer == null || Pointer.nativeValue(pointer) == 0L;
    }

    private interface NotificationCallback extends Callback {
        void invoke(Uuid service, Uuid characteristic, Pointer data, long dataLength, Pointer userData);
    }

    private interface ScanPeripheralCallback extends Callback {
        void invoke(Pointer adapterHandle, Pointer peripheralHandle, Pointer userData);
    }

    @Structure.FieldOrder({"value"})
    public static class Uuid extends Structure implements Structure.ByValue {
        public byte[] value = new byte[37];

        public Uuid() {
        }

        Uuid(String uuid) {
            byte[] encoded = uuid.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            System.arraycopy(encoded, 0, value, 0, Math.min(encoded.length, value.length - 1));
        }
    }
}
