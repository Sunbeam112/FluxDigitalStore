package com.artemhontar.fluxdigitalstore.service;

import org.springframework.stereotype.Service;

@Service
public class ISBNUtil {


    /**
     * Checks if the given string is a valid ISBN (10-digit or 13-digit).
     *
     * @param isbn The raw ISBN string, which may contain hyphens or spaces.
     * @return true if the ISBN is valid, false otherwise.
     */
    public boolean isValidISBN(String isbn) {
        if (isbn == null) {
            return false;
        }

        // 1. Normalize the ISBN by removing hyphens and spaces
        String normalizedIsbn = isbn.replaceAll("[\\s-]+", "");

        if (normalizedIsbn.length() == 10) {
            return isValidIsbn10(normalizedIsbn);
        } else if (normalizedIsbn.length() == 13) {
            return isValidIsbn13(normalizedIsbn);
        }

        // Invalid length
        return false;
    }

    /**
     * Validates a 13-digit ISBN using the Modulus 10 check digit system.
     */
    private boolean isValidIsbn13(String isbn) {
        if (!isbn.matches("\\d{13}")) {
            return false; // Must contain only 13 digits
        }

        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = Character.getNumericValue(isbn.charAt(i));
            // Weights alternate between 1 and 3
            sum += (i % 2 == 0) ? digit * 1 : digit * 3;
        }

        int checkDigit = Character.getNumericValue(isbn.charAt(12));
        int calculatedCheck = (10 - (sum % 10)) % 10;

        return checkDigit == calculatedCheck;
    }

    /**
     * Validates a 10-digit ISBN using the Modulus 11 check digit system.
     */
    private boolean isValidIsbn10(String isbn) {
        // ISBN-10 can have 'X' or 'x' as the last character
        if (!isbn.substring(0, 9).matches("\\d{9}") || !isbn.substring(9).matches("[0-9Xx]")) {
            return false;
        }

        int sum = 0;
        for (int i = 0; i < 9; i++) {
            int digit = Character.getNumericValue(isbn.charAt(i));
            // Weights are 10, 9, 8, ... 2
            sum += digit * (10 - i);
        }

        char checkChar = Character.toUpperCase(isbn.charAt(9));
        int checkDigit;

        if (checkChar == 'X') {
            checkDigit = 10;
        } else {
            checkDigit = Character.getNumericValue(checkChar);
        }

        // Final check: sum + check digit must be divisible by 11
        return (sum + checkDigit) % 11 == 0;
    }
}