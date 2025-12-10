package WidgetManager_;

import Globel.GUI;
import Widget_.Widget;
import processing.core.PApplet;

import java.util.ArrayList;

import static Containers_.GVI.container;
import static Debugging_.GF.verbosePrint;
import static GUI.GGVI.*;
import static WidgetManager_.GF.setupWidgets;
import static WidgetManager_.GVI.w_networking;
import static processing.core.PApplet.println;

public class WidgetManager{


    //this holds all of the widgets ... when creating/adding new widgets, we will add them to this ArrayList (below)
    public ArrayList<Widget> widgets;
    public ArrayList<String> widgetOptions; //List of Widget Titles, used to populate cp5 widgetSelector dropdown of all widgets

    //Variables for
    int currentContainerLayout; //this is the Layout structure for the main body of the GUI ... refer to [PUT_LINK_HERE] for layouts/numbers image
    ArrayList<Layout> layouts = new ArrayList<Layout>();  //this holds all of the different layouts ...

    public boolean isWMInitialized = false;
    private boolean visible = true;

    public WidgetManager(GUI _this){
        widgets = new ArrayList<Widget>();
        widgetOptions = new ArrayList<String>();
        isWMInitialized = false;

        //DO NOT re-order the functions below
        setupLayouts();
        setupWidgets(_this, widgets);
        setupWidgetSelectorDropdowns();

        if(nchan == 4 && eegDataSource == DATASOURCE_GANGLION) {
            currentContainerLayout = 1;
            settings.currentLayout = 1; // used for save/load settings
            setNewContainerLayout(currentContainerLayout); //sets and fills layout with widgets in order of widget index, to reorganize widget index, reorder the creation in setupWidgets()
        } else if (eegDataSource == DATASOURCE_PLAYBACKFILE) {
            currentContainerLayout = 1;
            settings.currentLayout = 1; // used for save/load settings
            setNewContainerLayout(currentContainerLayout); //sets and fills layout with widgets in order of widget index, to reorganize widget index, reorder the creation in setupWidgets()
        } else {
            currentContainerLayout = 4; //default layout ... tall container left and 2 shorter containers stacked on the right
            settings.currentLayout = 4; // used for save/load settings
            setNewContainerLayout(currentContainerLayout); //sets and fills layout with widgets in order of widget index, to reorganize widget index, reorder the creation in setupWidgets()
        }

        isWMInitialized = true;
    }
    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean _visible) {
        visible = _visible;
    }

    void setupWidgetSelectorDropdowns(){
        //create the widgetSelector dropdown of each widget
        //println("widgets.size() = " + widgets.size());
        //create list of WidgetTitles.. we will use this to populate the dropdown (widget selector) of each widget
        for(int i = 0; i < widgets.size(); i++){
            widgetOptions.add(widgets.get(i).widgetTitle);
        }
        //println("widgetOptions.size() = " + widgetOptions.size());
        for(int i = 0; i <widgetOptions.size(); i++){
            widgets.get(i).setupWidgetSelectorDropdown(widgetOptions);
            widgets.get(i).setupNavDropdowns();
        }
    }

    public void update(){
        // if(visible && updating){
        if(visible){
            for(int i = 0; i < widgets.size(); i++){
                if(widgets.get(i).getIsActive()){
                    widgets.get(i).update();
                    //if the widgets are not mapped to containers correctly, remap them..
                    // if(widgets.get(i).x != container[widgets.get(i).currentContainer].x || widgets.get(i).y != container[widgets.get(i).currentContainer].y || widgets.get(i).w != container[widgets.get(i).currentContainer].w || widgets.get(i).h != container[widgets.get(i).currentContainer].h){
                    if(widgets.get(i).x0 != (int)container[widgets.get(i).currentContainer].x || widgets.get(i).y0 != (int)container[widgets.get(i).currentContainer].y || widgets.get(i).w0 != (int)container[widgets.get(i).currentContainer].w || widgets.get(i).h0 != (int)container[widgets.get(i).currentContainer].h){
                        screenResized();
                        println("WidgetManager.pde: Remapping widgets to container layout...");
                    }
                }
            }
        }
    }

    public void draw(){
        if(visible){
            for(int i = 0; i < widgets.size(); i++){
                if(widgets.get(i).getIsActive()){
                    widgets.get(i).draw();
                    widgets.get(i).drawDropdowns();
                }else{
                    if(widgets.get(i).widgetTitle.equals("Networking")){
                        try{
                            w_networking.shutDown();
                        }catch (NullPointerException e){
                            println("WM:Networking_shutDown_Error: " + e);
                        }
                    }
                }
            }
        }
    }

    public void screenResized(){
        for(int i = 0; i < widgets.size(); i++){
            widgets.get(i).screenResized();
        }
    }

    public void mousePressed(){
        for(int i = 0; i < widgets.size(); i++){
            if(widgets.get(i).getIsActive()){
                widgets.get(i).mousePressed();
            }

        }
    }

    public void mouseReleased(){
        for(int i = 0; i < widgets.size(); i++){
            if(widgets.get(i).getIsActive()){
                widgets.get(i).mouseReleased();
            }
        }
    }

    public void mouseDragged(){
        for(int i = 0; i < widgets.size(); i++){
            if(widgets.get(i).getIsActive()){
                widgets.get(i).mouseDragged();
            }
        }
    }

    void setupLayouts(){
        //refer to [PUT_LINK_HERE] for layouts/numbers image
        //note that the order you create/add these layouts matters... if you reorganize these, the LayoutSelector will be out of order
        layouts.add(new Layout(new int[]{5})); //layout 1
        layouts.add(new Layout(new int[]{1,3,7,9})); //layout 2
        layouts.add(new Layout(new int[]{4,6})); //layout 3
        layouts.add(new Layout(new int[]{2,8})); //etc.
        layouts.add(new Layout(new int[]{4,3,9}));
        layouts.add(new Layout(new int[]{1,7,6}));
        layouts.add(new Layout(new int[]{1,3,8}));
        layouts.add(new Layout(new int[]{2,7,9}));
        layouts.add(new Layout(new int[]{4,11,12,13,14}));
        layouts.add(new Layout(new int[]{4,15,16,17,18}));
        layouts.add(new Layout(new int[]{1,7,11,12,13,14}));
        layouts.add(new Layout(new int[]{1,7,15,16,17,18}));
    }

    void printLayouts(){
        for(int i = 0; i < layouts.size(); i++){
            println("WM:printLayouts: " + layouts.get(i));
            String layoutString = "";
            for(int j = 0; j < layouts.get(i).myContainers.length; j++){
                // println("WM:layoutContainers: " + layouts.get(i).myContainers[j]);
                layoutString += layouts.get(i).myContainers[j].x + ", ";
                layoutString += layouts.get(i).myContainers[j].y + ", ";
                layoutString += layouts.get(i).myContainers[j].w + ", ";
                layoutString += layouts.get(i).myContainers[j].h;
            }
            println("WM:printLayouts: " + layoutString);
        }
    }

    public void setNewContainerLayout(int _newLayout){

        //find out how many active widgets we need...
        int numActiveWidgetsNeeded = layouts.get(_newLayout).myContainers.length;
        //calculate the number of current active widgets & keep track of which widgets are active
        int numActiveWidgets = 0;
        // ArrayList<int> activeWidgets = new ArrayList<int>();
        for(int i = 0; i < widgets.size(); i++){
            if(widgets.get(i).getIsActive()){
                numActiveWidgets++; //increment numActiveWidgets
                // activeWidgets.add(i); //keep track of the active widget
            }
        }

        if(numActiveWidgets > numActiveWidgetsNeeded){ //if there are more active widgets than needed
            //shut some down
            int numToShutDown = numActiveWidgets - numActiveWidgetsNeeded;
            int counter = 0;
            println("WM: Powering " + numToShutDown + " widgets down, and remapping.");
            for(int i = widgets.size()-1; i >= 0; i--){
                if(widgets.get(i).getIsActive() && counter < numToShutDown){
                    verbosePrint("WM: Deactivating widget [" + i + "]");
                    widgets.get(i).setIsActive(false);
                    counter++;
                }
            }

            //and map active widgets
            counter = 0;
            for(int i = 0; i < widgets.size(); i++){
                if(widgets.get(i).getIsActive()){
                    widgets.get(i).setContainer(layouts.get(_newLayout).containerInts[counter]);
                    counter++;
                }
            }

        } else if(numActiveWidgetsNeeded > numActiveWidgets){ //if there are less active widgets than needed
            //power some up
            int numToPowerUp = numActiveWidgetsNeeded - numActiveWidgets;
            int counter = 0;
            verbosePrint("WM: Powering " + numToPowerUp + " widgets up, and remapping.");
            for(int i = 0; i < widgets.size(); i++){
                if(!widgets.get(i).getIsActive() && counter < numToPowerUp){
                    verbosePrint("WM: Activating widget [" + i + "]");
                    widgets.get(i).setIsActive(true);
                    counter++;
                }
            }

            //and map active widgets
            counter = 0;
            for(int i = 0; i < widgets.size(); i++){
                if(widgets.get(i).getIsActive()){
                    widgets.get(i).setContainer(layouts.get(_newLayout).containerInts[counter]);
                    // widgets.get(i).screenResized(); // do this to make sure the container is updated
                    counter++;
                }
            }

        } else{ //if there are the same amount
            //simply remap active widgets
            verbosePrint("WM: Remapping widgets.");
            int counter = 0;
            for(int i = 0; i < widgets.size(); i++){
                if(widgets.get(i).getIsActive()){
                    widgets.get(i).setContainer(layouts.get(_newLayout).containerInts[counter]);
                    counter++;
                }
            }
        }
    }
};