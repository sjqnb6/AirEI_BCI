package TopNav_;

import GUI.GUIManager;
import controlP5.Button;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlP5;
import processing.core.PApplet;

import static Extras_.GF.isLinux;
import static Extras_.GF.isMac;
import static Globel.GUI.*;
import static Interactivity_.GF.openURLInBrowser;
import Globel.GUI;
public class TutorialSelector{

    GUI MAIN;
    private int x, y, w, h, margin, b_w, b_h;
    public boolean isVisible;
    private ControlP5 tutorial_cp5;
    private Button gettingStarted;
    private Button testingImpedance;
    private Button troubleshootingGuide;
    private Button customWidgets;
    private Button openbciForum;
    private Button ftdiBufferFix;
    private final int NUM_TUTORIAL_BUTTONS = 6;

    public TutorialSelector(GUI MAIN) {
        this.MAIN = MAIN;
        w = 180;
        //account for consoleLog button, help button, and spacing
        x = MAIN.width - 33 - w - 3*2;
        y = (MAIN.navBarHeight) - 3;
        margin = 6;
        b_w = w - margin*2;
        b_h = 22;
        h = margin*(NUM_TUTORIAL_BUTTONS+1) + b_h*NUM_TUTORIAL_BUTTONS;

        //Instantiate local cp5 for this box
        tutorial_cp5 = new ControlP5(MAIN);
        tutorial_cp5.setGraphics(MAIN, 0,0);
        tutorial_cp5.setAutoDraw(false);

        isVisible = false;

        int buttonNumber = 0;
        createGettingStartedButton("gettingStarted", "Getting Started", x + margin, y + margin*(buttonNumber+1) + b_h*(buttonNumber), b_w, b_h);
        buttonNumber++;
        createTestingImpedanceButton("testingImpedance", "Testing Impedance", x + margin, y + margin*(buttonNumber+1) + b_h*(buttonNumber), b_w, b_h);
        buttonNumber++;
        createFtdiBufferFixButton("ftdiBufferFix", "Cyton Driver Fix", x + margin, y + margin*(buttonNumber+1) + b_h*(buttonNumber), b_w, b_h);
        buttonNumber++;
        createTroubleshootingGuideButton("troubleshootingGuide", "Troubleshooting Guide", x + margin, y + margin*(buttonNumber+1) + b_h*(buttonNumber), b_w, b_h);
        buttonNumber++;
        createCustomWidgetsButton("customWidgets", "Building Custom Widgets", x + margin, y + margin*(buttonNumber+1) + b_h*(buttonNumber), b_w, b_h);
        buttonNumber++;
        createOpenbciForumButton("openbciForum", "OpenBCI Forum", x + margin, y + margin*(buttonNumber+1) + b_h*(buttonNumber), b_w, b_h);
    }

    void update() {
        if (isVisible) { //only update if visible
            // //close dropdown when mouse leaves
            // if ((mouseX < x || mouseX > x + w || mouseY < y || mouseY > y + h) && !topNav.tutorialsButton.isMouseHere()){
            //   toggleVisibility();
            // }
        }
    }

    public void draw() {
        if (isVisible) { //only draw if visible
            MAIN.pushStyle();

            MAIN.stroke(MAIN.OPENBCI_DARKBLUE);
            // fill(229); //bg
            MAIN.fill(MAIN.OPENBCI_BLUE); //bg
            MAIN.rect(x, y, w, h);


            // fill(177, 184, 193);
            MAIN.noStroke();
            //Draw a tiny rectangle to make it look like the box and button are connected
            MAIN.rect(x+w-(topNav.tutorialsButton.getWidth()-1), y, (topNav.tutorialsButton.getWidth()-1), 1);

            MAIN.popStyle();

            tutorial_cp5.draw();
        }
    }

    void isMouseHere() {
    }

    public void mousePressed() {
    }

    public void mouseReleased() {
        //only allow button interactivity if isVisible==true
        if (isVisible) {
            if ((MAIN.mouseX < x || MAIN.mouseX > x + w || MAIN.mouseY < y || MAIN.mouseY > y + h) && !topNav.tutorialsButton.isInside()) {
                toggleVisibility();
                //topNav.configButton.setIgnoreHover(false);
            }
        }
    }

    void screenResized() {

        tutorial_cp5.setGraphics(MAIN, 0,0);

        //update position of outer box and buttons. Y values do not change for this box.
        int oldX = x;
        x = MAIN.width - 33 - w - 3*2;
        int dx = oldX - x;

        for (int j = 0; j < tutorial_cp5.getAll().size(); j++) {
            Button c = (Button) tutorial_cp5.getController(tutorial_cp5.getAll().get(j).getAddress());
            c.setPosition(c.getPosition()[0] - dx, c.getPosition()[1]);
        }

    }

    void toggleVisibility() {
        isVisible = !isVisible;
        if (systemMode >= SYSTEMMODE_POSTINIT) {
            if (isVisible) {
                //the very convoluted way of locking all controllers of a single controlP5 instance...
                for (int i = 0; i < wm.widgets.size(); i++) {
                    for (int j = 0; j < wm.widgets.get(i).cp5_widget.getAll().size(); j++) {
                        wm.widgets.get(i).cp5_widget.getController(wm.widgets.get(i).cp5_widget.getAll().get(j).getAddress()).lock();
                    }
                }
            } else {
                //the very convoluted way of unlocking all controllers of a single controlP5 instance...
                for (int i = 0; i < wm.widgets.size(); i++) {
                    for (int j = 0; j < wm.widgets.get(i).cp5_widget.getAll().size(); j++) {
                        wm.widgets.get(i).cp5_widget.getController(wm.widgets.get(i).cp5_widget.getAll().get(j).getAddress()).unlock();
                    }
                }
            }
        }
    }

    private void createGettingStartedButton(String name, String text, int _x, int _y, int _w, int _h) {
        gettingStarted = MAIN.createButton(tutorial_cp5, name, text, _x, _y, _w, _h);
        gettingStarted.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                openURLInBrowser("https://docs.openbci.com/GettingStarted/GettingStartedLanding/");
                toggleVisibility(); //shut layoutSelector if something is selected
            }
        });
        gettingStarted.setDescription("Need help getting started? Click here to view the official OpenBCI Getting Started guides.");
    }

    private void createTestingImpedanceButton(String name, String text, int _x, int _y, int _w, int _h) {
        testingImpedance = MAIN.createButton(tutorial_cp5, name, text, _x, _y, _w, _h);
        testingImpedance.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                openURLInBrowser("https://docs.openbci.com/Software/OpenBCISoftware/GUIDocs/#impedance-testing");
                toggleVisibility(); //shut layoutSelector if something is selected
            }
        });
        testingImpedance.setDescription("Click here to learn more about testing the impedance on electrodes using the OpenBCI GUI. This process is different for Cyton and Ganglion. Checking impedance only works with passive electrodes.");
    }

    private void createTroubleshootingGuideButton(String name, String text, int _x, int _y, int _w, int _h) {
        troubleshootingGuide = MAIN.createButton(tutorial_cp5, name, text, _x, _y, _w, _h);
        troubleshootingGuide.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                openURLInBrowser("https://docs.openbci.com/Troubleshooting/GUI_Troubleshooting/");
                toggleVisibility(); //shut layoutSelector if something is selected
            }
        });
        troubleshootingGuide.setDescription("Having trouble? Start here with some general troubleshooting tips found on the OpenBCI Docs.");
    }

    private void createCustomWidgetsButton(String name, String text, int _x, int _y, int _w, int _h) {
        customWidgets = MAIN.createButton(tutorial_cp5, name, text, _x, _y, _w, _h);
        customWidgets.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                openURLInBrowser("https://docs.openbci.com/Software/OpenBCISoftware/GUIWidgets/#custom-widget");
                toggleVisibility(); //shut layoutSelector if something is selected
            }
        });
        customWidgets.setDescription("Click here to learn about creating your own custom OpenBCI widgets!");
    }

    private void createOpenbciForumButton(String name, String text, int _x, int _y, int _w, int _h) {
        openbciForum = MAIN.createButton(tutorial_cp5, name, text, _x, _y, _w, _h);
        openbciForum.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                openURLInBrowser("https://openbci.com/forum/");
                toggleVisibility(); //shut layoutSelector if something is selected
            }
        });
        openbciForum.setDescription("Click here to visit the official OpenBCI Forum.");
    }

    private void createFtdiBufferFixButton(String name, String text, int _x, int _y, int _w, int _h) {
        openbciForum = MAIN.createButton(tutorial_cp5, name, text, _x, _y, _w, _h);
        openbciForum.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                String ftdiDriverDocUrl;
                if (isMac()) {
                    ftdiDriverDocUrl = "https://docs.openbci.com/Troubleshooting/FTDI_Fix_Mac/";
                } else if (isLinux()){
                    ftdiDriverDocUrl = "https://docs.openbci.com/Troubleshooting/FTDI_Fix_Linux/";
                } else {
                    ftdiDriverDocUrl = "https://docs.openbci.com/Troubleshooting/FTDI_Fix_Windows/";
                }
                openURLInBrowser(ftdiDriverDocUrl);
                toggleVisibility(); //shut layoutSelector if something is selected
            }
        });
        openbciForum.setDescription("Click here to view information on how to lower the Cyton Dongle latency for your current operating system.");
    }
}