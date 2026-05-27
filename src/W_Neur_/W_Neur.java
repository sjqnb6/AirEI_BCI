package W_Neur_;

import Globel.GUI;
import Widget_.Widget;
import controlP5.*;
import org.json.JSONObject;
import processing.core.PApplet;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static Globel.GUI.*;

public class W_Neur extends Widget implements NeurProtocolClient.Listener {
    private static final int BG_TOP = 0xFF0A1220;
    private static final int BG_BOTTOM = 0xFF0E1B32;
    private static final int PANEL = 0xCC13243F;
    private static final int PANEL_STROKE = 0x6683A2CC;
    private static final int CARD = 0x2A163153;
    private static final int CARD_STROKE = 0x4D91B3DB;
    private static final int TEXT_MAIN = 0xFFEAF2FF;
    private static final int TEXT_SUB = 0xFF98AECE;
    private static final int GRID = 0x2D90AED8;
    private static final int ACCENT = 0xFF57D9FF;
    private static final int GOOD = 0xFF56D79E;
    private static final int WARN = 0xFFE9B45A;
    private static final int BAD = 0xFFEE6A74;

    private static final int HISTORY_SIZE = 180;
    private static final String[] RATE_LABELS = {"轨迹-高密", "轨迹-标准", "轨迹-稀疏"};

    private final GUI MAIN;
    private final NeurProtocolClient client;
    private final NeurDataStore store;

    private final ControlP5 localCp5;
    private final Textfield endpointTf;
    private final Button connectBtn;
    private final Button clearBtn;
    private final List<Controller> cp5Elements = new ArrayList<Controller>();

    private boolean requestedStreaming = false;
    private int rateMode = 0;
    private int viewMode = 0;
    private float zoom = 1f;
    private float panX = 0f;
    private float panY = 0f;

    private final float[] nllHistory = new float[HISTORY_SIZE];
    private final float[] accHistory = new float[HISTORY_SIZE];
    private final float[] noiseHistory = new float[HISTORY_SIZE];
    private final float[] alphaHistory = new float[HISTORY_SIZE];
    private int histWrite = 0;
    private boolean histFilled = false;

    private String connectionNotice = "";
    private long connectionNoticeMs = 0L;
    private long lastParamsSnapshotMs = 0L;

    public W_Neur(GUI MAIN) {
        super(MAIN);
        this.MAIN = MAIN;

        addDropdown("NeurRateMode", "轨迹", java.util.Arrays.asList(RATE_LABELS), 0);
        addDropdown("NeurViewMode", "视图", java.util.Arrays.asList("轨迹视图", "簇视图"), 0);

        store = new NeurDataStore();
        client = new NeurProtocolClient(this);

        localCp5 = new ControlP5(MAIN);
        localCp5.setGraphics(MAIN, 0, 0);
        localCp5.setAutoDraw(false);

        endpointTf = localCp5.addTextfield("neurEndpoint")
                .setPosition(x0 + 6, y0 + navH + 1)
                .setCaptionLabel("")
                .setSize(220, navH - 3)
                .setFont(p7)
                .setFocus(false)
                .setColor(MAIN.color(230))
                .setColorBackground(MAIN.color(22, 33, 54))
                .setColorValueLabel(MAIN.color(236))
                .setColorForeground(MAIN.color(115, 145, 190))
                .setColorActive(MAIN.isSelected_color)
                .setColorCursor(MAIN.color(230))
                .setAutoClear(false)
                .setText(client.getEndpoint());
        endpointTf.onDoublePress(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                endpointTf.clear();
            }
        });
        endpointTf.addCallback(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                if (theEvent.getAction() == ControlP5.ACTION_BROADCAST || theEvent.getAction() == ControlP5.ACTION_LEAVE) {
                    client.setEndpoint(endpointTf.getText());
                    endpointTf.setFocus(false);
                }
            }
        });

        connectBtn = MAIN.createButton(localCp5, "neurStartStop", "开始", x0 + 230, y0 + navH + 1, 84, navH - 3, p7, 12, MAIN.colorNotPressed, MAIN.OPENBCI_DARKBLUE);
        connectBtn.setColorBackground(0xFF2A4A70);
        connectBtn.setColorForeground(0xFF30557E);
        connectBtn.setColorActive(0xFF366293);
        connectBtn.setBorderColor(0xFF82A3CC);
        connectBtn.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                toggleStreaming();
            }
        });
        connectBtn.setDescription("开始/停止神经聚类实时流。");

        clearBtn = MAIN.createButton(localCp5, "neurClear", "重置", x0 + 320, y0 + navH + 1, 70, navH - 3, p7, 12, MAIN.colorNotPressed, MAIN.OPENBCI_DARKBLUE);
        clearBtn.setColorBackground(0xFF22344F);
        clearBtn.setColorForeground(0xFF2A3E5E);
        clearBtn.setColorActive(0xFF2F496E);
        clearBtn.setBorderColor(0xFF6F8FB9);
        clearBtn.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                resetViewAndTrends();
            }
        });

        cp5Elements.add(endpointTf);
        cp5Elements.add(connectBtn);
        cp5Elements.add(clearBtn);
    }

    public void NeurRateMode(int n) {
        rateMode = PApplet.constrain(n, 0, RATE_LABELS.length - 1);
    }

    public void NeurViewMode(int n) {
        viewMode = PApplet.constrain(n, 0, 1);
    }

    @Override
    public void update() {
        super.update();

        lockElementsOnOverlapCheck(cp5Elements);
        MAIN.textfieldUpdateHelper.checkTextfield(endpointTf);

        client.setEndpoint(endpointTf.getText());
        store.tickAnimation();

        if (requestedStreaming) {
            int sr = MAIN.currentBoard.getSampleRate();
            client.tick(MAIN.currentBoard.isStreaming(), sr);
            client.pushFrame(MAIN.currentBoard.getFrameData(), MAIN.currentBoard.getEXGChannels());
        }

        NeurDataStore.ParamsState ps = store.getParamsCopy();
        if (ps.tsMs > 0 && ps.tsMs != lastParamsSnapshotMs) {
            pushParamHistory(ps);
            lastParamsSnapshotMs = ps.tsMs;
        }
    }

    @Override
    public void draw() {
        super.draw();

        MAIN.pushStyle();
        drawGradientBackground();

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

        int contentY = panelY + 52;
        int contentH = panelH - 62;
        int leftW = (int) (panelW * 0.67f);
        int rightW = panelW - leftW - 12;

        drawMainScatter(panelX + 10, contentY, leftW - 10, contentH);
        drawRightPanel(panelX + leftW + 6, contentY, rightW - 16, contentH);

        localCp5.draw();
        MAIN.popStyle();
    }

    @Override
    public void screenResized() {
        super.screenResized();
        localCp5.setGraphics(pApplet, 0, 0);
        endpointTf.setPosition(x0 + 6, y0 + navH + 1);
        connectBtn.setPosition(x0 + 230, y0 + navH + 1);
        clearBtn.setPosition(x0 + 320, y0 + navH + 1);
    }

    @Override
    public void mouseDragged() {
        super.mouseDragged();
        if (MAIN.mouseButton == PApplet.RIGHT && isMouseInsideChart()) {
            panX += MAIN.mouseX - MAIN.pmouseX;
            panY += MAIN.mouseY - MAIN.pmouseY;
        }
    }

    @Override
    public void mousePressed() {
        super.mousePressed();
        if (isMouseInsideChart() && MAIN.mouseButton == PApplet.LEFT) {
            if (MAIN.keyPressed && MAIN.keyCode == PApplet.SHIFT) {
                resetView();
            }
        }
    }

    public void mouseWheel(MouseEvent event) {
        if (!isMouseInsideChart()) {
            return;
        }
        float delta = event.getCount();
        float factor = delta > 0 ? 0.92f : 1.08f;
        zoom *= factor;
        zoom = PApplet.constrain(zoom, 0.45f, 4.0f);
    }

    @Override
    public void onSocketOpen() {
        setNotice("WebSocket 已连接");
    }

    @Override
    public void onSocketClose(String reason) {
        setNotice("WebSocket 已关闭: " + reason);
    }

    @Override
    public void onSocketError(String error) {
        setNotice("WebSocket 错误: " + error);
    }

    @Override
    public void onServerError(String message) {
        store.onServerError(message);
        setNotice("收到 server.error");
    }

    @Override
    public void onPoint(JSONObject payload, long tsMs) {
        store.onPoint(payload, tsMs);
    }

    @Override
    public void onPatch(JSONObject payload, long tsMs) {
        store.onPatch(payload, tsMs);
    }

    @Override
    public void onSnapshot(JSONObject payload, long tsMs) {
        store.onSnapshot(payload, tsMs);
    }

    @Override
    public void onParams(JSONObject payload, long tsMs) {
        store.onParams(payload, tsMs);
    }

    private void toggleStreaming() {
        requestedStreaming = !requestedStreaming;
        if (requestedStreaming) {
            client.setEndpoint(endpointTf.getText());
            client.start();
            setNotice("已请求启动会话");
        } else {
            client.stop();
            setNotice("会话已停止");
        }
        connectBtn.getCaptionLabel().setText(requestedStreaming ? "停止" : "开始");
        connectBtn.setColorBackground(requestedStreaming ? 0xFF7A3645 : 0xFF2A4A70);
    }

    private void resetViewAndTrends() {
        resetView();
        for (int i = 0; i < HISTORY_SIZE; i++) {
            nllHistory[i] = 0f;
            accHistory[i] = 0f;
            noiseHistory[i] = 0f;
            alphaHistory[i] = 0f;
        }
        histWrite = 0;
        histFilled = false;
        setNotice("界面已重置");
    }

    private void resetView() {
        zoom = 1f;
        panX = 0f;
        panY = 0f;
    }

    private void drawHeader(int x0, int y0, int w0) {
        MAIN.fill(TEXT_MAIN);
        MAIN.textFont(p7);
        MAIN.textSize(15);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text("神经聚类中心", x0 + 12, y0 + 9);

        String ws = client.isConnected() ? "在线" : "离线";
        int wsColor = client.isConnected() ? GOOD : WARN;
        drawBadge(x0 + w0 - 204, y0 + 10, 66, 22, ws, wsColor);
        drawBadge(x0 + w0 - 134, y0 + 10, 58, 22, "fs250", client.isConnected() ? GOOD : WARN);
        drawBadge(x0 + w0 - 72, y0 + 10, 58, 22, requestedStreaming ? "运行" : "待机", requestedStreaming ? GOOD : WARN);

        MAIN.fill(TEXT_SUB);
        MAIN.textSize(11);
        String line = "会话 " + client.getSessionId()
                + " | 缓冲 " + client.getQueuedSampleCount()
                + " | 活跃簇 " + store.getParamsCopy().activeClusters
                + "/" + store.getParamsCopy().maxClusters;
        MAIN.text(line, x0 + 90, y0 + 31);

        long now = System.currentTimeMillis();
        if (now - connectionNoticeMs < 3000L && connectionNotice != null && connectionNotice.length() > 0) {
            MAIN.textAlign(PApplet.RIGHT, PApplet.TOP);
            MAIN.fill(TEXT_SUB);
            MAIN.text(connectionNotice, x0 + w0 - 12, y0 + 31);
        }

        MAIN.stroke(PANEL_STROKE);
        MAIN.line(x0 + 10, y0 + 46, x0 + w0 - 10, y0 + 46);
    }

    private void drawMainScatter(int x0, int y0, int w0, int h0) {
        drawCard(x0, y0, w0, h0, "潜空间聚类图");

        int gx = x0 + 38;
        int gy = y0 + 28;
        int gw = w0 - 54;
        int gh = h0 - 44;

        drawScatterGrid(gx, gy, gw, gh);

        List<NeurDataStore.PointState> points = store.getPointsOrdered();
        if (points.size() < 1) {
            MAIN.fill(TEXT_SUB);
            MAIN.textFont(p7);
            MAIN.textSize(11);
            MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
            MAIN.text("等待 brain.point 实时数据...", gx + 4, gy + 6);
            return;
        }

        float[] bounds = computeBounds(points);
        float minX = bounds[0], maxX = bounds[1], minY = bounds[2], maxY = bounds[3];
        float spanX = Math.max(1e-3f, maxX - minX);
        float spanY = Math.max(1e-3f, maxY - minY);
        float padX = spanX * 0.2f / zoom;
        float padY = spanY * 0.2f / zoom;
        minX -= padX;
        maxX += padX;
        minY -= padY;
        maxY += padY;

        minX -= panX * spanX / Math.max(10f, gw);
        maxX -= panX * spanX / Math.max(10f, gw);
        minY += panY * spanY / Math.max(10f, gh);
        maxY += panY * spanY / Math.max(10f, gh);

        List<ClusterStats> recentStats = collectRecentClusterStats(points);
        drawClusterEllipsesFromStats(recentStats, gx, gy, gw, gh, minX, maxX, minY, maxY);
        drawPointsAndTrajectory(points, gx, gy, gw, gh, minX, maxX, minY, maxY);
        drawEventsOverlay(store.getEvents(), gx, gy, gw);
        drawClusterCountDebug(recentStats, points, gx, gy, gw, gh);
    }

    private List<ClusterStats> collectRecentClusterStats(List<NeurDataStore.PointState> points) {
        java.util.HashMap<String, ClusterStats> statsMap = new java.util.HashMap<String, ClusterStats>();
        int start = Math.max(0, points.size() - 360);
        for (int i = start; i < points.size(); i++) {
            NeurDataStore.PointState p = points.get(i);
            String uid = p.clusterUid == null ? "unknown" : p.clusterUid;
            ClusterStats st = statsMap.get(uid);
            if (st == null) {
                st = new ClusterStats();
                st.uid = uid;
                st.clusterId = p.clusterId;
                statsMap.put(uid, st);
            }
            st.add(p.x, p.y, p.confidence);
            st.addRenderedColor(boostPointColor(blendColorByTransition(p)));
        }
        return new ArrayList<ClusterStats>(statsMap.values());
    }

    private void drawClusterEllipsesFromStats(List<ClusterStats> statsList, int gx, int gy, int gw, int gh,
                                              float minX, float maxX, float minY, float maxY) {
        for (ClusterStats st : statsList) {
            if (st.count < 6) {
                continue;
            }
            float[] cov = st.covariance();
            float[] eig = eig2x2(cov[0], cov[1], cov[2], cov[3]);
            float l1 = PApplet.constrain(eig[0], 0.001f, 1.8f);
            float l2 = PApplet.constrain(eig[1], 0.001f, 1.8f);
            float angle = eig[2];
            float cx = mapX(st.mx, minX, maxX, gx, gw);
            float cy = mapY(st.my, minY, maxY, gy, gh);

            float sigmaScale = 1.85f;
            float aData = sigmaScale * (float) Math.sqrt(l1);
            float bData = sigmaScale * (float) Math.sqrt(l2);
            float sx = (aData / Math.max(1e-3f, maxX - minX)) * gw;
            float sy = (bData / Math.max(1e-3f, maxY - minY)) * gh;
            sx = PApplet.constrain(sx, 8f, gw * 0.24f);
            sy = PApplet.constrain(sy, 8f, gh * 0.24f);

            int color = st.avgRenderedColor();
            if (color == 0) {
                color = boostPointColor(clusterColor(st.uid));
            }
            float fade = PApplet.constrain(st.avgConfidence * 0.6f + 0.4f, 0.35f, 1f);

            MAIN.pushMatrix();
            MAIN.translate(cx, cy);
            MAIN.rotate(-angle);
            for (int layer = 5; layer >= 1; layer--) {
                float k = layer / 5f;
                int alpha = (int) (58f * fade * k * k);
                MAIN.noStroke();
                MAIN.fill((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, alpha);
                MAIN.ellipse(0, 0, sx * 2f * k, sy * 2f * k);
            }
            MAIN.stroke((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (int) (235 * fade));
            MAIN.strokeWeight(2.0f);
            MAIN.noFill();
            MAIN.ellipse(0, 0, sx * 2f, sy * 2f);
            MAIN.popMatrix();

            MAIN.fill(TEXT_SUB);
            MAIN.textFont(p7);
            MAIN.textSize(10);
            MAIN.textAlign(PApplet.LEFT, PApplet.CENTER);
            MAIN.text(st.uid + " #" + st.clusterId, cx + 4, cy - 8);
        }
    }

    private void drawClusterCountDebug(List<ClusterStats> statsList, List<NeurDataStore.PointState> points, int gx, int gy, int gw, int gh) {
        if ((statsList == null || statsList.isEmpty()) && (points == null || points.isEmpty())) {
            return;
        }

        java.util.HashMap<String, Integer> recentCountByUid = new java.util.HashMap<String, Integer>();
        if (statsList != null) {
            for (ClusterStats st : statsList) {
                recentCountByUid.put(st.uid, st.count);
            }
        }

        java.util.HashMap<String, Integer> visibleCountByUid = new java.util.HashMap<String, Integer>();
        java.util.HashMap<String, Integer> sumRByUid = new java.util.HashMap<String, Integer>();
        java.util.HashMap<String, Integer> sumGByUid = new java.util.HashMap<String, Integer>();
        java.util.HashMap<String, Integer> sumBByUid = new java.util.HashMap<String, Integer>();
        long newestTs = points.get(points.size() - 1).tsMs;
        for (NeurDataStore.PointState p : points) {
            float age = PApplet.constrain((newestTs - p.tsMs) / 12000f, 0f, 1f);
            float conf = PApplet.constrain(p.confidence, 0f, 1f);
            int alpha = (int) PApplet.lerp(255, 112, age);
            alpha = Math.max(95, (int) (alpha * PApplet.lerp(0.78f, 1.0f, conf)));
            if (alpha < 45) {
                continue;
            }
            String uid = p.clusterUid == null ? "unknown" : p.clusterUid;
            int rendered = boostPointColor(blendColorByTransition(p));
            int rr = (rendered >> 16) & 0xFF;
            int gg = (rendered >> 8) & 0xFF;
            int bb = rendered & 0xFF;
            Integer old = visibleCountByUid.get(uid);
            visibleCountByUid.put(uid, old == null ? 1 : old + 1);
            sumRByUid.put(uid, sumRByUid.getOrDefault(uid, 0) + rr);
            sumGByUid.put(uid, sumGByUid.getOrDefault(uid, 0) + gg);
            sumBByUid.put(uid, sumBByUid.getOrDefault(uid, 0) + bb);
        }

        List<String> uids = new ArrayList<String>(visibleCountByUid.keySet());
        for (String uid : recentCountByUid.keySet()) {
            if (!visibleCountByUid.containsKey(uid)) {
                uids.add(uid);
            }
        }
        uids.sort((a, b) -> Integer.compare(
                visibleCountByUid.getOrDefault(b, 0),
                visibleCountByUid.getOrDefault(a, 0))
        );

        int boxX = gx + 8;
        int boxY = gy + 8;
        int boxW = Math.min(250, gw - 16);
        int rows = Math.min(16, uids.size());
        int boxH = 18 + rows * 12;
        if (uids.size() > rows) {
            boxH += 12;
        }

        MAIN.noStroke();
        MAIN.fill(11, 19, 34, 160);
        MAIN.rect(boxX, boxY, boxW, boxH, 3);
        MAIN.stroke(120, 146, 185, 110);
        MAIN.noFill();
        MAIN.rect(boxX, boxY, boxW, boxH, 3);

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7);
        MAIN.textSize(9);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text("Count: vis(可见)/rc(近期360), ellipse>=6", boxX + 6, boxY + 4);

        int y = boxY + 18;
        for (int i = 0; i < rows; i++) {
            String uid = uids.get(i);
            int vis = visibleCountByUid.getOrDefault(uid, 0);
            int rc = recentCountByUid.getOrDefault(uid, 0);
            int c;
            if (vis > 0) {
                int rr = clamp255(sumRByUid.getOrDefault(uid, 0) / vis);
                int gg = clamp255(sumGByUid.getOrDefault(uid, 0) / vis);
                int bb = clamp255(sumBByUid.getOrDefault(uid, 0) / vis);
                c = ((rr & 0xFF) << 16) | ((gg & 0xFF) << 8) | (bb & 0xFF);
            } else {
                c = boostPointColor(clusterColor(uid));
            }
            MAIN.noStroke();
            MAIN.fill((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, 240);
            MAIN.circle(boxX + 8, y + 4, 6);

            MAIN.fill(TEXT_MAIN);
            MAIN.textFont(p7);
            MAIN.textSize(9);
            MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
            String flag = rc >= 6 ? "ok" : "low";
            MAIN.text(uid + "  vis=" + vis + " rc=" + rc + " [" + flag + "]", boxX + 14, y);
            y += 12;
        }

        if (uids.size() > rows) {
            MAIN.fill(TEXT_SUB);
            MAIN.textFont(p7);
            MAIN.textSize(9);
            MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
            MAIN.text("... +" + (uids.size() - rows) + " more", boxX + 14, y);
        }
    }

    private void drawPointsAndTrajectory(List<NeurDataStore.PointState> points, int gx, int gy, int gw, int gh,
                                         float minX, float maxX, float minY, float maxY) {
        long newestTs = points.get(points.size() - 1).tsMs;

        float latestX = 0f;
        float latestY = 0f;
        float latestR = 0f;
        for (int i = 0; i < points.size(); i++) {
            NeurDataStore.PointState p = points.get(i);
            float age = PApplet.constrain((newestTs - p.tsMs) / 12000f, 0f, 1f);
            float conf = PApplet.constrain(p.confidence, 0f, 1f);
            int alpha = (int) PApplet.lerp(255, 112, age);
            alpha = Math.max(95, (int) (alpha * PApplet.lerp(0.78f, 1.0f, conf)));
            int c = boostPointColor(blendColorByTransition(p));
            float px = mapX(p.x, minX, maxX, gx, gw);
            float py = mapY(p.y, minY, maxY, gy, gh);

            float rr = 4.4f + 1.8f * conf;
            MAIN.fill((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, alpha);
            int strokeAlpha = Math.max(120, (int) (alpha * 0.92f));
            MAIN.stroke(245, 249, 255, strokeAlpha);
            MAIN.strokeWeight(1.3f);
            MAIN.circle(px, py, rr);

            int coreAlpha = Math.max(85, (int) (alpha * 0.82f));
            MAIN.noStroke();
            MAIN.fill(245, 250, 255, coreAlpha);
            MAIN.circle(px, py, Math.max(1.6f, rr * 0.26f));

            if (i == points.size() - 1) {
                latestX = px;
                latestY = py;
                latestR = rr;
            }
        }

        if (latestR > 0f) {
            MAIN.noFill();
            MAIN.stroke(250, 252, 255, 245);
            MAIN.strokeWeight(1.6f);
            MAIN.circle(latestX, latestY, latestR + 4.0f);
        }
    }

    private void drawEventsOverlay(List<NeurDataStore.EventState> events, int gx, int gy, int gw) {
        if (events.isEmpty()) {
            return;
        }
        int y = gy + 8;
        for (int i = events.size() - 1; i >= 0; i--) {
            NeurDataStore.EventState e = events.get(i);
            int c = "birth".equals(e.event) ? GOOD : ("merge".equals(e.event) ? WARN : BAD);
            int alpha = (int) (220 * e.life);
            MAIN.fill((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, alpha);
            MAIN.textAlign(PApplet.RIGHT, PApplet.TOP);
            MAIN.textFont(p7);
            MAIN.textSize(10);
            MAIN.text(eventNameCn(e.event) + "  " + e.clusterUid, gx + gw - 6, y);
            y += 12;
            if (y > gy + 60) {
                break;
            }
        }
    }

    private void drawRightPanel(int x0, int y0, int w0, int h0) {
        int topH = 116;
        int gap = 8;
        int trendH = (h0 - topH - gap * 2) / 2;
        int weightH = h0 - topH - gap * 2 - trendH;

        drawStatusCard(x0, y0, w0, topH);
        drawTrendCard(x0, y0 + topH + gap, w0, trendH);
        drawWeightCard(x0, y0 + topH + gap + trendH + gap, w0, weightH);
    }

    private void drawStatusCard(int x0, int y0, int w0, int h0) {
        drawCard(x0, y0, w0, h0, "运行状态");

        NeurDataStore.ParamsState ps = store.getParamsCopy();
        float stability = store.getStability();
        int statusColor = client.isConnected() ? GOOD : WARN;

        MAIN.fill(TEXT_SUB);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.textFont(p7);
        MAIN.textSize(11);
        MAIN.text("WebSocket", x0 + 10, y0 + 26);
        MAIN.fill(statusColor);
        MAIN.text(client.isConnected() ? "已连接" : "未连接", x0 + 94, y0 + 26);

        MAIN.fill(TEXT_SUB);
        MAIN.text("活跃簇", x0 + 10, y0 + 44);
        MAIN.fill(TEXT_MAIN);
        MAIN.text(ps.activeClusters + " / " + ps.maxClusters, x0 + 94, y0 + 44);

        MAIN.fill(TEXT_SUB);
        MAIN.text("稳定度", x0 + 10, y0 + 62);
        MAIN.fill(qualityColor(stability));
        MAIN.text(fmt(stability), x0 + 94, y0 + 62);

        MAIN.fill(TEXT_SUB);
        MAIN.text("接受率", x0 + 10, y0 + 80);
        MAIN.fill(TEXT_MAIN);
        MAIN.text(fmt(ps.acceptanceRate), x0 + 94, y0 + 80);

        int sr = MAIN.currentBoard.getSampleRate();
        if (sr != NeurProtocolClient.REQUIRED_FS) {
            MAIN.fill(WARN);
            MAIN.textSize(9);
            MAIN.text("采样率必须为 250 (当前 " + sr + ")", x0 + 10, y0 + h0 - 34);
        }

        String clientErr = client.getLastError();
        if (clientErr != null && clientErr.length() > 0) {
            MAIN.fill(BAD);
            MAIN.textSize(9);
            MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
            String clippedErr = clientErr.length() > 90 ? clientErr.substring(0, 90) + "..." : clientErr;
            MAIN.text("ws错误: " + clippedErr, x0 + 10, y0 + h0 - 22);
        }

        String err = store.getLastServerError();
        if (err != null && err.length() > 0) {
            MAIN.fill(BAD);
            MAIN.textSize(9);
            MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
            String clipped = err.length() > 90 ? err.substring(0, 90) + "..." : err;
            MAIN.text("服务端错误", x0 + 10, y0 + h0 - 10);
        }
    }

    private void drawTrendCard(int x0, int y0, int w0, int h0) {
        drawCard(x0, y0, w0, h0, "参数趋势");
        int gx = x0 + 16;
        int gy = y0 + 44;
        int gw = w0 - 26;
        int gh = h0 - 58;
        gh = Math.max(26, gh);

        MAIN.stroke(GRID);
        MAIN.strokeWeight(1f);
        for (int i = 0; i <= 4; i++) {
            float yy = gy + gh * i / 4f;
            MAIN.line(gx, yy, gx + gw, yy);
        }
        MAIN.stroke(PANEL_STROKE);
        MAIN.line(gx, gy, gx, gy + gh);
        MAIN.line(gx, gy + gh, gx + gw, gy + gh);

        drawHistoryLine(nllHistory, histCount(), gx, gy, gw, gh, 0xFF84D8FF, false);
        drawHistoryLine(accHistory, histCount(), gx, gy, gw, gh, 0xFF76E7A4, true);
        drawHistoryLine(noiseHistory, histCount(), gx, gy, gw, gh, 0xFFFFC978, true);
        drawHistoryLine(alphaHistory, histCount(), gx, gy, gw, gh, 0xFFCD97FF, true);

        drawTrendLegend(x0 + 12, y0 + 22, w0 - 24);
    }

    private void drawWeightCard(int x0, int y0, int w0, int h0) {
        drawCard(x0, y0, w0, h0, "簇权重");
        NeurDataStore.ParamsState ps = store.getParamsCopy();
        List<NeurDataStore.ClusterState> clusters = store.getClusters();
        java.util.HashMap<Integer, String> uidByClusterId = new java.util.HashMap<Integer, String>();
        for (NeurDataStore.ClusterState c : clusters) {
            if (c == null || c.clusterUid == null) {
                continue;
            }
            if (c.clusterId > 0 && !"inactive".equals(c.status)) {
                uidByClusterId.put(c.clusterId, c.clusterUid);
            }
        }
        int gx = x0 + 12;
        int gy = y0 + 24;
        int gw = w0 - 24;
        int gh = h0 - 34;

        float[] ws = ps.clusterWeights;
        if (ws.length < 1) {
            MAIN.fill(TEXT_SUB);
            MAIN.textFont(p7);
            MAIN.textSize(11);
            MAIN.text("等待 brain.params 数据...", gx, gy + 4);
            return;
        }

        int barW = Math.max(10, (gw - 4 * (ws.length - 1)) / ws.length);
        for (int i = 0; i < ws.length; i++) {
            float v = PApplet.constrain(ws[i], 0f, 1f);
            int clusterId = i + 1;
            String uid = uidByClusterId.get(clusterId);
            int c = uid != null ? clusterColor(uid) : clusterColor("weight_" + i);
            int x = gx + i * (barW + 4);
            int bh = (int) (gh * v);
            MAIN.noStroke();
            MAIN.fill((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, 215);
            MAIN.rect(x, gy + gh - bh, barW, bh, 2);

            MAIN.fill(TEXT_SUB);
            MAIN.textFont(p7);
            MAIN.textSize(9);
            MAIN.textAlign(PApplet.CENTER, PApplet.TOP);
            MAIN.text(String.valueOf(i + 1), x + barW * 0.5f, gy + gh + 3);
        }
    }

    private void drawScatterGrid(int gx, int gy, int gw, int gh) {
        MAIN.stroke(GRID);
        MAIN.strokeWeight(1f);
        for (int i = 0; i <= 8; i++) {
            float xx = gx + gw * i / 8f;
            MAIN.line(xx, gy, xx, gy + gh);
        }
        for (int i = 0; i <= 6; i++) {
            float yy = gy + gh * i / 6f;
            MAIN.line(gx, yy, gx + gw, yy);
        }

        MAIN.stroke(0x66C3DBFF);
        MAIN.strokeWeight(1.4f);
        MAIN.line(gx, gy + gh * 0.5f, gx + gw, gy + gh * 0.5f);
        MAIN.line(gx + gw * 0.5f, gy, gx + gw * 0.5f, gy + gh);
    }

    private void drawCard(int x0, int y0, int w0, int h0, String title) {
        MAIN.noStroke();
        MAIN.fill(CARD);
        MAIN.rect(x0, y0, w0, h0, 3);
        MAIN.stroke(CARD_STROKE);
        MAIN.noFill();
        MAIN.rect(x0, y0, w0, h0, 3);
        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7);
        MAIN.textSize(11);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text(title, x0 + 8, y0 + 6);
    }

    private void drawBadge(int x0, int y0, int w0, int h0, String text, int color) {
        MAIN.noStroke();
        MAIN.fill((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 34);
        MAIN.rect(x0, y0, w0, h0, 2);
        MAIN.stroke((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 190);
        MAIN.noFill();
        MAIN.rect(x0, y0, w0, h0, 2);
        MAIN.fill(TEXT_MAIN);
        MAIN.textFont(p7);
        MAIN.textSize(10);
        MAIN.textAlign(PApplet.CENTER, PApplet.CENTER);
        MAIN.text(text, x0 + w0 * 0.5f, y0 + h0 * 0.5f + 0.3f);
    }

    private void drawTrendLegend(int x0, int y0, int w0) {
        MAIN.textFont(p7);
        MAIN.textSize(9);
        MAIN.textAlign(PApplet.LEFT, PApplet.CENTER);

        int colW = Math.max(44, w0 / 2);
        int rowGap = 12;

        drawLegendItem(x0, y0, 0xFF84D8FF, "NLL");
        drawLegendItem(x0 + colW, y0, 0xFF76E7A4, "ACC");
        drawLegendItem(x0, y0 + rowGap, 0xFFFFC978, "NOISE");
        drawLegendItem(x0 + colW, y0 + rowGap, 0xFFCD97FF, "ALPHA");
    }

    private void drawLegendItem(int x0, int y0, int color, String label) {
        MAIN.noStroke();
        MAIN.fill((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 235);
        MAIN.circle(x0 + 3, y0, 6);

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7);
        MAIN.textSize(9);
        MAIN.textAlign(PApplet.LEFT, PApplet.CENTER);
        MAIN.text(label, x0 + 10, y0 + 0.3f);
    }

    private void pushParamHistory(NeurDataStore.ParamsState ps) {
        nllHistory[histWrite] = ps.nll;
        accHistory[histWrite] = ps.acceptanceRate;
        noiseHistory[histWrite] = ps.gpNoiseVar;
        alphaHistory[histWrite] = ps.dpAlpha;
        histWrite = (histWrite + 1) % HISTORY_SIZE;
        if (histWrite == 0) {
            histFilled = true;
        }
    }

    private int histCount() {
        return histFilled ? HISTORY_SIZE : histWrite;
    }

    private int historyToIndex(int i, int count) {
        if (!histFilled) {
            return i;
        }
        int start = histWrite % HISTORY_SIZE;
        return (start + i) % HISTORY_SIZE;
    }

    private void drawHistoryLine(float[] hist, int count, int gx, int gy, int gw, int gh, int color, boolean normalize01) {
        if (count < 2) {
            return;
        }

        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            float v = hist[historyToIndex(i, count)];
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        if (normalize01) {
            min = 0f;
            max = Math.max(1f, max);
        }
        if (Math.abs(max - min) < 1e-5f) {
            max = min + 1f;
        }

        MAIN.noFill();
        MAIN.stroke((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 220);
        MAIN.strokeWeight(1.5f);
        MAIN.beginShape();
        for (int i = 0; i < count; i++) {
            float v = hist[historyToIndex(i, count)];
            float t = (v - min) / (max - min);
            float px = gx + gw * i / (float) Math.max(1, count - 1);
            float py = gy + gh - t * gh;
            MAIN.vertex(px, py);
        }
        MAIN.endShape();
    }

    private float[] computeBounds(List<NeurDataStore.PointState> points) {
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (NeurDataStore.PointState p : points) {
            minX = Math.min(minX, p.x);
            maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y);
            maxY = Math.max(maxY, p.y);
        }
        if (minX == Float.MAX_VALUE) {
            minX = -1f;
            maxX = 1f;
            minY = -1f;
            maxY = 1f;
        }
        return new float[]{minX, maxX, minY, maxY};
    }

    private float mapX(float x, float minX, float maxX, int gx, int gw) {
        return gx + (x - minX) / Math.max(1e-6f, maxX - minX) * gw;
    }

    private float mapY(float y, float minY, float maxY, int gy, int gh) {
        return gy + gh - (y - minY) / Math.max(1e-6f, maxY - minY) * gh;
    }

    private int blendColorByTransition(NeurDataStore.PointState p) {
        if (p.previousColor == 0 || p.colorTransition <= 0f) {
            return p.color;
        }
        return lerpRgb(p.color, p.previousColor, p.colorTransition);
    }

    private int clusterColor(String uid) {
        int h = uid == null ? 0 : uid.hashCode();
        int r = 90 + (int) (Math.abs((long) h * 31L) % 150L);
        int g = 90 + (int) (Math.abs((long) h * 57L) % 150L);
        int b = 90 + (int) (Math.abs((long) h * 83L) % 150L);
        return (0xFF << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    private int boostPointColor(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        float gray = (r + g + b) / 3f;
        float satBoost = 1.65f;
        float lift = 26f;

        r = clamp255((int) (gray + (r - gray) * satBoost + lift));
        g = clamp255((int) (gray + (g - gray) * satBoost + lift));
        b = clamp255((int) (gray + (b - gray) * satBoost + lift));

        int max = Math.max(r, Math.max(g, b));
        if (max < 190) {
            float scale = 190f / Math.max(1f, max);
            r = clamp255((int) (r * scale));
            g = clamp255((int) (g * scale));
            b = clamp255((int) (b * scale));
        }

        return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    private int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private int qualityColor(float v) {
        float t = PApplet.constrain(v, 0f, 1f);
        if (t > 0.7f) {
            return GOOD;
        }
        if (t > 0.4f) {
            return WARN;
        }
        return BAD;
    }

    private String fmt(float v) {
        return String.format(Locale.US, "%.2f", v);
    }

    private int lerpRgb(int c1, int c2, float t) {
        int r1 = (c1 >> 16) & 0xFF;
        int g1 = (c1 >> 8) & 0xFF;
        int b1 = c1 & 0xFF;
        int r2 = (c2 >> 16) & 0xFF;
        int g2 = (c2 >> 8) & 0xFF;
        int b2 = c2 & 0xFF;
        int r = (int) PApplet.lerp(r1, r2, t);
        int g = (int) PApplet.lerp(g1, g2, t);
        int b = (int) PApplet.lerp(b1, b2, t);
        return (r << 16) | (g << 8) | b;
    }

    private float[] eig2x2(float a00, float a01, float a10, float a11) {
        float trace = a00 + a11;
        float det = a00 * a11 - a01 * a10;
        float d = Math.max(0f, trace * trace * 0.25f - det);
        float root = (float) Math.sqrt(d);
        float l1 = trace * 0.5f + root;
        float l2 = trace * 0.5f - root;

        float vx = a01;
        float vy = l1 - a00;
        if (Math.abs(vx) + Math.abs(vy) < 1e-6f) {
            vx = l1 - a11;
            vy = a10;
        }
        if (Math.abs(vx) + Math.abs(vy) < 1e-6f) {
            vx = 1f;
            vy = 0f;
        }
        float ang = (float) Math.atan2(vy, vx);
        return new float[]{l1, l2, ang};
    }

    private void drawGradientBackground() {
        for (int i = 0; i < h; i++) {
            float t = i / (float) Math.max(1, h - 1);
            int c = lerpRgb(BG_TOP, BG_BOTTOM, t);
            MAIN.stroke((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF);
            MAIN.line(x, y + i, x + w, y + i);
        }
    }

    private boolean isMouseInsideChart() {
        int pad = 10;
        int panelX = x + pad;
        int panelY = y + pad;
        int panelW = w - pad * 2;
        int panelH = h - pad * 2;
        int contentY = panelY + 52;
        int contentH = panelH - 62;
        int leftW = (int) (panelW * 0.67f);
        int chartX = panelX + 10;
        int chartY = contentY;
        int chartW = leftW - 10;
        int chartH = contentH;
        return MAIN.mouseX >= chartX && MAIN.mouseX <= chartX + chartW
                && MAIN.mouseY >= chartY && MAIN.mouseY <= chartY + chartH;
    }

    private void setNotice(String msg) {
        connectionNotice = msg;
        connectionNoticeMs = System.currentTimeMillis();
    }

    private String eventNameCn(String en) {
        if ("birth".equals(en)) {
            return "新生";
        }
        if ("death".equals(en)) {
            return "消亡";
        }
        if ("merge".equals(en)) {
            return "合并";
        }
        return en;
    }

    private static class ClusterStats {
        String uid;
        int clusterId;
        int count;
        float sumW;
        float mx;
        float my;
        float cxx;
        float cxy;
        float cyy;
        float avgConfidence;
        int sumRenderedR;
        int sumRenderedG;
        int sumRenderedB;

        void add(float x, float y, float conf) {
            float w = PApplet.constrain(conf, 0.2f, 1f);
            count++;
            float prevW = sumW;
            sumW += w;
            float dx = x - mx;
            float dy = y - my;
            float r = w / Math.max(1e-6f, sumW);
            mx += r * dx;
            my += r * dy;
            cxx += w * dx * (x - mx);
            cxy += w * dx * (y - my);
            cyy += w * dy * (y - my);
            avgConfidence += (conf - avgConfidence) / count;
        }

        void addRenderedColor(int color) {
            sumRenderedR += (color >> 16) & 0xFF;
            sumRenderedG += (color >> 8) & 0xFF;
            sumRenderedB += color & 0xFF;
        }

        int avgRenderedColor() {
            if (count <= 0) {
                return 0;
            }
            int r = Math.max(0, Math.min(255, sumRenderedR / count));
            int g = Math.max(0, Math.min(255, sumRenderedG / count));
            int b = Math.max(0, Math.min(255, sumRenderedB / count));
            return ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
        }

        float[] covariance() {
            float den = Math.max(1e-6f, sumW - 1f);
            float vxx = cxx / den;
            float vxy = cxy / den;
            float vyy = cyy / den;
            vxx = Math.max(1e-4f, vxx);
            vyy = Math.max(1e-4f, vyy);
            vxy = PApplet.constrain(vxy, -0.9f * (float) Math.sqrt(vxx * vyy), 0.9f * (float) Math.sqrt(vxx * vyy));
            return new float[]{vxx, vxy, vxy, vyy};
        }
    }
}
