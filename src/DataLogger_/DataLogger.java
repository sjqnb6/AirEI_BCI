package DataLogger_;

import AuxDataBoard_.AuxDataBoard;
import DataWriterAuxODF_.DataWriterAuxODF;
import DataWriterBDF_.DataWriterBDF;
import DataWriterBF_.DataWriterBF;
import DataWriterODF_.DataWriterODF;
import Globel.GUI;
import processing.core.PApplet;

import static Globel.GUI.*;
import static processing.core.PApplet.println;
import Globel.GUI;
public class DataLogger {
    GUI MAIN;
    //variables for writing EEG data out to a file
    private DataWriterODF fileWriterODF;
    private DataWriterAuxODF fileWriterAuxODF;
    private DataWriterBDF fileWriterBDF;
    public DataWriterBF fileWriterBF; //Add the ability to simulataneously save to BrainFlow CSV, independent of BDF or ODF
    private String sessionName = "N/A";
    public final int OUTPUT_SOURCE_NONE = 0;
    public final int OUTPUT_SOURCE_ODF = 1; // The OpenBCI CSV Data Format
    public final int OUTPUT_SOURCE_BDF = 2; // The BDF data format http://www.biosemi.com/faq/file_format.htm
    private int outputDataSource;

    public DataLogger(GUI MAIN) {
        //Default to OpenBCI CSV Data Format
        outputDataSource = OUTPUT_SOURCE_ODF;
        fileWriterBF = new DataWriterBF(MAIN);
    }

    public void initialize() {

    }

    public void uninitialize() {
        closeLogFile();  //close log file
        fileWriterBF.resetBrainFlowStreamer();
    }

    public void update() {
        limitRecordingFileDuration();

        saveNewData();
    }


    private void saveNewData() {
        //If data is available, save to playback file...
        if(!MAIN.settings.isLogFileOpen()) {
            return;
        }

        double[][] newData = MAIN.currentBoard.getFrameData();

        switch (outputDataSource) {
            case OUTPUT_SOURCE_ODF:
                fileWriterODF.append(newData);
                if (MAIN.currentBoard instanceof AuxDataBoard)
                    fileWriterAuxODF.append(((AuxDataBoard)MAIN.currentBoard).getAuxFrameData());
                break;
            case OUTPUT_SOURCE_BDF:
                fileWriterBDF.writeRawData_dataPacket(MAIN, newData);
                break;
            case OUTPUT_SOURCE_NONE:
            default:
                // Do nothing...
                break;
        }
    }

    public void limitRecordingFileDuration() {
        if (MAIN.settings.isLogFileOpen() && outputDataSource == OUTPUT_SOURCE_ODF && MAIN.settings.maxLogTimeReached()) {
            println("DataLogging: Max recording duration reached for OpenBCI data format. Creating a new recording file in the session folder.");
            closeLogFile();
            openNewLogFile(directoryManager.getFileNameDateTime());
            MAIN.settings.setLogFileStartTime(System.nanoTime());
        }
    }

    public void onStartStreaming() {
        if (outputDataSource > OUTPUT_SOURCE_NONE && eegDataSource != DATASOURCE_PLAYBACKFILE) {
            //open data file if it has not already been opened
            if (!MAIN.settings.isLogFileOpen()) {
                openNewLogFile(directoryManager.getFileNameDateTime());
            }
            MAIN.settings.setLogFileStartTime(System.nanoTime());
        }

        //Print BrainFlow Streamer Info here after ODF and BDF println
        if (eegDataSource != DATASOURCE_PLAYBACKFILE && eegDataSource != DATASOURCE_STREAMING) {
            controlPanel.setBrainFlowStreamerOutput();
            StringBuilder sb = new StringBuilder("OpenBCI_GUI: BrainFlow Streamer Location: ");
            sb.append(brainflowStreamer);
            println(sb.toString());
        }
    }

    public void onStopStreaming() {
        //Close the log file when using OpenBCI Data Format (.txt)
        if (outputDataSource == OUTPUT_SOURCE_ODF) closeLogFile();
    }

    public float getSecondsWritten() {
        if (outputDataSource == OUTPUT_SOURCE_ODF && fileWriterODF != null) {
            return (float)(fileWriterODF.getRowsWritten())/MAIN.currentBoard.getSampleRate();
        }

        if (outputDataSource == OUTPUT_SOURCE_BDF && fileWriterBDF != null) {
            return fileWriterBDF.getRecordsWritten();
        }

        return 0.f;
    }

    private void openNewLogFile(String _fileName) {
        //close the file if it's open
        switch (outputDataSource) {
            case OUTPUT_SOURCE_ODF:
                openNewLogFileODF(_fileName);
                break;
            case OUTPUT_SOURCE_BDF:
                openNewLogFileBDF(_fileName);
                break;
            case OUTPUT_SOURCE_NONE:
            default:
                // Do nothing...
                break;
        }
        MAIN.settings.setLogFileIsOpen(true);
    }

    /**
     * @description Opens (and closes if already open) and BDF file. BDF is the
     *  biosemi data format.
     * @param `_fileName` {String} - The meat of the file name
     */
    private void openNewLogFileBDF(String _fileName) {
        if (fileWriterBDF != null) {
            println("OpenBCI_GUI: closing log file");
            closeLogFile();
        }
        //open the new file
        fileWriterBDF = new DataWriterBDF(_fileName);

        output_fname = fileWriterBDF.fname;
        println("OpenBCI_GUI: openNewLogFile: opened BDF output file: " + output_fname); //Print filename of new BDF file to console
    }

    /**
     * @description Opens (and closes if already open) and ODF file. ODF is the
     *  openbci data format.
     * @param `_fileName` {String} - The meat of the file name
     */
    private void openNewLogFileODF(String _fileName) {
        if (fileWriterODF != null) {
            println("OpenBCI_GUI: closing log file");
            closeLogFile();
        }
        //open the new file
        fileWriterODF = new DataWriterODF(MAIN, sessionName, _fileName);
        if (MAIN.currentBoard instanceof AuxDataBoard) {
            if (fileWriterAuxODF != null)
                fileWriterAuxODF.closeFile();
            fileWriterAuxODF = new DataWriterAuxODF(MAIN, sessionName, _fileName);
        }

        output_fname = fileWriterODF.fname;
        println("OpenBCI_GUI: openNewLogFile: opened ODF output file: " + output_fname); //Print filename of new ODF file to console
    }

    private void closeLogFile() {
        switch (outputDataSource) {
            case OUTPUT_SOURCE_ODF:
                closeLogFileODF();
                break;
            case OUTPUT_SOURCE_BDF:
                closeLogFileBDF();
                break;
            case OUTPUT_SOURCE_NONE:
            default:
                // Do nothing...
                break;
        }
        MAIN.settings.setLogFileIsOpen(false);
    }

    /**
     * @description Close an open BDF file. This will also update the number of data
     *  records.
     */
    private void closeLogFileBDF() {
        if (fileWriterBDF != null) {
            fileWriterBDF.closeFile();
        }
        fileWriterBDF = null;
    }

    /**
     * @description Close an open ODF file.
     */
    private void closeLogFileODF() {
        if (fileWriterODF != null) {
            fileWriterODF.closeFile();
        }
        fileWriterODF = null;
        if (fileWriterAuxODF != null) {
            fileWriterAuxODF.closeFile();
        }
        fileWriterAuxODF = null;
    }

    public int getDataLoggerOutputFormat() {
        return outputDataSource;
    }

    public void setDataLoggerOutputFormat(int outputSource) {
        outputDataSource = outputSource;
    }

    public void setSessionName(String s) {
        sessionName = s;
    }

    public final String getSessionName() {
        return sessionName;
    }

    public void setBfWriterFolder(String _folderName, String _folderPath) {
        fileWriterBF.setBrainFlowStreamerFolderName(_folderName, _folderPath);
    }

    public void setBfWriterDefaultFolder() {
        if (MAIN.settings.getSessionPath() != "") {
            MAIN.settings.setSessionPath(directoryManager.getRecordingsPath() + "OpenBCISession_" + sessionName);
        }
        fileWriterBF.setBrainFlowStreamerFolderName(sessionName, MAIN.settings.getSessionPath());
    }

    public String getBfWriterFilePath() {
        return fileWriterBF.getBrainFlowStreamerRecordingFileName();
    }
};