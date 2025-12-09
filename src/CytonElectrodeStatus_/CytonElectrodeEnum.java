package CytonElectrodeStatus_;

public interface CytonElectrodeEnum {
    public int getIndex();
    public Integer getChanGUI();
    public String getADSChan();
    public String getMeasurementType();
    public boolean isPin_N();
    public float[] getCircleXY();
    public String getLabelName();
    public float[] getLabelXY();
    public float getBorderScalar();
}
