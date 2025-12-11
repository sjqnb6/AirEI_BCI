package DataWriterBF_;

import java.io.File;

import static processing.core.PApplet.println;
import Globel.GUI;
public class DataWriterBF {
    GUI MAIN;
    private String folderPath = "";
    private String folderName = "";
    private StringBuilder fileName = null;
    private final String brainflowWriteOption = ":w";
    private int fileNumber = 0;

    //variation on constructor to have custom name
    public DataWriterBF(GUI MAIN) {
        this.MAIN = MAIN;
    }

    public void setBrainFlowStreamerFolderName(String _folderName, String _folderPath) {
        //settings.setSessionPath(directoryManager.getRecordingsPath() + "OpenBCISession_" + _sessionName + File.separator);
        folderName = _folderName;
        folderPath = _folderPath;

        if (folderName == null || folderPath == null) {
            println("Error setting BrainFlow Streamer file output path. Try selecting the custom path again.");
            fileName = null;
            return;
        }

        generateBrainFlowStreamerFileName();

        File directory = new File(fileName.toString());
        if (!directory.exists()){
            directory.mkdirs();
            // If you require it to make the entire directory path including parents,
            // use directory.mkdirs(); here instead.
        }
    }

    private void generateBrainFlowStreamerFileName() {
        fileName = new StringBuilder("file://");
        fileName.append(folderPath);
        fileName.append(File.separator);
        fileName.append("BrainFlow-RAW_");
        fileName.append(folderName);
        fileName.append("_");
        fileName.append(fileNumber);
        fileName.append(".csv");
        fileName.append(brainflowWriteOption);
    }

    public void incrementBrainFlowStreamerFileNumber() {
        fileNumber++;
        generateBrainFlowStreamerFileName();
    }

    public void resetBrainFlowStreamer() {
        fileNumber = 0;
        folderName = "";
        folderPath = "";
        fileName = null;
        MAIN.brainflowStreamer = "";
    }

    public String getBrainFlowStreamerRecordingFileName() {
        return fileName == null ? null : fileName.toString();
    }
}
