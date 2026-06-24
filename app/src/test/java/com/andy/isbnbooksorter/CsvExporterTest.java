package com.andy.isbnbooksorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

import java.nio.charset.StandardCharsets;
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
                "152*225",
                "종이책",
                "단행본",
                "총서",
                "3",
                "9790000000000",
                "https://example.com/title",
                "9790000000000",
                "03230",
                "20240101",
                "20240102",
                "Y",
                "N",
                "N",
                1_700_000_000_000L);

        String csv = CsvExporter.exportBooks(Collections.singletonList(book));

        assertEquals(
                "ISBN,Title,Subtitle,Authors,Translators,Publisher,Published Date,Category,Source,Description,Table Of Contents,Contents,Introduction,Page Count,Thumbnail URL,Language,Price,Book Size,Form,Form Detail,Series Title,Series No,Related ISBN,Title URL,EA ISBN,EA Add Code,Input Date,Update Date,BIB YN,Deposit YN,Ebook YN,Saved At\r\n"
                        + "9788998139766,\"수식, \"\"인용\"\"\n줄\",부제,'=홍길동,번역자,'+출판사,2024-01-03,'-분류,'@source,\"설명\r\n다음 줄\",\"목차1, 목차2\",내용,소개,321,\"https://example.com/a,b.jpg\",ko,18000,152*225,종이책,단행본,총서,3,9790000000000,https://example.com/title,9790000000000,03230,20240101,20240102,Y,N,N,1700000000000\r\n",
                csv);
    }

    @Test
    public void exportBooksWritesHeaderForEmptyList() {
        assertEquals(
                "ISBN,Title,Subtitle,Authors,Translators,Publisher,Published Date,Category,Source,Description,Table Of Contents,Contents,Introduction,Page Count,Thumbnail URL,Language,Price,Book Size,Form,Form Detail,Series Title,Series No,Related ISBN,Title URL,EA ISBN,EA Add Code,Input Date,Update Date,BIB YN,Deposit YN,Ebook YN,Saved At\r\n",
                CsvExporter.exportBooks(Collections.emptyList()));
    }

    @Test
    public void exportBooksAsUtf8BomBytesStartsWithBomForSpreadsheetCompatibility() {
        byte[] bytes = CsvExporter.exportBooksAsUtf8BomBytes(Collections.emptyList());

        assertArrayEquals(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, Arrays.copyOf(bytes, 3));
        assertEquals(CsvExporter.exportBooks(Collections.emptyList()), new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8));
    }

    @Test
    public void exportBooksKeepsInputOrder() {
        Book first = new Book("1", "첫 책", "", "", "", "A", "테스트", "", 0, "", 30L);
        Book second = new Book("2", "둘째 책", "", "", "", "B", "테스트", "", 0, "", 20L);

        String csv = CsvExporter.exportBooks(Arrays.asList(first, second));

        assertEquals(
                "ISBN,Title,Subtitle,Authors,Translators,Publisher,Published Date,Category,Source,Description,Table Of Contents,Contents,Introduction,Page Count,Thumbnail URL,Language,Price,Book Size,Form,Form Detail,Series Title,Series No,Related ISBN,Title URL,EA ISBN,EA Add Code,Input Date,Update Date,BIB YN,Deposit YN,Ebook YN,Saved At\r\n"
                        + "1,첫 책,,,,,,A,테스트,,,,,0,,,,,,,,,,,,,,,,,,30\r\n"
                        + "2,둘째 책,,,,,,B,테스트,,,,,0,,,,,,,,,,,,,,,,,,20\r\n",
                csv);
    }
}
