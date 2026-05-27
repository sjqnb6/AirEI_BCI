package W_Neur_;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.util.LinkedList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

public class NeurProtocolClient {
    public interface Listener {
        void onSocketOpen();
        void onSocketClose(String reason);
        void onSocketError(String error);
        void onServerError(String message);
        void onPoint(JSONObject payload, long tsMs);
        void onPatch(JSONObject payload, long tsMs);
        void onSnapshot(JSONObject payload, long tsMs);
        void onParams(JSONObject payload, long tsMs);
    }

    public static final int REQUIRED_FS = 250;
    public static final int PACKET_INTERVAL_MS = 100;
    public static final int PACKET_SAMPLES = 25;
    private static final String[] REQUIRED_CHANNELS = {
            "Fp1", "Fp2", "C3", "C4", "P3", "P4", "O1", "O2"
    };

    private final Listener listener;
    private final AtomicLong upSeq = new AtomicLong(0L);
    private final LinkedList<double[]> sampleQueue = new LinkedList<double[]>();

    private volatile String endpoint = "ws://127.0.0.1:8000/ws/eeg_realtime";
    private volatile String sessionId = "S001";

    private WebSocketClient socket;
    private boolean enabled = false;
    private boolean sessionStarted = false;
    private long lastPacketMs = 0L;
    private long lastConnectAttemptMs = 0L;
    private long lastRxMs = 0L;
    private long lastTxMs = 0L;
    private String lastError = "";

    public NeurProtocolClient(Listener listener) {
        this.listener = listener;
    }

    public synchronized void setEndpoint(String endpoint) {
        if (endpoint == null) {
            return;
        }
        String e = endpoint.trim();
        if (e.isEmpty()) {
            return;
        }
        if (!e.startsWith("ws://") && !e.startsWith("wss://")) {
            e = "ws://" + e;
        }
        this.endpoint = e;
    }

    public synchronized String getEndpoint() {
        return endpoint;
    }

    public synchronized String getSessionId() {
        return sessionId;
    }

    public synchronized boolean isEnabled() {
        return enabled;
    }

    public synchronized boolean isConnected() {
        return socket != null && socket.isOpen();
    }

    public synchronized boolean isSessionStarted() {
        return sessionStarted;
    }

    public synchronized String getLastError() {
        return lastError;
    }

    public synchronized long getLastRxMs() {
        return lastRxMs;
    }

    public synchronized long getLastTxMs() {
        return lastTxMs;
    }

    public synchronized int getQueuedSampleCount() {
        return sampleQueue.size();
    }

    public synchronized void start() {
        if (enabled) {
            return;
        }
        enabled = true;
        sessionId = "S" + (System.currentTimeMillis() % 1000000L);
        upSeq.set(0L);
        sessionStarted = false;
        lastPacketMs = 0L;
        sampleQueue.clear();
        connectIfNeeded();
    }

    public synchronized void stop() {
        enabled = false;
        sampleQueue.clear();
        sendControl("stop");
        sessionStarted = false;
        closeSocket();
    }

    public synchronized void tick(boolean boardStreaming, int sampleRate) {
        if (!enabled) {
            return;
        }

        connectIfNeeded();
        if (!isConnected()) {
            return;
        }

        if (!boardStreaming) {
            if (sessionStarted) {
                sendControl("stop");
                sessionStarted = false;
            }
            return;
        }

        if (sampleRate != REQUIRED_FS) {
            setLastError("Neur streaming requires fs=250, current=" + sampleRate);
            return;
        }

        if (!sessionStarted) {
            if (!sendControl("start")) {
                return;
            }
            sessionStarted = true;
        }

        long now = System.currentTimeMillis();
        if (now - lastPacketMs < PACKET_INTERVAL_MS) {
            return;
        }

        if (sampleQueue.size() < PACKET_SAMPLES) {
            return;
        }

        sendEegRaw(PACKET_SAMPLES);
        lastPacketMs = now;
    }

    public synchronized void pushFrame(double[][] frame, int[] exgChannels) {
        if (!enabled) {
            return;
        }
        if (frame == null || frame.length == 0 || exgChannels == null || exgChannels.length < 8) {
            return;
        }

        int sampleCount = frame[0].length;
        if (sampleCount <= 0) {
            return;
        }

        for (int i = 0; i < sampleCount; i++) {
            double[] s = new double[8];
            boolean valid = true;
            for (int ch = 0; ch < 8; ch++) {
                int source = exgChannels[ch];
                if (source < 0 || source >= frame.length || i >= frame[source].length) {
                    valid = false;
                    break;
                }
                s[ch] = frame[source][i];
            }
            if (valid) {
                sampleQueue.add(s);
            }
        }

        while (sampleQueue.size() > PACKET_SAMPLES * 10) {
            sampleQueue.poll();
        }
    }

    private void connectIfNeeded() {
        if (!enabled) {
            return;
        }
        if (socket != null) {
            ReadyState state = socket.getReadyState();
            if (state == ReadyState.NOT_YET_CONNECTED || state == ReadyState.OPEN || state == ReadyState.CLOSING) {
                return;
            }
        }

        long now = System.currentTimeMillis();
        if (now - lastConnectAttemptMs < 1000L) {
            return;
        }
        lastConnectAttemptMs = now;

        try {
            final String endpointSnapshot = endpoint;
            socket = new WebSocketClient(new URI(endpointSnapshot)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    synchronized (NeurProtocolClient.this) {
                        lastError = "";
                        lastRxMs = System.currentTimeMillis();
                    }
                    listener.onSocketOpen();
                }

                @Override
                public void onMessage(String message) {
                    synchronized (NeurProtocolClient.this) {
                        lastRxMs = System.currentTimeMillis();
                    }
                    handleMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    synchronized (NeurProtocolClient.this) {
                        sessionStarted = false;
                    }
                    listener.onSocketClose("code=" + code + ", reason=" + reason);
                }

                @Override
                public void onError(Exception ex) {
                    String msg = ex == null ? "WebSocket error" : ex.getMessage();
                    setLastError(msg == null ? "WebSocket error" : msg);
                    listener.onSocketError(getLastError());
                }
            };
            socket.connect();
        } catch (Exception e) {
            String msg = e.getMessage() == null ? "Connect failed" : e.getMessage();
            setLastError(msg);
            listener.onSocketError(msg);
        }
    }

    private void handleMessage(String message) {
        try {
            JSONObject json = new JSONObject(message);
            String type = json.optString("type", "");
            long tsMs = toEpochMs(json.optDouble("ts", 0.0));
            JSONObject payload = json.optJSONObject("payload");

            if ("brain.point".equals(type) && payload != null) {
                listener.onPoint(payload, tsMs);
            } else if ("brain.patch".equals(type) && payload != null) {
                listener.onPatch(payload, tsMs);
            } else if ("brain.snapshot".equals(type) && payload != null) {
                listener.onSnapshot(payload, tsMs);
            } else if ("brain.params".equals(type) && payload != null) {
                listener.onParams(payload, tsMs);
            } else if ("server.error".equals(type)) {
                String serverMsg = payload != null ? payload.toString() : json.toString();
                listener.onServerError(serverMsg);
            }
        } catch (Exception e) {
            listener.onSocketError("Parse failed: " + (e.getMessage() == null ? "unknown" : e.getMessage()));
        }
    }

    private boolean sendControl(String action) {
        if (socket == null || !socket.isOpen()) {
            return false;
        }
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "session.control");
            msg.put("session_id", sessionId);
            msg.put("seq", upSeq.incrementAndGet());
            msg.put("ts", nowEpochSec());

            JSONObject payload = new JSONObject();
            payload.put("action", action);
            msg.put("payload", payload);

            socket.send(msg.toString());
            lastTxMs = System.currentTimeMillis();
            return true;
        } catch (Exception e) {
            setLastError("send control failed: " + safeMessage(e));
            listener.onSocketError(lastError);
            return false;
        }
    }

    private void sendEegRaw(int nSamples) {
        if (socket == null || !socket.isOpen()) {
            return;
        }
        try {
            JSONObject msg = new JSONObject();
            msg.put("type", "eeg.raw");
            msg.put("session_id", sessionId);
            msg.put("seq", upSeq.incrementAndGet());
            msg.put("ts", nowEpochSec());

            JSONObject payload = new JSONObject();
            payload.put("fs", REQUIRED_FS);

            JSONArray chNames = new JSONArray();
            for (String ch : REQUIRED_CHANNELS) {
                chNames.put(ch);
            }
            payload.put("channels", chNames);

            JSONArray samples = new JSONArray();
            for (int ch = 0; ch < 8; ch++) {
                samples.put(new JSONArray());
            }

            for (int i = 0; i < nSamples && !sampleQueue.isEmpty(); i++) {
                double[] v = sampleQueue.poll();
                for (int ch = 0; ch < 8; ch++) {
                    samples.getJSONArray(ch).put(v[ch]);
                }
            }
            payload.put("samples", samples);

            msg.put("payload", payload);
            socket.send(msg.toString());
            lastTxMs = System.currentTimeMillis();
        } catch (Exception e) {
            setLastError("send eeg.raw failed: " + safeMessage(e));
            listener.onSocketError(lastError);
        }
    }

    private void closeSocket() {
        WebSocketClient s = socket;
        socket = null;
        if (s == null) {
            return;
        }
        try {
            ReadyState rs = s.getReadyState();
            if (rs == ReadyState.OPEN || rs == ReadyState.NOT_YET_CONNECTED || rs == ReadyState.CLOSING) {
                s.close();
            }
        } catch (Exception ignored) {
        }
    }

    private static double nowEpochSec() {
        return System.currentTimeMillis() / 1000.0;
    }

    private static long toEpochMs(double tsSec) {
        if (tsSec <= 0) {
            return System.currentTimeMillis();
        }
        return (long) (tsSec * 1000.0);
    }

    private static String safeMessage(Exception e) {
        String msg = e.getMessage();
        return msg == null ? e.getClass().getSimpleName().toLowerCase(Locale.US) : msg;
    }

    private synchronized void setLastError(String err) {
        lastError = err == null ? "" : err;
    }
}
