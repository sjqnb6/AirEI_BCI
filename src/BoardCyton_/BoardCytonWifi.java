package BoardCyton_;

import Globel.GUI;
import brainflow.BoardIds;

public class BoardCytonWifi extends BoardCytonWifiBase {
    public BoardCytonWifi(GUI MAIN) {
        super(MAIN);
    }
    public BoardCytonWifi(GUI MAIN, String ipAddress, int samplingRate) {
        super(MAIN, samplingRate);
        this.ipAddress = ipAddress;
    }

    @Override
    public BoardIds getBoardId() {
        return BoardIds.CYTON_WIFI_BOARD;
    }
};