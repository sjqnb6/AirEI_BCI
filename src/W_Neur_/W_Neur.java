package W_Neur_;

import Widget_.Widget;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;
import processing.core.PApplet;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static Globel.GUI.*;
import Globel.GUI;

public class W_Neur extends Widget {
    private static final String WS_URL = "ws://127.0.0.1:8000/ws/eeg_realtime";
    private static final String[] MODE_LABELS = {"仅显示", "上传+显示"};
    private static final String[] HISTORY_LABELS = {"60秒", "90秒", "120秒"};
    private static final int[] HISTORY_SECONDS = {60, 90, 120};

    private static final int BG_TOP = 0xFF0A1220;
    private static final int BG_BOTTOM = 0xFF0D1830;
    private static final int PANEL = 0xCC13243F;
    private static final int PANEL_STROKE = 0x6683A2CC;
    private static final int CARD_BG = 0x38172B48;
    private static final int CARD_STROKE = 0x58A3C4ED;
    private static final int TEXT_MAIN = 0xFFEAF2FF;
    private static final int TEXT_SUB = 0xFF95ADD1;
    private static final int TEXT_WARN = 0xFFFFC28A;
    private static final int GRID = 0x2F8CB1DB;

    private static final int MAX_TRAIL_POINTS = 8000;
    private static final int MAX_RENDER_MSG_PER_FRAME = 300;
    private static final int RAW_FS_REQUIRED = 250;
    private static final int RAW_CHUNK_LEN = 25; // 250Hz * 0.1s
    private static final String[] RAW_CHANNELS = {
            "Fp1", "Fp2", "C3", "C4", "P3", "P4", "O1", "O2"
    };

    private final GUI MAIN;

    private volatile WebSocketClient ws;
    private volatile boolean wsConnected = false;
    private volatile boolean wantWs = true;
    private volatile String wsStatusText = "未连接";

    private final String sessionId = "S001";
    private long sendSeq = 0;
    private double lastSendTs = 0;

    private boolean sessionRunning = true;
    private boolean stopRequested = false;
    private long lastConnectAttemptMs = 0;
    private long lastServerMsgLocalMs = 0;
    private long lastServerSeq = 0;
    private String modelVersion = "-";
    private int parseErrorCount = 0;

    private int modeIndex = 1;
    private int historyIndex = 1;
    private boolean enableRawUpload = true;

    private ScheduledExecutorService rawTicker;
    private final AtomicBoolean rawTick = new AtomicBoolean(false);

    private int rawFs = RAW_FS_REQUIRED;
    private int rawChunkLen = RAW_CHUNK_LEN;
    private int rawFilled = 0;
    private float[][] rawAgg = new float[8][RAW_CHUNK_LEN];
    private final Deque<RawChunk> rawChunkQueue = new ArrayDeque<RawChunk>();

    private final ConcurrentLinkedQueue<InboundMsg> inboundQueue = new ConcurrentLinkedQueue<InboundMsg>();
    private final Deque<Long> recvTimeMs = new ArrayDeque<Long>();

    private final HashMap<Integer, PointState> pointMap = new HashMap<Integer, PointState>();
    private final Deque<TrailPoint> trail = new ArrayDeque<TrailPoint>();
    private final HashMap<Integer, ContourState> contourMap = new HashMap<Integer, ContourState>();

    private int currentClusterId = -1;
    private float currentConfidence = 0f;
    private float currentSignalQuality = 0f;
    private int activeClusters = 0;
    private float paramsNll = 0f;
    private float paramsAcceptRate = 0f;
    private float hyperAlpha = 0f;
    private float hyperBetainv = 0f;
    private float hyperGamma = 0f;
    private float[] latestProb = new float[0];
    private float[] clusterWeights = new float[0];

    private float viewMinX = -2f;
    private float viewMaxX = 2f;
    private float viewMinY = -2f;
    private float viewMaxY = 2f;

    private boolean prevPDown = false;
    private boolean prevTDown = false;

    private long sentControlCount = 0;
    private long sentRawCount = 0;
    private long recvPointCount = 0;
    private long recvPatchCount = 0;
    private long recvSnapshotCount = 0;
    private long recvParamsCount = 0;
    private String lastServerError = "";

    public W_Neur(GUI MAIN) {
        super(MAIN);
        this.MAIN = MAIN;

        addDropdown("NeurMode", "传输模式", Arrays.asList(MODE_LABELS), modeIndex);
        addDropdown("NeurHistory", "历史窗口", Arrays.asList(HISTORY_LABELS), historyIndex);

        connectWs();
        startRawTicker();

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                gracefulStopAndClose();
            }
        }));
    }

    public void NeurMode(int n) {
        modeIndex = PApplet.constrain(n, 0, MODE_LABELS.length - 1);
        enableRawUpload = modeIndex == 1;
    }

    public void NeurHistory(int n) {
        historyIndex = PApplet.constrain(n, 0, HISTORY_SECONDS.length - 1);
    }

    @Override
    public void update() {
        super.update();

        pollHotkeys();
        ensureWsLifecycle();

        if (enableRawUpload && sessionRunning && wsConnected) {
            collectRawSamplesFromBoard();
        }

        if (rawTick.getAndSet(false) && enableRawUpload && sessionRunning && wsConnected) {
            sendOneRawChunkIfAvailable();
        }
    }

    @Override
    public void draw() {
        super.draw();

        consumeInboundQueue();
        trimHistory();
        updateViewBounds();

        MAIN.pushStyle();
        drawGradientBackground(x, y, w, h);

        int pad = 10;
        int panelX = x + pad;
        int panelY = y + pad;
        int panelW = w - 2 * pad;
        int panelH = h - 2 * pad;

        MAIN.noStroke();
        MAIN.fill(PANEL);
        MAIN.rect(panelX, panelY, panelW, panelH, 6);
        MAIN.noFill();
        MAIN.stroke(PANEL_STROKE);
        MAIN.strokeWeight(1.1f);
        MAIN.rect(panelX, panelY, panelW, panelH, 6);

        int headerH = 58;
        int timelineH = 62;
        int gap = 10;

        int contentX = panelX + 10;
        int contentY = panelY + headerH;
        int contentW = panelW - 20;
        int contentH = panelH - headerH - timelineH - 16;

        int leftW = (int) (contentW * 0.68f);
        int rightW = contentW - leftW - gap;

        drawHeader(panelX, panelY, panelW, headerH);
        drawMainPlot(contentX, contentY, leftW, contentH);
        drawRightPanel(contentX + leftW + gap, contentY, rightW, contentH);
        drawTimeline(contentX, panelY + panelH - timelineH - 8, contentW, timelineH);

        MAIN.popStyle();
    }

    private void ensureWsLifecycle() {
        if (!wantWs || stopRequested) {
            return;
        }
        long now = MAIN.millis();
        if (!wsConnected && now - lastConnectAttemptMs > 1800) {
            connectWs();
        }
    }

    private void connectWs() {
        lastConnectAttemptMs = MAIN.millis();
        try {
            ws = new WebSocketClient(new URI(WS_URL)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    wsConnected = true;
                    wsStatusText = "已连接";
                    if (!stopRequested) {
                        sendControl("start");
                    }
                }

                @Override
                public void onMessage(String message) {
                    try {
                        JSONObject obj = new JSONObject(message);
                        inboundQueue.offer(new InboundMsg(obj, System.currentTimeMillis()));
                    } catch (Exception ex) {
                        parseErrorCount++;
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    wsConnected = false;
                    wsStatusText = "已断开";
                }

                @Override
                public void onError(Exception ex) {
                    wsConnected = false;
                    wsStatusText = "错误";
                }
            };
            ws.connect();
            wsStatusText = "连接中";
        } catch (Exception e) {
            wsConnected = false;
            wsStatusText = "连接失败";
        }
    }

    private void consumeInboundQueue() {
        int processed = 0;
        InboundMsg msg;
        while ((msg = inboundQueue.poll()) != null && processed < MAX_RENDER_MSG_PER_FRAME) {
            processed++;
            JSONObject root = msg.root;
            String type = root.optString("type", "");
            if (type.length() == 0) {
                continue;
            }

            lastServerMsgLocalMs = msg.recvLocalMs;
            lastServerSeq = root.optLong("seq", lastServerSeq);
            modelVersion = root.optString("model_version", modelVersion);
            recvTimeMs.addLast(msg.recvLocalMs);

            if ("brain.point".equals(type)) {
                recvPointCount++;
                handleBrainPoint(root);
            } else if ("brain.patch".equals(type)) {
                recvPatchCount++;
                handleBrainPatch(root);
            } else if ("brain.snapshot".equals(type)) {
                recvSnapshotCount++;
                handleBrainSnapshot(root);
            } else if ("brain.params".equals(type)) {
                recvParamsCount++;
                handleBrainParams(root);
            } else if ("server.error".equals(type)) {
                JSONObject payload = root.optJSONObject("payload");
                if (payload != null) {
                    lastServerError = payload.optString("message", payload.toString());
                } else {
                    lastServerError = root.toString();
                }
            }
        }

        long now = System.currentTimeMillis();
        while (!recvTimeMs.isEmpty() && now - recvTimeMs.peekFirst() > 1000) {
            recvTimeMs.pollFirst();
        }
    }

    private void handleBrainPoint(JSONObject root) {
        JSONObject payload = root.optJSONObject("payload");
        if (payload == null) {
            return;
        }

        int windowId = payload.optInt("window_id", -1);
        if (windowId < 0) {
            return;
        }

        JSONArray latentArr = payload.optJSONArray("latent");
        if (latentArr == null || latentArr.length() < 2) {
            return;
        }

        float lx = (float) latentArr.optDouble(0, 0.0);
        float ly = (float) latentArr.optDouble(1, 0.0);

        PointState p = pointMap.get(windowId);
        if (p == null) {
            p = new PointState();
            p.windowId = windowId;
            pointMap.put(windowId, p);
        }

        p.latentX = lx;
        p.latentY = ly;
        p.clusterId = payload.optInt("cluster_id", p.clusterId);
        p.confidence = (float) payload.optDouble("confidence", p.confidence);
        p.signalQuality = (float) payload.optDouble("signal_quality", p.signalQuality);
        p.ts = root.optDouble("ts", nowSec());

        JSONArray probArr = payload.optJSONArray("cluster_prob");
        if (probArr != null) {
            p.clusterProb = jsonArrayToFloat(probArr);
            latestProb = p.clusterProb;
        }

        trail.addLast(new TrailPoint(windowId, p.latentX, p.latentY, p.clusterId, p.confidence, p.ts));
        if (trail.size() > MAX_TRAIL_POINTS) {
            trail.pollFirst();
        }

        currentClusterId = p.clusterId;
        currentConfidence = p.confidence;
        currentSignalQuality = p.signalQuality;
    }

    private void handleBrainPatch(JSONObject root) {
        JSONObject payload = root.optJSONObject("payload");
        if (payload == null) {
            return;
        }
        JSONArray updates = payload.optJSONArray("updates");
        if (updates == null) {
            return;
        }

        for (int i = 0; i < updates.length(); i++) {
            JSONObject up = updates.optJSONObject(i);
            if (up == null) {
                continue;
            }
            int windowId = up.optInt("window_id", -1);
            if (windowId < 0) {
                continue;
            }
            PointState p = pointMap.get(windowId);
            if (p == null) {
                continue;
            }
            JSONArray latentArr = up.optJSONArray("latent");
            if (latentArr != null && latentArr.length() >= 2) {
                p.latentX = (float) latentArr.optDouble(0, p.latentX);
                p.latentY = (float) latentArr.optDouble(1, p.latentY);
            }
            p.clusterId = up.optInt("cluster_id", p.clusterId);
        }
    }

    private void handleBrainSnapshot(JSONObject root) {
        JSONObject payload = root.optJSONObject("payload");
        if (payload == null) {
            return;
        }
        activeClusters = payload.optInt("active_clusters", activeClusters);

        JSONArray contours = payload.optJSONArray("contours");
        if (contours == null) {
            return;
        }

        long now = MAIN.millis();
        for (int i = 0; i < contours.length(); i++) {
            JSONObject c = contours.optJSONObject(i);
            if (c == null) {
                continue;
            }
            int cid = c.optInt("cluster_id", -1);
            JSONArray poly = c.optJSONArray("polygon");
            if (cid < 0 || poly == null || poly.length() < 2) {
                continue;
            }

            float[][] target = parsePolygon(poly);
            if (target == null || target.length < 2) {
                continue;
            }

            ContourState state = contourMap.get(cid);
            if (state == null) {
                state = new ContourState();
                state.clusterId = cid;
                state.current = target;
                state.target = target;
                state.tweenStartMs = now;
                state.tweenDurationMs = 500;
                contourMap.put(cid, state);
            } else {
                state.current = getInterpolatedPolygon(state, now);
                state.target = target;
                state.tweenStartMs = now;
                state.tweenDurationMs = 500;
            }
        }
    }

    private void handleBrainParams(JSONObject root) {
        JSONObject payload = root.optJSONObject("payload");
        if (payload == null) {
            return;
        }

        activeClusters = payload.optInt("active_clusters", activeClusters);
        paramsAcceptRate = (float) payload.optDouble("acceptance_rate", paramsAcceptRate);
        paramsNll = (float) payload.optDouble("nll", paramsNll);

        JSONObject hypers = payload.optJSONObject("hypers");
        if (hypers != null) {
            hyperAlpha = (float) hypers.optDouble("alpha", hyperAlpha);
            hyperBetainv = (float) hypers.optDouble("betainv", hyperBetainv);
            hyperGamma = (float) hypers.optDouble("gamma", hyperGamma);
        }

        JSONArray weightsArr = payload.optJSONArray("cluster_weights");
        if (weightsArr != null) {
            clusterWeights = jsonArrayToFloat(weightsArr);
        }
    }

    private void trimHistory() {
        double minTs = nowSec() - HISTORY_SECONDS[historyIndex];

        while (!trail.isEmpty() && trail.peekFirst().ts < minTs) {
            trail.pollFirst();
        }

        Iterator<Map.Entry<Integer, PointState>> it = pointMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, PointState> en = it.next();
            if (en.getValue().ts < minTs) {
                it.remove();
            }
        }
    }

    private void updateViewBounds() {
        if (trail.isEmpty()) {
            return;
        }

        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        for (TrailPoint t : trail) {
            if (t.x < minX) minX = t.x;
            if (t.x > maxX) maxX = t.x;
            if (t.y < minY) minY = t.y;
            if (t.y > maxY) maxY = t.y;
        }

        float padX = Math.max(0.2f, (maxX - minX) * 0.15f);
        float padY = Math.max(0.2f, (maxY - minY) * 0.15f);
        float tgtMinX = minX - padX;
        float tgtMaxX = maxX + padX;
        float tgtMinY = minY - padY;
        float tgtMaxY = maxY + padY;

        viewMinX += (tgtMinX - viewMinX) * 0.08f;
        viewMaxX += (tgtMaxX - viewMaxX) * 0.08f;
        viewMinY += (tgtMinY - viewMinY) * 0.08f;
        viewMaxY += (tgtMaxY - viewMaxY) * 0.08f;

        if (Math.abs(viewMaxX - viewMinX) < 0.01f) {
            viewMaxX = viewMinX + 0.01f;
        }
        if (Math.abs(viewMaxY - viewMinY) < 0.01f) {
            viewMaxY = viewMinY + 0.01f;
        }
    }

    private void drawHeader(int panelX, int panelY, int panelW, int headerH) {
        MAIN.fill(TEXT_MAIN);
        MAIN.textFont(p7, 14);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text("神经聚类中枢", panelX + 12, panelY + 8);

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7, 11);
        int hz = recvTimeMs.size();
        String line1 = "WS:" + (wsConnected ? "在线" : "离线")
                + "  |  session:" + sessionId
                + "  |  model:" + modelVersion
                + "  |  接收频率:" + hz + "Hz"
                + "  |  模式:" + MODE_LABELS[modeIndex];
        MAIN.text(line1, panelX + 12, panelY + 26);

        String line2 = "发送 control:" + sentControlCount + "  eeg.raw:" + sentRawCount
                + "  |  接收 point:" + recvPointCount
                + " patch:" + recvPatchCount
                + " snapshot:" + recvSnapshotCount
                + " params:" + recvParamsCount;
        MAIN.text(line2, panelX + 12, panelY + 40);

        if (parseErrorCount > 0) {
            MAIN.fill(TEXT_WARN);
            MAIN.textAlign(PApplet.RIGHT, PApplet.TOP);
            MAIN.text("解析错误:" + parseErrorCount, panelX + panelW - 12, panelY + 8);
        }

        if (lastServerError != null && lastServerError.length() > 0) {
            MAIN.fill(TEXT_WARN);
            MAIN.textAlign(PApplet.RIGHT, PApplet.TOP);
            String msg = lastServerError.length() > 50 ? lastServerError.substring(0, 50) + "..." : lastServerError;
            MAIN.text("后端错误:" + msg, panelX + panelW - 12, panelY + 24);
        }

        MAIN.stroke(0x3A8AB5DF);
        MAIN.strokeWeight(1f);
        MAIN.line(panelX + 8, panelY + headerH - 1, panelX + panelW - 8, panelY + headerH - 1);
    }

    private void drawMainPlot(int x0, int y0, int w0, int h0) {
        MAIN.noStroke();
        MAIN.fill(CARD_BG);
        MAIN.rect(x0, y0, w0, h0, 6);
        MAIN.noFill();
        MAIN.stroke(CARD_STROKE);
        MAIN.strokeWeight(1f);
        MAIN.rect(x0, y0, w0, h0, 6);

        int pad = 12;
        int gx = x0 + pad;
        int gy = y0 + 26;
        int gw = w0 - pad * 2;
        int gh = h0 - 36;

        drawGrid(gx, gy, gw, gh);
        drawContours(gx, gy, gw, gh);
        drawTrail(gx, gy, gw, gh);
        drawCurrentPoint(gx, gy, gw, gh);

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7, 11);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text("潜空间投影", x0 + 10, y0 + 8);
    }

    private void drawGrid(int x0, int y0, int w0, int h0) {
        MAIN.stroke(GRID);
        MAIN.strokeWeight(1f);
        for (int i = 0; i <= 8; i++) {
            float t = i / 8f;
            float xx = x0 + w0 * t;
            float yy = y0 + h0 * t;
            MAIN.line(xx, y0, xx, y0 + h0);
            MAIN.line(x0, yy, x0 + w0, yy);
        }
        MAIN.stroke(0x66B8D9FF);
        MAIN.line(x0, y0 + h0 / 2f, x0 + w0, y0 + h0 / 2f);
        MAIN.line(x0 + w0 / 2f, y0, x0 + w0 / 2f, y0 + h0);
    }

    private void drawContours(int x0, int y0, int w0, int h0) {
        long now = MAIN.millis();
        for (ContourState cs : contourMap.values()) {
            float[][] poly = getInterpolatedPolygon(cs, now);
            if (poly == null || poly.length < 2) {
                continue;
            }
            int c = clusterColor(cs.clusterId);
            MAIN.noStroke();
            MAIN.fill(c, 52);
            MAIN.beginShape();
            for (int i = 0; i < poly.length; i++) {
                MAIN.vertex(mapLatentX(poly[i][0], x0, w0), mapLatentY(poly[i][1], y0, h0));
            }
            MAIN.endShape(PApplet.CLOSE);

            MAIN.noFill();
            MAIN.stroke(c, 180);
            MAIN.strokeWeight(1.2f);
            MAIN.beginShape();
            for (int i = 0; i < poly.length; i++) {
                MAIN.vertex(mapLatentX(poly[i][0], x0, w0), mapLatentY(poly[i][1], y0, h0));
            }
            MAIN.endShape(PApplet.CLOSE);
        }
    }

    private void drawTrail(int x0, int y0, int w0, int h0) {
        if (trail.size() < 2) {
            return;
        }
        double now = nowSec();
        double historySec = HISTORY_SECONDS[historyIndex];

        TrailPoint prev = null;
        for (TrailPoint t : trail) {
            float age = (float) Math.max(0.0, now - t.ts);
            float fade = 1f - PApplet.constrain(age / (float) historySec, 0f, 1f);
            int c = clusterColor(t.clusterId);

            float px = mapLatentX(t.x, x0, w0);
            float py = mapLatentY(t.y, y0, h0);

            if (prev != null) {
                float ppx = mapLatentX(prev.x, x0, w0);
                float ppy = mapLatentY(prev.y, y0, h0);
                MAIN.stroke(c, (int) (120 * fade));
                MAIN.strokeWeight(1f);
                MAIN.line(ppx, ppy, px, py);
            }

            MAIN.noStroke();
            MAIN.fill(c, (int) (180 * fade));
            MAIN.ellipse(px, py, 3.8f, 3.8f);
            prev = t;
        }
    }

    private void drawCurrentPoint(int x0, int y0, int w0, int h0) {
        if (trail.isEmpty()) {
            return;
        }
        TrailPoint cur = trail.peekLast();
        float px = mapLatentX(cur.x, x0, w0);
        float py = mapLatentY(cur.y, y0, h0);
        int c = clusterColor(cur.clusterId);

        float pulse = 0.5f + 0.5f * PApplet.sin(MAIN.millis() * 0.008f);
        float r = 7f + 3f * pulse;

        MAIN.noFill();
        MAIN.stroke(0xCCF3FCFF);
        MAIN.strokeWeight(1.6f);
        MAIN.ellipse(px, py, r * 2f, r * 2f);

        MAIN.noStroke();
        MAIN.fill(c);
        MAIN.ellipse(px, py, 6f, 6f);
    }

    private void drawRightPanel(int x0, int y0, int w0, int h0) {
        MAIN.noStroke();
        MAIN.fill(CARD_BG);
        MAIN.rect(x0, y0, w0, h0, 6);
        MAIN.noFill();
        MAIN.stroke(CARD_STROKE);
        MAIN.strokeWeight(1f);
        MAIN.rect(x0, y0, w0, h0, 6);

        MAIN.fill(TEXT_MAIN);
        MAIN.textFont(p7, 12);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text("参数面板", x0 + 10, y0 + 8);

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7, 11);
        int yy = y0 + 30;
        int lineH = 17;

        MAIN.text("当前簇: " + currentClusterId, x0 + 10, yy); yy += lineH;
        MAIN.text("置信度: " + MAIN.nf(currentConfidence, 0, 3), x0 + 10, yy); yy += lineH;
        MAIN.text("信号质量: " + MAIN.nf(currentSignalQuality, 0, 3), x0 + 10, yy); yy += lineH + 4;
        MAIN.text("活跃簇数: " + activeClusters, x0 + 10, yy); yy += lineH;
        MAIN.text("NLL: " + MAIN.nf(paramsNll, 0, 3), x0 + 10, yy); yy += lineH;
        MAIN.text("接受率: " + MAIN.nf(paramsAcceptRate, 0, 3), x0 + 10, yy); yy += lineH + 4;
        MAIN.text("alpha: " + MAIN.nf(hyperAlpha, 0, 3), x0 + 10, yy); yy += lineH;
        MAIN.text("betainv: " + MAIN.nf(hyperBetainv, 0, 4), x0 + 10, yy); yy += lineH;
        MAIN.text("gamma: " + MAIN.nf(hyperGamma, 0, 3), x0 + 10, yy); yy += lineH + 8;

        float[] bars = clusterWeights.length > 0 ? clusterWeights : latestProb;
        drawBars(x0 + 10, yy, w0 - 20, h0 - (yy - y0) - 10, bars);
    }

    private void drawBars(int x0, int y0, int w0, int h0, float[] values) {
        if (values == null || values.length == 0 || h0 < 30) {
            MAIN.fill(TEXT_SUB);
            MAIN.textFont(p7, 10);
            MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
            MAIN.text("暂无簇概率/权重", x0, y0);
            return;
        }

        int n = values.length;
        float gap = 6f;
        float bw = (w0 - gap * (n - 1)) / n;
        bw = PApplet.max(8f, bw);

        for (int i = 0; i < n; i++) {
            float v = PApplet.constrain(values[i], 0f, 1f);
            float bh = v * (h0 - 16);
            float px = x0 + i * (bw + gap);
            float py = y0 + (h0 - bh);

            MAIN.noStroke();
            MAIN.fill(0x2A4A6B95);
            MAIN.rect(px, y0 + 2, bw, h0 - 2, 2);

            int c = clusterColor(i);
            MAIN.fill(c, 220);
            MAIN.rect(px, py, bw, bh, 2);

            MAIN.fill(TEXT_SUB);
            MAIN.textFont(p7, 10);
            MAIN.textAlign(PApplet.CENTER, PApplet.BOTTOM);
            MAIN.text("C" + i, px + bw * 0.5f, y0 - 1);
        }
    }

    private void drawTimeline(int x0, int y0, int w0, int h0) {
        MAIN.noStroke();
        MAIN.fill(CARD_BG);
        MAIN.rect(x0, y0, w0, h0, 6);
        MAIN.noFill();
        MAIN.stroke(CARD_STROKE);
        MAIN.strokeWeight(1f);
        MAIN.rect(x0, y0, w0, h0, 6);

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7, 11);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text("聚类时间线", x0 + 10, y0 + 6);

        if (trail.isEmpty()) {
            return;
        }

        int barX = x0 + 10;
        int barY = y0 + 24;
        int barW = w0 - 20;
        int barH = h0 - 30;

        int n = trail.size();
        float bw = Math.max(1f, (float) barW / Math.max(1, n));

        int i = 0;
        for (TrailPoint t : trail) {
            int c = clusterColor(t.clusterId);
            float px = barX + i * bw;
            MAIN.noStroke();
            MAIN.fill(c, 200);
            MAIN.rect(px, barY, bw + 1f, barH, 0);

            float confY = barY + barH - PApplet.constrain(t.confidence, 0f, 1f) * barH;
            MAIN.stroke(0xFFF7F2C2, 150);
            MAIN.strokeWeight(1f);
            MAIN.line(px, confY, px + bw, confY);
            i++;
        }
    }

    private void collectRawSamplesFromBoard() {
        if (MAIN.currentBoard == null) {
            return;
        }
        double[][] frame = MAIN.currentBoard.getFrameData();
        if (frame == null || frame.length == 0 || frame[0].length == 0) {
            return;
        }

        rawFs = RAW_FS_REQUIRED;
        ensureRawBuffers(RAW_CHUNK_LEN);

        int[] exg = MAIN.currentBoard.getEXGChannels();
        int samplesN = frame[0].length;

        for (int i = 0; i < samplesN; i++) {
            for (int ch = 0; ch < 8; ch++) {
                float v = 0f;
                if (exg != null && ch < exg.length) {
                    int src = exg[ch];
                    if (src >= 0 && src < frame.length && i < frame[src].length) {
                        v = (float) frame[src][i];
                    }
                }
                rawAgg[ch][rawFilled] = v;
            }

            rawFilled++;
            if (rawFilled >= rawChunkLen) {
                float[][] chunk = new float[8][rawChunkLen];
                for (int ch = 0; ch < 8; ch++) {
                    System.arraycopy(rawAgg[ch], 0, chunk[ch], 0, rawChunkLen);
                }
                rawChunkQueue.addLast(new RawChunk(chunk));
                if (rawChunkQueue.size() > 40) {
                    rawChunkQueue.pollFirst();
                }
                rawFilled = 0;
            }
        }
    }

    private void ensureRawBuffers(int chunkLen) {
        if (chunkLen <= 0) {
            chunkLen = 1;
        }
        if (rawChunkLen != chunkLen || rawAgg.length != 8 || rawAgg[0].length != chunkLen) {
            rawChunkLen = chunkLen;
            rawAgg = new float[8][rawChunkLen];
            rawFilled = 0;
        }
    }

    private void sendOneRawChunkIfAvailable() {
        RawChunk chunk = rawChunkQueue.pollFirst();
        if (chunk == null) {
            return;
        }
        if (!isValidRawChunk(chunk)) {
            return;
        }

        JSONObject payload = new JSONObject();
        payload.put("fs", RAW_FS_REQUIRED);

        JSONArray channels = new JSONArray();
        for (int i = 0; i < RAW_CHANNELS.length; i++) {
            channels.put(RAW_CHANNELS[i]);
        }
        payload.put("channels", channels);

        JSONArray samples = new JSONArray();
        for (int ch = 0; ch < 8; ch++) {
            JSONArray arr = new JSONArray();
            for (int i = 0; i < chunk.samples[ch].length; i++) {
                arr.put(chunk.samples[ch][i]);
            }
            samples.put(arr);
        }
        payload.put("samples", samples);

        sendEnvelope("eeg.raw", payload);
        sentRawCount++;
    }

    private boolean isValidRawChunk(RawChunk chunk) {
        if (chunk == null || chunk.samples == null) {
            return false;
        }
        if (chunk.samples.length != 8) {
            return false;
        }
        int n = -1;
        for (int ch = 0; ch < 8; ch++) {
            if (chunk.samples[ch] == null) {
                return false;
            }
            if (n < 0) {
                n = chunk.samples[ch].length;
            } else if (chunk.samples[ch].length != n) {
                return false;
            }
        }
        return n > 0;
    }

    private void sendControl(String action) {
        JSONObject payload = new JSONObject();
        payload.put("action", action);
        sendEnvelope("session.control", payload);
        sentControlCount++;
    }

    private synchronized void sendEnvelope(String type, JSONObject payload) {
        if (ws == null || !ws.isOpen()) {
            return;
        }
        try {
            sendSeq++;
            JSONObject msg = new JSONObject();
            msg.put("type", type);
            msg.put("session_id", sessionId);
            msg.put("seq", sendSeq);
            msg.put("ts", nextSendTs());
            msg.put("payload", payload);
            ws.send(msg.toString());
        } catch (Exception ignored) {
        }
    }

    private synchronized double nextSendTs() {
        double now = nowSec();
        if (now <= lastSendTs) {
            now = lastSendTs + 0.0001;
        }
        lastSendTs = now;
        return now;
    }

    private void startRawTicker() {
        if (rawTicker != null) {
            return;
        }
        rawTicker = Executors.newSingleThreadScheduledExecutor();
        rawTicker.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                rawTick.set(true);
            }
        }, 100, 100, TimeUnit.MILLISECONDS);
    }

    private void stopRawTicker() {
        if (rawTicker != null) {
            rawTicker.shutdownNow();
            rawTicker = null;
        }
    }

    private void pollHotkeys() {
        boolean pDown = MAIN.keyPressed && (MAIN.key == 'p' || MAIN.key == 'P');
        boolean tDown = MAIN.keyPressed && (MAIN.key == 't' || MAIN.key == 'T');

        if (pDown && !prevPDown) {
            if (sessionRunning) {
                sessionRunning = false;
                sendControl("pause");
            } else {
                sessionRunning = true;
                sendControl("start");
            }
        }

        if (tDown && !prevTDown) {
            stopRequested = true;
            sessionRunning = false;
            gracefulStopAndClose();
        }

        prevPDown = pDown;
        prevTDown = tDown;
    }

    private synchronized void gracefulStopAndClose() {
        try {
            if (ws != null && ws.isOpen()) {
                sendControl("stop");
            }
        } catch (Exception ignored) {
        }

        try {
            if (ws != null) {
                ws.close();
            }
        } catch (Exception ignored) {
        }

        wsConnected = false;
        wantWs = false;
        stopRawTicker();
    }

    private float[][] parsePolygon(JSONArray arr) {
        int n = arr.length();
        if (n < 2) {
            return null;
        }
        float[][] out = new float[n][2];
        for (int i = 0; i < n; i++) {
            JSONArray p = arr.optJSONArray(i);
            if (p == null || p.length() < 2) {
                return null;
            }
            out[i][0] = (float) p.optDouble(0, 0.0);
            out[i][1] = (float) p.optDouble(1, 0.0);
        }
        return out;
    }

    private float[][] getInterpolatedPolygon(ContourState state, long nowMs) {
        if (state.current == null) {
            return state.target;
        }
        if (state.target == null) {
            return state.current;
        }
        if (state.current.length != state.target.length) {
            return state.target;
        }

        float t = state.tweenDurationMs <= 0 ? 1f : PApplet.constrain((nowMs - state.tweenStartMs) / (float) state.tweenDurationMs, 0f, 1f);
        if (t >= 1f) {
            state.current = state.target;
            return state.current;
        }

        float[][] out = new float[state.current.length][2];
        for (int i = 0; i < state.current.length; i++) {
            out[i][0] = PApplet.lerp(state.current[i][0], state.target[i][0], t);
            out[i][1] = PApplet.lerp(state.current[i][1], state.target[i][1], t);
        }
        return out;
    }

    private float mapLatentX(float xVal, int x0, int w0) {
        float t = (xVal - viewMinX) / (viewMaxX - viewMinX);
        t = PApplet.constrain(t, 0f, 1f);
        return x0 + t * w0;
    }

    private float mapLatentY(float yVal, int y0, int h0) {
        float t = (yVal - viewMinY) / (viewMaxY - viewMinY);
        t = PApplet.constrain(t, 0f, 1f);
        return y0 + (1f - t) * h0;
    }

    private int clusterColor(int clusterId) {
        int idx = Math.abs(clusterId) % 8;
        int[] palette = {
                0xFF63D2FF,
                0xFF7DFFB4,
                0xFFFFD166,
                0xFFFF8A7A,
                0xFFC9A4FF,
                0xFF6EE7D8,
                0xFFFFB68F,
                0xFF9FB8FF
        };
        return palette[idx];
    }

    private float[] jsonArrayToFloat(JSONArray arr) {
        float[] out = new float[arr.length()];
        for (int i = 0; i < arr.length(); i++) {
            out[i] = (float) arr.optDouble(i, 0.0);
        }
        return out;
    }

    private void drawGradientBackground(int gx, int gy, int gw, int gh) {
        MAIN.pushStyle();
        MAIN.noFill();
        for (int i = 0; i < gh; i++) {
            float t = gh <= 1 ? 0f : (float) i / (float) (gh - 1);
            MAIN.stroke(lerpColorARGB(BG_TOP, BG_BOTTOM, t));
            MAIN.line(gx, gy + i, gx + gw, gy + i);
        }
        MAIN.popStyle();
    }

    private int lerpColorARGB(int c1, int c2, float t) {
        int a1 = (c1 >>> 24) & 0xFF;
        int r1 = (c1 >>> 16) & 0xFF;
        int g1 = (c1 >>> 8) & 0xFF;
        int b1 = c1 & 0xFF;
        int a2 = (c2 >>> 24) & 0xFF;
        int r2 = (c2 >>> 16) & 0xFF;
        int g2 = (c2 >>> 8) & 0xFF;
        int b2 = c2 & 0xFF;
        int a = (int) PApplet.lerp(a1, a2, t);
        int r = (int) PApplet.lerp(r1, r2, t);
        int g = (int) PApplet.lerp(g1, g2, t);
        int b = (int) PApplet.lerp(b1, b2, t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private double nowSec() {
        return System.currentTimeMillis() / 1000.0;
    }

    private static class InboundMsg {
        final JSONObject root;
        final long recvLocalMs;

        InboundMsg(JSONObject root, long recvLocalMs) {
            this.root = root;
            this.recvLocalMs = recvLocalMs;
        }
    }

    private static class PointState {
        int windowId;
        float latentX;
        float latentY;
        int clusterId;
        float confidence;
        float signalQuality;
        float[] clusterProb = new float[0];
        double ts;
    }

    private static class TrailPoint {
        final int windowId;
        final float x;
        final float y;
        final int clusterId;
        final float confidence;
        final double ts;

        TrailPoint(int windowId, float x, float y, int clusterId, float confidence, double ts) {
            this.windowId = windowId;
            this.x = x;
            this.y = y;
            this.clusterId = clusterId;
            this.confidence = confidence;
            this.ts = ts;
        }
    }

    private static class RawChunk {
        final float[][] samples;

        RawChunk(float[][] samples) {
            this.samples = samples;
        }
    }

    private static class ContourState {
        int clusterId;
        float[][] current;
        float[][] target;
        long tweenStartMs;
        long tweenDurationMs;
    }
}
