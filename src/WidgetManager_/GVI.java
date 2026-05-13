package WidgetManager_;

import W_Accelerometer_.W_Accelerometer;
import W_AnalogRead_.W_AnalogRead;
import W_BandPower_.W_BandPower;
import W_CytonImpedance_.W_CytonImpedance;
import W_DigitalRead_.W_DigitalRead;
import W_EMGJoystick_.W_EMGJoystick;
import W_EMG_.W_emg;
import W_FFT_.W_fft;
import W_Focus_.W_Focus;
import W_GanglionImpedance_.W_GanglionImpedance;
import W_HeadPlot_.W_HeadPlot;
import W_Marker_.W_Marker;
import W_Networking_.W_Networking;
import W_PacketLoss.W_PacketLoss;
import W_Playback_.W_playback;
import W_Prediction_.W_Prediction;
import W_PulseSensor_.W_PulseSensor;
import W_Spectrogram_.W_Spectrogram;
import W_Template_.W_template;
import W_TimeSeries_.W_timeSeries;

public class GVI {

    // MAKE YOUR WIDGET GLOBALLY
    public static W_timeSeries w_timeSeries;
    public static W_fft w_fft;
    public static W_Networking w_networking;
    public static W_BandPower w_bandPower;
    public static W_Accelerometer w_accelerometer;
    public static W_CytonImpedance w_cytonImpedance;
    public static W_GanglionImpedance w_ganglionImpedance;
    public static W_HeadPlot w_headPlot;
    public static W_template w_template1;
    public static W_emg w_emg;
    public static W_PulseSensor w_pulsesensor;
    public static W_AnalogRead w_analogRead;
    public static W_DigitalRead w_digitalRead;
    public static W_playback w_playback;
    public static W_Spectrogram w_spectrogram;
    public static W_PacketLoss w_packetLoss;
    public static W_Focus w_focus;
    public static W_EMGJoystick w_emgJoystick;
    public static W_Marker w_marker;
    public static W_Prediction w_prediction;

}
