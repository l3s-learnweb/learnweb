package de.l3s.learnweb.beans;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.l3s.learnweb.Folder;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.Resource.ResourceType;
import de.l3s.learnweb.Resource.ResourceViewRights;
import de.l3s.learnweb.ResourcePreviewMaker;
import de.l3s.learnweb.Submission;
import de.l3s.learnweb.User;
import de.l3s.learnweb.beans.GroupDetailBean.RPAction;
import de.l3s.office.FileEditorBean;
import de.l3s.util.StringHelper;

/**
 * Bean for pages myhome/submission_overview.jsf and myhome/submission_resources.jsf
 * 
 * @author Trevor
 *
 */
@ManagedBean
@ViewScoped
public class SubmitResourcesBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -2494290373382483709L;
    private final static Logger log = Logger.getLogger(SubmitResourcesBean.class);

    private int userId; //For checking specific user's submission
    private int courseId; //For retrieving submissions of specific course
    private int submissionId;
    private Submission newSubmission = new Submission();
    private Submission selectedSubmission = new Submission();
    private boolean submitted = false;
    private RPAction rightPanelAction = null;

    private Resource clickedResource;
    private Folder clickedFolder;
    private List<Resource> resources;
    private List<Resource> selectedResources;

    private List<Submission> pastSubmissions;
    private List<Submission> currentSubmissions;
    private List<Submission> futureSubmissions;
    private List<SelectItem> surveyResourcesList;
    private List<SelectItem> editSurveyResourcesList;
    private boolean submissionOverviewReadOnly = false;

    private List<User> users; //To fetch list of users for a given course
    private Map<Integer, Integer> userSubmissions; //to store map of user id and total no. of submissions
    @ManagedProperty(value = "#{resourceDetailBean}")
    private ResourceDetailBean resourceDetailBean;

    @ManagedProperty(value = "#{fileEditorBean}")
    private FileEditorBean fileEditorBean;

    public SubmitResourcesBean() throws SQLException
    {
        if(getUser() == null) // not logged in
            return;

        clickedResource = new Resource();
        surveyResourcesList = new ArrayList<SelectItem>();
        editSurveyResourcesList = new ArrayList<SelectItem>();

        if(getParameterInt("resource_id") != null)
            setRightPanelAction("viewResource");
    }

    public void preRenderView(ComponentSystemEvent e) throws SQLException
    {
        if(getUser() == null) // not logged in
            return;

        if(isAjaxRequest())
        {
            //log.debug("Skip ajax request");
            return;
        }

        selectedResources = new ArrayList<Resource>();
        if(submissionId > 0 && userId > 0)
        {
            selectedSubmission = getLearnweb().getSubmissionManager().getSubmissionById(submissionId);
            List<Resource> submittedResources = getLearnweb().getSubmissionManager().getResourcesByIdAndUserId(submissionId, userId);
            if(!submittedResources.isEmpty())
            {
                selectedResources.addAll(submittedResources);
                submitted = true;
            }
            if(selectedSubmission != null && selectedSubmission.isPastSubmission())
            {
                submitted = true;
            }
        }

        //log.info("submission id:" + submissionId + " max no. of resources: " + selectedSubmission.getNoOfResources());

        if(getUser().getId() != this.userId)
            submissionOverviewReadOnly = true;

        getFacesContext().getExternalContext().setResponseCharacterEncoding("UTF-8");
        // stop caching (back button problem)
        HttpServletResponse response = (HttpServletResponse) getFacesContext().getExternalContext().getResponse();

        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
        response.setDateHeader("Expires", 0); // Proxies.

    }

    public List<Resource> getResources()
    {
        if(resources == null)
        {
            try
            {
                resources = getLearnweb().getResourceManager().getFolderResourcesByUserId(0, 0, userId, 1000);
            }
            catch(SQLException e)
            {
                log.error("Error while retrieving my resources in submit resources page:", e);
            }
        }
        return resources;
    }

    public List<Resource> getSelectedResources()
    {
        return selectedResources;
    }

    public Resource getClickedResource()
    {
        return clickedResource;
    }

    public Folder getClickedFolder()
    {
        return clickedFolder;
    }

    public void setClickedResource(Resource resource)
    {
        clickedResource = resource;
        this.getResourceDetailBean().setClickedResource(clickedResource);
        this.rightPanelAction = RPAction.viewResource;
    }

    public void actionSelectGroupItem()
    {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();

        try
        {
            String itemType = params.get("itemType");
            int itemId = StringHelper.parseInt(params.get("itemId"), -1);

            if(itemType != null && itemType.equals("resource") && itemId > 0)
            {
                Resource resource = getLearnweb().getResourceManager().getResource(itemId);
                if(resource != null)
                {
                    this.setClickedResource(resource);
                    if((resource.getType().equals("Presentation") || resource.getType().equals("Text") || resource.getType().equals("Spreadsheet")) && resource.getStorageType() == 1)
                        getFileEditorBean().fillInFileInfo(resource);
                }
                else
                    throw new NullPointerException("Target resource does not exists");
            }

        }
        catch(NullPointerException | SQLException e)
        {
            log.error(e);
        }
    }

    public void actionUpdateSelectedItems()
    {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String action = params.get("action");

        try
        {
            JSONArray items = new JSONArray(params.get("items"));

            switch(action)
            {
            case "add":
                this.actionAddSelectedItems(items);
                break;
            case "remove":
                this.actionRemoveItems(items);
                break;
            default:
                log.warn("Unsupported action: " + action);
                break;
            }
        }
        catch(JSONException e)
        {
            log.error("Exception while parsing items in actionUpdateSelectedItems", e);
        }
    }

    public void actionSubmitItems()
    {
        try
        {
            log.info("No. of selected items: " + selectedResources.size());
            User u = getLearnweb().getUserManager().getUser(11212); //special user id
            ResourcePreviewMaker rpm = getLearnweb().getResourcePreviewMaker();

            List<Resource> clonedSelectedResources = new ArrayList<Resource>();
            for(Resource r : selectedResources)
            {
                log.debug(r.getId());
                Resource clonedResource = r.clone();

                //So that owner of clonedResource can view the resource
                clonedResource.setOriginalResourceId(r.getId());
                clonedResource.setRights(ResourceViewRights.original_owner_readable);

                u.addResource(clonedResource); //save cloned resource with special user id

                //clone comments/tags of resource if it exists
                clonedResource.cloneComments(r.getComments());
                clonedResource.cloneTags(r.getTags());

                if(clonedResource.getType() == ResourceType.website)
                {
                    String response = getLearnweb().getArchiveUrlManager().addResourceToArchive(clonedResource);
                    if(response.equals("ROBOTS_ERROR") || response.equals("GENERIC_ERROR") || response.equals("PARSE_DATE_ERROR") || response.equals("SQL_SAVE_ERROR"))
                    {
                        if(clonedResource.getThumbnail0() == null)
                        {
                            try
                            {
                                rpm.processResource(clonedResource);
                            }
                            catch(IOException | SQLException e)
                            {
                                log.error("Could not archive the resource during submission because of " + response + " for resource " + clonedResource.getId());
                                log.error("Error during submission while processing thumbnails for resource: " + clonedResource.getId(), e);
                            }
                        }
                    }
                }

                getLearnweb().getSubmissionManager().saveSubmissionResource(submissionId, clonedResource.getId(), userId);

                log(Action.submission_submitted, 0, clonedResource.getId());

                clonedSelectedResources.add(clonedResource);
            }
            selectedResources.clear();
            selectedResources.addAll(clonedSelectedResources);
            setSubmitted(true);

            addGrowl(FacesMessage.SEVERITY_INFO, "Submission.success_message");
        }
        catch(SQLException e)
        {
            log.error("Exception while submitting resources", e);
            addFatalMessage(e);
        }
    }

    private void actionRemoveItems(JSONArray objects)
    {
        try
        {
            for(int i = 0, len = objects.length(); i < len; ++i)
            {
                JSONObject item = objects.getJSONObject(i);

                String itemType = item.getString("itemType");
                int itemId = StringHelper.parseInt(item.getString("itemId"), -1);

                if(itemType != null && itemType.equals("resource") && itemId > 0)
                {
                    Resource resource = getLearnweb().getResourceManager().getResource(itemId);
                    if(resource != null && selectedResources.contains(resource))
                    {
                        selectedResources.remove(resource);
                    }
                    if(resource.equals(clickedResource))
                    {
                        setClickedResource(new Resource());
                    }
                }
            }

        }
        catch(NullPointerException | JSONException | SQLException e)
        {
            log.error("Exception while parsing selected items in actionRemoveItems", e);
        }

    }

    private void actionAddSelectedItems(JSONArray objects)
    {
        try
        {
            for(int i = 0, len = objects.length(); i < len; ++i)
            {
                JSONObject item = objects.getJSONObject(i);

                String itemType = item.getString("itemType");
                int itemId = StringHelper.parseInt(item.getString("itemId"), -1);

                if(itemType != null && itemType.equals("resource") && itemId > 0)
                {
                    Resource resource = getLearnweb().getResourceManager().getResource(itemId);
                    if(resource != null && !selectedResources.contains(resource))
                    {
                        selectedResources.add(resource);
                    }
                }
            }

        }
        catch(NullPointerException | JSONException | SQLException e)
        {
            log.error("Exception while parsing selected items in actionAddSelectedItems", e);
        }
    }

    public ResourceDetailBean getResourceDetailBean()
    {
        return resourceDetailBean;
    }

    public void setResourceDetailBean(ResourceDetailBean resourceDetailBean)
    {
        this.resourceDetailBean = resourceDetailBean;
    }

    public FileEditorBean getFileEditorBean()
    {
        return fileEditorBean;
    }

    public void setFileEditorBean(FileEditorBean fileEditorBean)
    {
        this.fileEditorBean = fileEditorBean;
    }

    public RPAction getRightPanelAction()
    {
        return rightPanelAction;
    }

    public void setRightPanelAction(RPAction rightPanelAction)
    {
        this.rightPanelAction = rightPanelAction;
    }

    public void setRightPanelAction(String value)
    {
        try
        {
            this.rightPanelAction = RPAction.valueOf(value);
        }
        catch(Exception e)
        {
            this.rightPanelAction = null;
            log.debug(e);
        }
    }

    public int getSubmissionId()
    {
        return submissionId;
    }

    public void setSubmissionId(int submissionId)
    {
        this.submissionId = submissionId;
    }

    public boolean isSubmitted()
    {
        return submitted;
    }

    public void setSubmitted(boolean submitted)
    {
        this.submitted = submitted;
    }

    public Submission getSelectedSubmission()
    {
        return selectedSubmission;
    }

    public void setSelectedSubmission(Submission submission)
    {
        this.selectedSubmission = submission;
    }

    public int getUserId()
    {
        return userId;
    }

    public void setUserId(int userId)
    {
        this.userId = userId;
    }

    public int getCourseId()
    {
        return courseId;
    }

    public void setCourseId(int courseId)
    {
        this.courseId = courseId;
    }

    /* -------- Methods below are used for the submission overview page ---------*/
    public void createNewSubmission()
    {
        getLearnweb().getSubmissionManager().saveSubmission(newSubmission);
        clearSubmissionLists();
        this.newSubmission = new Submission();
    }

    public Submission getNewSubmission()
    {
        return newSubmission;
    }

    public void updateSubmissionDetails()
    {
        getLearnweb().getSubmissionManager().saveSubmission(selectedSubmission);
        clearSubmissionLists();
    }

    public void deleteSubmission()
    {
        getLearnweb().getSubmissionManager().deleteSubmission(selectedSubmission.getId());
        clearSubmissionLists();
    }

    public void clearSubmissionLists()
    {
        pastSubmissions = null;
        currentSubmissions = null;
        futureSubmissions = null;
    }

    public void fetchSubmissions()
    {
        pastSubmissions = new ArrayList<Submission>();
        currentSubmissions = new ArrayList<Submission>();
        futureSubmissions = new ArrayList<Submission>();

        try
        {
            User u = Learnweb.getInstance().getUserManager().getUser(userId);
            //if no user_id parameter is provided in URL
            if(u == null)
                u = getUser();

            //int courseId = this.courseId > 0 ? this.courseId : u.getActiveCourseId();

            List<Submission> submissions = Learnweb.getInstance().getSubmissionManager().getSubmissionsByUser(u);
            for(Submission s : submissions)
            {
                if(s.isPastSubmission())
                    pastSubmissions.add(s);
                else if(s.isCurrentSubmission())
                    currentSubmissions.add(s);
                else
                    futureSubmissions.add(s);
            }
        }
        catch(SQLException e)
        {
            addFatalMessage(e);
        }
    }

    public List<Submission> getPastSubmissions()
    {
        if(pastSubmissions == null)
            fetchSubmissions();

        return pastSubmissions;
    }

    public List<Submission> getCurrentSubmissions()
    {
        if(currentSubmissions == null)
            fetchSubmissions();

        return currentSubmissions;
    }

    public List<Submission> getFutureSubmissions()
    {
        if(futureSubmissions == null)
            fetchSubmissions();

        return futureSubmissions;
    }

    public List<Submission> getSubmissions()
    {
        return Learnweb.getInstance().getSubmissionManager().getSubmissionsByUser(getUser());
    }

    public List<User> getUsers() throws SQLException
    {
        if(users == null)
        {
            //HashMap<User, Integer> usersSubmissions = new HashMap<User, Integer>();
            Integer courseId = getParameterInt("course_id");

            if(courseId != null)
            {
                this.courseId = courseId;
                users = getLearnweb().getCourseManager().getCourseById(courseId).getMembers();
                userSubmissions = getLearnweb().getSubmissionManager().getUsersSubmissionsByCourseId(courseId);
            }
        }
        return users;
    }

    public int getNumberOfSubmissions(int userId)
    {
        if(userSubmissions.containsKey(userId))
            return userSubmissions.get(userId);

        return 0;
    }

    public List<Submission> getActiveSubmissions()
    {
        if(currentSubmissions == null)
            currentSubmissions = getLearnweb().getSubmissionManager().getActiveSubmissionsByUser(getUser());
        return currentSubmissions;
    }

    public List<SelectItem> getSurveyResourcesList()
    {
        return surveyResourcesList;
    }

    public String getResourcePath(Resource r)
    {
        String resourcePath = null;
        try
        {
            if(r.getGroupId() == 0)
                resourcePath = "My Resources > " + r.getTitle();
            else if(r.getGroupId() > 0)
            {
                if(r.getPrettyPath() != null)
                {
                    resourcePath = r.getPrettyPath() + " > " + r.getTitle();
                }
                else
                    resourcePath = r.getGroup().getTitle() + " > " + r.getTitle();
            }
        }
        catch(SQLException e)
        {
            log.error("Error while retrieving group information for resource: " + r.getId(), e);
        }
        return resourcePath;
    }

    public List<SelectItem> getEditSurveyResourcesList()
    {
        if(editSurveyResourcesList.isEmpty() && this.selectedSubmission.getCourseId() > 0)
        {
            List<Resource> editSurveyResourcesForCourse = getLearnweb().getSurveyManager().getSurveyResourcesByUserAndCourse(getUserId(), this.selectedSubmission.getCourseId());
            for(Resource r : editSurveyResourcesForCourse)
            {
                String resourcePath = getResourcePath(r);
                if(resourcePath != null)
                    editSurveyResourcesList.add(new SelectItem(r.getId(), resourcePath));
            }
        }
        return editSurveyResourcesList;
    }

    public void onCreateSurveyChangeCourse(AjaxBehaviorEvent event)
    {
        surveyResourcesList.clear();
        List<Resource> surveyResourcesForCourse = getLearnweb().getSurveyManager().getSurveyResourcesByUserAndCourse(getUserId(), this.newSubmission.getCourseId());
        for(Resource r : surveyResourcesForCourse)
        {
            String resourcePath = getResourcePath(r);
            if(resourcePath != null)
                surveyResourcesList.add(new SelectItem(r.getId(), resourcePath));
        }
    }

    public void onEditSurveyChangeCourse(AjaxBehaviorEvent event)
    {
        editSurveyResourcesList.clear();
        List<Resource> surveyResourcesForCourse = getLearnweb().getSurveyManager().getSurveyResourcesByUserAndCourse(getUserId(), this.newSubmission.getCourseId());
        for(Resource r : surveyResourcesForCourse)
        {
            String resourcePath = getResourcePath(r);
            if(resourcePath != null)
                surveyResourcesList.add(new SelectItem(r.getId(), resourcePath));
        }
    }

    public boolean isSubmissionOverviewReadOnly()
    {
        return submissionOverviewReadOnly;
    }

    public String getCourseTitle(int courseId)
    {
        if(courseId > 0)
            return getLearnweb().getCourseManager().getCourseById(courseId).getTitle();
        return null;
    }
}
