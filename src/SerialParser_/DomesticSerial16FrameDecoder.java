package SerialParser_;

/**
 * Reserved decoder for the domestic 16-channel hardware protocol.
 *
 * When the hardware frame is finalized, implement only this class. Serial port
 * management, background reading, buffering and the GUI data path are already
 * handled outside it.
 */
public final class DomesticSerial16FrameDecoder implements Serial16FrameDecoder {
    // Keep serial control compatible with the existing 8-channel path.
    private static final byte[] START_COMMAND = {(byte) 'b'};
    private static final byte[] STOP_COMMAND = {(byte) 'c'};

    @Override
    public void reset() {
        // TODO(hardware): clear partial-frame bytes and parser state.
    }

    @Override
    public void accept(byte[] bytes, int offset, int length, SampleSink sink) {
        /*
         * TODO(hardware):
         * 1. Find the frame header and retain incomplete frames between calls.
         * 2. Validate frame length, footer and checksum/CRC.
         * 3. Decode the sample sequence number (0..255 is recommended).
         * 4. Decode CH1..CH16 and convert ADC counts to microvolts.
         * 5. For every valid frame emit:
         *
         * sink.onSample(new Decoded16ChannelSample(
         *         sequence,
         *         System.nanoTime(),
         *         channelsUv));
         */
    }

    @Override
    public byte[] getStartCommand() {
        return START_COMMAND;
    }

    @Override
    public byte[] getStopCommand() {
        return STOP_COMMAND;
    }
}
