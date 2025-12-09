package PacketLossTracker_;

import TimeTrackingQueue_.RealTimeProvider;
import TimeTrackingQueue_.TTQTimeProvider;

// sample index range 1-255, odd numbers only (skips evens)
public class PacketLossTrackerCytonSerialDaisy extends PacketLossTracker {

    public PacketLossTrackerCytonSerialDaisy(int _sampleIndexChannel, int _timestampChannel) {
        this(_sampleIndexChannel, _timestampChannel, new RealTimeProvider());
    }

    PacketLossTrackerCytonSerialDaisy(int _sampleIndexChannel, int _timestampChannel, TTQTimeProvider _timeProvider) {
        super(_sampleIndexChannel, _timestampChannel, _timeProvider);

        // add indices to array of indices
        // 0-254, event numbers only (skips odds)
        int firstIndex = 0;
        int lastIndex = 254;
        for (int i = firstIndex; i <= lastIndex; i += 2) {
            sampleIndexArray.add(i);
        }
    }
}