package de.l3s.learnweb.resource.submission;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.Resource.ResourceViewRights;
import de.l3s.learnweb.resource.ResourcePreviewMaker;
import de.l3s.learnweb.resource.ResourceType;
import de.l3s.learnweb.user.User;
import de.l3s.util.bean.BeanHelper;

/**
 * Bean for pages myhome/submission_overview.jsf and myhome/submission_resources.jsf
 *
 * TODO: the whole class needs to be refactored. First it has to be splitted into individual beans for the different pages
 *
 * @author Trevor
 */
@Named
@ViewScoped
public class SubmissionBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = -2494290373382483709L;
    private static final Logger log = LogManager.getLogger(SubmissionBean.class);

    private int userId; //For checking specific user's submission
    private int courseId; //For retrieving submissions of specific course
    private int submissionId;
    private Submission newSubmission = new Submission();
    private Submission selectedSubmission = new Submission();
    private boolean submitted = false;

    private List<Resource> resources;
    private List<Resource> selectedResources = new ArrayList<>(4);

    private List<Submission> pastSubmissions;
    private List<Submission> currentSubmissions;
    private List<Submission> futureSubmissions;
    private final List<SelectItem> surveyResourcesList = new ArrayList<>();
    private final List<SelectItem> editSurveyResourcesList = new ArrayList<>();
    private boolean submissionOverviewReadOnly = false;

    private List<User> users; //To fetch list of users for a given course
    private Map<Integer, Integer> userSubmissions; //to store map of user id and total no. of submissions

    public void onLoad() throws SQLException {
        if (getUser() == null) { // not logged in
            return;
        }

        if (this.userId == 0) { // don't want to view the submission of a specific user then use the current user (usual case)
            this.userId = getUser().getId();
        }

        //When moderator or other assessor accesses a student's overview page to not display edit option for a submission
        if (getUser().getId() != this.userId) {
            submissionOverviewReadOnly = true;

            // check if moderator or submission assessor
            if (!getUser().isModerator()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "You are not allowed to view this submission");
                log.error("Unprivileged access: " + BeanHelper.getRequestSummary());
                return;
            }
        }

        //When accessing the submission_resources page both these parameters are set
        if (submissionId > 0 && userId > 0) {
            selectedSubmission = getLearnweb().getSubmissionManager().getSubmissionById(submissionId);
            selectedResources = getLearnweb().getSubmissionManager().getResourcesByIdAndUserId(submissionId, userId);

            submitted = getLearnweb().getSubmissionManager().getSubmitStatusForUser(submissionId, userId);

            //Past submissions are always considered as submitted
            if (selectedSubmission != null && selectedSubmission.isPastSubmission()) {
                submitted = true;
            }

            log(Action.submission_view_resources, 0, submissionId, userId);
        }

        getFacesContext().getExternalContext().setResponseCharacterEncoding("UTF-8");
        // stop caching (back button problem)
        HttpServletResponse response = (HttpServletResponse) getFacesContext().getExternalContext().getResponse();

        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
        response.setDateHeader("Expires", 0); // Proxies.
    }

    /**
     * To display my resources in the lightbox that pops up in the submission_resources page.
     */
    public List<Resource> getResources() {
        if (resources == null) {
            try {
                resources = getLearnweb().getResourceManager().getFolderResourcesByUserId(0, 0, userId, 1000);
            } catch (SQLException e) {
                log.error("Error while retrieving my resources in submit resources page:", e);
            }
        }
        return resources;
    }

    public List<Resource> getSelectedResources() {
        return selectedResources;
    }

    public void actionUpdateSelectedItems() {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        String action = params.get("action");
        JsonArray items = JsonParser.parseString(params.get("items")).getAsJsonArray();
        if (items.size() > selectedSubmission.getNoOfResources()) {
            addGrowl(FacesMessage.SEVERITY_ERROR, "submission.resource_limit_exceeded", selectedSubmission.getNoOfResources());
            return;
        }
        try {

            switch (action) {
                case "add":
                    this.actionAddSelectedItems(items);
                    break;
                case "remove":
                    this.actionRemoveItems(items);
                    break;
                case "update":
                    selectedResources.clear();
                    this.actionAddSelectedItems(items);
                    break;
                default:
                    log.error("Unsupported action: " + action);
                    break;
            }
        } catch (JsonParseException e) {
            log.error("Exception while parsing items in actionUpdateSelectedItems", e);
        }
    }

    public void actionSubmitItems() throws SQLException {
        selectedSubmission = getLearnweb().getSubmissionManager().getSubmissionById(submissionId);
        submitted = getLearnweb().getSubmissionManager().getSubmitStatusForUser(submissionId, userId);
        if (submitted) {
            addGrowl(FacesMessage.SEVERITY_ERROR, "submission.already_submitted");
            return;
        }
        if (selectedResources.size() > selectedSubmission.getNoOfResources()) {
            addGrowl(FacesMessage.SEVERITY_ERROR, "submission.resource_limit_exceeded", selectedSubmission.getNoOfResources());
            return;
        }
        try {
            log.info("No. of selected items: " + selectedResources.size());
            User specialAdmin = getLearnweb().getUserManager().getUser(SubmissionManager.SUBMISSION_ADMIN_USER_ID); //special user id
            ResourcePreviewMaker rpm = getLearnweb().getResourcePreviewMaker();
            Date submissionDate = new Date(); //Date for the resources submitted, so that the moderator can know when they were submitted

            List<Resource> clonedSelectedResources = new ArrayList<>();
            for (Resource r : selectedResources) {
                log.debug("resource: " + r.getId() + "; submission: " + submissionId + "; user:" + userId);
                if (r.getUserId() != specialAdmin.getId()) {
                    Resource clonedResource = r.clone();

                    //So that owner of clonedResource can view the resource
                    clonedResource.setOriginalResourceId(r.getId());
                    clonedResource.setRights(ResourceViewRights.SUBMISSION_READABLE);
                    clonedResource.setCreationDate(submissionDate);

                    specialAdmin.addResource(clonedResource); //save cloned resource with special user id

                    //clone comments/tags of resource if it exists
                    clonedResource.cloneComments(r.getComments());
                    clonedResource.cloneTags(r.getTags());

                    if (clonedResource.getType() == ResourceType.website) {

                        String response = getLearnweb().getArchiveUrlManager().addResourceToArchive(clonedResource);
                        if (response.equals("ROBOTS_ERROR") || response.equals("GENERIC_ERROR") || response.equals("PARSE_DATE_ERROR") || response.equals("SQL_SAVE_ERROR")) {
                            if (clonedResource.getThumbnail0() == null) {
                                try {
                                    rpm.processResource(clonedResource);
                                } catch (IOException | SQLException e) {
                                    log.error("Could not archive the resource during submission because of " + response + " for resource " + clonedResource.getId());
                                    log.error("Error during submission while processing thumbnails for resource: " + clonedResource.getId(), e);
                                }
                            }
                        }
                    }

                    getLearnweb().getSubmissionManager().saveSubmissionResource(submissionId, clonedResource.getId(), userId);

                    log(Action.submission_submitted, 0, r.getId()); // this doesn't happen in a group context. Hence group_id = 0

                    clonedSelectedResources.add(clonedResource);
                } else {
                    clonedSelectedResources.add(r);
                }
            }
            selectedResources.clear();
            selectedResources.addAll(clonedSelectedResources);
            setSubmitted(true);
            getLearnweb().getSubmissionManager().saveSubmitStatusForUser(submissionId, userId, true);

            addGrowl(FacesMessage.SEVERITY_INFO, "Submission.success_message");
        } catch (SQLException e) {
            log.error("Exception while submitting resources", e);
            addErrorMessage(e);
        }
    }

    private void actionRemoveItems(JsonArray objects) {
        try {
            for (int i = 0, len = objects.size(); i < len; ++i) {
                JsonObject item = objects.get(i).getAsJsonObject();

                String itemType = item.get("itemType").getAsString();
                int itemId = item.get("itemId").getAsInt();

                if ("resource".equals(itemType) && itemId > 0) {
                    Resource resource = getLearnweb().getResourceManager().getResource(itemId);
                    if (resource != null && selectedResources.contains(resource)) {
                        selectedResources.remove(resource);
                        if (resource.getUserId() == SubmissionManager.SUBMISSION_ADMIN_USER_ID) {
                            resource.delete();
                            getLearnweb().getSubmissionManager().deleteSubmissionResource(submissionId, resource.getId(), userId);
                        }
                    }
                }
            }

        } catch (NullPointerException | JsonParseException | SQLException e) {
            log.error("Exception while parsing selected items in actionRemoveItems", e);
        }

    }

    private void actionAddSelectedItems(JsonArray objects) {
        try {
            for (int i = 0, len = objects.size(); i < len; ++i) {
                JsonObject item = objects.get(i).getAsJsonObject();
                String itemType = item.get("itemType").getAsString();

                int itemId = item.get("itemId").getAsInt();
                if ("resource".equals(itemType) && itemId > 0) {
                    Resource resource = getLearnweb().getResourceManager().getResource(itemId);
                    if (resource != null && !selectedResources.contains(resource)) {
                        selectedResources.add(resource);
                    }
                }
            }

        } catch (NullPointerException | JsonParseException | SQLException e) {
            log.error("Exception while parsing selected items in actionAddSelectedItems", e);
        }
    }

    public int getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(int submissionId) {
        this.submissionId = submissionId;
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public void setSubmitted(boolean submitted) {
        this.submitted = submitted;
    }

    public Submission getSelectedSubmission() {
        return selectedSubmission;
    }

    public void setSelectedSubmission(Submission submission) {
        this.selectedSubmission = submission;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    /* -------- Methods below are used for the submission overview page ---------*/
    // TODO use one bean per page

    public void createNewSubmission() {
        getLearnweb().getSubmissionManager().saveSubmission(newSubmission);
        clearSubmissionLists();
        this.newSubmission = new Submission();
        addMessage(FacesMessage.SEVERITY_INFO, "Changes_saved");

        getUser().clearCaches();
        // TODO call User.clearCaches of all users of the affected course
    }

    public Submission getNewSubmission() {
        return newSubmission;
    }

    public void updateSubmissionDetails() {
        getLearnweb().getSubmissionManager().saveSubmission(selectedSubmission);
        clearSubmissionLists();
        addMessage(FacesMessage.SEVERITY_INFO, "Changes_saved");
    }

    public void deleteSubmission() {
        getLearnweb().getSubmissionManager().deleteSubmission(selectedSubmission.getId());
        clearSubmissionLists();
        addMessage(FacesMessage.SEVERITY_INFO, "Changes_saved");
    }

    public void clearSubmissionLists() {
        pastSubmissions = null;
        currentSubmissions = null;
        futureSubmissions = null;
    }

    public void fetchSubmissions() {
        pastSubmissions = new ArrayList<>();
        currentSubmissions = new ArrayList<>();
        futureSubmissions = new ArrayList<>();

        try {
            User u = Learnweb.getInstance().getUserManager().getUser(userId);
            //if no user_id parameter is provided in URL
            if (u == null) {
                u = getUser();
            }

            List<Submission> submissions = Learnweb.getInstance().getSubmissionManager().getSubmissionsByUser(u);
            for (Submission s : submissions) {
                if (s.isPastSubmission()) {
                    pastSubmissions.add(s);
                } else if (s.isCurrentSubmission()) {
                    currentSubmissions.add(s);
                } else {
                    futureSubmissions.add(s);
                }
            }
        } catch (SQLException e) {
            addErrorMessage(e);
        }
    }

    public List<Submission> getPastSubmissions() {
        if (pastSubmissions == null) {
            fetchSubmissions();
        }

        return pastSubmissions;
    }

    public List<Submission> getCurrentSubmissions() {
        if (currentSubmissions == null) {
            fetchSubmissions();
        }

        return currentSubmissions;
    }

    public List<Submission> getFutureSubmissions() {
        if (futureSubmissions == null) {
            fetchSubmissions();
        }

        return futureSubmissions;
    }

    public List<Submission> getSubmissions() {
        try {
            return Learnweb.getInstance().getSubmissionManager().getSubmissionsByUser(getUser());
        } catch (SQLException e) {
            addErrorMessage(e);
        }
        return null;
    }

    public List<SelectItem> getSurveyResourcesList() {
        return surveyResourcesList;
    }

    public String getResourcePath(Resource r) {
        StringBuilder sb = new StringBuilder();
        try {
            sb.append(r.getGroupId() == 0 ? getLocaleMessage("myPrivateResources") : r.getGroup().getTitle());

            if (r.getPrettyPath() != null) {
                sb.append(" > ").append(r.getPrettyPath());
            }

            sb.append(" > ").append(r.getTitle());
        } catch (SQLException e) {
            log.error("Error while retrieving group information for resource: " + r.getId(), e);
        }
        return sb.toString();
    }

    public List<SelectItem> getEditSurveyResourcesList() {
        if (editSurveyResourcesList.isEmpty() && this.selectedSubmission.getCourseId() > 0) {
            try {
                List<Resource> editSurveyResourcesForCourse = getLearnweb().getSurveyManager().getSurveyResourcesByUserAndCourse(this.selectedSubmission.getCourseId());
                for (Resource r : editSurveyResourcesForCourse) {
                    String resourcePath = getResourcePath(r);
                    if (resourcePath != null) {
                        editSurveyResourcesList.add(new SelectItem(r.getId(), resourcePath));
                    }
                }
            } catch (Exception e) {
                log.error("Error in getting edit survey resources list for user: " + getUserId(), e);
            }
        }
        return editSurveyResourcesList;
    }

    public void onCreateSurveyChangeCourse(AjaxBehaviorEvent event) {
        surveyResourcesList.clear();
        try {
            List<Resource> surveyResourcesForCourse = getLearnweb().getSurveyManager().getSurveyResourcesByUserAndCourse(this.newSubmission.getCourseId());
            for (Resource r : surveyResourcesForCourse) {
                String resourcePath = getResourcePath(r);
                if (resourcePath != null) {
                    surveyResourcesList.add(new SelectItem(r.getId(), resourcePath));
                }
            }
        } catch (Exception e) {
            log.error("Error in getting survey resources for  course", e);
        }
    }

    public void onEditSurveyChangeCourse(AjaxBehaviorEvent event) {
        editSurveyResourcesList.clear();
        try {
            List<Resource> surveyResourcesForCourse = getLearnweb().getSurveyManager().getSurveyResourcesByUserAndCourse(this.newSubmission.getCourseId());
            for (Resource r : surveyResourcesForCourse) {
                String resourcePath = getResourcePath(r);
                if (resourcePath != null) {
                    editSurveyResourcesList.add(new SelectItem(r.getId(), resourcePath));
                }
            }
        } catch (Exception e) {
            log.error("Error in getting survey resources for course ", e);
        }
    }

    public boolean isSubmissionOverviewReadOnly() {
        return submissionOverviewReadOnly;
    }

    public void unlockSubmission() {
        if (selectedSubmission != null) {
            selectedSubmission.setSubmitted(false);
            getLearnweb().getSubmissionManager().saveSubmitStatusForUser(selectedSubmission.getId(), userId, false);
            addMessage(FacesMessage.SEVERITY_INFO, "Changes_saved");
        }
    }

    public void lockSubmission() {
        if (selectedSubmission != null) {
            selectedSubmission.setSubmitted(true);
            getLearnweb().getSubmissionManager().saveSubmitStatusForUser(selectedSubmission.getId(), userId, true);
            addMessage(FacesMessage.SEVERITY_INFO, "Changes_saved");
        }
    }

    /* -------- Methods below are used for the admin users submissions page ---------*/
    // TODO needs to be refactored. Has to be discussed first. Either move to other bean or integrate into Moderator user list or ....

    /**
     * To display the users for a particular course and the corresponding number of submissions.
     * on the admin/users_submissions page
     */
    public List<User> getUsers() throws SQLException {
        if (users == null) {
            Integer courseId = getParameterInt("course_id"); // TODO don't use this

            if (courseId != null) {
                this.courseId = courseId;
                users = getLearnweb().getCourseManager().getCourseById(courseId).getMembers();
                userSubmissions = getLearnweb().getSubmissionManager().getUsersSubmissionsByCourseId(courseId);
            }
        }
        return users;
    }

    public int getNumberOfSubmissions(int userId) {
        if (userSubmissions.containsKey(userId)) {
            return userSubmissions.get(userId);
        }

        return 0;
    }
}
