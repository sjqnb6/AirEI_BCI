package WidgetManager_;

import AccelerometerCapableBoard_.AccelerometerCapableBoard;
import AnalogCapableBoard_.AnalogCapableBoard;
import BoardCyton_.BoardCyton;
import BoardGanglion_.BoardGanglion;
import Board_.Board;
import DataSourcePlayback_.DataSourcePlayback;
import DigitalCapableBoard_.DigitalCapableBoard;
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
import W_PulseSensor_.W_PulseSensor;
import W_Spectrogram_.W_Spectrogram;
import W_Template_.W_template;
import W_TimeSeries_.W_timeSeries;
import Widget_.Widget;
import processing.core.PApplet;

import java.util.ArrayList;

import static GUI.GGVI.currentBoard;
import static GUI.GGVI.nchan;
import static WidgetManager_.GVI.*;

public class GF {

    //ADD YOUR WIDGET TO WIDGETS OF WIDGETMANAGER
    public static void setupWidgets(PApplet _this, ArrayList<Widget> w){
        // println("  setupWidgets start -- " + millis());

        //Widget_0 -- The Widget number helps when debugging GUI front-end
        w_timeSeries = new W_timeSeries(_this);
        w_timeSeries.setTitle("Time Series");
        addWidget(w_timeSeries, w);

        //Widget_1
        w_fft = new W_fft(_this);
        w_fft.setTitle("FFT Plot");
        addWidget(w_fft, w);

        if (currentBoard instanceof AccelerometerCapableBoard) {
            w_accelerometer = new W_Accelerometer(_this);
            w_accelerometer.setTitle("Accelerometer");
            addWidget(w_accelerometer, w);
        }

        if (currentBoard instanceof BoardCyton) {
            w_cytonImpedance = new W_CytonImpedance(_this);
            w_cytonImpedance.setTitle("Cyton Signal");
            addWidget(w_cytonImpedance, w);
        }

        if(currentBoard instanceof DataSourcePlayback){
            w_playback = new W_playback(_this);
            w_playback.setTitle("Playback History");
            addWidget(w_playback, w);
        }

        //only instantiate this widget if you are using a Ganglion board for live streaming
        if(nchan == 4 && currentBoard instanceof BoardGanglion){
            //If using Ganglion, this is Widget_3
            w_ganglionImpedance = new W_GanglionImpedance(_this);
            w_ganglionImpedance.setTitle("Ganglion Signal");
            addWidget(w_ganglionImpedance, w);
        }

        w_focus = new W_Focus(_this);
        w_focus.setTitle("Focus Widget");
        addWidget(w_focus, w);

        w_networking = new W_Networking(_this);
        w_networking.setTitle("Networking");
        addWidget(w_networking, w);

        w_bandPower = new W_BandPower(_this);
        w_bandPower.setTitle("Band Power");
        addWidget(w_bandPower, w);

        w_headPlot = new W_HeadPlot(_this);
        w_headPlot.setTitle("Head Plot");
        addWidget(w_headPlot, w);

        w_emg = new W_emg(_this);
        w_emg.setTitle("EMG");
        addWidget(w_emg, w);

        w_emgJoystick = new W_EMGJoystick(_this);
        w_emgJoystick.setTitle("EMG Joystick");
        addWidget(w_emgJoystick, w);

        w_spectrogram = new W_Spectrogram(_this);
        w_spectrogram.setTitle("Spectrogram");
        addWidget(w_spectrogram, w);

        if(currentBoard instanceof AnalogCapableBoard){
            w_pulsesensor = new W_PulseSensor(_this);
            w_pulsesensor.setTitle("Pulse Sensor");
            addWidget(w_pulsesensor, w);
        }

        if(currentBoard instanceof DigitalCapableBoard) {
            w_digitalRead = new W_DigitalRead(_this);
            w_digitalRead.setTitle("Digital Read");
            addWidget(w_digitalRead, w);
        }

        if(currentBoard instanceof AnalogCapableBoard) {
            w_analogRead = new W_AnalogRead(_this);
            w_analogRead.setTitle("Analog Read");
            addWidget(w_analogRead, w);
        }

        if (currentBoard instanceof Board) {
            w_packetLoss = new W_PacketLoss(_this);
            w_packetLoss.setTitle("Packet Loss");
            addWidget(w_packetLoss, w);
        }

        w_marker = new W_Marker(_this);
        w_marker.setTitle("Marker");
        addWidget(w_marker, w);

        //DEVELOPERS: Here is an example widget with the essentials/structure in place
        w_template1 = new W_template(_this);
        w_template1.setTitle("Widget Template 1");
        addWidget(w_template1, w);




    }

    //this is a global function for adding new widgets--and their children (timeSeries, FFT, headPlot, etc.)--to the WidgetManager's widget ArrayList
    public static void addWidget(Widget myNewWidget, ArrayList<Widget> w){
        w.add(myNewWidget);
    }

}
