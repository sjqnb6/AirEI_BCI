package ControlPanel_;

import CustomCp5Classes_.MenuList;
import GUI.GUIManager;
import Globel.GUI;
import com.vmichalak.protocol.ssdp.Device;
import com.vmichalak.protocol.ssdp.SSDPClient;
import controlP5.*;
import processing.core.PApplet;
import processing.core.PFont;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import static Debugging_.GF.output;
import static Globel.GF.isWindows;
import static Globel.GUI.*;
import static processing.core.PApplet.println;
import static processing.core.PConstants.*;

public class WifiBox{
    private static final String CUSTOM_WIFI_SSID = "BCI-ESP32";
    private static final String CUSTOM_WIFI_IP = "192.168.4.1";

    public int x, y, w, h, padding; //size and position
    private boolean wifiIsRefreshing = false;
    private ControlP5 wifiBox_cp5;
    MenuList wifiList;
    private Button refreshWifi;
    private Button wifiIPAddressDynamic;
    private Button wifiIPAddressStatic;
    Textfield staticIPAddressTF;
    private int wifiDynamic_x;
    private int wifiStatic_x;
    private int wifiButtons_y;
    private int refreshWifi_x;
    private int refreshWifi_y;
    private GUI MAIN;
    WifiBox(GUI MAIN, int _x, int _y, int _w, int _h, int _padding) {
        this.MAIN = MAIN;
        x = _x;
        y = _y;
        w = _w;
        h = 184 + _padding + 14;
        padding = _padding;

        //Instantiate local cp5 for this box
        wifiBox_cp5 = new ControlP5(MAIN);
        wifiBox_cp5.setGraphics(MAIN, 0,0);
        wifiBox_cp5.setAutoDraw(false);

        wifiDynamic_x = x + padding;
        wifiStatic_x = x + padding*2 + (w-padding*3)/2;
        wifiButtons_y = y + padding*2 + 16;
        createDynamicIPAddressButton("wifiIPAddressDynamicButton", "动态 IP", wifiDynamic_x, wifiButtons_y, (w-padding*3)/2, 24);
        createStaticIPAddressButton("wifiIPAddressStaticButton", "静态 IP", wifiStatic_x, wifiButtons_y, (w-padding*3)/2, 24);

        refreshWifi_x = x + padding;
        refreshWifi_y = y + padding*5 + 72 + 8 + 24;
        createRefreshWifiButton("refreshWifiButton", "开始搜索", refreshWifi_x, refreshWifi_y, w - padding*5, 24);
        createWifiList(wifiBox_cp5, "wifiList", x + padding, y + padding*4 + 8 + 24, w - padding*2, 72 + 8, p3);
        createStaticIPAddressTextfield();
    }

    public void update() {
        wifiList.updateMenu();
        copyPaste.checkForCopyPaste(staticIPAddressTF);
    }

    public void draw() {
        MAIN.pushStyle();
        MAIN.fill(MAIN.boxColor);
        MAIN.stroke(MAIN.boxStrokeColor);
        MAIN.strokeWeight(1);
        MAIN.rect(x, y, w, h);
        MAIN.fill(MAIN.OPENBCI_DARKBLUE);
        MAIN.textFont(p7, 16);
        MAIN.textAlign(LEFT, TOP);
        MAIN.text("WIFI 模块", x + padding, y + padding);
        MAIN.popStyle();

        wifiDynamic_x = x + padding;
        wifiStatic_x = x + padding*2 + (w-padding*3)/2;
        wifiButtons_y = y + padding*2 + 16;
        wifiIPAddressDynamic.setPosition(wifiDynamic_x, wifiButtons_y);
        wifiIPAddressStatic.setPosition(wifiStatic_x, wifiButtons_y);

        if (controlPanel.getWifiSearchStyle() == controlPanel.WIFI_STATIC) {
            MAIN.pushStyle();
            MAIN.fill(MAIN.OPENBCI_DARKBLUE);
            MAIN.textFont(p7, 16);
            MAIN.textAlign(LEFT, TOP);
            MAIN.text("设置IP地址", x + padding, y + h - 24 - 12 - padding*2);
            MAIN.popStyle();
            staticIPAddressTF.setPosition(x + padding, y + h - 24 - padding);
        } else {
            wifiList.setPosition(x + padding, wifiButtons_y + 24 + padding);

            refreshWifi_x = x + padding;
            refreshWifi_y = y + padding*5 + 72 + 8 + 24;
            refreshWifi.setPosition(refreshWifi_x, refreshWifi_y);

            String boardIpInfo = "数据板 IP: ";
            if (wifi_portName != "N/A") { // If user has selected a board from the menulist...
                boardIpInfo += wifi_ipAddress;
            }
            MAIN.pushStyle();
            MAIN.fill(MAIN.OPENBCI_DARKBLUE);
            MAIN.textFont(p7, 16);
            MAIN.textAlign(LEFT, TOP);
            MAIN.text(boardIpInfo, x + w/2 - MAIN.textWidth(boardIpInfo)/2, y + h - padding - 15);
            MAIN.popStyle();

            if (wifiIsRefreshing){
                //Display spinning cog gif
                MAIN.image(loadingGIF_blue, x + 225,  refreshWifi_y + 4, 20, 20);
            } else {
                //Draw small grey circle
                MAIN.pushStyle();
                MAIN.fill(0x999999);
                MAIN.ellipseMode(CENTER);
                MAIN.ellipse(x + 225 + 10, refreshWifi_y + 12, 12, 12);
                MAIN.popStyle();
            }
        }

        wifiBox_cp5.draw();
    }

    public void refreshWifiList() {
        output("WIFI设备搜索中...");
        wifiList.items.clear();
        Thread thread = new Thread(){
            public void run() {
                refreshWifi.getCaptionLabel().setText("搜索中...");
                wifiIsRefreshing = true;
                try {
                    addCustomWifiIfConnected();
                    List<Device> devices = SSDPClient.discover (3000, "urn:schemas-upnp-org:device:Basic:1");
                    if (devices.isEmpty ()) {
                        println("No WIFI Shields found");
                    }
                    for (int i = 0; i < devices.size(); i++) {
                        wifiList.addItem(devices.get(i).getName(), devices.get(i).getIPAddress(), "");
                    }
                    wifiList.updateMenu();
                    outputWifiSearchResult();
                } catch (Exception e) {
                    println("Exception in wifi shield scanning");
                    e.printStackTrace ();
                    addCustomWifiIfConnected();
                    wifiList.updateMenu();
                    outputWifiSearchResult();
                }
                refreshWifi.getCaptionLabel().setText("开始搜索");
                wifiIsRefreshing = false;
            }
        };
        thread.start();
    }

    private void outputWifiSearchResult() {
        if (wifiList.items.isEmpty()) {
            output("WIFI设备搜索完成，未找到设备。");
        } else {
            output("WIFI设备搜索完成，找到 " + wifiList.items.size() + " 个设备。");
        }
    }

    private void addCustomWifiIfConnected() {
        if (isConnectedToCustomWifi() && !wifiListHasItem(CUSTOM_WIFI_SSID)) {
            wifiList.addItem(CUSTOM_WIFI_SSID, CUSTOM_WIFI_IP, "");
            println("Detected custom WiFi board: " + CUSTOM_WIFI_SSID + " at " + CUSTOM_WIFI_IP);
        }
    }

    private boolean wifiListHasItem(String name) {
        for (Map<String, Object> item : wifiList.items) {
            Object headline = item.get("headline");
            if (headline != null && headline.toString().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private boolean isConnectedToCustomWifi() {
        if (!isWindows()) {
            return false;
        }
        try {
            Process process = new ProcessBuilder("netsh", "wlan", "show", "interfaces").start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("SSID") && !trimmed.startsWith("BSSID")) {
                    int colonIndex = trimmed.indexOf(':');
                    if (colonIndex >= 0) {
                        String ssid = trimmed.substring(colonIndex + 1).trim();
                        return CUSTOM_WIFI_SSID.equals(ssid);
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            println("Failed to check current WiFi SSID");
            e.printStackTrace();
        }
        return false;
    }

    private void createDynamicIPAddressButton(String name, String text, int _x, int _y, int _w, int _h) {
        wifiIPAddressDynamic = MAIN.createButton(wifiBox_cp5, name, text, _x, _y, _w, _h);
        wifiIPAddressDynamic.setSwitch(true);
        wifiIPAddressDynamic.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                h = 208;
                controlPanel.setWiFiSearchStyle(controlPanel.WIFI_DYNAMIC);
                println("ControlPanel: Using Dynamic IP address of the WiFi Shield!");
                wifiIPAddressDynamic.setOn();
                wifiIPAddressStatic.setOff();
                staticIPAddressTF.setVisible(false);
                wifiList.setVisible(true);
                refreshWifi.setVisible(true);
                refreshWifi.unlock();
            }
        });
        wifiIPAddressDynamic.setOn();
    }

    private void createStaticIPAddressButton(String name, String text, int _x, int _y, int _w, int _h) {
        wifiIPAddressStatic = MAIN.createButton(wifiBox_cp5, name, text, _x, _y, _w, _h);
        wifiIPAddressStatic.setSwitch(true);
        wifiIPAddressStatic.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                h = 120;
                controlPanel.setWiFiSearchStyle(controlPanel.WIFI_STATIC);
                println("ControlPanel: Using Static IP address of the WiFi Shield!");
                wifiIPAddressDynamic.setOff();
                wifiIPAddressStatic.setOn();
                staticIPAddressTF.setVisible(true);
                wifiList.setVisible(false);
                refreshWifi.setVisible(false);
                refreshWifi.lock();
            }
        });
    }

    private void createRefreshWifiButton(String name, String text, int _x, int _y, int _w, int _h) {
        refreshWifi = MAIN.createButton(wifiBox_cp5, name, text, _x, _y, _w, _h);
        refreshWifi.onRelease(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                refreshWifiList();
            }
        });
    }

    private void createWifiList(ControlP5 _cp5, String name, int _x, int _y, int _w, int _h, PFont font) {
        wifiList = new MenuList(_cp5, name, _w, _h, font, MAIN);
        wifiList.setPosition(_x, _y);
        wifiList.addCallback(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                if (theEvent.getAction() == ControlP5.ACTION_BROADCAST) {
                    Map bob = wifiList.getItem((int)(wifiList.getValue()));
                    wifi_portName = (String)bob.get("headline");
                    wifi_ipAddress = (String)bob.get("subline");
                    output("Selected WiFi Board: " + wifi_portName+ ", WiFi IP Address: " + wifi_ipAddress );
                }
            }
        });
    }

    private void createStaticIPAddressTextfield() {
        staticIPAddressTF = wifiBox_cp5.addTextfield("staticIPAddress")
                .setPosition(x + 90, y + 100)
                .setCaptionLabel("")
                .setSize(w - padding*2, 26)
                .setFont(f2)
                .setFocus(false)
                .setColor(MAIN.color(26, 26, 26))
                .setColorBackground(MAIN.color(255, 255, 255)) // text field bg color
                .setColorValueLabel(MAIN.OPENBCI_DARKBLUE)  // text color
                .setColorForeground(MAIN.OPENBCI_DARKBLUE)  // border color when not selected
                .setColorActive(MAIN.isSelected_color)  // border color when selected
                .setColorCursor(MAIN.color(26, 26, 26))
                .setText(wifi_ipAddress)
                .align(5, 10, 20, 40)
                .setAutoClear(true)
                .setVisible(false);
        //Clear textfield on double click
        staticIPAddressTF.onDoublePress(new CallbackListener() {
            public void controlEvent(CallbackEvent theEvent) {
                output("WiFi静态IP：输入你自定义的WiFi Shield IP地址。");
                staticIPAddressTF.clear();
            }
        });
    }

    public void setDefaultToDynamicIP() {
        h = 208;
        controlPanel.setWiFiSearchStyle(controlPanel.WIFI_DYNAMIC);
        wifiIPAddressDynamic.setOn();
        wifiIPAddressStatic.setOff();
        staticIPAddressTF.setVisible(false);
        wifiList.setVisible(true);
        refreshWifi.setVisible(true);
        refreshWifi.unlock();
    }

    void setStaticIPTextfield(String text) {
        staticIPAddressTF.setText(text);
    }
};
