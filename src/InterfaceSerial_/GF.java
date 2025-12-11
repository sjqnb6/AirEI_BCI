package InterfaceSerial_;

import static Globel.GUI.board_message;
import static Globel.GUI.iSerial;
import static InterfaceSerial_.GVI.*;
import static processing.core.PApplet.print;
import static processing.core.PApplet.println;
import Globel.GUI;
public class GF {

    void serialEvent(processing.serial.Serial port){
        //check to see which serial port it is
        if (iSerial.isOpenBCISerial(port)) {

            boolean echoBytes;

            if (iSerial.isStateNormal() != true) {  // || printingRegisters == true){
                echoBytes = true;
            } else {
                echoBytes = false;
            }
            iSerial.read(echoBytes);
            if (iSerial.get_isNewDataPacketAvailable()) {
                println("woo got a new packet");
                //copy packet into buffer of data packets

                iSerial.set_isNewDataPacketAvailable(false); //resets isNewDataPacketAvailable to false

                newPacketCounter++;
            }
        } else {

            //Used for serial communications, primarily everything in no_start_connection
            if (no_start_connection) {


                if (board_message == null || _myCounter>2) {
                    board_message = new StringBuilder();
                    _myCounter = 0;
                }

                inByte = (byte)(port.read());
                print(inByte);
                if ((char)(inByte) == 'S' || (char)(inByte) == 'F') isOpenBCI = true;

                // print(char(inByte));
                if (inByte != -1) {
                    if (isGettingPoll) {
                        if (inByte != '$') {
                            if (!spaceFound) board_message.append((char)(inByte));
                        else hexToInt = Integer.parseInt(String.format("%02X", inByte), 16);

                            if ((char)(inByte) == ' ') spaceFound = true;
                        } else _myCounter++;
                    } else {
                        if (inByte != '$') board_message.append((char)(inByte));
                    else _myCounter++;
                    }
                }
            } else {
                //println("Recieved serial data not from OpenBCI"); //this is a bit of a lie
                inByte = (byte)(port.read());
                if (isOpenBCI) {

                    if (board_message == null || _myCounter >2) {
                        board_message = new StringBuilder();
                        _myCounter=0;
                    }
                    if(inByte != '$'){
                        board_message.append((char)(inByte));
                    } else { _myCounter++; }
                } else if((char)(inByte) == 'S' || (char)(inByte) == 'F'){
                    isOpenBCI = true;
                    if(board_message == null){
                        board_message = new StringBuilder();
                        board_message.append((char)(inByte));
                    }
                }
            }
        }
    }

}
