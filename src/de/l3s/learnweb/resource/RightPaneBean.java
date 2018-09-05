package de.l3s.learnweb.resource;

import java.io.Serializable;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import de.l3s.learnweb.ArchiveUrl;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.TimelineData;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.resource.office.FileEditorBean;
import org.primefaces.PrimeFaces;

// TODO Oleh: rename to ResourcePaneBean
@Named
@ViewScoped
public class RightPaneBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 1284318005787537321L;
    private final static Logger log = Logger.getLogger(RightPaneBean.class);

    public enum RightPaneAction
    {
        none,
        newResource,
        viewResource,
        editResource,
        newFolder,
        editFolder,
        viewFolder,
        newFile,
        viewUpdatedResource // TODO Oleh: why do we need it? I think it can be replaced to some flag in Resource
    }

    private int resourceId = 0; // url param, force resource view

    private RightPaneAction paneAction = RightPaneAction.none;
    private AbstractResource clickedAbstractResource;

    @Inject
    private FileEditorBean fileEditorBean;

    @Inject
    private AddResourceBean addResourceBean;

    public void onLoad()
    {
        if(isAjaxRequest())
            return;

        if(resourceId > 0)
        {
            try
            {
                Resource resource = Learnweb.getInstance().getResourceManager().getResource(resourceId);
                if(resource == null)
                {
                    addInvalidParameterMessage("resource_id");
                    return;
                }

                setViewResource(resource);
            }
            catch(Exception e)
            {
                addFatalMessage(e);
            }
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

    public void editClickedResource() throws SQLException
    {
        if (clickedAbstractResource == null || !clickedAbstractResource.canEditResource(getUser())) {
            addGrowl(FacesMessage.SEVERITY_ERROR, "The resource is not selected or you don't have permission to edit it");
            return;
        }

        try
        {
            clickedAbstractResource.unlockResource(getUser());
            clickedAbstractResource.save();

            if (clickedAbstractResource instanceof Folder)
            {
                log(Action.edit_folder, clickedAbstractResource.getGroupId(), clickedAbstractResource.getId(), clickedAbstractResource.getTitle());
                addMessage(FacesMessage.SEVERITY_INFO, "folderUpdated", clickedAbstractResource.getTitle());
            }
            else
            {
                log(Action.edit_resource, clickedAbstractResource.getGroupId(), clickedAbstractResource.getId(), clickedAbstractResource.getTitle());
                addMessage(FacesMessage.SEVERITY_INFO, "resourceUpdated", clickedAbstractResource.getTitle());
            }

            setViewResource(clickedAbstractResource);
        }
        catch(SQLException e)
        {
            addFatalMessage(e);
        }
    }

    public void cancelEditClickedResource()
    {
        if (clickedAbstractResource != null)
        {
            clickedAbstractResource.unlockResource(getUser());
            setViewResource(clickedAbstractResource);
        }
    }

    public String getPanelTitle()
    {
        switch(this.paneAction)
        {
        case newResource:
            return UtilBean.getLocaleMessage("upload_resource");
        case viewResource:
        case viewUpdatedResource:
            return UtilBean.getLocaleMessage("resource") + " - " + clickedAbstractResource.getTitle();
        case editResource:
            return UtilBean.getLocaleMessage("edit_resource") + " - " + clickedAbstractResource.getTitle();
        case newFolder:
            return UtilBean.getLocaleMessage("create_folder");
        case editFolder:
            return UtilBean.getLocaleMessage("edit_folder");
        case viewFolder:
            return UtilBean.getLocaleMessage("folder") + " - " + clickedAbstractResource.getTitle();
        case newFile:
            return UtilBean.getLocaleMessage("create") + " - " + addResourceBean.getResource().getType().toString();
        default:
            return UtilBean.getLocaleMessage("click_to_view_details");
        }
    }

    public void resetPane()
    {
        clickedAbstractResource = null;
        paneAction = RightPaneAction.none;
    }

    public void setViewResource(AbstractResource resource)
    {
        setClickedAbstractResource(resource);

        if(resource == null)
        {
            resetPane();
        }
        else if(resource instanceof Folder)
        {
            paneAction = RightPaneAction.viewFolder;
            log(Action.opening_folder, resource.getGroupId(), resource.getId());
        }
        else
        {
            paneAction = RightPaneAction.viewResource;
            log(Action.opening_resource, resource.getGroupId(), resource.getId());
        }
    }

    public void setEditResource(AbstractResource resource)
    {
        if (!resource.lockResource(getUser())) {
            addGrowl(FacesMessage.SEVERITY_ERROR, "Editing of the resource is not possible, because another user is already editing it.");
            return;
        }

        setClickedAbstractResource(resource);

        if(resource instanceof Folder)
        {
            paneAction = RightPaneAction.editFolder;
            log(Action.opening_folder, resource.getGroupId(), resource.getId());
        }
        else
        {
            paneAction = RightPaneAction.editResource;
            log(Action.opening_resource, resource.getGroupId(), resource.getId());
        }
    }

    public void editActivityListener()
    {
        if (clickedAbstractResource != null)
        {
            if (!clickedAbstractResource.lockerUpdate(getUser())) {
                addGrowl(FacesMessage.SEVERITY_ERROR, "Your editing is interrupted by another user!");
                setViewResource(clickedAbstractResource);
                PrimeFaces.current().ajax().update(":right_pane_wrapper");
            }
        }
    }

    public RightPaneAction getPaneAction()
    {
        return paneAction;
    }

    public void setPaneAction(RightPaneAction paneAction)
    {
        this.paneAction = paneAction;
    }

    public boolean isTheResourceClicked(AbstractResource resource)
    {
        return clickedAbstractResource != null && clickedAbstractResource.equals(resource);
    }

    public Folder getClickedFolder()
    {
        if(clickedAbstractResource instanceof Folder)
            return (Folder) clickedAbstractResource;

        return null;
    }

    public Resource getClickedResource()
    {
        if(clickedAbstractResource instanceof Resource)
            return (Resource) clickedAbstractResource;

        return null;
    }

    public AbstractResource getClickedAbstractResource()
    {
        return clickedAbstractResource;
    }

    public void setClickedAbstractResource(AbstractResource resource)
    {
        clickedAbstractResource = resource;

        if(getClickedResource() != null && getClickedResource().isOfficeResource())
            fileEditorBean.fillInFileInfo(getClickedResource());
    }

    /* Archive view utils  */

    /**
     * The method is used from JS in archive_timeline_template.xhtml
     */
    @SuppressWarnings({ "unchecked" })
    public String getArchiveTimelineJsonData()
    {
        Resource resource = getClickedResource();
        JSONArray highChartsData = new JSONArray();
        try
        {
            List<TimelineData> timelineMonthlyData = getLearnweb().getTimelineManager().getTimelineDataGroupedByMonth(resource.getId(), resource.getUrl());

            for(TimelineData timelineData : timelineMonthlyData)
            {
                JSONArray innerArray = new JSONArray();
                innerArray.add(timelineData.getTimestamp().getTime());
                innerArray.add(timelineData.getNumberOfVersions());
                highChartsData.add(innerArray);
            }
        }
        catch(SQLException e)
        {
            log.error("Error while fetching the archive data aggregated by month for a resource", e);
            addGrowl(FacesMessage.SEVERITY_INFO, "fatal_error");
        }
        return highChartsData.toJSONString();
    }

    /**
     * The method is used from JS in archive_timeline_template.xhtml
     */
    @SuppressWarnings({ "unchecked" })
    public String getArchiveCalendarJsonData()
    {
        Resource resource = getClickedResource();
        JSONObject archiveDates = new JSONObject();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try
        {
            List<TimelineData> timelineDailyData = getLearnweb().getTimelineManager().getTimelineDataGroupedByDay(resource.getId(), resource.getUrl());
            for(TimelineData timelineData : timelineDailyData)
            {
                JSONObject archiveDay = new JSONObject();
                archiveDay.put("number", timelineData.getNumberOfVersions());
                archiveDay.put("badgeClass", "badge-warning");
                List<ArchiveUrl> archiveUrlsData = getLearnweb().getTimelineManager().getArchiveUrlsByResourceIdAndTimestamp(resource.getId(), timelineData.getTimestamp(), resource.getUrl());
                JSONArray archiveVersions = new JSONArray();
                for(ArchiveUrl archiveUrl : archiveUrlsData)
                {
                    JSONObject archiveVersion = new JSONObject();
                    archiveVersion.put("url", archiveUrl.getArchiveUrl());
                    archiveVersion.put("time", DateFormat.getTimeInstance(DateFormat.MEDIUM, UtilBean.getUserBean().getLocale()).format(archiveUrl.getTimestamp()));
                    archiveVersions.add(archiveVersion);
                }
                archiveDay.put("dayEvents", archiveVersions);
                archiveDates.put(dateFormat.format(timelineData.getTimestamp()), archiveDay);
            }
        }
        catch(SQLException e)
        {
            log.error("Error while fetching the archive data aggregated by day for a resource", e);
            addGrowl(FacesMessage.SEVERITY_INFO, "fatal_error");
        }
        return archiveDates.toJSONString();
    }

    /**
     * Function to get short week day names for the calendar
     */
    public List<String> getShortWeekDays()
    {
        DateFormatSymbols symbols = new DateFormatSymbols(UtilBean.getUserBean().getLocale());
        List<String> dayNames = Arrays.asList(symbols.getShortWeekdays());
        Collections.rotate(dayNames.subList(1, 8), -1);
        return dayNames.subList(1, 8);
    }

    /**
     * Function to localized month names for the calendar
     * The method is used from JS in archive_timeline_template.xhtml
     */
    @SuppressWarnings({ "unchecked" })
    public String getMonthNames()
    {
        DateFormatSymbols symbols = new DateFormatSymbols(UtilBean.getUserBean().getLocale());
        JSONArray monthNames = new JSONArray();
        for(String month : symbols.getMonths())
        {
            if(!month.equals(""))
                monthNames.add(month);
        }

        return monthNames.toJSONString();
    }

    /**
     * Function to get localized short month names for the timeline
     * The method is used from JS in archive_timeline_template.xhtml
     */
    @SuppressWarnings({ "unchecked" })
    public String getShortMonthNames()
    {
        DateFormatSymbols symbols = new DateFormatSymbols(UtilBean.getUserBean().getLocale());
        JSONArray monthNames = new JSONArray();
        for(String month : symbols.getShortMonths())
        {
            if(!month.equals(""))
                monthNames.add(month);
        }

        return monthNames.toJSONString();
    }

    /* Load beans  */

    public FileEditorBean getFileEditorBean()
    {
        return fileEditorBean;
    }

    public void setFileEditorBean(FileEditorBean fileEditorBean)
    {
        this.fileEditorBean = fileEditorBean;
    }

    public AddResourceBean getAddResourceBean()
    {
        return addResourceBean;
    }

    public void setAddResourceBean(AddResourceBean addResourceBean)
    {
        this.addResourceBean = addResourceBean;
    }
}
