package com.andy.isbnbooksorter;

final class CatalogUiContract {
    static final String APP_TITLE = "ISBN 도서 정리";
    static final String APP_SUBTITLE = "ISBN을 스캔하거나 직접 입력하면 카테고리별로 저장됩니다.";
    static final String MENU_TOGGLE = "☰ 메뉴";
    static final String MENU_ISBN_SEARCH = "ISBN 검색하기";
    static final String MENU_LIBRARY = "LIBRARY";
    static final String ISBN_SEARCH_TITLE = "ISBN 검색하기";
    static final String LIBRARY_TITLE = "LIBRARY";
    static final String ISBN_INPUT_HINT = "ISBN 직접 입력";
    static final String CATEGORY_INPUT_HINT = "카테고리 직접 지정(선택)";
    static final String LOOKUP_AND_SAVE = "조회 후 저장";
    static final String SAVED_BOOKS_TITLE = "저장된 책";
    static final String SAVED_SEARCH_HINT = "저장된 책 검색(제목/저자/출판사/ISBN/분류/출처)";
    static final String SAVED_CATEGORY_FILTER_HINT = "카테고리 필터";
    static final String APPLY_FILTERS = "적용";
    static final String CLEAR_FILTERS = "초기화";
    static final String EXPORT_VISIBLE_CSV = "현재 목록 CSV 내보내기";
    static final String BOOK_DETAIL_HINT = "눌러서 서지정보 상세보기";
    static final String BOOK_DETAIL_TITLE = "서지정보 상세";
    static final String BOOK_DETAIL_CLOSE = "닫기";
    static final String EMPTY_LIBRARY_MESSAGE = "아직 저장된 책이 없습니다. ISBN을 스캔하거나 직접 입력하세요.";
    static final String EMPTY_FILTERED_MESSAGE = "조건에 맞는 저장된 책이 없습니다. 검색어나 카테고리 필터를 지워보세요.";

    static final String[] SORT_LABELS = {
            "저장일 최신순",
            "제목순",
            "저자순",
            "카테고리순"
    };

    private CatalogUiContract() {
    }
}
