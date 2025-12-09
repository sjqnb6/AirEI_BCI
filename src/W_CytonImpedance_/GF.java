package W_CytonImpedance_;

import static WidgetManager_.GVI.w_cytonImpedance;

public class GF {

    //These functions need to be global! These functions are activated when an item from the corresponding dropdown is selected
//Update: It's not worth the trouble to implement a callback listener in the widget for this specifc kind of dropdown. Keep using this pattern for widget Nav dropdowns. - February 2021 RW
    public static void CytonImpedance_Mode(int n) {
        w_cytonImpedance.setSignalCheckMode(n);
    }

    public static void CytonImpedance_LabelMode(int n) {
        w_cytonImpedance.setShowAnatomicalName(n);
    }

    public static void CytonImpedance_MasterCheckInterval(int n) {
        w_cytonImpedance.setMasterCheckInterval(n);
    }

}
