package PacketLossTracker_;

import TimeTrackingQueue_.RealTimeProvider;
import TimeTrackingQueue_.TTQTimeProvider;

import java.util.ArrayList;

public class PacketLossTrackerGanglionBLE extends PacketLossTracker {

    ArrayList<Integer> sampleIndexArrayAccel = new ArrayList<Integer>();
    ArrayList<Integer> sampleIndexArrayNoAccel = new ArrayList<Integer>();

    public PacketLossTrackerGanglionBLE(int _sampleIndexChannel, int _timestampChannel) {
        this(_sampleIndexChannel, _timestampChannel, new RealTimeProvider());
    }

    public PacketLossTrackerGanglionBLE(int _sampleIndexChannel, int _timestampChannel, TTQTimeProvider _timeProvider) {
        super(_sampleIndexChannel, _timestampChannel, _timeProvider);
    }

    public void setAccelerometerActive(boolean active) {
        // choose correct array based on wether accel is active or not
        if (active) {
            sampleIndexArray = sampleIndexArrayAccel;
        }
        else {
            sampleIndexArray = sampleIndexArrayNoAccel;
        }

        reset();
    }
}