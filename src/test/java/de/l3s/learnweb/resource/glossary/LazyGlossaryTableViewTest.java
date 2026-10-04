package de.l3s.learnweb.resource.glossary;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.MatchMode;

class LazyGlossaryTableViewTest {
    private static GlossaryEntry createEntry(String topicTwo, String acronym) {
        GlossaryEntry entry = new GlossaryEntry();
        entry.setCreatedAt(LocalDateTime.now());
        entry.setTopicOne("Medicine");
        entry.setTopicTwo(topicTwo);
        GlossaryTerm term = new GlossaryTerm();
        term.setTerm("Cell");
        term.setAcronym(acronym);
        entry.addTerm(term);
        return entry;
    }

    private static List<GlossaryTableView> load(GlossaryResource resource, String field, String value) {
        FilterMeta filter = FilterMeta.builder().field(field).filterValue(value).matchMode(MatchMode.CONTAINS).build();
        return new LazyGlossaryTableView(resource).load(0, 200, Map.of(), Map.of(field, filter));
    }

    @Test
    void columnFiltersSkipNullValues() {
        // the columns are nullable in the database, e.g. topic_two and acronym
        GlossaryResource resource = new GlossaryResource();
        resource.setEntries(List.of(createEntry(null, null), createEntry("Anatomy", "ATP")));

        List<GlossaryTableView> topicRows = load(resource, "topicTwo", "anat");
        assertEquals(1, topicRows.size());
        assertEquals("<b>Anat</b>omy", topicRows.getFirst().getTopicTwoHtml());

        List<GlossaryTableView> acronymRows = load(resource, "acronym", "atp");
        assertEquals(1, acronymRows.size());
        assertEquals("<b>ATP</b>", acronymRows.getFirst().getAcronymHtml());
    }

    @Test
    void columnFilterMatchesWhenLowercasingChangesLength() {
        // "İ".toLowerCase() is two chars long, the filter must not be lowercased before matching
        GlossaryResource resource = new GlossaryResource();
        resource.setEntries(List.of(createEntry("İstanbul", null)));

        List<GlossaryTableView> rows = load(resource, "topicTwo", "İstanbul");
        assertEquals(1, rows.size());
        assertEquals("<b>İstanbul</b>", rows.getFirst().getTopicTwoHtml());
    }
}
