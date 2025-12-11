package W_Spectrogram_;

//////////////////////////////////////////////////////
//                                                  //
//                  W_Spectrogram.pde               //
//                                                  //
//                                                  //
//    Created by: Richard Waltman, September 2019   //
//                                                  //

import Widget_.ChannelSelect;
import Widget_.Widget;
import controlP5.Controller;
import processing.core.PApplet;
import processing.core.PImage;
import processing.data.StringList;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;

import static Extras_.GF.log10;
import static Globel.GUI.nchan;

import Globel.GUI;
//////////////////////////////////////////////////////

public class W_Spectrogram extends Widget {
    GUI MAIN;
    //to see all core variables/methods of the Widget class, refer to Widget.pde
    public ChannelSelect spectChanSelectTop;
    public ChannelSelect spectChanSelectBot;
    private boolean chanSelectWasOpen = false;
    List<Controller> cp5ElementsToCheck = new ArrayList<Controller>();

    int xPos = 0;
    int hueLimit = 160;

    PImage dataImg;
    int dataImageW = 1800;
    int dataImageH = 200;
    int prevW = 0;
    int prevH = 0;
    float scaledWidth;
    float scaledHeight;
    int graphX = 0;
    int graphY = 0;
    int graphW = 0;
    int graphH = 0;
    int midLineY = 0;

    private int lastShift = 0;
    private int scrollSpeed = 100; // == 10Hz
    private boolean wasRunning = false;

    int paddingLeft = 54;
    int paddingRight = 26;
    int paddingTop = 8;
    int paddingBottom = 50;
    int numHorizAxisDivs = 3;
    int numVertAxisDivs = 8;
    final int[][] vertAxisLabels = {
            {20, 15, 10, 5, 0, 5, 10, 15, 20},
            {40, 30, 20, 10, 0, 10, 20, 30, 40},
            {60, 45, 30, 15, 0, 15, 30, 45, 60},
            {100, 75, 50, 25, 0, 25,  50, 75, 100},
            {120, 90, 60, 30, 0, 30, 60, 90, 120},
            {250, 188, 125, 63, 0, 63, 125, 188, 250}
    };
    int[] vertAxisLabel;
    final float[][] horizAxisLabels = {
            {30, 25, 20, 15, 10, 5, 0},
            {6, 5, 4, 3, 2, 1, 0},
            {3, 2, 1, 0},
            {1.5F, 1, .5F, 0},
            {1, .5F, 0}
    };
    float[] horizAxisLabel;
    StringList horizAxisLabelStrings;

    float[] topFFTAvg;
    float[] botFFTAvg;

    public W_Spectrogram(GUI MAIN){
        super(MAIN); //calls the parent CONSTRUCTOR method of Widget (DON'T REMOVE)
        this.MAIN = MAIN;
        //Add channel select dropdown to this widget
        spectChanSelectTop = new ChannelSelect(pApplet, this, x, y, w, navH, "Spectrogram_Channels_Top");
        spectChanSelectBot = new ChannelSelect(pApplet, this, x, y + navH, w, navH, "Spectrogram_Channels_Bot");
        activateDefaultChannels();
        spectChanSelectTop.setIsDualChannelSelect(true);
        spectChanSelectBot.setIsDualChannelSelect(true);
        spectChanSelectBot.setIsFirstRowChannelSelect(false);
        cp5ElementsToCheck.addAll(spectChanSelectTop.getCp5ElementsForOverlapCheck());
        cp5ElementsToCheck.addAll(spectChanSelectBot.getCp5ElementsForOverlapCheck());

        xPos = w - 1; //draw on the right, and shift pixels to the left
        prevW = w;
        prevH = h;
        graphX = x + paddingLeft;
        graphY = y + paddingTop;
        graphW = w - paddingRight - paddingLeft;
        graphH = h - paddingBottom - paddingTop;

        MAIN.settings.spectMaxFrqSave = 1;
        MAIN.settings.spectSampleRateSave = 2;
        MAIN.settings.spectLogLinSave = 0;
        vertAxisLabel = vertAxisLabels[MAIN.settings.spectMaxFrqSave];
        horizAxisLabel = horizAxisLabels[MAIN.settings.spectSampleRateSave];
        horizAxisLabelStrings = new StringList();
        //Fetch/calculate the time strings for the horizontal axis ticks
        fetchTimeStrings(numHorizAxisDivs);

        //This is the protocol for setting up dropdowns.
        //Note that these 3 dropdowns correspond to the 3 global functions below
        //You just need to make sure the "id" (the 1st String) has the same name as the corresponding function
        addDropdown("SpectrogramMaxFreq", "Max Freq", Arrays.asList(MAIN.settings.spectMaxFrqArray), MAIN.settings.spectMaxFrqSave);
        addDropdown("SpectrogramSampleRate", "Window", Arrays.asList(MAIN.settings.spectSampleRateArray), MAIN.settings.spectSampleRateSave);
        addDropdown("SpectrogramLogLin", "Log/Lin", Arrays.asList(MAIN.settings.fftLogLinArray), MAIN.settings.spectLogLinSave);

        //Resize the height of the data image using default
        dataImageH = vertAxisLabel[0] * 2;
        //Create image using correct dimensions! Fixes bug where image size and labels do not align on session start.
        dataImg = MAIN.createImage(dataImageW, dataImageH, MAIN.RGB);
    }

    public void update(){
        super.update(); //calls the parent update() method of Widget (DON'T REMOVE)

        //Update channel checkboxes and active channels
        spectChanSelectTop.update(x, y, w);
        spectChanSelectBot.update(x, y + navH, w);
        //Let the top channel select open the bottom one also so we can open both with 1 button
        if (chanSelectWasOpen != spectChanSelectTop.isVisible()) {
            spectChanSelectBot.setIsVisible(spectChanSelectTop.isVisible());
            chanSelectWasOpen = spectChanSelectTop.isVisible();
            //Allow spectrogram to flex size and position depending on if the channel select is open
            flexSpectrogramSizeAndPosition();
        }

        if (spectChanSelectTop.isVisible()) {
            lockElementsOnOverlapCheck(cp5ElementsToCheck);
        }

        if (MAIN.currentBoard.isStreaming()) {
            //Make sure we are always draw new pixels on the right
            xPos = dataImg.width - 1;
            //Fetch/calculate the time strings for the horizontal axis ticks
            fetchTimeStrings(numHorizAxisDivs);
        }

        //State change check
        if (MAIN.currentBoard.isStreaming() && !wasRunning) {
            onStartRunning();
        } else if (!MAIN.currentBoard.isStreaming() && wasRunning) {
            onStopRunning();
        }
    }

    private void onStartRunning() {
        wasRunning = true;
        lastShift = MAIN.millis();
    }

    private void onStopRunning() {
        wasRunning = false;
    }

    public void draw(){
        super.draw(); //calls the parent draw() method of Widget (DON'T REMOVE)

        //put your code here... //remember to refer to x,y,w,h which are the positioning variables of the Widget class

        //Scale the dataImage to fit in inside the widget
        float scaleW = (float)(graphW) / dataImageW;
        float scaleH = (float)(graphH) / dataImageH;

        MAIN.pushStyle();
        MAIN.fill(0);
        MAIN.rect(x, y, w, h); //draw a black background for the widget
        MAIN.popStyle();

        //draw the spectrogram if the widget is open, and update pixels if board is streaming data
        if (MAIN.currentBoard.isStreaming()) {
            MAIN.pushStyle();
            dataImg.loadPixels();

            //Shift all pixels to the left! (every scrollspeed ms)
            if(MAIN.millis() - lastShift > scrollSpeed) {
                for (int r = 0; r < dataImg.height; r++) {
                    if (r != 0) {
                        MAIN.arrayCopy(dataImg.pixels, dataImg.width * r, dataImg.pixels, dataImg.width * r - 1, dataImg.width);
                    } else {
                        //When there would be an ArrayOutOfBoundsException, account for it!
                        MAIN.arrayCopy(dataImg.pixels, dataImg.width * (r + 1), dataImg.pixels, r * dataImg.width, dataImg.width);
                    }
                }

                lastShift += scrollSpeed;
            }
            //for (int i = 0; i < fftLin_L.specSize() - 80; i++) {
            for (int i = 0; i <= dataImg.height/2; i++) {
                //LEFT SPECTROGRAM ON TOP
                float hueValue = hueLimit - MAIN.map((fftAvgs(spectChanSelectTop.activeChan, i)*32), 0, 256, 0, hueLimit);
                if (MAIN.settings.spectLogLinSave == 0) {
                    hueValue = MAIN.map(log10(hueValue), 0, 2, 0, hueLimit);
                }
                // colorMode is HSB, the range for hue is 256, for saturation is 100, brightness is 100.
                MAIN.colorMode(MAIN.HSB, 256, 100, 100);
                // color for stroke is specified as hue, saturation, brightness.
                MAIN.stroke((int)(hueValue), 100, 80);
                // plot a point using the specified stroke
                //point(xPos, i);
                int loc = xPos + ((dataImg.height/2 - i) * dataImg.width);
                if (loc >= dataImg.width * dataImg.height) loc = dataImg.width * dataImg.height - 1;
                try {
                    dataImg.pixels[loc] = MAIN.color((int)(hueValue), 100, 80);
                } catch (Exception e) {
                    MAIN.println("Major drawing error Spectrogram Left image!");
                }

                //RIGHT SPECTROGRAM ON BOTTOM
                hueValue = hueLimit - MAIN.map((fftAvgs(spectChanSelectBot.activeChan, i)*32), 0, 256, 0, hueLimit);
                if (MAIN.settings.spectLogLinSave == 0) {
                    hueValue = MAIN.map(log10(hueValue), 0, 2, 0, hueLimit);
                }
                // colorMode is HSB, the range for hue is 256, for saturation is 100, brightness is 100.
                MAIN.colorMode(MAIN.HSB, 256, 100, 100);
                // color for stroke is specified as hue, saturation, brightness.
                MAIN.stroke((int)(hueValue), 100, 80);
                int y_offset = -1;
                // Pixel = X + ((Y + Height/2) * Width)
                loc = xPos + ((i + dataImg.height/2 + y_offset) * dataImg.width);
                if (loc >= dataImg.width * dataImg.height) loc = dataImg.width * dataImg.height - 1;
                try {
                    dataImg.pixels[loc] = MAIN.color((int)(hueValue), 100, 80);
                } catch (Exception e) {
                    MAIN.println("Major drawing error Spectrogram Right image!");
                }
            }
            dataImg.updatePixels();
            MAIN.popStyle();
        }

        MAIN.pushMatrix();
        MAIN.translate(graphX, graphY);
        MAIN.scale(scaleW, scaleH);
        MAIN.image(dataImg, 0, 0);
        MAIN.popMatrix();

        spectChanSelectTop.draw();
        spectChanSelectBot.draw();
        drawAxes(scaleW, scaleH);
        drawCenterLine();
    }

    public void screenResized(){
        super.screenResized(); //calls the parent screenResized() method of Widget (DON'T REMOVE)

        spectChanSelectTop.screenResized(pApplet);
        spectChanSelectBot.screenResized(pApplet);
        graphX = x + paddingLeft;
        graphY = y + paddingTop;
        graphW = w - paddingRight - paddingLeft;
        graphH = h - paddingBottom - paddingTop;
        //Allow spectrogram to flex size and position depending on if the channel select is open
        if (spectChanSelectTop.isVisible()) {
            graphY += navH * 2;
            graphH -= navH * 2;
        }
    }

    public void mousePressed(){
        super.mousePressed(); //calls the parent mousePressed() method of Widget (DON'T REMOVE)

        spectChanSelectTop.mousePressed(this.dropdownIsActive); //Calls channel select mousePressed and checks if clicked
        spectChanSelectBot.mousePressed(this.dropdownIsActive);
    }

    public void mouseReleased(){
        super.mouseReleased(); //calls the parent mouseReleased() method of Widget (DON'T REMOVE)

    }

    void drawAxes(float scaledW, float scaledH) {

        MAIN.pushStyle();
        MAIN.fill(255);
        MAIN.textSize(14);
        //draw horizontal axis label
        MAIN.text("Time", x + w/2 - MAIN.textWidth("Time")/3, y + h - 9);
        MAIN.noFill();
        MAIN.stroke(255);
        MAIN.strokeWeight(2);
        //draw rectangle around the spectrogram
        MAIN.rect(graphX, graphY, scaledW * dataImageW, scaledH * dataImageH);
        MAIN.popStyle();

        MAIN.pushStyle();
        //draw horizontal axis ticks from left to right
        int tickMarkSize = 7; //in pixels
        float horizAxisX = graphX;
        float horizAxisY = graphY + scaledH * dataImageH;
        MAIN.stroke(255);
        MAIN.fill(255);
        MAIN.strokeWeight(2);
        MAIN.textSize(11);
        for (int i = 0; i <= numHorizAxisDivs; i++) {
            float offset = scaledW * dataImageW * ((float)(i) / numHorizAxisDivs);
            MAIN.line(horizAxisX + offset, horizAxisY, horizAxisX + offset, horizAxisY + tickMarkSize);
            if (horizAxisLabelStrings.get(i) != null) {
                MAIN.text(horizAxisLabelStrings.get(i), horizAxisX + offset - (int)MAIN.textWidth(horizAxisLabelStrings.get(i))/2, horizAxisY + tickMarkSize * 3);
            }
        }
        MAIN.popStyle();

        MAIN.pushStyle();
        MAIN.pushMatrix();
        MAIN.rotate(MAIN.radians(-90));
        MAIN.translate(-h/2 - MAIN.textWidth("Frequency (Hz)")/3, 20);
        MAIN.fill(255);
        MAIN.textSize(14);
        //draw y axis label
        MAIN.text("Frequency (Hz)", -y, x);
        MAIN.popMatrix();
        MAIN.popStyle();

        MAIN.pushStyle();
        //draw vertical axis ticks from top to bottom
        float vertAxisX = graphX;
        float vertAxisY = graphY;
        MAIN.stroke(255);
        MAIN.fill(255);
        MAIN.textSize(12);
        MAIN.strokeWeight(2);
        for (int i = 0; i <= numVertAxisDivs; i++) {
            float offset = scaledH * dataImageH * ((float)(i) / numVertAxisDivs);
            //if (i <= numVertAxisDivs/2) offset -= 2;
            MAIN.line(vertAxisX, vertAxisY + offset, vertAxisX - tickMarkSize, vertAxisY + offset);
            if (vertAxisLabel[i] == 0) midLineY = (int)(vertAxisY + offset);
            offset += paddingTop/2;
            MAIN.text(vertAxisLabel[i], vertAxisX - tickMarkSize*2 - MAIN.textWidth(Integer.toString(vertAxisLabel[i])), vertAxisY + offset);
        }
        MAIN.popStyle();

        drawColorScaleReference();
    }

    void drawCenterLine() {
        //draw a thick line down the middle to separate the two plots
        MAIN.pushStyle();
        MAIN.stroke(255);
        MAIN.strokeWeight(3);
        MAIN.line(graphX, midLineY, graphX + graphW, midLineY);
        MAIN.popStyle();
    }

    void drawColorScaleReference() {
        int colorScaleHeight = 128;
        //Dynamically scale the Log/Lin amplitude-to-color reference line. If it won't fit, don't draw it.
        if (graphH < colorScaleHeight) {
            colorScaleHeight = (int)(h * 1/2);
            if (colorScaleHeight > graphH) {
                return;
            }
        }
        MAIN.pushStyle();
        //draw color scale reference to the right of the spectrogram
        for (int i = 0; i < colorScaleHeight; i++) {
            float hueValue = hueLimit - MAIN.map(i * 2, 0, colorScaleHeight*2, 0, hueLimit);
            if (MAIN.settings.spectLogLinSave == 0) {
                hueValue = MAIN.map(MAIN.log(hueValue) / MAIN.log(10), 0, 2, 0, hueLimit);
            }
            //println(hueValue);
            // colorMode is HSB, the range for hue is 256, for saturation is 100, brightness is 100.
            MAIN.colorMode(MAIN.HSB, 256, 100, 100);
            // color for stroke is specified as hue, saturation, brightness.
            MAIN.stroke(MAIN.ceil(hueValue), 100, 80);
            MAIN.strokeWeight(10);
            MAIN.point(x + w - paddingRight/2 + 1, midLineY + colorScaleHeight/2 - i);
        }
        MAIN.popStyle();
    }

    void activateDefaultChannels() {
        int[] topChansToActivate;
        int[] botChansToActivate;
        if (nchan == 4) {
            topChansToActivate = new int[]{0, 2};
            botChansToActivate = new int[]{1, 3};
        } else if (nchan == 8) {
            topChansToActivate = new int[]{0, 2, 4, 6};
            botChansToActivate = new int[]{1, 3, 5, 7};
        } else {
            topChansToActivate = new int[]{0, 2, 4, 6, 8 ,10, 12, 14};
            botChansToActivate = new int[]{1, 3, 5, 7, 9, 11, 13, 15};
        }

        for (int i = 0; i < topChansToActivate.length; i++) {
            spectChanSelectTop.setToggleState(topChansToActivate[i], true);

        }

        for (int i = 0; i < botChansToActivate.length; i++) {
            spectChanSelectBot.setToggleState(botChansToActivate[i], true);
        }
    }

    void flexSpectrogramSizeAndPosition() {
        if (spectChanSelectTop.isVisible()) {
            graphY += navH * 2;
            graphH -= navH * 2;
        } else {
            graphY -= navH * 2;
            graphH += navH * 2;
        }
    }

    void setScrollSpeed(int i) {
        scrollSpeed = i;
    }

    float fftAvgs(List<Integer> _activeChan, int freqBand) {
        float sum = 0f;
        for (int i = 0; i < _activeChan.size(); i++) {
            sum += MAIN.fftBuff[_activeChan.get(i)].getBand(freqBand);
        }
        return sum / _activeChan.size();
    }

    void fetchTimeStrings(int numAxisTicks) {
        horizAxisLabelStrings.clear();
        LocalDateTime time;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        if (getCurrentTimeStamp() == 0) {
            time = LocalDateTime.now();
        } else {
            time = LocalDateTime.ofInstant(Instant.ofEpochMilli(getCurrentTimeStamp()),
                    TimeZone.getDefault().toZoneId());
        }

        for (int i = 0; i <= numAxisTicks; i++) {
            long l = (long)(horizAxisLabel[i] * 60f);
            LocalDateTime t = time.minus(l, ChronoUnit.SECONDS);
            horizAxisLabelStrings.append(t.format(formatter));
        }
    }

    //Identical to the method in TimeSeries, but allows spectrogram to get the data directly from the playback data in the background
    //Find times to display for playback position
    private long getCurrentTimeStamp() {
        //return current playback time
        List<double[]> currentData = MAIN.currentBoard.getData(1);
        int timeStampChan = MAIN.currentBoard.getTimestampChannel();
        long timestampMS = (long)(currentData.get(0)[timeStampChan] * 1000.0);
        return timestampMS;
    }
};