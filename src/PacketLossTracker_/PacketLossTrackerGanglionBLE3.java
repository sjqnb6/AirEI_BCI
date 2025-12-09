package PacketLossTracker_;

import TimeTrackingQueue_.RealTimeProvider;
import TimeTrackingQueue_.TTQTimeProvider;

// With acceleration: sample index range 0-100, all sample indexes are duplicated (including zero).
// E.g. 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, ... , 99, 99, 100, 100, 0, 0, 1, 1, 2, 2, 3, 3, ...
// Without acceleration: 101-200
public class PacketLossTrackerGanglionBLE3 extends PacketLossTrackerGanglionBLE {
    public PacketLossTrackerGanglionBLE3(int _sampleIndexChannel, int _timestampChannel) {
        this(_sampleIndexChannel, _timestampChannel, new RealTimeProvider());
    }

    PacketLossTrackerGanglionBLE3(int _sampleIndexChannel, int _timestampChannel, TTQTimeProvider _timeProvider) {
        super(_sampleIndexChannel, _timestampChannel, _timeProvider);

        for (int i = 0; i < 100; i++) {
            sampleIndexArrayAccel.add(i);
            sampleIndexArrayAccel.add(i);
        }

        for (int i = 100; i < 200; i++) {
            sampleIndexArrayNoAccel.add(i);
            sampleIndexArrayNoAccel.add(i);
        }

        setAccelerometerActive(true);
    }
}