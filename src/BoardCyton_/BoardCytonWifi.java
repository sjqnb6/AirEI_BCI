package BoardCyton_;

import brainflow.BoardIds;

public class BoardCytonWifi extends BoardCytonWifiBase {
    public BoardCytonWifi() {
        super();
    }
    public BoardCytonWifi(String ipAddress, int samplingRate) {
        super(samplingRate);
        this.ipAddress = ipAddress;
    }

    @Override
    public BoardIds getBoardId() {
        return BoardIds.CYTON_WIFI_BOARD;
    }
};