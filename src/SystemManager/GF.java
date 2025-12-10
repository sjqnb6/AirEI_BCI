package SystemManager;


import BoardNull_.BoardNull;
import Globel.GUI;

import static Debugging_.GF.output;
import static Debugging_.GF.outputError;
import static Globel.GUI.*;
import static WidgetManager_.GVI.*;
import static processing.core.PApplet.println;
import static processing.core.PApplet.str;

public class GF {
    //Global function to update the number of channels
    public static void updateToNChan(int _nchan) {
        nchan = _nchan;
        settings.slnchan = _nchan; //used in SoftwareSettings.pde only
        fftBuff = new ddf.minim.analysis.FFT[nchan];  //reinitialize the FFT buffer
        println("OpenBCI_GUI: Channel count set to " + str(nchan));
    }

    //halt the data collection
    public static void haltSystem() {
        if (!systemHasHalted) { //prevents system from halting more than once
            println("openBCI_GUI: haltSystem: Halting system for reconfiguration of settings...");

            //Reset the text for the Start Session buttonscreen. Skip when reiniting board while already in playback mode session.
            if (!reinitRequested) {
                controlPanel.initBox.setInitSessionButtonText("START SESSION");
            }

            if (w_networking != null && w_networking.getNetworkActive()) {
                w_networking.stopNetwork();
                println("openBCI_GUI: haltSystem: Network streams stopped");
            }

            if (w_focus != null) {
                w_focus.endSession();
            }

            stopRunning();  //stop data transfer

            topNav.resetStartStopButton();
            topNav.destroySmoothingButton(); //Destroy this button if exists and make null, will be re-init if needed next time session starts

            //reset connect loadStrings
            openBCI_portName = "N/A";  // Fixes inability to reconnect after halding  JAM 1/2017
            ganglion_portName = "";
            wifi_portName = "";

            controlPanel.resetListItems();

            if (eegDataSource == DATASOURCE_PLAYBACKFILE) {
                controlPanel.recentPlaybackBox.getRecentPlaybackFiles();
            }
            systemMode = SYSTEMMODE_PREINIT;

            recentPlaybackFilesHaveUpdated = false;

            dataLogger.uninitialize();

            currentBoard.uninitialize();
            currentBoard = new BoardNull(); // back to null

            sessionTimeElapsed.stop();

            systemHasHalted = true;
        }
    } //end of halt system


    public static void stopRunning() {
        //Check again if board is streaming to avoid IllegalStateException
        if (currentBoard.isStreaming() && topNav.dataStreamingButtonIsActive()) {
            //If streaming, attempt to stop stream
            currentBoard.stopStreaming();
            output("Data stream stopped.");
            try {
                streamTimeElapsed.stop();
                sessionTimeElapsed.suspend();
                dataLogger.onStopStreaming();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                outputError("GUI Error: Failed to stop Timer. Please make an issue on GitHub in the GUI repo.");
            }
        } else {
            output("Data stream is already stopped.");
        }
    }

    public static void startRunning() {
        // start streaming on the chosen board
        dataLogger.onStartStreaming();
        currentBoard.startStreaming();
        if (currentBoard.isStreaming()) {
            output("Data stream started.");
            // todo: this should really be some sort of signal that listeners can register for "OnStreamStarted"
            // close hardware settings if user starts streaming
            w_timeSeries.closeADSSettings();
            try {
                streamTimeElapsed.reset();
                streamTimeElapsed.start();
                sessionTimeElapsed.resume();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                outputError("Failed to start Timer.");
            }
        } else {
            outputError("Failed to start data stream. Please check hardware. See Console Log or BrainFlow Log for more details.");
        }
    }


    /**
     * @description Get the correct points of FFT based on sampling rate
     * @returns `int` - Points of FFT. 125Hz, 200Hz, 250Hz -> 256points. 1000Hz -> 1024points. 1600Hz -> 2048 points.
     */
    public static int getNfftSafe() {
        int sampleRate = currentBoard.getSampleRate();
        switch (sampleRate) {
            case 500:
                return 512;
            case 1000:
                return 1024;
            case 1600:
                return 2048;
            case 125:
            case 200:
            case 250:
            default:
                return 256;
        }
    }

    public static int getCurrentBoardBufferSize() {
        return dataBuff_len_sec * currentBoard.getSampleRate();
    }
}
