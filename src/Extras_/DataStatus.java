package Extras_;

import ADS1299SettingsBoard_.ADS1299SettingsBoard;
import GUI.GUIManager;
import Globel.GUI;
import processing.core.PApplet;


public class DataStatus{
    GUI MAIN;
    public boolean is_railed;
    public boolean is_railed_warn;
    private double percentage;
    public String notificationString;
    private final int default_color = MAIN.OPENBCI_DARKBLUE;
    private final int yellow = MAIN.SIGNAL_CHECK_YELLOW;
    private final int red = MAIN.BOLD_RED;
    private int colorIndicator = default_color;
    // thresholds are pecentages of max possible value
    private double threshold_railed = 90.0;
    private double threshold_railed_warn = 75.0;

    public DataStatus(GUI MAIN) {
//        super(pApplet);
        notificationString = "";
        is_railed = false;
        is_railed_warn = false;
        percentage = 0.0;
        this.MAIN = MAIN;
    }
    // here data is a full range for 20sec of data and doesnt take in account window size
    public void update(float[] data, int channel) {
        percentage = 0.0;
        is_railed = false;
        is_railed_warn = false;

        if (data.length < 1) {
            return;
        }

        if (MAIN.currentBoard instanceof ADS1299SettingsBoard) {
            double scaler =  (4.5 / (MAIN.pow (2, 23) - 1) / ((ADS1299SettingsBoard)MAIN.currentBoard).getGain(channel) * 1000000.);
            double maxVal = scaler * MAIN.pow (2, 23);
            int numSeconds = 3;
            int nPoints = numSeconds * MAIN.currentBoard.getSampleRate();
            int endPos = data.length;
            int startPos = Math.max(0, endPos - nPoints);

            boolean is_straight_line = true;
            if (!MAIN.currentBoard.isStreaming()) {
                is_straight_line = false;
            }
            float max = Math.abs(data[startPos]);
            for (int i = startPos + 1; i < endPos; i++) {
                if (Math.abs(data[i]) > max) {
                    max = Math.abs(data[i]);
                }
                if ((Math.abs(data[i - 1] - data[i]) > 0.00001) && (Math.abs(data[i]) > 0.00001)) {
                    is_straight_line = false;
                }
            }
            percentage = (max / maxVal) * 100.0;

            notificationString = "Not Railed " + String.format("%1$,.2f", percentage) + "% ";
            colorIndicator = default_color;
            if (percentage > threshold_railed_warn) {
                is_railed_warn = true;
                notificationString = "Near Railed " + String.format("%1$,.2f", percentage) + "% ";
                colorIndicator = yellow;
            }
            if (percentage > threshold_railed) {
                is_railed = true;
                notificationString = "Railed " + String.format("%1$,.2f", percentage) + "% ";
                colorIndicator = red;
            } else {
                if (is_straight_line) {
                    is_railed = true;
                    notificationString = "Data from the board doesn't change";
                    colorIndicator = red;
                }
            }

        }
    }
    public int getColor() {
        return colorIndicator;
    }
    public double getPercentage() {
        return percentage;
    }

    public void setRailedWarnThreshold(double d) {
        threshold_railed_warn = d;
    }

    public void setRailedThreshold(double d) {
        threshold_railed = d;
    }
};
