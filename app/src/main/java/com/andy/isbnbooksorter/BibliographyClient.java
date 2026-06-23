package com.andy.isbnbooksorter;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class BibliographyClient {
    interface Callback {
        void onFound(BookLookupResult result);

        void onMissing(String message);
    }

    private interface LookupSource {
        Book find() throws Exception;
    }

    interface FieldReader {
        String read(String key);
    }

    private static final String EMPTY = "";
    private final String nationalLibraryKey;
    private final String aladinKey;
    private final String googleBooksKey;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    BibliographyClient(Context context) {
        this.nationalLibraryKey = context.getString(R.string.national_library_key).trim();
        this.aladinKey = context.getString(R.string.aladin_ttb_key).trim();
        this.googleBooksKey = context.getString(R.string.google_books_key).trim();
    }

    void lookup(String isbn, Callback callback) {
        executor.execute(() -> {
            Book nationalLibraryBook = tryLookup(() -> findNationalLibraryBook(isbn));
            Book aladinBook = tryLookup(() -> findAladinBook(isbn));
            Book googleBook = tryLookup(() -> findGoogleBook(isbn));
            Book openLibraryBook = tryLookup(() -> findOpenLibraryBook(isbn));
            if (nationalLibraryBook != null) {
                callback.onFound(new BookLookupResult(
                        mergeBooks(nationalLibraryBook, aladinBook, googleBook, openLibraryBook),
                        false));
                return;
            }
            if (aladinBook != null) {
                callback.onFound(new BookLookupResult(mergeBooks(aladinBook, googleBook, openLibraryBook), false));
                return;
            }
            if (googleBook != null) {
                callback.onFound(new BookLookupResult(mergeBooks(googleBook, openLibraryBook), true));
                return;
            }
            if (openLibraryBook != null) {
                callback.onFound(new BookLookupResult(openLibraryBook, true));
                return;
            }
            callback.onMissing(missingMessage(hasDomesticKey()));
        });
    }

    void shutdown() {
        executor.shutdownNow();
    }

    boolean hasDomesticKey() {
        return !nationalLibraryKey.isEmpty() || !aladinKey.isEmpty();
    }

    static String missingMessage(boolean hasDomesticKey) {
        if (hasDomesticKey) {
            return "검색 결과가 없습니다. ISBN을 다시 확인하거나 다른 조회 소스를 시도하세요.";
        }
        return "국내 API 키가 비어 있어 Google Books와 Open Library만 조회했습니다.";
    }

    private Book findNationalLibraryBook(String isbn) throws Exception {
        if (nationalLibraryKey.isEmpty()) {
            return null;
        }
        String url = "https://www.nl.go.kr/seoji/SearchApi.do?cert_key="
                + encode(nationalLibraryKey)
                + "&result_style=json&page_no=1&page_size=1&isbn="
                + encode(isbn);
        JSONObject root = readJson(url);
        JSONArray docs = root.optJSONArray("docs");
        if (docs == null || docs.length() == 0) {
            return null;
        }
        JSONObject doc = docs.getJSONObject(0);
        return new Book(
                isbn,
                firstNonEmpty(doc, "제목 없음", "TITLE", "title", "bookname"),
                firstNonEmpty(doc, "", "SUB_TITLE", "subtitle", "subTitle"),
                firstNonEmpty(doc, "", "AUTHOR", "author"),
                firstNonEmpty(doc, "", "TRANSLATOR", "translator", "TRSLTOR", "translatedBy"),
                firstNonEmpty(doc, "", "PUBLISHER", "publisher"),
                firstNonEmpty(doc, "", "PUBLISH_PREDATE", "publishPredate", "publishDate"),
                firstNonEmpty(doc, "미분류", "SUBJECT", "subject", "KDC", "class_no"),
                "국립중앙도서관",
                firstNonEmpty(doc, "", "DESCRIPTION", "description", "ABSTRACT", "abstract"),
                firstNonEmpty(doc, "", "TOC", "toc", "TABLE_OF_CONTENTS", "tableOfContents"),
                firstNonEmpty(doc, "", "CONTENTS", "contents", "CONTENT", "content", "SUMMARY", "summary"),
                firstNonEmpty(doc, "", "INTRODUCTION", "introduction", "BOOK_INTRODUCTION", "bookIntroduction"),
                firstInt(doc, "PAGE", "PAGE_CNT", "page", "pageCount"),
                firstNonEmpty(doc, "", "IMAGE", "image", "COVER_URL", "coverUrl", "thumbnailUrl"),
                firstNonEmpty(doc, "", "LANG", "language"),
                firstNonEmpty(doc, "", "PRICE", "price"),
                0L);
    }

    private Book findAladinBook(String isbn) throws Exception {
        if (aladinKey.isEmpty()) {
            return null;
        }
        String url = "https://www.aladin.co.kr/ttb/api/ItemLookUp.aspx?ttbkey="
                + encode(aladinKey)
                + "&itemIdType=ISBN13&ItemId="
                + encode(isbn)
                + "&output=js&Version=20131101&OptResult=authors,fulldescription,Toc";
        JSONObject root = readJson(url);
        JSONArray items = root.optJSONArray("item");
        if (items == null || items.length() == 0) {
            return null;
        }
        JSONObject item = items.getJSONObject(0);
        JSONObject subInfo = item.optJSONObject("subInfo");
        return new Book(
                isbn,
                item.optString("title", "제목 없음"),
                firstNonEmpty(item, "", "subTitle", "subtitle"),
                item.optString("author", ""),
                translatorsFromAladin(item, subInfo),
                item.optString("publisher", ""),
                item.optString("pubDate", ""),
                item.optString("categoryName", "미분류"),
                "알라딘",
                firstNonEmpty(item, "", "description", "fullDescription"),
                firstNonEmpty(subInfo, "", "toc", "Toc", "tableOfContents"),
                firstNonEmpty(item, "", "fullDescription", "description"),
                firstNonEmpty(subInfo, "", "description", "fulldescription", "fullDescription"),
                firstInt(subInfo, "itemPage", "pageCount"),
                item.optString("cover", ""),
                "",
                firstNonEmpty(item, "", "priceStandard", "priceSales"),
                0L);
    }

    private Book findGoogleBook(String isbn) throws Exception {
        String url = "https://www.googleapis.com/books/v1/volumes?q=isbn:"
                + encode(isbn)
                + "&maxResults=1&printType=books&langRestrict=ko"
                + googleBooksKeyParam();
        JSONObject root = readJson(url);
        JSONArray items = root.optJSONArray("items");
        if (items == null || items.length() == 0) {
            return null;
        }
        JSONObject volume = items.getJSONObject(0).getJSONObject("volumeInfo");
        return new Book(
                isbn,
                volume.optString("title", "제목 없음"),
                volume.optString("subtitle", ""),
                joinArray(volume.optJSONArray("authors")),
                "",
                volume.optString("publisher", ""),
                volume.optString("publishedDate", ""),
                firstCategory(volume.optJSONArray("categories")),
                "Google Books",
                volume.optString("description", ""),
                "",
                volume.optString("description", ""),
                volume.optString("description", ""),
                volume.optInt("pageCount", 0),
                thumbnailFromGoogle(volume),
                volume.optString("language", ""),
                "",
                0L);
    }

    private Book findOpenLibraryBook(String isbn) throws Exception {
        String key = "ISBN:" + isbn;
        String url = "https://openlibrary.org/api/books?bibkeys="
                + encode(key)
                + "&format=json&jscmd=data";
        JSONObject root = readJson(url);
        JSONObject item = root.optJSONObject(key);
        if (item == null) {
            return null;
        }
        return new Book(
                isbn,
                item.optString("title", "제목 없음"),
                item.optString("subtitle", ""),
                joinNames(item.optJSONArray("authors")),
                "",
                joinNames(item.optJSONArray("publishers")),
                item.optString("publish_date", ""),
                firstName(item.optJSONArray("subjects")),
                "Open Library",
                descriptionFromOpenLibrary(item),
                tableOfContentsFromOpenLibrary(item),
                contentsFromOpenLibrary(item),
                firstExcerptFromOpenLibrary(item),
                numberOfPagesFromOpenLibrary(item),
                thumbnailFromOpenLibrary(item),
                "",
                "",
                0L);
    }

    private static Book tryLookup(LookupSource source) {
        try {
            return source.find();
        } catch (Exception exception) {
            return null;
        }
    }

    private static Book mergeBooks(Book primary, Book... extras) {
        Book merged = primary;
        for (Book extra : extras) {
            if (extra == null) {
                continue;
            }
            merged = new Book(
                    merged.isbn,
                    firstFilled(merged.title, extra.title),
                    firstFilled(merged.subtitle, extra.subtitle),
                    firstFilled(merged.authors, extra.authors),
                    firstFilled(merged.translators, extra.translators),
                    firstFilled(merged.publisher, extra.publisher),
                    firstFilled(merged.publishedDate, extra.publishedDate),
                    firstFilled(merged.category, extra.category),
                    mergeSources(merged.source, extra.source),
                    firstFilled(merged.description, extra.description),
                    firstFilled(merged.tableOfContents, extra.tableOfContents),
                    firstFilled(merged.contents, extra.contents),
                    firstFilled(merged.introduction, extra.introduction),
                    merged.pageCount > 0 ? merged.pageCount : extra.pageCount,
                    firstFilled(merged.thumbnailUrl, extra.thumbnailUrl),
                    firstFilled(merged.language, extra.language),
                    firstFilled(merged.price, extra.price),
                    merged.savedAt);
        }
        return merged;
    }

    private static String firstFilled(String primary, String fallback) {
        if (primary != null
                && !primary.trim().isEmpty()
                && !"미분류".equals(primary.trim())
                && !"제목 없음".equals(primary.trim())) {
            return primary;
        }
        return fallback == null ? "" : fallback.trim();
    }

    private static String mergeSources(String primary, String extra) {
        String left = primary == null ? "" : primary.trim();
        String right = extra == null ? "" : extra.trim();
        if (left.isEmpty()) {
            return right;
        }
        if (right.isEmpty() || left.contains(right)) {
            return left;
        }
        return left + " + " + right;
    }

    private static JSONObject readJson(String urlValue) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlValue).openConnection();
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(10_000);
        connection.setRequestMethod("GET");
        try (InputStream stream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line = reader.readLine();
            while (line != null) {
                builder.append(line);
                line = reader.readLine();
            }
            return new JSONObject(builder.toString());
        } finally {
            connection.disconnect();
        }
    }

    private static String encode(String value) throws Exception {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
    }

    private String googleBooksKeyParam() throws Exception {
        if (googleBooksKey.isEmpty()) {
            return EMPTY;
        }
        return "&key=" + encode(googleBooksKey);
    }

    private static String firstNonEmpty(JSONObject object, String fallback, String... keys) {
        if (object == null) {
            return fallback;
        }
        return firstNonEmpty(key -> object.optString(key, EMPTY), fallback, keys);
    }

    static String firstNonEmpty(FieldReader reader, String fallback, String... keys) {
        for (String key : keys) {
            String value = reader.read(key).trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
        return fallback;
    }

    private static int firstInt(JSONObject object, String... keys) {
        if (object == null) {
            return 0;
        }
        for (String key : keys) {
            int numericValue = object.optInt(key, 0);
            if (numericValue > 0) {
                return numericValue;
            }
            String stringValue = object.optString(key, EMPTY).replaceAll("[^0-9]", "").trim();
            if (stringValue.isEmpty()) {
                continue;
            }
            try {
                return Integer.parseInt(stringValue);
            } catch (NumberFormatException exception) {
                continue;
            }
        }
        return 0;
    }

    private static String joinArray(JSONArray array) {
        if (array == null) {
            return "";
        }
        List<String> values = new ArrayList<>();
        for (int index = 0; index < array.length(); index += 1) {
            String value = array.optString(index).trim();
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return String.join(", ", values);
    }

    private static String firstCategory(JSONArray categories) {
        if (categories == null || categories.length() == 0) {
            return "미분류";
        }
        String category = categories.optString(0).trim();
        if (category.isEmpty()) {
            return "미분류";
        }
        return category;
    }

    private static String joinNames(JSONArray objects) {
        if (objects == null) {
            return "";
        }
        List<String> values = new ArrayList<>();
        for (int index = 0; index < objects.length(); index += 1) {
            JSONObject object = objects.optJSONObject(index);
            if (object == null) {
                continue;
            }
            String name = object.optString("name", EMPTY).trim();
            if (!name.isEmpty()) {
                values.add(name);
            }
        }
        return String.join(", ", values);
    }

    private static String firstName(JSONArray objects) {
        if (objects == null || objects.length() == 0) {
            return "미분류";
        }
        JSONObject object = objects.optJSONObject(0);
        if (object == null) {
            return "미분류";
        }
        String name = object.optString("name", EMPTY).trim();
        if (name.isEmpty()) {
            return "미분류";
        }
        return name;
    }

    private static String translatorsFromAladin(JSONObject item, JSONObject subInfo) {
        String fromSubInfo = authorNamesByRole(
                subInfo == null ? null : subInfo.optJSONArray("authors"),
                "옮긴이",
                "역자",
                "번역");
        if (!fromSubInfo.isEmpty()) {
            return fromSubInfo;
        }
        return namesWithRole(item.optString("author", ""), "옮긴이", "역자", "번역");
    }

    private static String authorNamesByRole(JSONArray authors, String... roles) {
        if (authors == null) {
            return "";
        }
        List<String> values = new ArrayList<>();
        for (int index = 0; index < authors.length(); index += 1) {
            JSONObject author = authors.optJSONObject(index);
            if (author == null) {
                continue;
            }
            String role = author.optString("authorType", author.optString("type", "")).trim();
            if (!containsAny(role, roles)) {
                continue;
            }
            String name = firstNonEmpty(author, "", "authorName", "name").trim();
            if (!name.isEmpty()) {
                values.add(name);
            }
        }
        return String.join(", ", values);
    }

    private static String namesWithRole(String value, String... roles) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        List<String> values = new ArrayList<>();
        String[] parts = value.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!containsAny(trimmed, roles)) {
                continue;
            }
            int roleStart = trimmed.indexOf('(');
            String name = roleStart > 0 ? trimmed.substring(0, roleStart).trim() : trimmed;
            if (!name.isEmpty()) {
                values.add(name);
            }
        }
        return String.join(", ", values);
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String thumbnailFromGoogle(JSONObject volume) {
        JSONObject links = volume.optJSONObject("imageLinks");
        if (links == null) {
            return "";
        }
        String thumbnail = links.optString("thumbnail", "").trim();
        if (!thumbnail.isEmpty()) {
            return thumbnail;
        }
        return links.optString("smallThumbnail", "").trim();
    }

    private static String descriptionFromOpenLibrary(JSONObject item) {
        Object description = item.opt("description");
        if (description instanceof JSONObject) {
            return ((JSONObject) description).optString("value", "");
        }
        if (description instanceof String) {
            return ((String) description).trim();
        }
        return "";
    }

    private static int numberOfPagesFromOpenLibrary(JSONObject item) {
        String numberOfPages = item.optString("number_of_pages", "").trim();
        if (numberOfPages.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(numberOfPages);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static String tableOfContentsFromOpenLibrary(JSONObject item) {
        return joinObjectValues(item.optJSONArray("table_of_contents"), "title", "name", "label");
    }

    private static String contentsFromOpenLibrary(JSONObject item) {
        return joinNames(item.optJSONArray("subjects"));
    }

    private static String firstExcerptFromOpenLibrary(JSONObject item) {
        JSONArray excerpts = item.optJSONArray("excerpts");
        if (excerpts == null || excerpts.length() == 0) {
            return "";
        }
        JSONObject excerpt = excerpts.optJSONObject(0);
        if (excerpt == null) {
            return "";
        }
        return excerpt.optString("text", "").trim();
    }

    private static String joinObjectValues(JSONArray objects, String... keys) {
        if (objects == null) {
            return "";
        }
        List<String> values = new ArrayList<>();
        for (int index = 0; index < objects.length(); index += 1) {
            JSONObject object = objects.optJSONObject(index);
            if (object == null) {
                continue;
            }
            String value = firstNonEmpty(object, "", keys);
            if (!value.isEmpty()) {
                values.add(value);
            }
        }
        return String.join(", ", values);
    }

    private static String thumbnailFromOpenLibrary(JSONObject item) {
        JSONObject cover = item.optJSONObject("cover");
        if (cover == null) {
            return "";
        }
        String large = cover.optString("large", "").trim();
        if (!large.isEmpty()) {
            return large;
        }
        String medium = cover.optString("medium", "").trim();
        if (!medium.isEmpty()) {
            return medium;
        }
        return cover.optString("small", "").trim();
    }
}
