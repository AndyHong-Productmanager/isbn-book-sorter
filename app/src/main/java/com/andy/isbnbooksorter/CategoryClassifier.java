package com.andy.isbnbooksorter;

final class CategoryClassifier {
    private static final String[] KDC_MAIN_CLASSES = {
            "총류",
            "철학",
            "종교",
            "사회과학",
            "자연과학",
            "기술과학",
            "예술",
            "언어",
            "문학",
            "역사"
    };

    private CategoryClassifier() {}

    static String displayName(String value) {
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.isEmpty()) {
            return "미분류";
        }
        if (!cleaned.matches("\\d+")) {
            return cleaned;
        }
        int index = Character.digit(cleaned.charAt(0), 10);
        if (index < 0 || index >= KDC_MAIN_CLASSES.length) {
            return cleaned;
        }
        return KDC_MAIN_CLASSES[index];
    }
}
