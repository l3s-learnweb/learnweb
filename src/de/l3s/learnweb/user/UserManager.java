package de.l3s.learnweb.user;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.cache.CacheBuilder;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.user.User.Gender;
import de.l3s.util.Cache;
import de.l3s.util.DummyCache;
import de.l3s.util.ICache;
import de.l3s.util.SHA512;
import de.l3s.util.database.Sql;

/**
 * DAO for the User class.
 *
 * @author Philipp Kemkes
 */
public class UserManager {
    private static final Logger log = LogManager.getLogger(UserManager.class);

    /**
     * Saves the User to the database.
     * If the User is not yet stored at the database, a new record will be created and the returned User contains the new id.
     */
    private static final String COLUMNS = "user_id, username, email, email_confirmation_token, is_email_confirmed, organisation_id, "
        + "image_file_id, gender, dateofbirth, address, profession, additionalinformation, interest, student_identifier, is_admin, "
        + "is_moderator, registration_date, password, hashing, preferences, credits, fullname, affiliation, accept_terms_and_conditions, "
        + "deleted, preferred_notification_frequency, time_zone, language, guides";

    // if you change this, you have to change createUser() too

    private final Learnweb learnweb;
    private final ICache<User> cache;
    // used by proxy and annotations to identify user
    private final com.google.common.cache.Cache<String, Integer> tokenStorage;

    public UserManager(Learnweb learnweb) {
        Properties properties = learnweb.getProperties();
        int userCacheSize = Integer.parseInt(properties.getProperty("USER_CACHE"));

        this.learnweb = learnweb;
        this.cache = userCacheSize == 0 ? new DummyCache<>() : new Cache<>(userCacheSize);
        this.tokenStorage = CacheBuilder.newBuilder().expireAfterAccess(Duration.ofHours(1)).build();
    }

    public void resetCache() {
        for (User user : cache.getValues()) {
            user.clearCaches();
        }

        cache.clear();
    }

    /**
     * @return number of cached objects
     */
    public int getCacheSize() {
        return cache.size();
    }

    public List<User> getUsersByCourseId(int courseId) throws SQLException {
        List<User> users = new LinkedList<>();
        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM `lw_user` JOIN lw_user_course USING(user_id) WHERE course_id = ? AND deleted = 0 ORDER BY username")) {
            select.setInt(1, courseId);
            try (ResultSet rs = select.executeQuery()) {
                while (rs.next()) {
                    users.add(createUser(rs));
                }
            }
        }

        return users;
    }

    /**
     * Returns a list of all users.
     */
    public List<User> getUsers() throws SQLException {
        List<User> users = new LinkedList<>();
        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM `lw_user` WHERE deleted = 0 ORDER BY username")) {
            try (ResultSet rs = select.executeQuery()) {
                while (rs.next()) {
                    users.add(createUser(rs));
                }
            }
        }

        return users;
    }

    public List<User> getUsersByOrganisationId(int organisationId) throws SQLException {
        List<User> users = new LinkedList<>();
        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM `lw_user` WHERE organisation_id = ? AND deleted = 0 ORDER BY username")) {
            select.setInt(1, organisationId);
            try (ResultSet rs = select.executeQuery()) {
                while (rs.next()) {
                    users.add(createUser(rs));
                }
            }
        }

        return users;
    }

    public List<User> getUsersByGroupId(int groupId) throws SQLException {
        List<User> users = new LinkedList<>();
        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM `lw_user` JOIN lw_group_user USING(user_id) WHERE group_id = ? AND deleted = 0 ORDER BY username")) {
            select.setInt(1, groupId);
            try (ResultSet rs = select.executeQuery()) {
                while (rs.next()) {
                    users.add(createUser(rs));
                }
            }
        }

        return users;
    }

    public List<User> getUsersByGroupId(int groupId, int limit) throws SQLException {
        List<User> users = new LinkedList<>();
        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM `lw_user` JOIN lw_group_user USING(user_id) WHERE group_id = ? AND deleted = 0 ORDER BY join_time LIMIT ?")) {
            select.setInt(1, groupId);
            select.setInt(2, limit);
            try (ResultSet rs = select.executeQuery()) {
                while (rs.next()) {
                    users.add(createUser(rs));
                }
            }
        }

        return users;
    }

    /**
     * Get a user by username and password.
     *
     * @return null if user not found
     */
    public User getUser(String username, String password) throws SQLException {
        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM lw_user WHERE username = ?")) {
            select.setString(1, username);
            try (ResultSet rs = select.executeQuery()) {
                if (rs.next()) {
                    User user = createUser(rs);
                    if (user.validatePassword(password)) {
                        if (user.getHashing() != User.PasswordHashing.PBKDF2) {
                            user.setPassword(password);
                            save(user);
                        }

                        return user;
                    }
                }

                return null;
            }
        }
    }

    public List<User> getUser(String email) throws SQLException {
        List<User> users = new LinkedList<>();
        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM lw_user WHERE email = ?")) {
            select.setString(1, email);
            try (ResultSet rs = select.executeQuery()) {
                while (rs.next()) {
                    users.add(createUser(rs));
                }
            }
        }

        return users;
    }

    /**
     * Get a User by his id.
     *
     * @return returns null if the user does not exist
     */
    public User getUser(int userId) throws SQLException {
        if (userId == 0) {
            return null;
        }

        User user = cache.get(userId);

        if (null != user) {
            return user;
        }

        try (PreparedStatement stmtGetUser = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM `lw_user` WHERE user_id = ?")) {
            stmtGetUser.setInt(1, userId);
            try (ResultSet rs = stmtGetUser.executeQuery()) {
                if (!rs.next()) {
                    log.warn("invalid user id was requested: " + userId, new IllegalArgumentException());
                    return null;
                }

                user = createUser(rs);
            }
        }

        user = cache.put(user);

        return user;
    }

    public User getUserByUsername(String username) throws SQLException {
        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM lw_user WHERE username = ?")) {
            select.setString(1, username);
            try (ResultSet rs = select.executeQuery()) {
                if (rs.next()) {
                    return createUser(rs);
                }

                return null;
            }
        }
    }

    public User getUserByEmailAndConfirmationToken(String email, String emailConfirmationToken) throws SQLException {
        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM lw_user WHERE email = ? AND email_confirmation_token = ?")) {
            select.setString(1, email);
            select.setString(2, emailConfirmationToken);
            try (ResultSet rs = select.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                return createUser(rs);
            }
        }
    }

    /**
     * Get a User ID given the username.
     *
     * @return Returns -1 if an invalid username was given
     */
    public int getUserIdByUsername(String username) throws SQLException {
        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT user_id FROM `lw_user` WHERE username = ?")) {
            select.setString(1, username);
            try (ResultSet rs = select.executeQuery()) {
                if (!rs.next()) {
                    return -1;
                }

                return rs.getInt("user_id");
            }
        }
    }

    public User registerUser(final User user, Course course) throws SQLException, IOException {
        if (null == course) {
            course = learnweb.getCourseManager().getCourseByWizard("default");
        }

        user.setOrganisationId(course.getOrganisationId());
        user.setRegistrationDate(new Date());
        user.setPreferences(new HashMap<>());
        // TODO @astappiev: we should create it by first request, not when we create a user
        user.setImage(user.getDefaultImageIS());

        save(user);

        course.addUser(user);
        return user;
    }

    /**
     * Checks whether the username is already taken.
     *
     * @return Returns true if username is already in use
     */
    public boolean isUsernameAlreadyTaken(String username) throws SQLException {
        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT 1 FROM lw_user WHERE username = ?")) {
            select.setString(1, username);
            try (ResultSet rs = select.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * @return The dateTime of the last recorded login event of the given user. Empty if the user has never logged in
     */
    public Optional<Instant> getLastLoginDate(int userId) throws SQLException {
        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT timestamp FROM `lw_user_log` WHERE `user_id` = ? AND action = " + Action.login.ordinal() + " ORDER BY `lw_user_log`.`timestamp` DESC LIMIT 1")) {
            select.setInt(1, userId);
            ResultSet rs = select.executeQuery();

            if (!rs.next()) {
                return Optional.empty();
            }

            return Optional.of(rs.getObject("timestamp", Timestamp.class).toInstant());
        }
    }

    public User save(User user) throws SQLException {
        // verify that the given obj is valid; added only attributes that had already caused problems in the past
        Objects.requireNonNull(user.getRealUsername());
        Objects.requireNonNull(user.getRegistrationDate());

        if (user.getId() <= 0 && isUsernameAlreadyTaken(user.getRealUsername())) { // for new users double check that the username is free. If not the existing user will be overwritten
            throw new IllegalArgumentException("Username is already taken");
        }

        try (PreparedStatement replace = learnweb.getConnection().prepareStatement("REPLACE INTO `lw_user` (" + COLUMNS + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            if (user.getId() < 0) { // the User is not yet stored at the database
                replace.setNull(1, java.sql.Types.INTEGER);
            } else {
                replace.setInt(1, user.getId());
            }
            replace.setString(2, user.getRealUsername());
            replace.setString(3, user.getEmail());
            replace.setString(4, user.getEmailConfirmationToken());
            replace.setBoolean(5, user.isEmailConfirmed());
            replace.setInt(6, user.getOrganisationId());
            replace.setInt(7, user.getImageFileId());
            replace.setInt(8, user.getGender().ordinal());
            replace.setDate(9, user.getDateOfBirth() == null ? null : new java.sql.Date(user.getDateOfBirth().getTime()));
            replace.setString(10, user.getAddress());
            replace.setString(11, user.getProfession());
            replace.setString(12, user.getAdditionalInformation());
            replace.setString(13, user.getInterest());
            replace.setString(14, user.getStudentId());
            replace.setInt(15, user.isAdmin() ? 1 : 0);
            replace.setInt(16, user.isModerator() ? 1 : 0);
            replace.setTimestamp(17, user.getRegistrationDate() == null
                ? new java.sql.Timestamp(System.currentTimeMillis())
                : new java.sql.Timestamp(user.getRegistrationDate().getTime()));
            replace.setString(18, user.getPassword());
            replace.setString(19, user.getHashing().name());

            Sql.setSerializedObject(replace, 20, user.getPreferences());

            replace.setString(21, user.getCredits());
            replace.setString(22, user.getFullName());
            replace.setString(23, user.getAffiliation());
            replace.setBoolean(24, user.isAcceptTermsAndConditions());
            replace.setBoolean(25, user.isDeleted());
            replace.setString(26, user.getPreferredNotificationFrequency().toString());
            replace.setString(27, user.getTimeZone().getId());
            replace.setString(28, user.getLocale().toString());
            replace.setLong(29, user.getGuides()[0]);
            replace.executeUpdate();

            if (user.getId() < 0) { // get the assigned id
                try (ResultSet rs = replace.getGeneratedKeys()) {
                    if (!rs.next()) {
                        throw new SQLException("database error: no id generated");
                    }

                    user.setId(rs.getInt(1));
                    cache.put(user); // add the createUser to the cache
                }
            }
        }

        return user;
    }

    @SuppressWarnings("unchecked")
    private User createUser(ResultSet rs) throws SQLException {
        int userId = rs.getInt("user_id");
        User user = cache.get(userId);
        if (null != user) {
            return user;
        }

        user = new User();
        user.setId(rs.getInt("user_id"));
        user.setDeleted(rs.getBoolean("deleted"));
        user.setUsername(rs.getString("username"));
        user.setEmailRaw(rs.getString("email"));
        user.setEmailConfirmationToken(rs.getString("email_confirmation_token"));
        user.setEmailConfirmed(rs.getBoolean("is_email_confirmed"));
        user.setPasswordRaw(rs.getString("password"));
        user.setHashing(rs.getString("hashing"));
        user.setOrganisationId(rs.getInt("organisation_id"));
        user.setImageFileId(rs.getInt("image_file_id"));
        user.setGender(Gender.values()[rs.getInt("gender")]);
        user.setDateOfBirth(rs.getDate("dateofbirth"));
        user.setFullName(rs.getString("fullname"));
        user.setAffiliation(rs.getString("affiliation"));
        user.setAddress(rs.getString("address"));
        user.setProfession(rs.getString("profession"));
        user.setAdditionalInformation(rs.getString("additionalinformation"));
        user.setInterest(rs.getString("interest"));
        user.setStudentId(rs.getString("student_identifier"));
        user.setRegistrationDate(new Date(rs.getTimestamp("registration_date").getTime()));
        user.setCredits(rs.getString("credits"));
        user.setAcceptTermsAndConditions(rs.getBoolean("accept_terms_and_conditions"));
        user.setPreferredNotificationFrequency(User.NotificationFrequency.valueOf(rs.getString("preferred_notification_frequency")));
        user.setGuides(new long[] {rs.getLong("guides")});

        user.setTimeZone(ZoneId.of(rs.getString("time_zone")));
        user.setLocale(Locale.forLanguageTag(rs.getString("language").replace("_", "-")));

        user.setAdmin(rs.getInt("is_admin") == 1);
        user.setModerator(rs.getInt("is_moderator") == 1);

        // deserialize preferences
        HashMap<String, String> preferences = null;

        byte[] preferenceBytes = rs.getBytes("preferences");

        if (preferenceBytes != null && preferenceBytes.length > 0) {
            try {
                ObjectInputStream preferencesStream = new ObjectInputStream(new ByteArrayInputStream(preferenceBytes));

                // re-create the object
                preferences = (HashMap<String, String>) preferencesStream.readObject();
            } catch (Exception e) {
                log.error("Couldn't load preferences for user " + user.getId(), e);
            }
        }

        if (preferences == null) {
            preferences = new HashMap<>();
        }

        user.setPreferences(preferences);

        user = cache.put(user);

        return user;
    }

    public void anonymize(User user) throws SQLException {
        log.debug("Anonymize user: " + user);

        user.setAdditionalInformation("");
        user.setAddress("");
        user.setAffiliation("");
        user.setDateOfBirth(new Date(0));
        user.setFullName("");
        user.setImageFileId(0);
        user.setInterest("");
        user.setProfession("");
        user.setStudentId("");
        user.setUsername("Anonym " + user.getId());
        user.setEmailRaw(SHA512.hash(user.getEmail()));

        user.save();
    }

    public void deleteUserHard(User user) throws SQLException {
        log.info("Delete user " + user);

        if (user.getResources().size() > 10) {
            log.warn("delete user: " + user + " and his " + user.getResources().size() + " resorces?");
            log.info("Delete user ");
        }

        String[] tables = {"lw_group_user", "lw_user_log", "lw_user_course", "lw_comment", "lw_resource_rating", "lw_resource_tag",
            "lw_thumb", "lw_survey_answer", "lw_survey_resource_user", "lw_glossary_entry", "lw_glossary_term", "lw_news",
            "lw_resource_history", "lw_submission_resource", "lw_submission_status", "lw_transcript_actions", "lw_transcript_summary"};

        for (String table : tables) {
            try (PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM " + table + " WHERE `user_id` = ?")) {
                delete.setInt(1, user.getId());
                int numRowsAffected = delete.executeUpdate();
                if (numRowsAffected > 0) {
                    log.debug("Deleted " + numRowsAffected + " rows from " + table);
                }
            }
        }

        // TODO @kemkes: how to handle lw_forum_post.post_edit_user_id

        /*
         TODO @kemkes: how to handle topics. We can not just delete them
         * topic_last_post_user_id
        */

        for (Resource resource : user.getResources()) {
            resource.deleteHard();
        }

        try (PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM message WHERE `from_user` = ? OR `to_user` = ?")) {
            delete.setInt(1, user.getId());
            delete.setInt(2, user.getId());
            delete.executeUpdate();
        }

        try (PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM lw_user WHERE `user_id` = ?")) {
            delete.setInt(1, user.getId());
            delete.executeUpdate();
        }
    }

    /**
     * <ul>
     * <li>The user is removed from all his groups.
     * <li>His private resources are deleted
     * <li>His name and password are changed so that he can't login
     * </ul>
     */
    public void deleteUserSoft(User user) throws SQLException {
        log.debug("Deleting user " + user);

        for (Resource resource : user.getResources()) {
            if (resource.getGroupId() == 0) { // delete only private resources
                resource.delete();
            }
        }

        try (PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM lw_group_user WHERE `user_id` = ?")) {
            delete.setInt(1, user.getId());
            delete.executeUpdate();
        }

        user.setDeleted(true);
        user.setEmailRaw(SHA512.hash(user.getEmail()));
        user.setPasswordRaw("deleted user");
        user.setUsername(user.getRealUsername() + " (Deleted)");
        user.save();
    }

    public void saveToken(String token, Integer userId) {
        tokenStorage.put(token, userId);
    }

    public Integer getUserByToken(String token) {
        return tokenStorage.getIfPresent(token);
    }
}
