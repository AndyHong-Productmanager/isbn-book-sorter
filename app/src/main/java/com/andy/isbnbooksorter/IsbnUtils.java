package com.andy.isbnbooksorter;

final class IsbnUtils {
    private IsbnUtils() {
    }

    static String normalize(String rawValue) {
        String normalized = rawValue.replaceAll("[^0-9Xx]", "").toUpperCase();
        if (normalized.length() == 10 || normalized.length() == 13) {
            return normalized;
        }
        return "";
    }

    static boolean isValid(String isbn) {
        if (isbn.length() == 10) {
            return hasValidIsbn10Checksum(isbn);
        }
        if (isbn.length() == 13) {
            return hasValidIsbn13Checksum(isbn);
        }
        return false;
    }

    static boolean isBooklandIsbn13(String isbn) {
        return isbn.length() == 13
                && (isbn.startsWith("978") || isbn.startsWith("979"))
                && hasValidIsbn13Checksum(isbn);
    }

    private static boolean hasValidIsbn10Checksum(String isbn) {
        int sum = 0;
        for (int index = 0; index < 10; index += 1) {
            char character = isbn.charAt(index);
            int value;
            if (character == 'X' && index == 9) {
                value = 10;
            } else if (character >= '0' && character <= '9') {
                value = character - '0';
            } else {
                return false;
            }
            sum += value * (10 - index);
        }
        return sum % 11 == 0;
    }

    private static boolean hasValidIsbn13Checksum(String isbn) {
        int sum = 0;
        for (int index = 0; index < 13; index += 1) {
            char character = isbn.charAt(index);
            if (character < '0' || character > '9') {
                return false;
            }
            int value = character - '0';
            sum += index % 2 == 0 ? value : value * 3;
        }
        return sum % 10 == 0;
    }
}
