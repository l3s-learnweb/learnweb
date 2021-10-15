package de.l3s.learnweb.resource.ted;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.omnifaces.util.Faces;
import org.primefaces.PrimeFaces;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.ResourceType;
import de.l3s.learnweb.resource.ted.TedManager.SummaryType;
import de.l3s.learnweb.user.Course;
import de.l3s.util.Misc;
import de.l3s.util.NlpHelper;

@Named
@ViewScoped
public class TedTranscriptBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = -1803725556672379697L;
    //private static final Logger log = LogManager.getLogger(TedTranscriptBean.class);

    private static final Pattern SPACES = Pattern.compile("\\s+");

    private Resource tedResource;
    private String transcriptLanguage;
    private int noteId;
    private String summaryTextS;
    private String summaryTextM;
    private String summaryTextL;

    /* To get the resource id from ted_video table since id_at_service may or may not be null in lw_resource for TED and
     * To get the resource id from lw_resource table corresponding to the video added by the admin for TEDx
     */
    private int videoResourceId;

    private int resourceId;
    private String locale;
    private List<SelectItem> languageList;

    @Inject
    private TedTranscriptDao tedTranscriptDao;

    public void onLoad() {
        BeanAssert.authorized(isLoggedIn());

        Resource resource = dao().getResourceDao().findByIdOrElseThrow(resourceId);
        BeanAssert.notDeleted(resource);
        setTedResource(resource);

        locale = getUserBean().getLocaleCode();
    }

    public Resource getTedResource() {
        return tedResource;
    }

    private void setTedResource(Resource tedResource) {
        this.tedResource = tedResource;

        if (tedResource.getService() == ResourceService.ted) {
            this.videoResourceId = tedTranscriptDao.findResourceIdBySlug(tedResource.getUrl()).orElseThrow(BeanAssert.NOT_FOUND);
        } else if (tedResource.getService() == ResourceService.tedx) {
            this.videoResourceId = tedTranscriptDao.findResourceIdByTedXUrl(tedResource.getUrl()).orElseThrow(BeanAssert.NOT_FOUND);
        }

        String transcript = tedResource.getTranscript();
        noteId = 0;

        if (StringUtils.isNotEmpty(transcript)) {
            Document doc = Jsoup.parse(transcript);
            Elements elements = doc.select("span");
            for (Element element : elements) {
                element.attr("id", Integer.toString(++noteId));
            }

            if (tedResource.getType() != ResourceType.website) {
                tedResource.setTranscript(doc.getElementsByTag("body").html());
            }

        } else {
            transcriptLanguage = "en";
            setTranscript();
        }

        List<TranscriptSummary> summaries = tedTranscriptDao.findTranscriptSummariesByResourceId(resourceId);
        for (TranscriptSummary summary : summaries) {
            if (summary.getSummaryType() == SummaryType.SHORT) {
                summaryTextS = summary.getSummaryText();
            } else if (summary.getSummaryType() == SummaryType.LONG) {
                summaryTextM = summary.getSummaryText();
            } else if (summary.getSummaryType() == SummaryType.DETAILED) {
                summaryTextL = summary.getSummaryText();
            }
        }
    }

    public void setTranscript() {
        List<Transcript.Paragraph> paragraphs = tedTranscriptDao.findTranscriptsParagraphs(videoResourceId, transcriptLanguage);

        StringBuilder sb = new StringBuilder();
        paragraphs.forEach(paragraph -> {
            sb.append(paragraph.getStartTimeInMinutes()).append("\t");
            sb.append(paragraph.text()).append("\n");
        });

        // TODO: but why do we need this (two lines below)?
        String transcript = sb.toString().replaceAll("\n", "<br/><br/>");
        Document doc = Jsoup.parse(transcript);
        tedResource.setTranscript(doc.getElementsByTag("body").html());
    }

    /**
     * Saves the changes in the TED transcript such as selections and user annotations;
     * Also logs the 'save' event.
     */
    public void commandSaveResource() {
        String transcript = Faces.getRequestParameter("transcript");

        tedResource.setTranscript(transcript);
        tedResource.save();
        TranscriptLog transcriptLog = new TranscriptLog(getUser().getId(), tedResource.getId(), "", "", "save transcript", Instant.now());
        tedTranscriptDao.saveTranscriptLog(transcriptLog);

        getUser().clearCaches();
        addGrowl(FacesMessage.SEVERITY_INFO, "Changes_saved");
    }

    /**
     * Submits the transcript of a TED resource for final evaluation;
     * Saves the 'submit' event and also the final selections in lw_transcript_selections.
     */
    public void commandSubmitResource() {
        String transcript = Faces.getRequestParameter("transcript");
        tedResource.setTranscript(transcript);
        tedResource.setReadOnlyTranscript(true);

        tedResource.save();
        TranscriptLog transcriptLog = new TranscriptLog(getUser().getId(), tedResource.getId(), "", "", "submit transcript", Instant.now());
        tedTranscriptDao.saveTranscriptLog(transcriptLog);
        tedTranscriptDao.saveTranscriptSelection(transcript, tedResource.getId());

        getUser().clearCaches();
        addGrowl(FacesMessage.SEVERITY_INFO, "Transcript Submitted");
    }

    /**
     * Stores a transcript action such as selection, de-selection, user annotation.
     */
    public void commandSaveLog() {
        Map<String, String> params = Faces.getRequestParameterMap();
        String word = params.get("word");
        String userAnnotation = params.get("user_annotation");
        String action = params.get("action");

        TranscriptLog transcriptLog = new TranscriptLog(getUser().getId(), tedResource.getId(), word, userAnnotation, action, Instant.now());
        tedTranscriptDao.saveTranscriptLog(transcriptLog);
    }

    /**
     * Retrieves the set of synonyms from WordNet for given selection of word.
     */
    public void commandSetSynonyms() {
        String words = Faces.getRequestParameter("word");
        StringBuilder synonymsList = new StringBuilder();
        int wordCount = SPACES.split(words.trim()).length;

        if (wordCount <= 5) {
            ArrayList<String> definitions = NlpHelper.getRitaWordnetDefinitions(words);

            for (String definition : definitions) {
                synonymsList.append(definition).append("&lt;br/&gt;");
            }

            if (definitions.isEmpty() && wordCount == 1) {
                synonymsList.append(getLocaleMessage("No definition available"));
            } else if (synonymsList.isEmpty()) {
                synonymsList.append(getLocaleMessage("Multiple"));
            }
            PrimeFaces.current().ajax().addCallbackParam("synonyms", synonymsList.toString());
        } else {
            PrimeFaces.current().ajax().addCallbackParam("synonyms", "multiple");
        }
    }

    public void submitShortSummary() {
        if (summaryTextS != null && !summaryTextS.isEmpty()) {
            tedTranscriptDao.saveTranscriptSummary(getUser().getId(), resourceId, SummaryType.SHORT, summaryTextS);
        }
    }

    public void submitLongSummary() {
        if (summaryTextM != null && !summaryTextM.isEmpty()) {
            tedTranscriptDao.saveTranscriptSummary(getUser().getId(), resourceId, SummaryType.LONG, summaryTextM);
        }
    }

    public void submitDetailedSummary() {
        if (summaryTextL != null && !summaryTextL.isEmpty()) {
            tedTranscriptDao.saveTranscriptSummary(getUser().getId(), resourceId, SummaryType.DETAILED, summaryTextL);
        }
    }

    public String getTranscriptLanguage() {
        return transcriptLanguage;
    }

    public void setTranscriptLanguage(String transcriptLanguage) {
        this.transcriptLanguage = transcriptLanguage;
    }

    public List<SelectItem> getLanguageList() {
        if (!locale.equals(getUserBean().getLocaleCode())) {
            languageList = null;
            locale = getUserBean().getLocaleCode();
        }

        if (languageList == null) {
            Map<String, String> langList;
            languageList = new LinkedList<>();
            langList = tedTranscriptDao.findLanguages(videoResourceId);

            if (langList.isEmpty()) {
                languageList.add(new SelectItem("NA", "No Transcripts Available"));
            } else {
                String langFromPropFile;

                for (Entry<String, String> entry : langList.entrySet()) {
                    langFromPropFile = getLocaleMessage("language_" + entry.getValue());
                    if (langFromPropFile == null) {
                        langFromPropFile = entry.getKey();
                    }

                    languageList.add(new SelectItem(entry.getValue(), langFromPropFile));
                }
                languageList.sort(Misc.SELECT_ITEM_LABEL_COMPARATOR);
            }
        }
        return languageList;
    }

    public int getNoteId() {
        return noteId;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public List<Course> getCourses() {
        return getUser().getCourses();
    }

    public String getSummaryTextS() {
        return summaryTextS;
    }

    public void setSummaryTextS(String summaryTextS) {
        this.summaryTextS = summaryTextS;
    }

    public String getSummaryTextM() {
        return summaryTextM;
    }

    public void setSummaryTextM(String summaryTextM) {
        this.summaryTextM = summaryTextM;
    }

    public String getSummaryTextL() {
        return summaryTextL;
    }

    public void setSummaryTextL(String summaryTextL) {
        this.summaryTextL = summaryTextL;
    }
}
