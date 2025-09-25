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

    /**
     * Configure file logging to write to the specified output directory.
     * Creates a timestamped log file that captures all log output in addition to console output.
     *
     * @param outputDirectory The directory where the log file should be created
     * @param logFilePrefix The prefix for the log file name (default: "mito_log")
     * @return The path to the created log file, or null if configuration failed
     */
    public static String configureFileLogging(String outputDirectory, String logFilePrefix) {
        try {
            // Create output directory if it doesn't exist
            File outputDir = new File(outputDirectory);
            if (!outputDir.exists()) {
                boolean created = outputDir.mkdirs();
                if (!created) {
                    logger.warn("Failed to create output directory: {}", outputDirectory);
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

            // Create pattern layout for log formatting
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
                    .withAppend(true)
                    .build();

            // Start the appender
            fileAppender.start();

            // Add appender to root logger
            config.addAppender(fileAppender);
            config.getRootLogger().addAppender(fileAppender, null, null);

            // Update logger configuration
            context.updateLoggers();

            String logFilePath = logFile.getAbsolutePath();
            logger.info("Log file configured: {}", logFilePath);
            logger.info("All log output will be written to this file in addition to console output");

            return logFilePath;

        } catch (Exception e) {
            logger.warn("Failed to configure file logging: {}", e.getMessage(), e);
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
}
