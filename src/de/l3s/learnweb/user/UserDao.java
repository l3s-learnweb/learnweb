package de.l3s.learnweb.user;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.CreateSqlObject;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

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
    int FIELDS = 1; // number of options_fieldX fields, increase if User.Options has more than 64 values
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

    @SqlQuery("SELECT * FROM lw_user WHERE username = ?")
    Optional<User> findByUsername(String username);

    @SqlQuery("SELECT * FROM lw_user WHERE email = ? AND email_confirmation_token = ?")
    Optional<User> findByEmailConfirmationToken(String email, String confirmationToken);

    default Optional<User> findByUsernameAndPassword(String username, String password) {
        Optional<User> user = findByUsername(username);

        if (user.isPresent() && user.get().validatePassword(password)) {
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

    @SqlUpdate("INSERT INTO lw_user_auth (user_id, auth_id, token_hash, expires) VALUES(?, ?, ?, ?)")
    void insertAuth(User user, long authId, String token, LocalDateTime expires);

    @SqlUpdate("DELETE FROM lw_user_auth WHERE auth_id = ?")
    void deleteAuth(long authId);

    default Optional<User> findByAuthToken(long authId, String token) {
        return getHandle().select("SELECT user_id, token_hash, expires FROM lw_user_auth WHERE auth_id = ?", authId).map((rs, ctx) -> {
            int userId = rs.getInt("user_id");
            String tokenHash = rs.getString("token_hash");
            boolean expired = rs.getTimestamp("expires").toInstant().isBefore(Instant.now());

            if (!expired && HashHelper.isValidSha256(token, tokenHash)) {
                return findById(userId).orElse(null);
            } else {
                deleteAuth(authId); // it is expired, or someone trying to hijack it
            }

            return null;
        }).findOne();
    }

    @SqlQuery("SELECT u.* FROM lw_user u JOIN lw_user_token t USING(user_id) WHERE t.type = ? AND t.token = ? LIMIT 1")
    Optional<User> findByToken(User.TokenType tokenType, String token);

    @SqlQuery("SELECT token FROM lw_user_token WHERE type = ? AND user_id = ? LIMIT 1")
    Optional<String> findToken(User.TokenType tokenType, int userId);

    /**
     * Retrieves token by type and userId from database or creates a new one if none exists.
     */
    default String findTokenOrElseGenerate(User.TokenType tokenType, int userId) {
        Optional<String> dbToken = findToken(tokenType, userId);

        if (dbToken.isEmpty()) {
            String token = RandomStringUtils.randomAlphanumeric(128);
            insertToken(userId, tokenType, token);
            return token;
        }

        return dbToken.get();
    }

    @SqlUpdate("INSERT INTO lw_user_token (user_id, type, token) VALUES(?, ?, ?)")
    void insertToken(int userId, User.TokenType tokenType, String token);

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
        user.setEmailRaw(HashHelper.sha512(user.getEmail()));
        user.setPasswordRaw("deleted user");
        user.setUsername(user.getRealUsername() + " (Deleted)");
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
        Objects.requireNonNull(user.getRegistrationDate());

        // for new users double check that the username is free. If not the existing user will be overwritten
        if (user.getId() == 0 && findByUsername(user.getRealUsername()).isPresent()) {
            throw new IllegalArgumentException("Username is already taken");
        }

        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("user_id", SqlHelper.toNullable(user.getId()));
        params.put("username", user.getUsername());
        params.put("email", SqlHelper.toNullable(user.getEmail()));
        params.put("email_confirmation_token", user.getEmailConfirmationToken());
        params.put("email_confirmed", user.isEmailConfirmed());
        params.put("organisation_id", user.getOrganisationId());
        params.put("image_file_id", SqlHelper.toNullable(user.getImageFileId()));
        params.put("gender", user.getGender().ordinal());
        params.put("birthdate", user.getDateOfBirth());
        params.put("address", SqlHelper.toNullable(user.getAddress()));
        params.put("profession", SqlHelper.toNullable(user.getProfession()));
        params.put("bio", SqlHelper.toNullable(user.getAdditionalInformation()));
        params.put("interest", SqlHelper.toNullable(user.getInterest()));
        params.put("student_identifier", SqlHelper.toNullable(user.getStudentId()));
        params.put("is_admin", user.isAdmin());
        params.put("is_moderator", user.isModerator());
        params.put("created_at", user.getRegistrationDate());
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
        params.put("guides", user.getGuides()[0]);

        Optional<Integer> userId = SqlHelper.handleSave(getHandle(), "lw_user", params)
            .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

        userId.ifPresent(id -> {
            user.setId(id);
            cache.put(user);
        });
    }

    default void anonymize(User user) {
        user.setAdditionalInformation("");
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
                user.setEmailConfirmationToken(rs.getString("email_confirmation_token"));
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
                user.setAdditionalInformation(rs.getString("bio"));
                user.setInterest(rs.getString("interest"));
                user.setStudentId(rs.getString("student_identifier"));
                user.setRegistrationDate(SqlHelper.getLocalDateTime(rs.getTimestamp("created_at")));
                user.setCredits(rs.getString("credits"));
                user.setAcceptTermsAndConditions(rs.getBoolean("accept_terms_and_conditions"));
                user.setPreferredNotificationFrequency(User.NotificationFrequency.valueOf(rs.getString("preferred_notification_frequency")));
                user.setGuides(new long[] {rs.getLong("guides")});

                user.setTimeZone(ZoneId.of(rs.getString("time_zone")));
                user.setLocale(Locale.forLanguageTag(rs.getString("language").replace("_", "-")));

                user.setAdmin(rs.getBoolean("is_admin"));
                user.setModerator(rs.getBoolean("is_moderator"));

                // deserialize preferences
                byte[] preferenceBytes = rs.getBytes("preferences");
                if (preferenceBytes != null && preferenceBytes.length > 0) {
                    try {
                        user.setPreferences(SerializationUtils.deserialize(preferenceBytes));
                    } catch (Exception e) {
                        LogManager.getLogger(UserMapper.class).error("Couldn't load preferences for user {}", user.getId(), e);
                    }
                }

                cache.put(user);
            }
            return user;
        }
    }
}
