package de.l3s.learnweb.beans;

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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;
import org.primefaces.model.timeline.TimelineEvent;
import org.primefaces.model.timeline.TimelineModel;

import de.l3s.archiveSearch.ArchiveItShingle;
import de.l3s.learnweb.ArchiveUrl;
import de.l3s.learnweb.ArchiveUrlManager;
import de.l3s.learnweb.Learnweb;

@ManagedBean
@ViewScoped
public class ArchiveThumbnailBean extends ApplicationBean
{
    public final static Logger log = Logger.getLogger(ArchiveThumbnailBean.class);

    private float frameSim;
    private float textSim;

    HashMap<String, Set<String>> hashmapFrame;
    HashMap<String, Set<String>> hashmapText;

    private int resourceId;
    private List<ArchiveUrl> archiveUrls;
    private Set<String> selectedArchiveUrls;
    private HashMap<String, Date> archiveUrlsHashMap;

    private ArchiveItShingle archiveItShingle;
    ArchiveUrlManager archiveUrlManager;

    //Timeline properties
    private TimelineModel model;
    private Date min;
    private Date max;
    private long zoomMin;
    private long zoomMax;

    //   private DateFormat df; was not used

    public ArchiveThumbnailBean() throws SQLException
    {
        frameSim = 1f;
        textSim = 1f;
        hashmapFrame = new LinkedHashMap<String, Set<String>>();
        hashmapText = new LinkedHashMap<String, Set<String>>();
        archiveItShingle = new ArchiveItShingle();
        archiveUrls = new LinkedList<ArchiveUrl>();
        selectedArchiveUrls = new HashSet<String>();
        archiveUrlsHashMap = new HashMap<String, Date>();
        archiveUrlManager = Learnweb.getInstance().getArchiveUrlManager();

        //	df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, UtilBean.getUserBean().getLocale());
    }

    public List<ArchiveUrl> getArchiveUrls()
    {
        return archiveUrls;
    }

    public void setArchiveUrls(List<ArchiveUrl> archiveUrls)
    {
        this.archiveUrls = archiveUrls;
    }

    public float getFrameSim()
    {
        return frameSim;
    }

    public void setFrameSim(float frameSim)
    {
        this.frameSim = frameSim;
    }

    public float getTextSim()
    {
        return textSim;
    }

    public void setTextSim(float textSim)
    {
        this.textSim = textSim;
    }

    public List<ArchiveUrl> getlistOfUrls() throws SQLException
    {
        List<ArchiveUrl> listOfUrls = new ArrayList<ArchiveUrl>();
        model = new TimelineModel();
        //Set<String> setOfNearUniqueArchives = archiveItShingle.computeUniqueArchivesBySequence(hashmapText, hashmapFrame, archiveUrls, resourceId, frameSim, textSim);

        for(String archiveUrl : selectedArchiveUrls)
        {
            String fileUrl = archiveUrlManager.getFileUrl(resourceId, archiveUrl);
            Date timestamp = archiveUrlsHashMap.get(archiveUrl);
            //model = getModel(timestamp);
            if(fileUrl == null)
                log.debug("Thumbnail does not exist");
            else
                listOfUrls.add(new ArchiveUrl(archiveUrl, fileUrl, timestamp));
        }
        loadTimelineModel();
        return listOfUrls;
    }

    public TimelineModel getModel()
    {
        return model;
    }

    public TimelineModel getModel(Date timestamp)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(timestamp);
        model.add(new TimelineEvent("", cal.getTime(), false, "", "selected"));
        return model;
    }

    public void loadTimelineModel()
    {
        for(ArchiveUrl version : archiveUrls)
        {
            Calendar cal = Calendar.getInstance();
            cal.setTime(version.getTimestamp());
            if(selectedArchiveUrls.contains(version.getArchiveUrl()))
                model.add(new TimelineEvent("", cal.getTime(), false, "", "selected"));
            else
                model.add(new TimelineEvent("", cal.getTime(), false, ""));
        }
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

    /*public void save()
    {
    RequestContext context = RequestContext.getCurrentInstance();
    context.update("contentFlow");
    context.scrollTo("contentFlow");
    }
    
    public void onSelect(TimelineSelectEvent e)
    {
    TimelineEvent timelineEvent = e.getTimelineEvent();
    addMessage(FacesMessage.SEVERITY_INFO, "Selected event:", timelineEvent.getData().toString());
    }*/

    public void computeSelectedArchivesHtmlBased() throws SQLException
    {
        selectedArchiveUrls = archiveItShingle.computeUniqueArchivesBySequence(hashmapText, hashmapFrame, archiveUrls, resourceId, frameSim, textSim);
        //log.debug("frame sim:" + frameSim + "; text sim:" + textSim + "; Timemap size: " + selectedArchiveUrls.size());
    }

    public void computeSelectedArchivesSimhashBased()
    {
        selectedArchiveUrls = archiveItShingle.computeUniqueArchivesByThresholdGrouping((LinkedList<ArchiveUrl>) archiveUrls);
        //log.debug("Archive Urls filtered based on simhashes: " + selectedArchiveUrls.size());
    }

    public void init()
    {
        if(getFacesContext().isPostback())
            return;

        //model = new TimelineModel();
        Calendar cal = Calendar.getInstance();
        max = cal.getTime(); // upper limit of visible range  
        zoomMin = 1000L * 60 * 60 * 24; // one day in milliseconds for zoomMin  
        zoomMax = 1000L * 60 * 60 * 24 * 31 * 12 * 12; // about 2 years for zoomMax 

        try
        {
            String htmlText = null;
            String htmlTags = null;

            PreparedStatement ps = getLearnweb().getConnection().prepareStatement("SELECT * FROM `lw_resource_archiveurl` JOIN `lw_resource_archive_shingles` USING(shingle_id) WHERE `resource_id`=? group by `shingle_id`");
            ps.setInt(1, resourceId);
            ResultSet rs = ps.executeQuery();
            if(rs.next())
            {
                cal.setTime(new Date(rs.getTimestamp("timestamp").getTime()));
                cal.add(Calendar.YEAR, -1);
                min = cal.getTime(); // lower limit of visible range 
                rs.previous();
            }

            while(rs.next())
            {
                Date timestamp = new Date(rs.getTimestamp("timestamp").getTime());
                String url = rs.getString("archive_url");
                long simhash = rs.getLong("simhash");
                archiveUrls.add(new ArchiveUrl(url, timestamp, simhash));
                archiveUrlsHashMap.put(url, timestamp);

                htmlTags = rs.getString("htmltags");
                String[] words = htmlTags.replaceAll("[!?,.]", "").split(" ");
                hashmapFrame.put(url, archiveItShingle.computeShingles(Arrays.asList(words)));

                htmlText = rs.getString("htmltext");
                words = htmlText.replaceAll("[!?,.]", "").split(" ");
                hashmapText.put(url, archiveItShingle.computeShingles(Arrays.asList(words)));
            }
            selectedArchiveUrls = archiveItShingle.computeUniqueArchivesBySequence(hashmapText, hashmapFrame, archiveUrls, resourceId, frameSim, textSim);
            //log.debug("Archive Urls Size after removing duplicates:" + archiveUrls.size());
        }
        catch(SQLException ex)
        {
            log.error(ex);
        }
    }

    public int getResourceId()
    {
        return resourceId;
    }

    public void setResourceId(int resourceId)
    {
        this.resourceId = resourceId;
    }
}
