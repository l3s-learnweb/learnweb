package de.l3s.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

class NlpHelperTest
{
    @Test
    void getRitaWordnetDefinitions()
    {
        ArrayList<String> definitions = NlpHelper.getRitaWordnetDefinitions("hello world");

        assertEquals(2, definitions.size());
        assertEquals("hello world(n) - an expression of greeting: hello, hullo, hi, howdy.", definitions.get(0));
        assertEquals("hello world(a) - involving the entire earth; not limited or provincial in scope: global, planetary, world, worldwide.", definitions.get(1));
    }

    @Test
    void getRitaWordnetDefinitions2()
    {
        ArrayList<String> definitions = NlpHelper.getRitaWordnetDefinitions("elementary school");

        assertEquals(3, definitions.size());
        assertEquals("elementary school(v) - educate in or as if in a school: school, educate, train, cultivate, civilize, civilise.", definitions.get(0));
        assertEquals("elementary school(a) - easy and not involved or complicated: elementary, simple, uncomplicated, unproblematic, elemental, primary.", definitions.get(1));
        assertEquals("elementary school(n) - a school for young children; usually the first 6 or 8 grades.", definitions.get(2));
    }

    @Test
    void getRitaWordnetDefinitions3()
    {
        ArrayList<String> definitions = NlpHelper.getRitaWordnetDefinitions("which");

        assertEquals(0, definitions.size());
    }
}