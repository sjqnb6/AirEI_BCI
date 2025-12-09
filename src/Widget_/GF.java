package Widget_;

import static GUI.GGVI.wm;
import static processing.core.PApplet.println;

public class GF {

    void WidgetSelector(int n){
        println("New widget [" + n + "] selected for container...");
        //find out if the widget you selected is already active
        boolean isSelectedWidgetActive = wm.widgets.get(n).getIsActive();

        //find out which widget & container you are currently in...
        int theContainer = -1;
        for(int i = 0; i < wm.widgets.size(); i++){
            if(wm.widgets.get(i).isMouseHere()){
                theContainer = wm.widgets.get(i).currentContainer; //keep track of current container (where mouse is...)
                if(isSelectedWidgetActive){ //if the selected widget was already active
                    wm.widgets.get(i).setContainer(wm.widgets.get(n).currentContainer); //just switch the widget locations (ie swap containers)
                } else{
                    wm.widgets.get(i).setIsActive(false);   //deactivate the current widget (if it is different than the one selected)
                }
            }
        }

        wm.widgets.get(n).setIsActive(true);//activate the new widget
        wm.widgets.get(n).setContainer(theContainer);//map it to the current container
    }

}
