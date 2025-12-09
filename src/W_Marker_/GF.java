package W_Marker_;

import java.nio.ByteBuffer;

import static WidgetManager_.GVI.w_marker;

public class GF {

    //The following global functions are used by the Marker widget dropdowns. This method is the least amount of code.
    public  static void markerWindowDropdown(int n) {
        w_marker.setMarkerWindow(n);
    }

    public static void markerVertScaleDropdown(int n) {
        w_marker.setMarkerVertScale(n);
    }

    //Custom UDP receive handler for receiving markers from external sources
    public static void receiveMarkerViaUdp( byte[] data, String ip, int port ) {
        float markerValue = convertByteArrayToFloat(data);
        String message = Float.toString(markerValue);

        //println( "received: \""+message+"\" from "+ip+" on port "+port );
        w_marker.insertMarkerFromExternal(markerValue);
    }

    public static float convertByteArrayToFloat(byte[] array) {
        ByteBuffer buffer = ByteBuffer.wrap(array);
        return buffer.getFloat();
    }

}
