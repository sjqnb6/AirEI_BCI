package W_TimeSeries_;

import ADS1299SettingsBoard_.ADS1299SettingsBoard;
import ADS1299SettingsController_.ADS1299SettingsController;
import AccelerometerCapableBoard_.AccelerometerCapableBoard;
import AnalogCapableBoard_.AnalogCapableBoard;
import FileBoard_.FileBoard;
import GUI.ColorPalette;
import PopupMessage_.PopupMessage;
import Widget_.ChannelSelect;
import Widget_.Widget;
import controlP5.*;
import processing.core.PApplet;
import processing.core.PImage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import Globel.GUI;

import static Globel.GUI.navHeight;
import static Globel.GUI.nchan;
import static WidgetManager_.GVI.*;
import static WidgetManager_.GVI.w_analogRead;

public class W_timeSeries extends Widget {
    //to see all core variables/methods of the Widget class, refer to Widget.pde
    //put your custom variables here...
    GUI MAIN;

    public ColorPalette CP;


    private int numChannelBars;
    private float xF, yF, wF, hF;
    private float ts_padding;
    private float ts_x, ts_y, ts_h, ts_w; //values for actual time series chart -- rectangle encompassing all channelBars
    private float pb_x, pb_y, pb_h, pb_w; //values for playback sub-widget
    private float plotBottomWell;
    private float playbackWidgetHeight;
    private int channelBarHeight;
    public final int interChannelBarSpace = 2;

    private ControlP5 tscp5;
    private Button hwSettingsButton;

    public ChannelSelect tsChanSelect;
    private ChannelBar[] channelBars;
    private PlaybackScrollbar scrollbar;
    private TimeDisplay timeDisplay;

    TimeSeriesXLim xLimit = TimeSeriesXLim.FIVE;
    TimeSeriesYLim yLimit = TimeSeriesYLim.UV_200;

    private PImage expand_default;
    private PImage expand_hover;
    private PImage expand_active;
    private PImage contract_default;
    private PImage contract_hover;
    private PImage contract_active;

    public ADS1299SettingsController adsSettingsController;

    private boolean allowSpillover = false;
    private boolean hasScrollbar = true; //used to turn playback scrollbar widget on/off

    List<Controller> cp5ElementsToCheck = new ArrayList<Controller>();

    public W_timeSeries(GUI MAIN) {
        super(MAIN); //calls the parent CONSTRUCTOR method of Widget (DON'T REMOVE)
        this.MAIN = MAIN;
        CP = new ColorPalette(MAIN);


        tscp5 = new ControlP5(MAIN);
        tscp5.setGraphics(MAIN, 0,0);
        tscp5.setAutoDraw(false);

        tsChanSelect = new ChannelSelect(pApplet, this, x, y, w, navH, "TS_Channel");
        //activate all channels in channelSelect by default for this widget
        tsChanSelect.activateAllButtons();
        cp5ElementsToCheck.addAll(tsChanSelect.getCp5ElementsForOverlapCheck());

        xF = (float)x; //float(int( ... is a shortcut for rounding the float down... so that it doesn't creep into the 1px margin
        yF = (float)y;
        wF = (float)w;
        hF = (float)h;

        plotBottomWell = 45.0F; //this appears to be an arbitrary vertical space adds GPlot leaves at bottom, I derived it through trial and error
        ts_padding = 10.0F;
        ts_x = xF + ts_padding;
        ts_y = yF + ts_padding;
        ts_w = wF - ts_padding*2;
        ts_h = hF - playbackWidgetHeight - plotBottomWell - (ts_padding*2);
        numChannelBars = nchan; //set number of channel bars = to current nchan of system (4, 8, or 16)

        //This is a newer protocol for setting up dropdowns.
        addDropdown("VertScale_TS", "垂直刻度", yLimit.getEnumStringsAsList(), yLimit.getIndex());
        addDropdown("Duration", "窗口", xLimit.getEnumStringsAsList(), xLimit.getIndex());

        //Instantiate scrollbar if using playback mode and scrollbar feature in use
        if((MAIN.currentBoard instanceof FileBoard) && hasScrollbar) {
            playbackWidgetHeight = 50.0F;
            pb_x = ts_x - ts_padding/2;
            pb_y = ts_y + ts_h + playbackWidgetHeight + (ts_padding * 3);
            pb_w = ts_w - ts_padding*4;
            pb_h = playbackWidgetHeight/2;
            int _x = MAIN.floor(xF) - 1;
            int _y = (int)(ts_y + ts_h + playbackWidgetHeight + 5);
            int _w = (int)(wF) + 1;
            int _h = (int)(playbackWidgetHeight);
            //Make a new scrollbar
            scrollbar = new PlaybackScrollbar(MAIN, _x, _y, _w, _h, (int)(pb_x), (int)(pb_y), (int)(pb_w), (int)(pb_h));
        } else {
            int td_h = 18;
            timeDisplay = new TimeDisplay(MAIN, (int)(ts_x), (int)(ts_y + hF - td_h), (int)(ts_w), td_h);
            playbackWidgetHeight = 0.0F;
        }

        expand_default = pApplet.loadImage("expand_default.png");
        expand_hover = pApplet.loadImage("expand_hover.png");
        expand_active = pApplet.loadImage("expand_active.png");
        contract_default = pApplet.loadImage("contract_default.png");
        contract_hover = pApplet.loadImage("contract_hover.png");
        contract_active = pApplet.loadImage("contract_active.png");

        channelBarHeight = (int)(ts_h/numChannelBars);
        channelBars = new ChannelBar[numChannelBars];
        //create our channel bars and populate our channelBars array!
        for(int i = 0; i < numChannelBars; i++) {
            int channelBarY = (int)(ts_y) + i*(channelBarHeight); //iterate through bar locations
            ChannelBar tempBar = new ChannelBar(MAIN, i, (int)(ts_x), channelBarY, (int)(ts_w), channelBarHeight, expand_default, expand_hover, expand_active, contract_default, contract_hover, contract_active);
            channelBars[i] = tempBar;
        }

        int x_hsc = (int)(channelBars[0].plot.getPos()[0] + 2);
        int y_hsc = (int)(channelBars[0].plot.getPos()[1]);
        int w_hsc = (int)(channelBars[0].plot.getOuterDim()[0]);
        int h_hsc = channelBarHeight * numChannelBars;

        if (MAIN.currentBoard instanceof ADS1299SettingsBoard) {
            hwSettingsButton = createHSCButton("HardwareSettings", "硬件设置", (int)(x0 + 80), (int)(y0 + navHeight + 1), 120, navHeight - 3);
            cp5ElementsToCheck.add((Controller)hwSettingsButton);
            adsSettingsController = new ADS1299SettingsController(MAIN, tsChanSelect.activeChan, x_hsc, y_hsc, w_hsc, h_hsc, channelBarHeight);
        }
    }

    public void update() {
        super.update(); //calls the parent update() method of Widget (DON'T REMOVE)

        // offset based on whether channel select or hardware settings are open or not
        int chanSelectOffset = tsChanSelect.isVisible() ? navHeight : 0;
        if (MAIN.currentBoard instanceof ADS1299SettingsBoard) {
            chanSelectOffset += adsSettingsController.getIsVisible() ? navHeight : 0;
        }

        //Responsively size the channelBarHeight
        channelBarHeight = (int)((ts_h - chanSelectOffset) / tsChanSelect.activeChan.size());

        //Update channel checkboxes
        tsChanSelect.update(x, y, w);

        //Update and resize all active channels
        for(int i = 0; i < tsChanSelect.activeChan.size(); i++) {
            int activeChan = tsChanSelect.activeChan.get(i);
            int channelBarY = (int)(ts_y + chanSelectOffset) + i*(channelBarHeight); //iterate through bar locations
            //To make room for channel bar separator, subtract space between channel bars from height
            int cb_h = channelBarHeight - interChannelBarSpace;
            channelBars[activeChan].resize((int)(ts_x), channelBarY, (int)(ts_w), cb_h);
            channelBars[activeChan].update();
        }

        //Responsively size and update the HardwareSettingsController
        if (MAIN.currentBoard instanceof ADS1299SettingsBoard) {
            int cb_h = channelBarHeight + interChannelBarSpace - 2;
            int h_hsc = channelBarHeight * tsChanSelect.activeChan.size();
            adsSettingsController.resize((int)channelBars[0].plot.getPos()[0], (int)channelBars[0].plot.getPos()[1], (int)channelBars[0].plot.getOuterDim()[0], h_hsc, cb_h);
            adsSettingsController.update(); //update channel controller
        }

        //Update Playback scrollbar and/or display time
        if((MAIN.currentBoard instanceof FileBoard) && hasScrollbar) {
            //scrub playback file
            scrollbar.update();
        } else {
            timeDisplay.update();
        }

        lockElementsOnOverlapCheck(cp5ElementsToCheck);
    }

    public void draw() {
        super.draw(); //calls the parent draw() method of Widget (DON'T REMOVE)

        //remember to refer to x,y,w,h which are the positioning variables of the Widget class
        //draw channel bars
        for (int i = 0; i < tsChanSelect.activeChan.size(); i++) {
            int activeChan = tsChanSelect.activeChan.get(i);
            channelBars[activeChan].draw(getAdsSettingsVisible());
        }

        //Display playback scrollbar, timeDisplay, or ADSSettingsController depending on data source
        if ((MAIN.currentBoard instanceof FileBoard) && hasScrollbar) { //you will only ever see the playback widget in Playback Mode ... otherwise not visible
            scrollbar.draw();
        } else if (MAIN.currentBoard instanceof ADS1299SettingsBoard) {
            //Hide time display when ADSSettingsController is open for compatible boards
            if (!getAdsSettingsVisible()) {
                timeDisplay.draw();
            }
            adsSettingsController.draw();
        } else {
            timeDisplay.draw();
        }

        tscp5.draw();

        tsChanSelect.draw();
    }

    public void screenResized() {
        super.screenResized(); //calls the parent screenResized() method of Widget (DON'T REMOVE)

        //Very important to allow users to interact with objects after app resize
        tscp5.setGraphics(pApplet, 0,0);

        tsChanSelect.screenResized(pApplet);

        xF = (float)x; //float(int( ... is a shortcut for rounding the float down... so that it doesn't creep into the 1px margin
        yF = (float)y;
        wF = (float)w;
        hF = (float)h;

        ts_x = xF + ts_padding;
        ts_y = yF + (ts_padding);
        ts_w = wF - ts_padding*2;
        ts_h = hF - playbackWidgetHeight - plotBottomWell - (ts_padding*2);

        ////Resize the playback slider if using playback mode, or resize timeDisplay div at the bottom of timeSeries
        if((MAIN.currentBoard instanceof FileBoard) && hasScrollbar) {
            int _x = MAIN.floor(xF) - 1;
            int _y = (int)(ts_y + ts_h + playbackWidgetHeight + 5);
            int _w = (int)(wF) + 1;
            int _h = (int)(playbackWidgetHeight);
            pb_x = ts_x - ts_padding/2;
            pb_y = ts_y + ts_h + playbackWidgetHeight + (ts_padding*3);
            pb_w = ts_w - ts_padding*4;
            pb_h = playbackWidgetHeight/2;
            scrollbar.screenResized(_x, _y, _w, _h, pb_x, pb_y, pb_w, pb_h);
        } else {
            int td_h = 18;
            timeDisplay.screenResized((int)(ts_x), (int)(ts_y + hF - td_h), (int)(ts_w), td_h);
        }

        // offset based on whether channel select is open or not.
        int chanSelectOffset = 0;
        if (tsChanSelect.isVisible()) {
            chanSelectOffset = navHeight;
        }

        for (ChannelBar cb : channelBars) {
            cb.updateCP5(pApplet);
        }

        for(int i = 0; i < tsChanSelect.activeChan.size(); i++) {
            int activeChan = tsChanSelect.activeChan.get(i);
            int channelBarY = (int)(ts_y + chanSelectOffset) + i*(channelBarHeight); //iterate through bar locations
            channelBars[activeChan].resize((int)(ts_x), channelBarY, (int)(ts_w), channelBarHeight); //bar x, bar y, bar w, bar h
        }

        if (MAIN.currentBoard instanceof ADS1299SettingsBoard) {
            hwSettingsButton.setPosition(x0 + 80, (int)(y0 + navHeight + 1));
        }

    }

    public void mousePressed() {
        super.mousePressed(); //calls the parent mousePressed() method of Widget (DON'T REMOVE)
        tsChanSelect.mousePressed(this.dropdownIsActive); //Calls channel select mousePressed and checks if clicked

        for(int i = 0; i < tsChanSelect.activeChan.size(); i++) {
            int activeChan = tsChanSelect.activeChan.get(i);
            channelBars[activeChan].mousePressed();
        }
    }

    public void mouseReleased() {
        super.mouseReleased(); //calls the parent mouseReleased() method of Widget (DON'T REMOVE)

        for(int i = 0; i < tsChanSelect.activeChan.size(); i++) {
            int activeChan = tsChanSelect.activeChan.get(i);
            channelBars[activeChan].mouseReleased();
        }
    }

    private void setAdsSettingsVisible(boolean visible) {
        if(!(MAIN.currentBoard instanceof ADS1299SettingsBoard)) {
            return;
        }

        String buttonText = "时间序列";

        if (visible && MAIN.currentBoard.isStreaming()) {
            PopupMessage msg = new PopupMessage(MAIN, "提示", "在进入硬件设置之前，必须先停止流媒体播放");
            return;
        }

        boolean inSync = adsSettingsController.setIsVisible(visible);

        if (!visible && adsSettingsController != null && inSync) {
            buttonText = "硬件设置";
        }
        hwSettingsButton.setCaptionLabel(buttonText);
    }

    public boolean getAdsSettingsVisible() {
        return adsSettingsController != null && adsSettingsController.getIsVisible();
    }

    public void closeADSSettings() {
        setAdsSettingsVisible(false);
    }

    private Button createHSCButton(String name, String text, int _x, int _y, int _w, int _h) {
        final Button myButton = MAIN.createButton(tscp5, name, text, _x, _y, _w, _h);
        myButton.setBorderColor(CP.OBJECT_BORDER_GREY);
        myButton.onClick(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                MAIN.println("HardwareSettings Toggle: " + !adsSettingsController.getIsVisible());
                setAdsSettingsVisible(!adsSettingsController.getIsVisible());
            }
        });
        return myButton;
    }

    public TimeSeriesYLim getTSVertScale() {
        return yLimit;
    }

    public TimeSeriesXLim getTSHorizScale() {
        return xLimit;
    }

    public void setTSVertScale(int n) {
        yLimit = yLimit.values()[n];
        for (int i = 0; i < numChannelBars; i++) {
            channelBars[i].adjustVertScale(yLimit.getValue());
        }
    }

    public void setTSHorizScale(int n) {
        xLimit = xLimit.values()[n];
        for (int i = 0; i < numChannelBars; i++) {
            channelBars[i].adjustTimeAxis(xLimit.getValue());
        }
    }
    void VertScale_TS(int n) {
        w_timeSeries.setTSVertScale(n);
    }

    //triggered when there is an event in the Duration Dropdown
    void Duration(int n) {
        w_timeSeries.setTSHorizScale(n);

        int newDuration = w_timeSeries.getTSHorizScale().getValue();
        //If selected by user, sync the duration of Time Series, Accelerometer, and Analog Read(Cyton Only)
        if (MAIN.currentBoard instanceof AccelerometerCapableBoard) {
            if (MAIN.settings.accHorizScaleSave == 0) {
                //set accelerometer x axis to the duration selected from dropdown
                w_accelerometer.accelerometerBar.adjustTimeAxis(newDuration);
            }
        }
        if (MAIN.currentBoard instanceof AnalogCapableBoard) {
            if (MAIN.settings.arHorizScaleSave == 0) {
                //set analog read x axis to the duration selected from dropdown
                for(int i = 0; i < w_analogRead.numAnalogReadBars; i++) {
                    w_analogRead.analogReadBars[i].adjustTimeAxis(newDuration);
                }
            }
        }
    }
};