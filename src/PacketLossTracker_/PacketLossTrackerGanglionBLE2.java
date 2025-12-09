package PacketLossTracker_;

import TimeTrackingQueue_.RealTimeProvider;
import TimeTrackingQueue_.TTQTimeProvider;

// With acceleration: sample index range 0-100, all sample indexes are duplicated except for zero.
// E.g. 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, ... , 99, 99, 100, 100, 0, 1, 1, 2, 2, 3, 3, ...
// Without acceleration: sample 0, then 101-200
public class PacketLossTrackerGanglionBLE2 extends PacketLossTrackerGanglionBLE {
    public PacketLossTrackerGanglionBLE2(int _sampleIndexChannel, int _timestampChannel) {
        this(_sampleIndexChannel, _timestampChannel, new RealTimeProvider());
    }

    PacketLossTrackerGanglionBLE2(int _sampleIndexChannel, int _timestampChannel, TTQTimeProvider _timeProvider) {
        super(_sampleIndexChannel, _timestampChannel, _timeProvider);

        // Add indices to array of indices
        // With acceleration: 0-100, all sample indexes are duplicated except for zero
        sampleIndexArrayAccel.add(0);
        for (int i = 1; i <= 100; i++) {
            sampleIndexArrayAccel.add(i);
            sampleIndexArrayAccel.add(i);
        }

        // Add indices to array of indices
        // Without acceleration: 0, then 101 to 200, all sample indexes are duplicated except for zero
        sampleIndexArrayNoAccel.add(0);
        for (int i = 101; i <= 200; i++) {
            sampleIndexArrayNoAccel.add(i);
            sampleIndexArrayNoAccel.add(i);
        }

        setAccelerometerActive(true);
    }
}