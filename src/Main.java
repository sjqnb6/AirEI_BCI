
//import CustomCp5Classes_.CopyPaste;
import BoardBrainFlowStreaming_.BoardBrainFlowStreaming;
import BoardBrainFlowSynthetic_.BoardBrainFlowSynthetic;
import BoardBrainflow_.BoardBrainFlow;
import BoardCyton_.BoardCytonSerial;
import BoardCyton_.BoardCytonSerialDaisy;
import BoardDomesticSerial_.BoardDomesticSerial16;
import BoardDomesticWifi_.BoardDomesticWifi16;
import BoardCyton_.BoardCytonWifi;
import BoardCyton_.BoardCytonWifiDaisy;
import BoardGanglion_.BoardGanglionBLE;
import BoardGanglion_.BoardGanglionNative;
import BoardGanglion_.BoardGanglionWifi;
import ConsoleLog_.CustomOutputStream;
import ControlPanel_.ControlPanel;
import CustomCp5Classes_.ButtonHelpText;
import CustomCp5Classes_.CopyPaste;
import CustomCp5Classes_.TextFieldUpdateHelper;
import DataLogger_.DataLogger;
import DataProcessing_.DataProcessing;
import DataSourceSDCard_.DataSourceSDCard;
import DataSource_.DataSource;
import Debugging_.HelpWidget;
import DirectoryManager_.DirectoryManager;
import Extras_.DataStatus;
import Extras_.PlotFontInfo;
import FilterSettings_.FilterSettings;
import Globel.GUI;
import GuiSettings_.GuiSettings;
import InterfaceSerial_.InterfaceSerial;
import PopupMessage_.PopupMessage;
import SessionSettings_.SessionSettings;
import TopNav_.TopNav;
import WidgetManager_.WidgetManager;
import brainflow.BoardShim;
import brainflow.BrainFlowError;
import ddf.minim.Minim;
import ddf.minim.ugens.FilePlayer;
import gifAnimation.Gif;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import processing.core.PApplet;
import brainflow.BoardIds;
import java.io.File;

import static Containers_.GF.drawContainers;
import static Containers_.GF.setupContainers;
import static DataProcessing_.GF.initializeFFTObjects;
import static DataProcessing_.GF.processNewData;
import static DataSourcePlayback_.GF.getDataSourcePlaybackClassFromFile;
import static Debugging_.GF.outputError;
import static Debugging_.GF.verbosePrint;
import static Globel.GF.*;
import static SystemManager.GF.*;
import static WidgetManager_.GVI.w_networking;

public class Main extends GUI {

    static public void main(String[] passedArgs) {
//        String[] appletArgs = new String[] { "com.jiakii.processtest.Test" };
        PApplet.main("Main");
    }

    public void settings() {
        super.settings();

        //LINUX GFX FIX #816
        System.setProperty("jogl.disable.openglcore", "false");

        win_w = 1224;
        win_h = 768;

        // If less than 1366x768, set smaller minimum GUI size
        // Nov 2020 - Accomodate as low as 1024 X 640
        if (displayWidth <= 1366 || displayHeight <= 768) {
            win_w = 980;
            win_h = 580;
        }
        size(win_w, win_h);


        globalScreenResolution = new StringBuilder("Screen Resolution: ");
        globalScreenResolution.append(displayWidth);
        globalScreenResolution.append(" X ");
        globalScreenResolution.append(displayHeight);
        //Account for high-dpi displays on Mac, Windows, and Linux Machines Fixes #968
        pixelDensity(displayDensity());
        globalScreenDPI = new StringBuilder("High-DPI Screen Detected: ");
        globalScreenDPI.append(displayDensity() == 2);
        dataLogger = new DataLogger(this);
        iSerial = new InterfaceSerial(this);

    }


    public void setup() {
        super.setup();
//        try {
//            System.out.println(BoardShim.get_package_num_channel(BoardIds.CYTON_BOARD));
//        } catch (BrainFlowError e) {
//            throw new RuntimeException(e);
//        }
//        try {
//            int[] channels = BoardShim.get_eeg_channels(0);
//            for(int i = 0; i < channels.length; i++){
//                System.out.println(channels[i]);
//            }
//        } catch (BrainFlowError e) {
//            throw new RuntimeException(e);
//        };
        System.out.println("---------------------------------------------------");
        println("=== AirEIBCI Setup Started ===");
        frameRate(30);  // 设置draw函数执行频率
        surface.setResizable(true);         // ✅ 允许最大化        frameRate(90);  // 设置draw函数执行频率
        surface.setLocation(100, 50);  // 将窗口左上角设置在屏幕坐标 (100, 50)

        copyPaste = new CopyPaste(this);

        //V1 FONTS
        println("Loading fonts...");
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
        p7 = createFont("fonts/SourceHanSansSC-Regular-2.otf", 18);

//        cog = loadImage("obci-logo-blu-cog.png");
        cog = loadImage("AirEI_BCI_logo.png");

        // check if the current directory is writable
        File dummy = new File(sketchPath());
        if (!dummy.canWrite()) {
            showStartupError = true;
            startupErrorMessage = "AirEIBCI was launched from a read-only location.\n\n" +
                    "Please move the application to a different location and re-launch.\n" +
                    "If you just downloaded the application, move it out of the disk image or Downloads folder.\n\n" +
                    "If this error persists, contact the AirEIBCI support team.";
            return; // early exit
        }

        directoryManager = new DirectoryManager();

        // redirect all output to a custom stream that will intercept all prints
        // write them to file and display them in the GUI's console window
        outputStream = new CustomOutputStream(System.out);
        System.setOut(outputStream);
        System.setErr(outputStream);

        StringBuilder osName = new StringBuilder("Operating System and Version: ");

        if (isLinux()) {
            osName.append("Linux");
            osName.append(" - ");
            osName.append(getOperatingSystemVersion());
        } else if (isWindows()) {
            osName.append(getOperatingSystemName());
            //Throw a popup if we detect an incompatible version of Windows. Fixes #964. Found in Extras.pde.
            checkIsOldVersionOfWindowsOS();
            //This is an edge case when using 32-bit Processing Java on Windows. Throw a popup if detected.
            checkIs64BitJava();
        } else if (isMac()) {
            osName.append("Mac");
            osName.append(" - ");
            osName.append(getOperatingSystemVersion());
        }


        println("Console Log Started at Local Time: " + directoryManager.getFileNameDateTime());
        println(globalScreenResolution.toString());
        println(globalScreenDPI.toString());
        println(osName.toString());
        if (isMac()) {
            checkIsMacFullDetail();
        }
        println("JVM Version: " + System.getProperty("java.version"));
        println("Welcome to AirEIBCI!"); //Welcome line.
        println("For more information, please visit: https://docs.openbci.com/Software/OpenBCISoftware/GUIDocs/");


        // Copy sample data to the Users' Documents folder +  create Recordings folder
        directoryManager.init();
        settings = new SessionSettings(this);
        guiSettings = new GuiSettings(directoryManager.getSettingsPath());
        userPlaybackHistoryFile = directoryManager.getSettingsPath()+"AirEIBCIPlaybackHistory.json";

        //open window
//        ourApplet = this;

        // Bug #426: If setup takes too long, JOGL will time out waiting for the GUI to draw something.
        // moving the setup to a separate thread solves this. We just have to make sure not to
        // start drawing until delayed setup is done.
        thread("delayedSetup");
    }
    int n = 0;
    public synchronized void draw() {
        if (showStartupError) {
            drawStartupError();
        }
        else if (setupComplete && (systemMode == SYSTEMMODE_PREINIT || systemMode == SYSTEMMODE_POSTINIT)) {
            systemUpdate(); //signPost("20");
            systemDraw();   //signPost("30");
            if (midInit) {
                //如果单击“开始会话”，请等待 2 个绘制周期以显示叠加，然后初始化会话。
                //当 Init 会话启动时，屏幕似乎挂起。
                systemInitSession();
            }
            if(reinitRequested) {
                haltSystem(this);
                initSystem();
                reinitRequested = false;
            }
            if (systemMode == SYSTEMMODE_POSTINIT) {
                //w_networking.compareAndSetNetworkingFrameLocks();
            }
        }
        else if (systemMode == SYSTEMMODE_INTROANIMATION) {
            if (settings != null) {
                if (settings.introAnimationInit == 0) {
                    settings.introAnimationInit = millis();
                } else {
                    introAnimation();
                }
            } else {
                // Settings not yet initialized, draw a simple loading screen
                background(226);
                fill(255);
                textAlign(CENTER, CENTER);
                textSize(20);
                text("Initializing AirEIBCI...", width/2, height/2);
            }
        }


//        introAnimation();



    }

    public void delayedSetup() {
        smooth(); //turn this off if it's too slow

        surface.setResizable(true);  //updated from frame.setResizable in Processing 2
        settings.widthOfLastScreen = width; //for screen resizing (Thank's Tao)
        settings.heightOfLastScreen = height;

        setupContainers(this);
        fontInfo = new PlotFontInfo();
        helpWidget = new HelpWidget(this,0, win_h - 30, win_w, 30);
        //Instantiate buttonHelpText before any buttons have been made
        buttonHelpText = new ButtonHelpText(this);
        textfieldUpdateHelper = new TextFieldUpdateHelper();

        //setup topNav
        topNav = new TopNav(this);

        //Print BrainFlow version
        StringBuilder brainflowVersion = new StringBuilder("BrainFlow Version: ");
        try {
            brainflowVersion.append(BoardShim.get_version());
        } catch (BrainFlowError e) {
            e.printStackTrace();
        }
        println(brainflowVersion);

        println("Loading UI images...");
        try {
            logo_black = loadImage("AirEI_BCI_black(1).png");
            if (logo_black == null) println("WARNING: Failed to load obci-logo-blk.png");
            logo_blue = loadImage("obci-logo-blu.png");
            if (logo_blue == null) println("WARNING: Failed to load obci-logo-blu.png");
            logo_white = loadImage("obci-logo-wht.png");
            if (logo_white == null) println("WARNING: Failed to load obci-logo-wht.png");
            consoleImgBlue = loadImage("console-45x45-dots_blue.png");
            if (consoleImgBlue == null) println("WARNING: Failed to load console-45x45-dots_blue.png");
            consoleImgWhite = loadImage("console-45x45-dots_white.png");
            if (consoleImgWhite == null) println("WARNING: Failed to load console-45x45-dots_white.png");
            loadingGIF = new Gif(this, "ajax_loader_gray_512.gif");
            if (loadingGIF != null) loadingGIF.loop();
            loadingGIF_blue = new Gif(this, "obci_cog_anim-normalblue.gif");
            if (loadingGIF_blue != null) loadingGIF_blue.loop();
            println("UI images loaded successfully");
        } catch (Exception e) {
            println("ERROR loading UI images: " + e.getMessage());
            e.printStackTrace();
        }

        prepareExitHandler();

        sessionTimeElapsed = new StopWatch();
        streamTimeElapsed = new StopWatch();

        asyncLoadAudioFiles();

        synchronized(this) {
            // Instantiate ControlPanel in the synchronized block.
            // It's important to avoid instantiating a ControlP5 during a draw() call
            // Otherwise we get a crash on launch 10% of the time
            println("Creating ControlPanel...");
            controlPanel = new ControlPanel(this);
            println("ControlPanel created successfully");

            setupComplete = true; // signal that the setup thread has finished
            println("AirEIBCI::Setup: Setup is complete!");
            println("systemMode = " + systemMode);
            println("setupComplete = " + setupComplete);
        }

        //Apply GUI-wide settings to front end at the end of setup
        println("Applying GUI settings...");
        guiSettings.applySettings();

        // Transition from intro animation to pre-init system mode
        systemMode = SYSTEMMODE_PREINIT;
        println("systemMode changed to SYSTEMMODE_PREINIT");

        if (!isAdminUser() || isElevationNeeded()) {
            outputError("AirEIBCI: 该应用未以管理员权限运行。这可能会限制连接设备或读写文件的能力。");
        }
    }


    void drawStartupError() {
        final int w = 600;
        final int h = 350;
        final int headerHeight = 75;
        final int padding = 20;

        pushStyle();
        background(226);
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

    void systemUpdate() { // for updating data values and variables
        //prepare for updating the GUI
        win_w = width;
        win_h = height;

        textfieldUpdateHelper.resetTextFieldIsActive();

        currentBoard.update();

        dataLogger.update();

        helpWidget.update();
        topNav.update();
        if (systemMode == SYSTEMMODE_PREINIT) {
            //updates while in system control panel before START SYSTEM
            controlPanel.update();

            if (settings.widthOfLastScreen != width || settings.heightOfLastScreen != height) {
                topNav.screenHasBeenResized(width, height);
                settings.widthOfLastScreen = width;
                settings.heightOfLastScreen = height;
                //println("W = " + width + " || H = " + height);
            }
        }
        if (systemMode == SYSTEMMODE_POSTINIT) {
            processNewData(this);

            //alternative component listener function (line 177 mouseReleased- 187 frame.addComponentListener) for processing 3,
            //Component listener doesn't seem to work, so staying with this method for now
            if (settings.widthOfLastScreen != width || settings.heightOfLastScreen != height) {
                settings.screenHasBeenResized = true;
                settings.timeOfLastScreenResize = millis();
                settings.widthOfLastScreen = width;
                settings.heightOfLastScreen = height;
            }

            //re-initialize GUI if screen has been resized and it's been more than 1/2 seccond (to prevent reinitialization of GUI from happening too often)
            if (settings.screenHasBeenResized && settings.timeOfLastScreenResize + 500 > millis()) {
//                ourApplet = this; //reset PApplet...
                topNav.screenHasBeenResized(width, height);
                wm.screenResized();
                settings.screenHasBeenResized = false;
            }

            if (wm.isWMInitialized) {
                wm.update();
            }
        }
    }
    //
//
    void systemDraw() { //for drawing to the screen
        //redraw the screen...not every time, get paced by when data is being plotted
        if (systemMode == SYSTEMMODE_PREINIT) {
            // Pre-connection page uses the same industrial light-gray base as the updated top controls.
            background(226);
        } else {
            background(OPENBCI_DARKBLUE);  //clear the screen
        }
        noStroke();
        //background(255);  //clear the screen

        if (systemMode >= SYSTEMMODE_POSTINIT) {
            wm.draw();
            drawContainers(this);
        }

        if (systemMode >= SYSTEMMODE_PREINIT) {
            topNav.draw();

            //control panel
            if (controlPanel.isOpen) {
                controlPanel.draw();
            }

            //Draw output window at the bottom of the GUI
            helpWidget.draw();
        }

        //Draw button help text close to the top
        buttonHelpText.draw();

        //Draw Session Start overlay on top of everything
        if (midInit) {
            drawOverlay("正在连接...");
        } else if (controlPanel.comPortBox.isAutoScanningForCytonSerial()) {
            drawOverlay("自动扫描 Cyton...");
        }

        //Display GUI version and FPS in the title bar of the app
        surface.setTitle(
                "AirEIBCI GUI "
                        + localGUIVersionString
                        + " - "
                        + localGUIVersionDate
                        + " - "
                        + (int) frameRate
                        + " fps"
        );
    }
    //
//    //Always Called after systemDraw()
    void systemInitSession() {
        if (midInitCheck2) {
            println("AirEIBCI: Start session. Calling initSystem().");
            try {
                initSystem(); //found in OpenBCI_GUI.pde
            } catch (Exception e) {
                e.printStackTrace();
                haltSystem(this);
            }
            midInitCheck2 = false;
            midInit = false;
        } else {
            midInitCheck2 = true;
        }
    }
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
    void initSystem() {
        println("");
        println("");
        println("=================================================");
        println("||             INITIALIZING SYSTEM             ||");
        println("=================================================");
        println("");

        verbosePrint("AirEIBCI: initSystem: -- Init 0 -- ");

        //reset init variables
        systemHasHalted = false;
        boolean abandonInit = false;

        sessionTimeElapsed.reset();
        sessionTimeElapsed.start();
        sessionTimeElapsed.suspend();

        //prepare the source of the input data
        switch (eegDataSource) {
            case DATASOURCE_CYTON:
                if (selectedProtocol == BoardProtocol.SERIAL) {
                    if(nchan == 16) {
                        currentBoard = new BoardDomesticSerial16(this, openBCI_portName);
                    }
                    else {
                        currentBoard = new BoardCytonSerial(this, openBCI_portName);
                    }
                }
                else if (selectedProtocol == BoardProtocol.WIFI) {
                    if(nchan == 16) {
                        currentBoard = new BoardDomesticWifi16(this, wifi_ipAddress, selectedSamplingRate);
                    }
                    else {
                        currentBoard = new BoardCytonWifi(this, wifi_ipAddress, selectedSamplingRate);
                    }
                }
                break;
            case DATASOURCE_SYNTHETIC:
                currentBoard = new BoardBrainFlowSynthetic(this, nchan);
                println("AirEIBCI: Init session using Synthetic data source");
                break;
            case DATASOURCE_PLAYBACKFILE:
                if (!playbackData_fname.equals("N/A")) {
                    currentBoard = getDataSourcePlaybackClassFromFile(this, playbackData_fname);
                    println("AirEIBCI: Init session using Playback data source");
                } else {
                    if (!sdData_fname.equals("N/A")) {
                        currentBoard = new DataSourceSDCard(this, sdData_fname);
                        println("AirEIBCI: Init session using Playback data source");
                    }
                    else {
                        // no code path to it
                        println("No playback or SD file selected.");
                    }
                }
                break;
            case DATASOURCE_GANGLION:
                boolean showUpgradePopup = false;
                if (guiSettings.getShowGanglionUpgradePopup())
                {
                    showUpgradePopup = true;
                    guiSettings.setShowGanglionUpgradePopup(false);
                }

                if (selectedProtocol == BoardProtocol.WIFI) {
                    currentBoard = new BoardGanglionWifi(this, wifi_ipAddress, selectedSamplingRate);
                } else if (selectedProtocol == BoardProtocol.BLED112) {
                    String ganglionName = (String)(controlPanel.bleBox.bleList.getItem(controlPanel.bleBox.bleList.activeItem).get("headline"));
                    String ganglionPort = (String)(controlPanel.bleBox.bleList.getItem(controlPanel.bleBox.bleList.activeItem).get("subline"));
                    String ganglionMac = controlPanel.bleBox.bleMACAddrMap.get(ganglionName);
                    println("MAC address for Ganglion is " + ganglionMac);
                    currentBoard = new BoardGanglionBLE(this, ganglionName, ganglionPort, ganglionMac, showUpgradePopup);
                } else if (selectedProtocol == BoardProtocol.NATIVE_BLE) {
                    String ganglionName = (String)(controlPanel.bleBox.bleList.getItem(controlPanel.bleBox.bleList.activeItem).get("headline"));
                    String ganglionMac = controlPanel.bleBox.bleMACAddrMap.get(ganglionName);
                    println("MAC address for Ganglion is " + ganglionMac);
                    currentBoard = new BoardGanglionNative(this, ganglionName, showUpgradePopup);
                }
                break;
            case DATASOURCE_STREAMING:
                currentBoard = new BoardBrainFlowStreaming(this,
                        controlPanel.streamingBoardBox.getBoard().getBoardId(),
                        controlPanel.streamingBoardBox.getIP(),
                        controlPanel.streamingBoardBox.getPort()
                );
                println("AirEIBCI: Init session using Streaming data source");
            default:
                break;
        }

        // initialize the chosen board
        boolean success = currentBoard.initialize();
        abandonInit = !success; // abandon if init fails

        //Handle edge cases for Cyton and Cyton+Daisy users immediately after board is initialized. Fixes #954
        if (eegDataSource == DATASOURCE_CYTON) {
            //-------------------------------------------------------------------------------------------
            println("AirEIBCI: 配置 Cyton 通道数量");
            if (currentBoard instanceof BoardCytonSerial) {
//                Pair<Boolean, String> res = ((BoardBrainFlow)currentBoard).sendCommand("c");
                //println(res.getKey().booleanValue(), res.getValue());guiSettings
//                if (res.getValue().startsWith("daisy removed")) {
//                    println("OpenBCI_GUI: Daisy is physically attached, using Cyton 8 Channels instead.");
//                }
            } else if (currentBoard instanceof BoardCytonSerialDaisy) {
                Pair<Boolean, String> res = ((BoardBrainFlow)currentBoard).sendCommand("C");
                //println(res.getKey().booleanValue(), res.getValue());
                if (res.getValue().startsWith("no daisy to attach")) {
                    haltSystem(this);
                    outputError("User selected Cyton+Daisy, but no Daisy is attached. Please change Channel Count to 8 Channels.");
                    controlPanel.open();
                    return;
                }
            }

            //Show a popup to inform first-time Cyton users about the FTDI buffer fix and Cyton Smoothing feature. Fixes #1026
            //Windows Users: Latest BrainFlow will automatically fix this in the background on Session Start! Fixed in #1039
            if (guiSettings.getShowCytonSmoothingPopup()) {
                println("AirEIBCI: Showing Cyton FTDI Buffer Fix Popup");
                String popupTitle = "Cyton FTDI Buffer Fix Info";
                String popupString = "The default settings for the Cyton Dongle driver can make data appear \"choppy.\" Visit the OpenBCI Docs to learn how to fix this. For now, the GUI will \"smooth\" the data for you.";
                String popupButtonText = "View Fix";
                String popupButtonURL;
                if (isMac()) {
                    popupButtonURL = "https://docs.openbci.com/Troubleshooting/FTDI_Fix_Mac/";
                    PopupMessage msg = new PopupMessage(popupTitle, popupString, popupButtonText, popupButtonURL);
                } else if (isLinux()){
                    popupButtonURL = "https://docs.openbci.com/Troubleshooting/FTDI_Fix_Linux/";
                    PopupMessage msg = new PopupMessage(popupTitle, popupString, popupButtonText, popupButtonURL);
                }
                guiSettings.setShowCytonSmoothingPopup(false);
            }
        }

        updateToNChan(this, currentBoard.getNumEXGChannels());

        dataLogger.initialize();

        verbosePrint("AirEIBCI: initSystem: Initializing core data objects");
        initCoreDataObjects();

        verbosePrint("AirEIBCI: initSystem: -- Init 1 -- " + millis());
        verbosePrint("AirEIBCI: initSystem: Initializing FFT data objects");
        initFFTObjectsAndBuffer();

        verbosePrint("AirEIBCI: initSystem: -- Init 2 -- " + millis());
        verbosePrint("AirEIBCI: initSystem: Closing ControlPanel...");

        controlPanel.close();
        topNav.controlPanelCollapser.setOff();

        verbosePrint("AirEIBCI: initSystem: -- Init 3 -- " + millis());

        if (abandonInit) {
            haltSystem(this);
            outputError("未能初始化主板。请检查电路板是否开启且有电。详情请参见控制台日志。");
            controlPanel.open();
            return;
        } else {
            //initilize the secondary topnav and all applicable widgets
            topNav.initSecondaryNav();
            wm = new WidgetManager(this);
            nextPlayback_millis = millis(); //used for synthesizeData and readFromFile.  This restarts the clock that keeps the playback at the right pace.
            systemMode = SYSTEMMODE_POSTINIT; //tell system it's ok to leave control panel and start interfacing GUI
        }

        verbosePrint("AirEIBCI: initSystem: -- Init 4 -- " + millis());

        //don't save default session settings StreamingBoard
        if (eegDataSource != DATASOURCE_STREAMING) {
            //Init software settings: create default settings file that is datasource unique
            settings.init();
            settings.initCheckPointFive();
        }

        //Make sure topNav buttons draw in the correct spot
        topNav.screenHasBeenResized(width, height);

        //Instantiate Global Filter Settings Class
        filterSettings = new FilterSettings(this, ((DataSource)currentBoard));

        verbosePrint("AirEIBCI: initSystem: -- Init 5 -- " + millis());

        midInit = false;
    } //end initSystem


    void introAnimation() {
        pushStyle();
        imageMode(CENTER);
        background(255);

        image(cog, width / 2, height / 2, width / 6, width / 6);

        popStyle();
    }


    //====================== END-OF-DRAW ==========================//

    private void prepareExitHandler () {
        // This callback will run when the GUI quits
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run () {
                System.out.println("SHUTDOWN HOOK");

                haltSystem(Main.this);
            }
        }
        ));
    }

    //Pre-load audio files into memory in delayedSetup for best app performance and no waiting
    void asyncLoadAudioFiles() {
        final int _numSoundFiles = 5;
        minim = new Minim(this);
        auditoryNfbFilePlayers = new FilePlayer[_numSoundFiles];
        auditoryNfbGains = new ddf.minim.ugens.Gain[_numSoundFiles];
        audioOutput = minim.getLineOut();
        println("AirEIBCI: AuditoryFeedback: Loading Audio...");
        for (int i = 0; i < _numSoundFiles; i++) {
            //Use large buffer size and cache files in memory
            try {
                auditoryNfbFilePlayers[i] = new FilePlayer( minim.loadFileStream("bp" + (i+1) + ".mp3", 2048, true) );
                auditoryNfbGains[i] = new ddf.minim.ugens.Gain(-15.0f);
                auditoryNfbFilePlayers[i].patch(auditoryNfbGains[i]).patch(audioOutput);
            } catch (Exception e) {
                outputError("AuditoryFeedback: 无法加载音频文件。要启用此功能，请连接或开启音频设备并重启图形界面。");
                audioOutputIsAvailable = false;
                return;
            }
        }
        println("AirEIBCI: AuditoryFeedback: Done Loading Audio!");
        audioOutputIsAvailable = true;
    }

    void drawOverlay(String text) {
        //Draw a gray overlay when the Start Session button is pressed
        pushStyle();
        //imageMode(CENTER);
        fill(124, 142);
        rect(0, 0, width, height);
        popStyle();

        pushStyle();
        textFont(p7, 24);
        fill(boxColor, 255);
        stroke(OPENBCI_DARKBLUE, 200);
        rect(width/2 - (textWidth(text)+20)/2, height/2 - 80/2, textWidth(text) + 20, 80);
        fill(OPENBCI_DARKBLUE, 255);
        text(text, width/2 - textWidth(text)/2, height/2 + 8);
        popStyle();
    }

    void initCoreDataObjects() {
//        nPointsPerUpdate = int(round(float(UPDATE_MILLIS) * currentBoard.getSampleRate()/ 1000.f));
        nPointsPerUpdate = Math.round(((float) UPDATE_MILLIS) * currentBoard.getSampleRate() / 1000f);
        dataProcessingRawBuffer = new float[nchan][getCurrentBoardBufferSize(this)];
        dataProcessingFilteredBuffer = new float[nchan][getCurrentBoardBufferSize(this)];

        data_elec_imp_ohm = new float[nchan];
        is_railed = new DataStatus[nchan];
        for (int i=0; i<nchan; i++) {
            is_railed[i] = new DataStatus(this);
        }

        dataProcessing = new DataProcessing(this, nchan, currentBoard.getSampleRate());
    }

    void initFFTObjectsAndBuffer() {
        //initialize the FFT objects
        for (int Ichan=0; Ichan < nchan; Ichan++) {
            // verbosePrint("Init FFT Buff – " + Ichan);
            fftBuff[Ichan] = new ddf.minim.analysis.FFT(getNfftSafe(this), currentBoard.getSampleRate());
        }  //make the FFT objects

        //Attempt initialization. If error, print to console and exit function.
        //Fixes GUI crash when trying to load outdated recordings
        try {
            initializeFFTObjects(fftBuff, dataProcessingRawBuffer, getNfftSafe(this), currentBoard.getSampleRate());
        } catch (ArrayIndexOutOfBoundsException e) {
            //e.printStackTrace();
            outputError("播放文件加载错误。试试用更近期的录音。");
            return;
        }
    }
}
