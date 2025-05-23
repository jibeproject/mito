package uk.cam.mrc.phm.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class parseMEL {

    private static final Logger logger = LogManager.getLogger(parseMEL.class);

    public static int zoneParse(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }
        input = input.trim();
        if (input.startsWith("\"") && input.endsWith("\"")) {
            input = input.substring(1, input.length() - 1);
        }

        if (input.isEmpty()) {
            throw new IllegalArgumentException("Input cannot be an empty string");
        }
        int length = input.length();

        if (length == 11) {
            // Extract first digit and last 6 digits
            String truncated = input.substring(0, 1) + input.substring(5);
            return Integer.parseInt(truncated);
        } else if (length == 7) {
            // Return the 7-digit number as an integer
            return Integer.parseInt(input);
        } else {
            // Log raw input and its length
            logger.info("Raw input: [" + input + "], Length: " + input.length());

            // Log character codes
            StringBuilder charCodes = new StringBuilder();
            for (char c : input.toCharArray()) {
                charCodes.append((int) c).append(" ");
            }
            logger.info("Character codes: [" + charCodes.toString().trim() + "]");

            // Raise an exception for invalid lengths
            throw new IllegalArgumentException("Input must be either 7 or 11 digits long: " + input);
        }
    }

    public static int intParse(String input) {
        try {
            if (input == null) {
                throw new IllegalArgumentException("Input cannot be null");
            }

            String cleaned = input.trim();
            if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
                cleaned = cleaned.substring(1, cleaned.length() - 1);
            }

            // Allow numeric values with optional decimals or scientific notation
            if (!cleaned.matches("\\d+(\\.\\d+)?([eE][+-]?\\d+)?")) {
                throw new NumberFormatException("Invalid numeric value: " + cleaned);
            }

            double value = Double.parseDouble(cleaned); // Parse as double to handle scientific notation
            return (int) Math.round(value); // Round and cast to int
        } catch (IllegalArgumentException e) {
            logger.error("Failed to parse input: " + input, e);
            throw e;
        }
    }
}