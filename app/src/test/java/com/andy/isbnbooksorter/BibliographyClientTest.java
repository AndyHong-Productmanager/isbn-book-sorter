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
}
