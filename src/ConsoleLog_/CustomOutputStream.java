package ConsoleLog_;

import controlP5.Textarea;
import processing.data.StringList;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

//import static GUI.GGVI.directoryManager;
import static Globel.GUI.directoryManager;
import static processing.core.PApplet.createWriter;
import static processing.core.PApplet.join;


public class CustomOutputStream extends PrintStream {

    private StringList data;
    private PrintWriter fileOutput;
    private Textarea textArea;
    private final String filePath = directoryManager.getConsoleDataPath()+"Console_"+directoryManager.getFileNameDateTime()+".txt";

    public CustomOutputStream(OutputStream out) {
        super(out);
        data = new StringList();
        // initialize the printwriter just in case the file open fails
        fileOutput = new PrintWriter(out);

        // create log file
        try {
            fileOutput = createWriter(new File(filePath));
        }
        catch (RuntimeException e) {
            println("Error! Failed to open " + filePath + " for write.");
            println(e);
        }
    }

    public void println(String string) {
        string += "\n";
        super.print(string);  // don't call super.println() here, you'll get double prints

        // add to array
        data.append(string);

        // print to file
        fileOutput.print(string);
        fileOutput.flush();

        // add to text area, if registered
        if (textArea != null) {
            textArea.append(string);
        }
    }

    public void print(String string) {
        super.print(string);
        string += "\n"; // TODO: shouldn't have to do this, but exceptions were printing on one line. investigate?

        // add to array
        data.append(string);

        // print to file
        fileOutput.print(string);
        fileOutput.flush();

        // add to text area, if registered
        if (textArea != null) {
            textArea.append(string);
        }
    }

    public void registerTextArea(Textarea area) {
        textArea = area;
        textArea.setText(getFullLog());
    }

    public String getFilePath() {
        return filePath;
    }

    public String getLastLine() {
        return data.get(data.size()-1);
    }

    public String getFullLog() {
        return join(data.array(), "");
    }
}
