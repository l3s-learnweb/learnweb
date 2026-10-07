package de.l3s.learnweb.resource.glossary;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import java.util.TreeSet;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.application.ViewExpiredException;
import jakarta.faces.event.ValueChangeEvent;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
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
import org.omnifaces.util.Faces;
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
import de.l3s.util.HashHelper;
import de.l3s.util.Image;
import de.l3s.util.StringHelper;
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

    /**
     * Values stored as {@link GlossaryTerm#getSource()} mapped to their message keys.
     * The stored values must not be changed, existing terms and exported glossaries rely on them.
     */
    private static final SequencedMap<String, String> SOURCES;

    static {
        SequencedMap<String, String> sources = new LinkedHashMap<>();
        sources.put("Wikipedia", "glossary.wikipedia");
        sources.put("encyclopaedia", "glossary.encyclopaedia");
        sources.put("monolingual dictionary", "glossary.mono_dictionary");
        sources.put("bilingual dictionary", "glossary.bi_dictionary");
        sources.put("scientific/academic publication", "glossary.publication");
        sources.put("institutional website", "glossary.website");
        sources.put("glossary", "glossary.glossary");
        sources.put("Linguee or Reverso", "glossary.linguee_reverso");
        sources.put("patients' websites and blogs", "glossary.web_blog");
        sources.put("other", "glossary.source_other");
        SOURCES = Collections.unmodifiableSequencedMap(sources);
    }

    private static final SequencedMap<String, List<String>> NO_SUBTOPICS = Collections.unmodifiableSequencedMap(new LinkedHashMap<>());

    /**
     * Suggested topics as topic 1 -> topic 2 -> topics 3, users can still enter any other topic on every level.
     */
    private static final SequencedMap<String, SequencedMap<String, List<String>>> TOPICS;

    static {
        SequencedMap<String, List<String>> medicine = new LinkedHashMap<>();
        medicine.put("Diseases and disorders", List.of("Signs and symptoms", "Diagnostic techniques", "Therapies", "Drugs"));
        medicine.put("Anatomy", List.of("Organs", "Bones", "Muscles", "Other"));
        medicine.put("Medical branches", List.of());
        medicine.put("Institutions", List.of());
        medicine.put("Professions", List.of());
        medicine.put("Food and nutrition", List.of());
        medicine.put("other", List.of());

        SequencedMap<String, List<String>> tourism = new LinkedHashMap<>();
        tourism.put("Accommodation", List.of());
        tourism.put("Surroundings", List.of());
        tourism.put("Heritage", List.of("History", "Architecture", "Festivals"));
        tourism.put("Food and Produce", List.of());
        tourism.put("Activities and Tours", List.of());
        tourism.put("Travel and Transport", List.of());

        SequencedMap<String, SequencedMap<String, List<String>>> topics = new LinkedHashMap<>();
        // for labint francesca.bianchi@unisalento.it
        topics.put("Environment", NO_SUBTOPICS);
        topics.put("European Politics", NO_SUBTOPICS);
        topics.put("Medicine", Collections.unmodifiableSequencedMap(medicine));
        topics.put("Tourism", Collections.unmodifiableSequencedMap(tourism));
        // for iryna.shylnikova@unisalento.it
        for (String topic : List.of("Business", "Migration", "Energy resources", "International relations", "Globalization", "Ecology")) {
            topics.put(topic, NO_SUBTOPICS);
        }
        TOPICS = Collections.unmodifiableSequencedMap(topics);
    }

    private GlossaryResource glossaryResource;

    private GlossaryEntry formEntry;

    private boolean optionMandatoryDescription;
    private boolean optionImportEnabled;
    private boolean optionExportEnabled;

    private ArrayList<Locale> tableLanguageFilter;

    private transient GlossaryParserResponse importResponse;
    private transient LazyGlossaryTableView lazyTableItems;
    private transient List<SelectItem> sources;
    private transient List<SelectItem> sourceFilterOptions; // cache, depends on the sources used in the glossary
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
        if (resource == null) {
            // happens on postback when the view scoped ResourceDetailBean was lost (e.g. expired), it is only initialized on page load
            throw new ViewExpiredException("The resource of the glossary is not loaded, the page has to be reloaded", Faces.getViewId());
        }
        glossaryResource = dao().getGlossaryDao().convertToGlossaryResource(resource).orElseThrow(BeanAssert.NOT_FOUND);

        long duration = Duration.between(start, Instant.now()).toMillis();
        if (duration > 500) {
            log.warn("Glossary loading time: {}", duration);
        }
        log(Action.glossary_open, glossaryResource);

        // convert tree like glossary structure to flat table
        repaintTable();

        onClearEntryForm();

        tableLanguageFilter = new ArrayList<>(glossaryResource.getAllowedLanguages());

        optionMandatoryDescription = user.getOrganisation().getOption(Option.Glossary_Mandatory_Description);
        optionImportEnabled = user.getOrganisation().getOption(Option.Glossary_Enable_Import);
        optionExportEnabled = user.getOrganisation().getOption(Option.Glossary_Enable_Export);
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
        if (!containsUndeletedTerms(formEntry)) {
            addGrowl(FacesMessage.SEVERITY_ERROR, "glossary.term_validation");
            return;
        }

        // the language is null if a term's select menu wasn't submitted (e.g. the client form was out of sync with the entry)
        if (formEntry.getTerms().stream().anyMatch(term -> !term.isDeleted() && term.getLanguage() == null)) {
            addGrowl(FacesMessage.SEVERITY_ERROR, "glossary.term_language_required");
            return;
        }

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

        Learnweb.dao().getGlossaryDao().saveEntry(formEntry);

        // the glossary edit form uses a working copy (clone) therefore we have to replace the original entry
        glossaryResource.getEntries().removeIf(entry -> entry.getId() == formEntry.getId());
        glossaryResource.getEntries().add(formEntry);

        repaintTable();
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
        repaintTable();

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

        addGrowl(FacesMessage.SEVERITY_INFO, "glossary.term_deleted", term.getTerm());

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

    /**
     * Clears the subtopics, unless the topic only changed in case (e.g. a suggestion was picked for a typed topic).
     */
    public void onChangeTopicOne(ValueChangeEvent event) {
        if (!isSameTopic(event)) {
            formEntry.setTopicTwo("");
            formEntry.setTopicThree("");
        }
    }

    /**
     * @see #onChangeTopicOne(ValueChangeEvent)
     */
    public void onChangeTopicTwo(ValueChangeEvent event) {
        if (!isSameTopic(event)) {
            formEntry.setTopicThree("");
        }
    }

    private static boolean isSameTopic(ValueChangeEvent event) {
        return Strings.CI.equals((String) event.getOldValue(), (String) event.getNewValue());
    }

    /**
     * Case-insensitive lookup of a topic's suggested subtopics.
     */
    private static <T> T findSubtopics(SequencedMap<String, T> topics, String topic, T fallback) {
        return topics.entrySet().stream().filter(entry -> Strings.CI.equals(entry.getKey(), topic))
            .map(Map.Entry::getValue).findFirst().orElse(fallback);
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
            List<GlossaryEntry> savedEntries = new ArrayList<>();
            try {
                for (GlossaryEntry entry : importResponse.getEntries()) {
                    // set creator of new entries
                    entry.setResourceId(glossaryResource.getId());
                    entry.setUserId(userId);
                    entry.getTerms().forEach(term -> term.setUserId(userId));
                    entry.setImported(true);

                    Learnweb.dao().getGlossaryDao().saveEntry(entry);
                    savedEntries.add(entry);

                    log(Action.glossary_entry_add, glossaryResource, entry.getId());
                    entry.getTerms().forEach(term -> log(Action.glossary_term_add, glossaryResource, term.getId()));
                }
            } finally {
                glossaryResource.getEntries().addAll(savedEntries);
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

                if (cellCurrent != null && !Strings.CI.equals(cellCurrent.getStringCellValue(), cellPrev.getStringCellValue())) {
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
                sheet.protectSheet(getXlsPassword());
            }
        } catch (RuntimeException | IOException e) {
            throw new HttpException("Error in postprocessing Glossary XLS for resource: " + glossaryResource.getId(), e);
        }
    }

    private String getXlsPassword() {
        String glossaryPassword = config().getProperty("glossary_password");
        if (glossaryPassword == null) {
            return HashHelper.sha256(config().getAppSecret() + "glossary");
        }
        return glossaryPassword;
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
     * force reload of the tableItems and the source filter options from the glossaryResource.
     */
    private void repaintTable() {
        tableItems = null;
        sourceFilterOptions = null;
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

    public Collection<String> getAvailableTopicTwo() {
        return findSubtopics(TOPICS, formEntry.getTopicOne(), NO_SUBTOPICS).sequencedKeySet();
    }

    public Collection<String> getAvailableTopicThree() {
        return findSubtopics(findSubtopics(TOPICS, formEntry.getTopicOne(), NO_SUBTOPICS), formEntry.getTopicTwo(), List.of());
    }

    public List<String> completeTopicOne(String query) {
        return StringHelper.filterContainsIgnoreCase(TOPICS.sequencedKeySet(), query);
    }

    public List<String> completeTopicTwo(String query) {
        return StringHelper.filterContainsIgnoreCase(getAvailableTopicTwo(), query);
    }

    public List<String> completeTopicThree(String query) {
        return StringHelper.filterContainsIgnoreCase(getAvailableTopicThree(), query);
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

    public List<SelectItem> getSources() {
        if (sources == null) {
            sources = SOURCES.entrySet().stream().map(source -> new SelectItem(source.getKey(), getLocaleMessage(source.getValue()))).toList();
        }
        return sources;
    }

    /**
     * @return options of the source column filter: the known sources followed by other values used in this glossary (e.g. imported from a file)
     */
    public List<SelectItem> getSourceFilterOptions() {
        if (sourceFilterOptions == null) {
            Set<String> otherSources = new TreeSet<>(String.CASE_INSENSITIVE_ORDER); // the filter is case-insensitive
            for (GlossaryEntry entry : glossaryResource.getEntries()) {
                for (GlossaryTerm term : entry.getTerms()) {
                    if (StringUtils.isNotBlank(term.getSource())) {
                        otherSources.add(term.getSource());
                    }
                }
            }
            SOURCES.keySet().forEach(otherSources::remove);

            List<SelectItem> options = new ArrayList<>(getSources());
            otherSources.forEach(source -> options.add(new SelectItem(source, source)));
            sourceFilterOptions = options;
        }
        return sourceFilterOptions;
    }

    /**
     * @return the translated label of a known source, otherwise the stored value (e.g. imported from a file)
     */
    public String getSourceLabel(String source) {
        if (source == null) {
            return null;
        }

        String msgKey = SOURCES.get(source);
        return msgKey != null ? getLocaleMessage(msgKey) : source;
    }

    public String getPronounciationVoice(Locale locale) {
        return PRONOUNCIATION_VOICES.getOrDefault(locale, null);
    }

    public boolean isOptionImportEnabled() {
        return optionImportEnabled;
    }

    public boolean isOptionExportEnabled() {
        return optionExportEnabled;
    }

    public Column[] getColumns() {
        return Column.values();
    }
}
