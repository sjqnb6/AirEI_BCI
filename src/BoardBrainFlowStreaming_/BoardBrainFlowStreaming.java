package BoardBrainFlowStreaming_;

import BoardBrainflow_.BoardBrainFlow;
import Globel.GUI;
import PacketLossTracker_.PacketLossTracker;
import PacketLossTracker_.PacketLossTrackerCytonSerialDaisy;
import PopupMessage_.PopupMessage;
import SerialParser_.CytonSerialParser;
import brainflow.BoardIds;
import brainflow.BoardShim;
import brainflow.BrainFlowError;
import brainflow.BrainFlowInputParams;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static Debugging_.GF.outputError;
import static Globel.GUI.directoryManager;
import static Globel.GUI.topNav;
import static SystemManager.GF.stopRunning;
import static processing.core.PApplet.println;


public class BoardBrainFlowStreaming extends BoardBrainFlow {
    GUI MAIN;
    private BoardIds masterBoardId;
    private String ipAddress;
    private int ipPort;

    public BoardBrainFlowStreaming(GUI MAIN, BoardIds masterBoardId, String ipAddress, int ipPort) {
        super(MAIN);
        this.MAIN = MAIN;
        this.masterBoardId = masterBoardId;
        this.ipAddress = ipAddress;
        this.ipPort = ipPort;
    }

    // implement mandatory abstract functions
    @Override
    protected BrainFlowInputParams getParams() {
        BrainFlowInputParams params = new BrainFlowInputParams();
        params.ip_address = ipAddress;
        params.ip_port = ipPort;
        params.master_board = masterBoardId.get_code();
        return params;
    }

    // for streaming board need to use master board id in function like  get_eeg_channels
    @Override
    public BoardIds getBoardId() {
        return masterBoardId;
    }

    @Override
    public boolean initializeInternal() {
        if(getBoardIdInt() == 0 ) {
            //parser = new CytonSerialParser(20000);
            return true;
        }
        try {
            // here we need to provide board id of streaming board
            boardShim = new BoardShim (BoardIds.STREAMING_BOARD.get_code(), getParams());
            try {
                BoardShim.enable_dev_board_logger();
                BoardShim.set_log_file(directoryManager.getConsoleDataPath() + "Brainflow_" +
                        directoryManager.getFileNameDateTime() + ".txt");
            } catch (BrainFlowError e) {
                e.printStackTrace();
            }
            boardShim.prepare_session();
            return true;

        } catch (Exception e) {
            boardShim = null;
            outputError("ERROR: " + e + " when initializing Brainflow board. Data will not stream.");
            e.printStackTrace();
            return false;
        }
    }
    public void startStreaming() {
        //super.startStreaming();

        println("Brainflow start streaming");


        if(streaming) {
            println("Already streaming, do nothing");
            return;
        }

        try {
            //sendToSerialPort("COM5", 'b');
            //-----------------------------------------------------------------------
            //-----------------------------------------------------------------------
            //-----------------------------------------------------------------------
            //parser.start_stream(MAIN.openBCI_portName, 921600);
            streaming = true;
            //-----------------------------------------------------------------------IOException e
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    protected double[][] getNewDataInternal() {
        if(getBoardIdInt() != 0) return getNewDataInternal1();
        if(streaming) {
            //-----------------------------------------------------------------------
            //double[][] data = parser.get_data();
            double[][] data = fetchDataFromNetwork();
            if ((data[0].length == 0) && (time_last_datapoint > 0)) {
                double cur_time = System.currentTimeMillis() / 1000L;
                double timeout = 5.0;
                if (cur_time - time_last_datapoint > timeout) {
                    if (data_popup_displayed == false) {
                        PopupMessage msg = new PopupMessage(MAIN,"数据流错误",
                                "没有接收到数据累计 " + timeout + " 秒. 请检查设备并且重启连接.");
                    }
                    outputError("没有接收到数据，累计 " + timeout + " 秒. 请检查设备并且重启连接");
                    data_popup_displayed = true;
                    stopRunning(MAIN);
                    topNav.resetStartStopButton();
                }
            } else {
                time_last_datapoint = System.currentTimeMillis() / 1000L;
                data_popup_displayed = false;
            }
            return data;
        }

        return emptyData;
    }
    /**
     * 从网络请求获取脑电数据
     * @return 二维数组，格式为 [channel][sample]
     */
    private double[][] fetchDataFromNetwork() {
        try {
            URL url = new URL("http://127.0.0.1:8000/api/data/latest");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                println("WARNING: Network request failed with code: " + responseCode);
                return emptyData;
            }

            // 读取响应
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // 解析 JSON
            JSONObject jsonResponse = new JSONObject(response.toString());

            if (!jsonResponse.getBoolean("success")) {
                println("WARNING: Server returned success=false");
                return emptyData;
            }

            JSONObject dataObj = jsonResponse.getJSONObject("data");
            JSONArray samplesArray = dataObj.getJSONArray("samples");
            int samplesCount = dataObj.getInt("samplesCount");

            // 转换为二维数组格式 [channel][sample]
            //int channelCount = samplesArray.length();
            int channelCount = 30;
            double[][] data = new double[channelCount][samplesCount];
            //System.out.println(samplesCount);
            //System.out.println("---------------------------------------");
            for (int ch = 0; ch < 8; ch++) {
                JSONArray channelData = samplesArray.getJSONArray(ch);
                for (int sample = 0; sample < samplesCount; sample++) {
                    data[ch][sample] = channelData.getDouble(sample);
                }
            }

            return data;

        } catch (IOException e) {
            println("WARNING: Network IO error: " + e.getMessage());
            e.printStackTrace();
            return emptyData;
        } catch (Exception e) {
            println("WARNING: Error fetching data from network: " + e.getMessage());
            e.printStackTrace();
            return emptyData;
        }
    }

    @Override
    public void setEXGChannelActive(int channelIndex, boolean active) {
        // do nothing here
    }

    @Override
    public boolean isEXGChannelActive(int channelIndex) {
        return true;
    }

    @Override
    protected void addChannelNamesInternal(String[] channelNames) {
        // do nothing here
    }

    @Override
    protected PacketLossTracker setupPacketLossTracker() {
        if (masterBoardId == BoardIds.CYTON_DAISY_BOARD) {
            return new PacketLossTrackerCytonSerialDaisy(getSampleIndexChannel(), getTimestampChannel());
        }
        final int minSampleIndex = 0;
        final int maxSampleIndex = 255;
        return new PacketLossTracker(getSampleIndexChannel(), getTimestampChannel(),
                minSampleIndex, maxSampleIndex);
    }

};
