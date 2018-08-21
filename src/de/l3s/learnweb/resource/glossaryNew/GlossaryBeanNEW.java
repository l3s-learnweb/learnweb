package de.l3s.learnweb.resource.glossaryNew;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.faces.application.FacesMessage;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.imageio.ImageIO;
import javax.inject.Named;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Workbook;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.user.User;
import de.l3s.util.Misc;

@Named
@ViewScoped
public class GlossaryBeanNEW extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 7104637880221636543L;
    private static final Logger log = Logger.getLogger(GlossaryBeanNEW.class);

    private int resourceId;
    private GlossaryResource glossaryResource = new GlossaryResource();
    private List<GlossaryTableView> tableItems;
    private List<GlossaryTableView> filteredTableItems;
    private GlossaryEntry formEntry;
    private final List<SelectItem> availableTopicOne = new ArrayList<>();
    private List<SelectItem> availableTopicTwo = new ArrayList<>();
    private List<SelectItem> availableTopicThree = new ArrayList<>();
    private boolean paginator = true;
    private String toggleLabel = "Show All";

    public void onLoad() throws SQLException
    {

        User user = getUser();
        if(user == null)
            return;
        glossaryResource.setId(resourceId);
        getLearnweb().getGlossaryManager().loadGlossaryResource(this.glossaryResource);

        if(glossaryResource == null)
        {
            log.error("Error in loading glossary resource");
            addInvalidParameterMessage("resource_id");
            return;
        }
        availableTopicOne.add(new SelectItem("Environment"));
        availableTopicOne.add(new SelectItem("European Politics"));
        availableTopicOne.add(new SelectItem("Medicine"));
        availableTopicOne.add(new SelectItem("Tourism"));
        log(Action.glossary_open, glossaryResource);
        loadGlossaryTable(glossaryResource);
        setFilteredTableItems(tableItems);
        setNewFormEntry();

    }

    private void loadGlossaryTable(GlossaryResource glossaryResource2)
    {
        //set tableItems
        tableItems = getLearnweb().getGlossaryManager().convertToGlossaryTableView(glossaryResource2);

    }

    public void setGlossaryForm(GlossaryTableView tableItem) // is that ever used?
    {
        //set form entry
        formEntry = tableItem.getEntry();

    }

    public String onSave()
    {
        formEntry.setLastChangedByUserId(getUser().getId());
        formEntry.getTerms().forEach(term -> term.setLastChangedByUserId(getUser().getId()));
        if(formEntry.getTerms().size() == 0 || formEntry.getTerms().size() == numberOfDeletedTerms() || formEntry.getTerms().get(0).getTerm().isEmpty())
        {
            addMessage(FacesMessage.SEVERITY_ERROR, getLocaleMessage("Glossary.entry_validation"));
            return "/lw/glossary/glossary.jsf?resource_id=" + Integer.toString(getResourceId()) + "&faces-redirect=false";
        }
        try
        {
            getLearnweb().getGlossaryManager().saveEntry(formEntry, getUser().getId());
        }
        catch(SQLException e)
        {
            log.error("Unable to save entry for resource " + formEntry.getResourceId() + ", entry ID: " + formEntry.getId(), e);

        }
        addMessage(FacesMessage.SEVERITY_INFO, getLocaleMessage("Changes_saved")); // TODO you will show a success message even when an error occured ...
        return "/lw/glossary/glossary.jsf?resource_id=" + Integer.toString(getResourceId()) + "&faces-redirect=true";
    }

    public void setNewFormEntry()
    {
        formEntry = new GlossaryEntry();
        formEntry.setResourceId(resourceId);
        formEntry.addTerm(new GlossaryTerm());
    }

    public String deleteEntry(GlossaryTableView row)
    {
        row.getEntry().setDeleted(true);
        try
        {
            getLearnweb().getGlossaryManager().saveEntry(row.getEntry(), getUser().getId());
            addMessage(FacesMessage.SEVERITY_INFO, getLocaleMessage("Glossary.deleted") + "!");
            return "/lw/glossary/glossary.jsf?resource_id=" + Integer.toString(getResourceId()) + "&faces-redirect=true";
        }
        catch(SQLException e)
        {
            log.error("Unable to delete entry for resource " + row.getEntry().getResourceId() + ", entry ID: " + row.getEntry().getId(), e);
            addFatalMessage(e); // TODO use at least this. Don't ignore errors. The user must be informed
        }

        return "/lw/glossary/glossary.jsf?resource_id=" + Integer.toString(getResourceId()) + "&faces-redirect=false";
    }

    public void deleteTerm(GlossaryTerm term)
    {
        if(formEntry.getTerms().size() <= 1 || numberOfDeletedTerms() == formEntry.getTerms().size())
            return;

        if(term.getId() < 0)
        {
            formEntry.getTerms().remove(term);

        }
        else
        {
            formEntry.getTerms().remove(term);
            term.setDeleted(true);
            formEntry.getTerms().add(term);
        }
        addMessage(FacesMessage.SEVERITY_INFO, getLocaleMessage("Glossary.term") + ": " + term.getTerm() + " " + getLocaleMessage("Glossary.deleted") + "!");

    }

    public int numberOfDeletedTerms()
    {
        int deletedTerms = 0;

        for(GlossaryTerm t : formEntry.getTerms())
        {
            if(t.isDeleted())
                deletedTerms++;
        }
        return deletedTerms;
    }

    public void addTerm()
    {
        formEntry.addTerm(new GlossaryTerm());
        log(Action.glossary_term_add, glossaryResource); // should store resourceId + term_id

    }

    public void changeTopicOne(AjaxBehaviorEvent event)
    {
        createAvailableTopicsTwo();
        formEntry.setTopicTwo("");
        formEntry.setTopicThree("");
        availableTopicThree.clear();
    }

    private void createAvailableTopicsTwo()
    {
        availableTopicTwo.clear();

        if(formEntry.getTopicOne().equalsIgnoreCase("medicine"))
        {
            availableTopicTwo.add(new SelectItem("Diseases and disorders"));
            availableTopicTwo.add(new SelectItem("Anatomy"));
            availableTopicTwo.add(new SelectItem("Medical branches"));
            availableTopicTwo.add(new SelectItem("Institutions"));
            availableTopicTwo.add(new SelectItem("Professions"));
            availableTopicTwo.add(new SelectItem("Food and nutrition"));
            availableTopicTwo.add(new SelectItem("other"));
        }
        else if(formEntry.getTopicOne().equalsIgnoreCase("TOURISM"))
        {
            availableTopicTwo.add(new SelectItem("Accommodation"));
            availableTopicTwo.add(new SelectItem("Surroundings"));
            availableTopicTwo.add(new SelectItem("Heritage"));
            availableTopicTwo.add(new SelectItem("Food and Produce"));
            availableTopicTwo.add(new SelectItem("Activities and Tours"));
            availableTopicTwo.add(new SelectItem("Travel and Transport"));
        }
    }

    public void changeTopicTwo(AjaxBehaviorEvent event)
    {
        createAvailableTopicsThree();
        formEntry.setTopicThree("");
    }

    private void createAvailableTopicsThree()
    {
        availableTopicThree.clear();

        if(formEntry.getTopicOne().equalsIgnoreCase("medicine"))
        {
            if(formEntry.getTopicTwo().equalsIgnoreCase("Diseases and disorders"))
            {
                availableTopicThree.add(new SelectItem("Signs and symptoms"));
                availableTopicThree.add(new SelectItem("Diagnostic techniques"));
                availableTopicThree.add(new SelectItem("Therapies"));
                availableTopicThree.add(new SelectItem("Drugs"));
            }
            else if(formEntry.getTopicTwo().equalsIgnoreCase("Anatomy"))
            {
                availableTopicThree.add(new SelectItem("Organs"));
                availableTopicThree.add(new SelectItem("Bones"));
                availableTopicThree.add(new SelectItem("Muscles"));
                availableTopicThree.add(new SelectItem("Other"));
            }
        }
        if(formEntry.getTopicOne().equalsIgnoreCase("TOURISM"))
        {
            if(formEntry.getTopicTwo().equalsIgnoreCase("Heritage"))
            {
                availableTopicThree.add(new SelectItem("History"));
                availableTopicThree.add(new SelectItem("Architecture"));
                availableTopicThree.add(new SelectItem("Festivals"));
            }
        }
    }

    public void togglePaginator()
    {
        paginator = !paginator;
        toggleLabel = toggleLabel.equalsIgnoreCase("show all") ? getLocaleMessage("Glossary.collapse") : getLocaleMessage("Glossary.show_all");
    }

    public String getToggleLabel()
    {
        return toggleLabel;
    }

    public void postProcessXls(Object document)
    {
        try
        {
            log.debug("post processing glossary xls");

            HSSFWorkbook wb = (HSSFWorkbook) document;

            HSSFSheet sheet = wb.getSheetAt(0);

            HSSFRow row0 = sheet.getRow(1);
            HSSFCell cell0;

            File watermark = text2Image(getLearnweb().getResourceManager().getResource(resourceId).getUser().getUsername());

            InputStream is = new FileInputStream(watermark);

            byte[] bytes = IOUtils.toByteArray(is);
            int pictureIdx = wb.addPicture(bytes, Workbook.PICTURE_TYPE_PNG);
            is.close();
            CreationHelper helper = wb.getCreationHelper();
            Drawing drawing = sheet.createDrawingPatriarch();
            ClientAnchor anchor = helper.createClientAnchor();
            anchor.setAnchorType(ClientAnchor.AnchorType.DONT_MOVE_AND_RESIZE);

            if(row0 != null)
            {
                cell0 = row0.getCell(0);
            }
            else
            {

                return;
            }

            for(int i = 2; i <= sheet.getLastRowNum(); i++)
            {
                HSSFRow row = sheet.getRow(i);
                HSSFCell cell = row.getCell(0);

                if(cell != null)
                {
                    cell.setCellValue(cell.getStringCellValue());
                    if(cell.getStringCellValue().equals(cell0.getStringCellValue()))
                    {
                        cell0.setCellValue(cell0.getStringCellValue());
                        continue;
                    }
                    else
                    {
                        int rowIndex = i;
                        if(sheet.getRow(rowIndex).getCell(0) != null)
                        {
                            cell0 = sheet.getRow(rowIndex).getCell(0);
                            sheet.shiftRows(rowIndex, sheet.getLastRowNum(), 1);
                        }
                    }
                }

            }

            //Set owner details

            //set top-left corner of the picture,
            //subsequent call of Picture#resize() will operate relative to it
            int row1 = (sheet.getLastRowNum() / 4);
            int row2 = ((sheet.getLastRowNum() / 4) * 3);
            anchor.setCol1(3);
            anchor.setRow1(row1);
            anchor.setCol2(10);
            anchor.setRow2(row2);

            Picture pict = drawing.createPicture(anchor, pictureIdx);
            HSSFCellStyle copyrightStyle = wb.createCellStyle();
            copyrightStyle.setLocked(true);
            sheet.protectSheet("learnweb");
            watermark.delete();

        }
        catch(Exception e)
        {
            log.error("Error in postprocessing Glossary xls for resource: " + resourceId, e);
        }

    }

    public File text2Image(String textString) throws IOException
    {
        //create a File Object

        File file = File.createTempFile(textString, ".png");
        //File file = new File("./uploaded_files/tmp/" + textString + ".png");
        //create the font you wish to use
        Font font = new Font("Tahoma", Font.PLAIN, 18);

        //create the FontRenderContext object which helps us to measure the text
        FontRenderContext frc = new FontRenderContext(null, true, true);

        //get the height and width of the text
        Rectangle2D bounds = font.getStringBounds(textString, frc);
        int width = (int) bounds.getWidth();
        int height = (int) bounds.getHeight();

        //create a BufferedImage object
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        //calling createGraphics() to get the Graphics2D
        Graphics2D graphic = image.createGraphics();

        //set color and other parameters
        /*Color background = new Color(1f, 1f, 1f, 0.0f);

        graphic.setColor(background);
        graphic.setBackground(background);*/
        graphic.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
        graphic.fillRect(0, 0, width, height);
        graphic.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        Color textColor = new Color(0, 0, 0, 0.5f);
        graphic.setColor(textColor);
        graphic.setFont(font);
        graphic.drawString(textString, (float) bounds.getX(), (float) -bounds.getY());

        //releasing resources
        graphic.dispose();

        //creating the file
        ImageIO.write(image, "png", file);

        return file;
    }

    public void rotatePDF(Object document)
    {
        Document doc = (Document) document;
        doc.setPageSize(PageSize.A4.rotate());
    }

    public int getResourceId()
    {
        return resourceId;
    }

    public void setResourceId(int resourceId)
    {
        this.resourceId = resourceId;
    }

    public GlossaryResource getGlossaryResource()
    {
        return glossaryResource;
    }

    public List<GlossaryTableView> getTableItems()
    {
        return tableItems;
    }

    public List<GlossaryTableView> getFilteredTableItems()
    {
        return filteredTableItems;
    }

    public void setFilteredTableItems(List<GlossaryTableView> filteredTableItems)
    {
        this.filteredTableItems = filteredTableItems;
    }

    public int getCount()
    {
        return glossaryResource.getEntries().size();

    }

    public GlossaryEntry getFormEntry()
    {
        return formEntry;
    }

    public List<SelectItem> getAvailableTopicOne()
    {
        return availableTopicOne;
    }

    public List<SelectItem> getAvailableTopicTwo()
    {
        return availableTopicTwo;
    }

    public List<SelectItem> getAvailableTopicThree()
    {
        return availableTopicThree;
    }

    private transient List<SelectItem> allowedTermLanguages; // cache for the allowed languages select list

    public List<SelectItem> getAllowedTermLanguages()
    {
        if(null == allowedTermLanguages)
        {
            allowedTermLanguages = new ArrayList<>();
            for(String language : glossaryResource.getAllowedLanguages())
            {
                Locale locale = Locale.forLanguageTag(language);
                log.debug("add locales " + locale.getLanguage());
                allowedTermLanguages.add(new SelectItem(locale, getLocaleMessage("language_" + locale.getLanguage())));
            }
            allowedTermLanguages.sort(Misc.selectItemLabelComparator);
        }
        return allowedTermLanguages;
    }

    public boolean isPaginator()
    {
        return paginator;
    }

}
