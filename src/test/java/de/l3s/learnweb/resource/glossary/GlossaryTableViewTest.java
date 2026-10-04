package de.l3s.learnweb.resource.glossary;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import de.l3s.util.StringHelper;

class GlossaryTableViewTest {
    @Test
    void highlightsGlobalFilterAndEscapes() {
        GlossaryEntry entry = new GlossaryEntry();
        entry.setTopicOne("Bio <logy>");
        GlossaryTerm term = new GlossaryTerm();
        term.setTerm("Cell & membrane");

        // the global filter is stored as "fulltext" in lower case
        GlossaryTableView view = new GlossaryTableView(entry, term, Map.of("fulltext", StringHelper.compileHighlightQuery("cell")));
        assertEquals("<b>Cell</b> &amp; membrane", view.getTermHtml());
        assertEquals("Bio &lt;logy&gt;", view.getTopicOneHtml());
        // the stored values are not modified
        assertEquals("Cell & membrane", view.getTerm());
    }

    @Test
    void columnFilterTakesPrecedence() {
        GlossaryEntry entry = new GlossaryEntry();
        entry.setTopicOne("Medicine and biology");

        GlossaryTableView view = new GlossaryTableView(entry, new GlossaryTerm(),
            Map.of("fulltext", StringHelper.compileHighlightQuery("medicine"), "topicOne", StringHelper.compileHighlightQuery("bio")));
        assertEquals("Medicine and <b>bio</b>logy", view.getTopicOneHtml());
        assertEquals("", view.getAcronymHtml());
    }

    @Test
    void highlightsSourceLabelByGlobalFilter() {
        GlossaryTerm term = new GlossaryTerm();
        term.setSource("bilingual dictionary");
        Map<String, Pattern> filter = Map.of("fulltext", StringHelper.compileHighlightQuery("dictionary"));

        // the label contains the query
        assertEquals("Bilingual <b>dictionary</b>", new GlossaryTableView(new GlossaryEntry(), term, filter).getSourceHtml("Bilingual dictionary"));
        // only the stored value matches, e.g. the label is translated
        assertEquals("<b>Zweisprachiges Wörterbuch</b>", new GlossaryTableView(new GlossaryEntry(), term, filter).getSourceHtml("Zweisprachiges Wörterbuch"));
        // the source column filter is a dropdown, its value is not highlighted
        Map<String, Pattern> sourceFilter = Map.of("source", StringHelper.compileHighlightQuery("bilingual dictionary"));
        assertEquals("Bilingual &amp; dictionary", new GlossaryTableView(new GlossaryEntry(), term, sourceFilter).getSourceHtml("Bilingual & dictionary"));
        assertEquals("", new GlossaryTableView(new GlossaryEntry(), new GlossaryTerm(), filter).getSourceHtml(null));
    }

    @Test
    void escapesWithoutFilters() {
        GlossaryEntry entry = new GlossaryEntry();
        entry.setDescription("<i>text</i>");

        assertEquals("&lt;i&gt;text&lt;/i&gt;", new GlossaryTableView(entry, new GlossaryTerm()).getDescriptionHtml());
    }
}
