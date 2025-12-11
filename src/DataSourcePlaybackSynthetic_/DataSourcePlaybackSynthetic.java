package DataSourcePlaybackSynthetic_;

import AccelerometerCapableBoard_.AccelerometerCapableBoard;
import BoardBrainFlowSynthetic_.BoardBrainFlowSynthetic;
import DataSourcePlayback_.DataSourcePlayback;
import FileBoard_.FileBoard;

import java.util.List;
import Globel.GUI;

import static Globel.GUI.nchan;

public class DataSourcePlaybackSynthetic extends DataSourcePlayback implements AccelerometerCapableBoard, FileBoard {
    GUI MAIN;
    public DataSourcePlaybackSynthetic(GUI MAIN, String filePath) {

        super(MAIN, filePath);
        this.MAIN = MAIN;
    }

    protected boolean instantiateUnderlyingBoard(GUI MAIN) {
        try {
            underlyingBoard = new BoardBrainFlowSynthetic(MAIN, nchan);
        } catch (Exception e) {
            MAIN.println(e.getMessage());
            e.printStackTrace();
            return false;
        }

        return underlyingBoard != null;
    }

    @Override
    public int getAccelSampleRate() {
        return getSampleRate();
    }

    @Override
    public boolean isAccelerometerActive() {
        return underlyingBoard instanceof AccelerometerCapableBoard;
    }

    @Override
    public void setAccelerometerActive(boolean active) {
        // nothing
    }

    @Override
    public boolean canDeactivateAccelerometer() {
        return false;
    }

    @Override
    public int[] getAccelerometerChannels() {
        if (underlyingBoard instanceof AccelerometerCapableBoard) {
            return ((AccelerometerCapableBoard)underlyingBoard).getAccelerometerChannels();
        }

        return new int[0];
    }

    @Override
    public List<double[]> getDataWithAccel(int maxSamples) {
        return getData(maxSamples);
    }

}
