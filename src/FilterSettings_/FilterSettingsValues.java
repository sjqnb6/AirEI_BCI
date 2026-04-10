package FilterSettings_;

import FilterEnums_.*;

import java.util.Arrays;

public class FilterSettingsValues {

    public BFFilter brainFlowFilter;
    public FilterChannelSelect filterChannelSelect;
    public GlobalEnvironmentalFilter globalEnvFilter;

    public FilterActiveOnChannel masterBandStopFilterActive;
    public double masterBandStopStartFreq;
    public double masterBandStopStopFreq;
    public BrainFlowFilterType masterBandStopFilterType = BrainFlowFilterType.BUTTERWORTH;
    public BrainFlowFilterOrder masterBandStopFilterOrder = BrainFlowFilterOrder.TWO;

    public FilterActiveOnChannel[] bandStopFilterActive;
    public double[] bandStopStartFreq;
    public double[] bandStopStopFreq;
    public BrainFlowFilterType[] bandStopFilterType;
    public BrainFlowFilterOrder[] bandStopFilterOrder;

    public FilterActiveOnChannel masterBandPassFilterActive;
    public double masterBandPassStartFreq;
    public double masterBandPassStopFreq;
    public BrainFlowFilterType masterBandPassFilterType = BrainFlowFilterType.BUTTERWORTH;
    public BrainFlowFilterOrder masterBandPassFilterOrder = BrainFlowFilterOrder.TWO;

    public FilterActiveOnChannel[] bandPassFilterActive;
    public double[] bandPassStartFreq;
    public double[] bandPassStopFreq;
    public BrainFlowFilterType[] bandPassFilterType;
    public BrainFlowFilterOrder[] bandPassFilterOrder;

    public FilterSettingsValues(int channelCount) {
        brainFlowFilter = BFFilter.BANDPASS;
        filterChannelSelect = FilterChannelSelect.ALL_CHANNELS;
        globalEnvFilter = GlobalEnvironmentalFilter.FIFTY_AND_SIXTY;

        //Set Master Values for all channels for BandStop Filter
        masterBandStopFilterActive = FilterActiveOnChannel.OFF;
        masterBandStopStartFreq = 58;
        masterBandStopStopFreq = 62;
        masterBandStopFilterType = BrainFlowFilterType.BUTTERWORTH;
        masterBandStopFilterOrder = BrainFlowFilterOrder.FOUR;
        //Create and assign master value to all channels
        bandStopFilterActive = new FilterActiveOnChannel[channelCount];
        bandStopStartFreq = new double[channelCount];
        bandStopStopFreq = new double[channelCount];
        bandStopFilterType = new BrainFlowFilterType[channelCount];
        bandStopFilterOrder = new BrainFlowFilterOrder[channelCount];
        Arrays.fill(bandStopFilterActive, masterBandStopFilterActive);
        Arrays.fill(bandStopStartFreq, masterBandStopStartFreq);
        Arrays.fill(bandStopStopFreq, masterBandStopStopFreq);
        Arrays.fill(bandStopFilterType, masterBandStopFilterType);
        Arrays.fill(bandStopFilterOrder, masterBandStopFilterOrder);

        //Set Master Values for all channels for BandPass Filter
        //Default to 5-50Hz BandPass on all channels since this has been the default for years
        masterBandPassFilterActive = FilterActiveOnChannel.ON;
        masterBandPassStartFreq = 0.4;
        masterBandPassStopFreq = 50;
        masterBandPassFilterType = BrainFlowFilterType.BUTTERWORTH;
        masterBandPassFilterOrder = BrainFlowFilterOrder.FOUR;
        //Create and assign master value to all channels
        bandPassFilterActive = new FilterActiveOnChannel[channelCount];
        bandPassStartFreq = new double[channelCount];
        bandPassStopFreq = new double[channelCount];
        bandPassFilterType = new BrainFlowFilterType[channelCount];
        bandPassFilterOrder = new BrainFlowFilterOrder[channelCount];
        Arrays.fill(bandPassFilterActive, masterBandPassFilterActive);
        Arrays.fill(bandPassStartFreq, masterBandPassStartFreq);
        Arrays.fill(bandPassStopFreq, masterBandPassStopFreq);
        Arrays.fill(bandPassFilterType, masterBandPassFilterType);
        Arrays.fill(bandPassFilterOrder, masterBandPassFilterOrder);
    }
}
