package de.l3s.learnweb.resource.glossary;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import de.l3s.learnweb.i18n.MessagesBundle;

/**
 * Allows exporting a glossary into an Excel file.
 *
 * @author Philipp
 */
public final class GlossaryXLSXExporter {
    private final ResourceBundle bundle; // language of the exported header fields

    public GlossaryXLSXExporter(Locale locale) {
        this.bundle = MessagesBundle.of(locale);
    }

    public InputStream streamWorkbook(GlossaryResource resource) throws IOException {
        return streamWorkbook(convertGlossaryToWorkbook(resource));
    }

    public static InputStream streamWorkbook(Workbook wb) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            wb.write(out);

            try (ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray())) {
                return in;
            }
        }
    }

    public Workbook convertGlossaryToWorkbook(GlossaryResource resource) {
        Workbook wb;

        wb = new XSSFWorkbook();
        Map<String, CellStyle> styles = createStyles(wb);

        Sheet sheet = wb.createSheet("Glossary");

        // turn off gridlines
        sheet.setDisplayGridlines(false);
        sheet.setPrintGridlines(false);
        // sheet.setFitToPage(true);
        // sheet.setHorizontallyCenter(true);
        PrintSetup printSetup = sheet.getPrintSetup();
        printSetup.setLandscape(true);

        // the following three statements are required only for HSSF
        sheet.setAutobreaks(true);
        printSetup.setFitHeight((short) 1);
        printSetup.setFitWidth((short) 1);

        // the header row: centered text in 48pt font
        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(12.75f);

        int columnIndex = 0;
        for (Column column : Column.values()) {
            Cell cell = headerRow.createCell(columnIndex++);
            cell.setCellValue(bundle.getString(column.getMsgKey()));
            cell.setCellStyle(styles.get("header"));
        }

        // freeze the first row
        sheet.createFreezePane(0, 1);

        Row row;
        Cell cell;

        int rowIndex = 1;
        int entryIndex = 0;

        for (GlossaryEntry entry : resource.getEntries()) {
            row = sheet.createRow(rowIndex);

            CellStyle style = styles.get("cell_normal_" + (entryIndex++ % 2));

            cell = row.createCell(0);
            cell.setCellValue(entry.getTopicOne());
            cell.setCellStyle(style);

            cell = row.createCell(1);
            cell.setCellValue(entry.getTopicTwo());
            cell.setCellStyle(style);

            cell = row.createCell(2);
            cell.setCellValue(entry.getTopicThree());
            cell.setCellStyle(style);

            cell = row.createCell(3);
            cell.setCellValue(entry.getDescription());
            cell.setCellStyle(style);

            // merge the topic cells if they belong to more than one term
            int rangeEndRow = rowIndex + entry.getTerms().size() - 1;
            if (rangeEndRow > rowIndex) {
                for (int column = 0; column < 4; column++) {
                    sheet.addMergedRegion(new CellRangeAddress(rowIndex, rangeEndRow, column, column));
                }
            }

            for (GlossaryTerm term : entry.getTerms()) {
                cell = row.createCell(4);
                cell.setCellValue(term.getTerm());
                cell.setCellStyle(style);

                cell = row.createCell(5);
                cell.setCellValue(term.getLanguage().getDisplayLanguage(bundle.getLocale()));
                cell.setCellStyle(style);

                cell = row.createCell(6);
                cell.setCellValue(StringUtils.join(term.getUses(), ", "));
                cell.setCellStyle(style);

                cell = row.createCell(7);
                cell.setCellValue(term.getAcronym());
                cell.setCellStyle(style);

                cell = row.createCell(8);
                cell.setCellValue(term.getSource());
                cell.setCellStyle(style);

                cell = row.createCell(9);
                cell.setCellValue(term.getPhraseology());
                cell.setCellStyle(style);

                row = sheet.createRow(++rowIndex);
            }
        }

        // set column widths, the width is measured in units of 1/256th of a character width
        sheet.setColumnWidth(0, 256 * 20);
        sheet.setColumnWidth(1, 256 * 20);
        sheet.setColumnWidth(2, 256 * 20);
        sheet.setColumnWidth(3, 256 * 25);
        sheet.setColumnWidth(4, 256 * 20);
        sheet.setColumnWidth(5, 256 * 12);
        sheet.setColumnWidth(6, 256 * 12);
        sheet.setColumnWidth(7, 256 * 12);
        sheet.setColumnWidth(8, 256 * 30);
        sheet.setColumnWidth(9, 256 * 30);
        // sheet.setZoom(75); //75% scale

        return wb;
    }

    /**
     * create a library of cell styles.
     */
    private static Map<String, CellStyle> createStyles(Workbook wb) {
        Map<String, CellStyle> styles = new HashMap<>();
        DataFormat df = wb.createDataFormat();

        CellStyle style;
        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        style = createBorderedStyle(wb);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFont(headerFont);
        styles.put("header", style);

        style = createBorderedStyle(wb);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setWrapText(true);
        styles.put("cell_normal_0", style);

        style = createBorderedStyle(wb);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.TOP);
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setWrapText(true);
        styles.put("cell_normal_1", style);

        Font font1 = wb.createFont();
        font1.setBold(true);
        style = createBorderedStyle(wb);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setFont(font1);
        styles.put("cell_b", style);

        style = createBorderedStyle(wb);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFont(font1);
        styles.put("cell_b_centered", style);

        style = createBorderedStyle(wb);
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setFont(font1);
        style.setDataFormat(df.getFormat("d-mmm"));
        styles.put("cell_b_date", style);

        style = createBorderedStyle(wb);
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setFont(font1);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setDataFormat(df.getFormat("d-mmm"));
        styles.put("cell_g", style);

        Font font2 = wb.createFont();
        font2.setColor(IndexedColors.BLUE.getIndex());
        font2.setBold(true);
        style = createBorderedStyle(wb);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setFont(font2);
        styles.put("cell_bb", style);

        style = createBorderedStyle(wb);
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setFont(font1);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setDataFormat(df.getFormat("d-mmm"));
        styles.put("cell_bg", style);

        Font font3 = wb.createFont();
        font3.setFontHeightInPoints((short) 14);
        font3.setColor(IndexedColors.DARK_BLUE.getIndex());
        font3.setBold(true);
        style = createBorderedStyle(wb);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setFont(font3);
        style.setWrapText(true);
        styles.put("cell_h", style);

        style = createBorderedStyle(wb);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setWrapText(true);
        styles.put("cell_normal_centered", style);

        style = createBorderedStyle(wb);
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setWrapText(true);
        style.setDataFormat(df.getFormat("d-mmm"));
        styles.put("cell_normal_date", style);

        style = createBorderedStyle(wb);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setIndention((short) 1);
        style.setWrapText(true);
        styles.put("cell_indented", style);

        style = createBorderedStyle(wb);
        style.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put("cell_blue", style);

        return styles;
    }

    private static CellStyle createBorderedStyle(Workbook wb) {
        BorderStyle thin = BorderStyle.THIN;
        short black = IndexedColors.BLACK.getIndex();

        CellStyle style = wb.createCellStyle();
        style.setBorderRight(thin);
        style.setRightBorderColor(black);
        style.setBorderBottom(thin);
        style.setBottomBorderColor(black);
        style.setBorderLeft(thin);
        style.setLeftBorderColor(black);
        style.setBorderTop(thin);
        style.setTopBorderColor(black);
        return style;
    }
}
