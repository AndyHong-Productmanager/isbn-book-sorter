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
            if (nationalLibraryBook != null) {
                callback.onFound(new BookLookupResult(nationalLibraryBook, false));
                return;
            }
            Book aladinBook = tryLookup(() -> findAladinBook(isbn));
            if (aladinBook != null) {
                callback.onFound(new BookLookupResult(aladinBook, false));
                return;
            }
            Book googleBook = tryLookup(() -> findGoogleBook(isbn));
            if (googleBook != null) {
                callback.onFound(new BookLookupResult(googleBook, true));
                return;
            }
            Book openLibraryBook = tryLookup(() -> findOpenLibraryBook(isbn));
            if (openLibraryBook != null) {
                callback.onFound(new BookLookupResult(openLibraryBook, true));
                return;
            }
            callback.onMissing("서지정보를 찾지 못했습니다. ISBN 또는 API 키를 확인하세요.");
        });
    }

    void shutdown() {
        executor.shutdownNow();
    }

    boolean hasDomesticKey() {
        return !nationalLibraryKey.isEmpty() || !aladinKey.isEmpty();
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
                firstNonEmpty(doc, "TITLE", "title", "bookname"),
                firstNonEmpty(doc, "AUTHOR", "author"),
                firstNonEmpty(doc, "PUBLISHER", "publisher"),
                firstNonEmpty(doc, "PUBLISH_PREDATE", "publishPredate", "publishDate"),
                firstNonEmpty(doc, "SUBJECT", "subject", "KDC", "class_no"),
                "국립중앙도서관");
    }

    private Book findAladinBook(String isbn) throws Exception {
        if (aladinKey.isEmpty()) {
            return null;
        }
        String url = "https://www.aladin.co.kr/ttb/api/ItemLookUp.aspx?ttbkey="
                + encode(aladinKey)
                + "&itemIdType=ISBN13&ItemId="
                + encode(isbn)
                + "&output=js&Version=20131101";
        JSONObject root = readJson(url);
        JSONArray items = root.optJSONArray("item");
        if (items == null || items.length() == 0) {
            return null;
        }
        JSONObject item = items.getJSONObject(0);
        return new Book(
                isbn,
                item.optString("title", "제목 없음"),
                item.optString("author", ""),
                item.optString("publisher", ""),
                item.optString("pubDate", ""),
                item.optString("categoryName", "미분류"),
                "알라딘");
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
                joinArray(volume.optJSONArray("authors")),
                volume.optString("publisher", ""),
                volume.optString("publishedDate", ""),
                firstCategory(volume.optJSONArray("categories")),
                "Google Books");
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
                joinNames(item.optJSONArray("authors")),
                joinNames(item.optJSONArray("publishers")),
                item.optString("publish_date", ""),
                firstName(item.optJSONArray("subjects")),
                "Open Library");
    }

    private static Book tryLookup(LookupSource source) {
        try {
            return source.find();
        } catch (Exception exception) {
            return null;
        }
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

    private static String firstNonEmpty(JSONObject object, String... keys) {
        for (String key : keys) {
            String value = object.optString(key, EMPTY).trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
        return "미분류";
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
}
