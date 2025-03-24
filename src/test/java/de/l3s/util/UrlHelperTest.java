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
    void validateUrl() {
        assertNull(UrlHelper.validateUrl("hello world"));
        assertNull(UrlHelper.validateUrl("example.com"));
        assertNull(UrlHelper.validateUrl("wikipedia.org/wiki/Hamburg"));
        assertEquals("https://wikipedia.org/%D0%BF%D1%80%D0%B8%D0%B2%D1%96%D1%82", UrlHelper.validateUrl("https://wikipedia.org/привіт"));
        assertEquals("https://example.com/%C3%BCber-uns", UrlHelper.validateUrl("https://example.com/über-uns"));
        assertEquals("https://en.wikipedia.org/wiki/Hamburg", UrlHelper.validateUrl("https://en.wikipedia.org/wiki/Hamburg"));
        assertEquals("https://en.wikipedia.org/wiki/Hamburg", UrlHelper.validateUrl("https%3A%2F%2Fwaps.l3s.uni-hannover.de%2Flive%2Fhttps%3A%2F%2Fen.wikipedia.org%2Fwiki%2FHamburg"));
    }

    @Test
    @Disabled("Remote HTTP requests should be disabled on CI")
    void verifyUrl() {
        assertEquals("https://en.wikipedia.org/wiki/Hamburg", UrlHelper.verifyUrl("https://en.wikipedia.org/wiki/Hamburg"));
        assertEquals("https://en.wikipedia.org/wiki/Hamburg", UrlHelper.verifyUrl("https%3A%2F%2Fwaps.l3s.uni-hannover.de%2Flive%2Fhttps%3A%2F%2Fen.wikipedia.org%2Fwiki%2FHamburg"));
        assertEquals("https://wikipedia.org/wiki/Hamburg", UrlHelper.verifyUrl("wikipedia.org/wiki/Hamburg"));
    }
}
