package de.l3s.learnweb;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.docx4j.convert.out.pdf.viaXSLFO.PdfSettings;
import org.docx4j.fonts.IdentityPlusMapper;
import org.docx4j.fonts.PhysicalFont;
import org.docx4j.fonts.PhysicalFonts;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;

import com.lowagie.text.Document;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

public class ProcessOffice
{

    static Logger logger = Logger.getLogger(ProcessOffice.class);

    public static void main(String args[])
    {
        // processXls("/Users/Rishita/test.xlsx");
        // processWord("/Users/Rishita/test");

    }

    public InputStream processXls(InputStream in, Resource resource)
    {
        try
        {
            // Read workbook into HSSFWorkbook
            Workbook my_xls_workbook = WorkbookFactory.create(in);

            // Read worksheet into HSSFSheet
            org.apache.poi.ss.usermodel.Sheet my_worksheet = my_xls_workbook.getSheetAt(0);
            // To iterate over the rows
            Iterator<Row> rowIterator = my_worksheet.iterator();
            //We will create output PDF document objects at this point
            int noOfColumns = my_worksheet.getRow(0).getPhysicalNumberOfCells();
            Document iText_xls_2_pdf = new Document();
            File xlPdffile = new File(resource.getTitle() + ".pdf");

            PdfWriter.getInstance(iText_xls_2_pdf, new FileOutputStream(xlPdffile));

            iText_xls_2_pdf.open();
            //we have two columns in the Excel sheet, so we create a PDF table with two columns
            //Note: There are ways to make this dynamic in nature, if you want to.
            PdfPTable my_table = new PdfPTable(noOfColumns);
            //We will use the object below to dynamically add new data to the table
            PdfPCell table_cell;
            //Loop through rows.
            while(rowIterator.hasNext())
            {
                Row row = rowIterator.next();
                Iterator<Cell> cellIterator = row.cellIterator();
                while(cellIterator.hasNext())
                {
                    Cell cell = cellIterator.next(); //Fetch CELL
                    switch(cell.getCellType())
                    { //Identify CELL type
                      //you need to add more code here based on
                      //your requirement / transformations
                    case Cell.CELL_TYPE_STRING:
                        //Push the data from Excel to PDF Cell
                        table_cell = new PdfPCell(new Phrase(cell.getStringCellValue()));
                        //feel free to move the code below to suit to your needs
                        my_table.addCell(table_cell);
                        break;
                    }
                    //next line
                }

            }
            //Finally add the table to PDF document

            iText_xls_2_pdf.add(my_table);

            iText_xls_2_pdf.close();
            InputStream xlStream = new FileInputStream(xlPdffile);

            xlPdffile.delete();
            return xlStream;
            //we created our pdf file..
        }
        catch(Exception e)
        {
            logger.error("Error in creating thumbnails from xls " + resource.getFormat() + " for resource: " + resource.getId());
        }
        return null;

    }

    public InputStream processWord(Resource resource, InputStream in)
    {
        try
        {
            WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.load(in);

            // Set up font mapper
            org.docx4j.fonts.Mapper fontMapper = new IdentityPlusMapper();

            wordMLPackage.setFontMapper(fontMapper);

            // Example of mapping missing font Algerian to installed font Comic Sans MS
            PhysicalFont font = PhysicalFonts.getPhysicalFonts().get("Comic Sans MS");
            fontMapper.getFontMappings().put("Algerian", font);

            org.docx4j.convert.out.pdf.PdfConversion c = new org.docx4j.convert.out.pdf.viaXSLFO.Conversion(wordMLPackage);
            //  = new org.docx4j.convert.out.pdf.viaIText.Conversion(wordMLPackage);
            File wordPdf = new File(resource.getTitle() + ".pdf");
            OutputStream os = new java.io.FileOutputStream(wordPdf);

            PdfSettings setting = new PdfSettings();

            c.output(os, setting);

            InputStream pdfStream = new FileInputStream(wordPdf);

            wordPdf.delete();
            return pdfStream;
        }
        catch(Exception e)
        {
            logger.error("Error in creating thumbnails from Word " + resource.getFormat() + " for resource: " + resource.getId());
        }
        return null;

    }

    public static BufferedImage processPPT(InputStream in, Resource resource)
    {
        try
        {
            //creating presentation

            XMLSlideShow ppt = new XMLSlideShow(in);

            //getting the dimensions and size of the slide 
            Dimension pgsize = ppt.getPageSize();
            List<XSLFSlide> slide = ppt.getSlides();

            int i = 0;

            BufferedImage img = new BufferedImage(pgsize.width, pgsize.height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = img.createGraphics();

            //clear the drawing area
            graphics.setPaint(Color.white);
            graphics.fill(new Rectangle2D.Float(0, 0, pgsize.width, pgsize.height));

            //render
            slide.get(i).draw(graphics);

            return img;
        }
        catch(Exception e)
        {
            logger.error("Error in creating thumbnails from PPT " + resource.getFormat() + " for resource: " + resource.getId());
        }
        return null;
    }
}
