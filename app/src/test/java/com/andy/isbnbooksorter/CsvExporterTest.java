package com.andy.isbnbooksorter;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public final class CsvExporterTest {
    @Test
    public void exportBooksEscapesRfc4180UnicodeAndFormulaCells() {
        Book book = new Book(
                "9788998139766",
                "수식, \"인용\"\n줄",
                "부제",
                "=홍길동",
                "번역자",
                "+출판사",
                "2024-01-03",
                "-분류",
                "@source",
                "설명\r\n다음 줄",
                "목차1, 목차2",
                "내용",
                "소개",
                321,
                "https://example.com/a,b.jpg",
                "ko",
                "18000",
                1_700_000_000_000L);

        String csv = CsvExporter.exportBooks(Collections.singletonList(book));

        assertEquals(
                "ISBN,Title,Subtitle,Authors,Translators,Publisher,Published Date,Category,Source,Description,Table Of Contents,Contents,Introduction,Page Count,Thumbnail URL,Language,Price,Saved At\r\n"
                        + "9788998139766,\"수식, \"\"인용\"\"\n줄\",부제,'=홍길동,번역자,'+출판사,2024-01-03,'-분류,'@source,\"설명\r\n다음 줄\",\"목차1, 목차2\",내용,소개,321,\"https://example.com/a,b.jpg\",ko,18000,1700000000000\r\n",
                csv);
    }

    @Test
    public void exportBooksWritesHeaderForEmptyList() {
        assertEquals(
                "ISBN,Title,Subtitle,Authors,Translators,Publisher,Published Date,Category,Source,Description,Table Of Contents,Contents,Introduction,Page Count,Thumbnail URL,Language,Price,Saved At\r\n",
                CsvExporter.exportBooks(Collections.emptyList()));
    }

    @Test
    public void exportBooksKeepsInputOrder() {
        Book first = new Book("1", "첫 책", "", "", "", "A", "테스트", "", 0, "", 30L);
        Book second = new Book("2", "둘째 책", "", "", "", "B", "테스트", "", 0, "", 20L);

        String csv = CsvExporter.exportBooks(Arrays.asList(first, second));

        assertEquals(
                "ISBN,Title,Subtitle,Authors,Translators,Publisher,Published Date,Category,Source,Description,Table Of Contents,Contents,Introduction,Page Count,Thumbnail URL,Language,Price,Saved At\r\n"
                        + "1,첫 책,,,,,,A,테스트,,,,,0,,,,30\r\n"
                        + "2,둘째 책,,,,,,B,테스트,,,,,0,,,,20\r\n",
                csv);
    }
}
