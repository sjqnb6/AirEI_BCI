package BoardCyton_;

import Globel.GUI;
import brainflow.BoardIds;

public class BoardCytonWifiDaisy extends BoardCytonWifiBase {
    public BoardCytonWifiDaisy(GUI MAIN) {
        super(MAIN);
    }
    public BoardCytonWifiDaisy(GUI MAIN, String ipAddress, int samplingRate) {
        super(MAIN, samplingRate);
        this.ipAddress = ipAddress;
    }

    @Override
    public BoardIds getBoardId() {
        return BoardIds.CYTON_DAISY_WIFI_BOARD;
    }
};
