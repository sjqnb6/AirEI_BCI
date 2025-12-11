package W_Marker_;

//////////////////////////////////////////////////////
//                                                  //
//                  W_Marker.pde                    //
//                                                  //
//    Created by: Richard Waltman, August 2023      //
//    Purpose: Add software markers to data         //
//    Marker Shortcuts: z x c v Z X C V             //
//                                                  //

import BoardBrainflow_.BoardBrainFlow;
import Board_.Board;
import DataSource_.DataSource;
import Extras_.RectDimensions;
import Grid_.Grid;
import Widget_.Widget;
import controlP5.*;
import hypermedia.net.UDP;
import processing.core.PApplet;

import java.util.ArrayList;
import java.util.List;

import static Debugging_.GF.output;
import static Debugging_.GF.outputSuccess;
import static Extras_.GF.dropNonPrintableChars;
import static Extras_.GF.getIpAddrFromStr;
import static Globel.GUI.*;

import Globel.GUI;
//////////////////////////////////////////////////////

public class W_Marker extends Widget {
    GUI MAIN;
    private ControlP5 localCP5;
    private List<Controller> cp5ElementsToCheckForOverlap;

    private final int MARKER_BUTTON_WIDTH = 125;
    private final int MARKER_BUTTON_HEIGHT = 20;
    private final int MARKER_UI_GRID_CELL_HEIGHT = 30;
    private final int MAX_NUMBER_OF_MARKER_BUTTONS = 8;
    private final int MARKER_UI_GRID_EXTERIOR_PADDING = 10;
    private final int MARKER_UI_GRID_ROWS = 4;
    private final int MARKER_UI_GRID_COLUMNS = 4;
    private Button[] markerButtons = new Button[MAX_NUMBER_OF_MARKER_BUTTONS];
    private Grid markerUIGrid;

    private Textfield markerReceiveIPTextfield;
    private Textfield markerReceivePortTextfield;
    private String markerReceiveIP = "127.0.0.1";
    private int markerReceivePort = 12350;
    private final int MARKER_RECEIVE_TEXTFIELD_WIDTH = 108;
    private final int MARKER_RECEIVE_TEXTFIELD_HEIGHT = 22;

    private UDP udpReceiver;

    private MarkerBar markerBar;
    private int graphX, graphY, graphW, graphH;
    private int PAD_FIVE = 5;
    private int GRAPH_PADDING = 30;

    private MarkerVertScale markerVertScale = MarkerVertScale.EIGHT;
    private MarkerWindow markerWindow = MarkerWindow.FIVE;

    public W_Marker(GUI MAIN){
        super(MAIN); //calls the parent CONSTRUCTOR method of Widget (DON'T REMOVE)
        this.MAIN = MAIN;
        //Instantiate local cp5 for this box. This allows extra control of drawing cp5 elements specifically inside this class.
        localCP5 = new ControlP5(MAIN);
        localCP5.setGraphics(MAIN, 0,0);
        localCP5.setAutoDraw(false);

        createMarkerButtons();

        updateGraphDims();
        addDropdown("markerVertScaleDropdown", "Vert Scale", markerVertScale.getEnumStringsAsList(), markerVertScale.getIndex());
        addDropdown("markerWindowDropdown", "Window", markerWindow.getEnumStringsAsList(), markerWindow.getIndex());
        markerBar = new MarkerBar(MAIN, MAX_NUMBER_OF_MARKER_BUTTONS, markerWindow.getValue(), markerVertScale.getValue(), graphX, graphY, graphW, graphH);

        markerUIGrid = new Grid(MAIN, MARKER_UI_GRID_ROWS, MARKER_UI_GRID_COLUMNS, MARKER_UI_GRID_CELL_HEIGHT);
        markerUIGrid.setDrawTableBorder(false);
        markerUIGrid.setDrawTableInnerLines(false);
        markerUIGrid.setTableFontAndSize(p4, 14);
        markerUIGrid.setString("Receive IP", 3, 0);
        markerUIGrid.setString("Receive Port", 3, 2);

        createMarkerReceiveTextfields();

        initUdpMarkerReceiver();

        //Add all cp5 elements to a list so that they can be checked for overlap
        cp5ElementsToCheckForOverlap = new ArrayList<Controller>();
        for (int i = 0; i < MAX_NUMBER_OF_MARKER_BUTTONS; i++) {
            cp5ElementsToCheckForOverlap.add(markerButtons[i]);
        }
        cp5ElementsToCheckForOverlap.add(markerReceiveIPTextfield);
        cp5ElementsToCheckForOverlap.add(markerReceivePortTextfield);
    }

    public void update(){
        super.update(); //calls the parent update() method of Widget (DON'T REMOVE)

        copyPaste.checkForCopyPaste(markerReceiveIPTextfield);
        copyPaste.checkForCopyPaste(markerReceivePortTextfield);

        lockElementsOnOverlapCheck(cp5ElementsToCheckForOverlap);

        if (MAIN.currentBoard.isStreaming()) {
            markerBar.update();
        }

    }

    public void draw(){
        super.draw(); //calls the parent draw() method of Widget (DON'T REMOVE)

        markerUIGrid.draw();
        markerBar.draw();

        //This draws all cp5 objects in the local instance
        localCP5.draw();
    }

    public void screenResized(){
        super.screenResized(); //calls the parent screenResized() method of Widget (DON'T REMOVE)

        //Very important to allow users to interact with objects after app resize
        localCP5.setGraphics(pApplet, 0, 0);

        resizeMarkerUIGrid();

        updateGraphDims();
        markerBar.screenResized(graphX, graphY, graphW, graphH);
    }

    private void updateGraphDims() {
        graphW = (int)(w - PAD_FIVE*4);
        graphH = (int)(h/2 - GRAPH_PADDING - PAD_FIVE*2);
        graphX = x + PAD_FIVE*2;
        graphY = y + h - graphH - (int)(GRAPH_PADDING) - PAD_FIVE*2;
    }

    private void resizeMarkerUIGrid() {
        int tableX = x + GRAPH_PADDING;
        int tableY = y + MARKER_UI_GRID_EXTERIOR_PADDING;
        int tableW = w - GRAPH_PADDING * 2;
        int tableH = y - graphY - GRAPH_PADDING * 2;
        markerUIGrid.setDim(tableX, tableY, tableW);
        markerUIGrid.setRowHeight(MARKER_UI_GRID_CELL_HEIGHT);
        markerUIGrid.dynamicallySetTextVerticalPadding(3, 0);
        markerUIGrid.dynamicallySetTextVerticalPadding(3, 2);
        markerUIGrid.setHorizontalCenterTextInCells(true);

        final int CELL_PADDING = 8;
        final int CELL_PADDING_TOTAL = CELL_PADDING * 2;
        final int HALF_CELL_PADDING = CELL_PADDING / 2;

        //Update positions of marker buttons
        for (int i = 0; i < MAX_NUMBER_OF_MARKER_BUTTONS; i++) {
            int row = i < MARKER_UI_GRID_COLUMNS ? 0 : 1;
            int column = i % (MARKER_UI_GRID_COLUMNS);
            RectDimensions cellDims = markerUIGrid.getCellDims(row, column);
            markerButtons[i].setPosition(cellDims.x + CELL_PADDING, cellDims.y + HALF_CELL_PADDING);
            markerButtons[i].setSize(cellDims.w - CELL_PADDING_TOTAL, cellDims.h - CELL_PADDING);
        }

        RectDimensions ipTextfieldPosition = markerUIGrid.getCellDims(3, 1);
        markerReceiveIPTextfield.setPosition(ipTextfieldPosition.x, ipTextfieldPosition.y + HALF_CELL_PADDING);

        RectDimensions portTextfieldPosition = markerUIGrid.getCellDims(3, 3);
        markerReceivePortTextfield.setPosition(portTextfieldPosition.x, portTextfieldPosition.y + HALF_CELL_PADDING);
    }

    private void createMarkerButtons() {
        for (int i = 0; i < MAX_NUMBER_OF_MARKER_BUTTONS; i++) {
            //Create marker buttons
            //Marker number is i + 1 because marker numbers start at 1, not 0. Otherwise, will throw BrainFlow error.
            //This initial position is temporary and will be updated in resizeMarkerUIGrid()
            markerButtons[i] = createMarkerButton(i + 1, x + 10 + (i * MARKER_BUTTON_WIDTH), y + 10);
        }
    }

    private Button createMarkerButton(final int markerNumber, int _x, int _y) {
        Button newButton = MAIN.createButton(localCP5, "markerButton" + markerNumber, "Insert " + markerNumber, _x, _y, MARKER_BUTTON_WIDTH, MARKER_BUTTON_HEIGHT, p5, 12, MAIN.colorNotPressed, MAIN.OPENBCI_DARKBLUE);
        newButton.setBorderColor(MAIN.OBJECT_BORDER_GREY);
        newButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                insertMarker(markerNumber);
            }
        });
        newButton.setDescription("Click to insert marker " + markerNumber + " into the data stream.");
        return newButton;
    }

    //Called in Interactivity.pde when a key is pressed
    //Returns true if a marker key was pressed, false otherwise
    //Can be used to check for marker key presses even when this widget is not active
    public boolean checkForMarkerKeyPress(char keyPress) {
        switch (keyPress) {
            case 'z':
                insertMarker(1);
                return true;
            case 'x':
                insertMarker(2);
                return true;
            case 'c':
                insertMarker(3);
                return true;
            case 'v':
                insertMarker(4);
                return true;
            case 'Z':
                insertMarker(5);
                return true;
            case 'X':
                insertMarker(6);
                return true;
            case 'C':
                insertMarker(7);
                return true;
            case 'V':
                insertMarker(8);
                return true;
            default:
                return false;
        }
    }

    private void createMarkerReceiveTextfields() {
        markerReceiveIPTextfield = createTextfield("markerReceiveIPTextfield", markerReceiveIP);
        markerReceivePortTextfield = createTextfield("markerReceivePortTextfield", Integer.toString(markerReceivePort));
    }

    /* Create textfields for network parameters */
    private Textfield createTextfield(String name, String default_text) {
        final Textfield myTextfield = localCP5.addTextfield(name).align(10, 100, 10, 100) // Alignment
                .setSize(MARKER_RECEIVE_TEXTFIELD_WIDTH, MARKER_RECEIVE_TEXTFIELD_HEIGHT) // Size of textfield
                .setFont(f2)
                .setFocus(false) // Deselects textfield
                .setColor(MAIN.OPENBCI_DARKBLUE)
                .setColorBackground(MAIN.color(255, 255, 255)) // text field bg color
                .setColorValueLabel(MAIN.OPENBCI_DARKBLUE) // text color
                .setColorForeground(MAIN.OPENBCI_DARKBLUE) // border color when not selected
                .setColorActive(MAIN.isSelected_color) // border color when selected
                .setColorCursor(MAIN.OPENBCI_DARKBLUE)
                .setText(default_text) // Default text in the field
                .setCaptionLabel("") // Remove caption label
                .setVisible(true) // Initially visible
                .setAutoClear(false) // Don't clear textfield when pressing Enter key
                ;
        //Clear textfield on double click
        myTextfield.onDoublePress(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                output("Marker Widget: Enter your Marker Receiver IP Address or Port");
                myTextfield.clear();
            }
        });
        //Autogenerate if user presses Enter key and textfield value is null
        myTextfield.addCallback(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                if (theEvent.getAction() == ControlP5.ACTION_BROADCAST && myTextfield.getText().equals("")) {
                    resetMarkerReceiveTextfield(myTextfield);
                    initUdpMarkerReceiver();
                }
            }
        });
        //Autogenerate name if user leaves textfield and value is null
        myTextfield.onReleaseOutside(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                if (!myTextfield.isActive() && myTextfield.getText().equals("")) {
                    resetMarkerReceiveTextfield(myTextfield);
                    initUdpMarkerReceiver();
                }
            }
        });
        //Reinitialize UDP receiver if user presses Enter key and textfield value is not null
        myTextfield.addCallback(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                if (theEvent.getAction() == ControlP5.ACTION_BROADCAST && !myTextfield.getText().equals("")) {
                    initUdpMarkerReceiver();
                }
            }
        });
        return myTextfield;
    }

    private void resetMarkerReceiveTextfield(Textfield tf) {
        if (tf.getName().equals("markerReceiveIPTextfield")) {
            tf.setText(markerReceiveIP);
        } else if (tf.getName().equals("markerReceivePortTextfield")) {
            tf.setText(Integer.toString(markerReceivePort));
        }
    }

    private void initUdpMarkerReceiver() {
        markerReceiveIP = getIpAddrFromStr(markerReceiveIPTextfield.getText());
        markerReceivePort = Integer.parseInt(dropNonPrintableChars(markerReceivePortTextfield.getText()));
        if (udpReceiver != null) {
            udpReceiver.close();
        }
        udpReceiver = new UDP(pApplet, markerReceivePort, markerReceiveIP);
        udpReceiver.listen(true);
        udpReceiver.log(false);
        udpReceiver.setReceiveHandler("receiveMarkerViaUdp");
        outputSuccess("Marker Widget: Listening for markers on " + markerReceiveIP + ":" + markerReceivePort);
    }

    private void insertMarker(int markerNumber) {
        int markerChannel = ((DataSource)MAIN.currentBoard).getMarkerChannel();

        if (MAIN.currentBoard instanceof BoardBrainFlow) {
            if (markerChannel != -1) {
                ((Board)MAIN.currentBoard).insertMarker(markerNumber);
            }
        }
    }

    public void insertMarkerFromExternal(float markerValue) {
        int markerChannel = ((DataSource)MAIN.currentBoard).getMarkerChannel();

        if (MAIN.currentBoard instanceof BoardBrainFlow) {
            if (markerChannel != -1) {
                ((Board)MAIN.currentBoard).insertMarker(markerValue);
            }
        }
    }

    public void setMarkerWindow(int n) {
        markerWindow = markerWindow.values()[n];
        markerBar.adjustTimeAxis(markerWindow.getValue());
    }

    public void setMarkerVertScale(int n) {
        markerVertScale = markerVertScale.values()[n];
        markerBar.adjustYAxis(markerVertScale.getValue());
    }

    public MarkerWindow getMarkerWindow() {
        return markerWindow;
    }

    public MarkerVertScale getMarkerVertScale() {
        return markerVertScale;
    }

    public String getMarkerReceiveIP() {
        return getIpAddrFromStr(markerReceiveIPTextfield.getText());
    }

    public String getMarkerReceivePort() {
        return dropNonPrintableChars(markerReceivePortTextfield.getText());
    }

}; //end class W_Marker