package W_SYHRV_;

import java.util.ArrayList;
import java.util.List;

/**
 * Decoder for the direct SY-HRV serial/Bluetooth packet documented as:
 * FF 01 HR SpO2 MICRO SYS DIA RESP FATIGUE RR SDNN RMSSD BODY_I BODY_F TEMP_I TEMP_F RESERVED(7) F1.
 */
public final class SyHrvPacketDecoder {
    public static final int FRAME_LENGTH = 24;
    private static final int FRAME_HEAD = 0xFF;
    private static final int FRAME_TYPE = 0x01;
    private static final int FRAME_TAIL = 0xF1;

    private final List<Byte> pending = new ArrayList<Byte>(FRAME_LENGTH * 2);

    public SyHrvFrame accept(int value) {
        pending.add((byte) (value & 0xFF));

        while (!pending.isEmpty() && u8(pending.get(0)) != FRAME_HEAD) {
            pending.remove(0);
        }
        if (pending.size() < FRAME_LENGTH) {
            return null;
        }

        if (u8(pending.get(1)) != FRAME_TYPE || u8(pending.get(FRAME_LENGTH - 1)) != FRAME_TAIL) {
            pending.remove(0);
            return null;
        }

        int[] data = new int[FRAME_LENGTH];
        for (int i = 0; i < FRAME_LENGTH; i++) {
            data[i] = u8(pending.remove(0));
        }

        return new SyHrvFrame(
                data[2], data[3], data[4],
                data[5], data[6], data[7],
                data[8], data[9], data[10], data[11],
                data[12] + data[13] / 100f,
                data[14] + data[15] / 100f,
                System.currentTimeMillis()
        );
    }

    public void reset() {
        pending.clear();
    }

    private static int u8(byte value) {
        return value & 0xFF;
    }
}
