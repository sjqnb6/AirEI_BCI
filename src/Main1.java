
import ConsoleLog_.CustomOutputStream;
import CustomCp5Classes_.CopyPaste;
import DataLogger_.DataLogger;
import DirectoryManager_.DirectoryManager;
import GuiSettings_.GuiSettings;
import InterfaceSerial_.InterfaceSerial;
import SessionSettings_.SessionSettings;
import processing.core.PApplet;
import Globel.GUI;

import java.io.File;

import static Globel.GF.*;

public class Main1 extends GUI {

    static public void main(String[] passedArgs) {
        PApplet.main("Main1");

    }


    public void setup() {
        super.setup();
        println("=== OpenBCI GUI Setup Started ===");
        frameRate(120);  // 设置draw函数执行频率
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
        println("Welcome to the Processing-based OpenBCI GUI!"); //Welcome line.
        println("For more information, please visit: https://docs.openbci.com/Software/OpenBCISoftware/GUIDocs/");


        // Copy sample data to the Users' Documents folder +  create Recordings folder
        directoryManager.init();
        settings = new SessionSettings(this);
        guiSettings = new GuiSettings(directoryManager.getSettingsPath());
        userPlaybackHistoryFile = directoryManager.getSettingsPath()+"UserPlaybackHistory.json";

        //open window
//        ourApplet = this;

        // Bug #426: If setup takes too long, JOGL will time out waiting for the GUI to draw something.
        // moving the setup to a separate thread solves this. We just have to make sure not to
        // start drawing until delayed setup is done.
//        thread("delayedSetup");
    }

    public void draw() {
        print("s");
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


}

