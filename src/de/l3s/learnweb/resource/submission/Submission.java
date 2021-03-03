package de.l3s.learnweb.resource.submission;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.survey.SurveyResource;
import de.l3s.learnweb.resource.survey.SurveyUserAnswers;
import de.l3s.learnweb.user.Course;
import de.l3s.util.HasId;

public class Submission implements Serializable, HasId {
    private static final long serialVersionUID = -3143872721852606973L;
    private static final Logger log = LogManager.getLogger(Submission.class);

    private int id;
    private int courseId;
    private String title;
    private String description;
    private LocalDateTime openDatetime = LocalDateTime.now();
    private LocalDateTime closeDatetime = LocalDateTime.now();
    private int noOfResources = 3; // Default max no. of resources 3

    // Fields to handle link display based on survey submitted or not
    private Integer surveyResourceId;
    private boolean surveyMandatory = false;
    private boolean submitted = false;
    private List<Resource> submittedResources;

    // caches
    private transient Course course;
    private transient List<SubmittedResources> submittedResourcesGroupedByUser;
    private transient SurveyUserAnswers surveyAnswer;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getOpenDatetime() {
        return openDatetime;
    }

    public void setOpenDatetime(LocalDateTime openDatetime) {
        this.openDatetime = openDatetime;
    }

    public LocalDateTime getCloseDatetime() {
        return closeDatetime;
    }

    public void setCloseDatetime(LocalDateTime closeDatetime) {
        this.closeDatetime = closeDatetime.toLocalDate().atTime(LocalTime.MAX);
    }

    public int getNoOfResources() {
        return noOfResources;
    }

    public void setNoOfResources(int noOfResources) {
        this.noOfResources = noOfResources;
    }

    public void setNoOfResources(String noOfResources) {
        if (noOfResources != null) {
            this.noOfResources = Integer.parseInt(noOfResources);
        }
    }

    public boolean isPastSubmission() {
        return closeDatetime.isBefore(LocalDateTime.now());
    }

    public boolean isCurrentSubmission() {
        return !openDatetime.isAfter(LocalDateTime.now()) && !closeDatetime.isBefore(LocalDateTime.now());
    }

    public boolean isFutureSubmission() {
        return openDatetime.isAfter(LocalDateTime.now());
    }

    public boolean isSurveyMandatory() {
        return surveyMandatory;
    }

    public void setSurveyMandatory(boolean surveyMandatory) {
        this.surveyMandatory = surveyMandatory;
        if (!surveyMandatory) {
            this.surveyResourceId = null;
        }
    }

    public Integer getSurveyResourceId() {
        return surveyResourceId;
    }

    public void setSurveyResourceId(Integer surveyResourceId) {
        this.surveyResourceId = surveyResourceId;
        if (this.surveyResourceId != null) {
            this.surveyMandatory = true;
        }
    }

    public boolean isSurveySubmitted(int userId) {
        if (!surveyMandatory) {
            return true;
        }

        // load surveyAnswer
        if (surveyResourceId != null && surveyAnswer == null) {
            SurveyResource surveyResource = Learnweb.dao().getSurveyDao().findResourceById(surveyResourceId).orElseThrow();
            surveyAnswer = surveyResource.getAnswersOfUser(userId);
        }

        if (surveyAnswer != null) {
            return surveyAnswer.isSubmitted();
        }

        return false;
    }

    /**
     * @return The submitted resource of one particular user
     */
    public List<Resource> getSubmittedResources() {
        return submittedResources;
    }

    public void setSubmittedResources(List<Resource> submittedResources) {
        this.submittedResources = submittedResources;
    }

    /**
     * Return all resources submitted for this submission form grouped by user.
     */
    public List<SubmittedResources> getSubmittedResourcesGroupedByUser() {
        if (submittedResourcesGroupedByUser == null) {
            submittedResourcesGroupedByUser = Learnweb.dao().getSubmissionDao().findSubmittedResources(id);
        }
        return submittedResourcesGroupedByUser;
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public void setSubmitted(boolean submitted) {
        this.submitted = submitted;
    }

    public Course getCourse() {
        if (course == null && courseId != 0) {
            course = Learnweb.dao().getCourseDao().findById(courseId);
        }
        return course;
    }

}
