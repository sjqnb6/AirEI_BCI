package Debugging_;

import processing.core.PApplet;

import static Debugging_.GVI.*;
import static Globel.GUI.helpWidget;
//import static Debugging_.GVI.*;
import static processing.core.PApplet.println;
import Globel.GUI;
public class GF {

    public static void verbosePrint(String _string) {
        if (isVerbose) {
            println(_string);
        }
    }

    public static void output(String _output) {
        output(_output, OutputLevel.DEFAULT);
    }

    public static void output(String _output, OutputLevel level) {
        helpWidget.output(_output, level);
    }

    public static void outputError(String _output) {
        output(_output, OutputLevel.ERROR);
    }

    public static void outputInfo(String _output) {
        output(_output, OutputLevel.INFO);
    }

    public static void outputSuccess(String _output) {
        output(_output, OutputLevel.SUCCESS);
    }

    public static void outputWarn(String _output) {
        output(_output, OutputLevel.WARN);
    }

// created 2/10/16 by Conor Russomanno to dissect the aspects of the GUI that are slowing it down
// here I will create methods used to identify where there are inefficiencies in the code
// note to self: make sure to check the frameRate() in setup... switched from 16 to 30... working much faster now... still a useful method below.
// --------------------------------------------------------------  START -------------------------------------------------------------------------------

    //method for printing out an ["indentifier"][millisSinceLastSignPost] for debugging purposes... allows us to look at what is taking too long.
    public static void signPost(PApplet pApplet, String identifier) {
        if (printSignPosts) {
            millisSinceLastSignPost = pApplet.millis() - millisOfLastSignPost;
            println("SIGN POST: [" + identifier + "][" + millisSinceLastSignPost + "]");
            millisOfLastSignPost = pApplet.millis();
        }
    }
}
