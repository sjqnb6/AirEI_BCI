package DataSourcePlaybackGanglion_;

import AccelerometerCapableBoard_.AccelerometerCapableBoard;
import DataSourcePlayback_.DataSourcePlayback;
import FileBoard_.FileBoard;

import java.util.List;

public class DataSourcePlaybackGanglion extends DataSourcePlayback implements AccelerometerCapableBoard, FileBoard {

    public DataSourcePlaybackGanglion(String filePath) {
        super(filePath);
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
