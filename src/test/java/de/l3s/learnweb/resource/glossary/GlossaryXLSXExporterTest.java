package de.l3s.learnweb.resource.glossary;

import static org.junit.jupiter.api.Assertions.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class GlossaryXLSXExporterTest {
    private static final Logger log = LogManager.getLogger(GlossaryXLSXExporterTest.class);

    @Test
    void convertGlossaryToWorkbook() {
        GlossaryResource resource = createTestGlossary();
        GlossaryXLSXExporter exporter = new GlossaryXLSXExporter(Locale.ENGLISH);
        Workbook workbook = exporter.convertGlossaryToWorkbook(resource);

        assertRowEquals(workbook.getSheet("Glossary")
            .getRow(0), "Topic 1", "Topic 2", "Topic 3", "Definition", "Term", "Language", "Uses", "Pronunciation", "Acronym", "Source", "Phraseology");
        assertRowEquals(workbook.getSheet("Glossary")
            .getRow(3), "Computer Science", "Artificial Intelligence", "Data Science", "A field of study that gives computers the ability to learn without being explicitly programmed.", "Machine Learning", "English", "technical, academic", "ML", "Academic publications", "algorithms, neural networks, deep learning");
        assertRowEquals(workbook.getSheet("Glossary")
            .getRow(4), "", "", "", "", "Maschinelles Lernen", "German", "technical", "ML", "Technical papers", "Algorithmen, künstliche Intelligenz");

        // saveWorkbook(workbook);
    }

    private void saveWorkbook(Workbook wb) throws IOException {
        // Write the output to a file
        String file = "glossary.xls";
        if (wb instanceof XSSFWorkbook) {
            file += "x";
        }
        try (FileOutputStream out = new FileOutputStream(file)) {
            wb.write(out);
        }

        wb.close();
    }

    /**
     * Helper method to compare a workbook row with expected values
     *
     * @param row The row to check
     * @param expected List of expected values in the row
     */
    private void assertRowEquals(Row row, String... expected) {
        List<String> actualRow = new ArrayList<>();
        for (int i = 0; i < row.getLastCellNum(); i++) {
            String cellValue = "";
            Cell cell = row.getCell(i);
            if (cell != null) {
                cellValue = cell.getStringCellValue();
            }
            actualRow.add(cellValue);
        }
        assertIterableEquals(List.of(expected), actualRow, "Row does not match expected values");
    }

    private static GlossaryResource createTestGlossary() {
        GlossaryResource resource = new GlossaryResource();
        resource.setId(9);
        resource.setUserId(3);
        resource.setTitle("Test Glossary");
        resource.setEntries(new LinkedList<>());

        GlossaryEntry entry1 = new GlossaryEntry();
        entry1.setTopicOne("Environment");
        entry1.setTopicTwo("Climate");
        entry1.setTopicThree("Global Issues");
        entry1.setDescription("The long-term change in Earth's climate patterns, particularly the rise in global temperature due to human activities.");
        entry1.setDescriptionPasted(false);
        entry1.setImported(false);
        entry1.setTerms(new LinkedList<>());

        GlossaryTerm term11 = new GlossaryTerm();
        term11.setTerm("Climate Change");
        term11.setAcronym("CC");
        term11.setLanguage(Locale.ENGLISH);
        term11.setSource("IPCC Report");
        term11.setPhraseology("global warming, greenhouse effect");
        term11.setUses(List.of("scientific", "political"));
        entry1.getTerms().add(term11);

        GlossaryTerm term12 = new GlossaryTerm();
        term12.setTerm("Cambio Climático");
        term12.setAcronym("CC");
        term12.setLanguage(new Locale("es"));
        term12.setSource("UN Documents");
        term12.setPhraseology("calentamiento global");
        term12.setUses(List.of("scientific"));
        entry1.getTerms().add(term12);
        resource.getEntries().add(entry1);

        GlossaryEntry entry2 = new GlossaryEntry();
        entry2.setTopicOne("Computer Science");
        entry2.setTopicTwo("Artificial Intelligence");
        entry2.setTopicThree("Data Science");
        entry2.setDescription("A field of study that gives computers the ability to learn without being explicitly programmed.");
        entry2.setDescriptionPasted(false);
        entry2.setImported(false);
        entry2.setTerms(new LinkedList<>());

        GlossaryTerm term21 = new GlossaryTerm();
        term21.setTerm("Machine Learning");
        term21.setAcronym("ML");
        term21.setLanguage(Locale.ENGLISH);
        term21.setSource("Academic publications");
        term21.setPhraseology("algorithms, neural networks, deep learning");
        term21.setUses(List.of("technical", "academic"));
        entry2.getTerms().add(term21);

        GlossaryTerm term22 = new GlossaryTerm();
        term22.setTerm("Maschinelles Lernen");
        term22.setAcronym("ML");
        term22.setLanguage(Locale.GERMAN);
        term22.setSource("Technical papers");
        term22.setPhraseology("Algorithmen, künstliche Intelligenz");
        term22.setUses(List.of("technical"));
        entry2.getTerms().add(term22);
        resource.getEntries().add(entry2);

        GlossaryEntry entry3 = new GlossaryEntry();
        entry3.setTopicOne("Technology");
        entry3.setTopicTwo("Ethics");
        entry3.setTopicThree("Legal");
        entry3.setDescription("The right of individuals to control their personal information and how it is collected, used, and shared in digital environments.");
        entry3.setDescriptionPasted(false);
        entry3.setImported(false);
        entry3.setTerms(new LinkedList<>());

        GlossaryTerm term31 = new GlossaryTerm();
        term31.setTerm("Digital Privacy");
        term31.setAcronym("DP");
        term31.setLanguage(Locale.ENGLISH);
        term31.setSource("Legal frameworks");
        term31.setPhraseology("data protection, information privacy");
        term31.setUses(List.of("legal", "technical"));
        entry3.getTerms().add(term31);

        GlossaryTerm term32 = new GlossaryTerm();
        term32.setTerm("Vie Privée Numérique");
        term32.setAcronym("VPN");
        term32.setLanguage(Locale.FRENCH);
        term32.setSource("GDPR documentation");
        term32.setPhraseology("protection des données");
        term32.setUses(List.of("legal"));
        entry3.getTerms().add(term32);

        GlossaryTerm term33 = new GlossaryTerm();
        term33.setTerm("Цифрова конфіденційність");
        term33.setAcronym("ЦК");
        term33.setLanguage(new Locale("uk"));
        term33.setSource("Технічні стандарти");
        term33.setPhraseology("Конфіденційність");
        term33.setUses(List.of("legal"));
        entry3.getTerms().add(term33);
        resource.getEntries().add(entry3);
        return resource;
    }
}
