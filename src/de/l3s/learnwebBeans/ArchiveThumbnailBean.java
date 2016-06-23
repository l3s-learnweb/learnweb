package de.l3s.learnwebBeans;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;
import org.primefaces.context.RequestContext;
import org.primefaces.extensions.event.timeline.TimelineSelectEvent;
import org.primefaces.extensions.model.timeline.TimelineEvent;
import org.primefaces.extensions.model.timeline.TimelineModel;

import de.l3s.archiveSearch.ArchiveItShingle;
import de.l3s.learnweb.ArchiveUrl;
import de.l3s.learnweb.ArchiveUrlManager;
import de.l3s.learnweb.Learnweb;

@ManagedBean
@ViewScoped
public class ArchiveThumbnailBean extends ApplicationBean
{
    public final static Logger log = Logger.getLogger(ArchiveThumbnailBean.class);

    private float frame;
    private float text;

    HashMap<String, Set<String>> hashmapframe;
    HashMap<String, Set<String>> hashmaptext;

    private int resource_id;
    private List<ArchiveUrl> listOfArchives;
    private List<ArchiveUrl> listOfUrls;
    private List<String> dates;

    private ArchiveItShingle archiveItShingle;
    ArchiveUrlManager archiveUrlManager;

    private TimelineModel model;
    private Date min;
    private Date max;
    private long zoomMin;
    private long zoomMax;

    public ArchiveThumbnailBean() throws SQLException
    {
	frame = 1;
	text = 1;
	hashmapframe = new LinkedHashMap<String, Set<String>>();
	hashmaptext = new LinkedHashMap<String, Set<String>>();
	archiveItShingle = new ArchiveItShingle();
	listOfArchives = new LinkedList<ArchiveUrl>();
	archiveUrlManager = Learnweb.getInstance().getArchiveUrlManager();
	listOfUrls = new ArrayList<ArchiveUrl>();
    }

    public List<ArchiveUrl> getListOfArchives()
    {
	return listOfArchives;
    }

    public void setListOfArchives(List<ArchiveUrl> listOfArchives)
    {
	this.listOfArchives = listOfArchives;
    }

    public float getFrame()
    {
	return frame;
    }

    public void setFrame(float frame)
    {
	this.frame = frame;
    }

    public float getText()
    {
	return text;
    }

    public void setText(float text)
    {
	this.text = text;
    }

    public List<ArchiveUrl> getlistOfUrls() throws SQLException
    {
	listOfUrls.clear();
	model = new TimelineModel();
	Set<String> setOfNearUniqueArchives = new LinkedHashSet<String>();
	setOfNearUniqueArchives = archiveItShingle.computeUniqueArchivesBySequence(hashmaptext, hashmapframe, listOfArchives, resource_id, frame, text);
	if(setOfNearUniqueArchives.isEmpty())
	{

	}
	for(String str : setOfNearUniqueArchives)
	{
	    String url = archiveUrlManager.getFileUrl(resource_id, str);
	    Date timestamp = archiveUrlManager.getTimestamp(resource_id, str);
	    model = getModel(timestamp);
	    if(url == null || timestamp == null)
		log.debug("Thumbnail does not exist");
	    else
		listOfUrls.add(new ArchiveUrl(str, url, timestamp));
	}
	while(listOfUrls.remove(null))
	    ;
	return listOfUrls;
    }

    public List<String> getDates()
    {
	return dates;
    }

    public TimelineModel getModel()
    {
	return model;
    }

    public TimelineModel getModel(Date timestamp)
    {
	Calendar cal = Calendar.getInstance();
	cal.setTime(timestamp);
	model.add(new TimelineEvent(timestamp.toString(), cal.getTime()));
	return model;
    }

    public Date getMin()
    {
	return min;
    }

    public Date getMax()
    {
	return max;
    }

    public long getZoomMin()
    {
	return zoomMin;
    }

    public long getZoomMax()
    {
	return zoomMax;
    }

    public void save()
    {
	RequestContext context = RequestContext.getCurrentInstance();
	context.update("contentFlow");
	context.scrollTo("contentFlow");
    }

    public void onSelect(TimelineSelectEvent e)
    {
	TimelineEvent timelineEvent = e.getTimelineEvent();

	addMessage(FacesMessage.SEVERITY_INFO, "Selected event:", timelineEvent.getData().toString());
    }

    @PostConstruct
    public void init()
    {
	model = new TimelineModel();
	Calendar cal = Calendar.getInstance();
	max = cal.getTime(); // upper limit of visible range  
	zoomMin = 1000L * 60 * 60 * 24; // one day in milliseconds for zoomMin  
	zoomMax = 1000L * 60 * 60 * 24 * 31 * 12 * 12; // about 2 years for zoomMax 

	try
	{
	    String htmlText = null;
	    String htmlTags = null;
	    final StringBuilder htmlString = new StringBuilder();
	    List<String> wordList = new ArrayList<String>();

	    Set<String> setOfShingles = new HashSet<String>();

	    resource_id = 169891; // TODO does this make sense?
	    PreparedStatement ps = getLearnweb().getConnection().prepareStatement("SELECT * FROM `lw_resource_archiveurl` NATURAL JOIN `lw_resource_archive_shingles` WHERE `resource_id`=? group by `shingle_id`");
	    ps.setInt(1, resource_id);
	    ResultSet rs = ps.executeQuery();
	    if(rs.next())
	    {
		cal.setTime(rs.getDate("timestamp"));
		cal.set(cal.get(Calendar.YEAR), 1, 1, 0, 0, 0);
		min = cal.getTime(); // lower limit of visible range 
		rs.previous();
	    }
	    while(rs.next())
	    {
		Date timestamp = new Date(rs.getTimestamp("timestamp").getTime());
		cal.setTime(timestamp);
		model.add(new TimelineEvent(timestamp.toString(), cal.getTime()));
		htmlString.setLength(0);
		wordList.clear();
		String url = rs.getString("archive_url");
		listOfArchives.add(new ArchiveUrl(url, timestamp));
		htmlTags = rs.getString("htmltags");
		htmlString.append(htmlTags);
		String[] words = htmlString.toString().replaceAll("[!?,.]", "").split(" ");
		wordList.addAll(Arrays.asList(words));
		setOfShingles = archiveItShingle.computeShingles(wordList);
		setOfShingles.clear();
		hashmapframe.put(url, new HashSet<>(setOfShingles));
		wordList.clear();
		htmlText = rs.getString("htmltext");
		words = htmlText.replaceAll("[!?,.]", "").split(" ");
		wordList.addAll(Arrays.asList(words));
		setOfShingles = archiveItShingle.computeShingles(wordList);
		hashmaptext.put(url, new HashSet<>(setOfShingles));
		url = archiveUrlManager.getFileUrl(resource_id, url);
		if(url == null)
		    log.debug("Thumbnail does not exist");
		else
		    listOfUrls.add(new ArchiveUrl(rs.getString("archive_url"), url, timestamp));
	    }
	}
	catch(SQLException ex)
	{
	    ex.printStackTrace();
	}
    }
}
