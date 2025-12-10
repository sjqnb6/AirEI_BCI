package Extras_;

import Globel.GUI;
import PopupMessage_.PopupMessage;
import org.apache.commons.lang3.SystemUtils;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PShape;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static Debugging_.GF.output;
import static Debugging_.GF.verbosePrint;
import static processing.core.PApplet.*;

public class GF {

    /**
     * @description Helper function to determine if the system is linux or not.
     * @return {boolean} true if os is linux, false otherwise.
     */
    public static boolean isLinux() {
        return System.getProperty("os.name").toLowerCase().indexOf("linux") > -1;
    }

    /**
     * @description Helper function to determine if the system is windows or not.
     * @return {boolean} true if os is windows, false otherwise.
     */
    public static  boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().indexOf("windows") > -1;
    }

    /**
     * @description Helper function to determine if the system is macOS or not.
     * @return {boolean} true if os is windows, false otherwise.
     */
    public static  boolean isMac() {
        return !isWindows() && !isLinux();
    }

    public static void checkIsMacFullDetail() {
        StringBuilder response = new StringBuilder("MacOS Details: ");
        if (isMacOsLowerThanCatalina()) {
            response.append("MacOS Mojave or earlier");
        } else if (isMacOsBigSur()) {
            response.append("MacOS Big Sur");
        } else if (isMacOsMonterey()) {
            response.append("MacOS Monterey");
        } else {
            response.append("MacOS Catalina");
        }
        println(response);
    }

    // For a full list of modern Mac OS versions, visit https://en.wikipedia.org/wiki/MacOS_version_history
    public static boolean isMacOsLowerThanCatalina() {
        int[] versionInfo = fetchAndParseMacOsVersion();
        return versionInfo[0] <= 10 && versionInfo[1] < 15;
    }

    public static boolean isMacOsBigSur() {
        int[] versionInfo = fetchAndParseMacOsVersion();
        //This should return 11, but there was a recently discovered bug in Java 8 -- https://bugs.openjdk.java.net/browse/JDK-8274907
        int[] javaInfo = fetchAndParseJavaVersion();
        boolean usingJava8_202 = javaInfo[0] == 1 && javaInfo[1] == 8 && javaInfo[2] == 202;
        if (usingJava8_202) {
            return versionInfo[0] == 10 && versionInfo[1] == 16;
        } else {
            return versionInfo[0] == 11;
        }
    }

    public static boolean isMacOsMonterey() {
        int[] versionInfo = fetchAndParseMacOsVersion();
        return versionInfo[0] == 12;
    }

    public static  String getOperatingSystemVersion() {
        return System.getProperty("os.version");
    }

    public static  String getOperatingSystemName() {
        return System.getProperty("os.name");
    }

    public static  int[] fetchAndParseMacOsVersion() {
        if (!isMac()) {
            println("Oops! Please only call this method on MacOS");
            return null;
        }
        final String version = getOperatingSystemVersion();
        final String[] splitStrings = split(version, '.');
        int[] versionVals = new int[splitStrings.length];
        for (int i = 0; i < splitStrings.length; i++) {
            versionVals[i] = Integer.valueOf(splitStrings[i]);
        }
        return versionVals;
    }

    public static int[] fetchAndParseJavaVersion() {
        final String version = System.getProperty("java.version");
        final String[] splitStrings = split(version, '.');
        int[] versionVals = new int[splitStrings.length];
        versionVals[0] = Integer.valueOf(splitStrings[0]);
        versionVals[1] = Integer.valueOf(splitStrings[1]);
        final String[] minorVersion = split(splitStrings[2], "_");
        versionVals[2] = Integer.valueOf(minorVersion[minorVersion.length - 1]);
        return versionVals;
    }

    //BrainFlow only supports Windows 8 and 10. This will help with OpenBCI support tickets. #964
    public static void checkIsOldVersionOfWindowsOS(GUI MAIN) {
        boolean isOld = SystemUtils.IS_OS_WINDOWS_7 || SystemUtils.IS_OS_WINDOWS_VISTA || SystemUtils.IS_OS_WINDOWS_XP;
        if (isOld) {
            PopupMessage msg = new PopupMessage(MAIN, "Old Windows OS Detected", "OpenBCI GUI v5 and BrainFlow are made for 64-bit Windows 8, 8.1, and 10. Please update your OS, computer, or revert to GUI v4.2.0.");
        }
    }

    //Sanity check for 64-bit Java for Windows users #964
    public static void checkIs64BitJava(GUI MAIN) {
        boolean is64Bit = System.getProperty("sun.arch.data.model").indexOf("64") >= 0;
        if (!is64Bit) {
            PopupMessage msg = new PopupMessage(MAIN, "32-bit Java Detected", "OpenBCI GUI v5 and BrainFlow are made for 64-bit Java (Windows, Linux, and Mac). Please update your OS, computer, Processing IDE, or revert to GUI v4 or earlier.");
        }
    }
    /**
     * Determines if elevated rights are required to install/uninstall the application.
     *
     * @return <code>true</code> if elevation is needed to have administrator permissions, <code>false</code> otherwise.
     */
    public static boolean isElevationNeeded() {
        return isElevationNeeded(null);
    }
    /**
     * Determines if elevated rights are required to install/uninstall the application.
     *
     * @param path the installation path, or <tt>null</tt> if the installation path is unknown
     * @return <tt>true</tt> if elevation is needed to have administrator permissions, <tt>false</tt> otherwise.
     */
    public static boolean isElevationNeeded(String path) {
        boolean result;
        if (isWindows()) {
            if (path != null) {
                // use the parent path, as that needs to be written to in order to delete the tree
                path = new File(path).getParent();
            }
            if (path == null || path.trim().length() == 0) {
                path = getWindowsProgramFiles();
            }
            result = !canWrite(path);
        } else {
            if (path != null) {
                result = !canWrite(path);
            } else {
                if (isMac()) {
                    //Mac user name is never simply "root"
                    return false;
                }
                result = !System.getProperty("user.name").equals("root");
            }
        }
        return result;
    }
    /**
     * Determine if user has administrative privileges.
     *
     * @return
     */
    public static boolean isAdminUser() {
        if (isMac()) {
            return true;
        }
        if (isWindows()) {
            try {
                String NTAuthority = "HKU\\S-1-5-19";
                String command = "reg query \""+ NTAuthority + "\"";
                Process p = Runtime.getRuntime().exec(command);
                p.waitFor();
                return (p.exitValue() == 0);
            } catch (Exception e) {
                return canWrite(getWindowsProgramFiles());
            }
        }
        try {
            String command = "id -u";
            Process p = Runtime.getRuntime().exec(command);
            p.waitFor();
            InputStream stdIn = p.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stdIn));
            String value = bufferedReader.readLine();
            return value.equals("0");
        } catch (Exception e) {
            return System.getProperty("user.name").equals("root");
        }
    }
    /**
     * Tries to determine the Windows Program Files directory.
     *
     * @return the Windows Program Files directory
     */
    public static String getWindowsProgramFiles() {
        String path = System.getenv("ProgramFiles");
        if (path == null) {
            path = "C:\\Program Files";
        }
        return path;
    }
    /**
     * Determines if the specified path can be written to.
     *
     * @param path the path to check
     * @return <tt>true</tt> if the path can be written to, otherwise <tt>false</tt>
     */
    public static boolean canWrite(String path) {
        File file = new File(path);
        boolean canWrite = file.canWrite();
        if (canWrite) {
            // make sure that the path can actually be written to, for IZPACK-727
            try {
                File test = File.createTempFile(".izpackwritecheck", null, file);
                if (!test.delete()) {
                    test.deleteOnExit();
                }
            } catch (IOException exception) {
                canWrite = false;
            }
        }
        return canWrite;
    }

    //compute the standard deviation
    public static float std(float[] data) {
        //calc mean
        float ave = mean(data);

        //calc sum of squares relative to mean
        float val = 0;
        for (int i=0; i < data.length; i++) {
            val += pow(data[i]-ave,2);
        }

        // divide by n to make it the average
        val /= data.length;

        //take square-root and return the standard
        return (float)Math.sqrt(val);
    }

    public static float mean(float[] data) {
        return mean(data,data.length);
    }

    // cp5 textfields adds garbage chars to text field and they are invisible
    public static String dropNonPrintableChars(String myString)
    {
        StringBuilder newString = new StringBuilder(myString.length());
        for (int offset = 0; offset < myString.length();)
        {
            int codePoint = myString.codePointAt(offset);
            offset += Character.charCount(codePoint);

            // Replace invisible control characters and unused code points
            switch (Character.getType(codePoint))
            {
                case Character.CONTROL:     // \p{Cc}
                case Character.FORMAT:      // \p{Cf}
                case Character.PRIVATE_USE: // \p{Co}
                case Character.SURROGATE:   // \p{Cs}
                case Character.UNASSIGNED:  // \p{Cn}
                    break;
                default:
                    newString.append(Character.toChars(codePoint));
                    break;
            }
        }
        String res = newString.toString();
        res = res.replace("\r", "");
        res = res.replace("\n", "");
        res = res.replace("\t", "");
        return res;
    }

    public static String getIpAddrFromStr(String strWithIP) {
        String temp = dropNonPrintableChars(strWithIP);
        String IPADDRESS_PATTERN =
                "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";
        Pattern pattern = Pattern.compile(IPADDRESS_PATTERN);
        Matcher matcher = pattern.matcher(temp);
        if (matcher.find()) {
            return matcher.group();
        } else{
            output("Invalid Ip address");
            println("Provided Ip address doesn't match regexp");
            return "";
        }
    }

    public static float getFontStringHeight(PFont font, String text) {
        if (text == null) {
            return 0;
        }
        float minY = Float.MAX_VALUE;
        float maxY = Float.NEGATIVE_INFINITY;
        for (Character c : text.toCharArray()) {
            PShape character = font.getShape(c); // create character vector
            for (int i = 0; i < character.getVertexCount(); i++) {
                minY = min(character.getVertex(i).y, minY);
                maxY = max(character.getVertex(i).y, maxY);
            }
        }
        return maxY - minY;
    }

//////////////////////////////////////////////////
//
// Some functions to implement some math and some filtering.  These functions
// probably already exist in Java somewhere, but it was easier for me to just
// recreate them myself as I needed them.
//
// Created: Chip Audette, Oct 2013
//
    //////////////////////////////////////////////////

    public static int findMax(float[] data) {
        float maxVal = data[0];
        int maxInd = 0;
        for (int I=1; I<data.length; I++) {
            if (data[I] > maxVal) {
                maxVal = data[I];
                maxInd = I;
            }
        }
        return maxInd;
    }

    public static float mean(float[] data, int Nback) {
        return sum(data,Nback)/Nback;
    }

    public static float sum(float[] data) {
        return sum(data, data.length);
    }

    public static float sum(float[] data, int Nback) {
        float sum = 0;
        if (Nback > 0) {
            for (int i=(data.length)-Nback; i < data.length; i++) {
                sum += data[i];
            }
        }
        return sum;
    }

    public static float calcDotProduct(float[] data1, float[] data2) {
        int len = min(data1.length, data2.length);
        float val= 0.0F;
        for (int I=0;I<len;I++) {
            val+=data1[I]*data2[I];
        }
        return val;
    }


    public static float log10(float val) {
        return (float)Math.log10(val);
    }

    public static float log10(int val) {
        return (float)Math.log10(val);
    }

    public static float filterWEA_1stOrderIIR(float[] filty, float learn_fac, float filt_state) {
        float prev = filt_state;
        for (int i=0; i < filty.length; i++) {
            filty[i] = prev*(1-learn_fac) + filty[i]*learn_fac;
            prev = filty[i]; //save for next time
        }
        return prev;
    }

    public static void removeMean(float[] filty, int Nback) {
        float meanVal = mean(filty,Nback);
        for (int i=0; i < filty.length; i++) {
            filty[i] -= meanVal;
        }
    }

    public static double[] floatToDoubleArray(float[] array) {
        double[] res = new double[array.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = (double)array[i];
        }
        return res;
    }

    public static float[] doubleToFloatArray(double[] array) {
        float[] res = new float[array.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = (float)array[i];
        }
        return res;
    }

    public static void floatToDoubleArray(float[] array, double[] res) {
        for (int i = 0; i < array.length; i++) {
            res[i] = (double)array[i];
        }
    }

    public static void doubleToFloatArray(double[] array, float[] res) {
        for (int i = 0; i < array.length; i++) {
            res[i] = (float)array[i];
        }
    }

    // shortens a string to a given width by adding [...] in the middle
// make sure to pass the right font for accurate sizing
    public static String shortenString(GUI pApplet, String str, float maxWidth, PFont font) {
        if (pApplet.textWidth(str) <= maxWidth) {
            return str;
        }

        pApplet.textFont(font); // set font for accurate sizing
        int firstIndex = 0; // forward iterator
        int lastIndex = str.length()-1; // reverse iterator
        float spaceLeft = maxWidth - pApplet.textWidth("..."); // account for the space taken by "..."

        while (firstIndex < lastIndex && spaceLeft >= 0.f) {
            spaceLeft -= pApplet.textWidth(str.charAt(firstIndex)); // subtract space taken by first char
            spaceLeft -= pApplet.textWidth(str.charAt(lastIndex)); // and last char

            // move interators inward
            firstIndex ++;
            lastIndex --;
        }

        String s1 = str.substring(0, firstIndex); // firstIndex is excluded here
        String s2 = str.substring(lastIndex + 1, str.length()); // manually exclude lastIndex
        return s1 + "..." + s2;
    }

    public static int lerpInt(long first, long second, float bias) {
        return round(lerp(first, second, bias));
    }

    public static int[] range(int first, int second) {
        int total = abs(first-second);
        int[] result = new int[total];

        for(int i=0; i<total; i++) {
            int newNumber = first;
            if(first > second) {
                newNumber -= i;
            }
            else {
                newNumber += i;
            }

            result[i] = newNumber;
        }

        return result;
    }


    public static  boolean pingWebsite(String url) {
        int code = 200;
        try {
            URL siteURL = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) siteURL.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(2000);
            connection.connect();

            code = connection.getResponseCode();
            if (code == 200) {
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            return false;

        }
    }


    public static  BufferedReader createBufferedReader(String filepath) {
        File file;
        BufferedReader reader;
        try {
            file = new File(filepath);
            reader = new BufferedReader(new FileReader(file));
            return reader;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    //Used to check for one string in a text file
//Uses a buffered reader for this method so that we do not load entire file to memory
    public static boolean checkTextFileForInfo(String path, String infoToCheck, int maxLinesToCheck) {
        verbosePrint("Checking " + path + " for " + infoToCheck);
        String strCurrentLine;
        int lineCounter = 0;
        BufferedReader reader = createBufferedReader(path);
        try {
            while (lineCounter < maxLinesToCheck) {
                strCurrentLine = reader.readLine();
                verbosePrint(strCurrentLine);
                if (strCurrentLine.equals(infoToCheck)) {
                    return true;
                }
                lineCounter++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return false;
    }

}
