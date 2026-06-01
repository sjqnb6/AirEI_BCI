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
import W_CFC_.W_CFC;
import W_CytonImpedance_.W_CytonImpedance;
import W_DigitalRead_.W_DigitalRead;
import W_EMGJoystick_.W_EMGJoystick;
import W_EMG_.W_emg;
import W_FFT_.W_fft;
import W_Focus_.W_Focus;
import W_GanglionImpedance_.W_GanglionImpedance;
import W_Head_.W_Head;
import W_PacketLoss.W_PacketLoss;
import W_Playback_.W_playback;
import W_Prediction_.W_Prediction;
import W_PulseSensor_.W_PulseSensor;
import W_Spectrogram_.W_Spectrogram;
import W_TimeSeries_.W_timeSeries;
import Widget_.Widget;
import W_Connectivity_.W_Connectivity;
import W_SignalQuality_.W_SignalQuality;
import W_Neur_.W_Neur;
import W_Indicator_.W_Indicator;
import java.util.ArrayList;

import static Globel.GUI.nchan;
import static WidgetManager_.GVI.*;
import Globel.GUI;
public class GF {

    //ADD YOUR WIDGET TO WIDGETS OF WIDGETMANAGER
    public static void setupWidgets(GUI _this, ArrayList<Widget> w){
        // println("  setupWidgets start -- " + millis());

        //Widget_0 -- The Widget number helps when debugging GUI front-end
        w_timeSeries = new W_timeSeries(_this);
        w_timeSeries.setTitle("时域波形");
        addWidget(w_timeSeries, w);

        //Widget_1
        w_fft = new W_fft(_this);
        w_fft.setTitle("频谱图");
        addWidget(w_fft, w);

        if (_this.currentBoard instanceof AccelerometerCapableBoard) {
            w_accelerometer = new W_Accelerometer(_this);
            w_accelerometer.setTitle("加速度计");
            addWidget(w_accelerometer, w);
        }

        // if (_this.currentBoard instanceof BoardCyton) {
        //     w_cytonImpedance = new W_CytonImpedance(_this);
        //     w_cytonImpedance.setTitle("Cyton 信号质量");
        //     addWidget(w_cytonImpedance, w);
        // }

        if(_this.currentBoard instanceof DataSourcePlayback){
            w_playback = new W_playback(_this);
            w_playback.setTitle("回放历史");
            addWidget(w_playback, w);
        }

        //only instantiate this widget if you are using a Ganglion board for live streaming
        // if(nchan == 4 && _this.currentBoard instanceof BoardGanglion){
        //     //If using Ganglion, this is Widget_3
        //     w_ganglionImpedance = new W_GanglionImpedance(_this);
        //     w_ganglionImpedance.setTitle("Ganglion 信号质量");
        //     addWidget(w_ganglionImpedance, w);
        // }

        w_focus = new W_Focus(_this);
        w_focus.setTitle("专注度");
        addWidget(w_focus, w);


        w_head = new W_Head(_this);
        w_head.setTitle("脑电地形图");
        addWidget(w_head, w);
        // w_networking = new W_Networking(_this);
        // w_networking.setTitle("网络通讯");
        // addWidget(w_networking, w);

        w_bandPower = new W_BandPower(_this);
        w_bandPower.setTitle("频带功率");
        addWidget(w_bandPower, w);

        // w_headPlot = new W_HeadPlot(_this);
        // w_headPlot.setTitle("脑电地形图");
        // addWidget(w_headPlot, w);

        w_emg = new W_emg(_this);
        w_emg.setTitle("肌电图");
        addWidget(w_emg, w);
        
        w_connectivity = new W_Connectivity(_this);
        w_connectivity.setTitle("脑区连接图谱");
        addWidget(w_connectivity, w);

        w_cfc = new W_CFC(_this);
        w_cfc.setTitle("跨频耦合图谱");
        addWidget(w_cfc, w);

        w_signalQuality = new W_SignalQuality(_this);
        w_signalQuality.setTitle("信号质量中心");
        addWidget(w_signalQuality, w);

        w_neur = new W_Neur(_this);
        w_neur.setTitle("神经聚类中枢");
        addWidget(w_neur, w);

        w_emgJoystick = new W_EMGJoystick(_this);
        w_emgJoystick.setTitle("肌电控制摇杆");
        addWidget(w_emgJoystick, w);

        w_spectrogram = new W_Spectrogram(_this);
        w_spectrogram.setTitle("时频图");
        addWidget(w_spectrogram, w);

        w_indicator = new W_Indicator(_this);
        w_indicator.setTitle("指标图");
        addWidget(w_indicator, w);
        // if(_this.currentBoard instanceof AnalogCapableBoard){
        //     w_pulsesensor = new W_PulseSensor(_this);
        //     w_pulsesensor.setTitle("脉搏传感器");
        //     addWidget(w_pulsesensor, w);
        // }

        // if(_this.currentBoard instanceof DigitalCapableBoard) {
        //     w_digitalRead = new W_DigitalRead(_this);
        //     w_digitalRead.setTitle("数字信号读取");
        //     addWidget(w_digitalRead, w);
        // }

        // if(_this.currentBoard instanceof AnalogCapableBoard) {
        //     w_analogRead = new W_AnalogRead(_this);
        //     w_analogRead.setTitle("模拟信号读取");
        //     addWidget(w_analogRead, w);
        // }

        if (_this.currentBoard instanceof Board) {
            w_packetLoss = new W_PacketLoss(_this);
            w_packetLoss.setTitle("丢包率");
            addWidget(w_packetLoss, w);
        }

        // w_marker = new W_Marker(_this);
        // w_marker.setTitle("事件标记");
        // addWidget(w_marker, w);

        w_prediction = new W_Prediction(_this);
        w_prediction.setTitle("疲劳检测");
        addWidget(w_prediction, w);

        //DEVELOPERS: Here is an example widget with the essentials/structure in place
        // w_template1 = new W_template(_this);
        // w_template1.setTitle("Widget Template 1");
        // addWidget(w_template1, w);




    }

    //this is a global function for adding new widgets--and their children (timeSeries, FFT, headPlot, etc.)--to the WidgetManager's widget ArrayList
    public static void addWidget(Widget myNewWidget, ArrayList<Widget> w){
        w.add(myNewWidget);
    }

}
