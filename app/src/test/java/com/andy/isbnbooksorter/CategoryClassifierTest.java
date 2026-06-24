package com.andy.isbnbooksorter;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class CategoryClassifierTest {
    @Test
    public void mapsKdcMainClassDigitsToNames() {
        assertEquals("총류", CategoryClassifier.displayName("0"));
        assertEquals("종교", CategoryClassifier.displayName("2"));
        assertEquals("기술과학", CategoryClassifier.displayName("5"));
        assertEquals("문학", CategoryClassifier.displayName("800"));
    }

    @Test
    public void keepsNamedCategoriesAndDefaultsBlankValues() {
        assertEquals("국내소설", CategoryClassifier.displayName("국내소설"));
        assertEquals("미분류", CategoryClassifier.displayName(""));
        assertEquals("미분류", CategoryClassifier.displayName(null));
    }
}
