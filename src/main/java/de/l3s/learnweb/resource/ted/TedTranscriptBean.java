package de.l3s.learnweb.resource.ted;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.omnifaces.util.Beans;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceAnnotationBean;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.ResourceType;
import de.l3s.learnweb.resource.ted.TedManager.SummaryType;
import de.l3s.learnweb.user.Course;
import de.l3s.util.NlpHelper;
import de.l3s.util.bean.BeanHelper;
import de.l3s.util.Misc;

@Named
@ViewScoped
public class TedTranscriptBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = -1803725556672379697L;
    private static final Logger log = LogManager.getLogger(TedTranscriptBean.class);

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
    private LinkedList<SelectItem> languageList;

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
     * Submits the transcript of a TED resource for final evaluation;
     * Saves the 'submit' event and also the final selections in lw_transcript_selections.
     */
    public void commandSubmitResource() {
        Beans.getInstance(ResourceAnnotationBean.class).commandCommitAnnotation(tedResource);
        tedTranscriptDao.saveTranscriptSelection(tedResource.getTranscript(), tedResource.getId());
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
            languageList = new LinkedList<>();
            List<String> langList = tedTranscriptDao.findLanguagesByResourceId(videoResourceId);
            if (langList.isEmpty()) {
                languageList.add(new SelectItem("NA", "No Transcripts Available"));
            } else {
                List<Locale> locales = new ArrayList<>();
                try {
                    for (String lang : langList) {
                        locales.add(LocaleUtils.toLocale(lang));
                    }
                } catch (IllegalArgumentException e) {
                    log.error("Error while converting language code to locale", e);
                }

                languageList.addAll(BeanHelper.getLocalesAsSelectItems(locales, getLocale()));
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
