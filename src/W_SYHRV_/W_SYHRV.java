package W_SYHRV_;

import Board_.WifiHealthDataSource;
import DataSourcePlayback_.DataSourcePlayback;
import Globel.GUI;
import SerialParser_.CytonWifiHealthFrame;
import Widget_.Widget;
import controlP5.Button;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlP5;
import controlP5.Controller;
import controlP5.Textfield;
import processing.core.PApplet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static Globel.GUI.p7;

/** Visualizes the calculated health metrics reported by a direct SY-HRV module. */
public class W_SYHRV extends Widget implements SyHrvSerialClient.Listener, SyHrvBleClient.Listener {
    private static final int BG_TOP = 0xFF071120;
    private static final int BG_BOTTOM = 0xFF0E1A30;
    private static final int PANEL = 0xCC13243F;
    private static final int PANEL_STROKE = 0x6683A2CC;
    private static final int CARD = 0x2A163153;
    private static final int TEXT_MAIN = 0xFFEAF2FF;
    private static final int TEXT_SUB = 0xFF98AECE;
    private static final int GOOD = 0xFF54D59D;
    private static final int WARN = 0xFFE9B45A;
    private static final int BAD = 0xFFF06B6E;
    private static final int INFO = 0xFF56C8FF;
    private static final int TREND_POINTS = 180;
    private static final String[] SOURCE_LABELS = {"硬件", "模拟", "暂停"};
    private static final String[] TRANSPORT_LABELS = {"USB 串口", "蓝牙 BLE", "EEG WiFi"};
    private static final String[] TREND_WINDOW_LABELS = {"60 秒", "120 秒", "180 秒"};
    private static final String[] AUX_DEBUG_LABELS = {"Off", "On"};
    private static final int[] TREND_WINDOW_POINTS = {60, 120, 180};

    private final GUI MAIN;
    private final SyHrvSerialClient serialClient;
    private final SyHrvBleClient bleClient;
    private final ControlP5 localCp5;
    private final Textfield portTf;
    private final Textfield baudTf;
    private final Textfield bleDeviceTf;
    private final Button connectBtn;
    private final Button scanBtn;
    private final Button clearBtn;
    private final List<Controller> cp5Elements = new ArrayList<Controller>();

    private final float[] spo2History = new float[TREND_POINTS];
    private final float[] heartHistory = new float[TREND_POINTS];
    private final float[] respirationHistory = new float[TREND_POINTS];
    private final float[] rrHistory = new float[TREND_POINTS];
    private final float[] sdnnHistory = new float[TREND_POINTS];
    private final float[] rmssdHistory = new float[TREND_POINTS];
    private int historyWrite = 0;
    private boolean historyFilled = false;

    private SyHrvFrame frame;
    private volatile SyHrvFrame incomingFrame;
    private int sourceIndex = 0;
    private int transportIndex = 0;
    private int trendWindowIndex = 0;
    private int auxDebugIndex = 0;
    private long lastDemoMs = 0L;
    private float demoPhase = 0f;
    private long lastVisualUpdateMs = 0L;
    private long frameTransitionMs = 0L;
    private float shownSpo2 = Float.NaN;
    private float shownHeartRate = Float.NaN;
    private float shownSystolic = Float.NaN;
    private float shownDiastolic = Float.NaN;
    private float shownBodyTemperature = Float.NaN;
    private volatile String connectionNotice = "等待连接 SY-HRV 模块";
    private volatile long connectionNoticeMs = 0L;
    private volatile List<SyHrvBleClient.Device> pendingBleDevices;
    private List<SyHrvBleClient.Device> bleCandidates = new ArrayList<SyHrvBleClient.Device>();
    private SyHrvBleClient.Device selectedBleDevice;
    private volatile SyHrvBleClient.Device connectedBleDevice;
    private volatile boolean bleScanInProgress = false;
    private volatile boolean bleConnectInProgress = false;
    private volatile boolean bleConnectionFailed = false;
    private long lastWifiHealthTimestampMs = -1L;
    private String lastPlaybackHealthKey = "";
    private long lastWifiNoticeMs = 0L;

    public W_SYHRV(GUI MAIN) {
        super(MAIN);
        this.MAIN = MAIN;
        dropdownWidth = 82;
        addDropdown("SyHrvSource", "数据源", Arrays.asList(SOURCE_LABELS), sourceIndex);
        addDropdown("SyHrvTransport", "传输方式", Arrays.asList(TRANSPORT_LABELS), transportIndex);
        addDropdown("SyHrvTrendWindow", "趋势窗口", Arrays.asList(TREND_WINDOW_LABELS), trendWindowIndex);
        addDropdown("SyHrvAuxDebug", "AUX", Arrays.asList(AUX_DEBUG_LABELS), auxDebugIndex);

        serialClient = new SyHrvSerialClient(this);
        bleClient = new SyHrvBleClient(this);
        localCp5 = new ControlP5(MAIN);
        localCp5.setGraphics(MAIN, 0, 0);
        localCp5.setAutoDraw(false);

        portTf = makeTextfield("syHrvPort", "COM3", x0 + 6, 142);
        baudTf = makeTextfield("syHrvBaud", "9600", x0 + 152, 70);
        bleDeviceTf = makeTextfield("syHrvBleDevice", "", x0 + 6, 214);

        connectBtn = MAIN.createButton(localCp5, "syHrvConnect", "连接", x0 + 226, y0 + navH + 1, 68, navH - 3,
                p7, 12, MAIN.colorNotPressed, MAIN.OPENBCI_DARKBLUE);
        connectBtn.setBorderColor(0xFF82A3CC);
        connectBtn.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent event) {
                toggleConnection();
            }
        });

        scanBtn = MAIN.createButton(localCp5, "syHrvBleScan", "自动查找", x0 + 224, y0 + navH + 1, 70, navH - 3,
                p7, 12, MAIN.colorNotPressed, MAIN.OPENBCI_DARKBLUE);
        scanBtn.setBorderColor(0xFF82A3CC);
        scanBtn.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent event) {
                scanBleDevices();
            }
        });

        clearBtn = MAIN.createButton(localCp5, "syHrvClear", "清空趋势", x0 + 298, y0 + navH + 1, 78, navH - 3,
                p7, 12, MAIN.colorNotPressed, MAIN.OPENBCI_DARKBLUE);
        clearBtn.setBorderColor(0xFF6F8FB9);
        clearBtn.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent event) {
                clearHistory();
                setNotice("趋势记录已清空");
            }
        });

        cp5Elements.add(portTf);
        cp5Elements.add(baudTf);
        cp5Elements.add(bleDeviceTf);
        cp5Elements.add(connectBtn);
        cp5Elements.add(scanBtn);
        cp5Elements.add(clearBtn);
        layoutTransportControls();
    }

    private Textfield makeTextfield(String name, String value, int x, int width) {
        Textfield field = localCp5.addTextfield(name)
                .setPosition(x, y0 + navH + 1)
                .setCaptionLabel("")
                .setSize(width, navH - 3)
                .setFont(p7)
                .setFocus(false)
                .setColor(MAIN.color(230))
                .setColorBackground(MAIN.color(22, 33, 54))
                .setColorValueLabel(MAIN.color(236))
                .setColorForeground(MAIN.color(115, 145, 190))
                .setColorActive(MAIN.isSelected_color)
                .setColorCursor(MAIN.color(230))
                .setAutoClear(false)
                .setText(value);
        field.onDoublePress(new CallbackListener() {
            public void controlEvent(CallbackEvent event) {
                field.clear();
            }
        });
        return field;
    }

    public void SyHrvSource(int value) {
        sourceIndex = PApplet.constrain(value, 0, SOURCE_LABELS.length - 1);
        if (sourceIndex == 2) {
            setNotice("数据更新已暂停");
        }
    }

    public void SyHrvTransport(int value) {
        transportIndex = PApplet.constrain(value, 0, TRANSPORT_LABELS.length - 1);
        layoutTransportControls();
        if (transportIndex == 0) {
            setNotice("USB 串口模式：填写 COM 端口后连接");
        } else if (transportIndex == 1) {
            setNotice("蓝牙 BLE 模式：自动查找名称以 simple 开头的设备");
        } else {
            setNotice("EEG WiFi 模式：跟随当前脑电 WiFi 数据流读取 AUX 健康字段");
        }
    }

    public void SyHrvTrendWindow(int value) {
        trendWindowIndex = PApplet.constrain(value, 0, TREND_WINDOW_POINTS.length - 1);
    }

    public void SyHrvAuxDebug(int value) {
        auxDebugIndex = PApplet.constrain(value, 0, AUX_DEBUG_LABELS.length - 1);
    }

    @Override
    public void update() {
        super.update();
        lockElementsOnOverlapCheck(cp5Elements);
        MAIN.textfieldUpdateHelper.checkTextfield(portTf);
        MAIN.textfieldUpdateHelper.checkTextfield(baudTf);

        applyPendingBleScan();
        applyPendingBleConnection();
        applyBleConnectionFailure();
        if (sourceIndex == 0 && transportIndex == 0) {
            serialClient.poll();
        } else if (sourceIndex == 0 && transportIndex == 2) {
            pollWifiHealthFrame();
        } else if (sourceIndex == 1) {
            updateDemoFrame();
        }
        SyHrvFrame pending = incomingFrame;
        if (pending != null && sourceIndex == 0 && transportIndex != 2) {
            incomingFrame = null;
            applyFrame(pending);
        }
        animateDisplayedValues();
    }

    @Override
    public void draw() {
        super.draw();
        MAIN.pushStyle();
        drawGradientBackground(x, y - 1, w, h + 1);

        int pad = 10;
        int panelX = x + pad;
        int panelY = y + pad;
        int panelW = w - pad * 2;
        int panelH = h - pad * 2;
        MAIN.noStroke();
        MAIN.fill(PANEL);
        MAIN.rect(panelX, panelY, panelW, panelH, 4);
        MAIN.stroke(PANEL_STROKE);
        MAIN.strokeWeight(1.1f);
        MAIN.noFill();
        MAIN.rect(panelX, panelY, panelW, panelH, 4);

        drawHeader(panelX, panelY, panelW);
        int contentY = panelY + 58;
        int contentH = panelH - 68;
        int gap = 8;
        int cardW = (panelW - 20 - gap * 3) / 4;
        int topH = Math.min(112, Math.max(86, contentH / 3));
        int cardX = panelX + 10;

        drawValueCard(cardX, contentY, cardW, topH, "血氧饱和度", valueOrDash(shownSpo2, 1), "%", spo2Color(), safeRatio(shownSpo2, 100f));
        drawValueCard(cardX + (cardW + gap), contentY, cardW, topH, "心率", valueOrDash(shownHeartRate, 1), "BPM", heartColor(), safeRatio(shownHeartRate, 160f));
        String pressure = frame == null || frame.systolicPressure < 0 || frame.diastolicPressure < 0
                ? "--"
                : PApplet.nf(frame.systolicPressure, 1, 1) + " / " + PApplet.nf(frame.diastolicPressure, 1, 1);
        drawValueCard(cardX + (cardW + gap) * 2, contentY, cardW, topH, "血压（收/舒）", pressure, "mmHg", pressureColor(), safeRatio(shownSystolic, 180f));
        drawValueCard(cardX + (cardW + gap) * 3, contentY, cardW, topH, "体温", valueOrDash(shownBodyTemperature, 1), "°C", temperatureColor(), safeRatio(shownBodyTemperature - 34f, 6f));

        int lowerY = contentY + topH + gap;
        int lowerH = contentH - topH - gap;
        int leftW = Math.max(270, (int) ((panelW - 20) * 0.60f));
        int rightW = panelW - 20 - leftW - gap;
        drawVitalTrendCard(cardX, lowerY, leftW, lowerH);

        int rightX = cardX + leftW + gap;
        int hrvH = Math.max(112, (int) (lowerH * 0.53f));
        drawHrvTrendCard(rightX, lowerY, rightW, hrvH);
        drawMetricCard(rightX, lowerY + hrvH + gap, rightW, lowerH - hrvH - gap);

        localCp5.draw();
        MAIN.popStyle();
    }

    @Override
    public void screenResized() {
        super.screenResized();
        localCp5.setGraphics(pApplet, 0, 0);
        portTf.setPosition(x0 + 6, y0 + navH + 1);
        baudTf.setPosition(x0 + 152, y0 + navH + 1);
        bleDeviceTf.setPosition(x0 + 6, y0 + navH + 1);
        scanBtn.setPosition(x0 + 224, y0 + navH + 1);
        layoutTransportControls();
    }

    @Override
    public void onFrame(SyHrvFrame decodedFrame) {
        incomingFrame = decodedFrame;
    }

    @Override
    public void onConnectionNotice(String message) {
        setNotice(message);
    }

    private void pollWifiHealthFrame() {
        DataSourcePlayback playback = currentPlaybackSource();
        if (playback != null) {
            pollPlaybackHealthFrame(playback);
            return;
        }

        WifiHealthDataSource board = currentWifiHealthBoard();
        long now = System.currentTimeMillis();
        if (board == null || !board.isUsingCustomWifiParser()) {
            if (now - lastWifiNoticeMs > 2500L) {
                setNotice("请先在控制面板启动脑电 WiFi 采集，再读取 AUX 健康字段");
                lastWifiNoticeMs = now;
            }
            return;
        }

        CytonWifiHealthFrame wifiFrame = board.getLatestWifiHealthFrame();
        if (wifiFrame == null) {
            if (now - lastWifiNoticeMs > 2500L) {
                setNotice(board.isStreaming()
                        ? "正在等待 EEG WiFi AUX 中的有效健康数据；0xFF 表示模块尚未给出有效值"
                        : "脑电 WiFi 数据流未启动");
                lastWifiNoticeMs = now;
            }
            return;
        }

        if (wifiFrame.timestampMs != lastWifiHealthTimestampMs) {
            lastWifiHealthTimestampMs = wifiFrame.timestampMs;
            applyFrame(toSyHrvFrame(wifiFrame));
        }
    }

    private void pollPlaybackHealthFrame(DataSourcePlayback playback) {
        long now = System.currentTimeMillis();
        if (!playback.hasPlaybackHealthData()) {
            if (now - lastWifiNoticeMs > 2500L) {
                setNotice("未找到与当前 EEG 回放文件对应的健康数据文件");
                lastWifiNoticeMs = now;
            }
            clearDisplayedFrame();
            return;
        }

        CytonWifiHealthFrame playbackFrame = playback.getPlaybackHealthFrameAtCurrentTime();
        if (playbackFrame == null) {
            if (now - lastWifiNoticeMs > 2500L) {
                setNotice("当前回放时间点之前还没有健康数据");
                lastWifiNoticeMs = now;
            }
            clearDisplayedFrame();
            return;
        }

        String playbackKey = playbackFrame.sequence + ":" + playbackFrame.timestampMs;
        if (!playbackKey.equals(lastPlaybackHealthKey)) {
            lastPlaybackHealthKey = playbackKey;
            applyFrame(toSyHrvFrame(playbackFrame));
        }
    }

    private SyHrvFrame toSyHrvFrame(CytonWifiHealthFrame healthFrame) {
        return new SyHrvFrame(
                healthFrame.heartRate,
                healthFrame.spo2,
                healthFrame.microcirculation,
                healthFrame.systolicPressure,
                healthFrame.diastolicPressure,
                healthFrame.respirationRate,
                healthFrame.fatigueIndex,
                healthFrame.rrIntervalRaw,
                "raw",
                healthFrame.sdnn,
                healthFrame.rmssd,
                healthFrame.bodyTemperature,
                healthFrame.ambientTemperature,
                healthFrame.timestampMs
        );
    }

    private WifiHealthDataSource currentWifiHealthBoard() {
        return MAIN.currentBoard instanceof WifiHealthDataSource
                ? (WifiHealthDataSource) MAIN.currentBoard
                : null;
    }

    private DataSourcePlayback currentPlaybackSource() {
        return MAIN.currentBoard instanceof DataSourcePlayback ? (DataSourcePlayback) MAIN.currentBoard : null;
    }

    private boolean isWifiHealthConnected() {
        DataSourcePlayback playback = currentPlaybackSource();
        if (playback != null) {
            return playback.hasPlaybackHealthData();
        }
        WifiHealthDataSource board = currentWifiHealthBoard();
        long now = System.currentTimeMillis();
        long lastAuxMs = board == null ? -1L : board.getLastWifiHealthAuxFrameTimestampMs();
        return board != null
                && board.isUsingCustomWifiParser()
                && board.isStreaming()
                && (board.getLatestWifiHealthFrame() != null || (lastAuxMs > 0L && now - lastAuxMs < 3500L));
    }

    private void toggleConnection() {
        if (transportIndex == 1) {
            toggleBleConnection();
            return;
        }
        if (transportIndex == 2) {
            setNotice("EEG WiFi 模式使用主采集系统连接，无需单独连接 SY-HRV");
            return;
        }
        if (serialClient.isConnected()) {
            serialClient.close();
            connectBtn.getCaptionLabel().setText("连接");
            connectBtn.setColorBackground(0xFF2A4A70);
            setNotice("串口已断开");
            return;
        }

        int baud = parseBaud(baudTf.getText());
        if (serialClient.connect(portTf.getText(), baud)) {
            baudTf.setText(String.valueOf(baud));
            connectBtn.getCaptionLabel().setText("断开");
            connectBtn.setColorBackground(0xFF7A3645);
            setNotice("已通过 " + TRANSPORT_LABELS[transportIndex] + " 连接，等待测量帧");
        }
    }

    private void toggleBleConnection() {
        if (bleConnectInProgress) {
            setNotice("正在连接并校验设备，请稍候…");
            return;
        }
        if (bleScanInProgress) {
            setNotice("正在搜索 SY-HRV 设备，请等待搜索完成…");
            return;
        }
        if (bleClient.isConnected()) {
            bleClient.close();
            selectedBleDevice = null;
            bleCandidates.clear();
            bleDeviceTf.setText("");
            connectBtn.getCaptionLabel().setText("连接");
            connectBtn.setColorBackground(0xFF2A4A70);
            setNotice("蓝牙 BLE 已断开；重新连接前请点击“自动查找”");
            return;
        }
        if (selectedBleDevice == null) {
            scanBleDevices();
            return;
        }
        beginBleConnection(bleCandidates.isEmpty() ? Arrays.asList(selectedBleDevice) : bleCandidates);
    }

    private void beginBleConnection(final SyHrvBleClient.Device target) {
        beginBleConnection(Arrays.asList(target));
    }

    private void beginBleConnection(final List<SyHrvBleClient.Device> candidates) {
        final SyHrvBleClient.Device target = candidates == null || candidates.isEmpty() ? null : candidates.get(0);
        if (target == null || bleConnectInProgress || bleClient.isConnected()) {
            return;
        }
        bleConnectInProgress = true;
        connectBtn.getCaptionLabel().setText("连接中");
        setNotice("已匹配 SY-HRV 服务，正在连接 " + target + "…");
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (bleClient.connect(target)) {
                    connectedBleDevice = target;
                } else {
                    bleConnectionFailed = true;
                }
                bleConnectInProgress = false;
            }
        }, "sy-hrv-ble-connect").start();
    }

    private void scanBleDevices() {
        if (transportIndex != 1 || bleScanInProgress || bleConnectInProgress) {
            return;
        }
        if (bleClient.isConnected()) {
            setNotice("模块已经连接，无需重新查找");
            return;
        }
        bleScanInProgress = true;
        scanBtn.getCaptionLabel().setText("扫描中");
        setNotice("正在主动搜索名称以 simple 开头的 BLE 设备（约 10 秒）…");
        new Thread(new Runnable() {
            @Override
            public void run() {
                pendingBleDevices = bleClient.scan(10000);
                bleScanInProgress = false;
            }
        }, "sy-hrv-ble-scan").start();
    }

    private void applyPendingBleScan() {
        if (bleScanInProgress) {
            return;
        }
        scanBtn.getCaptionLabel().setText("自动查找");
        List<SyHrvBleClient.Device> result = pendingBleDevices;
        if (result == null) {
            updateBleConnectionButton();
            return;
        }
        pendingBleDevices = null;
        if (result.isEmpty()) {
            bleCandidates.clear();
            selectedBleDevice = null;
            bleDeviceTf.setText("");
            setNotice(bleClient.getLastError().isEmpty() ? "未发现名称以 simple 开头的 BLE 设备" : bleClient.getLastError());
        } else {
            bleCandidates = new ArrayList<SyHrvBleClient.Device>(result);
            selectedBleDevice = result.get(0);
            bleDeviceTf.setText(selectedBleDevice.toString());
            setNotice("已找到设备：" + selectedBleDevice + "，请点击“连接”");
        }
        updateBleConnectionButton();
    }

    private void applyPendingBleConnection() {
        SyHrvBleClient.Device connectedDevice = connectedBleDevice;
        if (connectedDevice == null) {
            return;
        }
        connectedBleDevice = null;
        selectedBleDevice = connectedDevice;
        bleDeviceTf.setText(connectedDevice.toString());
    }

    private void applyBleConnectionFailure() {
        if (!bleConnectionFailed) {
            return;
        }
        bleConnectionFailed = false;
        selectedBleDevice = null;
        bleCandidates.clear();
        bleDeviceTf.setText("");
        String error = bleClient.getLastError();
        setNotice((error == null || error.isEmpty() ? "BLE 连接失败" : error) + "；请重新点击“自动查找”");
        updateBleConnectionButton();
    }

    private void updateBleConnectionButton() {
        if (transportIndex != 1 || bleConnectInProgress) {
            return;
        }
        if (bleClient.isConnected()) {
            connectBtn.getCaptionLabel().setText("断开");
            connectBtn.setColorBackground(0xFF7A3645);
        } else {
            connectBtn.getCaptionLabel().setText("连接");
            connectBtn.setColorBackground(0xFF2A4A70);
        }
    }

    private void layoutTransportControls() {
        boolean useBle = transportIndex == 1;
        boolean useWifi = transportIndex == 2;
        portTf.setVisible(!useBle && !useWifi);
        baudTf.setVisible(!useBle && !useWifi);
        bleDeviceTf.setVisible(useBle);
        scanBtn.setVisible(useBle);
        connectBtn.setVisible(!useWifi);
        connectBtn.setPosition(useBle ? x0 + 298 : x0 + 226, y0 + navH + 1);
        clearBtn.setPosition(useWifi ? x0 + 6 : (useBle ? x0 + 370 : x0 + 298), y0 + navH + 1);
        updateBleConnectionButton();
    }

    private int parseBaud(String value) {
        try {
            int baud = Integer.parseInt(value.trim());
            return PApplet.constrain(baud, 1200, 921600);
        } catch (Exception ignored) {
            return 9600;
        }
    }

    private void updateDemoFrame() {
        long now = MAIN.millis();
        if (now - lastDemoMs < 1000L) {
            return;
        }
        lastDemoMs = now;
        demoPhase += 0.36f;
        SyHrvFrame demo = new SyHrvFrame(
                Math.round(74f + 4f * PApplet.sin(demoPhase)),
                Math.round(98f + 0.7f * PApplet.sin(demoPhase * 0.45f)),
                Math.round(62f + 8f * PApplet.sin(demoPhase * 0.35f)),
                Math.round(116f + 5f * PApplet.sin(demoPhase * 0.23f)),
                Math.round(76f + 4f * PApplet.sin(demoPhase * 0.28f)),
                Math.round(15f + 2f * PApplet.sin(demoPhase * 0.20f)),
                Math.round(32f + 8f * PApplet.sin(demoPhase * 0.15f)),
                Math.round(810f + 55f * PApplet.sin(demoPhase)),
                Math.round(42f + 9f * PApplet.sin(demoPhase * 0.38f)),
                Math.round(35f + 8f * PApplet.sin(demoPhase * 0.30f)),
                36.6f + 0.15f * PApplet.sin(demoPhase * 0.12f),
                27.2f + 0.6f * PApplet.sin(demoPhase * 0.08f),
                System.currentTimeMillis()
        );
        applyFrame(demo);
    }

    private void applyFrame(SyHrvFrame nextFrame) {
        boolean firstFrame = frame == null;
        frame = nextFrame;
        if (firstFrame) {
            shownSpo2 = initialDisplayValue(nextFrame.spo2);
            shownHeartRate = initialDisplayValue(nextFrame.heartRate);
            shownSystolic = initialDisplayValue(nextFrame.systolicPressure);
            shownDiastolic = initialDisplayValue(nextFrame.diastolicPressure);
            shownBodyTemperature = initialDisplayValue(nextFrame.bodyTemperature);
        }
        frameTransitionMs = System.currentTimeMillis();
        push(spo2History, trendValue(nextFrame.spo2, spo2History));
        push(heartHistory, trendValue(nextFrame.heartRate, heartHistory));
        push(respirationHistory, trendValue(nextFrame.respirationRate, respirationHistory));
        push(rrHistory, trendValue(nextFrame.rrIntervalMs, rrHistory));
        push(sdnnHistory, trendValue(nextFrame.sdnn, sdnnHistory));
        push(rmssdHistory, trendValue(nextFrame.rmssd, rmssdHistory));
        historyWrite = (historyWrite + 1) % TREND_POINTS;
        if (historyWrite == 0) {
            historyFilled = true;
        }
    }

    private void animateDisplayedValues() {
        if (frame == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (lastVisualUpdateMs == 0L) {
            lastVisualUpdateMs = now;
            return;
        }
        long elapsed = Math.max(1L, now - lastVisualUpdateMs);
        lastVisualUpdateMs = now;
        float response = 1f - (float) Math.pow(0.002f, elapsed / 650f);
        shownSpo2 = smoothMetric(shownSpo2, frame.spo2, response);
        shownHeartRate = smoothMetric(shownHeartRate, frame.heartRate, response);
        shownSystolic = smoothMetric(shownSystolic, frame.systolicPressure, response);
        shownDiastolic = smoothMetric(shownDiastolic, frame.diastolicPressure, response);
        shownBodyTemperature = smoothMetric(shownBodyTemperature, frame.bodyTemperature, response);
    }

    private void clearHistory() {
        Arrays.fill(spo2History, 0f);
        Arrays.fill(heartHistory, 0f);
        Arrays.fill(respirationHistory, 0f);
        Arrays.fill(rrHistory, 0f);
        Arrays.fill(sdnnHistory, 0f);
        Arrays.fill(rmssdHistory, 0f);
        historyWrite = 0;
        historyFilled = false;
    }

    private void clearDisplayedFrame() {
        frame = null;
        shownSpo2 = Float.NaN;
        shownHeartRate = Float.NaN;
        shownSystolic = Float.NaN;
        shownDiastolic = Float.NaN;
        shownBodyTemperature = Float.NaN;
        lastVisualUpdateMs = 0L;
        frameTransitionMs = 0L;
        lastPlaybackHealthKey = "";
        clearHistory();
    }

    private void drawHeader(int x0, int y0, int w0) {
        MAIN.fill(TEXT_MAIN);
        MAIN.textFont(p7);
        MAIN.textSize(16);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text("SY-HRV 健康监测", x0 + 12, y0 + 9);
        MAIN.fill(TEXT_SUB);
        MAIN.textSize(11);
        String transportHint = transportIndex == 0 ? "USB-TTL" : (transportIndex == 1 ? "BLE GATT" : "EEG WiFi AUX");
        String protocolText = "心率 / 血氧 / 血压 / 呼吸 / HRV / 体温 · " + transportHint + " · 24 字节协议";
        boolean showNotice = sourceIndex == 0 && !connectionNotice.isEmpty()
                && System.currentTimeMillis() - connectionNoticeMs < 12000L;
        String headerDetail = auxDebugIndex == 1 ? wifiAuxDebugText() : (showNotice ? connectionNotice : protocolText);
        MAIN.text(fitTextToWidth(headerDetail, Math.max(120, w0 - 245)), x0 + 12, y0 + 32);

        boolean fresh = frame != null && (currentPlaybackSource() != null || System.currentTimeMillis() - frame.timestampMs < 3500L);
        boolean connected = transportIndex == 0 ? serialClient.isConnected()
                : (transportIndex == 1 ? bleClient.isConnected() : isWifiHealthConnected());
        boolean bleBusy = sourceIndex == 0 && transportIndex == 1 && (bleScanInProgress || bleConnectInProgress);
        int stateColor = sourceIndex == 2 ? WARN : (sourceIndex == 1 || bleBusy ? INFO : (connected && fresh ? GOOD : WARN));
        String state = sourceIndex == 2 ? "已暂停"
                : (sourceIndex == 1 ? "模拟数据"
                : (bleScanInProgress ? "正在扫描"
                : (bleConnectInProgress ? "连接校验中"
                : (connected ? (fresh ? "实时数据" : "等待数据") : "未连接"))));
        drawPill(x0 + w0 - 110, y0 + 11, 94, 22, state, stateColor);

        if (transportIndex == 1 && bleClient.isConnected()) {
            String deviceName = bleClient.getDeviceName() == null ? "" : bleClient.getDeviceName().trim();
            if (deviceName.isEmpty()) {
                deviceName = bleClient.getDeviceAddress();
            }
            String deviceLabel = fitTextToWidth("设备：" + deviceName, Math.min(220, w0 * 0.32f));
            MAIN.fill(GOOD);
            MAIN.textFont(p7);
            MAIN.textSize(10);
            MAIN.textAlign(PApplet.RIGHT, PApplet.TOP);
            MAIN.text(deviceLabel, x0 + w0 - 16, y0 + 35);
        }

        MAIN.stroke(PANEL_STROKE);
        MAIN.strokeWeight(1f);
        MAIN.line(x0 + 10, y0 + 50, x0 + w0 - 10, y0 + 50);
    }

    private String wifiAuxDebugText() {
        if (sourceIndex != 0 || transportIndex != 2) {
            return "AUX debug: select Hardware + EEG WiFi";
        }
        DataSourcePlayback playback = currentPlaybackSource();
        if (playback != null) {
            return playback.hasPlaybackHealthData()
                    ? "AUX debug: playback uses decoded health file"
                    : "AUX debug: no matching health playback file";
        }
        WifiHealthDataSource board = currentWifiHealthBoard();
        if (board == null || !board.isUsingCustomWifiParser()) {
            return "AUX debug: EEG WiFi parser is not active";
        }
        if (!board.isStreaming()) {
            return "AUX debug: EEG WiFi stream is not running";
        }
        String debugText = board.getLatestWifiHealthAuxDebugText();
        if (debugText == null || debugText.isEmpty()) {
            return "AUX debug: waiting for C1/C2/C3 frames";
        }
        long lastAuxMs = board.getLastWifiHealthAuxFrameTimestampMs();
        long ageMs = lastAuxMs > 0L ? Math.max(0L, System.currentTimeMillis() - lastAuxMs) : -1L;
        return ageMs >= 0L ? debugText + " age=" + ageMs + "ms" : debugText;
    }

    private String fitTextToWidth(String text, float maxWidth) {
        if (text == null || MAIN.textWidth(text) <= maxWidth) {
            return text;
        }
        String suffix = "…";
        int end = text.length();
        while (end > 1 && MAIN.textWidth(text.substring(0, end) + suffix) > maxWidth) {
            end--;
        }
        return text.substring(0, Math.max(1, end)) + suffix;
    }

    private void drawValueCard(int x0, int y0, int w0, int h0, String title, String value, String unit, int color, float ratio) {
        drawCard(x0, y0, w0, h0, title);
        boolean isPressure = title.startsWith("血压");
        boolean showGauge = !isPressure && !"体温".equals(title);
        float valueX = isPressure ? x0 + 5 : x0 + 12;
        MAIN.fill(color);
        MAIN.textFont(p7);
        MAIN.textSize(Math.max(18, Math.min(26, w0 / 6)));
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text(value, valueX, y0 + 34);
        MAIN.fill(TEXT_SUB);
        MAIN.textSize(10);
        MAIN.text(unit, x0 + 12, y0 + h0 - 18);
        if (showGauge) {
            drawGauge(x0 + w0 - 30, y0 + 27, 28, ratio, color);
        }
    }

    private void drawVitalTrendCard(int x0, int y0, int w0, int h0) {
        drawCard(x0, y0, w0, h0, "生命体征趋势 · " + TREND_WINDOW_LABELS[trendWindowIndex] + " · 1 Hz");
        int gx = x0 + 10;
        int gy = y0 + 32;
        int gw = w0 - 20;
        int rowH = Math.max(30, (h0 - 42) / 3);
        int count = visibleHistoryCount();
        drawMiniTrend(gx, gy, gw, rowH - 4, spo2History, count, 2f, 0xFFFF6678, "SpO2", "%");
        drawMiniTrend(gx, gy + rowH, gw, rowH - 4, heartHistory, count, 12f, INFO, "心率", " BPM");
        drawMiniTrend(gx, gy + rowH * 2, gw, rowH - 4, respirationHistory, count, 6f, GOOD, "呼吸", " 次/分");
    }

    private void drawHrvTrendCard(int x0, int y0, int w0, int h0) {
        drawCard(x0, y0, w0, h0, "HRV 趋势 · 平滑显示");
        int gx = x0 + 10;
        int gy = y0 + 32;
        int gw = w0 - 20;
        int rowH = Math.max(26, (h0 - 42) / 3);
        int count = visibleHistoryCount();
        String rrUnit = frame == null || frame.rrIntervalUnit.isEmpty() ? "" : " " + frame.rrIntervalUnit;
        drawMiniTrend(gx, gy, gw, rowH - 3, rrHistory, count, 120f, INFO, "RR", rrUnit);
        drawMiniTrend(gx, gy + rowH, gw, rowH - 3, sdnnHistory, count, 24f, 0xFFFFC45E, "SDNN", " ms");
        drawMiniTrend(gx, gy + rowH * 2, gw, rowH - 3, rmssdHistory, count, 24f, 0xFFC998FF, "RMSSD", " ms");
    }

    private void drawMetricCard(int x0, int y0, int w0, int h0) {
        drawCard(x0, y0, w0, h0, "模块数据");
        if (frame == null) {
            MAIN.fill(TEXT_SUB);
            MAIN.textFont(p7);
            MAIN.textSize(11);
            MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
            String instruction = transportIndex == 0
                    ? "填写 COM 端口并点击连接，等待模块上报。"
                    : connectionNotice;
            MAIN.text(instruction, x0 + 10, y0 + 32, w0 - 20, Math.max(30, h0 - 42));
            return;
        }
        int rowH = 19;
        int rowY = y0 + 28;
        String rrUnit = frame.rrIntervalUnit.isEmpty() ? "" : " " + frame.rrIntervalUnit;
        drawFieldRow(x0 + 10, rowY, w0 - 20, "微循环", intOrDash(frame.microcirculation, ""));
        drawFieldRow(x0 + 10, rowY + rowH, w0 - 20, "疲劳指数", intOrDash(frame.fatigueIndex, ""));
        drawFieldRow(x0 + 10, rowY + rowH * 2, w0 - 20, "RR 间期", intOrDash(frame.rrIntervalMs, rrUnit));
        drawFieldRow(x0 + 10, rowY + rowH * 3, w0 - 20, "SDNN", intOrDash(frame.sdnn, " ms"));
        drawFieldRow(x0 + 10, rowY + rowH * 4, w0 - 20, "RMSSD", intOrDash(frame.rmssd, " ms"));
        drawFieldRow(x0 + 10, rowY + rowH * 5, w0 - 20, "环境/预测温度", valueWithUnit(frame.predictedTemperature, 1, " °C"));

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7);
        MAIN.textSize(9);
        MAIN.textAlign(PApplet.LEFT, PApplet.BOTTOM);
        String transport = sourceIndex == 1 ? "模拟帧" : (transportIndex == 0
                ? TRANSPORT_LABELS[0] + " · " + serialClient.getPortName() + " / " + serialClient.getBaudRate()
                : (transportIndex == 1
                ? TRANSPORT_LABELS[1] + " · " + bleClient.getDeviceName() + " " + bleClient.getDeviceAddress()
                : TRANSPORT_LABELS[2] + " · AUX C1/C2/C3"));
        MAIN.text(transport + "  ·  " + frameTimeText(), x0 + 10, y0 + h0 - 7);
    }

    private void drawMiniTrend(int x0, int y0, int w0, int h0, float[] data, int count,
                               float minimumSpan, int color, String label, String unit) {
        int labelW = Math.min(92, Math.max(60, w0 / 4));
        int plotX = x0 + labelW;
        int plotW = Math.max(20, w0 - labelW);
        if (count < 1) {
            MAIN.fill(TEXT_SUB);
            MAIN.textFont(p7);
            MAIN.textSize(9);
            MAIN.textAlign(PApplet.LEFT, PApplet.CENTER);
            MAIN.text(label + " --", x0 + 2, y0 + h0 * 0.5f);
            return;
        }

        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < count; i++) {
            float value = historyValue(data, i, count);
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        float span = Math.max(minimumSpan, max - min);
        min -= span * 0.2f;
        max += span * 0.2f;

        float current = historyValue(data, count - 1, count);
        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7);
        MAIN.textSize(9);
        MAIN.textAlign(PApplet.LEFT, PApplet.CENTER);
        MAIN.text(label + " " + PApplet.nf(current, 1, 0) + unit, x0 + 2, y0 + h0 * 0.5f);
        MAIN.stroke(45, 70, 105, 170);
        MAIN.strokeWeight(1f);
        MAIN.line(plotX, y0 + h0, plotX + plotW, y0 + h0);
        MAIN.stroke(45, 70, 105, 95);
        for (int i = 1; i < 4; i++) {
            float xx = plotX + plotW * i / 4f;
            MAIN.line(xx, y0 + 2, xx, y0 + h0);
        }

        MAIN.noStroke();
        MAIN.fill((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 34);
        MAIN.beginShape();
        MAIN.vertex(plotX, y0 + h0);
        for (int i = 0; i < count; i++) {
            float px = plotX + plotW * i / (float) Math.max(1, count - 1);
            float py = y0 + h0 - h0 * PApplet.constrain((historyValue(data, i, count) - min) / Math.max(1e-6f, max - min), 0f, 1f);
            MAIN.vertex(px, py);
        }
        MAIN.vertex(plotX + plotW, y0 + h0);
        MAIN.endShape(PApplet.CLOSE);

        MAIN.noFill();
        MAIN.stroke((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 42);
        MAIN.strokeWeight(5f);
        MAIN.beginShape();
        for (int i = 0; i < count; i++) {
            float px = plotX + plotW * i / (float) Math.max(1, count - 1);
            float py = y0 + h0 - h0 * PApplet.constrain((historyValue(data, i, count) - min) / Math.max(1e-6f, max - min), 0f, 1f);
            if (i == 0 || i == count - 1) {
                MAIN.curveVertex(px, py);
            }
            MAIN.curveVertex(px, py);
        }
        MAIN.endShape();

        MAIN.noFill();
        MAIN.stroke((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 230);
        MAIN.strokeWeight(1.9f);
        MAIN.beginShape();
        for (int i = 0; i < count; i++) {
            float px = plotX + plotW * i / (float) Math.max(1, count - 1);
            float py = y0 + h0 - h0 * PApplet.constrain((historyValue(data, i, count) - min) / Math.max(1e-6f, max - min), 0f, 1f);
            if (i == 0 || i == count - 1) {
                MAIN.curveVertex(px, py);
            }
            MAIN.curveVertex(px, py);
        }
        MAIN.endShape();

        float latestX = plotX + plotW;
        float latestY = y0 + h0 - h0 * PApplet.constrain((current - min) / Math.max(1e-6f, max - min), 0f, 1f);
        float pulse = 0.65f + 0.35f * PApplet.sin(MAIN.frameCount * 0.12f);
        MAIN.noStroke();
        MAIN.fill((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (int) (52 * pulse));
        MAIN.circle(latestX, latestY, 14 + 5 * pulse);
        MAIN.fill(color);
        MAIN.circle(latestX, latestY, 5);
    }

    private void drawFieldRow(int x0, int y0, int w0, String key, String value) {
        MAIN.noStroke();
        MAIN.fill(0x20183357);
        MAIN.rect(x0, y0, w0, 18, 3);
        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7);
        MAIN.textSize(9);
        MAIN.textAlign(PApplet.LEFT, PApplet.CENTER);
        MAIN.text(key, x0 + 6, y0 + 9);
        MAIN.fill(TEXT_MAIN);
        MAIN.textAlign(PApplet.RIGHT, PApplet.CENTER);
        MAIN.text(value, x0 + w0 - 6, y0 + 9);
    }

    private void drawGauge(float cx, float cy, float diameter, float ratio, int color) {
        float start = -PApplet.HALF_PI;
        float sweep = PApplet.TWO_PI * PApplet.constrain(ratio, 0f, 1f);
        MAIN.noFill();
        MAIN.stroke(56, 79, 112, 180);
        MAIN.strokeWeight(4f);
        MAIN.arc(cx, cy, diameter, diameter, start, start + PApplet.TWO_PI);
        MAIN.stroke((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 235);
        MAIN.strokeWeight(4f);
        MAIN.arc(cx, cy, diameter, diameter, start, start + sweep);
        float angle = start + sweep;
        float r = diameter * 0.5f;
        MAIN.noStroke();
        MAIN.fill(color);
        MAIN.circle(cx + PApplet.cos(angle) * r, cy + PApplet.sin(angle) * r, 5);
    }

    private void drawCard(int x0, int y0, int w0, int h0, String title) {
        MAIN.noStroke();
        MAIN.fill(CARD);
        MAIN.rect(x0, y0, w0, h0, 5);
        MAIN.stroke(PANEL_STROKE);
        MAIN.strokeWeight(1f);
        MAIN.noFill();
        MAIN.rect(x0, y0, w0, h0, 5);
        MAIN.fill(TEXT_MAIN);
        MAIN.textFont(p7);
        MAIN.textSize(12);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text(title, x0 + 10, y0 + 9);
    }

    private void drawPill(int x0, int y0, int w0, int h0, String text, int color) {
        MAIN.noStroke();
        MAIN.fill((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 42);
        MAIN.rect(x0, y0, w0, h0, h0 / 2);
        MAIN.stroke((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 190);
        MAIN.noFill();
        MAIN.rect(x0, y0, w0, h0, h0 / 2);
        MAIN.fill(color);
        MAIN.textFont(p7);
        MAIN.textSize(10);
        MAIN.textAlign(PApplet.CENTER, PApplet.CENTER);
        MAIN.text(text, x0 + w0 * 0.5f, y0 + h0 * 0.5f);
    }

    private int historyCount() {
        return historyFilled ? TREND_POINTS : historyWrite;
    }

    private int visibleHistoryCount() {
        return Math.min(historyCount(), TREND_WINDOW_POINTS[trendWindowIndex]);
    }

    private int historyIndex(int position) {
        return historyFilled ? (historyWrite + position) % TREND_POINTS : position;
    }

    private int visibleHistoryIndex(int position, int visibleCount) {
        return historyIndex(historyCount() - visibleCount + position);
    }

    private float historyValue(float[] values, int position, int visibleCount) {
        float value = values[visibleHistoryIndex(position, visibleCount)];
        if (position != visibleCount - 1 || visibleCount < 2 || frameTransitionMs <= 0L) {
            return value;
        }
        float progress = PApplet.constrain((System.currentTimeMillis() - frameTransitionMs) / 560f, 0f, 1f);
        progress = progress * progress * (3f - 2f * progress);
        float previous = values[visibleHistoryIndex(position - 1, visibleCount)];
        return PApplet.lerp(previous, value, progress);
    }

    private void push(float[] values, float value) {
        values[historyWrite] = value;
    }

    private float initialDisplayValue(float value) {
        return isValidMetric(value) ? value : Float.NaN;
    }

    private float smoothMetric(float current, float target, float response) {
        if (!isValidMetric(target)) {
            return current;
        }
        if (!isValidMetric(current)) {
            return target;
        }
        return PApplet.lerp(current, target, response);
    }

    private float trendValue(float value, float[] history) {
        if (isValidMetric(value)) {
            return value;
        }
        int count = historyCount();
        if (count <= 0) {
            return 0f;
        }
        int previousIndex = historyFilled
                ? (historyWrite - 1 + TREND_POINTS) % TREND_POINTS
                : Math.max(0, historyWrite - 1);
        return history[previousIndex];
    }

    private boolean isValidMetric(float value) {
        return !Float.isNaN(value) && value >= 0f;
    }

    private String valueOrDash(float value, int decimals) {
        return Float.isNaN(value) || value < 0f ? "--" : PApplet.nf(value, 1, decimals);
    }

    private String valueWithUnit(float value, int decimals, String unit) {
        return isValidMetric(value) ? PApplet.nf(value, 1, decimals) + unit : "--";
    }

    private String intOrDash(int value, String unit) {
        return value >= 0 ? value + unit : "--";
    }

    private float safeRatio(float numerator, float denominator) {
        if (Float.isNaN(numerator) || denominator <= 0f) {
            return 0f;
        }
        return PApplet.constrain(numerator / denominator, 0f, 1f);
    }

    private int spo2Color() {
        if (frame == null || frame.spo2 <= 0) return TEXT_SUB;
        if (frame.spo2 >= 95) return GOOD;
        if (frame.spo2 >= 90) return WARN;
        return BAD;
    }

    private int heartColor() {
        if (frame == null || frame.heartRate <= 0) return TEXT_SUB;
        return frame.heartRate >= 50 && frame.heartRate <= 110 ? INFO : WARN;
    }

    private int pressureColor() {
        if (frame == null || frame.systolicPressure <= 0) return TEXT_SUB;
        boolean typical = frame.systolicPressure >= 90 && frame.systolicPressure <= 140
                && frame.diastolicPressure >= 60 && frame.diastolicPressure <= 90;
        return typical ? GOOD : WARN;
    }

    private int temperatureColor() {
        if (frame == null || frame.bodyTemperature <= 0f) return TEXT_SUB;
        return frame.bodyTemperature >= 35f && frame.bodyTemperature <= 37.5f ? GOOD : WARN;
    }

    private String ageText(long timestampMs) {
        long seconds = Math.max(0L, (System.currentTimeMillis() - timestampMs) / 1000L);
        return seconds == 0L ? "刚更新" : seconds + " 秒前";
    }

    private String frameTimeText() {
        DataSourcePlayback playback = currentPlaybackSource();
        if (playback != null) {
            return "回放 " + PApplet.nf(playback.getCurrentTimeSeconds(), 1, 1) + " s";
        }
        return ageText(frame.timestampMs);
    }

    private void setNotice(String message) {
        connectionNotice = message == null ? "" : message;
        connectionNoticeMs = System.currentTimeMillis();
    }

    private void drawGradientBackground(int x0, int y0, int w0, int h0) {
        for (int i = 0; i < h0; i++) {
            float t = i / (float) Math.max(1, h0 - 1);
            int c = lerpRgb(BG_TOP, BG_BOTTOM, t);
            MAIN.stroke((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF);
            MAIN.line(x0, y0 + i, x0 + w0, y0 + i);
        }
        float glow = 0.5f + 0.5f * PApplet.sin(MAIN.frameCount * 0.012f);
        MAIN.noStroke();
        MAIN.fill(86, 202, 255, (int) (14 * glow));
        MAIN.ellipse(x0 + w0 * 0.18f, y0 + h0 * 0.24f, w0 * 0.36f, h0 * 0.30f);
        MAIN.fill(255, 104, 120, (int) (11 * glow));
        MAIN.ellipse(x0 + w0 * 0.78f, y0 + h0 * 0.72f, w0 * 0.30f, h0 * 0.27f);
    }

    private static int lerpRgb(int c1, int c2, float t) {
        int r = (int) PApplet.lerp((c1 >> 16) & 0xFF, (c2 >> 16) & 0xFF, t);
        int g = (int) PApplet.lerp((c1 >> 8) & 0xFF, (c2 >> 8) & 0xFF, t);
        int b = (int) PApplet.lerp(c1 & 0xFF, c2 & 0xFF, t);
        return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }
}
