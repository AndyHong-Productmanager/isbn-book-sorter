package com.andy.isbnbooksorter;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public final class BibliographyClientTest {
    @Test
    public void firstNonEmptyUsesFieldSpecificFallback() {
        BibliographyClient.FieldReader reader = key -> {
            if ("TITLE".equals(key)) {
                return " 데미안 ";
            }
            if ("KDC".equals(key)) {
                return "8";
            }
            return "";
        };

        assertEquals("데미안", BibliographyClient.firstNonEmpty(reader, "제목 없음", "TITLE"));
        assertEquals("", BibliographyClient.firstNonEmpty(reader, "", "PUBLISH_PREDATE", "publishDate"));
        assertEquals("8", BibliographyClient.firstNonEmpty(reader, "미분류", "SUBJECT", "KDC"));
    }

    @Test
    public void missingMessageDoesNotBlameApiKeyWhenDomesticKeyExists() {
        assertEquals(
                "검색 결과가 없습니다. ISBN을 다시 확인하거나 다른 조회 소스를 시도하세요.",
                BibliographyClient.missingMessage(true));
        assertEquals(
                "국내 API 키가 비어 있어 Google Books와 Open Library만 조회했습니다.",
                BibliographyClient.missingMessage(false));
    }

    @Test
    public void nationalLibraryMapsFullBibliographyFields() throws Exception {
        Map<String, String> doc = new HashMap<>();
        doc.put("TITLE", "책 제목");
        doc.put("AUTHOR", "저자");
        doc.put("PUBLISHER", "출판사");
        doc.put("REAL_PUBLISH_DATE", "20260101");
        doc.put("SUBJECT", "2");
        doc.put("BOOK_TB_CNT", "1장 목차");
        doc.put("BOOK_SUMMARY", "내용 요약");
        doc.put("BOOK_INTRODUCTION", "책 소개");
        doc.put("PAGE", "742 p.");
        doc.put("PRE_PRICE", "25000");
        doc.put("BOOK_SIZE", "152*225");
        doc.put("FORM", "종이책");
        doc.put("FORM_DETAIL", "단행본");
        doc.put("SERIES_TITLE", "총서");
        doc.put("SERIES_NO", "3");
        doc.put("RELATED_ISBN", "9790000000000");
        doc.put("TITLE_URL", "https://example.com/title");
        doc.put("EA_ISBN", "9791198048356");
        doc.put("EA_ADD_CODE", "03230");
        doc.put("INPUT_DATE", "20250901");
        doc.put("UPDATE_DATE", "20250907");
        doc.put("BIB_YN", "Y");
        doc.put("DEPOSIT_YN", "N");
        doc.put("EBOOK_YN", "N");

        Book book = BibliographyClient.nationalBookFromFields(
                "9791198048356",
                key -> doc.getOrDefault(key, ""));

        assertEquals("1장 목차", book.tableOfContents);
        assertEquals("내용 요약", book.contents);
        assertEquals("책 소개", book.introduction);
        assertEquals(742, book.pageCount);
        assertEquals("25000", book.price);
        assertEquals("152*225", book.bookSize);
        assertEquals("종이책", book.form);
        assertEquals("단행본", book.formDetail);
        assertEquals("총서", book.seriesTitle);
        assertEquals("3", book.seriesNo);
        assertEquals("종교", book.category);
        assertEquals("9790000000000", book.relatedIsbn);
        assertEquals("https://example.com/title", book.titleUrl);
        assertEquals("9791198048356", book.eaIsbn);
        assertEquals("03230", book.eaAddCode);
        assertEquals("20250901", book.inputDate);
        assertEquals("20250907", book.updateDate);
        assertEquals("Y", book.bibYn);
        assertEquals("N", book.depositYn);
        assertEquals("N", book.ebookYn);
    }
}
