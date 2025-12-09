package InterfaceSerial_;

public class GVI {

    public static int _myCounter;
    public static int newPacketCounter = 0;
    public static boolean no_start_connection = false;
    public static byte inByte = -1;    // Incoming serial data
    public static boolean isOpenBCI;
    public static boolean isGettingPoll = false;
    public static boolean spaceFound = false;
    public static int hexToInt = 0;
    public static boolean currentlySyncing = false;
    public static long timeSinceStopRunning = 1000;

    //these variables are used for "Kill Spikes" ... duplicating the last received data packet if packets were droppeds
    public static boolean werePacketsDroppedSerial = false;
    public static int numPacketsDroppedSerial = 0;
}
