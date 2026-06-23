package com.andy.isbnbooksorter;

import java.util.List;

final class CsvExporter {
    private static final String LINE_ENDING = "\r\n";
    private static final String[] HEADER = {
            "ISBN",
            "Title",
            "Subtitle",
            "Authors",
            "Translators",
            "Publisher",
            "Published Date",
            "Category",
            "Source",
            "Description",
            "Table Of Contents",
            "Contents",
            "Introduction",
            "Page Count",
            "Thumbnail URL",
            "Language",
            "Price",
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
                    book.subtitle,
                    book.authors,
                    book.translators,
                    book.publisher,
                    book.publishedDate,
                    book.category,
                    book.source,
                    book.description,
                    book.tableOfContents,
                    book.contents,
                    book.introduction,
                    String.valueOf(book.pageCount),
                    book.thumbnailUrl,
                    book.language,
                    book.price,
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
