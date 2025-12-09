package DataProcessing_;

import BoardCyton_.BoardCytonConstants;

import java.util.Arrays;
import java.util.List;

import static GUI.GGVI.*;
import static SystemManager.GF.getCurrentBoardBufferSize;
import static java.lang.Math.sqrt;

public class GF {

    public static void processNewData() {

        List<double[]> currentData = currentBoard.getData(getCurrentBoardBufferSize());
        int[] exgChannels = currentBoard.getEXGChannels();
        int channelCount = currentBoard.getNumEXGChannels();

        //update the data buffers
        for (int Ichan=0; Ichan < channelCount; Ichan++) {
            for(int i = 0; i < getCurrentBoardBufferSize(); i++) {
                dataProcessingRawBuffer[Ichan][i] = (float)currentData.get(i)[exgChannels[Ichan]];
            }

            dataProcessingFilteredBuffer[Ichan] = dataProcessingRawBuffer[Ichan].clone();
        }

        //apply additional processing for the time-domain montage plot (ie, filtering)
        dataProcessing.process(dataProcessingFilteredBuffer, fftBuff);

        dataProcessing.newDataToSend = true;

        //look to see if the latest data is railed so that we can notify the user on the GUI
        for (int Ichan=0; Ichan < nchan; Ichan++) is_railed[Ichan].update(dataProcessingRawBuffer[Ichan], Ichan);

        //compute the electrode impedance. Do it in a very simple way [rms to amplitude, then uVolt to Volt, then Volt/Amp to Ohm]
        for (int Ichan=0; Ichan < nchan; Ichan++) {
            // Calculate the impedance
            float impedance = (float) ((sqrt(2.0)*dataProcessing.data_std_uV[Ichan]*1.0e-6) / BoardCytonConstants.leadOffDrive_amps);
            // Subtract the 2.2kOhm resistor
            impedance -= BoardCytonConstants.series_resistor_ohms;
            // Verify the impedance is not less than 0
            if (impedance < 0) {
                // Incase impedance some how dipped below 2.2kOhm
                impedance = 0;
            }
            // Store to the global variable
            data_elec_imp_ohm[Ichan] = impedance;
        }
    }

    public static void initializeFFTObjects(ddf.minim.analysis.FFT[] fftBuff, float[][] dataProcessingRawBuffer, int Nfft, float fs_Hz) {

        float[] fooData;
        for (int Ichan=0; Ichan < nchan; Ichan++) {
            //make the FFT objects...Following "SoundSpectrum" example that came with the Minim library
            fftBuff[Ichan].window(ddf.minim.analysis.FFT.HAMMING);

            //do the FFT on the initial data
            if (isFFTFiltered == true) {
                fooData = dataProcessingFilteredBuffer[Ichan];  //use the filtered data for the FFT
            } else {
                fooData = dataProcessingRawBuffer[Ichan];  //use the raw data for the FFT
            }
            fooData = Arrays.copyOfRange(fooData, fooData.length-Nfft, fooData.length);
            fftBuff[Ichan].forward(fooData); //compute FFT on this channel of data
        }
    }

}
