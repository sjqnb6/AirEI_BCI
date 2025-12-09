package W_Spectrogram_;

import processing.core.PApplet;

import static GUI.GGVI.settings;
import static WidgetManager_.GVI.w_spectrogram;
import static processing.core.PConstants.RGB;


public class GF {

    //These functions need to be global! These functions are activated when an item from the corresponding dropdown is selected
//triggered when there is an event in the Spectrogram Widget MaxFreq. Dropdown
    public static void SpectrogramMaxFreq(PApplet PApplet, int n) {
        settings.spectMaxFrqSave = n;
        //reset the vertical axis labels
        w_spectrogram.vertAxisLabel = w_spectrogram.vertAxisLabels[n];
        //Resize the height of the data image
        w_spectrogram.dataImageH = w_spectrogram.vertAxisLabel[0] * 2;
        //overwrite the existing image because the sample rate is about to change
        w_spectrogram.dataImg = PApplet.createImage(w_spectrogram.dataImageW, w_spectrogram.dataImageH, RGB);
    }

    public static void SpectrogramSampleRate(PApplet PApplet, int n) {
        settings.spectSampleRateSave = n;
        //overwrite the existing image because the sample rate is about to change
        w_spectrogram.dataImg = PApplet.createImage(w_spectrogram.dataImageW, w_spectrogram.dataImageH, RGB);
        w_spectrogram.horizAxisLabel = w_spectrogram.horizAxisLabels[n];
        if (n == 0) {
            w_spectrogram.numHorizAxisDivs = 6;
            w_spectrogram.setScrollSpeed(1000);
        } else if (n == 1) {
            w_spectrogram.numHorizAxisDivs = 6;
            w_spectrogram.setScrollSpeed(200);
        } else if (n == 2) {
            w_spectrogram.numHorizAxisDivs = 3;
            w_spectrogram.setScrollSpeed(100);
        } else if (n == 3) {
            w_spectrogram.numHorizAxisDivs = 3;
            w_spectrogram.setScrollSpeed(50);
        } else if (n == 4) {
            w_spectrogram.numHorizAxisDivs = 2;
            w_spectrogram.setScrollSpeed(25);
        }
        w_spectrogram.horizAxisLabelStrings.clear();
        w_spectrogram.fetchTimeStrings(w_spectrogram.numHorizAxisDivs);
    }

    public static void SpectrogramLogLin(int n) {
        settings.spectLogLinSave = n;
    }

}
