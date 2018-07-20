package de.l3s.learnweb.resource.submission;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceManager;
import de.l3s.learnweb.resource.survey.SurveyManager;
import de.l3s.learnweb.user.Course;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserManager;

/**
 * DAO for submissions pages
 *
 * @author Trevor
 */
public class SubmissionManager
{
    private final static String SUBMIT_RESOURCE_COLUMNS = "`submission_id`, `resource_id`, `user_id`";
    private final static Logger log = Logger.getLogger(SurveyManager.class);
    public final static int SUBMISSION_ADMIN_USER_ID = 11212;

    private final Learnweb learnweb;

    public SubmissionManager(Learnweb learnweb)
    {
        this.learnweb = learnweb;
    }

    public ArrayList<Submission> getSubmissionsByCourse(int courseId) throws SQLException
    {
        ArrayList<Submission> submissions = new ArrayList<>();
        try(PreparedStatement ps = learnweb.getConnection().prepareStatement("SELECT * FROM lw_submit WHERE course_id=? AND deleted=0 ORDER BY close_datetime");)
        {
            ps.setInt(1, courseId);
            ResultSet rs = ps.executeQuery();
            while(rs.next())
            {
                Submission s = createSubmission(rs);
                submissions.add(s);
            }
        }

        return submissions;
    }

    /**
     * Get all submissions for a particular user across all their courses
     *
     * @param user
     * @return
     * @throws SQLException
     */
    public ArrayList<Submission> getSubmissionsByUser(User user) throws SQLException
    {
        ArrayList<Submission> submissions = new ArrayList<>();

        List<Course> courses = user.getCourses();
        StringBuilder builder = new StringBuilder();

        for(int i = 0; i < courses.size(); i++)
        {
            builder.append("?,");
        }

        String pStmt = "SELECT * FROM lw_submit WHERE course_id IN (" + builder.deleteCharAt(builder.length() - 1).toString() + ") AND deleted=0 ORDER BY close_datetime";

        try(PreparedStatement ps = learnweb.getConnection().prepareStatement(pStmt);)
        {
            int index = 1;
            for(Course course : courses)
            {
                ps.setInt(index++, course.getId());
            }

            ResultSet rs = ps.executeQuery();
            while(rs.next())
            {
                int submissionId = rs.getInt("submission_id");
                Submission s = createSubmission(rs);
                s.setSubmittedResources(getResourcesByIdAndUserId(submissionId, user.getId()));
                s.setSubmitted(getSubmitStatusForUser(submissionId, user.getId()));
                submissions.add(s);
            }
        }

        return submissions;
    }

    /**
     * Retrieves current submissions for a user to be displayed in the homepage
     *
     * @param user
     * @return
     */
    public ArrayList<Submission> getActiveSubmissionsByUser(User user)
    {
        ArrayList<Submission> submissions = new ArrayList<>();
        try
        {
            List<Course> courses = user.getCourses();
            StringBuilder builder = new StringBuilder();

            for(int i = 0; i < courses.size(); i++)
            {
                builder.append("?,");
            }

            String pStmt = "SELECT * FROM lw_submit WHERE course_id IN (" + builder.deleteCharAt(builder.length() - 1).toString() + ") AND close_datetime >= NOW() AND open_datetime < NOW() AND deleted=0 ORDER BY close_datetime";

            PreparedStatement ps = learnweb.getConnection().prepareStatement(pStmt);
            int index = 1;
            for(Course course : courses)
            {
                ps.setInt(index++, course.getId());
            }

            ResultSet rs = ps.executeQuery();
            while(rs.next())
            {
                Submission s = createSubmission(rs);
                submissions.add(s);
            }
        }
        catch(SQLException e)
        {
            log.error("Error while retrieving submissions by course", e);
        }

        return submissions;
    }

    /**
     * Get details of a particular submission: required for submission_resources.jsf
     *
     * @param submissionId
     * @return
     * @throws SQLException
     */
    public Submission getSubmissionById(int submissionId) throws SQLException
    {
        Submission s = new Submission();
        try(PreparedStatement ps = learnweb.getConnection().prepareStatement("SELECT * FROM lw_submit WHERE submission_id=?");)
        {
            ps.setInt(1, submissionId);
            ResultSet rs = ps.executeQuery();
            while(rs.next())
            {
                s = createSubmission(rs);
            }
        }

        return s;
    }

    /**
     * Saving a resource for a particular submission after the user submits
     *
     * @param submissionId
     * @param resourceId
     * @param userId
     */
    public void saveSubmissionResource(int submissionId, int resourceId, int userId)
    {
        try
        {
            PreparedStatement ps = learnweb.getConnection().prepareStatement("INSERT INTO lw_submit_resource(" + SUBMIT_RESOURCE_COLUMNS + ") VALUES (?, ?, ?)");
            ps.setInt(1, submissionId);
            ps.setInt(2, resourceId);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
        catch(SQLException e)
        {
            log.error("Error while saving resource " + resourceId + " for submission id: " + submissionId, e);
        }
    }

    /**
     * To be able to remove submitted resource if the submission is re-opened
     * by the moderator
     *
     * @param submissionId
     * @param resourceId
     * @param userId
     */
    public void deleteSubmissionResource(int submissionId, int resourceId, int userId)
    {
        try
        {
            PreparedStatement ps = learnweb.getConnection().prepareStatement("DELETE FROM lw_submit_resource WHERE submission_id=? AND resource_id=? AND user_id=?");
            ps.setInt(1, submissionId);
            ps.setInt(2, resourceId);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
        catch(SQLException e)
        {
            log.error("Error while saving resource " + resourceId + " for submission id: " + submissionId, e);
        }
    }

    public void saveSubmission(Submission submission)
    {
        try
        {
            String query = "REPLACE INTO lw_submit(`submission_id`, `course_id`, `title`, `description`, `open_datetime`, `close_datetime`, `number_of_resources`, `survey_resource_id`) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement replace = learnweb.getConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

            if(submission.getId() < 0)
                replace.setNull(1, java.sql.Types.INTEGER);
            else
                replace.setInt(1, submission.getId());
            replace.setInt(2, submission.getCourseId());
            replace.setString(3, submission.getTitle());
            replace.setString(4, submission.getDescription());
            replace.setDate(5, new java.sql.Date(submission.getOpenDatetime().getTime()));
            replace.setTimestamp(6, new java.sql.Timestamp(submission.getCloseDatetime().getTime()));
            replace.setInt(7, submission.getNoOfResources());
            replace.setInt(8, submission.getSurveyResourceId());
            replace.executeUpdate();
        }
        catch(SQLException e)
        {
            log.error("Error while saving edited submission " + submission.getId(), e);
        }
    }

    public void deleteSubmission(int submissionId)
    {
        try
        {
            String query = "UPDATE lw_submit SET deleted = 1 WHERE submission_id = ?";
            PreparedStatement update = learnweb.getConnection().prepareStatement(query);
            update.setInt(1, submissionId);
            update.executeUpdate();
            update.close();
        }
        catch(SQLException e)
        {
            log.error("Error while deleting submission " + submissionId, e);
        }
    }

    /**
     * Retrieves submitted resources of a user for a particular submission
     *
     * @param submissionId
     * @param userId
     * @return
     * @throws SQLException
     */
    public List<Resource> getResourcesByIdAndUserId(int submissionId, int userId) throws SQLException
    {
        ResourceManager resourceManager = learnweb.getResourceManager();
        List<Resource> submittedResources = new ArrayList<>();
        try(PreparedStatement ps = learnweb.getConnection().prepareStatement("SELECT resource_id FROM lw_submit_resource WHERE submission_id = ? AND user_id = ?");)
        {
            ps.setInt(1, submissionId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            while(rs.next())
            {
                int resourceId = rs.getInt("resource_id");
                Resource r = resourceManager.getResource(resourceId);
                submittedResources.add(r);
            }
        }
        return submittedResources;
    }

    /**
     * Retrieve the number of submissions by the user for a particular course
     * to display it in the admin/users_submissions page
     *
     * @param courseId
     * @return
     */
    public HashMap<Integer, Integer> getUsersSubmissionsByCourseId(int courseId)
    {
        HashMap<Integer, Integer> usersSubmissions = new HashMap<>();
        try
        {
            PreparedStatement ps = learnweb.getConnection().prepareStatement("SELECT t1.user_id, COUNT(*) as count FROM (SELECT DISTINCT submission_id, user_id FROM lw_submit_resource) t1 JOIN lw_submit t2 USING(submission_id) WHERE course_id = ? AND deleted=0 GROUP BY user_id");
            ps.setInt(1, courseId);
            ResultSet rs = ps.executeQuery();
            while(rs.next())
            {
                usersSubmissions.put(rs.getInt("user_id"), rs.getInt("count"));
            }
        }
        catch(SQLException e)
        {
            log.error("Error while retrieving number of submissions for users of course id: " + courseId, e);
        }
        return usersSubmissions;
    }

    public boolean getSubmitStatusForUser(int submissionId, int userId) throws SQLException
    {
        boolean submitted = false;
        try(PreparedStatement ps = learnweb.getConnection().prepareStatement("SELECT submitted FROM lw_submit_status WHERE submission_id = ? AND user_id = ?");)
        {
            ps.setInt(1, submissionId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            if(rs.next())
                submitted = (rs.getInt(1) == 1);
        }
        return submitted;
    }

    public void saveSubmitStatusForUser(int submissionId, int userId, boolean submitted)
    {
        //  TODO to keep survey_resource_id use:
        // INSERT INTO lw_submit_status(submission_id, user_id, submitted) VALUES (?,?,?) ON DUPLICATE KEY UPDATE submitted = ?;
        try(PreparedStatement ps = learnweb.getConnection().prepareStatement("REPLACE INTO lw_submit_status(submission_id, user_id, submitted) VALUES (?,?,?)");)
        {
            ps.setInt(1, submissionId);
            ps.setInt(2, userId);
            ps.setInt(3, submitted ? 1 : 0);
            ps.executeUpdate();
        }
        catch(SQLException e)
        {
            log.error("Error while inserting submit status for submission: " + submissionId + "; user: " + userId, e);
        }
    }

    private Submission createSubmission(ResultSet rs) throws SQLException
    {
        Submission s = new Submission();
        s.setId(rs.getInt("submission_id"));
        s.setCourseId(rs.getInt("course_id"));
        s.setTitle(rs.getString("title"));
        s.setDescription(rs.getString("description"));
        s.setOpenDatetime(rs.getDate("open_datetime"));
        s.setCloseDatetime(rs.getDate("close_datetime"));
        s.setNoOfResources(rs.getInt("number_of_resources"));
        s.setSurveyResourceId(rs.getInt("survey_resource_id"));

        return s;
    }

    /**
     * Return all resources submitted for this submission form grouped by user
     *
     * @param submissionId
     * @return
     * @throws SQLException
     */
    public List<SubmittedResources> getSubmittedResourcesGroupedByUser(int submissionId) throws SQLException
    {
        ResourceManager resourceManager = learnweb.getResourceManager();
        UserManager userManager = learnweb.getUserManager();

        List<SubmittedResources> submittedResourcesPerUser = new ArrayList<>();
        SubmittedResources currentUserSubmission = null;

        try(PreparedStatement ps = learnweb.getConnection().prepareStatement("SELECT resource_id, user_id FROM lw_submit_resource WHERE submission_id = ? ORDER BY user_id");)
        {
            ps.setInt(1, submissionId);
            ResultSet rs = ps.executeQuery();
            while(rs.next())
            {
                int userId = rs.getInt("user_id");

                if(currentUserSubmission == null || userId != currentUserSubmission.getUserId()) // we have reached the entries of another user
                {
                    if(currentUserSubmission != null) // if not first entry finish the last entry set
                        submittedResourcesPerUser.add(currentUserSubmission);

                    //start new user entry set
                    currentUserSubmission = new SubmittedResources(userManager.getUser(userId), -1, getSubmitStatusForUser(submissionId, userId));
                }

                Resource resource = resourceManager.getResource(rs.getInt("resource_id"));
                currentUserSubmission.addResource(resource);
            }
            if(currentUserSubmission != null) // add last entry set to list
                submittedResourcesPerUser.add(currentUserSubmission);
        }

        return submittedResourcesPerUser;
    }

    /**
     * This class bundles the submitted resource of one particular user for one submission
     *
     * @author Philipp
     *
     */
    public class SubmittedResources implements Serializable
    {
        private static final long serialVersionUID = -7336342838037456587L;

        private int userId;
        private transient User user;
        private List<Resource> resources = new ArrayList<>(); // the submitted resources
        private int surveyResourceId = -1; // the survey that was used to grade this submission
        private boolean submitStatus = false; // keeps track of the submit status, so as to lock/unlock a submission

        public SubmittedResources(User user, int surveyResourceId, boolean submitStatus)
        {
            super();
            this.user = user;
            this.userId = user.getId();
            this.surveyResourceId = surveyResourceId;
            this.submitStatus = submitStatus;
        }

        public User getUser()
        {
            if(null == user)
            {
                try
                {
                    user = Learnweb.getInstance().getUserManager().getUser(userId);
                }
                catch(SQLException e)
                {
                    log.error("Can't get user " + userId, e);
                }
            }
            return user;
        }

        public int getUserId()
        {
            return userId;
        }

        private void addResource(Resource resource)
        {
            resources.add(resource);
        }

        public List<Resource> getResources()
        {
            return resources;
        }

        /**
         * The survey that grades this submission
         *
         * @return
         */
        public int getSurveyResourceId()
        {
            return surveyResourceId;
        }

        public boolean getSubmitStatus()
        {
            return submitStatus;
        }

        public void setSubmitStatus(boolean submitStatus)
        {
            this.submitStatus = submitStatus;
        }
    }
}
