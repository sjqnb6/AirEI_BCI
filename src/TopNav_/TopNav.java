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
import static java.awt.Font.createFont;

import Globel.GUI;
public class TopNav {

    private GUI MAIN;
    private final int TOPNAV_DARKBLUE;
    private final int SUBNAV_LIGHTBLUE;
    private int strokeColor;
    // Match W_Prediction industrial gray palette for target rows.
    private static final int INDUSTRIAL_TOP_BG = 0xFFB8B8B8;
    private static final int INDUSTRIAL_PANEL = 0xFFE9E9E9;
    private static final int INDUSTRIAL_CARD = 0xFFEFEFEF;
    private static final int INDUSTRIAL_TOP_BUTTON = 0xFF9A9A9A;
    private static final int INDUSTRIAL_BORDER = 0xFFA8A8A8;
    private static final int INDUSTRIAL_BORDER_DARK = 0xFF808080;
    private static final int INDUSTRIAL_TEXT = 0xFF242424;
    private static final int INDUSTRIAL_TEXT_LIGHT = 0xFFF6F6F6;
    private static final int INDUSTRIAL_HOVER = 0xFFE4E4E4;
    private static final int INDUSTRIAL_PRESSED = 0xFFD4D4D4;
    private static final int INDUSTRIAL_TOP_HOVER = 0xFFB3B3B3;
    private static final int INDUSTRIAL_TOP_PRESSED = 0xFF7A7A7A;
    private static final int INDUSTRIAL_TOP_BORDER = 0xFFE6E6E6;
    private static final int INDUSTRIAL_SUB_BUTTON = 0xFFEFEFEF;
    private static final int INDUSTRIAL_SUB_BUTTON_ACTIVE = 0xFFCECECE;

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
        //PFont font = MAIN.createFont("fonts/SourceHanSansSC-Regular-2.otf", 20);
        //topNav_cp5.setFont(font);

        //TOP LEFT OF GUI
        createControlPanelCollapser("系统控制面板", PAD_3, PAD_3, controlPanel_W, TOPNAV_BUT_H, p7, 16, INDUSTRIAL_SUB_BUTTON, INDUSTRIAL_TEXT);

        //TOP RIGHT OF GUI, FROM LEFT<---Right
        // createDebugButton("Debug", MAIN.width - DEBUG_BUT_W - PAD_3, PAD_3, DEBUG_BUT_W, TOPNAV_BUT_H, h3, 16, TOPNAV_DARKBLUE, MAIN.WHITE);
        // createTutorialsButton("帮助", (int)debugButton.getPosition()[0] - TOPRIGHT_BUT_W - PAD_3, PAD_3, TOPRIGHT_BUT_W, TOPNAV_BUT_H, p7, 16, TOPNAV_DARKBLUE, MAIN.WHITE);
        // createIssuesButton("问题", (int)tutorialsButton.getPosition()[0] - TOPRIGHT_BUT_W - PAD_3, PAD_3, TOPRIGHT_BUT_W, TOPNAV_BUT_H, p7, 16, TOPNAV_DARKBLUE, MAIN.WHITE);
        // createShopButton("购买", (int)issuesButton.getPosition()[0] - TOPRIGHT_BUT_W - PAD_3, PAD_3, TOPRIGHT_BUT_W, TOPNAV_BUT_H, p7, 16, TOPNAV_DARKBLUE, MAIN.WHITE);
        // createUpdateGuiButton("更新", (int)shopButton.getPosition()[0] - TOPRIGHT_BUT_W - PAD_3, PAD_3, TOPRIGHT_BUT_W, TOPNAV_BUT_H, p7, 16, TOPNAV_DARKBLUE, MAIN.WHITE);

        //SUBNAV TOP RIGHT
        createTopNavSettingsButton("设置", MAIN.width - SUBNAV_BUT_W - PAD_3, SUBNAV_BUT_Y, SUBNAV_BUT_W, SUBNAV_BUT_H, p7, 14, INDUSTRIAL_SUB_BUTTON, INDUSTRIAL_TEXT);

        layoutSelector = new LayoutSelector(MAIN);
        tutorialSelector = new TutorialSelector(MAIN);
        configSelector = new ConfigSelector(MAIN);

        //updateNavButtonsBasedOnColorScheme();
    }

    public void initSecondaryNav() {

        boolean needToMakeSmoothingButton = (MAIN.currentBoard instanceof SmoothingCapableBoard) && smoothingButton == null;

        if (!secondaryNavInit) {
            //Buttons on the left side of the GUI secondary nav bar
            createToggleDataStreamButton(stopButton_pressToStart_txt, PAD_3, SUBNAV_BUT_Y, DATASTREAM_BUT_W, SUBNAV_BUT_H, p7, 14, INDUSTRIAL_SUB_BUTTON, INDUSTRIAL_TEXT);
            createFiltersButton("滤波", PAD_3*2 + toggleDataStreamingButton.getWidth(), SUBNAV_BUT_Y, SUBNAV_BUT_W, SUBNAV_BUT_H, p7, 14, INDUSTRIAL_SUB_BUTTON, INDUSTRIAL_TEXT);

            //Appears at Top Right SubNav while in a Session
            createLayoutButton("界面布局", MAIN.width - 3 - 60, SUBNAV_BUT_Y, 60, SUBNAV_BUT_H, p7, 14, INDUSTRIAL_SUB_BUTTON, INDUSTRIAL_TEXT);
            secondaryNavInit = true;
        }

        if (needToMakeSmoothingButton) {
            int pos_x = (int)filtersButton.getPosition()[0] + filtersButton.getWidth() + PAD_3;
            //Make smoothing button wider than most other topnav buttons to fit text comfortably
            createSmoothingButton(getSmoothingString(), pos_x, SUBNAV_BUT_Y, SUBNAV_BUT_W + 48, SUBNAV_BUT_H, p7, 14, INDUSTRIAL_SUB_BUTTON, INDUSTRIAL_TEXT);
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
                    verbosePrint("TopNav：更新设置按钮位置");
                }
            } else {
                if ((int)(settingsButton.getPosition()[0]) != MAIN.width - 70 - 3) {
                    settingsButton.setPosition(MAIN.width - 70 - 3, SUBNAV_BUT_Y);
                    verbosePrint("TopNav：更新设置按钮位置");
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
        topNavBg = INDUSTRIAL_TOP_BG;
        subNavBg = INDUSTRIAL_PANEL;
        logo = logo_black;

        MAIN.pushStyle();
        //stroke(OPENBCI_DARKBLUE);
        MAIN.fill(topNavBg);
        MAIN.rect(0, 0, MAIN.width, navBarHeight);
        //noStroke();
        MAIN.stroke(INDUSTRIAL_BORDER);
        MAIN.fill(subNavBg);
        MAIN.rect(-1, navBarHeight, MAIN.width+2, navBarHeight);
        MAIN.stroke(INDUSTRIAL_BORDER_DARK);
        MAIN.line(0, navBarHeight, MAIN.width, navBarHeight);
        MAIN.popStyle();

        //hide the center logo if buttons would overlap it
        if (MAIN.width > 860) {
            //this is the center logo
            int logoH = 29;
            int logoW = (int)(logoH * ((float)logo.width / (float)logo.height));
            MAIN.image(logo, MAIN.width/2 - (logoW/2) - 2, 1, logoW, logoH);
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
        //MAIN.image(_logo, debugButton.getPosition()[0] + 6, debugButton.getPosition()[1] + 2, 22, 22);


    }

    public void screenHasBeenResized(int _x, int _y) {
        topNav_cp5.setGraphics(MAIN, 0, 0); //Important!
        //debugButton.setPosition(MAIN.width - debugButton.getWidth() - PAD_3, PAD_3);
        //tutorialsButton.setPosition((int)debugButton.getPosition()[0] - TOPRIGHT_BUT_W - PAD_3, PAD_3);
        //issuesButton.setPosition(tutorialsButton.getPosition()[0] - tutorialsButton.getWidth() - PAD_3, PAD_3);
        //shopButton.setPosition(issuesButton.getPosition()[0] - issuesButton.getWidth() - PAD_3, PAD_3);
        //updateGuiVersionButton.setPosition(shopButton.getPosition()[0] - shopButton.getWidth() - PAD_3, PAD_3);
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
                updateGuiVersionButton.setDescription("图形界面需要更新。——本地" + localGUIVersionString +  " GitHub: " + remoteVersionString);
                return false;
            } else {
                MAIN.println("GUI is up to date!");
                updateGuiVersionButton.setDescription("图形界面是最新的！——本地：" + localGUIVersionString +  " GitHub: " + remoteVersionString);
                return true;
            }
        } else {
            MAIN.println("TopNav: Internet Connection Not Available");
            MAIN.println("Local GUI Version: " + localGUIVersionString);
            updateGuiVersionButton.setDescription("连接互联网查看图形界面版本。——本地：" + localGUIVersionString);
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
            outputError("网络错误：无法解决服务问题 @ " + _url);
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
        return ((SmoothingCapableBoard)MAIN.currentBoard).getSmoothingActive() ? "平滑启用" : "平滑关闭";
    }

    private Button createTNButton(String name, String text, int _x, int _y, int _w, int _h, PFont _font, int _fontSize, int _bg, int _textColor) {
        return MAIN.createButton(topNav_cp5, name, text, _x, _y, _w, _h, 0, _font, _fontSize, _bg, _textColor, MAIN.BUTTON_HOVER, MAIN.BUTTON_PRESSED, MAIN.OPENBCI_DARKBLUE, -1);
    }

    private void applyIndustrialButtonStyle(Button button, int bgColor) {
        button.setColorBackground(bgColor);
        button.setColorForeground(INDUSTRIAL_HOVER);
        button.setColorActive(INDUSTRIAL_PRESSED);
        button.setBorderColor(INDUSTRIAL_BORDER_DARK);
        button.getCaptionLabel().setColor(INDUSTRIAL_TEXT);
    }

    private void applyIndustrialTopRowButtonStyle(Button button, int bgColor) {
        button.setColorBackground(bgColor);
        button.setColorForeground(INDUSTRIAL_TOP_HOVER);
        button.setColorActive(INDUSTRIAL_TOP_PRESSED);
        button.setBorderColor(INDUSTRIAL_TOP_BORDER);
        button.getCaptionLabel().setColor(INDUSTRIAL_TEXT_LIGHT);
    }

    private void updateDataStreamingButtonStyle(boolean streaming) {
        toggleDataStreamingButton.getCaptionLabel().setText(streaming ? stopButton_pressToStop_txt : stopButton_pressToStart_txt);
        int bgColor = streaming ? INDUSTRIAL_SUB_BUTTON_ACTIVE : INDUSTRIAL_SUB_BUTTON;
        applyIndustrialButtonStyle(toggleDataStreamingButton, bgColor);
    }

    private void createControlPanelCollapser(String text, int _x, int _y, int _w, int _h, PFont font, int _fontSize, int _bg, int _textColor) {
        controlPanelCollapser = createTNButton("controlPanelCollapser", text, _x, _y, _w, _h, font, _fontSize, _bg, _textColor);
        applyIndustrialButtonStyle(controlPanelCollapser, INDUSTRIAL_SUB_BUTTON);
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
        updateDataStreamingButtonStyle(false);
        toggleDataStreamingButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                stopButtonWasPressed();
            }
        });
        toggleDataStreamingButton.setDescription("按下这个按钮即可停止/启动数据流");
    }

    private void createFiltersButton(String text, int _x, int _y, int _w, int _h, PFont font, int _fontSize, int _bg, int _textColor) {
        filtersButton = createTNButton("filtersButton", text, _x, _y, _w, _h, font, _fontSize, _bg, _textColor);
        applyIndustrialButtonStyle(filtersButton, INDUSTRIAL_CARD);
        filtersButton.onRelease(new CallbackListener() {
            public synchronized void controlEvent(CallbackEvent theEvent) {
                if (!MAIN.filterUIPopupIsOpen) {
                    FilterUIPopup filtersUI = new FilterUIPopup(MAIN);
                }
            }
        });
        filtersButton.setDescription("在此可以调整应用于滤波数据的滤波器参数。");
    }

    private void createSmoothingButton(String text, int _x, int _y, int _w, int _h, PFont font, int _fontSize, final int _bg, int _textColor) {
        SmoothingCapableBoard smoothBoard = (SmoothingCapableBoard)MAIN.currentBoard;
        int bgColor = smoothBoard.getSmoothingActive() ? _bg : MAIN.BUTTON_LOCKED_GREY;
        smoothingButton = createTNButton("smoothingButton", text, _x, _y, _w, _h, font, _fontSize, bgColor, _textColor);
        applyIndustrialButtonStyle(smoothingButton, bgColor);
        smoothingButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                SmoothingCapableBoard smoothBoard = (SmoothingCapableBoard)MAIN.currentBoard;
                smoothBoard.setSmoothingActive(!smoothBoard.getSmoothingActive());
                smoothingButton.getCaptionLabel().setText(getSmoothingString());
                int _bgColor = smoothBoard.getSmoothingActive() ? _bg : MAIN.BUTTON_LOCKED_GREY;
                applyIndustrialButtonStyle(smoothingButton, _bgColor);
            }
        });
        smoothingButton.setDescription("Cyton 适配器驱动默认设置可能导致数据采样不连贯。启用此功能可平滑数据流。详见\"帮助\" -> \"Cyton 驱动修复\"。点击切换此设置。");
    }

    private void createLayoutButton(String text, int _x, int _y, int _w, int _h, PFont font, int _fontSize, int _bg, int _textColor) {
        layoutButton = createTNButton("layoutButton", text, _x, _y, _w, _h, font, _fontSize, _bg, _textColor);
        applyIndustrialButtonStyle(layoutButton, INDUSTRIAL_CARD);
        layoutButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                //make sure that you can't open the layout selector accidentally
                if (!tutorialSelector.isVisible) {
                    //println("TopNav: Layout Dropdown Toggled");
                    layoutSelector.toggleVisibility();
                }
            }
        });
        layoutButton.setDescription("在此可以调整图形界面的整体布局,支持不同的容器配置以显示更多或更少的部件。");
    }

    private void createDebugButton(String text, int _x, int _y, int _w, int _h, PFont font, int _fontSize, int _bg, int _textColor) {
        debugButton = createTNButton("debugButton", text, _x, _y, _w, _h, font, _fontSize, _bg, _textColor);
        debugButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                ConsoleWindow.display();
            }
        });
        debugButton.setDescription("点击打开控制台日志窗口。");
    }

    private void createTutorialsButton(String text, int _x, int _y, int _w, int _h, PFont font, int _fontSize, int _bg, int _textColor) {
        tutorialsButton = createTNButton("tutorialsButton", text, _x, _y, _w, _h, font, _fontSize, _bg, _textColor);
        tutorialsButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                tutorialSelector.toggleVisibility();
            }
        });
        tutorialsButton.setDescription("查看在线教程和入门指南");
    }

    private void createIssuesButton(String text, int _x, int _y, int _w, int _h, PFont font, int _fontSize, int _bg, int _textColor) {
        final String helpText = "如果你建议或想分享你发现的漏洞，请在Github仓库创建一个issue！";
        issuesButton = createTNButton("issuesButton", text, _x, _y, _w, _h, font, _fontSize, _bg, _textColor);
        issuesButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                openURLInBrowser("https://github.com/OpenBCI/OpenBCI_GUI/issues");
            }
        });
        issuesButton.setDescription("如果你想分享你发现的漏洞，请在Github仓库创建一个issue！");
    }

    private void createShopButton(String text, int _x, int _y, int _w, int _h, PFont font, int _fontSize, int _bg, int _textColor) {
        shopButton = createTNButton("shopButton", text, _x, _y, _w, _h, font, _fontSize, _bg, _textColor);
        shopButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                openURLInBrowser("https://shop.openbci.com/");
            }
        });
        shopButton.setDescription("访问在线商店购买最新产品");
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
                    outputError("更新 GUI: 无法检查新版本的图形界面。连接互联网后再试一次。");
                    return;
                }

                if (!guiIsUpToDate) {
                    openURLInBrowser(guiLatestReleaseLocation);
                    outputInfo("更新 GUI: 使用默认浏览器打开最新的 GUI 发布页面");
                } else {
                    outputSuccess("更新 GUI: 当前图形界面是最新的！");
                }
            }
        });

        if (guiIsUpToDate == null) {
            return;
        }

        if (!guiIsUpToDate) {
            outputWarn("发现新版本！点击界面顶部的\"更新\"按钮下载最新版本");
        }
    }

    private void createTopNavSettingsButton(String text, int _x, int _y, int _w, int _h, PFont font, int _fontSize, int _bg, int _textColor) {
        settingsButton = createTNButton("settingsButton", text, _x, _y, _w, _h, font, _fontSize, _bg, _textColor);
        applyIndustrialButtonStyle(settingsButton, INDUSTRIAL_SUB_BUTTON);
        settingsButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                //make Help button and Settings button mutually exclusive
                if (!tutorialSelector.isVisible) {
                    configSelector.toggleVisibility();
                }
            }
        });
        settingsButton.setDescription("保存并加载图形界面设置！");
    }

    //Execute this function whenver the stop button is pressed
    public void stopButtonWasPressed() {

        //Exit method if doing Cyton impedance check. Avoids a BrainFlow error.
        if (MAIN.currentBoard instanceof BoardCyton && w_cytonImpedance != null) {
            Integer checkingImpOnChan = ((ImpedanceSettingsBoard)MAIN.currentBoard).isCheckingImpedanceOnChannel();
            //println("isCheckingImpedanceOnAnythingEZCHECK==",w_cytonImpedance.isCheckingImpedanceOnAnything);
            if (checkingImpOnChan != null || w_cytonImpedance.cytonMasterImpedanceCheckIsActive() || w_cytonImpedance.isCheckingImpedanceOnAnything) {
                PopupMessage msg = new PopupMessage(MAIN, "Busy Checking Impedance", "Please turn off impedance check to begin recording the data stream.");
                MAIN.println("AirEIBCI::Cyton: Please turn off impedance check to begin recording the data stream.");
                return;
            }
        }

        //toggle the data transfer state of the ADS1299...stop it or start it...
        if (MAIN.currentBoard.isStreaming()) {
            output("AirEIBCI: 终止按钮被按下了。停止数据传输，等待几秒钟。");
            stopRunning(MAIN);
            if (!MAIN.currentBoard.isStreaming()) {
                updateDataStreamingButtonStyle(false);
            }
        } else { //not running
            output("AirEIBCI: 启动按钮被按下了。开始数据传输，等待几秒钟。");
            startRunning(MAIN);
            if (MAIN.currentBoard.isStreaming()) {
                updateDataStreamingButtonStyle(true);
                nextPlayback_millis = MAIN.millis();  //used for synthesizeData and readFromFile.  This restarts the clock that keeps the playback at the right pace.
            }
        }
    }

    public boolean dataStreamingButtonIsActive() {
        return toggleDataStreamingButton.getCaptionLabel().getText().equals(stopButton_pressToStop_txt);
    }

    public void resetStartStopButton() {
        if (toggleDataStreamingButton != null) {
            updateDataStreamingButtonStyle(false);
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
