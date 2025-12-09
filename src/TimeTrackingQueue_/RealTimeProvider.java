package TimeTrackingQueue_;

import processing.core.PApplet;

public class RealTimeProvider extends PApplet implements TTQTimeProvider {
    public int getMS() {
        return millis();
    }
}