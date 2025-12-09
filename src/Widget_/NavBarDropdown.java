package Widget_;

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
//    NavBarDropdown is a single dropdown item in any instance of a Widget
//

import java.util.List;

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

class NavBarDropdown{

    String id;
    String title;
    // String[] items;
    List<String> items;
    int defaultItem;

    NavBarDropdown(String _id, String _title, List _items, int _defaultItem){
        id = _id;
        title = _title;
        // int dropdownSize = _items.length;
        // items = new String[_items.length];
        items = _items;

        defaultItem = _defaultItem;
    }

    void update(){
    }

    void draw(){
    }

    void screenResized(){
    }

    void mousePressed(){
    }

    void mouseReleased(){
    }

    String returnDefaultAsString(){
        String _defaultItem = items.get(defaultItem);
        return _defaultItem;
    }

}