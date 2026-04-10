package BoardBrainflow_;

import com.fazecast.jSerialComm.SerialPort;
import Board_.Board;
import Globel.GUI;
import PopupMessage_.PopupMessage;
import SerialParser_.CytonSerialParser;
import brainflow.BoardIds;
import brainflow.BoardShim;
import brainflow.BrainFlowError;
import brainflow.BrainFlowInputParams;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import com.fazecast.jSerialComm.SerialPort;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SortedSet;
import java.util.TreeSet;

import static Debugging_.GF.outputError;
import static Globel.GUI.topNav;
import static SystemManager.GF.stopRunning;
import static processing.core.PApplet.println;

public abstract class BoardBrainFlow extends Board {
    GUI MAIN;
    protected CytonSerialParser parser = null;
    protected BoardShim boardShim = null;
    protected int samplingRateCache = -1;
    protected int sampleIndexChannelCache = -1;
    protected int timeStampChannelCache = -1;
    protected int totalChannelsCache = -1;
    protected int markerChannelCache = -1;
    protected int[] exgChannelsCache = null;
    protected int[] otherChannelsCache = null;

    protected boolean streaming = false;
    protected double time_last_datapoint = -1.0;
    protected boolean data_popup_displayed = false;

    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    /* Abstract Functions.
     * Implement these in your board.
     */
    abstract protected BrainFlowInputParams getParams();
    abstract public BoardIds getBoardId();

    public BoardBrainFlow(GUI MAIN) {
        super(MAIN);
        this.MAIN = MAIN;
    }

    public boolean initializeInternal1() {
        try {
            boardShim = new BoardShim (getBoardIdInt(), getParams());
            try {
                BoardShim.enable_dev_board_logger();
                BoardShim.set_log_file(MAIN.directoryManager.getConsoleDataPath() + "Brainflow_" +
                        MAIN.directoryManager.getFileNameDateTime() + ".txt");
            } catch (BrainFlowError e) {
                e.printStackTrace();
            }
            boardShim.prepare_session();
            /*
            //This does not seem to work with Windows and Processing.
            //For now, we will add a streamer using argument for start_stream(). -RW 9/18/2023
            if (brainflowStreamer != "")
                boardShim.add_streamer(brainflowStreamer);
            */
            return true;

        } catch (Exception e) {
            boardShim = null;
            outputError("ERROR: " + e + " when initializing Brainflow board. Data will not stream.");
            e.printStackTrace();
            return false;
        }
    }
    public boolean initializeInternal() {
        try {
            if(getBoardIdInt() == 0 )parser = new CytonSerialParser(20000);
            else{
                boardShim = new BoardShim (getBoardIdInt(), getParams());
                try {
                    BoardShim.enable_dev_board_logger();
                    BoardShim.set_log_file(MAIN.directoryManager.getConsoleDataPath() + "Brainflow_" +
                            MAIN.directoryManager.getFileNameDateTime() + ".txt");
                } catch (BrainFlowError e) {
                    e.printStackTrace();
                }
                boardShim.prepare_session();
            }
            /*
            //This does not seem to work with Windows and Processing.
            //For now, we will add a streamer using argument for start_stream(). -RW 9/18/2023
            if (brainflowStreamer != "")
                boardShim.add_streamer(brainflowStreamer);
            */
            return true;

        } catch (Exception e) {
            boardShim = null;
            outputError("错误: " + e + " 未能初始化数据版,没有数据流产生");
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void uninitializeInternal() {
        if(isConnected()) {
            parser.close();
        }
    }

    @Override
    public void updateInternal() {
        // empty
    }


    // 开始进行数据流传输
    public void startStreaming1() {
        super.startStreaming();

        println("Brainflow start streaming");
        if(streaming) {
            println("Already streaming, do nothing");
            return;
        }

        try {
            //sendToSerialPort("COM5", 'b');
            boardShim.start_stream (450000, MAIN.brainflowStreamer);
            streaming = true;
        }
        catch (BrainFlowError e) {
            println("ERROR: Exception when starting stream");
            e.printStackTrace();
            streaming = false;
        }
    }

    @Override
    public void startStreaming() {
        super.startStreaming();

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
            parser.start_stream(MAIN.openBCI_portName, 921600);
            streaming = true;
            //-----------------------------------------------------------------------IOException e
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public void stopStreaming1() {
        super.stopStreaming();

        println("Brainflow stop streaming");
        if(!streaming) {
            println("Already stopped streaming, do nothing");
            return;
        }
        try {
            boardShim.stop_stream();
            streaming = false;
            time_last_datapoint = -1.0;
        }
        catch (BrainFlowError e) {
            outputError("错误：停止数据流时出现异常，请重启主板和会话");
            e.printStackTrace();
            //If no data was received in X seconds, there is a serious problem with communications. Go ahead and stop trying to collect data.
            //Prevents feedback loop of errors.
            if (data_popup_displayed) {
                streaming = false;
            }
        }

        if (MAIN.eegDataSource != MAIN.DATASOURCE_PLAYBACKFILE && MAIN.eegDataSource != MAIN.DATASOURCE_STREAMING) {
            MAIN.dataLogger.fileWriterBF.incrementBrainFlowStreamerFileNumber();
        }
    }
    public void stopStreaming() {
        if(getBoardIdInt() != 0) {
            stopStreaming1();
            return ;
        }
        super.stopStreaming();

        println("Brainflow stop streaming");
        if(!streaming) {
            println("Already stopped streaming, do nothing");
            return;
        }
        if (parser != null)parser.stop_stream();
        streaming = false;
        time_last_datapoint = -1.0;

        if (MAIN.eegDataSource != MAIN.DATASOURCE_PLAYBACKFILE && MAIN.eegDataSource != MAIN.DATASOURCE_STREAMING) {
            MAIN.dataLogger.fileWriterBF.incrementBrainFlowStreamerFileNumber();
        }
    }

    public boolean isConnected1() {
        if (boardShim != null) {
            try {
                return boardShim.is_prepared();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return false;
    }
    @Override
    public boolean isConnected() {
        if (parser != null) {
            try {
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return false;
    }
    @Override
    public boolean isStreaming() {
        return streaming;
    }

    @Override
    public int getSampleRate() {
        if(samplingRateCache < 0) {
            try {
                samplingRateCache = BoardShim.get_sampling_rate(getBoardIdInt());
            } catch (BrainFlowError e) {
                println("WARNING: failed to get sample rate from BoardShim");
                e.printStackTrace();
            }
        }

        return samplingRateCache;
    }

//    @Override
//    public int[] getEXGChannels() {
//        if(exgChannelsCache == null) {
//            int[] channels;
//            // for some boards there can be duplicates
//            SortedSet<Integer> set = new TreeSet<Integer>();
//            // maybe it will be nice to add method like get_exg_channels to brainflow to avoid this ugly code?
//            // but I doubt that smth else will need it and in python I know how to implement it better using existing API
//            try {
//                channels = BoardShim.get_eeg_channels(getBoardIdInt());
//                for(int i = 0; i < channels.length; i++) {
//                    set.add(channels[i]);
//                }
//            } catch (BrainFlowError e) {
//                println("WARNING: failed to get eeg channels from BoardShim");
//            }
//            try {
//                channels = BoardShim.get_emg_channels(getBoardIdInt());
//                for(int i = 0; i < channels.length; i++) {
//                    set.add(channels[i]);
//                }
//            } catch (BrainFlowError e) {
//                println("WARNING: failed to get emg channels from BoardShim");
//            }
//            try {
//                channels = BoardShim.get_ecg_channels(getBoardIdInt());
//                for(int i = 0; i < channels.length; i++) {
//                    set.add(channels[i]);
//                }
//            } catch (BrainFlowError e) {
//                println("WARNING: failed to get ecg channels from BoardShim");
//            }
//            try {
//                channels = BoardShim.get_eog_channels(getBoardIdInt());
//                for(int i = 0; i < channels.length; i++) {
//                    set.add(channels[i]);
//                }
//            } catch (BrainFlowError e) {
//                println("WARNING: failed to get eog channels from BoardShim");
//            }
//            Integer[] toArray = set.toArray(new Integer[set.size()]);
//            exgChannelsCache = new int[toArray.length];
//            for (int i = 0; i < toArray.length; i++) {
//                exgChannelsCache[i] = toArray[i].intValue();
//            }
//        }
//
//        return exgChannelsCache;
//    }
public int[] getEXGChannels() {

    return new int[]{0, 1, 2, 3, 4, 5, 6, 7};
}
//    @Override
//    public int getTimestampChannel() {
//        if(timeStampChannelCache < 0) {
//            try {
//                timeStampChannelCache = BoardShim.get_timestamp_channel(getBoardIdInt());
//            } catch (BrainFlowError e) {
//                println("WARNING: failed to get timestamp channel from BoardShim");
//                e.printStackTrace();
//            }
//        }
//
//        return timeStampChannelCache;
//    }
    @Override
    public int getTimestampChannel() {
        return 10;
//        if(timeStampChannelCache < 0) {
//            try {
//                timeStampChannelCache = BoardShim.get_timestamp_channel(getBoardIdInt());
//            } catch (BrainFlowError e) {
//                println("WARNING: failed to get timestamp channel from BoardShim");
//                e.printStackTrace();
//            }
//        }
//
//        return timeStampChannelCache;
    }

    @Override
    public int getSampleIndexChannel() {
        if(sampleIndexChannelCache < 0) {
            try {
                sampleIndexChannelCache = BoardShim.get_package_num_channel(getBoardIdInt());
            } catch (BrainFlowError e) {
                println("WARNING: failed to get package num channel from BoardShim");
                e.printStackTrace();
            }
        }

        return sampleIndexChannelCache;
    }

    public int getBoardIdInt() {
        return getBoardId().get_code();
    }

    @Override
    public Pair<Boolean, String> sendCommand(String command) {
        if (command != null && isConnected()) {
            try {
                println("Sending config string to board: " + command);
                String resp = boardShim.config_board(command);
                return new ImmutablePair<Boolean, String>(Boolean.valueOf(true), resp);
            }
            catch (BrainFlowError e) {
                outputError("错误: " + e + " 发送指令: " + command);
                e.printStackTrace();
                return new ImmutablePair<Boolean, String>(Boolean.valueOf(false), "");
            }
        }
        return new ImmutablePair<Boolean, String>(Boolean.valueOf(false), "");
    }


    protected double[][] getNewDataInternal1() {
        if(streaming) {
            try {
                double[][] data = boardShim.get_board_data();
                if ((data[0].length == 0) && (time_last_datapoint > 0)) {
                    double cur_time = System.currentTimeMillis() / 1000L;
                    double timeout = 5.0;
                    if (cur_time - time_last_datapoint > timeout) {
                        if (data_popup_displayed == false) {
                            PopupMessage msg = new PopupMessage(MAIN,"数据流错误",
                                    "没有接收到数据持续" + timeout + " 秒. 请检查设备并且重启连接.");
                        }
                        outputError("数据流错误：未收到新数据持续 " + timeout + " 秒. 请检查您的设备并重启GUI会话.");
                        data_popup_displayed = true;
                        stopRunning(MAIN);
                        topNav.resetStartStopButton();
                    }
                } else {
                    time_last_datapoint = System.currentTimeMillis() / 1000L;
                    data_popup_displayed = false;
                }
                return data;
            } catch (BrainFlowError e) {
                println("WARNING: could not get board data.");
                e.printStackTrace();
            }
        }

        return emptyData;
    }
//    protected double[][] getNewDataInternal() {
//        if(getBoardIdInt() != 0) return getNewDataInternal1();
//        if(streaming) {
//            double[][] data = fetchDataFromNetwork();
//            if ((data[0].length == 0) && (time_last_datapoint > 0)) {
//                double cur_time = System.currentTimeMillis() / 1000L;
//                double timeout = 5.0;
//                if (cur_time - time_last_datapoint > timeout) {
//                    if (data_popup_displayed == false) {
//                        PopupMessage msg = new PopupMessage(MAIN,"数据流错误",
//                                "没有接收到数据累计 " + timeout + " 秒. 请检查设备并且重启连接.");
//                    }
//                    outputError("没有接收到数据，累计 " + timeout + " 秒. 请检查设备并且重启连接");
//                    data_popup_displayed = true;
//                    stopRunning(MAIN);
//                    topNav.resetStartStopButton();
//                }
//            } else {
//                time_last_datapoint = System.currentTimeMillis() / 1000L;
//                data_popup_displayed = false;
//            }
//            return data;
//        }
//
//        return emptyData;
//    }

    protected double[][] getNewDataInternal() {
        if(getBoardIdInt() != 0) return getNewDataInternal1();
        if(streaming) {
            //-----------------------------------------------------------------------
            double[][] data = parser.get_data();
            //double[][] data = fetchDataFromNetwork();
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
    public int getTotalChannelCount() {
        if(totalChannelsCache < 0) {
            try {
                totalChannelsCache = BoardShim.get_num_rows(getBoardIdInt());
            } catch (BrainFlowError e) {
                println("WARNING: failed to get num rows from BoardShim");
                e.printStackTrace();
            }
        }

        return totalChannelsCache;
    }

    protected int[] getOtherChannels() {
        if (otherChannelsCache == null) {
            try {
                otherChannelsCache = BoardShim.get_other_channels(getBoardIdInt());
            } catch (BrainFlowError e) {
                e.printStackTrace();
            }
        }

        return otherChannelsCache;
    }

    @Override
    public int getMarkerChannel() {
        if (markerChannelCache < 0) {
            try {
                markerChannelCache = BoardShim.get_marker_channel(getBoardIdInt());
            } catch (BrainFlowError e) {
                e.printStackTrace();
            }
        }

        return markerChannelCache;
    }

    @Override
    public void insertMarker(double value) {
        if (isConnected() && streaming) {
            try {
                boardShim.insert_marker(value);
                String currentTimeString = dateFormat.format(new Date());
                StringBuilder markerNotification = new StringBuilder("Inserted marker ");
                markerNotification.append(value);
                markerNotification.append(" at approximately: ");
                markerNotification.append(currentTimeString);
                println(markerNotification.toString());
            } catch (BrainFlowError e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void insertMarker(int value) {
        insertMarker((double) value);
    }
    private void sendToSerialPort(String portName, char c) {
        SerialPort[] ports = SerialPort.getCommPorts();
        SerialPort targetPort = null;

        // 查找指定的串口
        for (SerialPort port : ports) {
            if (port.getSystemPortName().equals(portName)) {
                targetPort = port;
                break;
            }
        }

        if (targetPort != null) {
            try {
                // 打开串口（波特率115200，数据位8，停止位1，无奇偶校验）
                if (!targetPort.isOpen()) {
                    targetPort.setComPortParameters(115200, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
                    targetPort.openPort();
                }

                // 发送字符
                byte[] buffer = new byte[] { (byte) c };
                int bytesWritten = targetPort.writeBytes(buffer, 1);

                if (bytesWritten > 0) {
                    println("Successfully sent character '" + c + "' to " + portName);
                } else {
                    println("Failed to send character '" + c + "' to " + portName);
                }
            } catch (Exception e) {
                println("Error sending to serial port: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            println("Serial port " + portName + " not found");
        }
    }
};