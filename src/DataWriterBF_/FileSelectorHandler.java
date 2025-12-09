package DataWriterBF_;

import DataLogger_.DataLogger;

import java.io.File;

public class FileSelectorHandler {

    private final DataLogger dataLogger;

    public FileSelectorHandler(DataLogger dataLogger) {
        this.dataLogger = dataLogger;
    }

    // Called when user selects a folder from a dialog box
    public void onFolderSelected(File selection) {
        if (selection == null) {
            System.err.println("BrainFlow File Streamer: Window was closed or the user hit cancel. Please select a new file location or choose Default.");
            dataLogger.setBfWriterFolder(null, null);
            return;
        }

        File directory = new File(selection.getAbsolutePath());
        if (!directory.exists()) {
            boolean created = directory.mkdirs(); // Ensure full path is created
            if (!created) {
                System.err.println("Error: Failed to create directory: " + selection.getAbsolutePath());
                return;
            }
        }

        System.out.println("DataLogging: onFolderSelected: User selected " + selection.getAbsolutePath());
        dataLogger.setBfWriterFolder(selection.getName(), selection.getAbsolutePath());
    }
}

