package TimeTrackingQueue_;

// For unit testing, we can pass this mock TTQTimeProvider
// that controls time
public class FakeTimeProvider implements TTQTimeProvider {
    private int ms = 0;

    public int getMS() {
        return ms;
    }

    public void addMS(int _ms) {
        ms += _ms;
    }
}