package de.l3s.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

class NlpHelperTest {
    @Test
    void getRitaWordnetDefinitions() {
        ArrayList<String> definitions = NlpHelper.getRitaWordnetDefinitions("hello world");

        assertEquals(2, definitions.size());
        assertEquals("hello world(n) - an expression of greeting: hello, hullo, hi, howdy.", definitions.get(0));
        assertEquals("hello world(a) - involving the entire earth; not limited or provincial in scope: global, planetary, world, worldwide.", definitions.get(1));
    }

    @Test
    void getRitaWordnetDefinitionsEmpty() {
        ArrayList<String> definitions = NlpHelper.getRitaWordnetDefinitions("which");

        assertEquals(0, definitions.size());
    }
}
