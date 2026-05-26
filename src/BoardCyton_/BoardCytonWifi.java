package BoardCyton_;

import Globel.GUI;
import brainflow.BoardIds;

public class BoardCytonWifi extends BoardCytonWifiBase {
    private static final int DEFAULT_CUSTOM_WIFI_PORT = 6677;

    public BoardCytonWifi(GUI MAIN) {
        super(MAIN);
        enableCustomWifiParser(this.ipAddress, DEFAULT_CUSTOM_WIFI_PORT);
    }
    public BoardCytonWifi(GUI MAIN, String ipAddress, int samplingRate) {
        super(MAIN, samplingRate);
        this.ipAddress = ipAddress;
        enableCustomWifiParser(this.ipAddress, DEFAULT_CUSTOM_WIFI_PORT);
    }

    @Override
    public BoardIds getBoardId() {
        return BoardIds.CYTON_WIFI_BOARD;
    }
};

