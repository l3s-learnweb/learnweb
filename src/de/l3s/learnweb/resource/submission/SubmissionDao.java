package de.l3s.learnweb.resource.submission;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.result.RowView;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.config.ValueColumn;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDao;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserDao;
import de.l3s.util.HasId;
import de.l3s.util.SqlHelper;

@RegisterRowMapper(SubmissionDao.SubmissionMapper.class)
public interface SubmissionDao extends SqlObject {
    @SqlQuery("SELECT * FROM lw_submission WHERE submission_id=?")
    Optional<Submission> findById(int submissionId);

    @SqlQuery("SELECT * FROM lw_submission WHERE course_id IN (<courseIds>) AND deleted=0 ORDER BY close_datetime")
    List<Submission> findByCourseIds(@BindList("courseIds") List<Integer> courseIds);

    @SqlQuery("SELECT * FROM lw_submission WHERE course_id IN (<courseIds>) AND close_datetime >= NOW() AND open_datetime < NOW() AND deleted=0 ORDER BY close_datetime")
    List<Submission> findActiveByCourseIds(@BindList("courseIds") List<Integer> courseIds);

    @SqlQuery("SELECT * FROM lw_submission JOIN lw_resource USING(resource_id) WHERE user_id IN (<userIds>) AND deleted = 0")
    List<Submission> findByUserIds(@BindList("userIds") Collection<Integer> userIds);

    @SqlQuery("SELECT submitted FROM lw_submission_status WHERE submission_id = ? AND user_id = ?")
    Optional<Boolean> findStatus(int submissionId, int userId);

    /**
     * Retrieve the number of submissions by the user for a particular course
     * to display it in the admin/users_submissions page.
     */
    @SqlQuery("SELECT t1.user_id, COUNT(*) as count FROM (SELECT DISTINCT submission_id, user_id FROM lw_submission_resource) t1 JOIN lw_submission USING(submission_id) WHERE course_id = ? AND deleted=0 GROUP BY user_id")
    @KeyColumn("user_id")
    @ValueColumn("count")
    Map<Integer, Integer> countPerUserByCourseId(int courseId);

    @SqlQuery("SELECT COUNT(*) FROM lw_submission WHERE course_id IN (<courseIds>) AND deleted=0 ORDER BY close_datetime")
    int countByCourseIds(@BindList("courseIds") List<Integer> courseIds);

    @SqlUpdate("INSERT INTO lw_submission_status(submission_id, user_id, submitted) VALUES (?,?,?) ON DUPLICATE KEY UPDATE submitted = VALUES(submitted)")
    void insertSubmissionStatus(int submissionId, int userId, boolean submitted);

    /**
     * Saving a resource for a particular submission after the user submits.
     */
    @SqlUpdate("INSERT INTO lw_submission_resource(`submission_id`, `resource_id`, `user_id`) VALUES (?, ?, ?)")
    void insertSubmissionResource(int submissionId, int resourceId, int userId);

    @SqlUpdate("UPDATE lw_submission SET deleted = 1 WHERE submission_id = ?")
    void deleteSoft(Submission submission);

    /**
     * To be able to remove submitted resource if the submission is re-opened by the moderator.
     */
    @SqlUpdate("DELETE FROM lw_submission_resource WHERE submission_id=? AND resource_id=? AND user_id=?")
    void deleteSubmissionResource(int submissionId, int resourceId, int userId);

    default List<Submission> findByUser(User user) {
        List<Submission> submissions = findByCourseIds(HasId.collectIds(user.getCourses()));

        ResourceDao resourceDao = getHandle().attach(ResourceDao.class);
        submissions.forEach(submission -> {
            List<Resource> submissionResources = resourceDao.findBySubmissionIdAndUserId(submission.getId(), user.getId());
            boolean submissionStatus = findStatus(submission.getId(), user.getId()).orElse(false);

            submission.setSubmittedResources(submissionResources);
            submission.setSubmitted(submissionStatus);
        });

        return submissions;
    }

    /**
     * Return all resources submitted for this submission form grouped by user.
     */
    default List<SubmittedResources> findSubmittedResources(int submissionId) {
        UserDao userDao = getHandle().attach(UserDao.class);
        ResourceDao resourceDao = getHandle().attach(ResourceDao.class);

        return getHandle().select("SELECT resource_id, user_id FROM lw_submission_resource WHERE submission_id = ? ORDER BY user_id", submissionId)
            .reduceRows((Map<Integer, SubmittedResources> map, RowView rowView) -> {
                int resourceId = rowView.getColumn("resource_id", Integer.class);
                int userId = rowView.getColumn("user_id", Integer.class);

                SubmittedResources userSubmissions = map.computeIfAbsent(userId, id ->
                    new SubmittedResources(userDao.findById(userId), -1, findStatus(submissionId, userId).orElse(false)));

                userSubmissions.addResource(resourceDao.findById(resourceId));
            }).collect(Collectors.toList());
    }

    default void save(Submission submission) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("submission_id", submission.getId() < 1 ? null : submission.getId());
        params.put("course_id", submission.getCourseId());
        params.put("title", submission.getTitle());
        params.put("description", submission.getDescription());
        params.put("open_datetime", submission.getOpenDatetime());
        params.put("close_datetime", submission.getCloseDatetime());
        params.put("number_of_resources", submission.getNoOfResources());
        params.put("survey_resource_id", submission.getSurveyResourceId());

        Optional<Integer> submissionId = SqlHelper.generateInsertQuery(getHandle(), "lw_submission", params)
            .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

        submissionId.ifPresent(submission::setId);
    }

    class SubmissionMapper implements RowMapper<Submission> {
        @Override
        public Submission map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            Submission submission = new Submission();
            submission.setId(rs.getInt("submission_id"));
            submission.setCourseId(rs.getInt("course_id"));
            submission.setTitle(rs.getString("title"));
            submission.setDescription(rs.getString("description"));
            submission.setOpenDatetime(rs.getDate("open_datetime"));
            submission.setCloseDatetime(rs.getDate("close_datetime"));
            submission.setNoOfResources(rs.getInt("number_of_resources"));
            submission.setSurveyResourceId(rs.getInt("survey_resource_id"));
            return submission;
        }
    }
}
