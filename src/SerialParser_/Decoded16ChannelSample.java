package SerialParser_;

import java.util.Arrays;

/** One complete 16-channel sample decoded from the single serial stream. */
public final class Decoded16ChannelSample {
    public static final int CHANNEL_COUNT = 16;
    public static final int NO_SEQUENCE = -1;

    private final int sequence;
    private final long receivedAtNanos;
    private final double[] channelsUv;

    public Decoded16ChannelSample(int sequence, long receivedAtNanos, double[] channelsUv) {
        if (channelsUv == null || channelsUv.length != CHANNEL_COUNT) {
            throw new IllegalArgumentException("A 16-channel sample must contain exactly 16 values");
        }
        this.sequence = sequence;
        this.receivedAtNanos = receivedAtNanos;
        this.channelsUv = Arrays.copyOf(channelsUv, channelsUv.length);
    }

    public int getSequence() {
        return sequence;
    }

    public long getReceivedAtNanos() {
        return receivedAtNanos;
    }

    public double getChannelUv(int channel) {
        return channelsUv[channel];
    }
}
