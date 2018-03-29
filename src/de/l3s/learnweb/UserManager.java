package de.l3s.learnweb;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import de.l3s.interwebj.AuthCredentials;
import de.l3s.util.Cache;
import de.l3s.util.DummyCache;
import de.l3s.util.ICache;
import de.l3s.util.Sql;

/**
 * DAO for the User class.
 * Because there are only a few Users we keep them all in memory
 *
 * @author Philipp
 *
 */
public class UserManager
{
    private final static Logger log = Logger.getLogger(UserManager.class);

    // if you change this, you have to change the constructor of User too
    private final static String COLUMNS = "user_id, username, email, organisation_id, iw_token, iw_secret, active_group_id, image_file_id, gender, dateofbirth, address, profession, additionalinformation, interest, phone, is_admin, is_moderator, active_course_id, registration_date, password, preferences, credits, fullname, affiliation, accept_terms_and_conditions";

    private Learnweb learnweb;
    private ICache<User> cache;

    protected UserManager(Learnweb learnweb) throws SQLException
    {
        super();
        Properties properties = learnweb.getProperties();
        int userCacheSize = Integer.parseInt(properties.getProperty("USER_CACHE"));

        this.learnweb = learnweb;
        this.cache = userCacheSize == 0 ? new DummyCache<User>() : new Cache<User>(userCacheSize);
    }

    public void resetCache()
    {
        for(User user : cache.getValues())
            user.clearCaches();

        cache.clear();
    }

    /**
     *
     * @return number of cached objects
     */
    public int getCacheSize()
    {
        return cache.size();
    }

    public List<User> getUsersByCourseId(int courseId) throws SQLException
    {
        List<User> users = new LinkedList<User>();
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM `lw_user` JOIN lw_user_course USING(user_id) WHERE course_id = ? AND deleted = 0 ORDER BY username");
        select.setInt(1, courseId);
        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            users.add(createUser(rs));
        }
        select.close();

        return users;
    }

    /**
     * returns a list of all users
     *
     * @return
     * @throws SQLException
     */

    public List<User> getUsers() throws SQLException
    {
        List<User> users = new LinkedList<User>();
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM `lw_user` WHERE deleted = 0 ORDER BY username");
        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            users.add(createUser(rs));
        }
        select.close();

        return users;
    }

    public List<User> getUsersByOrganisationId(int organisationId) throws SQLException
    {
        List<User> users = new LinkedList<User>();
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM `lw_user` WHERE organisation_id = ? AND deleted = 0 ORDER BY username");
        select.setInt(1, organisationId);
        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            users.add(createUser(rs));
        }
        select.close();

        return users;
    }

    public List<User> getUsersByGroupId(int groupId) throws SQLException
    {
        List<User> users = new LinkedList<User>();
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM `lw_user` JOIN lw_group_user USING(user_id) WHERE group_id = ? AND deleted = 0 ORDER BY username");
        select.setInt(1, groupId);
        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            users.add(createUser(rs));
        }
        select.close();

        return users;
    }

    /**
     * get a user by username and password
     *
     * @param Username
     * @param Password
     * @return null if user not found
     * @throws SQLException
     */

    public User getUser(String username, String password) throws SQLException
    {
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM lw_user WHERE username = ? AND password = MD5(?)");

        try
        {
            select.setString(1, username);
            select.setString(2, password);
            ResultSet rs = select.executeQuery();

            if(!rs.next())
                return null;

            User user = createUser(rs);

            return user;
        }
        finally
        {
            select.close();
        }
    }

    public List<User> getUser(String email) throws SQLException
    {
        List<User> users = new LinkedList<User>();
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM lw_user WHERE email = ?");
        select.setString(1, email);
        ResultSet rs = select.executeQuery();

        while(rs.next())
        {
            users.add(createUser(rs));
        }

        select.close();

        return users;
    }

    /**
     * Get a User by his id
     * returns null if the user does not exist
     *
     * @param userId
     * @return
     * @throws SQLException
     */

    public User getUser(int userId) throws SQLException
    {
        if(userId == 0)
            return null;
        else if(userId < 1)
            new IllegalArgumentException("invalid user id was requested: " + userId).printStackTrace();

        User user = cache.get(userId);

        if(null != user)
        {
            //	    log.debug("Get user " + user.getUsername() + " from cache");
            return user;
        }

        PreparedStatement pstmtGetUser = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM `lw_user` WHERE user_id = ?");
        pstmtGetUser.setInt(1, userId);
        ResultSet rs = pstmtGetUser.executeQuery();

        if(!rs.next())
        {
            new IllegalArgumentException("invalid user id was requested: " + userId).printStackTrace();
            return null; //throw new IllegalArgumentException("invalid user id");
        }
        user = createUser(rs);
        pstmtGetUser.close();

        user = cache.put(user);

        //log.debug("Get user " + user.getUsername() + " from db");

        return user;
    }

    /**
     * Get a User ID given the username
     *
     * @param username
     * @return Returns -1 if an invalid username was given
     * @throws SQLException
     */
    public int getUserIdByUsername(String username) throws SQLException
    {
        PreparedStatement pstmtGetUser = learnweb.getConnection().prepareStatement("SELECT user_id FROM `lw_user` WHERE username = ?");
        pstmtGetUser.setString(1, username);
        ResultSet rs = pstmtGetUser.executeQuery();

        if(!rs.next())
        {
            //log.warn("invalid user name was requested: " + username);
            return -1; //throw new IllegalArgumentException("invalid user id");
        }
        int userId = rs.getInt("user_id");
        pstmtGetUser.close();

        return userId;
    }

    /**
     *
     * @param username
     * @param password
     * @param email
     * @param wizard
     * @throws Exception
     */

    public User registerUser(String username, String password, String email, String wizardTitle) throws Exception
    {
        if(null == wizardTitle || wizardTitle.length() == 0)
            wizardTitle = "default";

        // get the course corresponding to the wizard
        Course course = learnweb.getCourseManager().getCourseByWizard(wizardTitle);

        if(null == course)
            throw new IllegalArgumentException("Invalid registration wizard parameter");

        AuthCredentials iwToken = null;

        try
        {
            // register a User at interweb
            iwToken = learnweb.getInterweb().registerUser(username, password, course.getDefaultInterwebUsername(), course.getDefaultInterwebPassword());

            int counter = 2;
            while(null == iwToken) // username already taken
            {
                iwToken = learnweb.getInterweb().registerUser(username + "_" + counter++, password, course.getDefaultInterwebUsername(), course.getDefaultInterwebPassword());
            }
        }
        catch(Exception e)
        {
            log.warn("Could not create interweb account for user: " + username);
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setInterwebToken(iwToken);
        user.setOrganisationId(course.getOrganisationId());
        user.setPassword(password, false);
        user.setPreferences(new HashMap<String, String>());
        user = save(user);

        course.addUser(user);

        if(course.getDefaultGroupId() != 0)
            user.joinGroup(course.getDefaultGroupId());

        return user;
    }

    /**
     * Returns true if username is already in use
     *
     * @param username
     * @return
     * @throws SQLException
     */

    public boolean isUsernameAlreadyTaken(String username) throws SQLException
    {
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT 1 FROM lw_user WHERE username = ?");
        select.setString(1, username);
        ResultSet rs = select.executeQuery();
        boolean result = rs.next();
        select.close();

        return result;
    }

    /**
     * Returns 1.1.1970 00:00:00 if the user never logged in
     *
     * @param userId
     * @return
     * @throws SQLException
     */

    public Date getLastLoginDate(int userId) throws SQLException
    {
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT timestamp FROM `lw_user_log` WHERE `user_id` = ? AND action = " + LogEntry.Action.login.ordinal() + " ORDER BY `lw_user_log`.`timestamp` DESC LIMIT 1");
        select.setInt(1, userId);
        ResultSet rs = select.executeQuery();
        if(!rs.next())
            return new Date(0);

        Date loginDate = new Date(rs.getTimestamp(1).getTime());

        select.close();

        return loginDate;
    }

    /**
     * Saves the User to the database.
     * If the User is not yet stored at the database, a new record will be created and the returned User contains the new id.
     *
     * @param user
     * @return
     * @throws SQLException
     */

    public User save(User user) throws SQLException
    {
        PreparedStatement replace = learnweb.getConnection().prepareStatement("REPLACE INTO `lw_user` (" + COLUMNS + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);

        if(user.getId() < 0) // the User is not yet stored at the database
            replace.setNull(1, java.sql.Types.INTEGER);
        else
            replace.setInt(1, user.getId());
        replace.setString(2, user.getRealUsername());
        replace.setString(3, user.getEmail());
        replace.setInt(4, user.getOrganisationId());
        replace.setString(5, user.getInterwebKey());
        replace.setString(6, user.getInterwebSecret());
        replace.setInt(7, user.getActiveGroupId());
        replace.setInt(8, user.getImageFileId());
        replace.setInt(9, user.getGender());
        replace.setDate(10, user.getDateofbirth() == null ? null : new java.sql.Date(user.getDateofbirth().getTime()));
        replace.setString(11, user.getAddress());
        replace.setString(12, user.getProfession());
        replace.setString(13, user.getAdditionalInformation());
        replace.setString(14, user.getInterest());
        replace.setString(15, user.getStudentId());
        replace.setInt(16, user.isAdmin() ? 1 : 0);
        replace.setInt(17, user.isModerator() ? 1 : 0);
        replace.setInt(18, 0); // not used any more
        replace.setTimestamp(19, user.getRegistrationDate() == null ? new java.sql.Timestamp(System.currentTimeMillis()) : new java.sql.Timestamp(user.getRegistrationDate().getTime()));
        replace.setString(20, user.getPassword());

        Sql.setSerializedObject(replace, 21, user.getPreferences());

        replace.setString(22, user.getCredits());
        replace.setString(23, user.getFullName());
        replace.setString(24, user.getAffiliation());
        replace.setBoolean(25, user.isAcceptTermsAndConditions());
        replace.executeUpdate();

        if(user.getId() < 0) // get the assigned id
        {
            ResultSet rs = replace.getGeneratedKeys();
            if(!rs.next())
                throw new SQLException("database error: no id generated");
            user.setId(rs.getInt(1));

            cache.put(user); // add the createUser to the cache
        }
        replace.close();

        return user;
    }

    @SuppressWarnings("unchecked")
    private User createUser(ResultSet rs) throws SQLException
    {
        int userId = rs.getInt("user_id");
        User user = cache.get(userId);
        if(null != user)
        {
            return user;
        }

        user = new User();
        user.setId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"), true);
        user.setOrganisationId(rs.getInt("organisation_id"));
        user.setActiveGroup(rs.getInt("active_group_id"));
        user.setImageFileId(rs.getInt("image_file_id"));

        user.setGender(rs.getInt("gender"));
        user.setDateofbirth(rs.getDate("dateofbirth"));
        user.setFullName(rs.getString("fullname"));
        user.setAffiliation(rs.getString("affiliation"));
        user.setAddress(rs.getString("address"));
        user.setProfession(rs.getString("profession"));
        user.setAdditionalinformation(rs.getString("additionalinformation"));
        user.setInterest(rs.getString("interest"));
        user.setStudentId(rs.getString("phone"));
        user.setRegistrationDate(new Date(rs.getTimestamp("registration_date").getTime()));
        user.setCredits(rs.getString("credits"));
        user.setAcceptTermsAndConditions(rs.getBoolean("accept_terms_and_conditions"));

        user.setAdmin(rs.getInt("is_admin") == 1);
        user.setModerator(rs.getInt("is_moderator") == 1);

        user.setInterwebKey(rs.getString("iw_token"));
        user.setInterwebSecret(rs.getString("iw_secret"));
        user.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));

        // deserialize preferences
        HashMap<String, String> preferences = null;

        byte[] preferenceBytes = rs.getBytes("preferences");

        if(preferenceBytes != null && preferenceBytes.length > 0)
        {
            ByteArrayInputStream preferenceBAIS = new ByteArrayInputStream(preferenceBytes);

            try
            {
                ObjectInputStream preferencesOIS = new ObjectInputStream(preferenceBAIS);

                // re-create the object
                preferences = (HashMap<String, String>) preferencesOIS.readObject();
            }
            catch(Exception e)
            {
                log.error("Couldn't load preferences for user " + user.getId(), e);
            }
        }

        if(preferences == null)
            preferences = new HashMap<String, String>();

        user.setPreferences(preferences);

        user = cache.put(user);

        return user;
    }

    public void saveGmailId(String gmailId, int userId) throws SQLException
    {
        PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT IGNORE INTO `lw_user_gmail` (`user_id` , `gmail_id`) VALUES (?, ?)");
        insert.setInt(1, userId);
        insert.setString(2, gmailId);
        insert.executeUpdate();
        insert.close();
    }

}
