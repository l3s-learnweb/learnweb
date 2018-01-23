package de.l3s.learnweb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * 
 * @author Trevor
 */
public class SubmissionManager
{
    private final static String SUBMIT_RESOURCE_COLUMNS = "`submission_id`, `resource_id`, `user_id`";

    public static Logger log = Logger.getLogger(SurveyManager.class);
    private final Learnweb learnweb;

    public SubmissionManager(Learnweb learnweb)
    {
        this.learnweb = learnweb;
    }

    public ArrayList<Submission> getSubmissionsByCourse(int courseId)
    {
        ArrayList<Submission> submissions = new ArrayList<Submission>();
        try
        {
            PreparedStatement ps = learnweb.getConnection().prepareStatement("SELECT * FROM lw_submit WHERE course_id=? AND deleted=0 ORDER BY close_datetime");
            ps.setInt(1, courseId);
            ResultSet rs = ps.executeQuery();
            while(rs.next())
            {
                Submission s = new Submission();
                s.setId(rs.getInt("submission_id"));
                s.setCourseId(courseId);
                s.setTitle(rs.getString("title"));
                s.setDescription(rs.getString("description"));
                s.setOpenDatetime(rs.getDate("open_datetime"));
                s.setCloseDatetime(rs.getDate("close_datetime"));
                s.setNoOfResources(rs.getInt("number_of_resources"));
                s.setSurveyResourceId(rs.getInt("survey_resource_id"));

                submissions.add(s);
            }
        }
        catch(SQLException e)
        {
            log.error("Error while retrieving submissions by course", e);
        }

        return submissions;
    }

    public ArrayList<Submission> getSubmissionsByUser(User user)
    {
        ArrayList<Submission> submissions = new ArrayList<Submission>();

        try
        {
            List<Course> courses = user.getCourses();
            StringBuilder builder = new StringBuilder();

            for(int i = 0; i < courses.size(); i++)
            {
                builder.append("?,");
            }

            String pStmt = "SELECT * FROM lw_submit WHERE course_id IN (" + builder.deleteCharAt(builder.length() - 1).toString() + ") AND deleted=0 ORDER BY close_datetime";

            PreparedStatement ps = learnweb.getConnection().prepareStatement(pStmt);
            int index = 1;
            for(Course course : courses)
            {
                ps.setInt(index++, course.getId());
            }

            ResultSet rs = ps.executeQuery();
            while(rs.next())
            {
                Submission s = new Submission();
                int submissionId = rs.getInt("submission_id");
                s.setId(submissionId);
                s.setCourseId(rs.getInt("course_id"));
                s.setTitle(rs.getString("title"));
                s.setDescription(rs.getString("description"));
                s.setOpenDatetime(rs.getDate("open_datetime"));
                s.setCloseDatetime(rs.getDate("close_datetime"));
                s.setNoOfResources(rs.getInt("number_of_resources"));
                s.setSurveyResourceId(rs.getInt("survey_resource_id"));
                s.setSubmittedResources(getResourcesByIdAndUserId(submissionId, user.getId()));

                submissions.add(s);
            }
        }
        catch(SQLException e)
        {
            log.error("Error while retrieving submissions by course", e);
        }

        return submissions;
    }

    public ArrayList<Submission> getActiveSubmissionsByUser(User user)
    {
        ArrayList<Submission> submissions = new ArrayList<Submission>();
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
                Submission s = new Submission();
                s.setId(rs.getInt("submission_id"));
                s.setCourseId(rs.getInt("course_id"));
                s.setTitle(rs.getString("title"));
                s.setDescription(rs.getString("description"));
                s.setOpenDatetime(rs.getDate("open_datetime"));
                s.setCloseDatetime(rs.getDate("close_datetime"));
                s.setNoOfResources(rs.getInt("number_of_resources"));
                s.setSurveyResourceId(rs.getInt("survey_resource_id"));

                submissions.add(s);
            }
        }
        catch(SQLException e)
        {
            log.error("Error while retrieving submissions by course", e);
        }

        return submissions;
    }

    public Submission getSubmissionById(int submissionId)
    {
        Submission s = new Submission();
        try
        {
            PreparedStatement ps = learnweb.getConnection().prepareStatement("SELECT * FROM lw_submit WHERE submission_id=?");
            ps.setInt(1, submissionId);
            ResultSet rs = ps.executeQuery();
            while(rs.next())
            {
                s.setId(rs.getInt("submission_id"));
                s.setCourseId(rs.getInt("course_id"));
                s.setTitle(rs.getString("title"));
                s.setDescription(rs.getString("description"));
                s.setOpenDatetime(rs.getDate("open_datetime"));
                s.setCloseDatetime(rs.getDate("close_datetime"));
                s.setNoOfResources(rs.getInt("number_of_resources"));
                s.setSurveyResourceId(rs.getInt("survey_resource_id"));
            }
        }
        catch(SQLException e)
        {
            log.error("Error while retrieving submission by id for id: " + submissionId, e);
        }

        return s;
    }

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
            replace.setDate(6, new java.sql.Date(submission.getCloseDatetime().getTime()));
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

    public List<Resource> getResourcesByIdAndUserId(int submissionId, int userId)
    {
        List<Resource> submittedResources = new ArrayList<Resource>();
        try
        {
            PreparedStatement ps = learnweb.getConnection().prepareStatement("SELECT resource_id FROM lw_submit_resource WHERE submission_id = ? AND user_id = ?");
            ps.setInt(1, submissionId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            while(rs.next())
            {
                int resourceId = rs.getInt("resource_id");
                Resource r = learnweb.getResourceManager().getResource(resourceId);
                submittedResources.add(r);
            }
        }
        catch(SQLException e)
        {
            log.error("Error while retrieving submitted resources for submission id: " + submissionId, e);
        }
        return submittedResources;
    }

    public HashMap<Integer, Integer> getUsersSubmissionsByCourseId(int courseId)
    {
        HashMap<Integer, Integer> usersSubmissions = new HashMap<Integer, Integer>();
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
}
