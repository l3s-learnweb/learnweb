package de.l3s.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StringHelperTest {
    @Test
    void testEscapeHtml() {
        assertEquals("&lt;img src=x onerror=alert(1)&gt;", StringHelper.escapeHtml("<img src=x onerror=alert(1)>"));
        // plain text that looks like an entity is kept as typed
        assertEquals("AT&amp;amp;T", StringHelper.escapeHtml("AT&amp;T"));
        assertEquals("", StringHelper.escapeHtml(null));
    }

    @Test
    void testHtmlToText() {
        assertEquals("Tom & Jerry DE > NL", StringHelper.htmlToText("Tom &amp; Jerry <b>DE > NL</b>"));
        assertNull(StringHelper.htmlToText(null));
    }

    @Test
    void testHighlightQuery() {
        assertEquals("&lt;i&gt;Tom&lt;/i&gt; <b>&amp; J</b>erry", StringHelper.highlightQuery("<i>Tom</i> & Jerry", StringHelper.compileHighlightQuery("& j")));
        assertEquals("&lt;i&gt;Tom&lt;/i&gt;", StringHelper.highlightQuery("<i>Tom</i>", StringHelper.compileHighlightQuery("Jerry")));
        // lowercasing "İ" changes the string length, the match must still use the original positions
        assertEquals("İİİ <b>abc</b>", StringHelper.highlightQuery("İİİ abc", StringHelper.compileHighlightQuery("ABC")));
        // the query is literal, regex characters are not interpreted
        assertEquals("<b>a.c</b> abc", StringHelper.highlightQuery("a.c abc", StringHelper.compileHighlightQuery("a.c")));
    }

    @Test
    void testRemoveNewLines() {
        assertEquals("Hello world ", StringHelper.removeNewLines("Hello\nworld\n"));
    }

    @Test
    void testTrimNotAlphabetical() {
        assertEquals("fsdfsdfsv", StringHelper.trimNotAlphabetical("43242424 234324 34 %%43 fsdfsdfsv"));
        assertEquals("Hello world", StringHelper.trimNotAlphabetical("Hello world"));
        assertEquals("Hello world", StringHelper.trimNotAlphabetical(" @#@@ Hello world"));
    }

    @Test
    void testShortnString() {
        assertEquals(
            "If the string is longer than maxLength it is...",
            StringHelper.shortnString("If the string is longer than maxLength it is split at the nearest blank space", 50));
    }

    @Test
    void testGetDomainName() {
        assertEquals(
            "learnweb.l3s.uni-hannover.de",
            StringHelper.getDomainName("https://learnweb.l3s.uni-hannover.de/lw/your_information/index.jsf"));
    }

    @Test
    void testGetDurationInMinutes() {
        assertEquals("1:31", StringHelper.getDurationInMinutes(91));
    }
}
