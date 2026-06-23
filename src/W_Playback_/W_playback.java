package W_Playback_;

import CustomCp5Classes_.MenuList;
import DataProcessing_.DataProcessing;
import Widget_.Widget;
import controlP5.Button;
import controlP5.CallbackEvent;
import controlP5.CallbackListener;
import controlP5.ControlP5;
import controlP5.Controller;
import controlP5.ScrollableList;
import processing.core.PApplet;
import processing.core.PFont;
import processing.data.JSONArray;
import processing.data.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import Globel.GUI;

import static Debugging_.GF.output;
import static Extras_.GF.shortenString;
import static Globel.GUI.*;
import static W_Playback_.GF.brandPlaybackName;
import static W_Playback_.GF.userSelectedPlaybackMenuList;

public class W_playback extends Widget {
    private static final int BG_TOP = 0xFF0A1220;
    private static final int BG_BOTTOM = 0xFF0D1830;
    private static final int PANEL = 0xCC13243F;
    private static final int PANEL_STROKE = 0x6683A2CC;
    private static final int CARD_BG = 0x2F172B48;
    private static final int CARD_STROKE = 0x7AA2BEE3;
    private static final int TEXT_MAIN = 0xFFEAF2FF;
    private static final int TEXT_SUB = 0xFF98AECE;
    private static final int ACCENT = 0xFF4FD8FF;
    private static final int BUTTON_BG = 0xFF1D3354;
    private static final int BUTTON_FG = 0xFF27446E;
    private static final int BUTTON_ACTIVE = 0xFF31598F;
    private static final int BUTTON_BORDER = 0xFF7EA6D4;

    GUI MAIN;
    protected PApplet pApplet;

    DataProcessing dataProcessing;
    ControlP5 cp5_playback;
    Button selectPlaybackFileButton;
    MenuList playbackMenuList;
    int padding = 10;
    List<Controller> cp5ElementsToCheck = new ArrayList<Controller>();

    private boolean menuHasUpdated = false;

    public W_playback(GUI MAIN) {
        super(MAIN);
        this.MAIN = MAIN;
        pApplet = MAIN;

        cp5_playback = new ControlP5(pApplet);
        cp5_playback.setGraphics(pApplet, 0, 0);
        cp5_playback.setAutoDraw(false);

        int initialWidth = w - padding * 2;
        createPlaybackMenuList(cp5_playback, "playbackMenuList", x + padding, y + 54, initialWidth, h - 68, p7);
        createSelectPlaybackFileButton("selectPlaybackFile_Session", "选择回放文件", x + w - 164, y - navHeight + 2, 162, navHeight - 6);
    }

    public void update() {
        super.update();
        if (!menuHasUpdated) {
            refreshPlaybackList();
            menuHasUpdated = true;
        }
        if (cp5_widget.get(ScrollableList.class, "WidgetSelector").isOpen() || topNav.getDropdownMenuIsOpen()) {
            if (!playbackMenuList.isLock()) {
                playbackMenuList.lock();
                playbackMenuList.setUpdate(false);
            }
        } else {
            if (playbackMenuList.isLock()) {
                playbackMenuList.unlock();
                playbackMenuList.setUpdate(true);
            }
            playbackMenuList.updateMenu();
        }
        lockElementsOnOverlapCheck(cp5ElementsToCheck);
    }

    public void draw() {
        super.draw();

        pApplet.pushStyle();
        drawGradientBackground(x, y, w, h);

        int panelX = x + 10;
        int panelY = y + 10;
        int panelW = w - 20;
        int panelH = h - 20;

        pApplet.noStroke();
        pApplet.fill(PANEL);
        pApplet.rect(panelX, panelY, panelW, panelH, 4);
        pApplet.stroke(PANEL_STROKE);
        pApplet.strokeWeight(1.1f);
        pApplet.noFill();
        pApplet.rect(panelX, panelY, panelW, panelH, 4);
        pApplet.stroke(130, 184, 255, 90);
        pApplet.line(panelX + 8, panelY + 8, panelX + panelW - 8, panelY + 8);

        drawHeader(panelX, panelY, panelW);
        drawListCard(panelX + 10, panelY + 48, panelW - 20, panelH - 58);

        pApplet.popStyle();

        cp5_playback.draw();
    }

    public void screenResized() {
        super.screenResized();

        cp5_playback.setGraphics(pApplet, 0, 0);

        selectPlaybackFileButton.setPosition(x + w - selectPlaybackFileButton.getWidth() - 12, y - navHeight + 2);

        playbackMenuList.setPosition(x + padding + 12, y + 72);
        playbackMenuList.setSize(w - padding * 2 - 24, h - 102);
        refreshPlaybackList();
    }

    public void refreshPlaybackList() {
        File f = new File(userPlaybackHistoryFile);
        if (!f.exists()) {
            MAIN.println("AirEIBCI::RefreshPlaybackList: 找不到回放历史文件。");
            return;
        }

        try {
            playbackMenuList.items.clear();
            loadPlaybackHistoryJSON = pApplet.loadJSONObject(userPlaybackHistoryFile);
            JSONArray loadPlaybackHistoryJSONArray = loadPlaybackHistoryJSON.getJSONArray("playbackFileHistory");
            for (int i = loadPlaybackHistoryJSONArray.size() - 1; i >= 0; i--) {
                JSONObject loadRecentPlaybackFile = loadPlaybackHistoryJSONArray.getJSONObject(i);
                int fileNumber = loadRecentPlaybackFile.getInt("recentFileNumber");
                String shortFileName = brandPlaybackName(loadRecentPlaybackFile.getString("id"));
                String longFilePath = loadRecentPlaybackFile.getString("filePath");

                int totalPadding = padding + playbackMenuList.padding + 24;
                shortFileName = shortenString(MAIN, shortFileName, w - totalPadding * 2.f, p4);
                playbackMenuList.addItem(shortFileName, Integer.toString(fileNumber), longFilePath);
            }
            playbackMenuList.updateMenu();
        } catch (NullPointerException e) {
            MAIN.println("PlaybackWidget: 找不到回放历史文件。");
        }
    }

    private void createSelectPlaybackFileButton(String name, String text, int _x, int _y, int _w, int _h) {
        selectPlaybackFileButton = MAIN.createButton(cp5_playback, name, text, _x, _y, _w, _h);
        selectPlaybackFileButton.setColorBackground(BUTTON_BG);
        selectPlaybackFileButton.setColorForeground(BUTTON_FG);
        selectPlaybackFileButton.setColorActive(BUTTON_ACTIVE);
        selectPlaybackFileButton.setBorderColor(BUTTON_BORDER);
        selectPlaybackFileButton.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                output("选择回放文件");
                pApplet.selectInput("选择 AirEIBCI 回放文件：", "playbackSelectedWidgetButton");
            }
        });
        selectPlaybackFileButton.setDescription("点击打开对话框，选择 AirEIBCI 回放文件（.txt 或 .csv）。");
        cp5ElementsToCheck.add((Controller) selectPlaybackFileButton);
    }

    private void createPlaybackMenuList(ControlP5 _cp5, String name, int _x, int _y, int _w, int _h, PFont font) {
        playbackMenuList = new MenuList(_cp5, name, _w, _h, font, MAIN);
        playbackMenuList.setPosition(_x, _y);
        //playbackMenuList.setTheme(0x1A0B1424, 0x3A1A2F4C, 0xAA27486F, 0xFF4FD8FF, 0xFF8EDCFF, 0xFFEAF2FF, 0x883B6D99);
        playbackMenuList.addCallback(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                if (theEvent.getAction() == ControlP5.ACTION_BROADCAST) {
                    float valueOfItem = playbackMenuList.getValue();
                    if (valueOfItem >= 0 && valueOfItem <= (playbackMenuList.items.size() - 1)) {
                        Map m = playbackMenuList.getItem((int) (valueOfItem));
                        userSelectedPlaybackMenuList(pApplet, m.get("copy").toString(), (int) (valueOfItem));
                    }
                }
            }
        });
        playbackMenuList.scrollerLength = 40;
    }

    private void drawHeader(int panelX, int panelY, int panelW) {
        pApplet.fill(TEXT_MAIN);
        pApplet.textFont(p7);
        pApplet.textSize(15);
        pApplet.textAlign(PApplet.LEFT, PApplet.TOP);
        pApplet.text("回放历史中心", panelX + 12, panelY + 9);

        pApplet.fill(TEXT_SUB);
        pApplet.textSize(11);
        pApplet.text("快速加载 AirEIBCI 历史文件，继续分析、复盘与演示。", panelX + 12, panelY + 29);

        int badgeW = 122;
        int badgeH = 24;
        int bx = panelX + panelW - badgeW - 12;
        int by = panelY + 10;
        pApplet.noStroke();
        pApplet.fill((ACCENT >> 16) & 0xFF, (ACCENT >> 8) & 0xFF, ACCENT & 0xFF, 38);
        pApplet.rect(bx, by, badgeW, badgeH, 3);
        pApplet.stroke((ACCENT >> 16) & 0xFF, (ACCENT >> 8) & 0xFF, ACCENT & 0xFF, 190);
        pApplet.noFill();
        pApplet.rect(bx, by, badgeW, badgeH, 3);
        pApplet.fill(TEXT_MAIN);
        pApplet.textAlign(PApplet.CENTER, PApplet.CENTER);
        pApplet.textSize(10);
        pApplet.text("历史回放", bx + badgeW * 0.5f, by + badgeH * 0.5f + 0.5f);

        pApplet.stroke(PANEL_STROKE);
        pApplet.line(panelX + 10, panelY + 44, panelX + panelW - 10, panelY + 44);
    }

    private void drawListCard(int x0, int y0, int w0, int h0) {
        pApplet.noStroke();
        pApplet.fill(CARD_BG);
        pApplet.rect(x0, y0, w0, h0, 3);
        pApplet.stroke(CARD_STROKE);
        pApplet.noFill();
        pApplet.rect(x0, y0, w0, h0, 3);

        pApplet.fill(TEXT_SUB);
        pApplet.textFont(p7);
        pApplet.textSize(11);
        pApplet.textAlign(PApplet.LEFT, PApplet.TOP);
        pApplet.text("最近文件", x0 + 8, y0 + 3);

        if (playbackMenuList.items.isEmpty()) {
            pApplet.fill(TEXT_SUB);
            pApplet.textFont(p7);
            pApplet.textSize(11);
            pApplet.textAlign(PApplet.LEFT, PApplet.TOP);
            pApplet.text("暂无回放历史，点击右上角按钮选择文件。", x0 + 12, y0 + 34);
        }
    }

    private void drawGradientBackground(int gx, int gy, int gw, int gh) {
        for (int i = 0; i < gh; i++) {
            float t = i / (float) Math.max(1, gh - 1);
            int c = lerpRgb(BG_TOP, BG_BOTTOM, t);
            pApplet.stroke((c >> 16) & 0xFF, (c >> 8) & 0xFF, c & 0xFF);
            pApplet.line(gx, gy + i, gx + gw, gy + i);
        }
    }

    private int lerpRgb(int c1, int c2, float t) {
        int r1 = (c1 >> 16) & 0xFF;
        int g1 = (c1 >> 8) & 0xFF;
        int b1 = c1 & 0xFF;
        int r2 = (c2 >> 16) & 0xFF;
        int g2 = (c2 >> 8) & 0xFF;
        int b2 = c2 & 0xFF;
        int r = (int) PApplet.lerp(r1, r2, t);
        int g = (int) PApplet.lerp(g1, g2, t);
        int b = (int) PApplet.lerp(b1, b2, t);
        return (r << 16) | (g << 8) | b;
    }
}
