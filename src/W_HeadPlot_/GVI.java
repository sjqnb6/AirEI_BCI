package W_HeadPlot_;

public class GVI {

    public static float[] smoothFac = new float[]{0.0F, 0.5F, 0.75F, 0.9F, 0.95F, 0.98F, 0.99F, 0.999F}; //used by FFT & Headplot
    public static int smoothFac_ind = 3;    //initial index into the smoothFac array = 0.75 to start .. used by FFT & Head Plots

    // ----- these variable/methods are used for adjusting the intensity factor of the headplot opacity ---------------------------------------------------------------------------------------------------------
    public static float default_vertScale_uV = 200.0F; //this defines the Y-scale on the montage plots...this is the vertical space between traces
    public static float[] vertScaleFactor = { 0.25f, 0.5f, 1.0f, 2.0f, 5.0f, 50.0f};
    public static int vertScaleFactor_ind = 2;
    public static float vertScale_uV = default_vertScale_uV;

}
