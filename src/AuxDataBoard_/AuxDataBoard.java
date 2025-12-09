package AuxDataBoard_;

import java.util.List;

public interface AuxDataBoard {

    public List<double[]> getAuxData(int maxSamples);

    public String[] getAuxChannelNames();

    public double[][] getAuxFrameData();

    public int getAuxSampleRate();

    public int getNumAuxChannels();

    public int getAuxTimestampChannel();
};
