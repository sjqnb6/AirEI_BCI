package SerialParser_;

/**
 * Hardware protocol boundary for the single-port 16-channel device.
 *
 * Serial reads do not preserve hardware-frame boundaries: one call can contain
 * half a frame or several frames. Implementations must retain partial bytes and
 * emit one sample for every complete valid hardware frame.
 */
public interface Serial16FrameDecoder {
    interface SampleSink {
        void onSample(Decoded16ChannelSample sample);
    }

    void reset();

    void accept(byte[] bytes, int offset, int length, SampleSink sink);

    /** Bytes sent once after the serial port is opened. */
    byte[] getStartCommand();

    /** Bytes sent once before the serial port is closed. */
    byte[] getStopCommand();
}
