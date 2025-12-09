package WidgetManager_;

import Containers_.Container;

import static Containers_.GVI.container;
import static processing.core.PApplet.println;

//the Layout class is an orgnanizational tool ... a layout consists of a combination of containers ... refer to Container.pde
public class Layout{

    Container[] myContainers;
    int[] containerInts;

    Layout(int[] _myContainers){ //when creating a new layout, you pass in the integer #s of the containers you want as part of the layout ... so if I pass in the array {5}, my layout is 1 container that takes up the whole GUI body
        //constructor stuff
        myContainers = new Container[_myContainers.length]; //make the myContainers array equal to the size of the incoming array of ints
        containerInts = new int[_myContainers.length];
        for(int i = 0; i < _myContainers.length; i++){
            myContainers[i] = container[_myContainers[i]];
            containerInts[i] = _myContainers[i];
        }
    }

    Container getContainer(int _numContainer){
        if(_numContainer < myContainers.length){
            return myContainers[_numContainer];
        } else{
            println("WM: Tried to return a non-existant container...");
            return myContainers[myContainers.length-1];
        }
    }
};