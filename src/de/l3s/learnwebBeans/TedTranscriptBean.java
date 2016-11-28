package de.l3s.learnwebBeans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.primefaces.context.RequestContext;

import de.l3s.learnweb.Course;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.SimpleTranscriptLog;
import de.l3s.learnweb.TedManager.SummaryType;
import de.l3s.learnweb.TranscriptLog;
import de.l3s.learnweb.beans.UtilBean;
import rita.wordnet.RiWordnet;

@ManagedBean
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
    private String summaryText;

    /* To get the resource id from ted_video table since id_at_service may or may not be null in lw_resource for TED and
     * To get the resource id from lw_resource table corresponding to the video added by the admin for TEDx
     */
    private int videoResourceId;

    private int resourceId;
    private List<SelectItem> languageList;
    private String locale;

    private List<SimpleTranscriptLog> simpleTranscriptLogs;
    private List<TranscriptLog> detailedTranscriptLogs;
    private boolean showDeletedResources = false;
    private int selectedCourseId;

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
	//Setting the Relatedness Calculator to calculate the semantic similarity algorithms based on POS
	//rcs.put("n", new LeacockChodorow(db));
	//rcs.put("v", new LeacockChodorow(db));
	//rcs.put("a", new Lesk(db));
	//rcs.put("r", new Lesk(db));
	locale = UtilBean.getUserBean().getLocaleAsString();

	String logPreference = getPreference("transcript_show_del_res");
	if(logPreference != null)
	    showDeletedResources = Boolean.parseBoolean(logPreference);
	selectedCourseId = UtilBean.getUserBean().getActiveCourseId();

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
	    if(tedResource.getSource().equalsIgnoreCase("ted"))
		this.videoResourceId = Learnweb.getInstance().getTedManager().getTedVideoResourceId(tedResource.getUrl());
	    else if(tedResource.getSource().equalsIgnoreCase("tedx"))
		this.videoResourceId = Learnweb.getInstance().getTedManager().getTedXVideoResourceId(tedResource.getUrl());
	}
	catch(SQLException e)
	{
	    addFatalMessage(e);
	    log.fatal(e);
	}

	if(tedResource.getSource().equalsIgnoreCase("TEDx"))
	    //this.tedResource.setEmbeddedRaw("<iframe width='100%' height='100%' src='https://www.youtube.com/embed/" + tedResource.getIdAtService() + "' frameborder='0' scrolling='no' webkitAllowFullScreen mozallowfullscreen allowfullscreen></iframe>");
	    this.tedResource.setEmbeddedRaw(tedResource.getEmbeddedRaw().replace("width=\"500\" height=\"400\"", "width='100%' height='100%'"));
	String transcript = tedResource.getTranscript();
	noteId = 0;
	if(transcript != null && transcript != "")
	{
	    Document doc = Jsoup.parse(transcript);
	    Elements elements = doc.select("span");
	    for(Element element : elements)
	    {
		element.attr("id", Integer.toString(++noteId));
	    }

	    tedResource.setTranscript(doc.getElementsByTag("body").html());
	}
	else
	{
	    transcriptLanguage = "en";
	    setTranscript();
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
	catch(RuntimeException e)
	{
	    addFatalMessage(e);
	    log.fatal(e);
	}
	catch(SQLException e)
	{
	    addFatalMessage(e);
	    log.fatal(e);
	}
    }

    public void processActionSaveResource()
    {
	String transcript = getParameter("transcript");
	tedResource.setTranscript(transcript);
	try
	{
	    Date actionTimestamp = new Date();
	    tedResource.save();
	    TranscriptLog transcriptLog = new TranscriptLog(UtilBean.getUserBean().getActiveCourseId(), getUser().getId(), tedResource.getId(), "", "", "save transcript", actionTimestamp);
	    getLearnweb().getTedManager().saveTranscriptLog(transcriptLog);
	}
	catch(Exception e)
	{
	    addFatalMessage(e);
	    log.fatal(e);
	}
	getUser().clearCaches();
	addGrowl(FacesMessage.SEVERITY_INFO, "Changes_saved");
    }

    public void processActionSubmitResource()
    {
	String transcript = getParameter("transcript");
	tedResource.setTranscript(transcript);
	tedResource.setReadOnly(true);
	try
	{
	    Date actionTimestamp = new Date();
	    tedResource.save();
	    TranscriptLog transcriptLog = new TranscriptLog(UtilBean.getUserBean().getActiveCourseId(), getUser().getId(), tedResource.getId(), "", "", "submit transcript", actionTimestamp);
	    getLearnweb().getTedManager().saveTranscriptLog(transcriptLog);
	    getLearnweb().getTedManager().saveTranscriptSelection(transcript, tedResource.getId());
	}
	catch(Exception e)
	{
	    addFatalMessage(e);
	    log.fatal(e);
	}
	getUser().clearCaches();
	addGrowl(FacesMessage.SEVERITY_INFO, "Transcript Submitted");
    }

    public void processActionSaveLog()
    {
	try
	{
	    Date actionTimestamp = new Date();

	    String word = getParameter("word");
	    String userAnnotation = getParameter("user_annotation");
	    String action = getParameter("action");

	    TranscriptLog transcriptLog = new TranscriptLog(UtilBean.getUserBean().getActiveCourseId(), getUser().getId(), tedResource.getId(), word, userAnnotation, action, actionTimestamp);
	    getLearnweb().getTedManager().saveTranscriptLog(transcriptLog);
	}
	catch(Exception e)
	{
	    addFatalMessage(e);
	    log.fatal(e);
	}
    }

    /*public double wordSimilarity(String word1, String word2, String pos)
    {
    WS4JConfiguration.getInstance().setMFS(true);
    
    double s = rcs.get(pos).calcRelatednessOfWords(word1, word2);
    
    return s;
    }
    
    public HashMap<String, Double> sortByValues(HashMap<String, Double> similarityMeasures)
    {
    List<Map.Entry<String, Double>> list = new LinkedList<Map.Entry<String, Double>>(similarityMeasures.entrySet());
    
    Collections.sort(list, new Comparator<Map.Entry<String, Double>>()
    {
        @Override
        public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2)
        {
    	return (o2.getValue()).compareTo(o1.getValue());
        }
    });
    
    HashMap<String, Double> sortedHashMap = new LinkedHashMap<String, Double>();
    for(Iterator<Map.Entry<String, Double>> it = list.iterator(); it.hasNext();)
    {
        Map.Entry<String, Double> entry = it.next();
        sortedHashMap.put(entry.getKey(), entry.getValue());
    }
    return sortedHashMap;
    }*/

    public void processActionSetSynonyms()
    {
	String word = getParameter("word");
	String synonymsList = "";
	RequestContext context = RequestContext.getCurrentInstance();
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
	    context.addCallbackParam("synonyms", synonymsList);
	}
	else
	    context.addCallbackParam("synonyms", "multiple");
    }

    public void submitShortSummary()
    {
	if(summaryText != null && summaryText.length() > 0)
	{
	    try
	    {
		getLearnweb().getTedManager().saveSummaryText(getUser().getId(), resourceId, summaryText, SummaryType.SHORT);
	    }
	    catch(SQLException e)
	    {
		addFatalMessage(e);
		log.fatal(e);
	    }
	}

    }

    public void submitLongSummary()
    {
	if(summaryText != null && summaryText.length() > 0)
	{
	    try
	    {
		getLearnweb().getTedManager().saveSummaryText(getUser().getId(), resourceId, summaryText, SummaryType.LONG);
	    }
	    catch(SQLException e)
	    {
		addFatalMessage(e);
		log.fatal(e);
	    }
	}
    }

    public void submitDetailedSummary()
    {
	if(summaryText != null && summaryText.length() > 0)
	{
	    try
	    {
		getLearnweb().getTedManager().saveSummaryText(getUser().getId(), resourceId, summaryText, SummaryType.DETAILED);
	    }
	    catch(SQLException e)
	    {
		addFatalMessage(e);
		log.fatal(e);
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
	    if(!locale.equals(UtilBean.getUserBean().getLocaleAsString()))
	    {
		languageList = null;
		locale = UtilBean.getUserBean().getLocaleAsString();
	    }

	    if(languageList == null)
	    {
		Map<String, String> langList;
		languageList = new LinkedList<SelectItem>();
		langList = Learnweb.getInstance().getTedManager().getLangList(videoResourceId);

		if(!langList.isEmpty())
		{
		    for(Map.Entry<String, String> entry : langList.entrySet())
		    {
			languageList.add(new SelectItem(entry.getValue(), entry.getKey()));
		    }
		    Collections.sort(languageList, languageComparator());
		}
		else
		    languageList.add(new SelectItem("NA", "No Transcripts Available"));

	    }

	}
	catch(RuntimeException e)
	{
	    addFatalMessage(e);
	    log.fatal(e);
	}
	catch(SQLException e)
	{
	    addFatalMessage(e);
	    log.fatal(e);
	}
	return languageList;
    }

    public int getNoteId()
    {
	return noteId;
    }

    public List<TranscriptLog> getTranscriptLogs() throws SQLException
    {
	if(detailedTranscriptLogs == null)
	    detailedTranscriptLogs = getLearnweb().getTedManager().getTranscriptLogs(selectedCourseId, showDeletedResources);
	return detailedTranscriptLogs;
    }

    public List<SimpleTranscriptLog> getSimpleTranscriptLogs() throws SQLException
    {
	if(simpleTranscriptLogs == null)
	    simpleTranscriptLogs = getLearnweb().getTedManager().getSimpleTranscriptLogs(selectedCourseId, showDeletedResources);
	return simpleTranscriptLogs;
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
	return new Comparator<SelectItem>()
	{
	    @Override
	    public int compare(SelectItem o1, SelectItem o2)
	    {
		return o1.getLabel().compareTo(o2.getLabel());
	    }
	};
    }

    public int getSelectedCourseId()
    {
	return selectedCourseId;
    }

    public void setSelectedCourseId(int selectedCourseId)
    {
	this.selectedCourseId = selectedCourseId;
    }

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

    public String getSummaryText()
    {
	return summaryText;
    }

    public void setSummaryText(String summaryText)
    {
	this.summaryText = summaryText;
    }

}
