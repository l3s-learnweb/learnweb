package de.l3s.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

class NlpHelperTest {
    @Test
    void getWordnetDefinitions() {
        ArrayList<String> definitions = NlpHelper.getWordnetDefinitions("hello world");

        assertEquals(2, definitions.size());
        assertEquals("hello world(noun) - an expression of greeting; \"every morning they exchanged polite hellos\": hello, hullo, hi, howdy, how-do-you-do.", definitions.get(0));
        assertEquals("hello world(adjective) - involving the entire earth; not limited or provincial in scope; \"global war\"; \"global monetary policy\"; \"neither national nor continental but planetary\"; \"a world crisis\"; \"of worldwide significance\": global, planetary, world, worldwide, world-wide.", definitions.get(1));
    }

    @Test
    void getWordnetDefinitionsEmpty() {
        ArrayList<String> definitions = NlpHelper.getWordnetDefinitions("which");

        assertEquals(0, definitions.size());
    }
}
