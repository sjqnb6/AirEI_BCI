package DataWriterAuxODF_;

import AuxDataBoard_.AuxDataBoard;
import DataWriterODF_.DataWriterODF;

import static GUI.GGVI.currentBoard;

public class DataWriterAuxODF extends DataWriterODF {
    protected String fileNamePrependString = "OpenBCI-RAW-Aux-";
    protected String headerFirstLineString = "%OpenBCI Raw Aux Data";

    //variation on constructor to have custom name
    public DataWriterAuxODF(String _sessionName, String _fileName) {
        super(_sessionName, _fileName);
    }

    protected int getNumberOfChannels() {
        return ((AuxDataBoard)currentBoard).getNumAuxChannels();
    }

    protected int getSamplingRate() {
        return ((AuxDataBoard)currentBoard).getAuxSampleRate();
    }

    protected String getUnderlyingBoardClass() {
        return ((AuxDataBoard)currentBoard).getClass().getName();
    }

    protected String[] getChannelNames() {
        return ((AuxDataBoard)currentBoard).getAuxChannelNames();
    }

    protected int getTimestampChannel() {
        return ((AuxDataBoard)currentBoard).getAuxTimestampChannel();
    }

};
