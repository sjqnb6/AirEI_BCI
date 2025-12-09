
//import CustomCp5Classes_.CopyPaste;
import ConsoleLog_.CustomOutputStream;
import CustomCp5Classes_.CopyPaste;
import DirectoryManager_.DirectoryManager;
import Globel.GGVI;
import Globel.GUI;
import processing.core.PApplet;

import java.io.File;

import static Globel.GF.isMac;

public class Main extends GUI {

    static public void main(String[] passedArgs) {
//        String[] appletArgs = new String[] { "com.jiakii.processtest.Test" };
        PApplet.main("Main");
    }

    public void settings() {
        super.settings();

        //LINUX GFX FIX #816
        System.setProperty("jogl.disable.openglcore", "false");

        win_w = 1024;
        win_h = 768;

        // If less than 1366x768, set smaller minimum GUI size
        // Nov 2020 - Accomodate as low as 1024 X 640
        if (displayWidth <= 1366 || displayHeight <= 768) {
            win_w = 980;
            win_h = 580;
        }
        size(win_w, win_h);

    }


    public void setup() {
        super.setup();
        frameRate(120);  // 设置draw函数执行频率
        surface.setResizable(true);         // ✅ 允许最大化        frameRate(90);  // 设置draw函数执行频率
        surface.setLocation(100, 50);  // 将窗口左上角设置在屏幕坐标 (100, 50)

        copyPaste = new CopyPaste(this);

        //V1 FONTS
        f1 = createFont("fonts/Raleway-SemiBold.otf", 16);
        //Account for Macs with Retina Display and textfield text being too large
        int f2FontSize = isMac() && (displayDensity() > 1) ? 8 : 15;
        f2 = createFont("fonts/Raleway-Regular.otf", f2FontSize);
        f3 = createFont("fonts/Raleway-SemiBold.otf", 15);
        f4 = createFont("fonts/Raleway-SemiBold.otf", 64);  // clear bigger fonts for widgets
        //Account for Macs with Retina Display and textfield text being too large
        int f5FontSize = isMac() && (displayDensity() > 1) ? 6 : 12;
        f5 = createFont("fonts/Raleway-Regular.otf", f5FontSize);

        h1 = createFont("fonts/Montserrat-Regular.otf", 20);
        h2 = createFont("fonts/Montserrat-Regular.otf", 18);
        h3 = createFont("fonts/Montserrat-Regular.otf", 16);
        h4 = createFont("fonts/Montserrat-Regular.otf", 14);
        h5 = createFont("fonts/Montserrat-Regular.otf", 12);

        p0 = createFont("fonts/OpenSans-Semibold.ttf", 24);
        p1 = createFont("fonts/OpenSans-Regular.ttf", 20);
        p2 = createFont("fonts/OpenSans-Regular.ttf", 18);
        p3 = createFont("fonts/OpenSans-Regular.ttf", 16);
        p15 = createFont("fonts/OpenSans-Regular.ttf", 15);
        p4 = createFont("fonts/OpenSans-Regular.ttf", 14);
        p13 = createFont("fonts/OpenSans-Regular.ttf", 13);
        p5 = createFont("fonts/OpenSans-Regular.ttf", 12);
        p6 = createFont("fonts/OpenSans-Regular.ttf", 10);


        cog = loadImage("obci-logo-blu-cog.png");

        // check if the current directory is writable
        File dummy = new File(sketchPath());
        if (!dummy.canWrite()) {
            showStartupError = true;
            startupErrorMessage = "OpenBCI GUI was launched from a read-only location.\n\n" +
                    "Please move the application to a different location and re-launch.\n" +
                    "If you just downloaded the GUI, move it out of the disk image or Downloads folder.\n\n" +
                    "If this error persists, contact the OpenBCI team for support.";
            return; // early exit
        }

        directoryManager = new DirectoryManager();

        // redirect all output to a custom stream that will intercept all prints
        // write them to file and display them in the GUI's console window
        outputStream = new CustomOutputStream(System.out);
//        System.setOut(outputStream);
//        System.setErr(outputStream);

    }

    public synchronized void draw() {
        super.draw();

//        if (showStartupError) {
//            drawStartupError();
//            return;
//        }
//
//        if (setupComplete && systemMode != SYSTEMMODE_INTROANIMATION) {
//            systemUpdate();
//            systemDraw();
//
//            if (midInit) {
//                systemInitSession();
//            }
//
//            if (reinitRequested) {
//                haltSystem();
//                initSystem();
//                reinitRequested = false;
//            }
//
//            if (systemMode == SYSTEMMODE_POSTINIT) {
//                w_networking.compareAndSetNetworkingFrameLocks();
//            }
//
//        } else if (systemMode == SYSTEMMODE_INTROANIMATION) {
//            if (settings.introAnimationInit == 0) {
//                settings.introAnimationInit = millis();
//            } else {
//                introAnimation();
//            }
//        }


        introAnimation();



    }

    void drawStartupError() {
        final int w = 600;
        final int h = 350;
        final int headerHeight = 75;
        final int padding = 20;

        pushStyle();
        background(OPENBCI_DARKBLUE);
        stroke(204);
        fill(GREY_235);
        rect((width - w)/2, (height - h)/2, w, h);
        noStroke();
        fill(217, 4, 4);
        rect((width - w)/2, (height - h)/2, w, headerHeight);
        textFont(p0, 24);
        fill(255);
        textAlign(LEFT, CENTER);
        text("Error", (width - w)/2 + padding, (height - h)/2, w, headerHeight);
        textFont(p3, 16);
        fill(102);
        textAlign(LEFT, TOP);
        text(startupErrorMessage, (width - w)/2 + padding, (height - h)/2 + padding + headerHeight, w-padding*2, h-padding*2-headerHeight);
        popStyle();
    }

//    void systemUpdate() { // for updating data values and variables
//        //prepare for updating the GUI
//        win_w = width;
//        win_h = height;
//
//        textfieldUpdateHelper.resetTextFieldIsActive();
//
//        currentBoard.update();
//
//        dataLogger.update();
//
//        helpWidget.update();
//        topNav.update();
//        if (systemMode == SYSTEMMODE_PREINIT) {
//            //updates while in system control panel before START SYSTEM
//            controlPanel.update();
//
//            if (settings.widthOfLastScreen != width || settings.heightOfLastScreen != height) {
//                topNav.screenHasBeenResized(width, height);
//                settings.widthOfLastScreen = width;
//                settings.heightOfLastScreen = height;
//                //println("W = " + width + " || H = " + height);
//            }
//        }
//        if (systemMode == SYSTEMMODE_POSTINIT) {
//            processNewData();
//
//            //alternative component listener function (line 177 mouseReleased- 187 frame.addComponentListener) for processing 3,
//            //Component listener doesn't seem to work, so staying with this method for now
//            if (settings.widthOfLastScreen != width || settings.heightOfLastScreen != height) {
//                settings.screenHasBeenResized = true;
//                settings.timeOfLastScreenResize = millis();
//                settings.widthOfLastScreen = width;
//                settings.heightOfLastScreen = height;
//            }
//
//            //re-initialize GUI if screen has been resized and it's been more than 1/2 seccond (to prevent reinitialization of GUI from happening too often)
//            if (settings.screenHasBeenResized && settings.timeOfLastScreenResize + 500 > millis()) {
//                ourApplet = this; //reset PApplet...
//                topNav.screenHasBeenResized(width, height);
//                wm.screenResized();
//                settings.screenHasBeenResized = false;
//            }
//
//            if (wm.isWMInitialized) {
//                wm.update();
//            }
//        }
//    }
//
//
//    void systemDraw() { //for drawing to the screen
//        //redraw the screen...not every time, get paced by when data is being plotted
//        background(OPENBCI_DARKBLUE);  //clear the screen
//        noStroke();
//        //background(255);  //clear the screen
//
//        if (systemMode >= SYSTEMMODE_POSTINIT) {
//            wm.draw();
//            drawContainers();
//        }
//
//        if (systemMode >= SYSTEMMODE_PREINIT) {
//            topNav.draw();
//
//            //control panel
//            if (controlPanel.isOpen) {
//                controlPanel.draw();
//            }
//
//            //Draw output window at the bottom of the GUI
//            helpWidget.draw();
//        }
//
//        //Draw button help text close to the top
//        buttonHelpText.draw();
//
//        //Draw Session Start overlay on top of everything
//        if (midInit) {
//            drawOverlay("Starting Session...");
//        } else if (controlPanel.comPortBox.isAutoScanningForCytonSerial()) {
//            drawOverlay("Auto-Scanning for Cyton...");
//        }
//
//        //Display GUI version and FPS in the title bar of the app
//        surface.setTitle("OpenBCI GUI " + localGUIVersionString + " - " + localGUIVersionDate + " - " + int(frameRate) + " fps");
//    }
//
//    //Always Called after systemDraw()
//    void systemInitSession() {
//        if (midInitCheck2) {
//            println("OpenBCI_GUI: Start session. Calling initSystem().");
//            try {
//                initSystem(); //found in OpenBCI_GUI.pde
//            } catch (Exception e) {
//                e.printStackTrace();
//                haltSystem();
//            }
//            midInitCheck2 = false;
//            midInit = false;
//        } else {
//            midInitCheck2 = true;
//        }
//    }
//
//
//    //halt the data collection
//    void haltSystem() {
//        if (!systemHasHalted) { //prevents system from halting more than once
//            println("openBCI_GUI: haltSystem: Halting system for reconfiguration of settings...");
//
//            //Reset the text for the Start Session buttonscreen. Skip when reiniting board while already in playback mode session.
//            if (!reinitRequested) {
//                controlPanel.initBox.setInitSessionButtonText("START SESSION");
//            }
//
//            if (w_networking != null && w_networking.getNetworkActive()) {
//                w_networking.stopNetwork();
//                println("openBCI_GUI: haltSystem: Network streams stopped");
//            }
//
//            if (w_focus != null) {
//                w_focus.endSession();
//            }
//
//            stopRunning();  //stop data transfer
//
//            topNav.resetStartStopButton();
//            topNav.destroySmoothingButton(); //Destroy this button if exists and make null, will be re-init if needed next time session starts
//
//            //reset connect loadStrings
//            openBCI_portName = "N/A";  // Fixes inability to reconnect after halding  JAM 1/2017
//            ganglion_portName = "";
//            wifi_portName = "";
//
//            controlPanel.resetListItems();
//
//            if (eegDataSource == DATASOURCE_PLAYBACKFILE) {
//                controlPanel.recentPlaybackBox.getRecentPlaybackFiles();
//            }
//            systemMode = SYSTEMMODE_PREINIT;
//
//            recentPlaybackFilesHaveUpdated = false;
//
//            dataLogger.uninitialize();
//
//            currentBoard.uninitialize();
//            currentBoard = new BoardNull(); // back to null
//
//            sessionTimeElapsed.stop();
//
//            systemHasHalted = true;
//        }
//    } //end of halt system
//
//
//    //Init system based on default settings. Called from the "START SESSION" button in the GUI's ControlPanel.
//    void initSystem() {
//        println("");
//        println("");
//        println("=================================================");
//        println("||             INITIALIZING SYSTEM             ||");
//        println("=================================================");
//        println("");
//
//        verbosePrint("OpenBCI_GUI: initSystem: -- Init 0 -- ");
//
//        //reset init variables
//        systemHasHalted = false;
//        boolean abandonInit = false;
//
//        sessionTimeElapsed.reset();
//        sessionTimeElapsed.start();
//        sessionTimeElapsed.suspend();
//
//        //prepare the source of the input data
//        switch (eegDataSource) {
//            case DATASOURCE_CYTON:
//                if (selectedProtocol == BoardProtocol.SERIAL) {
//                    if(nchan == 16) {
//                        currentBoard = new BoardCytonSerialDaisy(openBCI_portName);
//                    }
//                    else {
//                        currentBoard = new BoardCytonSerial(openBCI_portName);
//                    }
//                }
//                else if (selectedProtocol == BoardProtocol.WIFI) {
//                    if(nchan == 16) {
//                        currentBoard = new BoardCytonWifiDaisy(wifi_ipAddress, selectedSamplingRate);
//                    }
//                    else {
//                        currentBoard = new BoardCytonWifi(wifi_ipAddress, selectedSamplingRate);
//                    }
//                }
//                break;
//            case DATASOURCE_SYNTHETIC:
//                currentBoard = new BoardBrainFlowSynthetic(nchan);
//                println("OpenBCI_GUI: Init session using Synthetic data source");
//                break;
//            case DATASOURCE_PLAYBACKFILE:
//                if (!playbackData_fname.equals("N/A")) {
//                    currentBoard = getDataSourcePlaybackClassFromFile(playbackData_fname);
//                    println("OpenBCI_GUI: Init session using Playback data source");
//                } else {
//                    if (!sdData_fname.equals("N/A")) {
//                        currentBoard = new DataSourceSDCard(sdData_fname);
//                        println("OpenBCI_GUI: Init session using Playback data source");
//                    }
//                    else {
//                        // no code path to it
//                        println("No playback or SD file selected.");
//                    }
//                }
//                break;
//            case DATASOURCE_GANGLION:
//                boolean showUpgradePopup = false;
//                if (guiSettings.getShowGanglionUpgradePopup())
//                {
//                    showUpgradePopup = true;
//                    guiSettings.setShowGanglionUpgradePopup(false);
//                }
//
//                if (selectedProtocol == BoardProtocol.WIFI) {
//                    currentBoard = new BoardGanglionWifi(wifi_ipAddress, selectedSamplingRate);
//                } else if (selectedProtocol == BoardProtocol.BLED112) {
//                    String ganglionName = (String)(controlPanel.bleBox.bleList.getItem(controlPanel.bleBox.bleList.activeItem).get("headline"));
//                    String ganglionPort = (String)(controlPanel.bleBox.bleList.getItem(controlPanel.bleBox.bleList.activeItem).get("subline"));
//                    String ganglionMac = controlPanel.bleBox.bleMACAddrMap.get(ganglionName);
//                    println("MAC address for Ganglion is " + ganglionMac);
//                    currentBoard = new BoardGanglionBLE(ganglionName, ganglionPort, ganglionMac, showUpgradePopup);
//                } else if (selectedProtocol == BoardProtocol.NATIVE_BLE) {
//                    String ganglionName = (String)(controlPanel.bleBox.bleList.getItem(controlPanel.bleBox.bleList.activeItem).get("headline"));
//                    String ganglionMac = controlPanel.bleBox.bleMACAddrMap.get(ganglionName);
//                    println("MAC address for Ganglion is " + ganglionMac);
//                    currentBoard = new BoardGanglionNative(ganglionName, showUpgradePopup);
//                }
//                break;
//            case DATASOURCE_STREAMING:
//                currentBoard = new BoardBrainFlowStreaming(
//                        controlPanel.streamingBoardBox.getBoard().getBoardId(),
//                        controlPanel.streamingBoardBox.getIP(),
//                        controlPanel.streamingBoardBox.getPort()
//                );
//                println("OpenBCI_GUI: Init session using Streaming data source");
//            default:
//                break;
//        }
//
//        // initialize the chosen board
//        boolean success = currentBoard.initialize();
//        abandonInit = !success; // abandon if init fails
//
//        //Handle edge cases for Cyton and Cyton+Daisy users immediately after board is initialized. Fixes #954
//        if (eegDataSource == DATASOURCE_CYTON) {
//            println("OpenBCI_GUI: Configuring Cyton Channel Count...");
//            if (currentBoard instanceof BoardCytonSerial) {
//                Pair<Boolean, String> res = ((BoardBrainFlow)currentBoard).sendCommand("c");
//                //println(res.getKey().booleanValue(), res.getValue());guiSettings
//                if (res.getValue().startsWith("daisy removed")) {
//                    println("OpenBCI_GUI: Daisy is physically attached, using Cyton 8 Channels instead.");
//                }
//            } else if (currentBoard instanceof BoardCytonSerialDaisy) {
//                Pair<Boolean, String> res = ((BoardBrainFlow)currentBoard).sendCommand("C");
//                //println(res.getKey().booleanValue(), res.getValue());
//                if (res.getValue().startsWith("no daisy to attach")) {
//                    haltSystem();
//                    outputError("User selected Cyton+Daisy, but no Daisy is attached. Please change Channel Count to 8 Channels.");
//                    controlPanel.open();
//                    return;
//                }
//            }
//
//            //Show a popup to inform first-time Cyton users about the FTDI buffer fix and Cyton Smoothing feature. Fixes #1026
//            //Windows Users: Latest BrainFlow will automatically fix this in the background on Session Start! Fixed in #1039
//            if (guiSettings.getShowCytonSmoothingPopup()) {
//                println("OpenBCI_GUI: Showing Cyton FTDI Buffer Fix Popup");
//                String popupTitle = "Cyton FTDI Buffer Fix Info";
//                String popupString = "The default settings for the Cyton Dongle driver can make data appear \"choppy.\" Visit the OpenBCI Docs to learn how to fix this. For now, the GUI will \"smooth\" the data for you.";
//                String popupButtonText = "View Fix";
//                String popupButtonURL;
//                if (isMac()) {
//                    popupButtonURL = "https://docs.openbci.com/Troubleshooting/FTDI_Fix_Mac/";
//                    PopupMessage msg = new PopupMessage(popupTitle, popupString, popupButtonText, popupButtonURL);
//                } else if (isLinux()){
//                    popupButtonURL = "https://docs.openbci.com/Troubleshooting/FTDI_Fix_Linux/";
//                    PopupMessage msg = new PopupMessage(popupTitle, popupString, popupButtonText, popupButtonURL);
//                }
//                guiSettings.setShowCytonSmoothingPopup(false);
//            }
//        }
//
//        updateToNChan(currentBoard.getNumEXGChannels());
//
//        dataLogger.initialize();
//
//        verbosePrint("OpenBCI_GUI: initSystem: Initializing core data objects");
//        initCoreDataObjects();
//
//        verbosePrint("OpenBCI_GUI: initSystem: -- Init 1 -- " + millis());
//        verbosePrint("OpenBCI_GUI: initSystem: Initializing FFT data objects");
//        initFFTObjectsAndBuffer();
//
//        verbosePrint("OpenBCI_GUI: initSystem: -- Init 2 -- " + millis());
//        verbosePrint("OpenBCI_GUI: initSystem: Closing ControlPanel...");
//
//        controlPanel.close();
//        topNav.controlPanelCollapser.setOff();
//
//        verbosePrint("OpenBCI_GUI: initSystem: -- Init 3 -- " + millis());
//
//        if (abandonInit) {
//            haltSystem();
//            outputError("Failed to initialize board. Please check that the board is on and has power. See Console Log for more details.");
//            controlPanel.open();
//            return;
//        } else {
//            //initilize the secondary topnav and all applicable widgets
//            topNav.initSecondaryNav();
//            wm = new WidgetManager(this);
//            nextPlayback_millis = millis(); //used for synthesizeData and readFromFile.  This restarts the clock that keeps the playback at the right pace.
//            systemMode = SYSTEMMODE_POSTINIT; //tell system it's ok to leave control panel and start interfacing GUI
//        }
//
//        verbosePrint("OpenBCI_GUI: initSystem: -- Init 4 -- " + millis());
//
//        //don't save default session settings StreamingBoard
//        if (eegDataSource != DATASOURCE_STREAMING) {
//            //Init software settings: create default settings file that is datasource unique
//            settings.init();
//            settings.initCheckPointFive();
//        }
//
//        //Make sure topNav buttons draw in the correct spot
//        topNav.screenHasBeenResized(width, height);
//
//        //Instantiate Global Filter Settings Class
//        filterSettings = new FilterSettings(((DataSource)currentBoard));
//
//        verbosePrint("OpenBCI_GUI: initSystem: -- Init 5 -- " + millis());
//
//        midInit = false;
//    } //end initSystem


    void introAnimation() {
        pushStyle();
        imageMode(CENTER);
        background(255);

        image(cog, width / 2, height / 2, width / 6, width / 6);

        popStyle();
    }
}