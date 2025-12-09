package BoardCyton_;

import static processing.core.PApplet.pow;

public class BoardCytonConstants {
    public static final float series_resistor_ohms = 2200; // Ohms. There is a series resistor on the 32 bit board.
    public static final float ADS1299_Vref = 4.5f;  //reference voltage for ADC in ADS1299.  set by its hardware
    public static final float ADS1299_gain = 24.f;  //assumed gain setting for ADS1299.  set by its Arduino code
    public static final float scale_fac_uVolts_per_count = ADS1299_Vref / ((float)(pow(2, 23)-1)) / ADS1299_gain  * 1000000.f; //ADS1299 datasheet Table 7, confirmed through experiment
    public static final float leadOffDrive_amps = 6.0e-9F;  //6 nA, set by its Arduino code
    public static final float accelScale = (float) (0.002 / (pow (2, 4)));
}
