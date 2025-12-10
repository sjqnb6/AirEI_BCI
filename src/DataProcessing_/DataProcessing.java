package DataProcessing_;

import EmgSettings_.EmgSettings;
import GUI.GUIManager;
import brainflow.BrainFlowError;
import brainflow.DataFilter;
import brainflow.NoiseTypes;

import java.util.Arrays;

import static Extras_.GF.*;
import static Globel.GUI.*;
import static SystemManager.GF.getNfftSafe;
import static W_HeadPlot_.GVI.smoothFac;
import static W_HeadPlot_.GVI.smoothFac_ind;
import static WidgetManager_.GVI.*;
import Globel.GUI;
public class DataProcessing {
    GUI MAIN;
    private float fs_Hz;  //sample rate
    private int nchan;
    public float[] data_std_uV;
    public float[] polarity;
    boolean newDataToSend;
    final int[] processing_band_low_Hz = {
            1, 4, 8, 13, 30
    }; //lower bound for each frequency band of interest (2D classifier only)
    final int[] processing_band_high_Hz = {
            4, 8, 13, 30, 55
    };  //upper bound for each frequency band of interest
    public float[][] avgPowerInBins;
    public float[] headWidePower;

    public EmgSettings emgSettings;

    public DataProcessing(GUI MAIN, int NCHAN, float sample_rate_Hz) {
        nchan = NCHAN;
        fs_Hz = sample_rate_Hz;
        data_std_uV = new float[nchan];
        polarity = new float[nchan];
        newDataToSend = false;
        avgPowerInBins = new float[nchan][processing_band_low_Hz.length];
        headWidePower = new float[processing_band_low_Hz.length];

        emgSettings = new EmgSettings(MAIN);
    }

    //Process data on a channel-by-channel basis
    private synchronized void processChannel(int Ichan, float[][] data_forDisplay_uV, float[] prevFFTdata) {
        int Nfft = getNfftSafe();
        double foo;

        // Filter the data in the time domain
        // TODO: Use double arrays here and convert to float only to plot data.
        // ^^^ This might not feasible or meaningful performance improvement. I looked into it a while ago and it seems we need floats for the FFT library also. -RW 2022)
        try {
            double[] tempArray = floatToDoubleArray(data_forDisplay_uV[Ichan]);

            //Apply BandStop filter if the filter should be active on this channel
            if (filterSettings.values.bandStopFilterActive[Ichan].isActive()) {
                DataFilter.perform_bandstop(
                        tempArray,
                        MAIN.currentBoard.getSampleRate(),
                        filterSettings.values.bandStopStartFreq[Ichan],
                        filterSettings.values.bandStopStopFreq[Ichan],
                        filterSettings.values.bandStopFilterOrder[Ichan].getValue(),
                        filterSettings.values.bandStopFilterType[Ichan].getValue(),
                        1.0);
            }

            //Apply BandPass filter if the filter should be active on this channel
            if (filterSettings.values.bandPassFilterActive[Ichan].isActive()) {
                DataFilter.perform_bandpass(
                        tempArray,
                        MAIN.currentBoard.getSampleRate(),
                        filterSettings.values.bandPassStartFreq[Ichan],
                        filterSettings.values.bandPassStopFreq[Ichan],
                        filterSettings.values.bandPassFilterOrder[Ichan].getValue(),
                        filterSettings.values.bandPassFilterType[Ichan].getValue(),
                        1.0);
            }

            //Apply Environmental Noise filter on all channels. Do it like this since there are no codes for NONE or FIFTY_AND_SIXTY in BrainFlow
            switch (filterSettings.values.globalEnvFilter) {
                case FIFTY_AND_SIXTY:
                    DataFilter.remove_environmental_noise(
                            tempArray,
                            MAIN.currentBoard.getSampleRate(),
                            NoiseTypes.FIFTY.get_code());
                    DataFilter.remove_environmental_noise(
                            tempArray,
                            MAIN.currentBoard.getSampleRate(),
                            NoiseTypes.SIXTY.get_code());
                    break;
                case FIFTY:
                    DataFilter.remove_environmental_noise(
                            tempArray,
                            MAIN.currentBoard.getSampleRate(),
                            NoiseTypes.FIFTY.get_code());
                    break;
                case SIXTY:
                    DataFilter.remove_environmental_noise(
                            tempArray,
                            MAIN.currentBoard.getSampleRate(),
                            NoiseTypes.SIXTY.get_code());
                    break;
                default:
                    break;
            }

            doubleToFloatArray(tempArray, data_forDisplay_uV[Ichan]);
        } catch (BrainFlowError e) {
            e.printStackTrace();
        }

        //compute the standard deviation of the filtered signal...this is for the head plot
        float[] fooData_filt = dataProcessingFilteredBuffer[Ichan];  //use the filtered data
        fooData_filt = Arrays.copyOfRange(fooData_filt, fooData_filt.length-((int)fs_Hz), fooData_filt.length);   //just grab the most recent second of data
        data_std_uV[Ichan]=std(fooData_filt); //compute the standard deviation for the whole array "fooData_filt"

        //copy the previous FFT data...enables us to apply some smoothing to the FFT data
        for (int I=0; I < fftBuff[Ichan].specSize(); I++) {
            prevFFTdata[I] = fftBuff[Ichan].getBand(I); //copy the old spectrum values
        }

        //prepare the data for the new FFT
        float[] fooData;
        if (isFFTFiltered == true) {
            fooData = dataProcessingFilteredBuffer[Ichan];  //use the filtered data for the FFT
        } else {
            fooData = dataProcessingRawBuffer[Ichan];  //use the raw data for the FFT
        }
        fooData = Arrays.copyOfRange(fooData, fooData.length-Nfft, fooData.length);   //trim to grab just the most recent block of data
        float meanData = mean(fooData);  //compute the mean
        for (int I=0; I < fooData.length; I++) fooData[I] -= meanData; //remove the mean (for a better looking FFT

        //compute the FFT
        fftBuff[Ichan].forward(fooData); //compute FFT on this channel of data

        // FFT ref: https://www.mathworks.com/help/matlab/ref/fft.html
        // first calculate double-sided FFT amplitude spectrum
        for (int I=0; I <= Nfft/2; I++) {
            fftBuff[Ichan].setBand(I, (float)(fftBuff[Ichan].getBand(I) / Nfft));
        }
        // then convert into single-sided FFT spectrum: DC & Nyquist (i=0 & i=N/2) remain the same, others multiply by two.
        for (int I=1; I < Nfft/2; I++) {
            fftBuff[Ichan].setBand(I, (float)(fftBuff[Ichan].getBand(I) * 2));
        }

        //average the FFT with previous FFT data so that it makes it smoother in time
        double min_val = 0.01d;
        for (int I=0; I < fftBuff[Ichan].specSize(); I++) {   //loop over each fft bin
            if (prevFFTdata[I] < min_val) prevFFTdata[I] = (float)min_val; //make sure we're not too small for the log calls
            foo = fftBuff[Ichan].getBand(I);
            if (foo < min_val) foo = min_val; //make sure this value isn't too small

            if (true) {
                //smooth in dB power space
                foo =   (1.0d-smoothFac[smoothFac_ind]) * Math.log(Math.pow(foo, 2));
                foo += smoothFac[smoothFac_ind] * Math.log(Math.pow((double)prevFFTdata[I], 2));
                foo = Math.sqrt(Math.exp(foo)); //average in dB space
            } else {
                //smooth (average) in linear power space
                foo =   (1.0d-smoothFac[smoothFac_ind]) * Math.pow(foo, 2);
                foo+= smoothFac[smoothFac_ind] * Math.pow((double)prevFFTdata[I], 2);
                // take sqrt to be back into uV_rtHz
                foo = Math.sqrt(foo);
            }
            fftBuff[Ichan].setBand(I, (float)foo); //put the smoothed data back into the fftBuff data holder for use by everyone else
            // fftBuff[Ichan].setBand(I, 1.0f);  // test
        } //end loop over FFT bins

        // calculate single-sided psd by single-sided FFT amplitude spectrum
        // PSD ref: https://www.mathworks.com/help/dsp/ug/estimate-the-power-spectral-density-in-matlab.html
        // when i = 1 ~ (N/2-1), psd = (N / fs) * mag(i)^2 / 4
        // when i = 0 or i = N/2, psd = (N / fs) * mag(i)^2

        for (int i = 0; i < processing_band_low_Hz.length; i++) {
            float sum = 0;
            // int binNum = 0;
            for (int Ibin = 0; Ibin <= Nfft/2; Ibin ++) { // loop over FFT bins
                float FFT_freq_Hz = fftBuff[Ichan].indexToFreq(Ibin);   // center frequency of this bin
                float psdx = 0;
                // if the frequency matches a band
                if (FFT_freq_Hz >= processing_band_low_Hz[i] && FFT_freq_Hz < processing_band_high_Hz[i]) {
                    if (Ibin != 0 && Ibin != Nfft/2) {
                        psdx = fftBuff[Ichan].getBand(Ibin) * fftBuff[Ichan].getBand(Ibin) * Nfft/currentBoard.getSampleRate() / 4;
                    }
                    else {
                        psdx = fftBuff[Ichan].getBand(Ibin) * fftBuff[Ichan].getBand(Ibin) * Nfft/currentBoard.getSampleRate();
                    }
                    sum += psdx;
                    // binNum ++;
                }
            }
            avgPowerInBins[Ichan][i] = sum;   // total power in a band
            // println(i, binNum, sum);
        }
    }

    public void process(float[][] data_forDisplay_uV, ddf.minim.analysis.FFT[] fftData) {              //holds the FFT (frequency spectrum) of the latest data

        float prevFFTdata[] = new float[fftBuff[0].specSize()];

        for (int Ichan=0; Ichan < nchan; Ichan++) {
            processChannel(Ichan, data_forDisplay_uV, prevFFTdata);
        } //end the loop over channels.

        for (int i = 0; i < processing_band_low_Hz.length; i++) {
            float sum = 0;

            for (int j = 0; j < nchan; j++) {
                sum += avgPowerInBins[j][i];
            }
            headWidePower[i] = sum/nchan;   // averaging power over all channels
        }

        // Calculate data used for Headplot
        // Find strongest channel
        int refChanInd = findMax(data_std_uV);
        //println("EEG_Processing: strongest chan (one referenced) = " + (refChanInd+1));
        float[] refData_uV = dataProcessingFilteredBuffer[refChanInd];  //use the filtered data
        refData_uV = Arrays.copyOfRange(refData_uV, refData_uV.length-((int)fs_Hz), refData_uV.length);   //just grab the most recent second of data
        // Compute polarity of each channel
        for (int Ichan=0; Ichan < nchan; Ichan++) {
            float[] fooData_filt = dataProcessingFilteredBuffer[Ichan];  //use the filtered data
            fooData_filt = Arrays.copyOfRange(fooData_filt, fooData_filt.length-((int)fs_Hz), fooData_filt.length);   //just grab the most recent second of data
            float dotProd = calcDotProduct(fooData_filt, refData_uV);
            if (dotProd >= 0.0f) {
                polarity[Ichan]= 1.0F;
            } else {
                polarity[Ichan]= (float) -1.0;
            }
        }

        /////////////////////////////////////////////////////////////
        // Compute widget values independent of widgets being open //
        //                       -RW #1094                         //
        /////////////////////////////////////////////////////////////
        emgSettings.values.process(dataProcessingFilteredBuffer);
        w_focus.updateFocusWidgetData();
        w_bandPower.updateBandPowerWidgetData();
        w_emgJoystick.updateEmgJoystickWidgetData();
        if (w_pulsesensor != null) {
            w_pulsesensor.updatePulseSensorWidgetData();
        }

        w_networking.updateNetworkingWidgetData();
    }
}
