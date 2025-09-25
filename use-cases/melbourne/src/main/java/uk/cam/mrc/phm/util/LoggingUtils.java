package uk.cam.mrc.phm.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for configuring logging functionality across MITO applications.
 * Provides methods to set up file logging to capture all log output to text files.
 */
public class LoggingUtils {

    private static final Logger logger = LogManager.getLogger(LoggingUtils.class);
    private static String currentLogFilePath = null;

    /**
     * Configure file logging to write to the specified output directory.
     * Creates a timestamped log file that captures all log output in addition to console output.
     * This should be called as early as possible in the application startup.
     *
     * @param outputDirectory The directory where the log file should be created
     * @param logFilePrefix The prefix for the log file name
     * @return The path to the created log file, or null if configuration failed
     */
    public static String configureFileLogging(String outputDirectory, String logFilePrefix) {
        try {
            // Create output directory if it doesn't exist
            File outputDir = new File(outputDirectory);
            if (!outputDir.exists()) {
                boolean created = outputDir.mkdirs();
                if (!created) {
                    System.err.println("Failed to create output directory: " + outputDirectory);
                    return null;
                }
            }

            // Create log file with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String logFileName = logFilePrefix + "_" + timestamp + ".txt";
            File logFile = new File(outputDir, logFileName);

            // Get the current logger configuration
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            Configuration config = context.getConfiguration();

            // Create pattern layout for log formatting (matches console output)
            PatternLayout layout = PatternLayout.newBuilder()
                    .withConfiguration(config)
                    .withPattern("%d{yyyy-MM-dd HH:mm:ss} [%level] %logger{36} - %msg%n")
                    .build();

            // Create file appender with unique name to avoid conflicts
            String appenderName = "FileAppender_" + timestamp;
            FileAppender fileAppender = FileAppender.newBuilder()
                    .withFileName(logFile.getAbsolutePath())
                    .withLayout(layout)
                    .withName(appenderName)
                    .withAppend(false)  // Start fresh
                    .build();

            // Start the appender
            fileAppender.start();

            // Add file appender to root logger
            config.addAppender(fileAppender);
            config.getRootLogger().addAppender(fileAppender, null, null);

            // Update logger configuration
            context.updateLoggers();

            currentLogFilePath = logFile.getAbsolutePath();

            return currentLogFilePath;

        } catch (Exception e) {
            System.err.println("Failed to configure file logging: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Configure file logging with default prefix "mito_log".
     *
     * @param outputDirectory The directory where the log file should be created
     * @return The path to the created log file, or null if configuration failed
     */
    public static String configureFileLogging(String outputDirectory) {
        return configureFileLogging(outputDirectory, "mito_log");
    }

    /**
     * Configure file logging for a specific scenario with descriptive filename.
     *
     * @param outputDirectory The directory where the log file should be created
     * @param scenarioName The scenario name to include in the log filename
     * @param scenarioYear The scenario year to include in the log filename
     * @return The path to the created log file, or null if configuration failed
     */
    public static String configureFileLoggingForScenario(String outputDirectory, String scenarioName, String scenarioYear) {
        String logPrefix = "mito_" + scenarioName + "_" + scenarioYear;
        return configureFileLogging(outputDirectory, logPrefix);
    }

    /**
     * Get the current log file path if file logging has been configured.
     *
     * @return The current log file path, or null if not configured
     */
    public static String getCurrentLogFilePath() {
        return currentLogFilePath;
    }

    /**
     * Move a log file from its current location to a final output directory.
     * This is useful when logging is initially configured to a temporary location
     * and needs to be moved to the proper output directory later.
     *
     * @param currentLogPath The current path of the log file
     * @param finalOutputDir The final output directory where the log should be moved
     * @param scenarioName The scenario name to include in the final log filename
     * @param scenarioYear The scenario year to include in the final log filename
     * @return The path to the moved log file, or the original path if move failed
     */
    public static String moveLogFileToFinalLocation(String currentLogPath, String finalOutputDir, String scenarioName, String scenarioYear) {
        if (currentLogPath == null) {
            return null;
        }

        try {
            // Create final output directory if it doesn't exist
            File finalDir = new File(finalOutputDir);
            if (!finalDir.exists()) {
                boolean created = finalDir.mkdirs();
                if (!created) {
                    System.err.println("Failed to create final output directory: " + finalOutputDir);
                    return currentLogPath;
                }
            }

            // Generate final log file name with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String finalLogFileName = "mito_" + scenarioName + "_" + scenarioYear + "_" + timestamp + ".txt";
            File finalLogFile = new File(finalDir, finalLogFileName);

            // Move the current log file to final location
            File currentLogFile = new File(currentLogPath);
            if (currentLogFile.exists()) {
                java.nio.file.Files.move(currentLogFile.toPath(), finalLogFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                // Update the currentLogFilePath to reflect the new location
                currentLogFilePath = finalLogFile.getAbsolutePath();

                return finalLogFile.getAbsolutePath();
            } else {
                System.err.println("Current log file does not exist: " + currentLogPath);
            }
        } catch (Exception e) {
            System.err.println("Failed to move log file to final location: " + e.getMessage());
        }

        return currentLogPath; // Return original path if move failed
    }
}
