package CytonElectrodeStatus_;

import BoardCyton_.BoardCyton;
import Extras_.RectDimensions;
import GUI.GUIManager;
import Globel.GUI;
import Grid_.Grid;
import controlP5.Button;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlP5;
import gifAnimation.Gif;
import processing.core.PFont;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import static GUI.GGVI.data_elec_imp_ohm;
import static GUI.GGVI.is_railed;
import static WidgetManager_.GVI.w_cytonImpedance;

public class CytonElectrodeStatus {

    private CytonElectrodeLocations thisElectrode;

    protected BoardCyton cytonBoard;
    protected Integer channelNumber;
    protected String electrodeLocation;
    protected String measurement;
    protected int dataTableColumnOffset;
    protected double statusValue;
    protected String statusValueAsString;
    protected String anatomicalName;
    protected ElectrodeState state_live;
    protected ElectrodeState state_imp;
    protected NumberFormat railedNF = NumberFormat.getInstance();
    protected DecimalFormat impedanceNF;
    protected DecimalFormat impShortNF;
    //Impedance ranges in kOhms
    protected double impedanceGreenCutoff = 750d;
    protected double impedanceYellowCuttoff = 2500d;
    //Anything greater than impedanceYellowCuttoff is red
    private boolean isCheckingAnotherElectrode = false;
    protected boolean isInImpedanceMode = false;

    protected ControlP5 local_cp5;
    protected Button testing_button;
    protected RectDimensions cellDims;
    protected final int testingButtonPadding = 3;

    protected boolean is_N_Pin = false;

    protected Gif checkingElectrodeGif;
    protected final int gifDiameterBorderOffset = 30; //From the weight of the pixels in the original gif
    GUI MAIN;
    public CytonElectrodeStatus(GUI MAIN, ControlP5 _cp5, CytonElectrodeEnum electrodeEnum, BoardCyton _impBoard, Gif statusGif) {
        this.MAIN = MAIN;
        local_cp5 = _cp5;
        cytonBoard = (BoardCyton)_impBoard;
        impedanceNF = new DecimalFormat("###,###.#");
        impShortNF = new DecimalFormat("###,###");

        thisElectrode = (CytonElectrodeLocations)electrodeEnum;
        channelNumber = thisElectrode.getChanGUI();
        electrodeLocation = thisElectrode.getADSChan();
        measurement = thisElectrode.getMeasurementType();
        anatomicalName = thisElectrode.getLabelName();
        is_N_Pin = thisElectrode.isPin_N();
        railedNF.setMaximumFractionDigits(2);
        dataTableColumnOffset = is_N_Pin ? 1 : 2;
        checkingElectrodeGif = statusGif;

        state_imp = ElectrodeState.GREYED_OUT;
        state_live = ElectrodeState.GREYED_OUT;

        //This will be resized and positioned during session starts when widget is assigned a container
        createCytonElectrodeTestingButton("electrode_"+electrodeLocation, "Test", 0, 0, 20, 10);
    }

    public void draw(int w, int h) {

        float x = w * thisElectrode.getCircleXY()[0];
        float y = h * thisElectrode.getCircleXY()[1];

        ElectrodeState state = getElectrodeState();

        MAIN.pushStyle();
        MAIN.fill(state.getColor());
        float d = w * thisElectrode.getDiameterScalar();
        MAIN.ellipseMode(MAIN.CENTER);
        MAIN.ellipse(x, y, d, d);

        if (state != ElectrodeState.NOT_TESTABLE && cytonBoard.isCheckingImpedanceNorP(channelNumber-1, is_N_Pin)) {
            MAIN.imageMode(MAIN.CENTER);
            MAIN.image(checkingElectrodeGif, x - 1, y - 1, d + gifDiameterBorderOffset, d + gifDiameterBorderOffset);
        }
        MAIN.popStyle();
    }

    public void update(Grid _dataTable, boolean _isImpedanceMode) {

        isInImpedanceMode = _isImpedanceMode;
        ElectrodeState state = getElectrodeState();

        if (state == ElectrodeState.NOT_TESTABLE) {
            return;
        }

        int i = channelNumber - 1;

        if (_isImpedanceMode && cytonBoard.isCheckingImpedanceNorP(i, is_N_Pin) && cytonBoard.isStreaming()) {

            //update the impedance values
            statusValue = data_elec_imp_ohm[i]/1000; //value in kOhm
            boolean greaterThanZero = statusValue > Double.MIN_NORMAL;
            int railedTextColor = MAIN.OPENBCI_DARKBLUE;
            if (statusValue > impedanceYellowCuttoff) {
                state_imp = ElectrodeState.RED;
            } else if (statusValue < impedanceYellowCuttoff && statusValue > impedanceGreenCutoff) {
                state_imp = ElectrodeState.YELLOW;
            } else if (greaterThanZero && statusValue < impedanceGreenCutoff) {
                state_imp = ElectrodeState.GREEN;
            }
            //Impedance mode uses buttons carefully positioned in the table to display information
            testing_button.getCaptionLabel().setText(getImpValShortString());
            testing_button.setColorCaptionLabel(state.getColor());

        } else if (!_isImpedanceMode) {

            //update the railed percentage values
            statusValue = is_railed[i].getPercentage();
            boolean greaterThanZero = statusValue > Double.MIN_NORMAL;
            int railedTextColor = MAIN.OPENBCI_DARKBLUE;
            if (is_railed[i].is_railed) {
                state_live = ElectrodeState.RED;
                railedTextColor = MAIN.SIGNAL_CHECK_RED;
            } else if (is_railed[i].is_railed_warn) {
                state_live = ElectrodeState.YELLOW;
                railedTextColor = MAIN.SIGNAL_CHECK_YELLOW;
            } else if (greaterThanZero) {
                state_live = ElectrodeState.BLUE;
            }
            //Railed percentage mode (Live) uses text in the data table
            StringBuilder s = new StringBuilder(railedNF.format(statusValue));
            s.append(" %");
            _dataTable.setString(s.toString(), channelNumber, dataTableColumnOffset);
            _dataTable.setTextColor(railedTextColor, channelNumber, dataTableColumnOffset);

        }
    }

    public String getImpedanceValueAsString(boolean isAnatomicalName) {
        StringBuilder sb = new StringBuilder(isAnatomicalName ? anatomicalName : electrodeLocation);
        sb.append(" - ");
        sb.append(impedanceNF.format(statusValue));
        sb.append(" kOhm");
        return sb.toString();
    }

    public String getImpValShortString() {
        StringBuilder sb = new StringBuilder(impShortNF.format(statusValue));
        sb.append(" k\u2126");
        return sb.toString();
    }

    public Integer getGUIChannelNumber() {
        return channelNumber;
    }

    public final ElectrodeState getElectrodeState() {
        return isInImpedanceMode ? state_imp : state_live;
    }

    public void setElectrodeState(ElectrodeState s) {
        if (isInImpedanceMode) {
            state_imp = s;
        } else {
            state_live = s;
        }
    }

    public boolean getIsNPin() {
        return is_N_Pin;
    }

    public void overrideTestingButtonSwitch(boolean b) {
        if (b) {
            testing_button.setOn();
        } else {
            testing_button.setOff();
        }
    }

    public void updateGreenThreshold(double _d) {
        impedanceGreenCutoff = _d;
    }

    public void updateYellowThreshold(double _d) {
        impedanceYellowCuttoff = _d;
    }

    //Here is the method that creates a "Test" button for every electrode position
    protected void createCytonElectrodeTestingButton(String name, String text, int _x, int _y, int _w, int _h) {
        ElectrodeState state = getElectrodeState();
        if (state == ElectrodeState.NOT_TESTABLE) {
            return; //Some electrode positions cannot be tested
        }
        testing_button = MAIN.createButton(local_cp5, name, text, _x, _y, _w, _h);
        testing_button.setBorderColor(null);
        testing_button.setColorActive(MAIN.BUTTON_PRESSED_LIGHT);
        testing_button.setColorForeground(MAIN.BUTTON_HOVER_LIGHT);
        testing_button.setSwitch(true); //This turns the button into a switch. Switch will be Off by default.
        testing_button.onPress(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                final int _chan = channelNumber - 1;
                final int curMillis = MAIN.millis();
                MAIN.println("CytonElectrodeTestButton: Toggling Impedance on ~~ " + electrodeLocation);
                w_cytonImpedance.toggleImpedanceOnElectrode(!cytonBoard.isCheckingImpedanceNorP(_chan, is_N_Pin), _chan, is_N_Pin, curMillis);
            }
        });
        testing_button.setDescription("Click to toggle impedance check for this ADS pin.");
    }

    public void resizeButton(Grid _dataTable) {
        ElectrodeState state = getElectrodeState();
        if (state == ElectrodeState.NOT_TESTABLE) {
            return; //Some electrode positions cannot be tested
        }
        cellDims = _dataTable.getCellDims(channelNumber, dataTableColumnOffset);
        testing_button.setPosition(cellDims.x, cellDims.y + 1);
        testing_button.setSize(cellDims.w + 1, cellDims.h - 1);
    }

    //Override the electrode state
    public void setElectrodeGreyedOut() {
        ElectrodeState state = getElectrodeState();
        if (state == ElectrodeState.NOT_TESTABLE) {
            return;
        }
        state = ElectrodeState.GREYED_OUT;
    }

    //Override the electrode state
    public void setElectrodeGreenStatus() {
        ElectrodeState state = getElectrodeState();
        if (state == ElectrodeState.NOT_TESTABLE) {
            return;
        }
        state = ElectrodeState.GREEN;
    }

    public void resetTestingButton() {
        testing_button.getCaptionLabel().setText("Test");
        testing_button.setOff();
    }

    public void setLockTestingButton(boolean b) {
        if (testing_button != null) {
            testing_button.setLock(b);
        }
    }

    public Button getTestingButton() {
        return testing_button;
    }

    public void drawLabels(boolean _showAnatomicalName, int container_x, int container_y, int w, int h, PFont _font) {
        MAIN.pushStyle();
        MAIN.fill(MAIN.OPENBCI_DARKBLUE);
        MAIN.textAlign(MAIN.CENTER);
        MAIN.textFont(_font);
        float x = w * thisElectrode.getLabelXY()[0];
        float y = h * thisElectrode.getLabelXY()[1];
        String s = _showAnatomicalName ? thisElectrode.getLabelName() : thisElectrode.getADSChan();
        MAIN.text(s, container_x + x, container_y + y);
        MAIN.popStyle();
    }

    public String getThisElectrodeLabel() {
        return thisElectrode.getLabelName();
    }
}