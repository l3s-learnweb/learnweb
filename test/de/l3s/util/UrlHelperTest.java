package de.l3s.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UrlHelperTest {

    @Test
    void ensureTrailingSlash() {
        assertNull(UrlHelper.ensureTrailingSlash(null));
        assertEquals("", UrlHelper.ensureTrailingSlash(""));
        assertEquals("https://learnweb.l3s.uni-hannover.de/", UrlHelper.ensureTrailingSlash("https://learnweb.l3s.uni-hannover.de/"));
        assertEquals("https://learnweb.l3s.uni-hannover.de/", UrlHelper.ensureTrailingSlash("https://learnweb.l3s.uni-hannover.de/"));
    }
}
