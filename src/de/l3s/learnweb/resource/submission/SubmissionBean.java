package de.l3s.learnweb.resource.submission;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.util.Beans;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Servlets;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.Resource.ResourceViewRights;
import de.l3s.learnweb.resource.ResourceDao;
import de.l3s.learnweb.resource.ResourceType;
import de.l3s.learnweb.resource.archive.ArchiveUrlManager;
import de.l3s.learnweb.user.CourseDao;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserDao;

/**
 * Bean for pages myhome/submission_overview.jsf and myhome/submission_resources.jsf
 *
 * TODO @astappiev/@hulyi: the whole class needs to be refactored. First it has to be splitted into individual beans for the different pages
 *
 * @author Trevor
 */
@Named
@ViewScoped
public class SubmissionBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = -2494290373382483709L;
    private static final Logger log = LogManager.getLogger(SubmissionBean.class);

    public static final int SUBMISSION_ADMIN_USER_ID = 11212;

    private int userId; // For checking specific user's submission
    private int courseId; // For retrieving submissions of specific course
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

    @Inject
    private CourseDao courseDao;

    @Inject
    private UserDao userDao;

    @Inject
    private SubmissionDao submissionDao;

    @Inject
    private ResourceDao resourceDao;

    public void onLoad() {
        BeanAssert.authorized(isLoggedIn());

        if (this.userId == 0) { // don't want to view the submission of a specific user then use the current user (usual case)
            this.userId = getUser().getId();
        }

        //When moderator or other assessor accesses a student's overview page to not display edit option for a submission
        if (getUser().getId() != this.userId) {
            submissionOverviewReadOnly = true;

            // check if moderator or submission assessor
            BeanAssert.hasPermission(getUser().isModerator());
        }

        //When accessing the submission_resources page both these parameters are set
        if (submissionId != 0 && userId != 0) {
            selectedSubmission = submissionDao.findById(submissionId).orElseThrow(BeanAssert.NOT_FOUND);
            selectedResources = resourceDao.findBySubmissionIdAndUserId(submissionId, userId);

            submitted = submissionDao.findStatus(submissionId, userId).orElse(false);

            //Past submissions are always considered as submitted
            if (selectedSubmission != null && selectedSubmission.isPastSubmission()) {
                submitted = true;
            }

            log(Action.submission_view_resources, 0, submissionId, userId);
        }

        // stop caching (back button problem)
        Servlets.setNoCacheHeaders(Faces.getResponse());
    }

    /**
     * To display my resources in the lightbox that pops up in the submission_resources page.
     */
    public List<Resource> getResources() {
        if (resources == null) {
            resources = resourceDao.findByGroupIdAndFolderIdAndOwnerId(0, 0, userId, 1000);
        }
        return resources;
    }

    public List<Resource> getSelectedResources() {
        return selectedResources;
    }

    public void commandUpdateSelectedItems() {
        Map<String, String> params = Faces.getRequestParameterMap();
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
                    log.error("Unsupported action: {}", action);
                    break;
            }
        } catch (JsonParseException e) {
            log.error("Exception while parsing items in actionUpdateSelectedItems", e);
        }
    }

    public void actionSubmitItems() {
        selectedSubmission = submissionDao.findById(submissionId).orElseThrow(BeanAssert.NOT_FOUND);
        submitted = submissionDao.findStatus(submissionId, userId).orElse(false);
        if (submitted) {
            addGrowl(FacesMessage.SEVERITY_ERROR, "submission.already_submitted");
            return;
        }
        if (selectedResources.size() > selectedSubmission.getNoOfResources()) {
            addGrowl(FacesMessage.SEVERITY_ERROR, "submission.resource_limit_exceeded", selectedSubmission.getNoOfResources());
            return;
        }
        log.info("No. of selected items: {}", selectedResources.size());
        User specialAdmin = userDao.findByIdOrElseThrow(SUBMISSION_ADMIN_USER_ID); //special user id
        LocalDateTime submissionDate = LocalDateTime.now(); //Date for the resources submitted, so that the moderator can know when they were submitted

        List<Resource> clonedSelectedResources = new ArrayList<>();
        for (Resource r : selectedResources) {
            log.debug("resource: {}; submission: {}; user:{}", r.getId(), submissionId, userId);
            if (r.getUserId() == specialAdmin.getId()) {
                clonedSelectedResources.add(r);
            } else {
                Resource clonedResource = r.clone();

                //So that owner of clonedResource can view the resource
                clonedResource.setOriginalResourceId(r.getId());
                clonedResource.setRights(ResourceViewRights.SUBMISSION_READABLE);
                clonedResource.setCreationDate(submissionDate);
                clonedResource.setUser(specialAdmin);
                clonedResource.save();

                //clone comments/tags of resource if it exists
                clonedResource.cloneComments(r.getComments());
                clonedResource.cloneTags(r.getTags());

                if (clonedResource.getType() == ResourceType.website) {

                    String response = Beans.getInstance(ArchiveUrlManager.class).addResourceToArchive(clonedResource);
                    if (StringUtils.equalsAny(response, "ROBOTS_ERROR", "GENERIC_ERROR", "PARSE_DATE_ERROR", "SQL_SAVE_ERROR")) {
                        if (clonedResource.getSmallThumbnail() == null) {
                            try {
                                getLearnweb().getResourcePreviewMaker().processResource(clonedResource);
                            } catch (IOException e) {
                                log.error("Could not archive the resource during submission because of {} for resource {}", response, clonedResource.getId());
                                log.error("Error during submission while processing thumbnails for resource: {}", clonedResource.getId(), e);
                            }
                        }
                    }
                }

                submissionDao.insertSubmissionResource(submissionId, clonedResource.getId(), userId);

                log(Action.submission_submitted, 0, r.getId()); // this doesn't happen in a group context. Hence group_id = 0

                clonedSelectedResources.add(clonedResource);
            }
        }
        selectedResources.clear();
        selectedResources.addAll(clonedSelectedResources);
        setSubmitted(true);

        submissionDao.insertSubmissionStatus(submissionId, userId, true);

        addGrowl(FacesMessage.SEVERITY_INFO, "Submission.success_message");
    }

    private void actionRemoveItems(JsonArray objects) {
        try {
            for (int i = 0, len = objects.size(); i < len; ++i) {
                JsonObject item = objects.get(i).getAsJsonObject();

                String itemType = item.get("itemType").getAsString();
                int itemId = item.get("itemId").getAsInt();

                if ("resource".equals(itemType) && itemId != 0) {
                    resourceDao.findById(itemId).ifPresent(resource -> {
                        if (selectedResources.contains(resource)) {
                            selectedResources.remove(resource);
                            if (resource.getUserId() == SUBMISSION_ADMIN_USER_ID) {
                                resource.delete();

                                submissionDao.deleteSubmissionResource(submissionId, resource.getId(), userId);
                            }
                        }
                    });
                }
            }
        } catch (JsonParseException e) {
            log.error("Exception while parsing selected items in actionRemoveItems", e);
        }
    }

    private void actionAddSelectedItems(JsonArray objects) {
        try {
            for (int i = 0, len = objects.size(); i < len; ++i) {
                JsonObject item = objects.get(i).getAsJsonObject();
                String itemType = item.get("itemType").getAsString();

                int itemId = item.get("itemId").getAsInt();
                if ("resource".equals(itemType) && itemId != 0) {
                    resourceDao.findById(itemId).ifPresent(resource -> {
                        if (!selectedResources.contains(resource)) {
                            selectedResources.add(resource);
                        }
                    });
                }
            }
        } catch (JsonParseException e) {
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
    // TODO @astappiev/@hulyi: use one bean per page

    public void createNewSubmission() {
        submissionDao.save(newSubmission);
        clearSubmissionLists();
        this.newSubmission = new Submission();
        addMessage(FacesMessage.SEVERITY_INFO, "Changes_saved");

        getUser().clearCaches();
        // TODO @astappiev/@hulyi: call User.clearCaches of all users of the affected course
    }

    public Submission getNewSubmission() {
        return newSubmission;
    }

    public void updateSubmissionDetails() {
        submissionDao.save(selectedSubmission);
        clearSubmissionLists();
        addMessage(FacesMessage.SEVERITY_INFO, "Changes_saved");
    }

    public void deleteSubmission() {
        submissionDao.deleteSoft(selectedSubmission);
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

        //if no user_id parameter is provided in URL
        User user = userDao.findById(userId).orElse(getUser());

        List<Submission> submissions = submissionDao.findByUser(user);
        for (Submission submission : submissions) {
            if (submission.isPastSubmission()) {
                pastSubmissions.add(submission);
            } else if (submission.isCurrentSubmission()) {
                currentSubmissions.add(submission);
            } else {
                futureSubmissions.add(submission);
            }
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
        return submissionDao.findByUser(getUser());
    }

    public List<SelectItem> getSurveyResourcesList() {
        return surveyResourcesList;
    }

    public String getResourcePath(Resource r) {
        StringBuilder sb = new StringBuilder();
        sb.append(r.getGroupId() == 0 ? getLocaleMessage("myPrivateResources") : r.getGroup().getTitle());

        if (r.getPrettyPath() != null) {
            sb.append(" > ").append(r.getPrettyPath());
        }

        sb.append(" > ").append(r.getTitle());
        return sb.toString();
    }

    public List<SelectItem> getEditSurveyResourcesList() {
        if (editSurveyResourcesList.isEmpty() && this.selectedSubmission.getCourseId() != 0) {
            try {
                List<Resource> editSurveyResourcesForCourse = resourceDao.findSurveysByCourseId(this.selectedSubmission.getCourseId());
                for (Resource r : editSurveyResourcesForCourse) {
                    String resourcePath = getResourcePath(r);
                    editSurveyResourcesList.add(new SelectItem(r.getId(), resourcePath));
                }
            } catch (Exception e) {
                log.error("Error in getting edit survey resources list for user: {}", getUserId(), e);
            }
        }
        return editSurveyResourcesList;
    }

    public void onCreateSurveyChangeCourse(AjaxBehaviorEvent event) {
        surveyResourcesList.clear();
        try {
            List<Resource> surveyResourcesForCourse = resourceDao.findSurveysByCourseId(this.newSubmission.getCourseId());
            for (Resource r : surveyResourcesForCourse) {
                String resourcePath = getResourcePath(r);
                surveyResourcesList.add(new SelectItem(r.getId(), resourcePath));
            }
        } catch (Exception e) {
            log.error("Error in getting survey resources for  course", e);
        }
    }

    public void onEditSurveyChangeCourse(AjaxBehaviorEvent event) {
        editSurveyResourcesList.clear();
        try {
            List<Resource> surveyResourcesForCourse = resourceDao.findSurveysByCourseId(this.newSubmission.getCourseId());
            for (Resource r : surveyResourcesForCourse) {
                String resourcePath = getResourcePath(r);
                editSurveyResourcesList.add(new SelectItem(r.getId(), resourcePath));
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
            submissionDao.insertSubmissionStatus(selectedSubmission.getId(), userId, false);
            addMessage(FacesMessage.SEVERITY_INFO, "Changes_saved");
        }
    }

    public void lockSubmission() {
        if (selectedSubmission != null) {
            selectedSubmission.setSubmitted(true);
            submissionDao.insertSubmissionStatus(selectedSubmission.getId(), userId, true);
            addMessage(FacesMessage.SEVERITY_INFO, "Changes_saved");
        }
    }

    /* -------- Methods below are used for the admin users submissions page ---------*/
    // TODO @astappiev/@hulyi: needs to be refactored. Has to be discussed first. Either move to other bean or integrate into Moderator user list or ....

    /**
     * To display the users for a particular course and the corresponding number of submissions.
     * on the admin/users_submissions page
     */
    public List<User> getUsers() {
        if (users == null && courseId != 0) {
            users = courseDao.findByIdOrElseThrow(courseId).getMembers();
            userSubmissions = submissionDao.countPerUserByCourseId(courseId);
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
