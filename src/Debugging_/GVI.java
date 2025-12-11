package Debugging_;

public class GVI {

    //set true if you want more verbosity in console.. verbosePrint("print_this_thing") is used to output feedback when isVerbose = true
    public static boolean isVerbose = false;

    //Help Widget initiation

    //use signPost(String identifier) to print 'identifier' text and time since last signPost() for debugging latency/timing issues
    public static boolean printSignPosts = true;
    public static float millisOfLastSignPost = 0.0F;
    public static float millisSinceLastSignPost = 0.0F;

}
