package com.andy.isbnbooksorter;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class CatalogUiContractTest {
    @Test
    public void savedBookControlsUseMobileCatalogLabels() {
        assertEquals("ISBN 도서 정리", CatalogUiContract.APP_TITLE);
        assertEquals("☰ 메뉴", CatalogUiContract.MENU_TOGGLE);
        assertEquals("ISBN 검색하기", CatalogUiContract.MENU_ISBN_SEARCH);
        assertEquals("LIBRARY", CatalogUiContract.MENU_LIBRARY);
        assertEquals("ISBN 검색하기", CatalogUiContract.ISBN_SEARCH_TITLE);
        assertEquals("LIBRARY", CatalogUiContract.LIBRARY_TITLE);
        assertEquals("ISBN 직접 입력", CatalogUiContract.ISBN_INPUT_HINT);
        assertEquals("카테고리 직접 지정(선택)", CatalogUiContract.CATEGORY_INPUT_HINT);
        assertEquals("저장된 책 검색(제목/저자/출판사/ISBN/분류/출처)", CatalogUiContract.SAVED_SEARCH_HINT);
        assertEquals("카테고리 필터", CatalogUiContract.SAVED_CATEGORY_FILTER_HINT);
        assertEquals("전체 카테고리", CatalogUiContract.ALL_CATEGORIES);
        assertEquals("적용", CatalogUiContract.APPLY_FILTERS);
        assertEquals("초기화", CatalogUiContract.CLEAR_FILTERS);
        assertEquals("현재 목록 CSV 내보내기", CatalogUiContract.EXPORT_VISIBLE_CSV);
        assertEquals("눌러서 세부페이지 열기", CatalogUiContract.BOOK_DETAIL_HINT);
        assertEquals("도서 상세", CatalogUiContract.BOOK_DETAIL_TITLE);
        assertEquals("LIBRARY로 돌아가기", CatalogUiContract.BACK_TO_LIBRARY);
        assertEquals("아직 저장된 책이 없습니다. ISBN을 스캔하거나 직접 입력하세요.", CatalogUiContract.EMPTY_LIBRARY_MESSAGE);
    }

    @Test
    public void savedBookSortOptionsCoverRequiredBrowseModes() {
        assertArrayEquals(
                new String[]{"저장일 최신순", "제목순", "저자순", "카테고리순"},
                CatalogUiContract.SORT_LABELS);
    }
}
