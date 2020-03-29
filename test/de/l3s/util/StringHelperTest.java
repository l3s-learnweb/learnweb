package de.l3s.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StringHelperTest
{
    @Test
    void testRemoveNewLines()
    {
        assertEquals("Hello world ", StringHelper.removeNewLines("Hello\nworld\n"));
    }

    @Test
    void testTrimNotAlphabetical()
    {
        assertEquals("fsdfsdfsv", StringHelper.trimNotAlphabetical("43242424 234324 34 %%43 fsdfsdfsv"));
        assertEquals("Hello world", StringHelper.trimNotAlphabetical("Hello world"));
        assertEquals("Hello world", StringHelper.trimNotAlphabetical(" @#@@ Hello world"));
    }

    @Test
    void testShortnString()
    {
        assertEquals(
                "If the string is longer than maxLength it is...",
                StringHelper.shortnString("If the string is longer than maxLength it is split at the nearest blank space", 50));
    }

    @Test
    void testParseInt()
    {
        assertEquals(123, StringHelper.parseInt("123"));
        assertEquals(-123, StringHelper.parseInt("-123"));

        assertEquals(-1, StringHelper.parseInt("hello"));
        assertEquals(0, StringHelper.parseInt("world", 0));
    }

    @Test
    void testGetDomainName()
    {
        assertEquals(
                "learnweb.l3s.uni-hannover.de",
                StringHelper.getDomainName("https://learnweb.l3s.uni-hannover.de/v3/lw/your_information/index.jsf"));
    }

    @Test
    void testGetDurationInMinutes()
    {
        assertEquals("1:31", StringHelper.getDurationInMinutes(91));
    }
}
