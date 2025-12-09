package DataSourcePlayback_;

import DataSourcePlaybackCyton_.DataSourcePlaybackCyton;
import DataSourcePlaybackGanglion_.DataSourcePlaybackGanglion;
import DataSourcePlaybackSynthetic_.DataSourcePlaybackSynthetic;

import java.io.BufferedReader;
import java.io.IOException;

import static Debugging_.GF.verbosePrint;
import static Extras_.GF.createBufferedReader;
import static processing.core.PApplet.split;

public class GF {

    public static DataSourcePlayback getDataSourcePlaybackClassFromFile(String path) {
        verbosePrint("Checking " + path + " for underlying board class.");
        String strCurrentLine;
        int lineCounter = 0;
        int maxLinesToCheck = 4;
        String infoToCheck = "%Board = ";
        String underlyingBoardClassName = "";
        BufferedReader reader = createBufferedReader(path);
        try {
            while (lineCounter < maxLinesToCheck) {
                strCurrentLine = reader.readLine();
                verbosePrint(strCurrentLine);
                if (strCurrentLine.startsWith(infoToCheck)) {
                    String[] splitCurrentLine = split(strCurrentLine, "OpenBCI_GUI$");
                    underlyingBoardClassName = splitCurrentLine[1];
                }
                lineCounter++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        switch (underlyingBoardClassName) {
            case ("BoardCytonSerial"):
            case ("BoardCytonSerialDaisy"):
            case ("BoardCytonWifi"):
            case ("BoardCytonWifiDaisy"):
                return new DataSourcePlaybackCyton(path);
            case ("BoardGanglionBLE"):
            case ("BoardGanglionNative"):
            case ("BoardGanglionWifi"):
                return new DataSourcePlaybackGanglion(path);
            case ("BoardBrainFlowSynthetic"):
                return new DataSourcePlaybackSynthetic(path);
            default:
                return null;
        }
    }

}
