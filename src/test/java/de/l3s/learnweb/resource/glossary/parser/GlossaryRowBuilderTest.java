package de.l3s.learnweb.resource.glossary.parser;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.l3s.learnweb.resource.glossary.GlossaryEntry;

class GlossaryRowBuilderTest {
    private Sheet sheet;
    private GlossaryRowBuilder builder;

    @BeforeEach
    void setUp() {
        HSSFWorkbook workbook = new HSSFWorkbook();
        sheet = workbook.createSheet();

        // columns: Topic 1, Topic 2, Topic 3, Definition, Term, Language
        builder = new GlossaryRowBuilder();
        builder.topicOneHeaderPosition = 0;
        builder.topicTwoHeaderPosition = 1;
        builder.topicThreeHeaderPosition = 2;
        builder.descriptionHeaderPosition = 3;
        builder.termHeaderPosition = 4;
        builder.languageHeaderPosition = 5;
        builder.languageMap = Map.of("English", Locale.ENGLISH, "German", Locale.GERMAN);
    }

    @Test
    void reportsEmptyTopicOne() {
        builder.build(createRow(1, "Computer Science", "", "", "A definition", "Machine Learning", "English"));
        builder.build(createRow(2, " ", "Biology", "", "Another definition", "Cell", "English"));

        assertEquals(1, builder.getErrors().size());
        assertEquals(2, builder.getErrors().getFirst().getRow());
    }

    @Test
    void reportsEmptyTopicOneInFirstRow() {
        builder.build(createRow(1, "", "", "", "", "Machine Learning", "English"));

        assertEquals(1, builder.getErrors().size());
    }

    @Test
    void joinsFurtherTermsOfEntry() {
        // the layout of GlossaryXLSXExporter: topics and definition are only set in the first row of an entry
        List<GlossaryEntry> entries = List.of(
            builder.build(createRow(1, "Computer Science", "AI", "", "A definition", "Machine Learning", "English")),
            builder.build(createRow(2, "", "", "", "", "Maschinelles Lernen", "German")),
            builder.build(createRow(3, "Biology", "", "", "Another definition", "Cell", "English")));

        assertTrue(builder.getErrors().isEmpty());

        List<GlossaryEntry> joined = GlossaryXLSParser.joinEntries(entries);
        assertEquals(2, joined.size());
        assertEquals("Computer Science", joined.getFirst().getTopicOne());
        assertEquals(2, joined.getFirst().getTerms().size());
        assertEquals("Maschinelles Lernen", joined.getFirst().getTerms().get(1).getTerm());
        assertEquals(1, joined.get(1).getTerms().size());
    }

    private Row createRow(int index, String... values) {
        Row row = sheet.createRow(index);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
        return row;
    }
}
