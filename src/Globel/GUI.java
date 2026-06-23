package Globel;

import BoardCyton_.CytonSDMode;
import BoardNull_.BoardNull;
import ConsoleLog_.CustomOutputStream;
import ControlPanel_.ControlPanel;
import CustomCp5Classes_.ButtonHelpText;
import CustomCp5Classes_.CopyPaste;
import CustomCp5Classes_.TextFieldUpdateHelper;
import DataLogger_.DataLogger;
import DataProcessing_.DataProcessing;
import DataSource_.DataSource;
import Debugging_.HelpWidget;
import DirectoryManager_.DirectoryManager;
import Extras_.DataStatus;
import Extras_.PlotFontInfo;
import FilterSettings_.FilterSettings;
import GuiSettings_.GuiSettings;
import InterfaceSerial_.InterfaceSerial;
import PopupMessage_.PopupMessage;
import SessionSettings_.SessionSettings;
import TopNav_.TopNav;
import WidgetManager_.WidgetManager;
import brainflow.BoardShim;
import brainflow.BrainFlowError;
import controlP5.*;
import ddf.minim.AudioOutput;
import ddf.minim.Minim;
import ddf.minim.ugens.FilePlayer;
import gifAnimation.Gif;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;
import processing.data.JSONArray;
import processing.data.JSONObject;

import java.io.*;

import static Debugging_.GF.*;
import static Debugging_.GF.outputError;
import static W_Playback_.GF.brandPlaybackName;
import static W_Playback_.GF.isPlaybackHeader;

//import static GUI.GGVI.buttonHelpText;
//import static GUI.GGVI.p5;

public class GUI extends PApplet {
    //Used to check GUI version in TopNav.pde and displayed on the splash screen on startup
    public static String localGUIVersionString = "v1.0.0-beta.1";
    public static String localGUIVersionDate = "November 2025";
    public static String guiLatestVersionGithubAPI = "https://api.github.com/repos/OpenBCI/OpenBCI_GUI/releases/latest";
    public static String guiLatestReleaseLocation = "https://github.com/OpenBCI/OpenBCI_GUI/releases/latest";
    public static Boolean guiIsUpToDate;


    //used to switch between application states
    public static final int SYSTEMMODE_INTROANIMATION = -10;
    public static final int SYSTEMMODE_PREINIT = 0;
    public static final int SYSTEMMODE_POSTINIT = 10;
    public static int systemMode = SYSTEMMODE_INTROANIMATION; /* Modes: -10 = intro sequence; 0 = system stopped/control panel setings; 10 = gui; 20 = help guide */




    public int selectedSamplingRate = -1; //program-wide variable to track sampling rate, which can change depending on selected data source

    public static boolean midInit = false;
    public static boolean midInitCheck2 = false;
    public static boolean abandonInit = false;
    public static boolean systemHasHalted = true;
    public static boolean reinitRequested = false;

    public static final int NCHAN_CYTON = 8;
    public final int NCHAN_CYTON_DAISY = 16;
    public final int NCHAN_GANGLION = 4;

    //choose where to get the EEG data
    public static final int DATASOURCE_CYTON = 0; // new default, data from serial with Accel data CHIP 2014-11-03
    public static final int DATASOURCE_GANGLION = 1;  //looking for signal from OpenBCI board via Serial/COM port, no Aux data
    public static final int DATASOURCE_PLAYBACKFILE = 2;  //playback from a pre-recorded text file
    public static final int DATASOURCE_SYNTHETIC = 3;  //Synthetically generated data
    public static final int DATASOURCE_STREAMING = 5;
    public static  int eegDataSource = -1; //default to none of the options
    public static final int NUM_ACCEL_DIMS = 3;


    public enum BoardProtocol {
        NONE,
        SERIAL,
        NATIVE_BLE,
        WIFI,
        BLED112
    }
    public BoardProtocol selectedProtocol = BoardProtocol.NONE;

    public static boolean showStartupError = false;
    public static String startupErrorMessage = "";
    //here are variables that are used if loading input data from a CSV text file...double slash ("\\") is necessary to make a single slash
    public static String playbackData_fname = "N/A"; //only used if loading input data from a file
    public static String sdData_fname = "N/A"; //only used if loading input data from a sd file
    public static int nextPlayback_millis = -100; //any negative number

    public static String openBCI_portName = "N/A";  //starts as N/A but is selected from control panel to match your OpenBCI USB Dongle's serial/COM
    public static int openBCI_baud = 115200; //baud rate from the Arduino

    public static String ganglion_portName = "N/A";

    public static String wifi_portName = "N/A";
    public static String wifi_ipAddress = "192.168.4.1";

    public static String brainflowStreamer = "";

    ////// ---- Define variables related to OpenBCI board operations
//Define number of channels from cyton...first EEG channels, then aux channels
    public static int nchan = NCHAN_CYTON; //Normally, 8 or 16.  Choose a smaller number to show fewer on the GUI



    // Calculate nPointsPerUpdate based on sampling rate and buffer update rate
// @UPDATE_MILLIS: update the buffer every 40 milliseconds
// @nPointsPerUpdate: update the GUI after this many data points have been received.
// The sampling rate should be ideally a multiple of 25, so as to make actual buffer update rate exactly 40ms
    public static final int UPDATE_MILLIS = 40;
    public static int nPointsPerUpdate;   // no longer final, calculate every time in initSystem

    //define some data fields for handling data here in processing
    public static float dataProcessingRawBuffer[][]; //2D array to handle multiple data channels, each row is a new channel so that dataBuffY[3][] is channel 4
    public static float dataProcessingFilteredBuffer[][];
    public static float data_elec_imp_ohm[];

    //define how much time is shown on the time-domain montage plot (and how much is used in the FFT plot?)
    public static int dataBuff_len_sec = 20 + 2; //Add two seconds to max buffer to account for filter artifact on the left of the graph

    public static StopWatch sessionTimeElapsed;
    public static StopWatch streamTimeElapsed;

    public static String output_fname;

    //Used mostly in W_playback.pde
    public static JSONObject savePlaybackHistoryJSON;
    public static JSONObject loadPlaybackHistoryJSON;
    public static String userPlaybackHistoryFile;
    public static boolean playbackHistoryFileExists = false;
    public static String playbackData_ShortName;
    public static boolean recentPlaybackFilesHaveUpdated = false;

    // Serial output
    public static processing.serial.Serial serial_output;


    //program variables
    public static StringBuilder board_message;
    public static boolean textFieldIsActive = false;

    //set window size
    public static int win_w;  //window width
    public static int win_h; //window height

    public PImage cog ;
    public static Gif loadingGIF;
    public static Gif loadingGIF_blue;

    public static PImage logo_black;
    public static PImage logo_blue;
    public static PImage logo_white;
    public static PImage consoleImgBlue;
    public static PImage consoleImgWhite;

    public static PFont f1;
    public static PFont f2;
    public static PFont f3;
    public static PFont f4;
    public static PFont f5;

    public static PFont h1; //large Montserrat
    public static PFont h2; //large/medium Montserrat
    public static PFont h3; //medium Montserrat
    public static PFont h4; //small/medium Montserrat
    public static PFont h5; //small Montserrat

    public static PFont p0; //large bold Open Sans
    public static PFont p1; //large Open Sans
    public static PFont p2; //large/medium Open Sans
    public static PFont p3; //medium Open Sans
    public static PFont p15;
    public static PFont p4; //medium/small Open Sans
    public static PFont p13;
    public static PFont p5; //small Open Sans
    public static PFont p6; //small Open Sans
    public static PFont p7; //small Open Sans


    public static boolean setupComplete = false;


    public final static int COLOR_SCHEME_DEFAULT = 1;
    public final static int COLOR_SCHEME_ALTERNATIVE_A = 2;
    // int COLOR_SCHEME_ALTERNATIVE_B = 3;
    public static int colorScheme = COLOR_SCHEME_ALTERNATIVE_A;



    public static boolean wmVisible = true;
    public static CColor cp5_colors;


    //Global variable for general navigation bar height
    public static final int navHeight = 22;

    //Variables from TopNav.pde. Used to set text when stopping/starting data stream.
    public final static String stopButton_pressToStop_txt = "停止数据流";
    public final static String stopButton_pressToStart_txt = "开始数据流";



    public static final int navBarHeight = 32;

    public static ddf.minim.analysis.FFT[] fftBuff = new ddf.minim.analysis.FFT[nchan];    //from the minim library
    public static boolean isFFTFiltered = true; //yes by default ... this is used in dataProcessing.pde to determine which uV array feeds the FFT calculation

    public static StringBuilder globalScreenResolution;
    public static StringBuilder globalScreenDPI;

    //Starting to collect the GUI-wide color pallet here. Rename constants all caps later...
    public final int WHITE = color(255);
    public final int BLACK = color(0);
    public final int OPENBCI_DARKBLUE = color(1, 18, 41);
    public final int OPENBCI_BLUE = color(31, 69, 110);
    public final int OPENBCI_BLUE_ALPHA50 = color(31, 69, 110, 50);
    public final int OPENBCI_BLUE_ALPHA100 = color(31, 69, 110, 100);
    public int boxColor = color(200);
    public final int boxStrokeColor = OPENBCI_DARKBLUE;
    public final int isSelected_color = color(184, 220, 105); //Used for textfield borders,
    public final int colorNotPressed = WHITE;
    public final int buttonsLightBlue = color(57,128,204);
    public final int GREY_235 = color(235);
    public final int GREY_200 = color(200);
    public final int GREY_125 = color(125);
    public final int GREY_100 = color(100);
    public final int GREY_20 = color(20);
    public final int TURN_ON_GREEN = color(195, 242, 181);
    public final int TURN_OFF_RED = color(255, 210, 210);
    public final int BOLD_RED = color(224, 56, 45);
    public final int BUTTON_HOVER = color(177, 184, 193);//color(252, 221, 198);
    public final int BUTTON_HOVER_LIGHT = color(211, 222, 232);
    public final int BUTTON_PRESSED = color(150, 170, 200); //OPENBCI_DARKBLUE;
    public final int BUTTON_PRESSED_LIGHT = color(179, 187, 199);
    public final int BUTTON_LOCKED_GREY = color(128);
    public final int BUTTON_PRESSED_DARKGREY = color(50);
    public final int BUTTON_NOOBGREEN = color(114,204,171);
    public final int BUTTON_EXPERTPURPLE = color(135,95,154);
    public final int BUTTON_CAUTIONRED = color(214,100,100);
    public final int OBJECT_BORDER_GREY = color(150);
    public final int TOPNAV_DARKBLUE = OPENBCI_BLUE;
    public final int SUBNAV_LIGHTBLUE = buttonsLightBlue;
    //Use the same colors for X,Y,Z throughout Accelerometer widget
    public final int ACCEL_X_COLOR = BOLD_RED;
    public final int ACCEL_Y_COLOR = color(49, 113, 89);
    public final int ACCEL_Z_COLOR = color(54, 87, 158);
    //Signal check colors
    public final int SIGNAL_CHECK_YELLOW = color(221, 178, 13); //Same color as yellow channel color found below
    public final int SIGNAL_CHECK_YELLOW_LOWALPHA = color(221, 178, 13, 150);
    public final int SIGNAL_CHECK_RED = BOLD_RED;
    public final int SIGNAL_CHECK_RED_LOWALPHA = color(224, 56, 45, 150);


    public final int[] channelColors = new int[]{
                color(129, 129, 129),
                color(124, 75, 141),
                color(54, 87, 158),
                color(49, 113, 89),
                SIGNAL_CHECK_YELLOW,
                color(253, 94, 52),
                BOLD_RED,
                color(162, 82, 49),
    };

    public final int graphStroke = color(210);
    public final int graphBG = color(245);
    public final int textColor = OPENBCI_DARKBLUE;
    public final int strokeColor = color(138, 146, 153);
    public final int eggshell = color(255, 253, 248);


    public final int[] lineColor = new int[]{
        color(129, 129, 129),
                color(124, 75, 141),
                color(54, 87, 158),
                color(49, 113, 89),
                SIGNAL_CHECK_YELLOW,
                color(253, 94, 52),
                BOLD_RED,
                color(162, 82, 49),
                color(129, 129, 129),
                color(124, 75, 141),
                color(54, 87, 158),
                color(49, 113, 89),
                SIGNAL_CHECK_YELLOW,
                color(253, 94, 52),
                BOLD_RED,
                color(162, 82, 49)
    };


    public boolean emgSettingsPopupIsOpen = false;

    public static float[] smoothFac = new float[]{0.0F, 0.5F, 0.75F, 0.9F, 0.95F, 0.98F, 0.99F, 0.999F}; //used by FFT & Headplot
    public static int smoothFac_ind = 3;    //initial index into the smoothFac array = 0.75 to start .. used by FFT & Head Plots

    // ----- these variable/methods are used for adjusting the intensity factor of the headplot opacity ---------------------------------------------------------------------------------------------------------
    public static float default_vertScale_uV = 200.0F; //this defines the Y-scale on the montage plots...this is the vertical space between traces
    public static float[] vertScaleFactor = { 0.25f, 0.5f, 1.0f, 2.0f, 5.0f, 50.0f};
    public static int vertScaleFactor_ind = 2;
    public static float vertScale_uV = default_vertScale_uV;

    public boolean filterUIPopupIsOpen = false;

    public static boolean filterSettingsWereLoadedFromFile = false;

    public static ButtonHelpText buttonHelpText;
//
    public static TextFieldUpdateHelper textfieldUpdateHelper;

    public static CustomOutputStream outputStream;


    public static DirectoryManager directoryManager;
    public SessionSettings settings;
    public static GuiSettings guiSettings;
    public static DataProcessing dataProcessing;
    public static FilterSettings filterSettings;


    public static CopyPaste copyPaste;

    public static HelpWidget helpWidget;

//
//    // Initialize board
    public DataSource currentBoard = new BoardNull();
//
    public static DataLogger dataLogger;
//
//    // Intialize interface protocols
    public static InterfaceSerial iSerial; //This is messy, half-deprecated code. See comments in InterfaceSerial.pde - Nov. 2020
//
//    //define variables related to warnings to the user about whether the EEG data is nearly railed (and, therefore, of dubious quality)
    public static DataStatus[] is_railed;
//
//    //Cyton SD Card setting
    public static CytonSDMode cyton_sdSetting = CytonSDMode.NO_WRITE;
//
    public static ControlPanel controlPanel;
//
//    //Control Panel for (re)configuring system settings
    public static PlotFontInfo fontInfo;
//
    public static WidgetManager wm;
//

//
    public static TopNav topNav;

    public static Minim minim;
    public static FilePlayer[] auditoryNfbFilePlayers;
    public static ddf.minim.ugens.Gain[] auditoryNfbGains;
    public static AudioOutput audioOutput;
    public static boolean audioOutputIsAvailable;
//
//    // MAKE YOUR WIDGET GLOBALLY
//    public static W_timeSeries w_timeSeries;
//    public static W_fft w_fft;
//    public static W_Networking w_networking;
//    public static W_BandPower w_bandPower;
//    public static W_Accelerometer w_accelerometer;
//    public static W_CytonImpedance w_cytonImpedance;
//    public static W_GanglionImpedance w_ganglionImpedance;
//    public static W_HeadPlot w_headPlot;
//    public static W_template w_template1;
//    public static W_emg w_emg;
//    public static W_PulseSensor w_pulsesensor;
//    public static W_AnalogRead w_analogRead;
//    public static W_DigitalRead w_digitalRead;
//    public static W_playback w_playback;
//    public static W_Spectrogram w_spectrogram;
//    public static W_PacketLoss w_packetLoss;
//    public static W_Focus w_focus;
//    public static W_EMGJoystick w_emgJoystick;
//    public static W_Marker w_marker;

    //Reusable method for creating CP5 buttons throughout the GUI
    public Button createButton(ControlP5 _cp5, String name, String text, int _x, int _y, int _w, int _h, int _roundness, PFont _font, int _fontSize, int _bgColor, int _textColor, int _colorHover, int _colorPressed, Integer _strokeColor, int _marginTop) {
        final Button b = _cp5.addButton(name)
                .setPosition(_x, _y)
                .setSize(_w, _h)
                .setColorLabel(_textColor)
                .setCornerRoundness(_roundness) //From Processing rect(): To draw a rounded rectangle, add a fifth parameter, which is used as the radius value for all four corners.
                .setColorForeground(_colorHover)
                .setColorBackground(_bgColor)
                .setColorActive(_colorPressed)
                .setBorderColor(_strokeColor)
                ;
        b.getCaptionLabel()
                .setFont(_font)
                .toUpperCase(false)
                .setSize(_fontSize)
                .setText(text)
                .setColor(_textColor) //This sets the color of the button label
                .getStyle()
                .setMarginTop(_marginTop)
        ;
        //Add Help Text to all Buttons. If description is null or object is locked, take no action.
        b.addCallback(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                if (theEvent.getAction() == ControlP5.ACTION_ENTER && !b.isLock() && b.getDescription() != null) {
                    //Show helpt text if object is not locked and has a description
                    buttonHelpText.setButtonHelpText(b.getDescription(), (int)b.getPosition()[0] + b.getWidth()/2, (int)b.getPosition()[1] + (3*b.getHeight())/4);
                    buttonHelpText.setTimeUserEnteredUIObject();
                } else if (theEvent.getAction() == ControlP5.ACTION_LEAVE || theEvent.getAction() == ControlP5.ACTION_BROADCAST) {
                    //Hide help text if clicked or user's mouse leaves object
                    buttonHelpText.setVisible(false);
                }
            }
        });
        return b;
    }

    //Square corners and no text label adjustment w/ default hover and press colors
    public Button createButton(ControlP5 _cp5, String name, String text, int _x, int _y, int _w, int _h, PFont _font, int _fontSize, int _bgColor, int _textColor) {
        return createButton(_cp5, name, text, _x, _y, _w, _h, 0, _font, _fontSize, _bgColor, _textColor, BUTTON_HOVER, BUTTON_PRESSED, OPENBCI_DARKBLUE, 0);
    }

    //Default button colors and fonts
    public Button createButton(ControlP5 _cp5, String name, String text, int _x, int _y, int _w, int _h) {
        return createButton(_cp5, name, text, _x, _y, _w, _h, 0, p7, 12, colorNotPressed, OPENBCI_DARKBLUE, BUTTON_HOVER, BUTTON_PRESSED, OPENBCI_DARKBLUE, 0);
    }
    //////////////////////////////////////
// GLOBAL FUNCTIONS BELOW THIS LINE   锛?W_Playback_\GF.java
    //////////////////////////////////////

    //Called when user selects a playback file from controlPanel dialog box
    public void playbackFileSelected(File selection) {
        if (selection == null) {
            println("DataLogging: playbackSelected: dialog was closed or cancelled.");
        } else {
            println("DataLogging: playbackSelected: user selected " + selection.getAbsolutePath());
            playbackFileSelected(selection.getAbsolutePath(), selection.getName());
        }
    }


    //Activated when user selects a file using the "Select Playback File" button in PlaybackHistory
    public void playbackSelectedWidgetButton(File selection) {
        if (selection == null) {
            println("W_Playback: playbackSelected: dialog was closed or cancelled.");
        } else {
            println("W_Playback: playbackSelected: user selected " + selection.getAbsolutePath());
            if (playbackFileSelected(selection.getAbsolutePath(), selection.getName())) {
                requestReinit();
            }
        }
    }

    //Activated when user selects a file using the recent file MenuList
    public void userSelectedPlaybackMenuList (String filePath, int listItem) {
        if (new File(filePath).isFile()) {
            playbackFileFromList(filePath, listItem);
            requestReinit();
        } else {
            verbosePrint("Playback: " + filePath);
            outputError("Playback: 选中的文件不存在，请重新选择可用文件。");
        }
    }

    //Called when user selects a playback file from a list
    public void playbackFileFromList (String longName, int listItem) {
        String shortName = "";
        try {
            savePlaybackHistoryJSON = loadJSONObject(new File(userPlaybackHistoryFile));
            JSONArray recentFilesArray = savePlaybackHistoryJSON.getJSONArray("playbackFileHistory");
            JSONObject playbackFile = recentFilesArray.getJSONObject(-listItem + recentFilesArray.size() - 1);
            shortName = brandPlaybackName(playbackFile.getString("id"));
            playbackHistoryFileExists = true;
        } catch (NullPointerException e) {
            playbackHistoryFileExists = false;
        }
        playbackFileSelected(longName, shortName);
    }

    //Handles the work for the above cases
    public boolean playbackFileSelected (String longName, String shortName) {
        playbackData_fname = longName;
        playbackData_ShortName = brandPlaybackName(shortName);
        try (BufferedReader brTest = new BufferedReader(new FileReader(longName))) {
            String line = brTest.readLine();
            if (isPlaybackHeader(line)) {
                verbosePrint("PLAYBACK: Found playback header in file.");
                sdData_fname = "N/A";
                for (int i = 0; i < 3; i++) {
                    line = brTest.readLine();
                    verbosePrint("PLAYBACK: " + line);
                }
                if (line == null || !line.startsWith("%Board")) {
                    playbackData_fname = "N/A";
                    playbackData_ShortName = "N/A";
                    outputError("检测到过旧版本的回放文件，请先完成格式转换后再加载。");
                    PopupMessage msg = new PopupMessage("旧版回放文件转换", "检测到较旧版本的回放文件，请先转换后再加载。", "LINK", "https://github.com/OpenBCI/OpenBCI_GUI/tree/development/tools");
                    return false;
                }
            } else if ("%STOP AT".equals(line)) {
                verbosePrint("PLAYBACK: Found SD recording header in file.");
                playbackData_fname = "N/A";
                sdData_fname = longName;
            } else {
                outputError("不支持该回放文件格式，请选择有效的 AirEIBCI 数据文件。");
                playbackData_fname = "N/A";
                playbackData_ShortName = "N/A";
                sdData_fname = "N/A";
                return false;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        outputSuccess("已选择 \"" + playbackData_ShortName + "\" 用于回放。");

        File f = new File(userPlaybackHistoryFile);
        if (!f.exists()) {
            println("AirEIBCI::playbackFileSelected: playback history file was not found.");
            playbackHistoryFileExists = false;
        } else {
            try {
                savePlaybackHistoryJSON = loadJSONObject(new File(userPlaybackHistoryFile));
                savePlaybackHistoryJSON.getJSONArray("playbackFileHistory");
                playbackHistoryFileExists = true;
            } catch (RuntimeException e) {
                outputError("回放历史文件损坏，请删除后重新启动软件。");
                File file = new File(userPlaybackHistoryFile);
                if (!file.isDirectory()) {
                    file.delete();
                }
            }
        }

        savePlaybackFileToHistory(longName);
        return true;
    }

    public void savePlaybackFileToHistory(String fileName) {
        int maxNumHistoryFiles = 36;
        if (playbackHistoryFileExists) {
            println("Playback history file found.");
            savePlaybackHistoryJSON = loadJSONObject(new File(userPlaybackHistoryFile));
            JSONArray recentFilesArray = savePlaybackHistoryJSON.getJSONArray("playbackFileHistory");
            removePlaybackFileFromHistory(recentFilesArray, playbackData_fname);
            for (int i = 0; i < recentFilesArray.size(); i++) {
                JSONObject playbackFile = recentFilesArray.getJSONObject(i);
                playbackFile.setInt("recentFileNumber", recentFilesArray.size()-i);
                playbackFile.setString("id", brandPlaybackName(playbackFile.getString("id")));
                playbackFile.setString("filePath", playbackFile.getString("filePath"));
                recentFilesArray.setJSONObject(i, playbackFile);
            }
            JSONObject mostRecentFile = new JSONObject();
            mostRecentFile.setInt("recentFileNumber", 0);
            mostRecentFile.setString("id", playbackData_ShortName);
            mostRecentFile.setString("filePath", playbackData_fname);
            recentFilesArray.append(mostRecentFile);
            if (recentFilesArray.size() >= maxNumHistoryFiles) {
                for (int i = 0; i <= recentFilesArray.size()-maxNumHistoryFiles; i++) {
                    recentFilesArray.remove(i);
                    println("ARRAY INDEX " + i + " REMOVED----");
                }
            }
            savePlaybackHistoryJSON.setJSONArray("playbackFileHistory", recentFilesArray);
            saveJSONObject(savePlaybackHistoryJSON, userPlaybackHistoryFile);

        } else if (!playbackHistoryFileExists) {
            println("Playback history file not found. Creating a new one.");
            JSONObject newHistoryFile = new JSONObject();
            JSONArray newHistoryFileArray = new JSONArray();
            JSONObject mostRecentFile = new JSONObject();
            mostRecentFile.setInt("recentFileNumber", 0);
            mostRecentFile.setString("id", playbackData_ShortName);
            mostRecentFile.setString("filePath", playbackData_fname);
            newHistoryFileArray.setJSONObject(0, mostRecentFile);
            newHistoryFile.setJSONArray("playbackFileHistory", newHistoryFileArray);
            saveJSONObject(newHistoryFile, userPlaybackHistoryFile);
            println("Playback history JSON has been created.");
            playbackHistoryFileExists = true;
        }
    }

    public static void removePlaybackFileFromHistory(JSONArray array, String _filePath) {
        for (int i = 0; i < array.size(); i++) {
            JSONObject playbackFile = array.getJSONObject(i);
            if (playbackFile.getString("filePath").equals(_filePath)) {
                array.remove(i);
            }
        }
    }


    public static void requestReinit() {
        reinitRequested = true;
    }


}
