package W_Template_;

import static processing.core.PApplet.println;

public class GF {

    //These functions need to be global! These functions are activated when an item from the corresponding dropdown is selected
    void Dropdown1(int n){
        println("Item " + (n+1) + " selected from Dropdown 1");
        if(n==0){
            //do this
        } else if(n==1){
            //do this instead
        }
    }

    void Dropdown2(int n){
        println("Item " + (n+1) + " selected from Dropdown 2");
    }

    void Dropdown3(int n){
        println("Item " + (n+1) + " selected from Dropdown 3");
    }

}
