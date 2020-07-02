package de.l3s.learnweb.resource.submission;

import java.security.InvalidParameterException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceManager;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserManager;
import de.l3s.util.HasId;

/**
 * DAO for submissions pages.
 *
 * @author Trevor
 */
public class SubmissionManager {
    private static final Logger log = LogManager.getLogger(SubmissionManager.class);

    private static final String SUBMISSION_RESOURCE_COLUMNS = "`submission_id`, `resource_id`, `user_id`";
    public static final int SUBMISSION_ADMIN_USER_ID = 11212;

    private final Learnweb learnweb;

    public SubmissionManager(Learnweb learnweb) {
        this.learnweb = learnweb;
    }

    /**
     * Get all submissions for a particular user across all their courses.
     */
    public ArrayList<Submission> getSubmissionsByUser(User user) throws SQLException {
        ArrayList<Submission> submissions = new ArrayList<>();

        try (PreparedStatement ps = learnweb.getConnection().prepareStatement("SELECT * FROM lw_submission WHERE course_id IN (" + HasId.implodeIds(user.getCourses()) + ") AND deleted=0 ORDER BY close_datetime")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Submission s = createSubmission(rs);
                s.setSubmittedResources(getResourcesByIdAndUserId(s.getId(), user.getId()));
                s.setSubmitted(getSubmitStatusForUser(s.getId(), user.getId()));
                submissions.add(s);
            }
        }

        return submissions;
    }

    /**
     * Retrieves current submissions for a user to be displayed in the homepage.
     */
    public ArrayList<Submission> getActiveSubmissionsByUser(User user) throws SQLException {
        ArrayList<Submission> submissions = new ArrayList<>(1); // most users will have zero or at most one active submission

        try (PreparedStatement ps = learnweb.getConnection().prepareStatement("SELECT * FROM lw_submission WHERE course_id IN (" + HasId.implodeIds(user.getCourses()) + ") AND close_datetime >= NOW() AND open_datetime < NOW() AND deleted=0 ORDER BY close_datetime")) {

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                submissions.add(createSubmission(rs));
            }
        }

        return submissions;
    }

    /**
     * Get details of a particular submission: required for submission_resources.jsf.
     */
    public Submission getSubmissionById(int submissionId) throws SQLException {
        try (PreparedStatement ps = learnweb.getConnection().prepareStatement("SELECT * FROM lw_submission WHERE submission_id=?")) {
            ps.setInt(1, submissionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return createSubmission(rs);
            }
        }

        throw new InvalidParameterException("unknown submissionId: " + submissionId);
    }

    /**
     * Saving a resource for a particular submission after the user submits.
     */
    public void saveSubmissionResource(int submissionId, int resourceId, int userId) {
        try (PreparedStatement ps = learnweb.getConnection().prepareStatement("INSERT INTO lw_submission_resource(" + SUBMISSION_RESOURCE_COLUMNS + ") VALUES (?, ?, ?)")) {
            ps.setInt(1, submissionId);
            ps.setInt(2, resourceId);
            ps.setInt(3, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error while saving resource " + resourceId + " for submission id: " + submissionId, e);
        }
    }

    /**
     * To be able to remove submitted resource if the submission is re-opened by the moderator.
     */
    public void deleteSubmissionResource(int submissionId, int resourceId, int userId) {
        try (PreparedStatement ps = learnweb.getConnection().prepareStatement("DELETE FROM lw_submission_resource WHERE submission_id=? AND resource_id=? AND user_id=?")) {
            ps.setInt(1, submissionId);
            ps.setInt(2, resourceId);
            ps.setInt(3, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error while saving resource " + resourceId + " for submission id: " + submissionId, e);
        }
    }

    public void saveSubmission(Submission submission) {
        try (PreparedStatement replace = learnweb.getConnection().prepareStatement("REPLACE INTO lw_submission(`submission_id`, `course_id`, `title`, `description`, `open_datetime`, `close_datetime`, `number_of_resources`, `survey_resource_id`) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS)) {
            if (submission.getId() < 0) {
                replace.setNull(1, java.sql.Types.INTEGER);
            } else {
                replace.setInt(1, submission.getId());
            }
            replace.setInt(2, submission.getCourseId());
            replace.setString(3, submission.getTitle());
            replace.setString(4, submission.getDescription());
            replace.setDate(5, new java.sql.Date(submission.getOpenDatetime().getTime()));
            replace.setTimestamp(6, new java.sql.Timestamp(submission.getCloseDatetime().getTime()));
            replace.setInt(7, submission.getNoOfResources());
            replace.setInt(8, submission.getSurveyResourceId());
            replace.executeUpdate();
        } catch (SQLException e) {
            log.error("Error while saving edited submission " + submission.getId(), e);
        }
    }

    public void deleteSubmission(int submissionId) {
        try (PreparedStatement update = learnweb.getConnection().prepareStatement("UPDATE lw_submission SET deleted = 1 WHERE submission_id = ?")) {
            update.setInt(1, submissionId);
            update.executeUpdate();
        } catch (SQLException e) {
            log.error("Error while deleting submission " + submissionId, e);
        }
    }

    /**
     * Retrieves submitted resources of a user for a particular submission.
     */
    public List<Resource> getResourcesByIdAndUserId(int submissionId, int userId) throws SQLException {
        ResourceManager resourceManager = learnweb.getResourceManager();
        List<Resource> submittedResources = new ArrayList<>();
        try (PreparedStatement ps = learnweb.getConnection().prepareStatement("SELECT resource_id FROM lw_submission_resource WHERE submission_id = ? AND user_id = ?")) {
            ps.setInt(1, submissionId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Resource r = resourceManager.getResource(rs.getInt("resource_id"));
                submittedResources.add(r);
            }
        }
        return submittedResources;
    }

    /**
     * Retrieve the number of submissions by the user for a particular course
     * to display it in the admin/users_submissions page.
     */
    public HashMap<Integer, Integer> getUsersSubmissionsByCourseId(int courseId) {
        HashMap<Integer, Integer> usersSubmissions = new HashMap<>();
        try (PreparedStatement ps = learnweb.getConnection()
            .prepareStatement("SELECT t1.user_id, COUNT(*) as count FROM (SELECT DISTINCT submission_id, user_id FROM lw_submission_resource) t1 JOIN lw_submission t2 USING(submission_id) WHERE course_id = ? AND deleted=0 GROUP BY user_id")) {
            ps.setInt(1, courseId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                usersSubmissions.put(rs.getInt("user_id"), rs.getInt("count"));
            }
        } catch (SQLException e) {
            log.error("Error while retrieving number of submissions for users of course id: " + courseId, e);
        }
        return usersSubmissions;
    }

    public boolean getSubmitStatusForUser(int submissionId, int userId) throws SQLException {
        boolean submitted = false;
        try (PreparedStatement ps = learnweb.getConnection().prepareStatement("SELECT submitted FROM lw_submission_status WHERE submission_id = ? AND user_id = ?")) {
            ps.setInt(1, submissionId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                submitted = (rs.getInt(1) == 1);
            }
        }
        return submitted;
    }

    public void saveSubmitStatusForUser(int submissionId, int userId, boolean submitted) {
        // TODO @astappiev/@hulyi: to keep survey_resource_id use:
        // INSERT INTO lw_submission_status(submission_id, user_id, submitted) VALUES (?,?,?) ON DUPLICATE KEY UPDATE submitted = ?;
        try (PreparedStatement ps = learnweb.getConnection().prepareStatement("REPLACE INTO lw_submission_status(submission_id, user_id, submitted) VALUES (?,?,?)")) {
            ps.setInt(1, submissionId);
            ps.setInt(2, userId);
            ps.setInt(3, submitted ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Error while inserting submit status for submission: " + submissionId + "; user: " + userId, e);
        }
    }

    private Submission createSubmission(ResultSet rs) throws SQLException {
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
     * Return all resources submitted for this submission form grouped by user.
     */
    public List<SubmittedResources> getSubmittedResourcesGroupedByUser(int submissionId) throws SQLException {
        ResourceManager resourceManager = learnweb.getResourceManager();
        UserManager userManager = learnweb.getUserManager();

        List<SubmittedResources> submittedResourcesPerUser = new ArrayList<>();
        SubmittedResources currentUserSubmission = null;

        try (PreparedStatement ps = learnweb.getConnection().prepareStatement("SELECT resource_id, user_id FROM lw_submission_resource WHERE submission_id = ? ORDER BY user_id")) {
            ps.setInt(1, submissionId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int userId = rs.getInt("user_id");

                if (currentUserSubmission == null || userId != currentUserSubmission.getUserId()) { // we have reached the entries of another user
                    if (currentUserSubmission != null) { // if not first entry finish the last entry set
                        submittedResourcesPerUser.add(currentUserSubmission);
                    }

                    //start new user entry set
                    currentUserSubmission = new SubmittedResources(userManager.getUser(userId), -1, getSubmitStatusForUser(submissionId, userId));
                }

                Resource resource = resourceManager.getResource(rs.getInt("resource_id"));
                currentUserSubmission.addResource(resource);
            }
            if (currentUserSubmission != null) { // add last entry set to list
                submittedResourcesPerUser.add(currentUserSubmission);
            }
        }

        return submittedResourcesPerUser;
    }
}
