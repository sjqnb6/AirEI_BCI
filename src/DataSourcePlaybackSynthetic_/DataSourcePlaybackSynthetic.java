package DataSourcePlaybackSynthetic_;

import AccelerometerCapableBoard_.AccelerometerCapableBoard;
import BoardBrainFlowSynthetic_.BoardBrainFlowSynthetic;
import DataSourcePlayback_.DataSourcePlayback;
import FileBoard_.FileBoard;

import java.util.List;

import static GUI.GGVI.nchan;

public class DataSourcePlaybackSynthetic extends DataSourcePlayback implements AccelerometerCapableBoard, FileBoard {

    public DataSourcePlaybackSynthetic(String filePath) {
        super(filePath);
    }

    protected boolean instantiateUnderlyingBoard() {
        try {
            underlyingBoard = new BoardBrainFlowSynthetic(nchan);
        } catch (Exception e) {
            println(e.getMessage());
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
