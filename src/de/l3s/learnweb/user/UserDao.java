package de.l3s.learnweb.user;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.CreateSqlObject;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import de.l3s.learnweb.exceptions.NotFoundHttpException;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.LogDao;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDao;
import de.l3s.util.Cache;
import de.l3s.util.HashHelper;
import de.l3s.util.ICache;
import de.l3s.util.SqlHelper;

@RegisterRowMapper(UserDao.UserMapper.class)
public interface UserDao extends SqlObject, Serializable {
    ICache<User> cache = new Cache<>(500);

    @CreateSqlObject
    ResourceDao getResourceDao();

    default Optional<User> findById(int userId) {
        return Optional.ofNullable(cache.get(userId))
            .or(() -> getHandle().select("SELECT * FROM lw_user WHERE user_id = ?", userId).mapTo(User.class).findOne());
    }

    default User findByIdOrElseThrow(int userId) {
        return findById(userId).orElseThrow(() -> new NotFoundHttpException("error_pages.not_found_object_description"));
    }

    @SqlQuery("SELECT * FROM lw_user WHERE username = ? AND deleted = 0")
    Optional<User> findByUsername(String username);

    default Optional<User> findByUsernameAndPassword(String username, String password) {
        Optional<User> user = findByUsername(username);

        if (user.isPresent() && user.get().validatePassword(password)) {
            // update pw to use new hashing algorithm
            if (user.get().getHashing() != User.PasswordHashing.PBKDF2) {
                user.get().setPassword(password);
                save(user.get());
            }

            return user;
        }

        return Optional.empty();
    }

    @SqlQuery("SELECT * FROM lw_user WHERE email = ?")
    List<User> findByEmail(String email);

    @SqlQuery("SELECT * FROM lw_user WHERE deleted = 0 ORDER BY username")
    List<User> findAll();

    @SqlQuery("SELECT * FROM lw_user WHERE deleted = 0 AND preferred_notification_frequency != 'NEVER'")
    List<User> findWithEnabledForumNotifications();

    @SqlQuery("SELECT * FROM lw_user WHERE organisation_id = ? AND deleted = 0 ORDER BY username")
    List<User> findByOrganisationId(int organisationId);

    @SqlQuery("SELECT u.* FROM lw_user u JOIN lw_course_user USING(user_id) WHERE course_id = ? AND deleted = 0 ORDER BY username")
    List<User> findByCourseId(int courseId);

    @SqlQuery("SELECT u.* FROM lw_user u JOIN lw_group_user USING(user_id) WHERE group_id = ? AND deleted = 0 ORDER BY username")
    List<User> findByGroupId(int groupId);

    @SqlQuery("SELECT * FROM lw_user JOIN lw_group_user USING(user_id) WHERE group_id = ? AND deleted = 0 ORDER BY join_time LIMIT ?")
    List<User> findByGroupIdLastJoined(int groupId, int limit);

    /**
     * @return All users who have saved the survey at least once
     */
    @SqlQuery("SELECT * FROM lw_user WHERE user_id IN (SELECT DISTINCT user_id FROM lw_survey_answer WHERE resource_id = ?)")
    List<User> findBySavedSurveyResourceId(int surveyResourceId);

    @SqlQuery("SELECT * FROM lw_user WHERE user_id IN (SELECT user_id FROM lw_survey_resource_user WHERE resource_id = ? AND submitted = 1)")
    List<User> findBySubmittedSurveyResourceId(int surveyResourceId);

    /**
     * @return the Instant of the last recorded login event of the given user. Empty if the user has never logged in
     */
    default Optional<LocalDateTime> findLastLoginDate(int userId) {
        return getHandle().attach(LogDao.class).findDateOfLastByUserIdAndAction(userId, Action.login.ordinal());
    }

    @SqlQuery("SELECT COUNT(*) FROM lw_user u JOIN lw_course_user USING(user_id) WHERE course_id = ? AND deleted = 0")
    int countByCourseId(int courseId);

    @SqlQuery("SELECT COUNT(*) FROM lw_course_user WHERE user_id = ?")
    int countCoursesByUserId(int userId);

    /**
     * <ul>
     * <li>The user is removed from all his groups.
     * <li>His private resources are deleted
     * <li>His name and password are changed so that he can't login
     * </ul>
     */
    default void deleteSoft(User user) {
        for (Resource resource : user.getResources()) {
            if (resource.getGroupId() == 0) { // delete only private resources
                getResourceDao().deleteSoft(resource);
            }
        }

        getHandle().execute("DELETE FROM lw_group_user WHERE user_id = ?", user);

        user.setDeleted(true);
        user.setPassword(null);
        user.setEmailRaw(HashHelper.sha512(user.getEmail()));
        // alter username so that it is unlikely to cause conflicts with other usernames
        user.setUsername(user.getRealUsername() + " (Deleted) " + user.getId() + " - " + RandomUtils.nextInt(1, 100));
        save(user);

        cache.remove(user.getId());
    }

    default void deleteHard(User user) {
        // FIXME: delete restricted if the user used as leader of a group

        for (Resource resource : user.getResources()) {
            getResourceDao().deleteHard(resource);
        }

        getHandle().execute("DELETE FROM lw_user WHERE user_id = ?", user);
        cache.remove(user.getId());
    }

    default void save(User user) {
        // verify that the given obj is valid; added only attributes that had already caused problems in the past
        Objects.requireNonNull(user.getRealUsername());

        if (user.getCreatedAt() == null) {
            user.setCreatedAt(SqlHelper.now());
        }

        // for new users double check that the username is free. If not the existing user will be overwritten
        if (user.getId() == 0 && findByUsername(user.getRealUsername()).isPresent()) {
            throw new IllegalArgumentException("Username is already taken");
        }

        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("user_id", SqlHelper.toNullable(user.getId()));
        params.put("username", user.getUsername());
        params.put("email", SqlHelper.toNullable(user.getEmail()));
        params.put("email_confirmed", user.isEmailConfirmed());
        params.put("organisation_id", user.getOrganisationId());
        params.put("image_file_id", SqlHelper.toNullable(user.getImageFileId()));
        params.put("gender", user.getGender().ordinal());
        params.put("birthdate", user.getDateOfBirth());
        params.put("address", SqlHelper.toNullable(user.getAddress()));
        params.put("profession", SqlHelper.toNullable(user.getProfession()));
        params.put("interest", SqlHelper.toNullable(user.getInterest()));
        params.put("student_identifier", SqlHelper.toNullable(user.getStudentId()));
        params.put("is_admin", user.isAdmin());
        params.put("is_moderator", user.isModerator());
        params.put("created_at", user.getCreatedAt());
        params.put("password", user.getPassword());
        params.put("hashing", user.getHashing().name());
        params.put("preferences", SerializationUtils.serialize(user.getPreferences()));
        params.put("credits", SqlHelper.toNullable(user.getCredits()));
        params.put("fullname", SqlHelper.toNullable(user.getFullName()));
        params.put("affiliation", SqlHelper.toNullable(user.getAffiliation()));
        params.put("accept_terms_and_conditions", user.isAcceptTermsAndConditions());
        params.put("deleted", user.isDeleted());
        params.put("preferred_notification_frequency", user.getPreferredNotificationFrequency().toString());
        params.put("time_zone", user.getTimeZone().getId());
        params.put("language", user.getLocale().toString());
        SqlHelper.setBitSet(params, "guide_field", user.getGuides());

        Optional<Integer> userId = SqlHelper.handleSave(getHandle(), "lw_user", params)
            .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

        if (userId.isPresent() && userId.get() != 0) {
            user.setId(userId.get());
            cache.put(user);
        }
    }

    default void anonymize(User user) {
        user.setAddress("");
        user.setAffiliation("");
        user.setDateOfBirth(null);
        user.setFullName("");
        user.setImageFileId(0);
        user.setInterest("");
        user.setProfession("");
        user.setStudentId("");
        user.setUsername("Anonym " + user.getId());
        user.setEmailRaw(HashHelper.sha512(user.getEmail()));
        save(user);
    }

    class UserMapper implements RowMapper<User> {
        @Override
        public User map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            User user = cache.get(rs.getInt("user_id"));

            if (user == null) {
                user = new User();
                user.setId(rs.getInt("user_id"));
                user.setDeleted(rs.getBoolean("deleted"));
                user.setUsername(rs.getString("username"));
                user.setEmailRaw(rs.getString("email"));
                user.setEmailConfirmed(rs.getBoolean("email_confirmed"));
                user.setPasswordRaw(rs.getString("password"));
                user.setHashing(rs.getString("hashing"));
                user.setOrganisationId(rs.getInt("organisation_id"));
                user.setImageFileId(rs.getInt("image_file_id"));
                user.setGender(User.Gender.values()[rs.getInt("gender")]);
                user.setDateOfBirth(SqlHelper.getLocalDate(rs.getDate("birthdate")));
                user.setFullName(rs.getString("fullname"));
                user.setAffiliation(rs.getString("affiliation"));
                user.setAddress(rs.getString("address"));
                user.setProfession(rs.getString("profession"));
                user.setInterest(rs.getString("interest"));
                user.setStudentId(rs.getString("student_identifier"));
                user.setCreatedAt(SqlHelper.getLocalDateTime(rs.getTimestamp("created_at")));
                user.setCredits(rs.getString("credits"));
                user.setAcceptTermsAndConditions(rs.getBoolean("accept_terms_and_conditions"));
                user.setPreferredNotificationFrequency(User.NotificationFrequency.valueOf(rs.getString("preferred_notification_frequency")));
                user.setGuides(SqlHelper.getBitSet(rs, "guide_field", User.Guide.values().length));
                user.setTimeZone(ZoneId.of(rs.getString("time_zone")));
                user.setLocale(Locale.forLanguageTag(rs.getString("language").replace("_", "-")));
                user.setAdmin(rs.getBoolean("is_admin"));
                user.setModerator(rs.getBoolean("is_moderator"));
                user.setPreferences(SqlHelper.deserializeHashMap(rs.getBytes("preferences")));
                cache.put(user);
            }
            return user;
        }
    }
}
