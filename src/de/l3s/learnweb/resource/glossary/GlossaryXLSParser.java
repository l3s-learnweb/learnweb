package de.l3s.learnweb.resource.glossary;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.OfficeXmlFileException;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Row;
import org.primefaces.model.file.UploadedFile;

public class GlossaryXLSParser {
    private static final Logger log = LogManager.getLogger(GlossaryXLSParser.class);

    private final UploadedFile uploadedFile;
    private final Map<String, Locale> languageMap;

    public GlossaryXLSParser(UploadedFile uploadedFile, Map<String, Locale> languageMap) {
        this.uploadedFile = uploadedFile;
        this.languageMap = languageMap;
    }

    private boolean isEmptyRow(Row row) {
        if (row != null) {
            for (int cellNumber = 0; cellNumber < row.getPhysicalNumberOfCells(); ++cellNumber) {
                if (StringUtils.isNoneEmpty(row.getCell(cellNumber).getStringCellValue())) {
                    return false;
                }
            }
        }
        return true;
    }

    public GlossaryParserResponse parseGlossaryEntries() throws IOException {

        try (POIFSFileSystem fs = new POIFSFileSystem(uploadedFile.getInputStream()); HSSFWorkbook wb = new HSSFWorkbook(fs)) {
            HSSFSheet sheet = wb.getSheetAt(0);
            GlossaryRowBuilder glossaryRowBuilder = null;

            List<GlossaryEntry> glossaryEntries = new ArrayList<>();

            for (int rowNumber = 0; rowNumber < sheet.getPhysicalNumberOfRows(); ++rowNumber) {
                if (!isEmptyRow(sheet.getRow(rowNumber))) {
                    if (glossaryRowBuilder == null) { // parse header
                        glossaryRowBuilder = new GlossaryRowBuilder();
                        if (!glossaryRowBuilder.headerInit(sheet.getRow(rowNumber), languageMap)) {
                            log.error("Errors during header processing, can't continue.");

                            return new GlossaryParserResponse(null, glossaryRowBuilder.getErrors());
                        }
                    } else { // parse entry
                        GlossaryEntry entry = glossaryRowBuilder.build(sheet.getRow(rowNumber));
                        glossaryEntries.add(entry);
                    }
                }
            }

            if (glossaryRowBuilder == null || glossaryEntries.isEmpty()) {
                return new GlossaryParserResponse(new ParsingError(-1, "", "The file is empty"));
            }

            return new GlossaryParserResponse(joinEntries(glossaryEntries), glossaryRowBuilder.getErrors());
        } catch (OfficeXmlFileException e) {
            if (uploadedFile.getFileName().endsWith(".xlsx")) { // wrong file format
                return new GlossaryParserResponse(new ParsingError(-1, "", "Please save the file in *.xls format, also called Excel 97-2003, and try again."));
            } else {
                throw e;
            }
        }
    }

    private static List<GlossaryEntry> joinEntries(final List<GlossaryEntry> glossaryEntries) {
        List<GlossaryEntry> result = new ArrayList<>();
        for (final GlossaryEntry entry : glossaryEntries) {
            boolean alreadyExist = false;
            if (!result.isEmpty() && StringUtils.equals(result.get(result.size() - 1).getTopicOne(), entry.getTopicOne())
                && StringUtils.equals(result.get(result.size() - 1).getTopicTwo(), entry.getTopicTwo())
                && StringUtils.equals(result.get(result.size() - 1).getTopicThree(), entry.getTopicThree())
                && StringUtils.equals(result.get(result.size() - 1).getDescription(), entry.getDescription())) {
                result.get(result.size() - 1).getTerms().addAll(entry.getTerms());
                alreadyExist = true;
            }
            if (!alreadyExist) {
                result.add(entry);
            }
        }
        return result;
    }
}
