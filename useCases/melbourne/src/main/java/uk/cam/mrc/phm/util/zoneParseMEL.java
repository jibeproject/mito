package uk.cam.mrc.phm.util;

public class zoneParseMEL {

    public static int zoneParse(String input) {
        // Check if the input is numeric
        if (!input.matches("\\d+")) {
            throw new IllegalArgumentException("Input is not a valid number: " + input);
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
            // Raise an exception for invalid lengths
            throw new IllegalArgumentException("Input must be either 7 or 11 digits long: " + input);
        }
    }
}