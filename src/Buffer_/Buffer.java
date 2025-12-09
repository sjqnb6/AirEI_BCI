package Buffer_;

import GUI.GUIManager;
import Globel.GUI;

import java.util.LinkedList;

import static processing.core.PApplet.ceil;


public class Buffer<T> extends LinkedList<T> {

    private int samplingRate;
    private int maxSize;
    private Long timeOfLastCallMS;

    private GUI MAIN;

    public Buffer(GUI MAIN, int samplingRate, int maxSize) {
        this.MAIN = MAIN;

        this.samplingRate = samplingRate;
        this.maxSize = maxSize;
        timeOfLastCallMS = null;
    }

    public Buffer(GUI MAIN, int samplingRate) {
        // max delay 1 second
        this(MAIN, samplingRate, samplingRate /*max size*/);
    }

    public void addNewEntry(T object) {
        this.add(object);
    }

    public T popFirstEntry() {
        return this.poll();
    }

    public int getDataCount() {
        long currentTime = MAIN.millis();
        int numSamples = 0;
        // skip first call to set time
        if (timeOfLastCallMS != null) {
            float deltaTimeSeconds = (float) ((currentTime - timeOfLastCallMS.longValue()) / 1000.0);
            // for safety, err on the side of delivering more samples (hence the use of ceil())
            numSamples = ceil(samplingRate * deltaTimeSeconds);
        }
        timeOfLastCallMS = currentTime;
        // ensure that buffer is not bigger than maxSize
        if (this.size() > maxSize) {
            numSamples += this.size() - maxSize;
        }
        return Math.min(numSamples, this.size());
    }
}