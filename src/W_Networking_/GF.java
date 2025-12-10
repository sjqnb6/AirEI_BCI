package W_Networking_;

import Globel.GUI;
import controlP5.ScrollableList;

import static WidgetManager_.GVI.w_networking;
import static processing.core.PApplet.println;

public class GF {

    /* Dropdown Menu Callback Functions */
    /**
     * @description Sets the selected protocol mode from the widget's dropdown menu
     * @param `n` {int} - Index of protocol item selected in menu
     */
    public static void Protocol(GUI MAIN, int protocolIndex) {
        MAIN.settings.nwProtocolSave = protocolIndex;
        if (protocolIndex == 0) {
            w_networking.protocolMode = "UDP";
        } else if (protocolIndex == 1) {
            w_networking.protocolMode = "LSL";
        } else if (protocolIndex == 2) {
            w_networking.protocolMode = "OSC";
        } else if (protocolIndex == 3) {
            w_networking.protocolMode = "Serial";
            w_networking.disableCertainOutputs(
                    (int) w_networking.cp5_networking_dropdowns.get(ScrollableList.class, "dataType1").getValue());
        }
        println("Networking: Protocol mode set to " + w_networking.protocolMode + ". Stopping network");
        w_networking.screenResized();
        w_networking.showCP5();
        if (!w_networking.getNetworkActive()) {
            w_networking.turnOffButton();
        }
    }

    public static void dataType1(int n) {
        w_networking.putCP5DataIntoMap();
    }

    public static void dataType2(int n) {
        w_networking.putCP5DataIntoMap();
    }

    public static void dataType3(int n) {
        w_networking.putCP5DataIntoMap();
    }

    public static void dataType4(int n) {
        w_networking.putCP5DataIntoMap();
    }

    public static void port_name(int n) {
        w_networking.setComPortToSave(n);
        w_networking.putCP5DataIntoMap();
    }

    public static void baud_rate(int n) {
        w_networking.putCP5DataIntoMap();
    }


}
