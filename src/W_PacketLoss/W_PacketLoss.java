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
    private static final int STATUS_OK = 0xFF4FD18B;
    private static final int STATUS_NOTICE = 0xFFFFD166;
    private static final int STATUS_WARN = 0xFFFF9F43;
    private static final int STATUS_DANGER = 0xFFFF5C6C;

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
        super(MAIN); // 调用 Widget 父类构造函数。
        this.MAIN = MAIN;
        dataGrid = new Grid(MAIN, 5/*行数*/, 4/*列数*/, cellHeight);
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

        // 构造时先按当前窗口尺寸布局一次。
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
                .setBarHeight(cellHeight) // 主栏高度。
                .setItemHeight(cellHeight) // 下拉项高度。
        ;

        // 将所有时间窗口选项加入下拉菜单。
        for (CalculationWindowSize value : CalculationWindowSize.values()) {
            // 下拉项中保存枚举对象，便于后续直接读取。
            tableDropdown.addItem(getWindowLabel(value), value);
        }

        tableDropdown.getCaptionLabel()
                .toUpperCase(false)
                .setText(getWindowLabel(tableWindowSize))
                .setFont(p7)
                .setSize(12)
                .getStyle()
                .setPaddingTop(3)
        ;
        tableDropdown.getValueLabel()
                .toUpperCase(false)
                .setText("窗口选项")
                .setFont(p7)
                .setSize(12)
                .getStyle()
                .setPaddingTop(3)
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
        super.update(); // 调用 Widget 父类更新逻辑。

        lastMillisPacketRecord = packetLossTracker.getCumulativePacketRecordForLast(tableWindowSize.getMilliseconds());

        dataGrid.setString(MAIN.nfc(sessionPacketRecord.numLost), 1, 1);
        dataGrid.setString(MAIN.nfc(sessionPacketRecord.numReceived), 2, 1);
        dataGrid.setString(MAIN.nfc(sessionPacketRecord.getNumExpected()), 3, 1);
        dataGrid.setString(MAIN.nf(sessionPacketRecord.getLostPercent(), 0, 4 /*小数位*/) + " %", 4, 1);

        dataGrid.setString(MAIN.nfc(streamPacketRecord.numLost), 1, 2);
        dataGrid.setString(MAIN.nfc(streamPacketRecord.numReceived), 2, 2);
        dataGrid.setString(MAIN.nfc(streamPacketRecord.getNumExpected()), 3, 2);
        dataGrid.setString(MAIN.nf(streamPacketRecord.getLostPercent(), 0, 4 /*小数位*/) + " %", 4, 2);

        dataGrid.setString(MAIN.nfc(lastMillisPacketRecord.numLost), 1, 3);
        dataGrid.setString(MAIN.nfc(lastMillisPacketRecord.numReceived), 2, 3);
        dataGrid.setString(MAIN.nfc(lastMillisPacketRecord.getNumExpected()), 3, 3);
        dataGrid.setString(MAIN.nf(lastMillisPacketRecord.getLostPercent(), 0, 4 /*小数位*/) + " %", 4, 3);

        // 将时间窗口下拉菜单放在表格对应单元格中。
        RectDimensions cellDim = dataGrid.getCellDims(0, 3);
        tableDropdown.setPosition(cellDim.x, cellDim.y);

        int dropdownHeight = tableDropdown.getBarHeight() + tableDropdown.getBarHeight() * tableDropdown.getItems().size();
        tableDropdown.setSize(cellDim.w, dropdownHeight);
    }

    public void draw(){
        super.draw(); // 调用 Widget 父类绘制逻辑。

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
        drawPacketLossWarningPanel();
    }

    public void screenResized(){
        super.screenResized(); // 调用 Widget 父类尺寸更新逻辑。

        dataGrid.setDim(x, y + 50, w);
    }

    public void mousePressed(){
        super.mousePressed(); // 调用 Widget 父类鼠标按下逻辑。

    }

    public void mouseReleased(){
        super.mouseReleased(); // 调用 Widget 父类鼠标释放逻辑。

    }

    private float calcPercent(float total, float fraction) {
        if(total == 0) {
            return 0;
        }

        return fraction * 100 / total;
    }

    private void drawPacketLossWarningPanel() {
        if (lastMillisPacketRecord == null) {
            return;
        }

        int panelX = x + 14;
        int panelY = y + 165;
        int panelW = w - 28;
        int panelH = h - 180;
        if (panelH < 120) {
            panelH = 120;
        }

        float lostPercent = lastMillisPacketRecord.getLostPercent();
        int statusColor = getStatusColor(lostPercent);
        String level = getStatusLevel(lostPercent);
        String description = getStatusDescription(lostPercent);
        String action = getStatusAction(lostPercent);

        MAIN.pushStyle();
        MAIN.fill(0x661A2B46);
        MAIN.stroke(PANEL_STROKE);
        MAIN.rect(panelX, panelY, panelW, panelH, 8);

        MAIN.fill(TEXT_HEADER);
        MAIN.textFont(p7, 14);
        MAIN.textAlign(MAIN.LEFT, MAIN.TOP);
        MAIN.text("丢包警示", panelX + 12, panelY + 10);

        int contentY = panelY + 38;
        boolean compact = panelW < 430;
        int leftW = compact ? panelW - 24 : panelW / 2 - 10;
        int rightX = compact ? panelX + 12 : panelX + leftW + 18;
        int rightY = compact ? contentY + 142 : contentY;

        MAIN.noStroke();
        MAIN.fill(statusColor);
        MAIN.ellipse(panelX + 22, contentY + 14, 16, 16);
        MAIN.fill(TEXT_VALUE);
        MAIN.textFont(p7, 16);
        MAIN.text(level, panelX + 38, contentY + 4);

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7, 12);
        MAIN.text(description, panelX + 12, contentY + 34, leftW, 34);

        MAIN.fill(TEXT_PERCENT);
        MAIN.textFont(p7, 13);
        MAIN.text("当前窗口丢包率: " + MAIN.nf(lostPercent, 0, 4) + " %", panelX + 12, contentY + 74);

        MAIN.fill(TEXT_SUB);
        MAIN.textFont(p7, 12);
        MAIN.text(action, panelX + 12, contentY + 98, leftW, 44);

        if (compact) {
            MAIN.stroke(PANEL_STROKE);
            MAIN.line(panelX + 12, rightY - 10, panelX + panelW - 12, rightY - 10);
        } else {
            MAIN.stroke(PANEL_STROKE);
            MAIN.line(rightX - 10, contentY, rightX - 10, panelY + panelH - 12);
        }

        MAIN.fill(TEXT_HEADER);
        MAIN.textFont(p7, 13);
        MAIN.text("分级阈值", rightX, rightY + 2);

        drawLevelRow(rightX, rightY + 30, STATUS_OK, "正常", "1% 以下，链路稳定");
        drawLevelRow(rightX, rightY + 54, STATUS_NOTICE, "关注", "1% - 10%，建议观察");
        drawLevelRow(rightX, rightY + 78, STATUS_WARN, "警告", "10% - 20%，检查连接");
        drawLevelRow(rightX, rightY + 102, STATUS_DANGER, "严重", "20% 以上，建议重连");

        MAIN.popStyle();
    }

    private void drawLevelRow(int rowX, int rowY, int color, String label, String note) {
        MAIN.noStroke();
        MAIN.fill(color);
        MAIN.rect(rowX, rowY + 5, 12, 8, 3);
        MAIN.fill(TEXT_VALUE);
        MAIN.textFont(p7, 12);
        MAIN.text(label, rowX + 20, rowY);
        MAIN.fill(TEXT_SUB);
        MAIN.text(note, rowX + 68, rowY);
    }

    private int getStatusColor(float lostPercent) {
        if (lostPercent >= 20.0f) {
            return STATUS_DANGER;
        } else if (lostPercent >= 10.0f) {
            return STATUS_WARN;
        } else if (lostPercent >= 1.0f) {
            return STATUS_NOTICE;
        }
        return STATUS_OK;
    }

    private String getStatusLevel(float lostPercent) {
        if (lostPercent >= 20.0f) {
            return "严重丢包";
        } else if (lostPercent >= 10.0f) {
            return "丢包警告";
        } else if (lostPercent >= 1.0f) {
            return "轻微丢包";
        }
        return "链路正常";
    }

    private String getStatusDescription(float lostPercent) {
        if (lostPercent >= 20.0f) {
            return "当前时间窗口内丢包率较高，波形可能出现明显断续或失真。";
        } else if (lostPercent >= 10.0f) {
            return "当前时间窗口内有连续丢包风险，建议尽快检查采集链路。";
        } else if (lostPercent >= 1.0f) {
            return "当前时间窗口内出现少量丢包，建议继续观察趋势。";
        }
        return "当前时间窗口内未见明显丢包，数据流状态稳定。";
    }

    private String getStatusAction(float lostPercent) {
        if (lostPercent >= 20.0f) {
            return "处理建议: 停止采集后重新连接设备，并检查串口、供电和无线距离。";
        } else if (lostPercent >= 10.0f) {
            return "处理建议: 检查接口松动、降低干扰，并观察丢包率是否回落。";
        } else if (lostPercent >= 1.0f) {
            return "处理建议: 暂时无需中断采集，持续观察最近窗口变化。";
        }
        return "处理建议: 无需处理，保持当前连接状态即可。";
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
