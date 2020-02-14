package de.l3s.learnweb.resource.ted;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.primefaces.PrimeFaces;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.ResourceType;
import de.l3s.learnweb.resource.ted.TedManager.SummaryType;
import de.l3s.learnweb.user.Course;
import de.l3s.learnweb.user.User;
import rita.wordnet.RiWordnet;

@Named
@ViewScoped
public class TedTranscriptBean extends ApplicationBean implements Serializable
{
    private final static Logger log = Logger.getLogger(TedTranscriptBean.class);
    private static final long serialVersionUID = -1803725556672379697L;
    //private static ILexicalDatabase db = new NictWordNet();
    //private static HashMap<String, RelatednessCalculator> rcs = new HashMap<String, RelatednessCalculator>();

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
    private List<SelectItem> languageList;
    private String locale;

    private List<SimpleTranscriptLog> simpleTranscriptLogs;
    private List<TranscriptLog> detailedTranscriptLogs;
    private List<TranscriptSummary> transcriptSummaries;
    private boolean showDeletedResources = false;
    //private int selectedCourseId;
    private TreeSet<Integer> selectedUsers;

    public void preRenderView() throws SQLException
    {
        if(isAjaxRequest())
            return;

        if(resourceId > 0)
        {
            Resource resource = Learnweb.getInstance().getResourceManager().getResource(resourceId);
            setTedResource(resource);
        }
    }

    public TedTranscriptBean()
    {
        User user = getUser();
        if(user == null) // not logged in
            return;

        //Setting the Relatedness Calculator to calculate the semantic similarity algorithms based on POS
        //rcs.put("n", new LeacockChodorow(db));
        //rcs.put("v", new LeacockChodorow(db));
        //rcs.put("a", new Lesk(db));
        //rcs.put("r", new Lesk(db));
        locale = UtilBean.getUserBean().getLocaleCode();

        //String logPreference = getPreference("transcript_show_del_res");
        //if(logPreference != null)
        //    showDeletedResources = Boolean.parseBoolean(logPreference);

        selectedUsers = new TreeSet<>();
    }

    public Resource getTedResource()
    {
        return tedResource;
    }

    public void setTedResource(Resource tedResource)
    {
        this.tedResource = tedResource;

        try
        {
            if(tedResource.getSource().equals(ResourceService.ted))
                this.videoResourceId = Learnweb.getInstance().getTedManager().getTedVideoResourceId(tedResource.getUrl());
            else if(tedResource.getSource().equals(ResourceService.tedx))
                this.videoResourceId = Learnweb.getInstance().getTedManager().getTedXVideoResourceId(tedResource.getUrl());
        }
        catch(SQLException e)
        {
            addErrorMessage(e);
            log.error("Error while retrieving TED video id", e);
        }

        if(tedResource.getSource().equals(ResourceService.tedx))
            //this.tedResource.setEmbeddedRaw("<iframe width='100%' height='100%' src='https://www.youtube-nocookie.com/embed/" + tedResource.getIdAtService() + "' frameborder='0' scrolling='no' allowfullscreen></iframe>");
            this.tedResource.setEmbeddedRaw(tedResource.getEmbeddedRaw().replace("width=\"500\" height=\"400\"", "width='100%' height='100%'"));
        String transcript = tedResource.getTranscript();
        noteId = 0;

        if(StringUtils.isNotEmpty(transcript))
        {
            Document doc = Jsoup.parse(transcript);
            Elements elements = doc.select("span");
            for(Element element : elements)
            {
                element.attr("id", Integer.toString(++noteId));
            }

            if(tedResource.getType() != ResourceType.website)
            {
                tedResource.setTranscript(doc.getElementsByTag("body").html());
            }

        }
        else
        {
            transcriptLanguage = "en";
            setTranscript();
        }

        try
        {
            HashMap<SummaryType, String> summaries = Learnweb.getInstance().getTedManager().getTranscriptSummariesForResource(resourceId);
            for(Entry<SummaryType, String> e : summaries.entrySet())
            {
                if(e.getKey().equals(SummaryType.SHORT))
                    summaryTextS = e.getValue();
                else if(e.getKey().equals(SummaryType.LONG))
                    summaryTextM = e.getValue();
                else if(e.getKey().equals(SummaryType.DETAILED))
                    summaryTextL = e.getValue();
            }
        }
        catch(SQLException e)
        {
            addErrorMessage(e);
            log.error("Error while retrieving summaries for particular TED resource: " + resourceId, e);
        }
    }

    public void setTranscript()
    {
        try
        {
            String transcript = Learnweb.getInstance().getTedManager().getTranscript(videoResourceId, transcriptLanguage);
            transcript = transcript.replaceAll("\n", "<br/><br/>");
            Document doc = Jsoup.parse(transcript);
            tedResource.setTranscript(doc.getElementsByTag("body").html());
        }
        catch(SQLException e)
        {
            addErrorMessage(e);
            log.error("Error while fetching transcript for ted video id: " + videoResourceId + "; language: " + transcriptLanguage, e);
        }
    }

    /**
     * Saves the changes in the TED transcript such as selections and user annotations;
     * Also logs the 'save' event
     */
    public void processActionSaveResource()
    {
        String transcript = getParameter("transcript");
        tedResource.setTranscript(transcript);
        try
        {
            Date actionTimestamp = new Date();
            tedResource.save();
            TranscriptLog transcriptLog = new TranscriptLog(getUser().getId(), tedResource.getId(), "", "", "save transcript", actionTimestamp);
            getLearnweb().getTedManager().saveTranscriptLog(transcriptLog);
        }
        catch(SQLException e)
        {
            addErrorMessage(e);
            log.error("Error while saving transcript changes for ted resource: " + tedResource.getId(), e);
        }
        getUser().clearCaches();
        addGrowl(FacesMessage.SEVERITY_INFO, "Changes_saved");
    }

    /**
     * Submits the transcript of a TED resource for final evaluation;
     * Saves the 'submit' event and also the final selections in lw_transcript_selections
     */
    public void processActionSubmitResource()
    {
        String transcript = getParameter("transcript");
        tedResource.setTranscript(transcript);
        tedResource.setReadOnlyTranscript(true);
        try
        {
            Date actionTimestamp = new Date();
            tedResource.save();
            TranscriptLog transcriptLog = new TranscriptLog(getUser().getId(), tedResource.getId(), "", "", "submit transcript", actionTimestamp);
            getLearnweb().getTedManager().saveTranscriptLog(transcriptLog);
            getLearnweb().getTedManager().saveTranscriptSelection(transcript, tedResource.getId());
        }
        catch(SQLException e)
        {
            addErrorMessage(e);
            log.error("Error while submitting TED resource: " + tedResource.getId(), e);
        }
        getUser().clearCaches();
        addGrowl(FacesMessage.SEVERITY_INFO, "Transcript Submitted");
    }

    /**
     * Stores a transcript action such as selection, de-selection, user annotation
     */
    public void processActionSaveLog()
    {
        try
        {
            Date actionTimestamp = new Date();

            String word = getParameter("word");
            String userAnnotation = getParameter("user_annotation");
            String action = getParameter("action");

            TranscriptLog transcriptLog = new TranscriptLog(getUser().getId(), tedResource.getId(), word, userAnnotation, action, actionTimestamp);
            getLearnweb().getTedManager().saveTranscriptLog(transcriptLog);
        }
        catch(SQLException e)
        {
            addErrorMessage(e);
            log.error("Error while storing transcription action", e);
        }
    }

    /*public double wordSimilarity(String word1, String word2, String pos)
    {
    WS4JConfiguration.getInstance().setMFS(true);

    double s = rcs.get(pos).calcRelatednessOfWords(word1, word2);

    return s;
    }
    */

    /**
     * Retrieves the set of synonyms from WordNet for given selection of word
     */
    public void processActionSetSynonyms()
    {
        String word = getParameter("word");
        String synonymsList = "";
        int wordCount = word.trim().split("\\s+").length;
        word = word.replaceAll("\\p{P}", "");

        if(wordCount <= 5)
        {
            RiWordnet wordnet = new RiWordnet(null);

            //For storing the similarity measures between input and retrieved synset words
            //HashMap<String, Double> similarityMeasures = new HashMap<String, Double>();

            String[] pos = wordnet.getPos(word);
            String[] synonyms = new String[pos.length];
            for(int i = 0; i < pos.length; i++)
            {
                synonyms[i] = word + "(" + pos[i] + ")- " + wordnet.getDescription(word, pos[i]) + ": ";

                String[] possynonyms = wordnet.getAllSynsets(word, pos[i]);
                //similarityMeasures.clear();
                if(possynonyms != null)
                {
                    for(int j = 0; j < possynonyms.length; j++)
                    {
                        //double measure = wordSimilarity(word, possynonyms[j], pos[i]);

                        //similarityMeasures.put(possynonyms[j], measure);
                        if(j == possynonyms.length - 1 && j < 9)
                            synonyms[i] += possynonyms[j] + ". ";
                        else if(j < 9)
                            synonyms[i] += possynonyms[j] + ", ";
                        else if(j == 9)
                        {
                            synonyms[i] += possynonyms[j] + ". ";
                            break;
                        }

                    }

                    //HashMap<String, Double> sortedSimilarityMeasures = sortByValues(similarityMeasures);
                    //int j = 0;
                    //int synonymListSize = sortedSimilarityMeasures.entrySet().size();
                    /*for(Map.Entry<String, Double> entry : sortedSimilarityMeasures.entrySet())
                    {
                    j++;
                    if(j == synonymListSize)
                        synonyms[i] += entry.getKey() + ".";
                    else if(j < 10)
                        synonyms[i] += entry.getKey() + ", ";
                    else
                    {
                        synonyms[i] += entry.getKey() + ".";
                        break;
                    }
                    }*/
                }
                log.debug(synonyms[i]);
                synonymsList += synonyms[i] + "&lt;br/&gt;";

            }

            if(pos.length == 0 && wordCount == 1)
                synonymsList += "No synonyms";
            else if(synonymsList.isEmpty())
                synonymsList += "multiple";
            PrimeFaces.current().ajax().addCallbackParam("synonyms", synonymsList);
        }
        else
            PrimeFaces.current().ajax().addCallbackParam("synonyms", "multiple");
    }

    public void submitShortSummary()
    {
        if(summaryTextS != null && summaryTextS.length() > 0)
        {
            try
            {
                getLearnweb().getTedManager().saveSummaryText(getUser().getId(), resourceId, summaryTextS, SummaryType.SHORT);
            }
            catch(SQLException e)
            {
                addErrorMessage(e);
            }
        }

    }

    public void submitLongSummary()
    {
        if(summaryTextM != null && summaryTextM.length() > 0)
        {
            try
            {
                getLearnweb().getTedManager().saveSummaryText(getUser().getId(), resourceId, summaryTextM, SummaryType.LONG);
            }
            catch(SQLException e)
            {
                addErrorMessage(e);
            }
        }
    }

    public void submitDetailedSummary()
    {
        if(summaryTextL != null && summaryTextL.length() > 0)
        {
            try
            {
                getLearnweb().getTedManager().saveSummaryText(getUser().getId(), resourceId, summaryTextL, SummaryType.DETAILED);
            }
            catch(SQLException e)
            {
                addErrorMessage(e);
            }
        }
    }

    public String getTranscriptLanguage()
    {
        return transcriptLanguage;
    }

    public void setTranscriptLanguage(String transcriptLanguage)
    {
        this.transcriptLanguage = transcriptLanguage;
    }

    public List<SelectItem> getLanguageList()
    {
        try
        {
            if(!locale.equals(UtilBean.getUserBean().getLocaleCode()))
            {
                languageList = null;
                locale = UtilBean.getUserBean().getLocaleCode();
            }

            if(languageList == null)
            {
                Map<String, String> langList;
                languageList = new LinkedList<>();
                langList = Learnweb.getInstance().getTedManager().getLangList(videoResourceId);

                if(!langList.isEmpty())
                {
                    for(Map.Entry<String, String> entry : langList.entrySet())
                    {
                        languageList.add(new SelectItem(entry.getValue(), entry.getKey()));
                    }
                    languageList.sort(languageComparator());
                }
                else
                {
                    languageList.add(new SelectItem("NA", "No Transcripts Available"));
                }

            }

        }
        catch(SQLException | RuntimeException e)
        {
            addErrorMessage(e);
        }
        return languageList;
    }

    public int getNoteId()
    {
        return noteId;
    }

    /**
     * Returns detailed transcript logs for selected users
     *
     * @throws SQLException
     */
    public List<TranscriptLog> getTranscriptLogs() throws SQLException
    {
        if(detailedTranscriptLogs == null)
            detailedTranscriptLogs = getLearnweb().getTedManager().getTranscriptLogs(selectedUsers, showDeletedResources);
        return detailedTranscriptLogs;
    }

    public TreeSet<Integer> getSelectedUsers() throws SQLException
    {
        HttpServletRequest request = (HttpServletRequest) (FacesContext.getCurrentInstance().getExternalContext().getRequest());
        String[] tempSelectedUsers = request.getParameterValues("selected_users");

        if(null == tempSelectedUsers || tempSelectedUsers.length == 0)
        {
            addMessage(FacesMessage.SEVERITY_WARN, "select_user");
            return null;
        }

        TreeSet<Integer> selectedUsersSet = new TreeSet<>();
        for(String userId : tempSelectedUsers)
            selectedUsersSet.add(Integer.parseInt(userId));

        return selectedUsersSet;
    }

    public void onSubmitSelectedUsers()
    {
        try
        {
            this.selectedUsers = getSelectedUsers();
            resetTranscriptLogs();
            resetTranscriptSummaries();
        }
        catch(SQLException e)
        {
            addErrorMessage(e);
        }
    }

    /**
     * Returns transcript logs of selected users aggregating selection, deselection and user annotation counts
     *
     * @throws SQLException
     */
    public List<SimpleTranscriptLog> getSimpleTranscriptLogs() throws SQLException
    {
        if(simpleTranscriptLogs == null)
            simpleTranscriptLogs = getLearnweb().getTedManager().getSimpleTranscriptLogs(selectedUsers, showDeletedResources);
        return simpleTranscriptLogs;
    }

    /**
     * Returns transcript summaries of selected users
     *
     * @throws SQLException
     */
    public List<TranscriptSummary> getTranscriptSummaries() throws SQLException
    {
        if(transcriptSummaries == null)
            transcriptSummaries = getLearnweb().getTedManager().getTranscriptSummaries(selectedUsers);
        return transcriptSummaries;
    }

    public int getResourceId()
    {
        return resourceId;
    }

    public void setResourceId(int resourceId)
    {
        this.resourceId = resourceId;
    }

    public List<Course> getCourses() throws SQLException
    {
        return getUser().getCourses();
    }

    public static Comparator<SelectItem> languageComparator()
    {
        return Comparator.comparing(SelectItem::getLabel);
    }

    /*public int getSelectedCourseId()
    {
        return selectedCourseId;
    }

    public void setSelectedCourseId(int selectedCourseId)
    {
        this.selectedCourseId = selectedCourseId;
    }*/

    public boolean isShowDeletedResources()
    {
        return showDeletedResources;
    }

    public void setShowDeletedResources(boolean showDeletedResources)
    {
        this.showDeletedResources = showDeletedResources;
        setPreference("transcript_show_del_res", Boolean.toString(showDeletedResources));
    }

    public void resetTranscriptLogs()
    {
        simpleTranscriptLogs = null;
        detailedTranscriptLogs = null;
    }

    public void resetTranscriptSummaries()
    {
        transcriptSummaries = null;
    }

    public String getSummaryTextS()
    {
        return summaryTextS;
    }

    public void setSummaryTextS(String summaryTextS)
    {
        this.summaryTextS = summaryTextS;
    }

    public String getSummaryTextM()
    {
        return summaryTextM;
    }

    public void setSummaryTextM(String summaryTextM)
    {
        this.summaryTextM = summaryTextM;
    }

    public String getSummaryTextL()
    {
        return summaryTextL;
    }

    public void setSummaryTextL(String summaryTextL)
    {
        this.summaryTextL = summaryTextL;
    }

}
