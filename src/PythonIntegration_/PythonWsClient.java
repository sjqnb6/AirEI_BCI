package PythonIntegration_;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.util.LinkedList;

public class PythonWsClient {
    private static PythonWsClient instance;
    private WebSocketClient webSocketClient;
    private LinkedList<double[]> eegBuffer = new LinkedList<>();
    private long sequenceId = 1000;
    private String modelType = "EEGNet";
    
    private double class0Prob = 0.0;
    private double class1Prob = 0.0;
    private double class2Prob = 0.0;

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
            // 注意这里的 URI 路径必须和 Python 端一致，强制使用 127.0.0.1 阻止 Windows IPv6 解析问题
            webSocketClient = new WebSocketClient(new URI("ws://127.0.0.1:8000/ws/eeg")) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("成功连接到 Python FastAPI WebSocket");
                }

                @Override
                public void onMessage(String message) {
                    try {
                        JSONObject json = new JSONObject(message);
                        if (json.has("probabilities")) {
                            JSONObject probs = json.getJSONObject("probabilities");
                            // 存入最新的概率分布供可视化显示
                            class0Prob = probs.optDouble("class0", 0.0);
                            class1Prob = probs.optDouble("class1", 0.0);
                            class2Prob = probs.optDouble("class2", 0.0);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("WebSocket 连接关闭: " + reason);
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("WebSocket 错误: " + ex.getMessage());
                }
            };
            webSocketClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void pushData(double[][] dataThisFrame) {
        if (dataThisFrame == null || dataThisFrame.length < 9 || dataThisFrame[0].length == 0) return;
        
        // 解析当前帧数据 (只需要通道 1-8)
        for (int i = 0; i < dataThisFrame[0].length; i++) {
            double[] sample = new double[8];
            for (int ch = 0; ch < 8; ch++) {
                if (ch + 1 < dataThisFrame.length) {
                    sample[ch] = dataThisFrame[ch + 1][i]; 
                } else {
                    sample[ch] = 0.0;
                }
            }
            eegBuffer.add(sample);
        }

        // 当缓冲区达到 128 个数据时发送
        while (eegBuffer.size() >= 128) {
            if (webSocketClient != null && webSocketClient.isOpen()) {
                try {
                    JSONObject payload = new JSONObject();
                    payload.put("sequence_id", ++sequenceId);
                    payload.put("timestamp", System.currentTimeMillis());
                    payload.put("model_type", modelType);
                    
                    JSONArray eegDataJson = new JSONArray();
                    for (int ch = 0; ch < 8; ch++) {
                        JSONArray chData = new JSONArray();
                        for (int s = 0; s < 128; s++) {
                            chData.put(eegBuffer.get(s)[ch]);
                        }
                        eegDataJson.put(chData);
                    }
                    payload.put("eeg_data", eegDataJson);
                    
                    webSocketClient.send(payload.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                    // 发生发送异常时尝试重连
                    reconnectIfClosed();
                }
            } else {
                // 如果连接未打开（比如刚才 Python 端重启断开了），尝试重新连接
                reconnectIfClosed();
            }
            
            // 步长为 64，意味着我们需要保留最后 64 个数据，丢弃前 64 个
            for (int i = 0; i < 64; i++) {
                eegBuffer.poll();
            }
        }
    }
    
    private void reconnectIfClosed() {
        if (webSocketClient == null || webSocketClient.isClosed()) {
            System.out.println("检测到 WebSocket 连接关闭，尝试重新连接...");
            connect();
        }
    }
    
    public double[] getLatestProbabilities() {
        return new double[]{class0Prob, class1Prob, class2Prob};
    }
    
    public void setModelType(String modelType) {
        this.modelType = modelType;
    }
}
