package DirectoryManager_;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static processing.core.PApplet.println;

public class DirectoryManager {
    private static final String BRAND_NAME = "AirEIBCI";
    private static final String SAMPLE_DATA_FOLDER = "Sample_Data";
    private static final String CURRENT_SAMPLE_DATA_FILE = "AirEIBCI-v6-meditation.txt";

    private final String guiDataPath = System.getProperty("user.home") + File.separator + "Documents" + File.separator + BRAND_NAME + File.separator;
    private final String recordingsPath = guiDataPath + "Recordings" + File.separator;
    private final String settingsPath = guiDataPath + "Settings" + File.separator;
    private final String consoleDataPath = guiDataPath + "Console_Data" + File.separator;
    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    public DirectoryManager() {

    }

    public String getFileNameDateTime() {
        return dateFormat.format(new Date());
    }

    public String getGuiDataPath() {
        return guiDataPath;
    }

    public String getRecordingsPath() {
        return recordingsPath;
    }

    public String getSettingsPath() {
        return settingsPath;
    }

    public String getConsoleDataPath() {
        return consoleDataPath;
    }

    public void init() {
        ensureBaseFolders();
        syncSampleDataFiles();
    }

    private void ensureBaseFolders() {
        ensureDirectory(guiDataPath, "Documents" + File.separator + BRAND_NAME);
        ensureDirectory(recordingsPath, "Documents" + File.separator + BRAND_NAME + File.separator + "Recordings");
        ensureDirectory(settingsPath, "Documents" + File.separator + BRAND_NAME + File.separator + "Settings");
        ensureDirectory(consoleDataPath, "Documents" + File.separator + BRAND_NAME + File.separator + "Console_Data");
        ensureDirectory(guiDataPath + "Screenshots" + File.separator, "Documents" + File.separator + BRAND_NAME + File.separator + "Screenshots");
        ensureDirectory(guiDataPath + SAMPLE_DATA_FOLDER + File.separator, "Documents" + File.separator + BRAND_NAME + File.separator + SAMPLE_DATA_FOLDER);
    }

    private void syncSampleDataFiles() {
        String directoryName = guiDataPath + SAMPLE_DATA_FOLDER + File.separator;
        File directory = new File(directoryName);
        File currentSampleData = new File(directory, CURRENT_SAMPLE_DATA_FILE);

        if (!currentSampleData.exists()) {
            copySampleDataFiles(directory);
        } else {
            println("AirEIBCI::Setup: Sample data is ready in Documents/AirEIBCI/Sample_Data.");
        }
    }

    private void copySampleDataFiles(File directory) {
        println("AirEIBCI::Setup: Copying sample data to Documents/AirEIBCI/Sample_Data");
        directory.mkdirs();
        try {
            File dataDir = new File("data" + File.separator + "EEG_Sample_Data");
            if (!dataDir.exists()) {
                dataDir = new File("EEG_Sample_Data");
            }
            File[] filesFound = dataDir.listFiles();
            if (filesFound == null) {
                println("AirEIBCI::Setup: Sample data source folder was not found.");
                return;
            }
            for (File file : filesFound) {
                if (file.isFile()) {
                    Files.copy(file.toPath(),
                            (new File(directory, brandSampleDataName(file.getName()))).toPath(),
                            StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {
            println("AirEIBCI::Setup: Error trying to copy sample data to Documents.");
        }
    }

    private void ensureDirectory(String absolutePath, String displayPath) {
        File directory = new File(absolutePath);
        if (!directory.exists() && directory.mkdirs()) {
            println("AirEIBCI::Setup: Created " + displayPath);
        }
    }

    private String brandSampleDataName(String rawName) {
        return rawName.replace("OpenBCI_GUI", BRAND_NAME).replace("OpenBCI", BRAND_NAME);
    }

};
