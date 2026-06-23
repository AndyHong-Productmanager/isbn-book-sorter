package com.andy.isbnbooksorter;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

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
}
