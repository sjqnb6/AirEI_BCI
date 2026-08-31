package DataSourcePlayback_;

import Board_.Board;
import DataSource_.DataSource;
import FileBoard_.FileBoard;
import GUI.GUIManager;
import SerialParser_.CytonWifiHealthFrame;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static Debugging_.GF.outputError;
import static Debugging_.GF.outputWarn;
import static Globel.GUI.topNav;
import static SystemManager.GF.updateToNChan;
import Globel.GUI;
import BoardCyton_.BoardCytonSerial;

public abstract class DataSourcePlayback implements DataSource, FileBoard {
    GUI MAIN;
    private String playbackFilePathExg;
    private ArrayList<double[]> rawDataExg;
    private ArrayList<PlaybackHealthRecord> rawDataHealth;
    private int currentSampleExg;
    private int currentHealthIndex = -1;
    private int timeOfLastUpdateMSExg;
    private String underlyingClassName;
    private String playbackFilePathHealth = "";
    private int numNewSamplesThisFrameExg;

    private boolean initialized = false;
    private boolean streaming = false;

    public Board underlyingBoard = null;
    private int sampleRateExg = -1;
    private int numChannelsExg = 0;  // use it instead getTotalChannelCount() method for old playback files

    protected DataSourcePlayback(GUI MAIN, String filePath) {
        this.MAIN = MAIN;

        playbackFilePathExg = filePath;
    }

    @Override
    public boolean initialize() {
        currentSampleExg = 0;
        String[] lines = MAIN.loadStrings(playbackFilePathExg);

        if(!parseExgHeader(lines)) {
            return false;
        }
        if(!instantiateUnderlyingBoard()) {
            return false;
        }
        if(!parseExgData(lines)) {
            return false;
        }
        parseHealthData();

        return true;
    }

    @Override
    public void uninitialize() {
        initialized = false;
    }

    protected boolean parseExgHeader(String[] lines) {
        for (String line : lines) {
            if (!line.startsWith("%")) {
                break; // reached end of header
            }

            //only needed for synthetic board. can delete if we get rid of synthetic board.
            if (line.startsWith("%Number of channels")) {
                int startIndex = line.indexOf('=') + 2;
                String nchanStr = line.substring(startIndex);
                int chanCount = Integer.parseInt(nchanStr);
                updateToNChan(MAIN, chanCount); // sythetic board depends on this being set before it's initialized
            }

            // some boards have configurable sample rate, so read it from header
            if (line.startsWith("%Sample Rate")) {
                int startIndex = line.indexOf('=') + 2;
                int endIndex = line.indexOf("Hz") - 1;

                String hzString = line.substring(startIndex, endIndex);
                sampleRateExg = Integer.parseInt(hzString);
            }

            // used to figure out the underlying board type
            if (line.startsWith("%Board")) {
                int startIndex = line.indexOf('=') + 2;
                underlyingClassName = line.substring(startIndex);
            }
        }

        boolean success = sampleRateExg > 0 && underlyingClassName != "";
        if(!success) {
            outputError("播放文件不包含所需的头部数据。");
        }
        return success;
    }

    protected boolean instantiateUnderlyingBoard() {
        try {
            // get class from name, try with package prefix if not found
            Class<?> boardClass = null;
            String[] packagePrefixes = {
                "",                          // 尝试原始类名
                "BoardCyton_.",              // Cyton 相关的板子
                "BoardGanglion_.",           // Ganglion 相关的板子
                "BoardBrainFlowSynthetic_.", // 合成板
                "BoardBrainflow_.",          // Brainflow 板
                "BoardNull_.",               // 空板
                "Board_."                    // 通用板
            };
            
            for (String prefix : packagePrefixes) {
                try {
                    String fullClassName = prefix + underlyingClassName;
                    boardClass = Class.forName(fullClassName);
                    break; // 找到了，退出循环
                } catch (ClassNotFoundException e) {
                    // 继续尝试下一个前缀
                }
            }
            
            if (boardClass == null) {
                throw new ClassNotFoundException(underlyingClassName);
            }
            
            // find default contructor, try GUI.class first, then GUIManager.class
            Constructor<?> constructor = null;
            try {
                constructor = boardClass.getConstructor(GUI.class);
            } catch (NoSuchMethodException e) {
                try {
                    constructor = boardClass.getConstructor(GUIManager.class);
                } catch (NoSuchMethodException e2) {
                    // 尝试带 int 参数的构造函数（用于 BoardBrainFlowSynthetic）
                    try {
                        constructor = boardClass.getConstructor(GUI.class, int.class);
                        underlyingBoard = (Board)constructor.newInstance(MAIN, GUI.nchan);
                        return underlyingBoard != null;
                    } catch (NoSuchMethodException e3) {
                        throw e;
                    }
                }
            }
            underlyingBoard = (Board)constructor.newInstance(MAIN);
        } catch (Exception e) {
            outputError("Cannot instantiate underlying board of class " + underlyingClassName);
            MAIN.println(e.getMessage());
            e.printStackTrace();
            return false;
        }

        return underlyingBoard != null;
    }

    protected boolean parseExgData(String[] lines) {
        int dataStart;
        // set data start to first line of data (skip header)
        for (dataStart = 0; dataStart < lines.length; dataStart++) {
            String line = lines[dataStart];
            if (!line.startsWith("%")) {
                dataStart++; // skip column names
                break;
            }
        }

        int dataLength = lines.length - dataStart;
        rawDataExg = new ArrayList<double[]>(dataLength);

        for (int iData=0; iData<dataLength; iData++) {
            String line = lines[dataStart + iData];
            String[] valStrs = line.split(",");
            if (((valStrs.length - 1) != getTotalChannelCount()) && (numChannelsExg == 0)) {
                outputWarn("你正在使用旧文件进行播放。");
            }
            numChannelsExg = valStrs.length - 1;  // -1 becaise of gui's timestamps
//------------------------------------------------------------------------------------------------------------
            double[] row = new double[numChannelsExg];
            //double[] row = new double[40];
            for (int iCol = 0; iCol < numChannelsExg; iCol++) {
                row[iCol] = Double.parseDouble(valStrs[iCol]);
            }
            rawDataExg.add(row);
        }

        return true;
    }

    private void parseHealthData() {
        rawDataHealth = new ArrayList<PlaybackHealthRecord>();
        currentHealthIndex = -1;
        playbackFilePathHealth = resolveHealthPlaybackFilePath();
        if (playbackFilePathHealth.isEmpty()) {
            return;
        }

        String[] lines = MAIN.loadStrings(playbackFilePathHealth);
        if (lines == null || lines.length == 0) {
            return;
        }

        int headerIndex = -1;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line == null) {
                continue;
            }
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("%")) {
                headerIndex = i;
                break;
            }
        }
        if (headerIndex < 0 || headerIndex + 1 >= lines.length) {
            return;
        }

        Map<String, Integer> columns = parseColumnMap(lines[headerIndex]);
        long firstTimestampMs = -1L;
        for (int i = headerIndex + 1; i < lines.length; i++) {
            String line = lines[i];
            if (line == null || line.trim().isEmpty() || line.trim().startsWith("%")) {
                continue;
            }
            String[] values = line.split(",");
            long timestampMs = parseLong(values, columns.get("timestamp_ms"), -1L);
            if (firstTimestampMs < 0L && timestampMs >= 0L) {
                firstTimestampMs = timestampMs;
            }

            double elapsedSeconds = parseDouble(values, columns.get("elapsed_seconds"), Double.NaN);
            if (Double.isNaN(elapsedSeconds)) {
                elapsedSeconds = timestampMs >= 0L && firstTimestampMs >= 0L
                        ? (timestampMs - firstTimestampMs) / 1000.0
                        : rawDataHealth.size();
            }

            CytonWifiHealthFrame frame = new CytonWifiHealthFrame(
                    parseInt(values, columns.get("health_seq"), -1),
                    parseInt(values, columns.get("heart_rate_bpm"), -1),
                    parseInt(values, columns.get("spo2_percent"), -1),
                    parseInt(values, columns.get("microcirculation"), -1),
                    parseInt(values, columns.get("systolic_mmhg"), -1),
                    parseInt(values, columns.get("diastolic_mmhg"), -1),
                    parseInt(values, columns.get("respiration_rate_per_min"), -1),
                    parseInt(values, columns.get("fatigue_index"), -1),
                    parseInt(values, columns.get("rr_interval_raw"), -1),
                    parseInt(values, columns.get("hrv_sdnn"), -1),
                    parseInt(values, columns.get("hrv_rmssd"), -1),
                    parseFloat(values, columns.get("body_temperature_c"), Float.NaN),
                    parseFloat(values, columns.get("ambient_temperature_c"), Float.NaN),
                    timestampMs >= 0L ? timestampMs : 0L
            );
            if (frame.hasMeasurement()) {
                rawDataHealth.add(new PlaybackHealthRecord(elapsedSeconds, frame));
            }
        }

        if (!rawDataHealth.isEmpty()) {
            MAIN.println("Playback: loaded health data file " + playbackFilePathHealth);
        }
    }

    private String resolveHealthPlaybackFilePath() {
        File exgFile = new File(playbackFilePathExg);
        File parent = exgFile.getParentFile();
        if (parent == null) {
            return "";
        }

        String exgName = exgFile.getName();
        String healthName = "";
        if (exgName.startsWith("AirEIBCI-RAW-")) {
            healthName = "AirEIBCI-Health-" + exgName.substring("AirEIBCI-RAW-".length());
        } else if (exgName.contains("-RAW-")) {
            healthName = exgName.replace("-RAW-", "-Health-");
        }

        if (!healthName.isEmpty()) {
            File healthFile = new File(parent, healthName);
            if (healthFile.isFile()) {
                return healthFile.getAbsolutePath();
            }
        }
        return "";
    }

    private Map<String, Integer> parseColumnMap(String headerLine) {
        Map<String, Integer> columns = new HashMap<String, Integer>();
        String[] names = headerLine.split(",");
        for (int i = 0; i < names.length; i++) {
            columns.put(names[i].trim(), i);
        }
        return columns;
    }

    private int parseInt(String[] values, Integer index, int fallback) {
        double value = parseDouble(values, index, Double.NaN);
        return Double.isNaN(value) ? fallback : (int) Math.round(value);
    }

    private long parseLong(String[] values, Integer index, long fallback) {
        double value = parseDouble(values, index, Double.NaN);
        return Double.isNaN(value) ? fallback : (long) value;
    }

    private float parseFloat(String[] values, Integer index, float fallback) {
        double value = parseDouble(values, index, Double.NaN);
        return Double.isNaN(value) ? fallback : (float) value;
    }

    private double parseDouble(String[] values, Integer index, double fallback) {
        if (index == null || index < 0 || index >= values.length) {
            return fallback;
        }
        try {
            return Double.parseDouble(values[index].trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    @Override
    public void update() {
        if (!streaming) {
            return; // do not update
        }

        float sampleRateMS = getSampleRate() / 1000.f;

        int timeElapsedMS = MAIN.millis() - timeOfLastUpdateMSExg;
        numNewSamplesThisFrameExg = MAIN.floor(timeElapsedMS * sampleRateMS);

        // account for the fact that each update will not coincide with a sample exactly.
        // to keep the streaming rate accurate, we increment the time of last update
        // based on how many samples we incremented this frame.
        timeOfLastUpdateMSExg += numNewSamplesThisFrameExg / sampleRateMS;

        currentSampleExg += numNewSamplesThisFrameExg;

        if (endOfFileReached()) {
            topNav.stopButtonWasPressed();
        }

        // don't go beyond raw data array size
        currentSampleExg = MAIN.min(currentSampleExg, getTotalSamples());
    }

    @Override
    public void startStreaming() {
        streaming = true;
        timeOfLastUpdateMSExg = MAIN.millis();
    }

    @Override
    public void stopStreaming() {
        streaming = false;
    }

    @Override
    public boolean isStreaming() {
        return streaming;
    }

    @Override
    public int getSampleRate() {
        return sampleRateExg;
    }

    @Override
    public void setEXGChannelActive(int channelIndex, boolean active) {
        outputWarn("播放板无法关闭频道。");
    }

    @Override
    public boolean isEXGChannelActive(int channelIndex) {
        return true;
    }

    @Override
    public int[] getEXGChannels() {
        return underlyingBoard.getEXGChannels();
    }

    @Override
    public int getNumEXGChannels() {
        return getEXGChannels().length;
    }

    @Override
    public int getTimestampChannel() {
        return underlyingBoard.getTimestampChannel();
    }

    @Override
    public int getSampleIndexChannel() {
        return underlyingBoard.getSampleIndexChannel();
    }

    public int getTotalSamples() {
        return rawDataExg.size();
    }

    public float getTotalTimeSeconds() {
        return (float) (getTotalSamples()) / (getSampleRate());
    }

    public int getCurrentSample() {
        return currentSampleExg;
    }

    public float getCurrentTimeSeconds() {
        return (float) (getCurrentSample()) / (getSampleRate());
    }

    public boolean hasPlaybackHealthData() {
        return rawDataHealth != null && !rawDataHealth.isEmpty();
    }

    public String getPlaybackHealthFilePath() {
        return playbackFilePathHealth;
    }

    public CytonWifiHealthFrame getPlaybackHealthFrameAtCurrentTime() {
        if (!hasPlaybackHealthData()) {
            return null;
        }

        double currentTimeSeconds = getCurrentTimeSeconds();
        int lo = 0;
        int hi = rawDataHealth.size() - 1;
        int result = -1;
        while (lo <= hi) {
            int mid = (lo + hi) / 2;
            if (rawDataHealth.get(mid).elapsedSeconds <= currentTimeSeconds) {
                result = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }

        currentHealthIndex = result;
        return result >= 0 ? rawDataHealth.get(result).frame : null;
    }

    @Override
    public int getMarkerChannel() {
        return underlyingBoard.getMarkerChannel();
    }

    public void goToIndex(int index) {
        currentSampleExg = index;
    }

    @Override
    public int getTotalChannelCount() {
        if (numChannelsExg == 0)
            return underlyingBoard.getTotalChannelCount();
        return numChannelsExg;
    }

    @Override
    public double[][] getFrameData() {
        double[][] array = new double[numChannelsExg][numNewSamplesThisFrameExg];
        //double[][] array = new double[40][numNewSamplesThisFrameExg];
        List<double[]> list = getData(numNewSamplesThisFrameExg);
        for (int i = 0; i < numNewSamplesThisFrameExg; i++) {
            for (int j = 0; j < numChannelsExg; j++) {
                array[j][i] = list.get(i)[j];
            }
        }
        return array;
    }

    @Override
    public List<double[]> getData(int maxSamples) {
        int firstSample = MAIN.max(0, currentSampleExg - maxSamples);
        List<double[]> result = rawDataExg.subList(firstSample, currentSampleExg);

        // if needed, pad the beginning of the array with empty data
        if (maxSamples > currentSampleExg) {
            int sampleDiff = maxSamples - currentSampleExg;

            double[] emptyData = new double[numChannelsExg];
            //double[] emptyData = new double[40];
            ArrayList<double[]> newResult = new ArrayList(maxSamples);
            for (int i=0; i<sampleDiff; i++) {
                newResult.add(emptyData);
            }

            newResult.addAll(result);
            return newResult;
        }

        return result;
    }

    @Override
    public boolean endOfFileReached() {
        return currentSampleExg >= getTotalSamples();
    }

    private static final class PlaybackHealthRecord {
        final double elapsedSeconds;
        final CytonWifiHealthFrame frame;

        PlaybackHealthRecord(double elapsedSeconds, CytonWifiHealthFrame frame) {
            this.elapsedSeconds = elapsedSeconds;
            this.frame = frame;
        }
    }
}
