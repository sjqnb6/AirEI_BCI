package W_PacketLoss;

import Board_.Board;
import Extras_.RectDimensions;
import Grid_.Grid;
import PacketLossTracker_.PacketLossTracker;
import PacketLossTracker_.PacketRecord;
import Widget_.Widget;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ScrollableList;
import processing.core.PApplet;

import java.util.Map;
import Globel.GUI;

import static Globel.GUI.*;

public class W_PacketLoss extends Widget {
    GUI MAIN;

    private static final int BG_TOP = 0xFF0A1220;
    private static final int BG_BOTTOM = 0xFF101E36;
    private static final int PANEL_BG = 0xCC13243F;
    private static final int PANEL_STROKE = 0x6683A2CC;
    private static final int TEXT_MAIN = 0xFFEAF2FF;
    private static final int TEXT_SUB = 0xFFB5C8E5;
    private static final int TEXT_HEADER = 0xFFEAF2FF;
    private static final int TEXT_VALUE = 0xFFDDF3FF;
    private static final int TEXT_PERCENT = 0xFFFFD889;
    private static final int COLOR_DROPDOWN_BG = 0xFF1A2B46;
    private static final int COLOR_DROPDOWN_FG = 0xFF2D4668;

    private Grid dataGrid;
    private PacketLossTracker packetLossTracker;

    private PacketRecord sessionPacketRecord;
    private PacketRecord streamPacketRecord;
    private PacketRecord lastMillisPacketRecord;

    private ScrollableList tableDropdown;

    private final int padding = 5;
    private final int cellHeight = 20;

    private CalculationWindowSize tableWindowSize = CalculationWindowSize.SECONDS10;

    public W_PacketLoss(GUI MAIN){
        super(MAIN); //calls the parent CONSTRUCTOR method of Widget (DON'T REMOVE)
        this.MAIN = MAIN;
        dataGrid = new Grid(MAIN, 5/*numRows*/, 4/*numCols*/, cellHeight);
        packetLossTracker = ((Board)MAIN.currentBoard).getPacketLossTracker();
        sessionPacketRecord = packetLossTracker.getSessionPacketRecord();
        streamPacketRecord = packetLossTracker.getStreamPacketRecord();

        dataGrid.setString("当前会话", 0, 1);
        dataGrid.setString("连续数据流", 0, 2);
        dataGrid.setString("时间窗口", 0, 3);

        dataGrid.setString("丢包数", 1, 0);
        dataGrid.setString("接收包数", 2, 0);
        dataGrid.setString("期望包数", 3, 0);
        dataGrid.setString("丢包率", 4, 0);
        applyGridTextTheme();

        createTableDropdown();

        // call once in constructor
        screenResized();
    }

    private void createTableDropdown() {
        tableDropdown = cp5_widget.addScrollableList("TableTimeWindow")
                .setDrawOutline(false)
                .setOpen(false)
                .setColorBackground(COLOR_DROPDOWN_BG)
                .setColorForeground(COLOR_DROPDOWN_FG)
                .setColorValueLabel(TEXT_MAIN)
                .setColorCaptionLabel(TEXT_MAIN)
                .setOutlineColor(PANEL_STROKE)
                .setBarHeight(cellHeight) //height of top/primary bar
                .setItemHeight(cellHeight) //height of all item/dropdown bars
        ;

        // for each entry in the enum, add it to the dropdown.
        for (CalculationWindowSize value : CalculationWindowSize.values()) {
            // this will store the *actual* enum object inside the dropdown!
            tableDropdown.addItem(getWindowLabel(value), value);
        }

        tableDropdown.getCaptionLabel() //the caption label is the text object in the primary bar
                .toUpperCase(false) //DO NOT AUTOSET TO UPPERCASE!!!
                .setText(getWindowLabel(tableWindowSize))
                .setFont(p7)
                .setSize(12)
                .getStyle() //need to grab style before affecting the paddingTop
                .setPaddingTop(3)
        ;
        tableDropdown.getValueLabel() //the value label is connected to the text objects in the dropdown item bars
                .toUpperCase(false) //DO NOT AUTOSET TO UPPERCASE!!!
                .setText("VALUE LABEL")
                .setFont(p7)
                .setSize(12) //set the font size of the item bars to 14pt
                .getStyle() //need to grab style before affecting the paddingTop
                .setPaddingTop(3) //4-pixel vertical offset to center text
        ;

        tableDropdown.onChange(new CallbackListener() {
            public void controlEvent(CallbackEvent event) {
                int val = (int)tableDropdown.getValue();
                Map bob = tableDropdown.getItem(val);
                tableWindowSize = (CalculationWindowSize)bob.get("value");
                tableDropdown.getCaptionLabel().setText(getWindowLabel(tableWindowSize));
            }
        });
    }

    public void update(){
        super.update(); //calls the parent update() method of Widget (DON'T REMOVE)

        lastMillisPacketRecord = packetLossTracker.getCumulativePacketRecordForLast(tableWindowSize.getMilliseconds());

        dataGrid.setString(MAIN.nfc(sessionPacketRecord.numLost), 1, 1);
        dataGrid.setString(MAIN.nfc(sessionPacketRecord.numReceived), 2, 1);
        dataGrid.setString(MAIN.nfc(sessionPacketRecord.getNumExpected()), 3, 1);
        dataGrid.setString(MAIN.nf(sessionPacketRecord.getLostPercent(), 0, 4 /*decimals*/) + " %", 4, 1);

        dataGrid.setString(MAIN.nfc(streamPacketRecord.numLost), 1, 2);
        dataGrid.setString(MAIN.nfc(streamPacketRecord.numReceived), 2, 2);
        dataGrid.setString(MAIN.nfc(streamPacketRecord.getNumExpected()), 3, 2);
        dataGrid.setString(MAIN.nf(streamPacketRecord.getLostPercent(), 0, 4 /*decimals*/) + " %", 4, 2);

        dataGrid.setString(MAIN.nfc(lastMillisPacketRecord.numLost), 1, 3);
        dataGrid.setString(MAIN.nfc(lastMillisPacketRecord.numReceived), 2, 3);
        dataGrid.setString(MAIN.nfc(lastMillisPacketRecord.getNumExpected()), 3, 3);
        dataGrid.setString(MAIN.nf(lastMillisPacketRecord.getLostPercent(), 0, 4 /*decimals*/) + " %", 4, 3);

        // place dropdown on table
        RectDimensions cellDim = dataGrid.getCellDims(0, 3);
        tableDropdown.setPosition(cellDim.x, cellDim.y);

        int dropdownHeight = tableDropdown.getBarHeight() + tableDropdown.getBarHeight() * tableDropdown.getItems().size();
        tableDropdown.setSize(cellDim.w, dropdownHeight);
    }

    public void draw(){
        super.draw(); //calls the parent draw() method of Widget (DON'T REMOVE)

        MAIN.pushStyle();
        drawGradientBackground(x, y, w, h);
        MAIN.noStroke();
        MAIN.fill(PANEL_BG);
        MAIN.rect(x + 6, y + 6, w - 12, h - 12, 8);
        MAIN.noFill();
        MAIN.stroke(PANEL_STROKE);
        MAIN.rect(x + 6, y + 6, w - 12, h - 12, 8);

        MAIN.fill(TEXT_MAIN);
        MAIN.textFont(p7, 12);
        MAIN.text("会话时长: " + sessionTimeElapsed.toString(), x + padding, y + 15);
        MAIN.fill(TEXT_SUB);
        MAIN.text("数据流时长: " + streamTimeElapsed.toString(), x + padding, y + 35);
        MAIN.popStyle();

        dataGrid.draw();
    }

    public void screenResized(){
        super.screenResized(); //calls the parent screenResized() method of Widget (DON'T REMOVE)

        dataGrid.setDim(x, y + 50, w);
    }

    public void mousePressed(){
        super.mousePressed(); //calls the parent mousePressed() method of Widget (DON'T REMOVE)

    }

    public void mouseReleased(){
        super.mouseReleased(); //calls the parent mouseReleased() method of Widget (DON'T REMOVE)

    }

    private float calcPercent(float total, float fraction) {
        if(total == 0) {
            return 0;
        }

        return fraction * 100 / total;
    }

    private void drawGradientBackground(int gx, int gy, int gw, int gh) {
        MAIN.pushStyle();
        MAIN.noFill();
        for (int i = 0; i < gh; i++) {
            float t = gh <= 1 ? 0f : (float) i / (float) (gh - 1);
            MAIN.stroke(lerpColorARGB(BG_TOP, BG_BOTTOM, t));
            MAIN.line(gx, gy + i, gx + gw, gy + i);
        }
        MAIN.popStyle();
    }

    private int lerpColorARGB(int c1, int c2, float t) {
        int a1 = (c1 >>> 24) & 0xFF;
        int r1 = (c1 >>> 16) & 0xFF;
        int g1 = (c1 >>> 8) & 0xFF;
        int b1 = c1 & 0xFF;
        int a2 = (c2 >>> 24) & 0xFF;
        int r2 = (c2 >>> 16) & 0xFF;
        int g2 = (c2 >>> 8) & 0xFF;
        int b2 = c2 & 0xFF;
        int a = (int) PApplet.lerp(a1, a2, t);
        int r = (int) PApplet.lerp(r1, r2, t);
        int g = (int) PApplet.lerp(g1, g2, t);
        int b = (int) PApplet.lerp(b1, b2, t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void applyGridTextTheme() {
        dataGrid.setTextColor(TEXT_SUB, 1, 0);
        dataGrid.setTextColor(TEXT_SUB, 2, 0);
        dataGrid.setTextColor(TEXT_SUB, 3, 0);
        dataGrid.setTextColor(TEXT_SUB, 4, 0);

        dataGrid.setTextColor(TEXT_HEADER, 0, 1);
        dataGrid.setTextColor(TEXT_HEADER, 0, 2);
        dataGrid.setTextColor(TEXT_HEADER, 0, 3);

        for (int row = 1; row <= 3; row++) {
            dataGrid.setTextColor(TEXT_VALUE, row, 1);
            dataGrid.setTextColor(TEXT_VALUE, row, 2);
            dataGrid.setTextColor(TEXT_VALUE, row, 3);
        }
        dataGrid.setTextColor(TEXT_PERCENT, 4, 1);
        dataGrid.setTextColor(TEXT_PERCENT, 4, 2);
        dataGrid.setTextColor(TEXT_PERCENT, 4, 3);
    }

    private String getWindowLabel(CalculationWindowSize windowSize) {
        switch (windowSize) {
            case SECONDS1:
                return "过去1秒";
            case SECONDS10:
                return "过去10秒";
            case MINUTE1:
                return "过去1分钟";
            default:
                return windowSize.getName();
        }
    }

}
