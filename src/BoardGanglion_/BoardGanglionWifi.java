package BoardGanglion_;

import PacketLossTracker_.PacketLossTracker;
import brainflow.BoardIds;

import java.util.HashMap;
import java.util.Map;

public class BoardGanglionWifi extends BoardGanglion {
    // https://docs.openbci.com/docs/03Ganglion/GanglionSDK
    private Map<Integer, String> samplingRateCommands = new HashMap<Integer, String>() {{
        put(25600, "~0");
        put(12800, "~1");
        put(6400, "~2");
        put(3200, "~3");
        put(1600, "~4");
        put(800, "~5");
        put(400, "~6");
        put(200, "~7");
    }};

    public BoardGanglionWifi(String ipAddress, int samplingRate) {
        super();
        this.ipAddress = ipAddress;
        samplingRateCache = samplingRate;
    }

    @Override
    public boolean initializeInternal()
    {
        // turn on accel by default, or is it handled somewhere else?
        boolean res = super.initializeInternal();

        if ((res) && (samplingRateCache > 0)){
            String command = samplingRateCommands.get(samplingRateCache);
            sendCommand(command);
        }

        return res;
    }

    @Override
    public BoardIds getBoardId() {
        return BoardIds.GANGLION_WIFI_BOARD;
    }

    @Override
    protected PacketLossTracker setupPacketLossTracker() {
        final int minSampleIndex = 0;
        final int maxSampleIndex = 200;
        return new PacketLossTracker(getSampleIndexChannel(), getTimestampChannel(),
                minSampleIndex, maxSampleIndex);
    }
};