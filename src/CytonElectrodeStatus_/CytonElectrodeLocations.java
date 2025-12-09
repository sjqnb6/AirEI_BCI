package CytonElectrodeStatus_;

import java.util.Arrays;

public enum CytonElectrodeLocations implements CytonElectrodeEnum {
    ONE_N(0, 1, "1N", "EEG", 0.100f, 0.38992f, "Fp1", 0.500f, 0.15265f),
    TWO_N(1, 2, "2N", "EEG", 0.100f, 0.51967f, "Fp2", 0.500f, 0.18308f),
    THREE_N(2, 3, "3N", "EEG", 0.100f, 0.64941f, "C3", 0.500f, 0.11283f),
    FOUR_N(3, 4, "4N", "EEG", 0.100f, 0.92547f, "C4", 0.500f, 0.17101f),
    FIVE_N(4, 5, "5N", "EEG", 0.1814f, 0.64941f, "P7", 0.38278f, 0.19765f),
    SIX_N(5, 6, "6N", "EEG", 0.11781f, 0.64941f, "P8", 0.61722f, 0.19765f),
    SEVEN_N(6, 7, "7N", "EEG", 0.17313f, 0.8882f, "O1", 0.37352f, 0.15514f),
    EIGHT_N(7, 8, "8N", "EEG", 0.12608f, 0.8882f, "O2", 0.6253f, 0.15514f),
    NINE_N(8, 9, "9N", "EEG", 0.100f, 0.38992f, "F7", 0.500f, 0.15265f),
    TEN_N(9, 10, "10N", "EEG", 0.100f, 0.51967f, "F8", 0.500f, 0.18308f),
    ELEVEN_N(10, 11, "11N", "EEG", 0.100f, 0.64941f, "F3", 0.500f, 0.11283f),
    TWELVE_N(11, 12, "12N", "EEG", 0.100f, 0.92547f, "F4", 0.500f, 0.17101f),
    THIRTEEN_N(12, 13, "13N", "EEG", 0.1814f, 0.64941f, "T7", 0.18278f, 0.19765f),
    FOURTEEN_N(13, 14, "14N", "EEG", 0.11781f, 0.64941f, "T8", 0.11722f, 0.19765f),
    FIFTEEN_N(14, 15, "15N", "EEG", 0.17313f, 0.8882f, "P3", 0.11352f, 0.11514f),
    SIXTEEN_N(15, 16, "16N", "EEG", 0.12608f, 0.8882f, "P4", 0.1153f, 0.11514f);

    private final int index;
    private final int guiChan;
    private final String adsChan;
    private final String measurement;
    private final float xPosScale;
    private final float yPosScale;
    private final String labelName;
    private final float labelXScale;
    private final float labelYScale;

    private static final CytonElectrodeLocations[] vals = values();

    CytonElectrodeLocations(int index, int channel, String adsChan, String type,
                            float xPosScale, float yPosScale, String labelName, float labelXScale, float labelYScale) {
        this.index = index;
        this.guiChan = channel;
        this.adsChan = adsChan;
        this.measurement = type;
        this.xPosScale = xPosScale;
        this.yPosScale = yPosScale;
        this.labelName = labelName;
        this.labelXScale = labelXScale;
        this.labelYScale = labelYScale;
    }

    @Override
    public int getIndex() {
        return index;
    }

    public static CytonElectrodeLocations getByIndex(int i) {
        if (i >= 0 && i < vals.length) {
            return vals[i];
        }
        throw new IndexOutOfBoundsException("Invalid index for CytonElectrodeLocations: " + i);
    }

    public static CytonElectrodeLocations getByADSChan(String value) {
        if (value != null) {
            for (CytonElectrodeLocations location : vals) {
                if (location.adsChan.equalsIgnoreCase(value)) {
                    return location;
                }
            }
        }
        System.out.println("getByADSChan - ERROR | Value == " + value);
        throw new IllegalArgumentException("Invalid electrode location: " + value);
    }

    public static String[] getAllLocationNames() {
        return Arrays.stream(vals)
                .map(Enum::name)
                .toArray(String[]::new);
    }

    @Override
    public Integer getChanGUI() {
        return guiChan;
    }

    @Override
    public String getADSChan() {
        return adsChan;
    }

    @Override
    public String getMeasurementType() {
        return measurement;
    }

    @Override
    public boolean isPin_N() {
        return adsChan.endsWith("N");
    }

    public static float getDiameterScalar() {
        return 0.022688f; // 80% scale
    }

    @Override
    public float[] getCircleXY() {
        return new float[]{ xPosScale, yPosScale };
    }

    @Override
    public String getLabelName() {
        return labelName;
    }

    @Override
    public float[] getLabelXY() {
        return new float[]{ labelXScale, labelYScale };
    }

    @Override
    public float getBorderScalar() {
        return 0.05f;
    }

    @Override
    public String toString() {
        return String.format("%s (GUI#%d, ADS=%s, Label=%s)", name(), guiChan, adsChan, labelName);
    }
}
