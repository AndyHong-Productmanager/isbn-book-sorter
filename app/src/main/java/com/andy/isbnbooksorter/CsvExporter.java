package com.andy.isbnbooksorter;

import java.util.List;

final class CsvExporter {
    private static final String LINE_ENDING = "\r\n";
    private static final String[] HEADER = {
            "ISBN",
            "Title",
            "Authors",
            "Publisher",
            "Published Date",
            "Category",
            "Source",
            "Description",
            "Page Count",
            "Thumbnail URL",
            "Saved At"
    };

    private CsvExporter() {}

    static String exportBooks(List<Book> books) {
        StringBuilder builder = new StringBuilder();
        appendRow(builder, HEADER);
        for (Book book : books) {
            appendRow(builder, new String[] {
                    book.isbn,
                    book.title,
                    book.authors,
                    book.publisher,
                    book.publishedDate,
                    book.category,
                    book.source,
                    book.description,
                    String.valueOf(book.pageCount),
                    book.thumbnailUrl,
                    String.valueOf(book.savedAt)
            });
        }
        return builder.toString();
    }

    private static void appendRow(StringBuilder builder, String[] cells) {
        for (int index = 0; index < cells.length; index += 1) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(escapeCell(cells[index]));
        }
        builder.append(LINE_ENDING);
    }

    private static String escapeCell(String value) {
        String safeValue = value == null ? "" : value;
        if (startsWithFormulaPrefix(safeValue)) {
            safeValue = "'" + safeValue;
        }
        boolean mustQuote = safeValue.contains(",")
                || safeValue.contains("\"")
                || safeValue.contains("\r")
                || safeValue.contains("\n");
        if (!mustQuote) {
            return safeValue;
        }
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }

    private static boolean startsWithFormulaPrefix(String value) {
        if (value.isEmpty()) {
            return false;
        }
        char first = value.charAt(0);
        return first == '=' || first == '+' || first == '-' || first == '@';
    }
}
