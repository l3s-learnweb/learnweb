package de.l3s.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class UrlHelperTest {

    @Test
    void ensureTrailingSlash() {
        assertNull(UrlHelper.ensureTrailingSlash(null));
        assertEquals("", UrlHelper.ensureTrailingSlash(""));
        assertEquals("https://learnweb.l3s.uni-hannover.de/", UrlHelper.ensureTrailingSlash("https://learnweb.l3s.uni-hannover.de"));
        assertEquals("https://learnweb.l3s.uni-hannover.de/", UrlHelper.ensureTrailingSlash("https://learnweb.l3s.uni-hannover.de/"));
    }

    @Test
    void removeTrailingSlash() {
        assertNull(UrlHelper.removeTrailingSlash(null));
        assertEquals("", UrlHelper.removeTrailingSlash(""));
        assertEquals("https://learnweb.l3s.uni-hannover.de", UrlHelper.removeTrailingSlash("https://learnweb.l3s.uni-hannover.de"));
        assertEquals("https://learnweb.l3s.uni-hannover.de", UrlHelper.removeTrailingSlash("https://learnweb.l3s.uni-hannover.de/"));
    }

    @Test
    @Disabled("Remote HTTP requests should be disabled on CI")
    void validateUrl() {
        assertEquals("https://en.wikipedia.org/wiki/Hamburg", UrlHelper.validateUrl("https://en.wikipedia.org/wiki/Hamburg"));
        assertEquals("https://waps.io/live/https://en.wikipedia.org/wiki/Hamburg", UrlHelper.validateUrl("https%3A%2F%2Fwaps.io%2Flive%2Fhttps%3A%2F%2Fen.wikipedia.org%2Fwiki%2FHamburg"));
        assertEquals("https://wikipedia.org/wiki/Hamburg", UrlHelper.validateUrl("wikipedia.org/wiki/Hamburg"));
    }
}
