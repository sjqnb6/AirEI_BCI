package DataWriterAuxODF_;

import AuxDataBoard_.AuxDataBoard;
import DataWriterODF_.DataWriterODF;
import Globel.GUI;
public class DataWriterAuxODF extends DataWriterODF {
    GUI MAIN;

    //variation on constructor to have custom name
    public DataWriterAuxODF(GUI MAIN, String _sessionName, String _fileName) {
        super(MAIN, _sessionName, _fileName);
        this.MAIN = MAIN;
    }

    protected int getNumberOfChannels() {
        return ((AuxDataBoard)MAIN.currentBoard).getNumAuxChannels();
    }

    protected int getSamplingRate() {
        return ((AuxDataBoard)MAIN.currentBoard).getAuxSampleRate();
    }

    protected String getUnderlyingBoardClass() {
        return ((AuxDataBoard)MAIN.currentBoard).getClass().getName();
    }

    protected String[] getChannelNames() {
        return ((AuxDataBoard)MAIN.currentBoard).getAuxChannelNames();
    }

    protected int getTimestampChannel() {
        return ((AuxDataBoard)MAIN.currentBoard).getAuxTimestampChannel();
    }

    @Override
    protected String getFileNamePrependString() {
        return BRAND_NAME + "-RAW-Aux-";
    }

    @Override
    protected String getHeaderFirstLineString() {
        return "%" + BRAND_NAME + " Raw Aux Data";
    }

};
