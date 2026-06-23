package com.andy.isbnbooksorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class IsbnUtilsTest {
    @Test
    public void normalizeKeepsIsbnDigitsAndUppercaseX() {
        assertEquals("9788937460449", IsbnUtils.normalize("978-89-374-6044-9"));
        assertEquals("030640615X", IsbnUtils.normalize("0-306-40615-x"));
    }

    @Test
    public void isValidChecksIsbn10AndIsbn13Checksum() {
        assertTrue(IsbnUtils.isValid("0306406152"));
        assertTrue(IsbnUtils.isValid("080442957X"));
        assertTrue(IsbnUtils.isValid("9788937460449"));

        assertFalse(IsbnUtils.isValid("0306406153"));
        assertFalse(IsbnUtils.isValid("9788937460440"));
        assertFalse(IsbnUtils.isValid("1234567890123"));
    }

    @Test
    public void isBooklandIsbn13RequiresBookPrefixAndChecksum() {
        assertTrue(IsbnUtils.isBooklandIsbn13("9788937460449"));
        assertFalse(IsbnUtils.isBooklandIsbn13("8801234567890"));
        assertFalse(IsbnUtils.isBooklandIsbn13("9788937460440"));
        assertFalse(IsbnUtils.isBooklandIsbn13("0306406152"));
    }
}
