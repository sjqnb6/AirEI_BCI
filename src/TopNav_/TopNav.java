package TopNav_;

import BoardCyton_.BoardCyton;
import ConsoleLog_.ConsoleWindow;
import FilterUI_.FilterUIPopup;
import GUI.GUIManager;
import ImpedanceSettingsBoard_.ImpedanceSettingsBoard;
import PopupMessage_.PopupMessage;
import SmoothingBoard_.SmoothingCapableBoard;
import controlP5.Button;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlP5;
import http.requests.GetRequest;
import processing.core.PFont;
import processing.core.PImage;
import processing.data.JSONObject;

import static Debugging_.GF.*;
import static Extras_.GF.pingWebsite;
//import static GUI.GGVI.*;
import static Globel.GUI.*;
import static Interactivity_.GF.openURLInBrowser;
import static SystemManager.GF.startRunning;
import static SystemManager.GF.stopRunning;
import static WidgetManager_.GVI.w_cytonImpedance;
import Globel.GUI;
public class TopNav {

    private GUI MAIN;
    private final int TOPNAV_DARKBLUE;
    private final int SUBNAV_LIGHTBLUE;
    private int strokeColor;

    private ControlP5 topNav_cp5;

    public Button controlPanelCollapser;

    public Button toggleDataStreamingButton;

    public Button filtersButton;
    public Button smoothingButton;

    public Button debugButton;
    public Button tutorialsButton;
    public Button shopButton;
    public Button issuesButton;
    public Button updateGuiVersionButton;

    public Button layoutButton;
    public Button settingsButton;

    public LayoutSelector layoutSelector;
    public TutorialSelector tutorialSelector;
    public ConfigSelector configSelector;
    private int previousSystemMode = 0;

    private boolean secondaryNavInit = false;

    private final int PAD_3 = 3;
    private final int DEBUG_BUT_W = 33;
    private final int TOPRIGHT_BUT_W = 80;
    private final int DATASTREAM_BUT_W = 170;
    private final int SUBNAV_BUT_Y = 35;
    private final int SUBNAV_BUT_W = 70;
    private final int SUBNAV_BUT_H = 26;
    private final int TOPNAV_BUT_H = SUBNAV_BUT_H;

    private boolean topNavDropdownMenuIsOpen = false;

    public TopNav(GUI MAIN) {
        this.MAIN = MAIN;
        this.TOPNAV_DARKBLUE = MAIN.OPENBCI_BLUE;
        this.SUBNAV_LIGHTBLUE = MAIN.buttonsLightBlue;
        this.strokeColor = MAIN.OPENBCI_DARKBLUE;


        int controlPanel_W = 256;

        //Instantiate local cp5 for this box
        topNav_cp5 = new ControlP5(MAIN);
        topNav_cp5.setGraphics(MAIN, 0, 0);
        topNav_cp5.setAutoDraw(false);

        //TOP LEFT OF GUI
        createControlPanelCollapser("System Control Panel", PAD_3, PAD_3, controlPanel_W, TOPNAV_BUT_H, h3, 16, TOPNAV_DARKBLUE, MAIN.WHITE);

        //TOP RIGHT OF GUI, FROM LEFT<---Right
        createDebugButton(" ", MAIN.width - DEBUG_BUT_W - PAD_3, PAD_3, DEBUG_BUT_W, TOPNAV_BUT_H, h3, 16, TOPNAV_DARKBLUE, MAIN.WHITE);
        createTutorialsButton("Help", (int)debugButton.getPosition()[0] - TOPRIGHT_BUT_W - PAD_3, PAD_3, TOPRIGHT_BUT_W, TOPNAV_BUT_H, h3, 16, TOPNAV_DARKBLUE, MAIN.WHITE);
        createIssuesButton("Issues", (int)tutorialsButton.getPosition()[0] - TOPRIGHT_BUT_W - PAD_3, PAD_3, TOPRIGHT_BUT_W, TOPNAV_BUT_H, h3, 16, TOPNAV_DARKBLUE, MAIN.WHITE);
        createShopButton("Shop", (int)issuesButton.getPosition()[0] - TOPRIGHT_BUT_W - PAD_3, PAD_3, TOPRIGHT_BUT_W, TOPNAV_BUT_H, h3, 16, TOPNAV_DARKBLUE, MAIN.WHITE);
        createUpdateGuiButton("Update", (int)shopButton.getPosition()[0] - TOPRIGHT_BUT_W - PAD_3, PAD_3, TOPRIGHT_BUT_W, TOPNAV_BUT_H, h3, 16, TOPNAV_DARKBLUE, MAIN.WHITE);

        //SUBNAV TOP RIGHT
        createTopNavSettingsButton("Settings", MAIN.width - SUBNAV_BUT_W - PAD_3, SUBNAV_BUT_Y, SUBNAV_BUT_W, SUBNAV_BUT_H, h4, 14, SUBNAV_LIGHTBLUE, MAIN.WHITE);

        layoutSelector = new LayoutSelector(MAIN);
        tutorialSelector = new TutorialSelector(MAIN);
        configSelector = new ConfigSelector(MAIN);

        //updateNavButtonsBasedOnColorScheme();
    }

    public void initSecondaryNav() {

        boolean needToMakeSmoothingButton = (currentBoard instanceof SmoothingCapableBoard) && smoothingButton == null;

        if (!secondaryNavInit) {
            //Buttons on the left side of the GUI secondary nav bar
            createToggleDataStreamButton(stopButton_pressToStart_txt, PAD_3, SUBNAV_BUT_Y, DATASTREAM_BUT_W, SUBNAV_BUT_H, h4, 14, MAIN.TURN_ON_GREEN, MAIN.OPENBCI_DARKBLUE);
            createFiltersButton("Filters", PAD_3*2 + toggleDataStreamingButton.getWidth(), SUBNAV_BUT_Y, SUBNAV_BUT_W, SUBNAV_BUT_H, h4, 14, SUBNAV_LIGHTBLUE, MAIN.WHITE);

            //Appears at Top Right SubNav while in a Session
            createLayoutButton("Layout", MAIN.width - 3 - 60, SUBNAV_BUT_Y, 60, SUBNAV_BUT_H, h4, 14, SUBNAV_LIGHTBLUE, MAIN.WHITE);
            secondaryNavInit = true;
        }

        if (needToMakeSmoothingButton) {
            int pos_x = (int)filtersButton.getPosition()[0] + filtersButton.getWidth() + PAD_3;
            //Make smoothing button wider than most other topnav buttons to fit text comfortably
            createSmoothingButton(getSmoothingString(), pos_x, SUBNAV_BUT_Y, SUBNAV_BUT_W + 48, SUBNAV_BUT_H, h4, 14, SUBNAV_LIGHTBLUE, MAIN.WHITE);
        }


        //updateSecondaryNavButtonsColor();
    }

    public void update() {
        //ignore settings button when help dropdown is open
        settingsButton.setLock(tutorialSelector.isVisible);

        //Make sure these buttons don't get accidentally locked
        if (systemMode >= SYSTEMMODE_POSTINIT) {
            setLockTopLeftSubNavCp5Objects(controlPanel.isOpen);
        }

        if (previousSystemMode != systemMode) {
            if (systemMode >= SYSTEMMODE_POSTINIT) {
                layoutSelector.update();
                tutorialSelector.update();
                if ((int)(settingsButton.getPosition()[0]) != MAIN.width - (SUBNAV_BUT_W*2) + 3) {
                    settingsButton.setPosition(MAIN.width - (SUBNAV_BUT_W*2) + 3, SUBNAV_BUT_Y);
                    verbosePrint("TopNav: Updated Settings Button Position");
                }
            } else {
                if ((int)(settingsButton.getPosition()[0]) != MAIN.width - 70 - 3) {
                    settingsButton.setPosition(MAIN.width - 70 - 3, SUBNAV_BUT_Y);
                    verbosePrint("TopNav: Updated Settings Button Position");
                }
            }
            configSelector.update();
            previousSystemMode = systemMode;
        }

        boolean topNavSubClassIsOpen = layoutSelector.isVisible || configSelector.isVisible || tutorialSelector.isVisible;
        setDropdownMenuIsOpen(topNavSubClassIsOpen);
    }

    public void draw() {
        PImage logo;
        int topNavBg;
        int subNavBg;
        if (colorScheme == COLOR_SCHEME_ALTERNATIVE_A) {
            topNavBg = MAIN.OPENBCI_BLUE;
            subNavBg = SUBNAV_LIGHTBLUE;
            logo = logo_white;
        } else {
            topNavBg = MAIN.color(255);
            subNavBg = MAIN.color(229);
            logo = logo_black;
        }

        MAIN.pushStyle();
        //stroke(OPENBCI_DARKBLUE);
        MAIN.fill(topNavBg);
        MAIN.rect(0, 0, MAIN.width, navBarHeight);
        //noStroke();
        MAIN.stroke(strokeColor);
        MAIN.fill(subNavBg);
        MAIN.rect(-1, navBarHeight, MAIN.width+2, navBarHeight);
        MAIN.popStyle();

        //hide the center logo if buttons would overlap it
        if (MAIN.width > 860) {
            //this is the center logo
            MAIN.image(logo, MAIN.width/2 - (128/2) - 2, 1, 128, 29);
        }

        //Draw these buttons during a Session
        boolean isSession = systemMode == SYSTEMMODE_POSTINIT;
        if (secondaryNavInit) {
            toggleDataStreamingButton.setVisible(isSession);
            filtersButton.setVisible(isSession);
            layoutButton.setVisible(isSession);

        }
        if (smoothingButton != null) {
            smoothingButton.setVisible(isSession);
        }

        //Draw CP5 Objects
        topNav_cp5.draw();

        //Draw everything in these selector boxes above all topnav cp5 objects
        layoutSelector.draw();
        tutorialSelector.draw();
        configSelector.draw();

        //Draw Console Log Image on top of cp5 object
        PImage _logo = (colorScheme == COLOR_SCHEME_DEFAULT) ? consoleImgBlue : consoleImgWhite;
        MAIN.image(_logo, debugButton.getPosition()[0] + 6, debugButton.getPosition()[1] + 2, 22, 22);


    }

    public void screenHasBeenResized(int _x, int _y) {
        topNav_cp5.setGraphics(MAIN, 0, 0); //Important!
        debugButton.setPosition(MAIN.width - debugButton.getWidth() - PAD_3, PAD_3);
        tutorialsButton.setPosition((int)debugButton.getPosition()[0] - TOPRIGHT_BUT_W - PAD_3, PAD_3);
        issuesButton.setPosition(tutorialsButton.getPosition()[0] - tutorialsButton.getWidth() - PAD_3, PAD_3);
        shopButton.setPosition(issuesButton.getPosition()[0] - issuesButton.getWidth() - PAD_3, PAD_3);
        updateGuiVersionButton.setPosition(shopButton.getPosition()[0] - shopButton.getWidth() - PAD_3, PAD_3);
        settingsButton.setPosition(MAIN.width - settingsButton.getWidth() - PAD_3, SUBNAV_BUT_Y);

        if (systemMode == SYSTEMMODE_POSTINIT) {
            toggleDataStreamingButton.setPosition(PAD_3, SUBNAV_BUT_Y);
            filtersButton.setPosition(PAD_3*2 + toggleDataStreamingButton.getWidth(), SUBNAV_BUT_Y);

            layoutButton.setPosition(MAIN.width - 3 - layoutButton.getWidth(), SUBNAV_BUT_Y);
            settingsButton.setPosition(MAIN.width - (settingsButton.getWidth()*2) + PAD_3, SUBNAV_BUT_Y);
            //Make sure to re-position UI in selector boxes
            layoutSelector.screenResized();
        }

        tutorialSelector.screenResized();
        configSelector.screenResized();
    }

    public void mousePressed() {
        layoutSelector.mousePressed();     //pass mousePressed along to layoutSelector
        tutorialSelector.mousePressed();
        configSelector.mousePressed();
    }

    public void mouseReleased() {
        layoutSelector.mouseReleased();    //pass mouseReleased along to layoutSelector
        tutorialSelector.mouseReleased();
        configSelector.mouseReleased();
    } //end mouseReleased

    //Load data from the latest release page using Github API and compare to local version
    public Boolean guiVersionIsUpToDate() {
        //Copy the local GUI version from OpenBCI_GUI.pde
        float localVersion = getVersionAsFloat(localGUIVersionString);

        boolean internetIsConnected = pingWebsite(guiLatestVersionGithubAPI);

        if (internetIsConnected) {
            MAIN.println("TopNav: Internet Connection Successful");
            //Get the latest release version from Github
            String remoteVersionString = getGUIVersionFromInternet(guiLatestVersionGithubAPI);
            float remoteVersion = getVersionAsFloat(remoteVersionString);

            MAIN.println("Local Version: " + localGUIVersionString + ", Latest Version: " + remoteVersionString);

            if (localVersion < remoteVersion) {
                MAIN.println("GUI needs to be updated. Download at https://github.com/OpenBCI/OpenBCI_GUI/releases/latest");
                updateGuiVersionButton.setDescription("GUI needs to be updated. -- Local: " + localGUIVersionString +  " GitHub: " + remoteVersionString);
                return false;
            } else {
                MAIN.println("GUI is up to date!");
                updateGuiVersionButton.setDescription("GUI is up to date! -- Local: " + localGUIVersionString +  " GitHub: " + remoteVersionString);
                return true;
            }
        } else {
            MAIN.println("TopNav: Internet Connection Not Available");
            MAIN.println("Local GUI Version: " + localGUIVersionString);
            updateGuiVersionButton.setDescription("Connect to internet to check GUI version. -- Local: " + localGUIVersionString);
            return null;
        }
    }

    private String getGUIVersionFromInternet(String _url) {
        String version = null;
        try {
            GetRequest get = new GetRequest(_url);
            get.send(); // program will wait untill the request is completed
            JSONObject response = MAIN.parseJSONObject(get.getContent());
            version = response.getString("name");
        } catch (Exception e) {
            outputError("Network Error: Unable to resolve host @ " + _url);
        }
        return version;
    }

    //Convert version string to float using each segment as a digit.
    //Examples: 5.0.0-alpha.2 -> 500.12, 5.0.1-beta.9 -> 501.29, 5.0.1 -> 501.5
    private float getVersionAsFloat(String s) {
        float val = 0f;

        //Remove v
        if (s.charAt(0) == 'v') {
            String[] tempArr = MAIN.split(s, 'v');
            s = tempArr[1];
        }

        //Check for minor version
        if (s.length() > 5) {
            String[] minorVersion = MAIN.split(s, '-'); //separate the string at the dash between "5.0.0" and "alpha.2"
            s = minorVersion[0];
            String[] mv = MAIN.split(minorVersion[1], '.');
            if (mv[0].equals("alpha")) {
                val += .1;
            } else if (mv[0].equals("beta")) {
                val += .2;
            }
            val += Integer.parseInt(mv[1]) * .01;
        } else {
            val += .5; //For stable version, add .5 so that it is greater than all alpha and beta versions
        }


        String[] strArray = MAIN.split(s, '.');


        int[] webVersionCompareArray = new int[strArray.length];
        for (int i = 0; i < strArray.length; i++) {
            try {
                webVersionCompareArray[i] = Integer.parseInt(strArray[i].trim());
            } catch (NumberFormatException e) {
                webVersionCompareArray[i] = 0; // 或者其他默认值
            }
        }


        val = webVersionCompareArray[0]*100 + webVersionCompareArray[1]*10 + webVersionCompareArray[2] + val;

        return val;
    }

    public void updateSmoothingButtonText() {
        smoothingButton.getCaptionLabel().setText(getSmoothingString());
    }

    private String getSmoothingString() {
        return ((SmoothingCapableBoard)currentBoard).getSmoothingActive() ? "Smoothing On" : "Smoothing Off";
    }

    private Button createTNButton(String name, String text, int _x, int _y, int _w, int _h, PFont _font, int _fontSize, int _bg, int _textColor) {
        return MAIN.createButton(topNav_cp5, name, text, _x, _y, _w, _h, 0, _font, _fontSize, _bg, _textColor, MAIN.BUTTON_HOVER, MAIN.BUTTON_PRESSED, MAIN.OPENBCI_DARKBLUE, -1);
    }

    private void createControlPanelCollapser(String text, int _x, int _y, int _w, int _h, PFont font, int _fontSize, int _bg, int _textColor) {
        controlPanelCollapser = createTNButton("controlPanelCollapser", text, _x, _y, _w, _h, font, _fontSize, _bg, _textColor);
        controlPanelCollapser.setSwitch(true);
        controlPanelCollapser.setOn();
        controlPanelCollapser.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                if (controlPanelCollapser.isOn()) {
                    controlPanel.open();
                } else {
                    controlPanel.close();
                }
            }
        });
    }

    private void createToggleDataStreamButton(String text, int _x, int _y, int _w, int _h, PFont font, int _fontSize, int _bg, int _textColor) {
        toggleDataStreamingButton = createTNButton("toggleDataStreamingButton", text, _x, _y, _w, _h, font, _fontSize, _bg, _textColor);
        toggleDataStreamingButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                stopButtonWasPressed();
            }
        });
        toggleDataStreamingButton.setDescription("Press this button to Stop/Start the data stream. Or press <SPACEBAR>");
    }

    private void createFiltersButton(String text, int _x, int _y, int _w, int _h, PFont font, int _fontSize, int _bg, int _textColor) {
        filtersButton = createTNButton("filtersButton", text, _x, _y, _w, _h, font, _fontSize, _bg, _textColor);
        filtersButton.onRelease(new CallbackListener() {
            public synchronized void controlEvent(CallbackEvent theEvent) {
                if (!MAIN.filterUIPopupIsOpen) {
                    FilterUIPopup filtersUI = new FilterUIPopup(MAIN);
                }
            }
        });
        filtersButton.setDescription("Here you can adjust the Filters that are applied to \"Filtered\" data.");
    }

    private void createSmoothingButton(String text, int _x, int _y, int _w, int _h, PFont font, int _fontSize, final int _bg, int _textColor) {
        SmoothingCapableBoard smoothBoard = (SmoothingCapableBoard)currentBoard;
        int bgColor = smoothBoard.getSmoothingActive() ? _bg : MAIN.BUTTON_LOCKED_GREY;
        smoothingButton = createTNButton("smoothingButton", text, _x, _y, _w, _h, font, _fontSize, bgColor, _textColor);
        smoothingButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                SmoothingCapableBoard smoothBoard = (SmoothingCapableBoard)currentBoard;
                smoothBoard.setSmoothingActive(!smoothBoard.getSmoothingActive());
                smoothingButton.getCaptionLabel().setText(getSmoothingString());
                int _bgColor = smoothBoard.getSmoothingActive() ? _bg : MAIN.BUTTON_LOCKED_GREY;
                smoothingButton.setColorBackground(_bgColor);
            }
        });
        smoothingButton.setDescription("The default settings for the Cyton Dongle driver can make data appear \"choppy.\" This feature will \"smooth\" the data for you. Click \"Help\" -> \"Cyton Driver Fix\" for more info. Clicking here will toggle this setting.");
    }

    private void createLayoutButton(String text, int _x, int _y, int _w, int _h, PFont font, int _fontSize, int _bg, int _textColor) {
        layoutButton = createTNButton("layoutButton", text, _x, _y, _w, _h, font, _fontSize, _bg, _textColor);
        layoutButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                //make sure that you can't open the layout selector accidentally
                if (!tutorialSelector.isVisible) {
                    //println("TopNav: Layout Dropdown Toggled");
                    layoutSelector.toggleVisibility();
                }
            }
        });
        layoutButton.setDescription("Here you can alter the overall layout of the GUI, allowing for different container configurations with more or less widgets.");
    }

    private void createDebugButton(String text, int _x, int _y, int _w, int _h, PFont font, int _fontSize, int _bg, int _textColor) {
        debugButton = createTNButton("debugButton", text, _x, _y, _w, _h, font, _fontSize, _bg, _textColor);
        debugButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                ConsoleWindow.display();
            }
        });
        debugButton.setDescription("Click to open the Console Log window.");
    }

    private void createTutorialsButton(String text, int _x, int _y, int _w, int _h, PFont font, int _fontSize, int _bg, int _textColor) {
        tutorialsButton = createTNButton("tutorialsButton", text, _x, _y, _w, _h, font, _fontSize, _bg, _textColor);
        tutorialsButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                tutorialSelector.toggleVisibility();
            }
        });
        tutorialsButton.setDescription("Click to find links to helpful online tutorials and getting started guides. Also, check out how to create custom widgets for the GUI!");
    }

    private void createIssuesButton(String text, int _x, int _y, int _w, int _h, PFont font, int _fontSize, int _bg, int _textColor) {
        final String helpText = "If you have suggestions or want to share a bug you've found, please create an issue on the GUI's Github repo!";
        issuesButton = createTNButton("issuesButton", text, _x, _y, _w, _h, font, _fontSize, _bg, _textColor);
        issuesButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                openURLInBrowser("https://github.com/OpenBCI/OpenBCI_GUI/issues");
            }
        });
        issuesButton.setDescription("If you have suggestions or want to share a bug you've found, please create an issue on the GUI's Github repo!");
    }

    private void createShopButton(String text, int _x, int _y, int _w, int _h, PFont font, int _fontSize, int _bg, int _textColor) {
        shopButton = createTNButton("shopButton", text, _x, _y, _w, _h, font, _fontSize, _bg, _textColor);
        shopButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                openURLInBrowser("https://shop.openbci.com/");
            }
        });
        shopButton.setDescription("Head to our online store to purchase the latest OpenBCI hardware and accessories.");
    }

    private void createUpdateGuiButton(String text, int _x, int _y, int _w, int _h, PFont font, int _fontSize, int _bg, int _textColor) {
        updateGuiVersionButton = createTNButton("updateGuiVersionButton", text, _x, _y, _w, _h, font, _fontSize, _bg, _textColor);
        //Attempt to compare local and remote GUI versions when TopNav is instantiated
        //This will also set the description/help-text for this cp5 button
        //Do this check on app start and store as a global variable
        guiIsUpToDate = guiVersionIsUpToDate();

        updateGuiVersionButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                //Perform check again when button is pressed. User may have connected to internet by now!
                guiIsUpToDate = guiVersionIsUpToDate();

                if (guiIsUpToDate == null) {
                    outputError("Update GUI: Unable to check for new version of GUI. Try again when connected to the internet.");
                    return;
                }

                if (!guiIsUpToDate) {
                    openURLInBrowser(guiLatestReleaseLocation);
                    outputInfo("Update GUI: Opening latest GUI release page using default browser");
                } else {
                    outputSuccess("Update GUI: Local OpenBCI GUI is up-to-date!");
                }
            }
        });

        if (guiIsUpToDate == null) {
            return;
        }

        if (!guiIsUpToDate) {
            outputWarn("Update Available! Press the \"Update\" button at the top of the GUI to download the latest version.");
        }
    }

    private void createTopNavSettingsButton(String text, int _x, int _y, int _w, int _h, PFont font, int _fontSize, int _bg, int _textColor) {
        settingsButton = createTNButton("settingsButton", text, _x, _y, _w, _h, font, _fontSize, _bg, _textColor);
        settingsButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                //make Help button and Settings button mutually exclusive
                if (!tutorialSelector.isVisible) {
                    configSelector.toggleVisibility();
                }
            }
        });
        settingsButton.setDescription("Save and Load GUI Settings! Click Default to revert to factory settings.");
    }

    //Execute this function whenver the stop button is pressed
    public void stopButtonWasPressed() {

        //Exit method if doing Cyton impedance check. Avoids a BrainFlow error.
        if (currentBoard instanceof BoardCyton && w_cytonImpedance != null) {
            Integer checkingImpOnChan = ((ImpedanceSettingsBoard)currentBoard).isCheckingImpedanceOnChannel();
            //println("isCheckingImpedanceOnAnythingEZCHECK==",w_cytonImpedance.isCheckingImpedanceOnAnything);
            if (checkingImpOnChan != null || w_cytonImpedance.cytonMasterImpedanceCheckIsActive() || w_cytonImpedance.isCheckingImpedanceOnAnything) {
                PopupMessage msg = new PopupMessage(MAIN, "Busy Checking Impedance", "Please turn off impedance check to begin recording the data stream.");
                MAIN.println("OpenBCI_GUI::Cyton: Please turn off impedance check to begin recording the data stream.");
                return;
            }
        }

        //toggle the data transfer state of the ADS1299...stop it or start it...
        if (currentBoard.isStreaming()) {
            output("openBCI_GUI: stopButton was pressed. Stopping data transfer, wait a few seconds.");
            stopRunning();
            if (!currentBoard.isStreaming()) {
                toggleDataStreamingButton.getCaptionLabel().setText(stopButton_pressToStart_txt);
                toggleDataStreamingButton.setColorBackground(MAIN.TURN_ON_GREEN);
            }
        } else { //not running
            output("openBCI_GUI: startButton was pressed. Starting data transfer, wait a few seconds.");
            startRunning();
            if (currentBoard.isStreaming()) {
                toggleDataStreamingButton.getCaptionLabel().setText(stopButton_pressToStop_txt);
                toggleDataStreamingButton.setColorBackground(MAIN.TURN_OFF_RED);
                nextPlayback_millis = MAIN.millis();  //used for synthesizeData and readFromFile.  This restarts the clock that keeps the playback at the right pace.
            }
        }
    }

    public boolean dataStreamingButtonIsActive() {
        return toggleDataStreamingButton.getCaptionLabel().getText().equals(stopButton_pressToStop_txt);
    }

    public void resetStartStopButton() {
        if (toggleDataStreamingButton != null) {
            toggleDataStreamingButton.getCaptionLabel().setText(stopButton_pressToStart_txt);
            toggleDataStreamingButton.setColorBackground(MAIN.TURN_ON_GREEN);
        }
    }

    public void destroySmoothingButton() {
        topNav_cp5.remove("smoothingButton");
        smoothingButton = null;
    }

    public void setLockTopLeftSubNavCp5Objects(boolean _b) {
        toggleDataStreamingButton.setLock(_b);
        filtersButton.setLock(_b);
    }

    public boolean getDropdownMenuIsOpen() {
        return topNavDropdownMenuIsOpen;
    }

    public void setDropdownMenuIsOpen(boolean b) {
        topNavDropdownMenuIsOpen = b;
    }
}