package GUI;

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
import DirectoryManager_.DirectoryManager;
import Extras_.DataStatus;
import Extras_.PlotFontInfo;
import FilterSettings_.FilterSettings;
import GuiSettings_.GuiSettings;
import InterfaceSerial_.InterfaceSerial;
import SessionSettings_.SessionSettings;
import TopNav_.TopNav;
import WidgetManager_.WidgetManager;
import controlP5.CColor;
import gifAnimation.Gif;
import org.apache.commons.lang3.time.StopWatch;
import processing.core.PFont;
import processing.core.PImage;
import processing.data.JSONObject;

public class GGVI {

    //Used to check GUI version in TopNav.pde and displayed on the splash screen on startup
    public static String localGUIVersionString = "v6.0.0-beta.1";
    public static String localGUIVersionDate = "September 2023";
    public static String guiLatestVersionGithubAPI = "https://api.github.com/repos/OpenBCI/OpenBCI_GUI/releases/latest";
    public static String guiLatestReleaseLocation = "https://github.com/OpenBCI/OpenBCI_GUI/releases/latest";
    public static Boolean guiIsUpToDate;

    public static CopyPaste copyPaste;

    //used to switch between application states
    public static final int SYSTEMMODE_INTROANIMATION = -10;
    public static final int SYSTEMMODE_PREINIT = 0;
    public static final int SYSTEMMODE_POSTINIT = 10;
    public static int systemMode = SYSTEMMODE_INTROANIMATION; /* Modes: -10 = intro sequence; 0 = system stopped/control panel setings; 10 = gui; 20 = help guide */

    public static ControlPanel controlPanel;


    public static int selectedSamplingRate = -1; //program-wide variable to track sampling rate, which can change depending on selected data source

    public static boolean midInit = false;
    public static boolean midInitCheck2 = false;
    public static boolean abandonInit = false;
    public static boolean systemHasHalted = true;
    public static boolean reinitRequested = false;

    public static final int NCHAN_CYTON = 8;
    public static final int NCHAN_CYTON_DAISY = 16;
    public static final int NCHAN_GANGLION = 4;

    //choose where to get the EEG data
    public static final int DATASOURCE_CYTON = 0; // new default, data from serial with Accel data CHIP 2014-11-03
    public static final int DATASOURCE_GANGLION = 1;  //looking for signal from OpenBCI board via Serial/COM port, no Aux data
    public static final int DATASOURCE_PLAYBACKFILE = 2;  //playback from a pre-recorded text file
    public static final int DATASOURCE_SYNTHETIC = 3;  //Synthetically generated data
    public static final int DATASOURCE_STREAMING = 5;
    public static  int eegDataSource = -1; //default to none of the options
    public static final int NUM_ACCEL_DIMS = 3;


    public static enum BoardProtocol {
        NONE,
        SERIAL,
        NATIVE_BLE,
        WIFI,
        BLED112
    }
    public static  BoardProtocol selectedProtocol = BoardProtocol.NONE;

    public static boolean showStartupError = false;
    public static String startupErrorMessage = "";
    //here are variables that are used if loading input data from a CSV text file...double slash ("\\") is necessary to make a single slash
    public static String playbackData_fname = "N/A"; //only used if loading input data from a file
    public static String sdData_fname = "N/A"; //only used if loading input data from a sd file
    public static int nextPlayback_millis = -100; //any negative number

    // Initialize board
    public static DataSource currentBoard = new BoardNull();

    public static DataLogger dataLogger = new DataLogger();

    // Intialize interface protocols
    public static InterfaceSerial iSerial = new InterfaceSerial(); //This is messy, half-deprecated code. See comments in InterfaceSerial.pde - Nov. 2020
    public static String openBCI_portName = "N/A";  //starts as N/A but is selected from control panel to match your OpenBCI USB Dongle's serial/COM
    public static int openBCI_baud = 115200; //baud rate from the Arduino

    public static String ganglion_portName = "N/A";

    public static String wifi_portName = "N/A";
    public static String wifi_ipAddress = "192.168.4.1";

    public static String brainflowStreamer = "";

    ////// ---- Define variables related to OpenBCI board operations
//Define number of channels from cyton...first EEG channels, then aux channels
    public static int nchan = NCHAN_CYTON; //Normally, 8 or 16.  Choose a smaller number to show fewer on the GUI

    //define variables related to warnings to the user about whether the EEG data is nearly railed (and, therefore, of dubious quality)
    public static DataStatus[] is_railed;

    //Cyton SD Card setting
    public static CytonSDMode cyton_sdSetting = CytonSDMode.NO_WRITE;

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

    //Control Panel for (re)configuring system settings
    public static PlotFontInfo fontInfo;

    //program variables
    public static StringBuilder board_message;
    public static boolean textFieldIsActive = false;

    //set window size
    public static int win_w;  //window width
    public static int win_h; //window height

    public static PImage cog;
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

    public static boolean setupComplete = false;


    public final static int COLOR_SCHEME_DEFAULT = 1;
    public final static int COLOR_SCHEME_ALTERNATIVE_A = 2;
    // int COLOR_SCHEME_ALTERNATIVE_B = 3;
    public static int colorScheme = COLOR_SCHEME_ALTERNATIVE_A;


    public static WidgetManager wm;
    public static boolean wmVisible = true;
    public static CColor cp5_colors;


    //Global variable for general navigation bar height
    public static final int navHeight = 22;

    public static ButtonHelpText buttonHelpText;

    public static TextFieldUpdateHelper textfieldUpdateHelper;

    public static CustomOutputStream outputStream;

    //Variables from TopNav.pde. Used to set text when stopping/starting data stream.
    public final static String stopButton_pressToStop_txt = "Stop Data Stream";
    public final static String stopButton_pressToStart_txt = "Start Data Stream";

    public static DirectoryManager directoryManager;
    public static SessionSettings settings;
    public static GuiSettings guiSettings;
    public static DataProcessing dataProcessing;
    public static FilterSettings filterSettings;

    public static final int navBarHeight = 32;
    public static TopNav topNav;

    public static ddf.minim.analysis.FFT[] fftBuff = new ddf.minim.analysis.FFT[nchan];    //from the minim library
    public static boolean isFFTFiltered = true; //yes by default ... this is used in dataProcessing.pde to determine which uV array feeds the FFT calculation

    public static StringBuilder globalScreenResolution;
    public static StringBuilder globalScreenDPI;
}
