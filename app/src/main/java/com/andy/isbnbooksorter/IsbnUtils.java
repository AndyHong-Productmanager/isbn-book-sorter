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
        return isbn.length() == 10 || isbn.length() == 13;
    }
}
