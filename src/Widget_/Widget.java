package Widget_;

import GUI.GUIManager;
import controlP5.ControlP5;
import controlP5.Controller;
import controlP5.ScrollableList;
import processing.core.PApplet;

import java.util.ArrayList;
import java.util.List;

import static Containers_.GVI.container;
import static GUI.GGVI.*;

public class Widget extends GUIManager {

//    public ColorPalette CP;

    public int x0;
    public int y0;
    public int w0;
    public int h0; //true x,y,w,h of container
    protected int x, y, w, h; //adjusted x,y,w,h of white space `blank rectangle` under the nav...

    public int currentContainer; //this determines where the widget is located ... based on the x/y/w/h of the parent container

    protected boolean dropdownIsActive = false;
    private boolean previousDropdownIsActive = false;
    private boolean previousTopNavDropdownMenuIsOpen = false;
    private boolean widgetSelectorIsActive = false;

    private ArrayList<NavBarDropdown> dropdowns;
    public ControlP5 cp5_widget;
    public String widgetTitle = "No Title Set";
    //used to limit the size of the widget selector, forces a scroll bar to show and allows us to add even more widgets in the future
    private final float widgetDropdownScaling = .90F;
    private boolean isWidgetActive = false;

    //some variables for the dropdowns
    protected final int navH = 22;
    private int widgetSelectorWidth = 160;
    private int widgetSelectorHeight = 0;
    protected int dropdownWidth = 64;
    private boolean initialResize = false; //used to properly resize the widgetSelector when loading default settings

    public Widget(PApplet _parent){
//        super(_parent);

        cp5_widget = new ControlP5(_parent);
        cp5_widget.setAutoDraw(false); //this prevents the cp5 object from drawing automatically (if it is set to true it will be drawn last, on top of all other GUI stuff... not good)
        dropdowns = new ArrayList<NavBarDropdown>();
        //setup dropdown menus

        currentContainer = 5; //central container by default
        mapToCurrentContainer();

    }

    public boolean getIsActive() {
        return isWidgetActive;
    }

    public void setIsActive(boolean isActive) {
        isWidgetActive = isActive;
        //mapToCurrentContainer();
    }

    public void update(){
        updateDropdowns();
    }

    public void draw(){
        pushStyle();
        noStroke();
        fill(255);
        rect(x,y-1,w,h+1); //draw white widget background
        popStyle();

        //draw nav bars and button bars
        pushStyle();
        fill(150, 150, 150);
        rect(x0, y0, w0, navH); //top bar
        fill(200, 200, 200);
        rect(x0, y0+navH, w0, navH); //button bar
        popStyle();
    }

    public void addDropdown(String _id, String _title, List _items, int _defaultItem){
        NavBarDropdown dropdownToAdd = new NavBarDropdown(_id, _title, _items, _defaultItem);
        dropdowns.add(dropdownToAdd);
    }

    public void setupWidgetSelectorDropdown(ArrayList<String> _widgetOptions){
        cp5_widget.setColor(settings.dropdownColors);
        ScrollableList scrollList = cp5_widget.addScrollableList("WidgetSelector")
                .setPosition(x0+2, y0+2) //upper left corner
                // .setFont(h2)
                .setOpen(false)
                .setColor(settings.dropdownColors)
                .setOutlineColor(OBJECT_BORDER_GREY)
                //.setSize(widgetSelectorWidth, int(h0 * widgetDropdownScaling) )// + maxFreqList.size())
                //.setSize(widgetSelectorWidth, (NUM_WIDGETS_TO_SHOW+1)*(navH-4) )// + maxFreqList.size())
                // .setScrollSensitivity(0.0)
                .setBarHeight(navH-4) //height of top/primary bar
                .setItemHeight(navH-4) //height of all item/dropdown bars
                .addItems(_widgetOptions) // used to be .addItems(maxFreqList)
                ;

        scrollList.getCaptionLabel() //the caption label is the text object in the primary bar
                .toUpperCase(false) //DO NOT AUTOSET TO UPPERCASE!!!
                .setText(widgetTitle)
                .setFont(h4)
                .setSize(14)
                .getStyle() //need to grab style before affecting the paddingTop
                .setPaddingTop(4)
        ;

        scrollList.getValueLabel() //the value label is connected to the text objects in the dropdown item bars
                .toUpperCase(false) //DO NOT AUTOSET TO UPPERCASE!!!
                .setText(widgetTitle)
                .setFont(h5)
                .setSize(12) //set the font size of the item bars to 14pt
                .getStyle() //need to grab style before affecting the paddingTop
                .setPaddingTop(3) //4-pixel vertical offset to center text
        ;
    }

    public void setupNavDropdowns(){
        cp5_widget.setColor(settings.dropdownColors);
        // println("Setting up dropdowns...");
        for(int i = 0; i < dropdowns.size(); i++){
            int dropdownPos = dropdowns.size() - i;
            // println("dropdowns.get(i).id = " + dropdowns.get(i).id);
            ScrollableList scrollList = cp5_widget.addScrollableList(dropdowns.get(i).id)
                    .setPosition(x0+w0-(dropdownWidth*(dropdownPos))-(2*(dropdownPos)), y0 + navH + 2) //float right
                    .setFont(h5)
                    .setOpen(false)
                    .setColor(settings.dropdownColors)
                    .setOutlineColor(OBJECT_BORDER_GREY)
                    .setSize(dropdownWidth, (dropdowns.get(i).items.size()+1)*(navH-4) )// + maxFreqList.size())
                    .setBarHeight(navH-4)
                    .setItemHeight(navH-4)
                    .addItems(dropdowns.get(i).items) // used to be .addItems(maxFreqList)
                    ;

            scrollList.getCaptionLabel()
                    .toUpperCase(false) //DO NOT AUTOSET TO UPPERCASE!!!
                    .setText(dropdowns.get(i).returnDefaultAsString())
                    .setSize(12)
                    .getStyle()
                    .setPaddingTop(4)
            ;

            scrollList.getValueLabel() //the value label is connected to the text objects in the dropdown item bars
                    .toUpperCase(false) //DO NOT AUTOSET TO UPPERCASE!!!
                    .setText(widgetTitle)
                    .setSize(12) //set the font size of the item bars to 14pt
                    .getStyle() //need to grab style before affecting the paddingTop
                    .setPaddingTop(3) //4-pixel vertical offset to center text
            ;
        }
    }
    private void updateDropdowns(){
        //if a dropdown is open and mouseX/mouseY is outside of dropdown, then close it
        // println("dropdowns.size() = " + dropdowns.size());
        dropdownIsActive = false;

        if (!initialResize) {
            resizeWidgetSelector(); //do this once after instantiation to fix grey background drawing error
            initialResize = true;
        }

        //auto close dropdowns based on mouse location
        if(cp5_widget.get(ScrollableList.class, "WidgetSelector").isOpen()){
            dropdownIsActive = true;

        }
        for(int i = 0; i < dropdowns.size(); i++){
            if(cp5_widget.get(ScrollableList.class, dropdowns.get(i).id).isOpen()){
                //println("++++++++Mouse is over " + dropdowns.get(i).id);
                dropdownIsActive = true;
            }
        }

        //make sure that the widgetSelector CaptionLabel always corresponds to its widget
        cp5_widget.getController("WidgetSelector")
                .getCaptionLabel()
                .setText(widgetTitle)
        ;

    }

    public void drawDropdowns(){
        cp5_widget.draw(); //this draws all cp5 elements... in this case, the scrollable lists that populate our dropdowns<>

        //draw dropdown titles
        pushStyle();
        noStroke();
        textFont(h5);
        textSize(12);
        textAlign(CENTER, BOTTOM);
        fill(OPENBCI_DARKBLUE);
        for(int i = 0; i < dropdowns.size(); i++){
            int dropdownPos = dropdowns.size() - i;
            int _width = cp5_widget.getController(dropdowns.get(i).id).getWidth();
            int _x = (int)(cp5_widget.getController(dropdowns.get(i).id).getPosition()[0]);
            text(dropdowns.get(i).title, _x+_width/2, y0+(navH-2));
        }
        popStyle();
    }

    public void mouseDragged(){
    }

    public void mousePressed(){
    }

    public void mouseReleased(){
    }

    public void screenResized(){
        mapToCurrentContainer();
    }

    public void setTitle(String _widgetTitle){
        widgetTitle = _widgetTitle;
    }

    public void setContainer(int _currentContainer){
        currentContainer = _currentContainer;
        mapToCurrentContainer();
        screenResized();

    }

    private void resizeWidgetSelector() {
        int dropdownsItemsToShow = (int)((h0 * widgetDropdownScaling) / (navH - 4));
        widgetSelectorHeight = (dropdownsItemsToShow + 1) * (navH - 4);
        if (wm != null) {
            int maxDropdownHeight = (wm.widgetOptions.size() + 1) * (navH - 4);
            if (widgetSelectorHeight > maxDropdownHeight) widgetSelectorHeight = maxDropdownHeight;
        }

        cp5_widget.getController("WidgetSelector")
                .setPosition(x0+2, y0+2) //upper left corner
        ;
        cp5_widget.getController("WidgetSelector")
                .setSize(widgetSelectorWidth, widgetSelectorHeight);
        ;
    }

    private void mapToCurrentContainer(){
        x0 = (int)container[currentContainer].x;
        y0 = (int)container[currentContainer].y;
        w0 = (int)container[currentContainer].w;
        h0 = (int)container[currentContainer].h;

        x = x0;
        y = y0 + navH*2;
        w = w0;
        h = h0 - navH*2;

        //This line resets the origin for all cp5 elements under "cp5_widget" when the screen is resized, otherwise there will be drawing errors
        cp5_widget.setGraphics(this, 0, 0);

        if (cp5_widget.getController("WidgetSelector") != null) {
            resizeWidgetSelector();
        }

        //Other dropdowns
        for(int i = 0; i < dropdowns.size(); i++){
            int dropdownPos = dropdowns.size() - i;
            cp5_widget.getController(dropdowns.get(i).id)
                    //.setPosition(w-(dropdownWidth*dropdownPos)-(2*(dropdownPos+1)), navHeight+(y+2)) // float left
                    .setPosition(x0+w0-(dropdownWidth*(dropdownPos))-(2*(dropdownPos)), navH +(y0+2)) //float right
            //.setSize(dropdownWidth, (maxFreqList.size()+1)*(navBarHeight-4))
            ;
        }
    }

    public boolean isMouseHere(){
        if(getIsActive()){
            if(mouseX >= x0 && mouseX <= x0 + w0 && mouseY >= y0 && mouseY <= y0 + h0){
                println("Your cursor is in " + widgetTitle);
                return true;
            } else{
                return false;
            }
        } else {
            return false;
        }
    }

    //For use with multiple Cp5 controllers per class/widget. Can only be called once per widget during update loop.
    protected void lockElementsOnOverlapCheck(List<Controller> listOfControllers) {
        //Check against TopNav Menus
        if (topNav.getDropdownMenuIsOpen() != previousTopNavDropdownMenuIsOpen) {
            for (Controller c : listOfControllers) {
                if (c == null) {
                    continue; //Gracefully skip over a controller if it is null
                }
                //println(widgetTitle, " ", c.getName(), " lock because of topnav == ", topNav.getDropdownMenuIsOpen());
                c.setLock(topNav.getDropdownMenuIsOpen());
            }
            previousTopNavDropdownMenuIsOpen = topNav.getDropdownMenuIsOpen();
            if (previousTopNavDropdownMenuIsOpen) {
                return;
            }
        }
        //Check against Widget Dropdowns
        if (dropdownIsActive != previousDropdownIsActive) {
            for (Controller c : listOfControllers) {
                if (c == null) {
                    continue; //Gracefully skip over a controller if it is null
                }
                //println(widgetTitle, " ", c.getName(), " lock because of widget navbar dropdown == ", dropdownIsActive);
                c.setLock(dropdownIsActive);
            }
            previousDropdownIsActive = dropdownIsActive;
        }
    }
}; //end of base Widget class