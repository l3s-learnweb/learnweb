package de.l3s.learnweb;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
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
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

public class ProcessOffice
{
    private final static int SIZE4_MAX_WIDTH = 1280;
    private final static int SIZE4_MAX_HEIGHT = 1024;

    public static void main(String args[])
    {
        // processXls("/Users/Rishita/test.xlsx");
        // processWord("/Users/Rishita/test");
        Logger logger = Logger.getLogger(ProcessOffice.class);

    }

    public InputStream processXls(InputStream in, Resource resource)
    {

        // Read workbook into HSSFWorkbook
        Workbook my_xls_workbook = null;

        try
        {
            my_xls_workbook = WorkbookFactory.create(in);
        }
        catch(EncryptedDocumentException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        catch(InvalidFormatException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        catch(IOException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        // Read worksheet into HSSFSheet
        org.apache.poi.ss.usermodel.Sheet my_worksheet = my_xls_workbook.getSheetAt(0);
        // To iterate over the rows
        Iterator<Row> rowIterator = my_worksheet.iterator();
        //We will create output PDF document objects at this point
        int noOfColumns = my_worksheet.getRow(0).getPhysicalNumberOfCells();
        Document iText_xls_2_pdf = new Document();
        File xlPdffile = new File(resource.getTitle() + ".pdf");
        try
        {
            PdfWriter.getInstance(iText_xls_2_pdf, new FileOutputStream(xlPdffile));
        }
        catch(FileNotFoundException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch(DocumentException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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
        try
        {
            iText_xls_2_pdf.add(my_table);
        }
        catch(DocumentException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        iText_xls_2_pdf.close();
        InputStream xlStream = null;
        try
        {
            xlStream = new FileInputStream(xlPdffile);
        }
        catch(FileNotFoundException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        xlPdffile.delete();
        return xlStream;
        //we created our pdf file..

    }

    public InputStream processWord(Resource resource, InputStream in)
    {
        WordprocessingMLPackage wordMLPackage = null;
        try
        {
            wordMLPackage = WordprocessingMLPackage.load(in);
        }
        catch(Docx4JException e2)
        {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }

        // Set up font mapper
        org.docx4j.fonts.Mapper fontMapper = new IdentityPlusMapper();
        try
        {
            wordMLPackage.setFontMapper(fontMapper);
        }
        catch(Exception e2)
        {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }

        // Example of mapping missing font Algerian to installed font Comic Sans MS
        PhysicalFont font = PhysicalFonts.getPhysicalFonts().get("Comic Sans MS");
        fontMapper.getFontMappings().put("Algerian", font);

        org.docx4j.convert.out.pdf.PdfConversion c = new org.docx4j.convert.out.pdf.viaXSLFO.Conversion(wordMLPackage);
        //  = new org.docx4j.convert.out.pdf.viaIText.Conversion(wordMLPackage);
        File wordPdf = new File(resource.getTitle() + ".pdf");
        OutputStream os = null;

        try
        {
            os = new java.io.FileOutputStream(wordPdf);
        }
        catch(FileNotFoundException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        PdfSettings setting = new PdfSettings();
        try
        {
            c.output(os, setting);
        }
        catch(Docx4JException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        InputStream pdfStream = null;
        try
        {
            pdfStream = new FileInputStream(wordPdf);
        }
        catch(FileNotFoundException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        wordPdf.delete();
        return pdfStream;

    }

    public static BufferedImage processPPT(InputStream in, Resource resource) throws IOException
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
        /* //creating an image file as output
        File pptImg = new File(resource.getTitle() + i + ".ppt");
        FileOutputStream out = new FileOutputStream(pptImg);
        
        javax.imageio.ImageIO.write(img, "png", out);
        ppt.write(out);
        out.close();*/

    }
}
