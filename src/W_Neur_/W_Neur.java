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
    private static final String[] RATE_LABELS = {"Trail Dense", "Trail Normal", "Trail Sparse"};

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

        addDropdown("NeurRateMode", "Trail", java.util.Arrays.asList(RATE_LABELS), 0);
        addDropdown("NeurViewMode", "View", java.util.Arrays.asList("Track", "Cluster"), 0);

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

        connectBtn = MAIN.createButton(localCp5, "neurStartStop", "Start", x0 + 230, y0 + navH + 1, 84, navH - 3, p7, 12, MAIN.colorNotPressed, MAIN.OPENBCI_DARKBLUE);
        connectBtn.setColorBackground(0xFF2A4A70);
        connectBtn.setColorForeground(0xFF30557E);
        connectBtn.setColorActive(0xFF366293);
        connectBtn.setBorderColor(0xFF82A3CC);
        connectBtn.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                toggleStreaming();
            }
        });
        connectBtn.setDescription("Start/Stop neural clustering streaming.");

        clearBtn = MAIN.createButton(localCp5, "neurClear", "Reset", x0 + 320, y0 + navH + 1, 70, navH - 3, p7, 12, MAIN.colorNotPressed, MAIN.OPENBCI_DARKBLUE);
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
        setNotice("WS connected");
    }

    @Override
    public void onSocketClose(String reason) {
        setNotice("WS closed: " + reason);
    }

    @Override
    public void onSocketError(String error) {
        setNotice("WS error: " + error);
    }

    @Override
    public void onServerError(String message) {
        store.onServerError(message);
        setNotice("server.error received");
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
            setNotice("Neur session start requested");
        } else {
            client.stop();
            setNotice("Neur session stopped");
        }
        connectBtn.getCaptionLabel().setText(requestedStreaming ? "Stop" : "Start");
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
        setNotice("Panel reset");
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
        MAIN.text("Neural Cluster Center", x0 + 12, y0 + 9);

        String ws = client.isConnected() ? "ONLINE" : "OFFLINE";
        int wsColor = client.isConnected() ? GOOD : WARN;
        drawBadge(x0 + w0 - 204, y0 + 10, 66, 22, ws, wsColor);
        drawBadge(x0 + w0 - 134, y0 + 10, 58, 22, "fs250", client.isConnected() ? GOOD : WARN);
        drawBadge(x0 + w0 - 72, y0 + 10, 58, 22, requestedStreaming ? "RUN" : "IDLE", requestedStreaming ? GOOD : WARN);

        MAIN.fill(TEXT_SUB);
        MAIN.textSize(11);
        String line = "session " + client.getSessionId()
                + " | queue " + client.getQueuedSampleCount()
                + " | active clusters " + store.getParamsCopy().activeClusters
                + "/" + store.getParamsCopy().maxClusters;
        MAIN.text(line, x0 + 12, y0 + 31);

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
        drawCard(x0, y0, w0, h0, "Latent Clustering");

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
            MAIN.text("Waiting for brain.point stream...", gx + 4, gy + 6);
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

        List<NeurDataStore.ClusterState> clusters = store.getClusters();
        drawClusterEllipses(clusters, gx, gy, gw, gh, minX, maxX, minY, maxY);
        drawPointsAndTrajectory(points, gx, gy, gw, gh, minX, maxX, minY, maxY);
        drawEventsOverlay(store.getEvents(), gx, gy, gw);
    }

    private void drawClusterEllipses(List<NeurDataStore.ClusterState> clusters, int gx, int gy, int gw, int gh,
                                     float minX, float maxX, float minY, float maxY) {
        for (NeurDataStore.ClusterState c : clusters) {
            if (c.fade <= 0.01f) {
                continue;
            }
            float cx = mapX(c.centerX, minX, maxX, gx, gw);
            float cy = mapY(c.centerY, minY, maxY, gy, gh);
            float[] eig = eig2x2(c.cov00, c.cov01, c.cov10, c.cov11);
            float l1 = Math.max(1e-5f, eig[0]);
            float l2 = Math.max(1e-5f, eig[1]);
            float angle = eig[2];

            float sigmaScale = 2f;
            float a = sigmaScale * (float) Math.sqrt(l1);
            float b = sigmaScale * (float) Math.sqrt(l2);
            float sx = (a / Math.max(1e-3f, maxX - minX)) * gw;
            float sy = (b / Math.max(1e-3f, maxY - minY)) * gh;

            int color = clusterColor(c.clusterUid);
            int alphaFill = (int) (42 * c.fade);
            int alphaStroke = (int) (185 * c.fade);
            if ("inactive".equals(c.status)) {
                alphaFill = (int) (22 * c.fade);
                alphaStroke = (int) (90 * c.fade);
            }

            MAIN.pushMatrix();
            MAIN.translate(cx, cy);
            MAIN.rotate(-angle);
            MAIN.noStroke();
            MAIN.fill((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, alphaFill);
            MAIN.ellipse(0, 0, sx * 2f, sy * 2f);
            MAIN.stroke((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, alphaStroke);
            MAIN.strokeWeight("inactive".equals(c.status) ? 1f : 1.8f);
            MAIN.noFill();
            MAIN.ellipse(0, 0, sx * 2f, sy * 2f);
            MAIN.popMatrix();

            MAIN.fill(TEXT_SUB);
            MAIN.textFont(p7);
            MAIN.textSize(10);
            MAIN.textAlign(PApplet.LEFT, PApplet.CENTER);
            MAIN.text(c.clusterUid + " #" + c.clusterId, cx + 4, cy - 8);
        }
    }

    private void drawPointsAndTrajectory(List<NeurDataStore.PointState> points, int gx, int gy, int gw, int gh,
                                         float minX, float maxX, float minY, float maxY) {
        long newestTs = points.get(points.size() - 1).tsMs;

        if (viewMode == 0) {
            int stride = rateMode == 0 ? 1 : (rateMode == 1 ? 2 : 4);
            MAIN.noFill();
            MAIN.strokeWeight(1.2f);
            for (int i = stride; i < points.size(); i += stride) {
                NeurDataStore.PointState a = points.get(i - stride);
                NeurDataStore.PointState b = points.get(i);
                float age = PApplet.constrain((newestTs - b.tsMs) / 12000f, 0f, 1f);
                int alpha = (int) PApplet.lerp(220, 30, age);
                int c = blendColorByTransition(b);
                MAIN.stroke((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, alpha);
                MAIN.line(
                        mapX(a.x, minX, maxX, gx, gw),
                        mapY(a.y, minY, maxY, gy, gh),
                        mapX(b.x, minX, maxX, gx, gw),
                        mapY(b.y, minY, maxY, gy, gh)
                );
            }
        }

        MAIN.noStroke();
        for (int i = 0; i < points.size(); i++) {
            NeurDataStore.PointState p = points.get(i);
            float age = PApplet.constrain((newestTs - p.tsMs) / 12000f, 0f, 1f);
            int alpha = (int) PApplet.lerp(255, 40, age);
            alpha = Math.max(18, (int) (alpha * Math.max(0.25f, p.confidence)));
            int c = blendColorByTransition(p);
            MAIN.fill((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF, alpha);
            float px = mapX(p.x, minX, maxX, gx, gw);
            float py = mapY(p.y, minY, maxY, gy, gh);
            float rr = 3.2f + 2.4f * p.confidence;
            MAIN.circle(px, py, rr);
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
            MAIN.text(e.event + "  " + e.clusterUid, gx + gw - 6, y);
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
        drawCard(x0, y0, w0, h0, "Runtime");

        NeurDataStore.ParamsState ps = store.getParamsCopy();
        float stability = store.getStability();
        int statusColor = client.isConnected() ? GOOD : WARN;

        MAIN.fill(TEXT_SUB);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.textFont(p7);
        MAIN.textSize(11);
        MAIN.text("WebSocket", x0 + 10, y0 + 26);
        MAIN.fill(statusColor);
        MAIN.text(client.isConnected() ? "connected" : "disconnected", x0 + 94, y0 + 26);

        MAIN.fill(TEXT_SUB);
        MAIN.text("Active Clusters", x0 + 10, y0 + 44);
        MAIN.fill(TEXT_MAIN);
        MAIN.text(ps.activeClusters + " / " + ps.maxClusters, x0 + 94, y0 + 44);

        MAIN.fill(TEXT_SUB);
        MAIN.text("Stability", x0 + 10, y0 + 62);
        MAIN.fill(qualityColor(stability));
        MAIN.text(fmt(stability), x0 + 94, y0 + 62);

        MAIN.fill(TEXT_SUB);
        MAIN.text("Accept Rate", x0 + 10, y0 + 80);
        MAIN.fill(TEXT_MAIN);
        MAIN.text(fmt(ps.acceptanceRate), x0 + 94, y0 + 80);

        int sr = MAIN.currentBoard.getSampleRate();
        if (sr != NeurProtocolClient.REQUIRED_FS) {
            MAIN.fill(WARN);
            MAIN.textSize(9);
            MAIN.text("sample_rate must be 250 (now " + sr + ")", x0 + 10, y0 + h0 - 34);
        }

        String clientErr = client.getLastError();
        if (clientErr != null && clientErr.length() > 0) {
            MAIN.fill(BAD);
            MAIN.textSize(9);
            MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
            String clippedErr = clientErr.length() > 90 ? clientErr.substring(0, 90) + "..." : clientErr;
            MAIN.text("ws.error: " + clippedErr, x0 + 10, y0 + h0 - 22);
        }

        String err = store.getLastServerError();
        if (err != null && err.length() > 0) {
            MAIN.fill(BAD);
            MAIN.textSize(9);
            MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
            String clipped = err.length() > 90 ? err.substring(0, 90) + "..." : err;
            MAIN.text("server.error: " + clipped, x0 + 10, y0 + h0 - 10);
        }
    }

    private void drawTrendCard(int x0, int y0, int w0, int h0) {
        drawCard(x0, y0, w0, h0, "Params Trend");
        int gx = x0 + 36;
        int gy = y0 + 24;
        int gw = w0 - 46;
        int gh = h0 - 34;

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

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7);
        MAIN.textSize(9);
        MAIN.textAlign(PApplet.LEFT, PApplet.TOP);
        MAIN.text("cyan:nll  green:acc  gold:noise  purple:alpha", gx, y0 + 8);
    }

    private void drawWeightCard(int x0, int y0, int w0, int h0) {
        drawCard(x0, y0, w0, h0, "Cluster Weights");
        NeurDataStore.ParamsState ps = store.getParamsCopy();
        int gx = x0 + 12;
        int gy = y0 + 24;
        int gw = w0 - 24;
        int gh = h0 - 34;

        float[] ws = ps.clusterWeights;
        if (ws.length < 1) {
            MAIN.fill(TEXT_SUB);
            MAIN.textFont(p7);
            MAIN.textSize(11);
            MAIN.text("Waiting brain.params...", gx, gy + 4);
            return;
        }

        int barW = Math.max(10, (gw - 4 * (ws.length - 1)) / ws.length);
        for (int i = 0; i < ws.length; i++) {
            float v = PApplet.constrain(ws[i], 0f, 1f);
            int c = clusterColor("weight_" + i);
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
        int r = 90 + Math.abs((h * 31) % 150);
        int g = 90 + Math.abs((h * 57) % 150);
        int b = 90 + Math.abs((h * 83) % 150);
        return (0xFF << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
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
}
