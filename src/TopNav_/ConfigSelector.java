package TopNav_;

import GUI.GUIManager;
import GuiSettings_.ExpertModeEnum;
import controlP5.Button;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlP5;
import processing.core.PApplet;

import static Debugging_.GF.output;
import static Extras_.GF.isLinux;
import static GUI.GGVI.*;
import static SystemManager.GF.haltSystem;

public class ConfigSelector extends GUIManager {
    private int x, y, w, h, margin, b_w, b_h;
    private boolean clearAllSettingsPressed;
    public boolean isVisible;
    private ControlP5 settings_cp5;
    private Button expertMode;
    private Button saveSessionSettings;
    private Button loadSessionSettings;
    private Button defaultSessionSettings;
    private Button clearAllGUISettings;
    private Button clearAllSettingsNo;
    private Button clearAllSettingsYes;

    private int configHeight = 0;

    private int osPadding = 0;
    private int osPadding2 = 0;
    private int buttonSpacer = 0;

    public ConfigSelector(PApplet pApplet) {
        int _padding = (systemMode == SYSTEMMODE_POSTINIT) ? -3 : 3;
        w = 140;
        x = width - w - _padding;
        y = (navBarHeight * 2) - 3;
        margin = 6;
        b_w = w - margin*2;
        b_h = 22;
        h = margin*9 + b_h*8;
        //makes the setting text "are you sure" display correctly on linux
        osPadding = isLinux() ? -3 : -2;
        osPadding2 = isLinux() ? 5 : 0;

        //Instantiate local cp5 for this box
        settings_cp5 = new ControlP5(pApplet);
        settings_cp5.setGraphics(pApplet, 0,0);
        settings_cp5.setAutoDraw(false);

        isVisible = false;

        int buttonNumber = 0;
        createExpertModeButton("expertMode", "Turn Expert Mode On", x + margin, y + margin*(buttonNumber+1) + b_h*(buttonNumber), b_w, b_h);
        buttonNumber++;
        createSaveSettingsButton("saveSessionSettings", "Save", x + margin, y + margin*(buttonNumber+1) + b_h*(buttonNumber), b_w, b_h);
        buttonNumber++;
        createLoadSettingsButton("loadSessionSettings", "Load", x + margin, y + margin*(buttonNumber+1) + b_h*(buttonNumber), b_w, b_h);
        buttonNumber++;
        createDefaultSettingsButton("defaultSessionSettings", "Default", x + margin, y + margin*(buttonNumber+1) + b_h*(buttonNumber), b_w, b_h);
        buttonNumber++;
        createClearAllSettingsButton("clearAllGUISettings", "Clear All", x + margin, y + margin*(buttonNumber+1) + b_h*(buttonNumber), b_w, b_h);
        buttonNumber += 2;
        createClearSettingsNoButton("clearAllSettingsNo", "No", x + margin, y + margin*(buttonNumber+1) + b_h*(buttonNumber), b_w, b_h);
        buttonNumber++;
        createClearSettingsYesButton("clearAllSettingsYes", "Yes", x + margin, y + margin*(buttonNumber+1) + b_h*(buttonNumber), b_w, b_h);
    }

    public void update() {
    }

    public void draw() {
        if (isVisible) { //only draw if visible
            pushStyle();

            stroke(OPENBCI_DARKBLUE);
            fill(57, 128, 204); //bg
            rect(x, y, w, h);

            boolean isSessionStarted = (systemMode == SYSTEMMODE_POSTINIT);
            saveSessionSettings.setVisible(isSessionStarted);
            loadSessionSettings.setVisible(isSessionStarted);
            defaultSessionSettings.setVisible(isSessionStarted);

            if (clearAllSettingsPressed) {
                textFont(p2, 16);
                fill(255);
                textAlign(CENTER);
                text("Are You Sure?", x + w/2, clearAllGUISettings.getPosition()[1] + b_h*2);
            }
            clearAllSettingsYes.setVisible(clearAllSettingsPressed);
            clearAllSettingsNo.setVisible(clearAllSettingsPressed);

            fill(57, 128, 204);
            noStroke();
            //This makes the dropdown box look like it's apart of the button by drawing over the part that overlaps
            rect(x+w-(topNav.settingsButton.getWidth()-1), y, (topNav.settingsButton.getWidth()-1), 1);

            popStyle();

            settings_cp5.draw();
        }
    }

    public void isMouseHere() {
    }

    public void mousePressed() {
    }

    public void mouseReleased() {
        //only allow button interactivity if isVisible==true
        if (isVisible) {
            if ((mouseX < x || mouseX > x + w || mouseY < y || mouseY > y + h) && !topNav.settingsButton.isInside()) {
                toggleVisibility();
                clearAllSettingsPressed = false;
            }
        }
    }

    public void screenResized() {
        settings_cp5.setGraphics(pApplet, 0,0);
        updateConfigButtonPositions();
    }

    private void updateConfigButtonPositions() {
        //update position of outer box and buttons
        final boolean isSessionStarted = (systemMode == SYSTEMMODE_POSTINIT);
        int oldX = x;
        int multiplier = isSessionStarted ? 3 : 2;
        int _padding = isSessionStarted ? -3 : 3;
        x = width - 70*multiplier - _padding;
        int dx = oldX - x;

        h = !isSessionStarted ? margin*3 + b_h*2 : margin*6 + b_h*5;

        //Update the Y position for the clear settings buttons
        float clearSettingsButtonY = !isSessionStarted ?
                expertMode.getPosition()[1] + margin + b_h :
                defaultSessionSettings.getPosition()[1] + margin + b_h;
        clearAllGUISettings.setPosition(clearAllGUISettings.getPosition()[0], clearSettingsButtonY);
        clearAllSettingsNo.setPosition(clearAllSettingsNo.getPosition()[0], clearSettingsButtonY + margin*2 + b_h*2);
        clearAllSettingsYes.setPosition(clearAllSettingsYes.getPosition()[0], clearSettingsButtonY + margin*3 + b_h*3);

        //Update the X position for all buttons
        for (int j = 0; j < settings_cp5.getAll().size(); j++) {
            Button c = (Button) settings_cp5.getController(settings_cp5.getAll().get(j).getAddress());
            c.setPosition(c.getPosition()[0] - dx, c.getPosition()[1]);
        }

        //println("TopNav: ConfigSelector: Button Positions Updated");
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
                clearAllSettingsPressed = false;
            } else {
                //the very convoluted way of unlocking all controllers of a single controlP5 instance...
                for (int i = 0; i < wm.widgets.size(); i++) {
                    for (int j = 0; j < wm.widgets.get(i).cp5_widget.getAll().size(); j++) {
                        wm.widgets.get(i).cp5_widget.getController(wm.widgets.get(i).cp5_widget.getAll().get(j).getAddress()).unlock();
                    }
                }
            }
        }

        //When closed by any means and confirmation buttons are open...
        //Hide confirmation buttons and shorten height of this box
        if (clearAllSettingsPressed && !isVisible) {
            //Shorten height of this box
            h -= margin*4 + b_h*3;
            clearAllSettingsPressed = false;
        }

        updateConfigButtonPositions();
    }

    private void createExpertModeButton(String name, String text, int _x, int _y, int _w, int _h) {
        expertMode = createButton(settings_cp5, name, text, _x, _y, _w, _h, p5, 12, BUTTON_NOOBGREEN, WHITE);
        expertMode.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                toggleVisibility();
                boolean isActive = !guiSettings.getExpertModeBoolean();
                toggleExpertModeFrontEnd(isActive);
                String outputMsg = isActive ?
                        "Expert Mode ON: All keyboard shortcuts and features are enabled!" :
                        "Expert Mode OFF: Use spacebar to start/stop the data stream.";
                output(outputMsg);
                guiSettings.setExpertMode(isActive ? ExpertModeEnum.ON : ExpertModeEnum.OFF);
            }
        });
        expertMode.setDescription("Expert Mode enables advanced keyboard shortcuts and access to all GUI features.");
    }

    private void createSaveSettingsButton(String name, String text, int _x, int _y, int _w, int _h) {
        saveSessionSettings = createButton(settings_cp5, name, text, _x, _y, _w, _h);
        saveSessionSettings.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                toggleVisibility();
                settings.saveButtonPressed();
            }
        });
        saveSessionSettings.setDescription("Expert Mode enables advanced keyboard shortcuts and access to all GUI features.");
    }

    private void createLoadSettingsButton(String name, String text, int _x, int _y, int _w, int _h) {
        loadSessionSettings = createButton(settings_cp5, name, text, _x, _y, _w, _h);
        loadSessionSettings.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                toggleVisibility();
                settings.loadButtonPressed();
            }
        });
        loadSessionSettings.setDescription("Expert Mode enables advanced keyboard shortcuts and access to all GUI features.");
    }

    private void createDefaultSettingsButton(String name, String text, int _x, int _y, int _w, int _h) {
        defaultSessionSettings = createButton(settings_cp5, name, text, _x, _y, _w, _h);
        defaultSessionSettings.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                toggleVisibility();
                settings.defaultButtonPressed();
            }
        });
        defaultSessionSettings.setDescription("Expert Mode enables advanced keyboard shortcuts and access to all GUI features.");
    }

    private void createClearAllSettingsButton(String name, String text, int _x, int _y, int _w, int _h) {
        clearAllGUISettings = createButton(settings_cp5, name, text, _x, _y, _w, _h, p5, 12, BUTTON_CAUTIONRED, WHITE);
        clearAllGUISettings.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                //Leave box open if this button was pressed and toggle flag
                clearAllSettingsPressed = !clearAllSettingsPressed;
                //Expand or shorten height of this box
                final int delta_h = margin*4 + b_h*3;
                h += clearAllSettingsPressed ? delta_h : -delta_h;
            }
        });
        clearAllGUISettings.setDescription("This will clear all user settings and playback history. You will be asked to confirm.");
    }

    private void createClearSettingsNoButton(String name, String text, int _x, int _y, int _w, int _h) {
        clearAllSettingsNo = createButton(settings_cp5, name, text, _x, _y, _w, _h);
        clearAllSettingsNo.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                toggleVisibility();
                //Do nothing because the user clicked Are You Sure?->No
                clearAllSettingsPressed = false;
                //Shorten height of this box
                h -= margin*4 + b_h*3;
            }
        });
    }

    private void createClearSettingsYesButton(String name, String text, int _x, int _y, int _w, int _h) {
        clearAllSettingsYes = createButton(settings_cp5, name, text, _x, _y, _w, _h);
        clearAllSettingsYes.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                toggleVisibility();
                //Shorten height of this box
                h -= margin*4 + b_h*3;
                //User has selected Are You Sure?->Yes
                settings.clearAll();
                clearAllSettingsPressed = false;
                //Stop the system if the user clears all settings
                if (systemMode == SYSTEMMODE_POSTINIT) {
                    haltSystem();
                }
            }
        });
        clearAllSettingsYes.setDescription("Clicking 'Yes' will delete all user settings and stop the session if running.");
    }

    public void toggleExpertModeFrontEnd(boolean b) {
        if (b) {
            expertMode.getCaptionLabel().setText("Turn Expert Mode Off");
            expertMode.setColorBackground(BUTTON_EXPERTPURPLE);
        } else {
            expertMode.getCaptionLabel().setText("Turn Expert Mode On");
            expertMode.setColorBackground(BUTTON_NOOBGREEN);
        }
    }
}