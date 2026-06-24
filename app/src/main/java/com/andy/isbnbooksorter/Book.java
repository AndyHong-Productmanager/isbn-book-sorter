package com.andy.isbnbooksorter;

final class Book {
    final String isbn;
    final String title;
    final String subtitle;
    final String authors;
    final String translators;
    final String publisher;
    final String publishedDate;
    final String category;
    final String source;
    final String description;
    final String tableOfContents;
    final String contents;
    final String introduction;
    final int pageCount;
    final String thumbnailUrl;
    final String language;
    final String price;
    final String bookSize;
    final String form;
    final String formDetail;
    final String seriesTitle;
    final String seriesNo;
    final String relatedIsbn;
    final String titleUrl;
    final String eaIsbn;
    final String eaAddCode;
    final String inputDate;
    final String updateDate;
    final String bibYn;
    final String depositYn;
    final String ebookYn;
    final long savedAt;

    Book(
            String isbn,
            String title,
            String authors,
            String publisher,
            String publishedDate,
            String category,
            String source) {
        this(isbn, title, authors, publisher, publishedDate, category, source, "", 0, "", 0L);
    }

    Book(
            String isbn,
            String title,
            String authors,
            String publisher,
            String publishedDate,
            String category,
            String source,
            String description,
            int pageCount,
            String thumbnailUrl,
            long savedAt) {
        this(
                isbn,
                title,
                "",
                authors,
                "",
                publisher,
                publishedDate,
                category,
                source,
                description,
                "",
                "",
                "",
                pageCount,
                thumbnailUrl,
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                savedAt);
    }

    Book(
            String isbn,
            String title,
            String subtitle,
            String authors,
            String translators,
            String publisher,
            String publishedDate,
            String category,
            String source,
            String description,
            String tableOfContents,
            String contents,
            String introduction,
            int pageCount,
            String thumbnailUrl,
            String language,
            String price,
            String bookSize,
            String form,
            String formDetail,
            String seriesTitle,
            String seriesNo,
            String relatedIsbn,
            String titleUrl,
            String eaIsbn,
            String eaAddCode,
            String inputDate,
            String updateDate,
            String bibYn,
            String depositYn,
            String ebookYn,
            long savedAt) {
        this.isbn = clean(isbn);
        this.title = defaultValue(title, "제목 없음");
        this.subtitle = clean(subtitle);
        this.authors = clean(authors);
        this.translators = clean(translators);
        this.publisher = clean(publisher);
        this.publishedDate = clean(publishedDate);
        this.category = CategoryClassifier.displayName(category);
        this.source = clean(source);
        this.description = clean(description);
        this.tableOfContents = clean(tableOfContents);
        this.contents = clean(contents);
        this.introduction = clean(introduction);
        this.pageCount = Math.max(0, pageCount);
        this.thumbnailUrl = clean(thumbnailUrl);
        this.language = clean(language);
        this.price = clean(price);
        this.bookSize = clean(bookSize);
        this.form = clean(form);
        this.formDetail = clean(formDetail);
        this.seriesTitle = clean(seriesTitle);
        this.seriesNo = clean(seriesNo);
        this.relatedIsbn = clean(relatedIsbn);
        this.titleUrl = clean(titleUrl);
        this.eaIsbn = clean(eaIsbn);
        this.eaAddCode = clean(eaAddCode);
        this.inputDate = clean(inputDate);
        this.updateDate = clean(updateDate);
        this.bibYn = clean(bibYn);
        this.depositYn = clean(depositYn);
        this.ebookYn = clean(ebookYn);
        this.savedAt = Math.max(0L, savedAt);
    }

    Book withCategory(String category) {
        return new Book(
                isbn,
                title,
                subtitle,
                authors,
                translators,
                publisher,
                publishedDate,
                category,
                source,
                description,
                tableOfContents,
                contents,
                introduction,
                pageCount,
                thumbnailUrl,
                language,
                price,
                bookSize,
                form,
                formDetail,
                seriesTitle,
                seriesNo,
                relatedIsbn,
                titleUrl,
                eaIsbn,
                eaAddCode,
                inputDate,
                updateDate,
                bibYn,
                depositYn,
                ebookYn,
                savedAt);
    }

    Book withSavedAt(long savedAt) {
        return new Book(
                isbn,
                title,
                subtitle,
                authors,
                translators,
                publisher,
                publishedDate,
                category,
                source,
                description,
                tableOfContents,
                contents,
                introduction,
                pageCount,
                thumbnailUrl,
                language,
                price,
                bookSize,
                form,
                formDetail,
                seriesTitle,
                seriesNo,
                relatedIsbn,
                titleUrl,
                eaIsbn,
                eaAddCode,
                inputDate,
                updateDate,
                bibYn,
                depositYn,
                ebookYn,
                savedAt);
    }

    private static String defaultValue(String value, String fallback) {
        String cleaned = clean(value);
        if (cleaned.isEmpty()) {
            return fallback;
        }
        return cleaned;
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
