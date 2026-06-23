package DataWriterODF_;

import Board_.Board;

import java.io.File;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import static Globel.GUI.directoryManager;
import static Globel.GUI.nchan;
import static processing.core.PApplet.createWriter;
import Globel.GUI;
public class DataWriterODF {
    protected static final String BRAND_NAME = "AirEIBCI";
    protected static final String SESSION_PREFIX = BRAND_NAME + "_Session_";
    GUI MAIN;
    private PrintWriter output;
    public String fname;
    private int rowsWritten;
    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    //variation on constructor to have custom name
    public DataWriterODF(GUI MAIN, String _sessionName, String _fileName) {
        this.MAIN = MAIN;
        MAIN.settings.setSessionPath(directoryManager.getRecordingsPath() + SESSION_PREFIX + _sessionName + File.separator);
        fname = MAIN.settings.getSessionPath();
        fname += getFileNamePrependString();
        fname += _fileName;
        fname += ".txt";
        output = createWriter(new File(fname));        //open the file
        writeHeader();    //add the header
        rowsWritten = 0;    //init the counter
    }

    public void writeHeader() {
        output.println(getHeaderFirstLineString());
        output.println("%Number of channels = " + getNumberOfChannels());
        output.println("%Sample Rate = " + getSamplingRate() + " Hz");
        output.println("%Board = " + getUnderlyingBoardClass());

        String[] colNames = getChannelNames();

        for (int i = 0; i < colNames.length; i++) {
            output.print(colNames[i]);
            output.print(", ");
        }
        output.print("Timestamp (Formatted)");
        output.println();
        output.flush();
    }

    public void append(double[][] data) {
        //get current date time with Date()
        for (int iSample = 0; iSample < data[0].length; iSample++) {
            for (int iChan = 0; iChan < data.length; iChan++) {
                output.print(data[iChan][iSample]);
                output.print(", ");
            }
            
            //int timestampChan = getTimestampChannel();
            // *1000 to convert from seconds to milliserconds
            //long timestampMS = (long)(data[timestampChan][iSample] * 1000.0);
            Long timestampMS = System.currentTimeMillis();
            output.print(dateFormat.format(new Date(timestampMS)));
            output.println();

            rowsWritten++;
        }
    }

    public void closeFile() {
        output.close();
    }

    public int getRowsWritten() {
        return rowsWritten;
    }

    protected int getNumberOfChannels() {
        return nchan;
    }

    protected int getSamplingRate() {
        return ((Board)MAIN.currentBoard).getSampleRate();
    }

    protected String getUnderlyingBoardClass() {
        return ((Board)MAIN.currentBoard).getClass().getName();
    }

    protected String[] getChannelNames() {
        return ((Board)MAIN.currentBoard).getChannelNames();
    }

    protected int getTimestampChannel() {
        return ((Board)MAIN.currentBoard).getTimestampChannel();
    }

    protected int getMarkerChannel() {
        return ((Board)MAIN.currentBoard).getMarkerChannel();
    }

    protected String getFileNamePrependString() {
        return BRAND_NAME + "-RAW-";
    }

    protected String getHeaderFirstLineString() {
        return "%" + BRAND_NAME + " Raw EXG Data";
    }

};
