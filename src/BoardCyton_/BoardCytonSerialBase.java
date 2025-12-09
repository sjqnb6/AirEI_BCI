package BoardCyton_;

import Buffer_.Buffer;
import GUI.GUIManager;
import Globel.GUI;
import SmoothingBoard_.SmoothingCapableBoard;

public abstract class BoardCytonSerialBase extends BoardCyton implements SmoothingCapableBoard {

    private Buffer<double[]> buffer = null;
    private volatile boolean smoothData;

    private GUI MAIN;

    public BoardCytonSerialBase(GUI MAIN) {
        super();
        setSmoothingActive(true);
        this.MAIN = MAIN;
    }

    // synchronized is important to ensure that we dont free buffers during getting data
    @Override
    public synchronized void setSmoothingActive(boolean active) {
        if (smoothData == active) {
            return;
        }
        // dont touch accumulatedData buffer to dont pause streaming
        if (active) {
            buffer = new Buffer<double[]>(MAIN, getSampleRate());
        } else {
            buffer = null;
        }
        smoothData = active;
    }

    @Override
    public boolean getSmoothingActive() {
        return smoothData;
    }

    @Override
    protected synchronized double[][] getNewDataInternal() {
        double[][] data = super.getNewDataInternal();
        if (!smoothData) {
            return data;
        }
        // transpose to push to buffer
        for (int i = 0; i < data[0].length; i++) {
            double[] newEntry = new double[getTotalChannelCount()];
            for (int j = 0; j < getTotalChannelCount(); j++) {
                newEntry[j] = data[j][i];
            }
            buffer.addNewEntry(newEntry);
        }
        int numData = buffer.getDataCount();
        if (numData == 0) {
            return emptyData;
        }
        // transpose back
        double[][] res = new double[getTotalChannelCount()][numData];
        for (int i = 0; i < numData; i++) {
            double[] curData = buffer.popFirstEntry();
            for (int j = 0; j < getTotalChannelCount(); j++) {
                res[j][i] = curData[j];
            }
        }
        return res;
    }

};
