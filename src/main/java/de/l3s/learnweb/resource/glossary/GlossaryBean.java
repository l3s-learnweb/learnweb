package de.l3s.learnweb.resource.glossary;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.event.AjaxBehaviorEvent;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFPatriarch;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Workbook;
import org.omnifaces.util.Beans;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.FilesUploadEvent;
import org.primefaces.model.file.UploadedFile;

import com.lowagie.text.Document;
import com.lowagie.text.PageSize;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.exceptions.HttpException;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.resource.FileDao;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDetailBean;
import de.l3s.learnweb.resource.glossary.parser.GlossaryParserResponse;
import de.l3s.learnweb.resource.glossary.parser.GlossaryXLSParser;
import de.l3s.learnweb.resource.search.solrClient.FileInspector;
import de.l3s.learnweb.user.Organisation.Option;
import de.l3s.learnweb.user.User;
import de.l3s.util.Image;
import de.l3s.util.bean.BeanHelper;

@Named
@ViewScoped
public class GlossaryBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 7104637880221636543L;
    private static final Logger log = LogManager.getLogger(GlossaryBean.class);

    private static final Map<Locale, String> PRONOUNCIATION_VOICES = Map.ofEntries(
        Map.entry(new Locale.Builder().setLanguage("sq").build(), "Albanian Male"),
        Map.entry(new Locale.Builder().setLanguage("ar").build(), "Arabic Male"),
        Map.entry(new Locale.Builder().setLanguage("ca").build(), "Catalan Male"),
        Map.entry(Locale.CHINESE, "Chinese Male"),
        Map.entry(new Locale.Builder().setLanguage("ch").setRegion("HK").build(), "Chinese (Hong Kong) Female"),
        Map.entry(Locale.TAIWAN, "Chinese Taiwan Male"),
        Map.entry(new Locale.Builder().setLanguage("hr").build(), "Croatian Male"),
        Map.entry(new Locale.Builder().setLanguage("cs").build(), "Czech Female"),
        Map.entry(new Locale.Builder().setLanguage("da").build(), "Danish Male"),
        Map.entry(Locale.GERMAN, "Deutsch Male"),
        Map.entry(new Locale.Builder().setLanguage("nl").build(), "Dutch Male"),
        Map.entry(Locale.ENGLISH, "UK English Male"),
        Map.entry(Locale.UK, "UK English Male"),
        Map.entry(Locale.US, "US English Male"),
        Map.entry(new Locale.Builder().setLanguage("en").setRegion("AU").build(), "Australian Female"),
        Map.entry(new Locale.Builder().setLanguage("et").build(), "Estonian Female"),
        Map.entry(new Locale.Builder().setLanguage("fi").build(), "Finnish Female"),
        Map.entry(Locale.FRENCH, "French Female"),
        Map.entry(Locale.FRANCE, "French Female"),
        Map.entry(Locale.CANADA_FRENCH, "French Canadian Female"),
        Map.entry(new Locale.Builder().setLanguage("el").build(), "Greek Male"),
        Map.entry(new Locale.Builder().setLanguage("hi").setRegion("IN").build(), "Hindi Male"),
        Map.entry(new Locale.Builder().setLanguage("hu").build(), "Hungarian Female"),
        Map.entry(new Locale.Builder().setLanguage("is").build(), "Icelandic Male"),
        Map.entry(new Locale.Builder().setLanguage("in").build(), "Indonesian Male"),
        Map.entry(Locale.ITALIAN, "Italian Female"),
        Map.entry(Locale.JAPAN, "Japanese Male"),
        Map.entry(Locale.KOREA, "Korean Female"),
        Map.entry(new Locale.Builder().setLanguage("lv").build(), "Latvian Male"),
        Map.entry(new Locale.Builder().setLanguage("mk").build(), "Macedonian Male"),
        Map.entry(new Locale.Builder().setLanguage("no").build(), "Norwegian Female"),
        Map.entry(new Locale.Builder().setLanguage("pl").build(), "Polish Female"),
        Map.entry(new Locale.Builder().setLanguage("pt").build(), "Portuguese Female"),
        Map.entry(new Locale.Builder().setLanguage("pt").setRegion("PT").build(), "Portuguese Female"),
        Map.entry(new Locale.Builder().setLanguage("pt").setRegion("BR").build(), "Brazilian Portuguese Female"),
        Map.entry(new Locale.Builder().setLanguage("ro").build(), "Romanian Female"),
        Map.entry(new Locale.Builder().setLanguage("ru").build(), "Russian Male"),
        Map.entry(new Locale.Builder().setLanguage("sr").build(), "Serbian Male"),
        Map.entry(new Locale.Builder().setLanguage("sk").build(), "Slovak Female"),
        Map.entry(new Locale.Builder().setLanguage("es").build(), "Spanish Female"),
        Map.entry(new Locale.Builder().setLanguage("es").setRegion("ES").build(), "Spanish Female"),
        Map.entry(new Locale.Builder().setLanguage("es").setRegion("MX").build(), "Spanish Latin American Female"),
        Map.entry(new Locale.Builder().setLanguage("sv").build(), "Swedish Male"),
        Map.entry(new Locale.Builder().setLanguage("th").build(), "Thai Female"),
        Map.entry(new Locale.Builder().setLanguage("tr").build(), "Turkish Male"),
        Map.entry(new Locale.Builder().setLanguage("uk").build(), "Ukrainian Female"),
        Map.entry(new Locale.Builder().setLanguage("vi").build(), "Vietnamese Male")
    );

    private GlossaryResource glossaryResource;

    private GlossaryEntry formEntry;
    private final ArrayList<SelectItem> availableTopicOne = new ArrayList<>();
    private final ArrayList<SelectItem> availableTopicTwo = new ArrayList<>();
    private final ArrayList<SelectItem> availableTopicThree = new ArrayList<>();

    private boolean optionMandatoryDescription;
    private boolean optionImportEnabled;

    private ArrayList<Locale> tableLanguageFilter;

    private transient GlossaryParserResponse importResponse;
    private transient LazyGlossaryTableView lazyTableItems;
    private transient List<GlossaryTableView> tableItems;
    private transient List<SelectItem> allowedTermLanguages; // cache for the allowed languages select list

    @Inject
    private FileDao fileDao;

    @PostConstruct
    public void init() {
        User user = getUser();
        BeanAssert.authorized(user);

        Instant start = Instant.now();
        Resource resource = Beans.getInstance(ResourceDetailBean.class).getResource();
        glossaryResource = dao().getGlossaryDao().convertToGlossaryResource(resource).orElseThrow(BeanAssert.NOT_FOUND);

        long duration = Duration.between(start, Instant.now()).toMillis();
        if (duration > 500) {
            log.warn("Glossary loading time: {}", duration);
        }
        log(Action.glossary_open, glossaryResource);

        // for labint francesca.bianchi@unisalento.it
        availableTopicOne.add(new SelectItem("Environment"));
        availableTopicOne.add(new SelectItem("European Politics"));
        availableTopicOne.add(new SelectItem("Medicine"));
        availableTopicOne.add(new SelectItem("Tourism"));

        // for iryna.shylnikova@unisalento.it
        availableTopicOne.add(new SelectItem("Business"));
        availableTopicOne.add(new SelectItem("Migration"));
        availableTopicOne.add(new SelectItem("Energy resources"));
        availableTopicOne.add(new SelectItem("International relations"));
        availableTopicOne.add(new SelectItem("Globalization"));
        availableTopicOne.add(new SelectItem("Ecology"));

        // convert tree like glossary structure to flat table
        repaintTable();

        onClearEntryForm();

        tableLanguageFilter = new ArrayList<>(glossaryResource.getAllowedLanguages());

        optionMandatoryDescription = user.getOrganisation().getOption(Option.Glossary_Mandatory_Description);
        optionImportEnabled = user.getOrganisation().getOption(Option.Glossary_Enable_Import);
    }

    public void setGlossaryForm(GlossaryTableView tableItem) {
        //set form entry
        formEntry = new GlossaryEntry(tableItem.getEntry());
        //Reset ID to old entry ID as it is not copy action
        formEntry.setId(tableItem.getEntryId()); //entry ID
        //Reset original entry ID as it is not a copy action
        formEntry.setOriginalEntryId(tableItem.getEntry().getOriginalEntryId());
        //Reset old term ID and original term id as it is not a copy action
        for (GlossaryTerm term : formEntry.getTerms()) {
            term.setId(term.getOriginalTermId());
            term.setOriginalTermId(tableItem.getEntry().getTerm(term.getId()).getOriginalTermId());
        }
    }

    public void onSave() {
        //logging
        if (formEntry.getId() > 1) {
            log(Action.glossary_entry_edit, glossaryResource, formEntry.getId());
        } else {
            log(Action.glossary_entry_add, glossaryResource, formEntry.getId());
        }

        formEntry.setLastChangedByUserId(getUser().getId());

        //to reset fulltext search
        formEntry.setFulltext(null);

        //set last changed by user id for terms
        for (GlossaryTerm term : formEntry.getTerms()) {
            // TODO @kemkes: check if the term was really modified

            term.setLastChangedByUserId(getUser().getId());
            //log term edit actions
            if (term.getId() != 0) {
                log(Action.glossary_term_edit, glossaryResource, term.getId());
            }
        }

        if (!containsUndeletedTerms(formEntry)) {
            addGrowl(FacesMessage.SEVERITY_ERROR, "glossary.entry_validation");
            return;
        }

        Learnweb.dao().getGlossaryDao().saveEntry(formEntry);

        // the glossary edit form uses a working copy (clone) therefore we have to replace the original entry
        glossaryResource.getEntries().removeIf(entry -> entry.getId() == formEntry.getId());
        glossaryResource.getEntries().add(formEntry);

        addGrowl(FacesMessage.SEVERITY_INFO, "changes_saved");
        onClearEntryForm();
    }

    public void onClearEntryForm() {
        formEntry = new GlossaryEntry();
        formEntry.setResourceId(glossaryResource.getId());

        // add two terms
        onAddTerm();
        onAddTerm();
    }

    public void onDeleteEntry(GlossaryTableView row) {
        Learnweb.dao().getGlossaryEntryDao().deleteSoft(row.getEntry(), getUser().getId());

        //Remove entry from resource
        glossaryResource.getEntries().remove(row.getEntry());

        log(Action.glossary_entry_delete, glossaryResource, row.getEntryId());
        addGrowl(FacesMessage.SEVERITY_INFO, "entry_deleted");
    }

    public void onDeleteTerm(GlossaryTerm term) {
        if (!containsUndeletedTerms(formEntry)) {
            addGrowl(FacesMessage.SEVERITY_INFO, "glossary.term_validation");
            return;
        }

        term.setDeleted(true);

        if (term.getId() == 0) { // It's a new term. Safe to remove here.
            formEntry.getTerms().remove(term);
        }
        formEntry.setFulltext(null); // reset full text index

        addGrowl(FacesMessage.SEVERITY_INFO, getLocaleMessage("entry_deleted") + ": " + term.getTerm());

        log(Action.glossary_term_delete, glossaryResource, term.getId());
    }

    private boolean containsUndeletedTerms(GlossaryEntry entry) {
        int undeletedTerms = 0;

        for (GlossaryTerm term : entry.getTerms()) {
            if (!term.isDeleted()) {
                undeletedTerms++;
            }
        }
        return undeletedTerms > 0;
    }

    public void onAddTerm() {
        GlossaryTerm newTerm = new GlossaryTerm();

        // find a language that is not used yet in this entry
        List<Locale> unusedLanguages = new ArrayList<>(glossaryResource.getAllowedLanguages());
        for (GlossaryTerm term : formEntry.getTerms()) {
            unusedLanguages.remove(term.getLanguage());
        }
        if (unusedLanguages.isEmpty()) { // all languages have been used in this glossary
            unusedLanguages = glossaryResource.getAllowedLanguages();
        }

        newTerm.setLanguage(unusedLanguages.getFirst());
        formEntry.addTerm(newTerm);

        log(Action.glossary_term_add, glossaryResource, formEntry.getId());
    }

    public void onChangeTopicOne(AjaxBehaviorEvent event) {
        createAvailableTopicsTwo();
        formEntry.setTopicTwo("");
        formEntry.setTopicThree("");
        availableTopicThree.clear();
    }

    private void createAvailableTopicsTwo() {
        availableTopicTwo.clear();

        if (formEntry.getTopicOne().equalsIgnoreCase("medicine")) {
            availableTopicTwo.add(new SelectItem("Diseases and disorders"));
            availableTopicTwo.add(new SelectItem("Anatomy"));
            availableTopicTwo.add(new SelectItem("Medical branches"));
            availableTopicTwo.add(new SelectItem("Institutions"));
            availableTopicTwo.add(new SelectItem("Professions"));
            availableTopicTwo.add(new SelectItem("Food and nutrition"));
            availableTopicTwo.add(new SelectItem("other"));
        } else if (formEntry.getTopicOne().equalsIgnoreCase("TOURISM")) {
            availableTopicTwo.add(new SelectItem("Accommodation"));
            availableTopicTwo.add(new SelectItem("Surroundings"));
            availableTopicTwo.add(new SelectItem("Heritage"));
            availableTopicTwo.add(new SelectItem("Food and Produce"));
            availableTopicTwo.add(new SelectItem("Activities and Tours"));
            availableTopicTwo.add(new SelectItem("Travel and Transport"));
        }
    }

    public void onChangeTopicTwo(AjaxBehaviorEvent event) {
        createAvailableTopicsThree();
        formEntry.setTopicThree("");
    }

    private void createAvailableTopicsThree() {
        String topic1 = formEntry.getTopicOne();
        String topic2 = formEntry.getTopicTwo();
        availableTopicThree.clear();

        if (topic1.equalsIgnoreCase("medicine")) {
            if (topic2.equalsIgnoreCase("Diseases and disorders")) {
                availableTopicThree.add(new SelectItem("Signs and symptoms"));
                availableTopicThree.add(new SelectItem("Diagnostic techniques"));
                availableTopicThree.add(new SelectItem("Therapies"));
                availableTopicThree.add(new SelectItem("Drugs"));
            } else if (topic2.equalsIgnoreCase("Anatomy")) {
                availableTopicThree.add(new SelectItem("Organs"));
                availableTopicThree.add(new SelectItem("Bones"));
                availableTopicThree.add(new SelectItem("Muscles"));
                availableTopicThree.add(new SelectItem("Other"));
            }
        }
        if (topic1.equalsIgnoreCase("TOURISM")) {
            if (topic2.equalsIgnoreCase("Heritage")) {
                availableTopicThree.add(new SelectItem("History"));
                availableTopicThree.add(new SelectItem("Architecture"));
                availableTopicThree.add(new SelectItem("Festivals"));
            }
        }
    }

    /**
     * Maps all valid names of allowed languages to their Locale.
     */
    private Map<String, Locale> getLanguageMap() {
        HashMap<String, Locale> languageMap = new HashMap<>();
        for (Locale locale : BeanHelper.getSupportedLocales()) {
            for (Locale glossaryLocale : glossaryResource.getAllowedLanguages()) {
                languageMap.put(glossaryLocale.getDisplayLanguage(locale), glossaryLocale);
            }
        }
        return languageMap;
    }

    public void onImportXls(FileUploadEvent fileUploadEvent) throws IOException, IllegalAccessException {
        log.debug("parseXls");

        User user = getUser();
        if (user == null) {
            return;
        }

        if (!optionImportEnabled) {
            throw new IllegalAccessException("This feature isn't enabled for your organisation");
        }

        GlossaryXLSParser parser = new GlossaryXLSParser(fileUploadEvent.getFile(), getLanguageMap());

        importResponse = parser.parseGlossaryEntries();

        if (importResponse.isSuccessful()) {
            // persist parsed entries
            int userId = getUser().getId();
            for (GlossaryEntry entry : importResponse.getEntries()) {
                // set creator of new entries
                entry.setResourceId(glossaryResource.getId());
                entry.setUserId(userId);
                entry.getTerms().forEach(term -> term.setUserId(userId));
                entry.setImported(true);

                Learnweb.dao().getGlossaryDao().saveEntry(entry);
                glossaryResource.getEntries().add(entry);

                log(Action.glossary_entry_add, glossaryResource, entry.getId());
                entry.getTerms().forEach(term -> log(Action.glossary_term_add, glossaryResource, formEntry.getId()));
            }

            repaintTable();
        }
        log.debug("parseXls done");
    }

    public void postProcessXls(Object document) {
        User user = getUser();
        if (user == null) {
            return;
        }

        try {
            HSSFWorkbook wb = (HSSFWorkbook) document;
            HSSFSheet sheet = wb.getSheetAt(0);
            HSSFRow row0 = sheet.getRow(1);

            if (row0 == null) {
                return; // empty sheet -> nothing to do
            }

            // add empty separator row between topics
            HSSFCell cellPrev = row0.getCell(0);
            for (int i = 2; i <= sheet.getLastRowNum(); i++) {
                HSSFRow row = sheet.getRow(i);
                HSSFCell cellCurrent = row.getCell(0);

                if (cellCurrent != null && !StringUtils.equalsIgnoreCase(cellCurrent.getStringCellValue(), cellPrev.getStringCellValue())) {
                    cellPrev = cellCurrent;
                    sheet.shiftRows(i, sheet.getLastRowNum(), 1);
                }
            }

            if (user.getOrganisation().getOption(Option.Glossary_Add_Watermark)) {
                // create image from username
                Image watermark = Image.fromText(glossaryResource.getUser().getDisplayName());
                InputStream is = watermark.getInputStream();
                int pictureIdx = wb.addPicture(IOUtils.toByteArray(is), Workbook.PICTURE_TYPE_PNG);
                is.close();

                // create anchor
                CreationHelper helper = wb.getCreationHelper();
                HSSFPatriarch drawing = sheet.createDrawingPatriarch();
                ClientAnchor anchor = helper.createClientAnchor();
                anchor.setAnchorType(ClientAnchor.AnchorType.DONT_MOVE_AND_RESIZE);

                //set top-left corner of the picture,
                //subsequent call of Picture#resize() will operate relative to it
                int row = sheet.getLastRowNum() / 4;
                anchor.setCol1(0);
                anchor.setRow1(row);
                anchor.setCol2(3);
                anchor.setRow2(row + 3);

                drawing.createPicture(anchor, pictureIdx);

                HSSFCellStyle copyrightStyle = wb.createCellStyle();
                copyrightStyle.setLocked(true);
                sheet.protectSheet(Learnweb.SALT_1); // use SALT as password
            }
        } catch (RuntimeException | IOException e) {
            throw new HttpException("Error in postprocessing Glossary XLS for resource: " + glossaryResource.getId(), e);
        }
    }

    public void rotatePDF(Object document) {
        Document doc = (Document) document;
        doc.setPageSize(PageSize.A4.rotate());
    }

    public void handleFileUpload(FilesUploadEvent event) {
        try {
            log.debug("Handle File upload");
            for (UploadedFile uploadedFile : event.getFiles().getFiles()) {
                log.debug("Getting the fileInfo from uploaded file...");
                FileInspector.FileInfo info = getLearnweb().getResourceMetadataExtractor().getFileInfo(uploadedFile.getInputStream(), uploadedFile.getFileName());

                log.debug("Saving the file...");
                File file = new File(File.FileType.GLOSSARY, info.getFileName(), info.getMimeType());
                fileDao.save(file, uploadedFile.getInputStream());
                getFormEntry().getPictures().add(file);
            }
        } catch (IOException e) {
            throw new HttpException("Failed to handle file upload", e);
        }
    }

    public void handleDeletePicture(File picture) {
        getFormEntry().getPictures().remove(picture);
        getFormEntry().setPicturesCount(getFormEntry().getPictures().size());

        if (getFormEntry().getId() != 0) {
            fileDao.deleteGlossaryEntryFiles(getFormEntry(), Collections.singleton(picture));
        }
    }

    public GlossaryResource getGlossaryResource() {
        return glossaryResource;
    }

    /**
     * force reload of the tableItems from the glossaryResource.
     */
    private void repaintTable() {
        tableItems = null;
    }

    public List<GlossaryTableView> getTableItems() {
        if (null == tableItems) {
            tableItems = glossaryResource.getGlossaryTableView();
        }

        return tableItems;
    }

    public LazyGlossaryTableView getLazyTableItems() {
        if (null == lazyTableItems) {
            lazyTableItems = new LazyGlossaryTableView(glossaryResource);

        }

        return lazyTableItems;
    }

    public int getEntryCount() {
        return glossaryResource.getEntries().size();
    }

    public GlossaryEntry getFormEntry() {
        return formEntry;
    }

    public List<SelectItem> getAvailableTopicOne() {
        return availableTopicOne;
    }

    public List<SelectItem> getAvailableTopicTwo() {
        return availableTopicTwo;
    }

    public List<SelectItem> getAvailableTopicThree() {
        return availableTopicThree;
    }

    public boolean isOptionMandatoryDescription() {
        return optionMandatoryDescription;
    }

    public GlossaryParserResponse getImportResponse() {
        return importResponse;
    }

    public ArrayList<Locale> getTableLanguageFilter() {
        return tableLanguageFilter;
    }

    public void setTableLanguageFilter(ArrayList<Locale> tableLanguageFilter) {
        this.tableLanguageFilter = tableLanguageFilter;
    }

    public String getPronounciationVoice(Locale locale) {
        return PRONOUNCIATION_VOICES.getOrDefault(locale, null);
    }

    public boolean isOptionImportEnabled() {
        return optionImportEnabled;
    }

    public Column[] getColumns() {
        return Column.values();
    }
}
