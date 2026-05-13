package PythonIntegration_;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.util.LinkedList;

public class PythonWsClient {
    private static final int DEFAULT_CHANNEL_COUNT = 8;
    private static final int WINDOW_SIZE = 128;
    private static final int HOP_SIZE = 64;

    private static PythonWsClient instance;

    private WebSocketClient webSocketClient;
    private final LinkedList<double[]> eegBuffer = new LinkedList<>();

    private volatile boolean wsConnected = false;
    private volatile String lastError = "";

    private long sequenceId = 1000;
    private volatile long lastInferenceSequenceId = 0;
    private volatile long lastInferenceTimestampMs = 0;
    private volatile double latestInferenceLatencyMs = -1;

    private volatile long lastSentSequenceId = 0;
    private volatile long lastSentTimestampMs = 0;

    private volatile String modelType = "EEGNet";

    private volatile double class0Prob = 0.0;
    private volatile double class1Prob = 0.0;
    private volatile double class2Prob = 0.0;
    private volatile double fatigueScore = 0.0;

    public static PythonWsClient getInstance() {
        if (instance == null) {
            instance = new PythonWsClient();
        }
        return instance;
    }

    private PythonWsClient() {
        connect();
    }

    private void connect() {
        try {
            webSocketClient = new WebSocketClient(new URI("ws://127.0.0.1:8000/ws/eeg")) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    wsConnected = true;
                    lastError = "";
                    System.out.println("Connected to Python FastAPI WebSocket");
                }

                @Override
                public void onMessage(String message) {
                    try {
                        JSONObject json = new JSONObject(message);
                        long nowMs = System.currentTimeMillis();

                        if (json.has("sequence_id")) {
                            lastInferenceSequenceId = json.optLong("sequence_id", lastInferenceSequenceId);
                            if (lastInferenceSequenceId == lastSentSequenceId && lastSentTimestampMs > 0) {
                                latestInferenceLatencyMs = nowMs - lastSentTimestampMs;
                            }
                        }

                        if (json.has("latency_ms")) {
                            latestInferenceLatencyMs = json.optDouble("latency_ms", latestInferenceLatencyMs);
                        }

                        if (json.has("probabilities")) {
                            parseProbabilities(json.get("probabilities"));
                        }

                        if (json.has("fatigue_score")) {
                            double rawScore = json.optDouble("fatigue_score", fatigueScore);
                            // Accept both normalized score (0-1) and percentage score (0-100).
                            if (rawScore <= 1.0) {
                                fatigueScore = clamp01(rawScore) * 100.0;
                            } else {
                                fatigueScore = Math.max(0.0, Math.min(100.0, rawScore));
                            }
                        } else {
                            fatigueScore = computeFatigueScoreFromProbs(class0Prob, class1Prob, class2Prob);
                        }

                        if (json.has("model_type")) {
                            modelType = json.optString("model_type", modelType);
                        }

                        lastInferenceTimestampMs = nowMs;
                    } catch (Exception e) {
                        e.printStackTrace();
                        lastError = "Parse message failed: " + e.getMessage();
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    wsConnected = false;
                    System.out.println("WebSocket closed: " + reason);
                }

                @Override
                public void onError(Exception ex) {
                    wsConnected = false;
                    lastError = ex.getMessage() == null ? "WebSocket error" : ex.getMessage();
                    System.err.println("WebSocket error: " + lastError);
                }
            };
            webSocketClient.connect();
        } catch (Exception e) {
            lastError = e.getMessage() == null ? "Connect failed" : e.getMessage();
            e.printStackTrace();
        }
    }

    public synchronized void pushData(double[][] dataThisFrame) {
        if (dataThisFrame == null || dataThisFrame.length < 2 || dataThisFrame[0].length == 0) {
            return;
        }

        int channelCount = Math.min(DEFAULT_CHANNEL_COUNT, Math.max(0, dataThisFrame.length - 1));
        if (channelCount == 0) {
            return;
        }

        for (int i = 0; i < dataThisFrame[0].length; i++) {
            double[] sample = new double[channelCount];
            for (int ch = 0; ch < channelCount; ch++) {
                int sourceIndex = ch + 1;
                if (sourceIndex < dataThisFrame.length) {
                    sample[ch] = dataThisFrame[sourceIndex][i];
                } else {
                    sample[ch] = 0.0;
                }
            }
            eegBuffer.add(sample);
        }

        while (eegBuffer.size() >= WINDOW_SIZE) {
            if (webSocketClient != null && webSocketClient.isOpen()) {
                try {
                    long nowMs = System.currentTimeMillis();
                    long seq = ++sequenceId;

                    JSONObject payload = new JSONObject();
                    payload.put("protocol_version", 1);
                    payload.put("sequence_id", seq);
                    payload.put("client_timestamp_ms", nowMs);
                    payload.put("model_type", modelType);
                    payload.put("window_size", WINDOW_SIZE);
                    payload.put("hop_size", HOP_SIZE);
                    payload.put("channel_count", channelCount);
                    payload.put("class_count", 3);

                    JSONArray classLabels = new JSONArray();
                    classLabels.put("alert");
                    classLabels.put("fatigue");
                    classLabels.put("drowsy");
                    payload.put("class_labels", classLabels);

                    JSONArray eegDataJson = new JSONArray();
                    for (int ch = 0; ch < channelCount; ch++) {
                        JSONArray chData = new JSONArray();
                        for (int s = 0; s < WINDOW_SIZE; s++) {
                            chData.put(eegBuffer.get(s)[ch]);
                        }
                        eegDataJson.put(chData);
                    }
                    payload.put("eeg_data", eegDataJson);

                    lastSentSequenceId = seq;
                    lastSentTimestampMs = nowMs;

                    webSocketClient.send(payload.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                    lastError = "Send failed: " + e.getMessage();
                    reconnectIfClosed();
                }
            } else {
                reconnectIfClosed();
            }

            int hop = Math.min(HOP_SIZE, eegBuffer.size());
            for (int i = 0; i < hop; i++) {
                eegBuffer.poll();
            }
        }
    }

    private void reconnectIfClosed() {
        if (webSocketClient == null || webSocketClient.isClosed()) {
            System.out.println("WebSocket closed. Reconnecting...");
            connect();
        }
    }

    private void parseProbabilities(Object probabilitiesObj) {
        if (probabilitiesObj instanceof JSONObject) {
            JSONObject probs = (JSONObject) probabilitiesObj;
            class0Prob = probs.optDouble("class0", class0Prob);
            class1Prob = probs.optDouble("class1", class1Prob);
            class2Prob = probs.optDouble("class2", class2Prob);
        } else if (probabilitiesObj instanceof JSONArray) {
            JSONArray probs = (JSONArray) probabilitiesObj;
            if (probs.length() > 0) class0Prob = probs.optDouble(0, class0Prob);
            if (probs.length() > 1) class1Prob = probs.optDouble(1, class1Prob);
            if (probs.length() > 2) class2Prob = probs.optDouble(2, class2Prob);
        }

        double[] normalized = normalizeProbs(class0Prob, class1Prob, class2Prob);
        class0Prob = normalized[0];
        class1Prob = normalized[1];
        class2Prob = normalized[2];
    }

    private static double computeFatigueScoreFromProbs(double p0, double p1, double p2) {
        double[] probs = normalizeProbs(p0, p1, p2);
        return 100.0 * (0.0 * probs[0] + 0.5 * probs[1] + 1.0 * probs[2]);
    }

    private static double[] normalizeProbs(double p0, double p1, double p2) {
        double c0 = clamp01(p0);
        double c1 = clamp01(p1);
        double c2 = clamp01(p2);
        double sum = c0 + c1 + c2;
        if (sum <= 0.0) {
            return new double[]{0.0, 0.0, 0.0};
        }
        return new double[]{c0 / sum, c1 / sum, c2 / sum};
    }

    private static double clamp01(double value) {
        if (value < 0.0) return 0.0;
        if (value > 1.0) return 1.0;
        return value;
    }

    public double[] getLatestProbabilities() {
        return new double[]{class0Prob, class1Prob, class2Prob};
    }

    public double getLatestFatigueScore() {
        return fatigueScore;
    }

    public double getLatestInferenceLatencyMs() {
        return latestInferenceLatencyMs;
    }

    public long getLastInferenceTimestampMs() {
        return lastInferenceTimestampMs;
    }

    public long getLastInferenceSequenceId() {
        return lastInferenceSequenceId;
    }

    public boolean isConnected() {
        return wsConnected;
    }

    public String getLastError() {
        return lastError;
    }

    public String getModelType() {
        return modelType;
    }

    public void setModelType(String modelType) {
        if (modelType != null && !modelType.trim().isEmpty()) {
            this.modelType = modelType;
        }
    }
}
