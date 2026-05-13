package com.example.desofs.infrastructure.utils;

import java.util.regex.Pattern;

/**
 * Public ID Handler for encoding/decoding database IDs to obfuscated public IDs
 * Useful for hiding internal database sequences in API responses
 */
public class PublicIdHandler {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final Pattern VALID_PATTERN = Pattern.compile("^[0-9A-Za-z]+$");

    /**
     * Encode database ID to public ID (obfuscated)
     * @param databaseId internal database ID
     * @return obfuscated public ID string
     */
    public static String encode(Long databaseId) {
        if (databaseId == null || databaseId < 0) {
            return null;
        }
        if (databaseId == 0) {
            return "0";
        }
        StringBuilder encoded = new StringBuilder();
        long value = databaseId;
        while (value > 0) {
            int index = (int) (value % ALPHABET.length());
            encoded.insert(0, ALPHABET.charAt(index));
            value /= ALPHABET.length();
        }
        return encoded.toString();
    }

    /**
     * Decode public ID back to database ID
     * @param publicId obfuscated public ID string
     * @return original database ID
     */
    public static Long decode(String publicId) {
        if (!isValid(publicId)) {
            return null;
        }
        long value = 0;
        for (char c : publicId.toCharArray()) {
            value = value * ALPHABET.length() + ALPHABET.indexOf(c);
        }
        return value;
    }

    /**
     * Check if string is a valid public ID
     * @param publicId string to validate
     * @return true if valid public ID format
     */
    public static boolean isValid(String publicId) {
        return publicId != null && !publicId.isBlank() && VALID_PATTERN.matcher(publicId).matches();
    }
}
