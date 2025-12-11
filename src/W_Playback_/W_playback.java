package W_Playback_;

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//    W_playback.pde (ie "Playback History")
//
//    Allow user to load playback files from within GUI without having to restart the system
//                       Created: Richard Waltman - August 2018
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

import CustomCp5Classes_.MenuList;
import DataProcessing_.DataProcessing;
import GUI.ColorPalette;
import Widget_.Widget;
import controlP5.*;
import processing.core.PApplet;
import processing.core.PFont;
import processing.data.JSONArray;
import processing.data.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static Debugging_.GF.output;
import static Extras_.GF.shortenString;
import static Globel.GUI.*;
import static W_Playback_.GF.userSelectedPlaybackMenuList;
import Globel.GUI;
public class W_playback extends Widget {
    GUI MAIN;
    protected PApplet pApplet;

    public ColorPalette CP;

    //allow access to dataProcessing
    DataProcessing dataProcessing;
    //Set up variables for Playback widget
    ControlP5 cp5_playback;
    Button selectPlaybackFileButton;
    MenuList playbackMenuList;
    //Used for spacing
    int padding = 10;
    List<Controller> cp5ElementsToCheck = new ArrayList<Controller>();

    private boolean menuHasUpdated = false;

    public W_playback(GUI MAIN) {
        super(MAIN); //calls the parent CONSTRUCTOR method of Widget (DON'T REMOVE)

        CP = new ColorPalette(MAIN);

        pApplet = MAIN;

        cp5_playback = new ControlP5(pApplet);
        cp5_playback.setGraphics(pApplet, 0,0);
        cp5_playback.setAutoDraw(false);

        int initialWidth = w - padding*2;
        createPlaybackMenuList(cp5_playback, "playbackMenuList", x + padding/2, y + 2, initialWidth, h - padding*2, p3);
        createSelectPlaybackFileButton("selectPlaybackFile_Session", "Select Playback File", x + w/2 - (padding*2), y - navHeight + 2, 200, navHeight - 6);
    }

    public void update() {
        super.update(); //calls the parent update() method of Widget (DON'T REMOVE)
        if (!menuHasUpdated) {
            refreshPlaybackList();
            menuHasUpdated = true;
        }
        //Lock the MenuList if Widget selector is open, otherwise update
        if (cp5_widget.get(ScrollableList.class, "WidgetSelector").isOpen() || topNav.getDropdownMenuIsOpen()) {
            if (!playbackMenuList.isLock()) {
                playbackMenuList.lock();
                playbackMenuList.setUpdate(false);
            }
        } else {
            if (playbackMenuList.isLock()) {
                playbackMenuList.unlock();
                playbackMenuList.setUpdate(true);
            }
            playbackMenuList.updateMenu();
        }
        lockElementsOnOverlapCheck(cp5ElementsToCheck);
    }

    public void draw() {
        super.draw(); //calls the parent draw() method of Widget (DON'T REMOVE)

        //x,y,w,h are the positioning variables of the Widget class
        pApplet.pushStyle();
        pApplet.fill(CP.boxColor);
        pApplet.stroke(CP.boxStrokeColor);
        pApplet.strokeWeight(1);
        pApplet.rect(x-1, y, w+1, h);
        //Add text if needed
        /*
        fill(OPENBCI_DARKBLUE);
        textFont(h3, 16);
        textAlign(LEFT, TOP);
        text("PLAYBACK FILE", x + padding, y + padding);
        */
        pApplet.popStyle();

        cp5_playback.draw();
    } //end draw loop

    public void screenResized() {
        super.screenResized(); //calls the parent screenResized() method of Widget (DON'T REMOVE)

        //**IMPORTANT FOR CP5**//
        //This makes the cp5 objects within the widget scale properly
        cp5_playback.setGraphics(pApplet, 0, 0);

        //Resize and position cp5 objects within this widget
        selectPlaybackFileButton.setPosition(x + w - selectPlaybackFileButton.getWidth() - 2, y - navHeight + 2);

        playbackMenuList.setPosition(x + padding/2, y + 2);
        playbackMenuList.setSize(w - padding*2, h - padding*2);
        refreshPlaybackList();
    }

    public void refreshPlaybackList() {

        File f = new File(userPlaybackHistoryFile);
        if (!f.exists()) {
            MAIN.println("OpenBCI_GUI::RefreshPlaybackList: Playback history file not found.");
            return;
        }

        try {
            playbackMenuList.items.clear();
            loadPlaybackHistoryJSON = pApplet.loadJSONObject(userPlaybackHistoryFile);
            JSONArray loadPlaybackHistoryJSONArray = loadPlaybackHistoryJSON.getJSONArray("playbackFileHistory");
            //println("Array Size:" + loadPlaybackHistoryJSONArray.size());
            int currentFileNameToDraw = 0;
            for (int i = loadPlaybackHistoryJSONArray.size() - 1; i >= 0; i--) { //go through array in reverse since using append
                JSONObject loadRecentPlaybackFile = loadPlaybackHistoryJSONArray.getJSONObject(i);
                int fileNumber = loadRecentPlaybackFile.getInt("recentFileNumber");
                String shortFileName = loadRecentPlaybackFile.getString("id");
                String longFilePath = loadRecentPlaybackFile.getString("filePath");

                int totalPadding = padding + playbackMenuList.padding;
                shortFileName = shortenString(MAIN, shortFileName, w-totalPadding*2.f, p4);
                //add as an item in the MenuList
                playbackMenuList.addItem(shortFileName, Integer.toString(fileNumber), longFilePath);
                currentFileNameToDraw++;
            }
            playbackMenuList.updateMenu();
        } catch (NullPointerException e) {
           MAIN.println("PlaybackWidget: Playback history file not found.");
        }
    }

    private void createSelectPlaybackFileButton(String name, String text, int _x, int _y, int _w, int _h) {
        selectPlaybackFileButton = MAIN.createButton(cp5_playback, name, text, _x, _y, _w, _h);
        selectPlaybackFileButton.setBorderColor(CP.OBJECT_BORDER_GREY);
        selectPlaybackFileButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                output("Select a file for playback");
                pApplet.selectInput("Select a pre-recorded file for playback:", "playbackSelectedWidgetButton");
            }
        });
        selectPlaybackFileButton.setDescription("Click to open a dialog box to select an OpenBCI playback file (.txt or .csv).");
        cp5ElementsToCheck.add((Controller)selectPlaybackFileButton);
    }

    private void createPlaybackMenuList(ControlP5 _cp5, String name, int _x, int _y, int _w, int _h, PFont font) {
        playbackMenuList = new MenuList(_cp5, name, _w, _h, font, MAIN);
        playbackMenuList.setPosition(_x, _y);
        playbackMenuList.addCallback(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                if (theEvent.getAction() == ControlP5.ACTION_BROADCAST) {
                    //Check to make sure value of clicked item is in valid range. Fixes #480
                    float valueOfItem = playbackMenuList.getValue();
                    if (valueOfItem < 0 || valueOfItem > (playbackMenuList.items.size() - 1) ) {
                        //println("CP: No such item " + value + " found in list.");
                    } else {
                        Map m = playbackMenuList.getItem((int)(valueOfItem));
                        //println("got a menu event from item " + value + " : " + m);
                        userSelectedPlaybackMenuList(pApplet, m.get("copy").toString(), (int)(valueOfItem));
                    }
                }
            }
        });
        playbackMenuList.scrollerLength = 40;
    }
}; //end Playback widget class