package BoardCyton_;

import brainflow.BoardIds;

public class BoardCytonWifiDaisy extends BoardCytonWifiBase {
    public BoardCytonWifiDaisy() {
        super();
    }
    public BoardCytonWifiDaisy(String ipAddress, int samplingRate) {
        super(samplingRate);
        this.ipAddress = ipAddress;
    }

    @Override
    public BoardIds getBoardId() {
        return BoardIds.CYTON_DAISY_WIFI_BOARD;
    }
};
