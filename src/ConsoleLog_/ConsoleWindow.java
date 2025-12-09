package ConsoleLog_;

import Globel.GUI;
import controlP5.ControlP5;
import controlP5.Textarea;
import processing.awt.PSurfaceAWT;
import processing.core.PApplet;
import processing.core.PFont;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.File;
import java.io.IOException;

public class ConsoleWindow extends PApplet implements Runnable {
    private static ConsoleWindow instance = null;

    PApplet logApplet;

    private ControlP5 cp5;
    private Textarea consoleTextArea;
    private ClipHelper clipboardCopy;

    private final int headerHeight = 42;
    private final int defaultWidth = 620;
    private final int defaultHeight = 620;
    private final int buttonWidth = 142;
    private final int buttonHeight = 34;

    //for screen resizing
    private boolean screenHasBeenResized = false;
    private float timeOfLastScreenResize = 0;
    private int widthOfLastScreen = defaultWidth;
    private int heightOfLastScreen = defaultHeight;

    public static void display() {        // enforce only one Console Window
        if (instance == null) {
            instance = new ConsoleWindow();
            Thread t = new Thread(instance);
            t.start();
        }
    }

    @Override
    public void run() {
        PApplet.runSketch(new String[] {instance.getClass().getSimpleName()}, instance);
    }

    private ConsoleWindow() {
        super();
    }

    public void settings() {
        size(defaultWidth, defaultHeight);
    }

    public void setup(GUI MAIN) {

        logApplet = this;

        surface.setAlwaysOnTop(false);
        surface.setResizable(false);

        Frame frame = ( (PSurfaceAWT.SmoothCanvas) ((PSurfaceAWT)surface).getNative()).getFrame();
        frame.toFront();
        frame.requestFocus();

        clipboardCopy = new ClipHelper();
        cp5 = new ControlP5(this);
        PFont textAreaFont = createFont("Arial", 12, true);
        consoleTextArea = cp5.addTextarea("ConsoleWindow")
                .setPosition(0, headerHeight)
                .setSize(width, height - headerHeight)
                .setFont(textAreaFont)
                .setLineHeight(18)
                .setColor(color(242))
                .setColorBackground(color(42, 100))
                .setColorForeground(color(42, 100))
                .setScrollBackground(color(70, 100))
                .setScrollForeground(color(144, 100))
        ;

        // register this console's Textarea with the output stream object
        GUI.outputStream.registerTextArea(consoleTextArea);

        int cW = (int)(width/4);
        int bX = (int)((cW - buttonWidth) / 2);
        createConsoleLogButton("openLogFileAsText", "Open Log as Text (F)", bX);
        bX += cW;
        createConsoleLogButton("copyFullTextToClipboard", "Copy Full Text (C)", bX);
        bX += cW;
        createConsoleLogButton("copyLastLineToClipboard", "Copy Last Line (L)", bX);
        bX += cW;
        createConsoleLogButton("jumpToLastLine", "Jump to Last Line (J)", bX);
    }

    void createConsoleLogButton (String bName, String bText, int x) {
        int y = 4;  // vertical position for button
        PFont buttonFont = createFont("Arial", 14, true);
        cp5.addButton(bName)
                .setPosition(x, y)
                .setSize(buttonWidth, buttonHeight)
                .setColorLabel(color(255))
                .setColorForeground(color(31, 69, 110)) //openbci blue
                .setColorBackground(color(144, 100));
        cp5.getController(bName)
                .getCaptionLabel()
                .setFont(buttonFont)
                .toUpperCase(false)
                .setText(bText);
    }

    public void draw() {
        clear();
        scene();
        cp5.draw();
        //checks if the screen is resized, similar to main GUI window
        screenResized();
    }

    void screenResized() {
        if (this.widthOfLastScreen != width || this.heightOfLastScreen != height) {
            //println("ConsoleLog: RESIZED");
            this.screenHasBeenResized = true;
            this.timeOfLastScreenResize = millis();
            this.widthOfLastScreen = width;
            this.heightOfLastScreen = height;
        }
        if (this.screenHasBeenResized) {
            //setGraphics() is very important, it lets the cp5 elements know where the origin is.
            //Without this, cp5 elements won't work after screen is resized.
            //This also happens in most widgets when the main GUI window is resized.
            logApplet = this;
            cp5.setGraphics(logApplet, 0, 0);

            imposeMinConsoleLogDimensions();
            // dynamically resize text area to fit widget
            consoleTextArea.setSize(width, height - headerHeight);
            // update button positions when screen width changes
            updateButtonPositions();
        }
        //re-initialize console log if screen has been resized and it's been more than 1 seccond (to prevent reinitialization happening too often)
        if (this.screenHasBeenResized == true && (millis() - this.timeOfLastScreenResize) > 1000) {
            this.screenHasBeenResized = false;
        }
    }

    void scene() {
        background(42);
        fill(42);
        rect(0, 0, width, headerHeight);
    }

    public void keyReleased() {
        if (key == 'c') {
            copyFullTextToClipboard();
        } else if (key == 'f') {
            openLogFileAsText();
        } else if (key == 'l') {
            copyLastLineToClipboard();
        } else if (key == 'j' ) {
            jumpToLastLine();
        }

    }

    public void keyPressed() {
        if (key == CODED) {
            if (keyCode == UP) {
                consoleTextArea.scrolled(-5);
            } else if (keyCode == DOWN) {
                consoleTextArea.scrolled(5);
            }
        }
    }



    public void mousePressed() {
    }

    public void mouseReleased() {

    }

    void openLogFileAsText() {
        try {
            println("ConsoleLog: Opening console log as text file!");
            File file = new File(GUI.outputStream.getFilePath());
            Desktop desktop = Desktop.getDesktop();
            if (file.exists()) {
                desktop.open(file);
            } else {
                println("ConsoleLog: ERROR - Unable to open console log as text file...");
            }
        } catch (IOException e) {}
    }

    void copyFullTextToClipboard() {
        println("ConsoleLog: Copying console log to clipboard!");
        String stringToCopy = GUI.outputStream.getFullLog();
        String formattedCodeBlock = "```\n" + stringToCopy + "\n```";
        clipboardCopy.copyString(formattedCodeBlock);
    }

    void copyLastLineToClipboard() {
        clipboardCopy.copyString(GUI.outputStream.getLastLine());
        println("ConsoleLog: Previous line copied to clipboard.");
    }

    void jumpToLastLine() {
        consoleTextArea.scroll(1.0F);
    }

    void updateButtonPositions() {
        int cW = width / 4;
        int bX = (cW - buttonWidth) / 2;
        int bY = 4;
        cp5.getController("openLogFileAsText").setPosition(bX, bY);
        bX += cW;
        cp5.getController("copyFullTextToClipboard").setPosition(bX, bY);
        bX += cW;
        cp5.getController("copyLastLineToClipboard").setPosition(bX, bY);
        bX += cW;
        cp5.getController("jumpToLastLine").setPosition(bX, bY);
    }

    void imposeMinConsoleLogDimensions() {
        //impose minimum gui dimensions
        int minHeight = (int)(defaultHeight/2);
        if (width < defaultWidth || height < minHeight) {
            int _w = (width < defaultWidth) ? defaultWidth : width;
            int _h = (height < minHeight) ? minHeight : height;
            surface.setSize(_w, _h);
        }
    }

    public void exit() {
        println("ConsoleLog: Console closed!");
        instance = null;
        dispose();
    }

    // ===============================================================
    // CLIPHELPER OBJECT CLASS
    class ClipHelper {
        Clipboard clipboard;

        ClipHelper() {
            getClipboard();
        }

        void getClipboard () {
            // this is our simple thread that grabs the clipboard
            Thread clipThread = new Thread() {
                public void run() {
                    clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                }
            };

            // start the thread as a daemon thread and wait for it to die
            if (clipboard == null) {
                try {
                    clipThread.setDaemon(true);
                    clipThread.start();
                    clipThread.join();
                }
                catch (Exception e) {}
            }
        }

        void copyString (String data) {
            copyTransferableObject(new StringSelection(data));
        }

        void copyTransferableObject (Transferable contents) {
            getClipboard();
            clipboard.setContents(contents, null);
        }

        String pasteString () {
            String data = null;
            try {
                data = (String)pasteObject(DataFlavor.stringFlavor);
            }
            catch (Exception e) {
                println("ConsoleLog: Error getting String from clipboard: " + e);
            }
            return data;
        }

        Object pasteObject (DataFlavor flavor)
                throws UnsupportedFlavorException, IOException
        {
            Object obj = null;
            getClipboard();

            Transferable content = clipboard.getContents(null);
            if (content != null)
                obj = content.getTransferData(flavor);

            return obj;
        }
    }//end class
}//end class
