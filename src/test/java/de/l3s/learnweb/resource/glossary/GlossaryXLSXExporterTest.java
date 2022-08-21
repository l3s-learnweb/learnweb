package de.l3s.learnweb.resource.glossary;

import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Use to debug Glossary Export")
class GlossaryXLSXExporterTest {
    private static final Logger log = LogManager.getLogger(GlossaryXLSXExporterTest.class);

    @Test
    void convertGlossaryToWorkbook() {
        try {
            // GlossaryResource resource = getBeanProvider().getGlossaryDao().findById(231104).orElseThrow();
            //
            // GlossaryXLSXExporter exporter = new GlossaryXLSXExporter(resource, Locale.ENGLISH);
            // test(exporter.convertGlossaryToWorkbook(resource));

            log.debug("exported");
        } catch (Exception e) {
            log.error("fatal error", e);
        }
    }

    private void test(Workbook wb) throws IOException {
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
}
