package de.l3s.learnweb;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import de.l3s.interwebj.AuthCredentials;
import de.l3s.interwebj.InterWeb;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.util.HasId;
import de.l3s.util.Image;
import de.l3s.util.MD5;

public class User implements Comparable<User>, Serializable, HasId
{
    private static final long serialVersionUID = 2482790243930271009L;

    public static final int GENDER_MALE = 1;
    public static final int GENDER_FEMALE = 2;

    private int id = -1;
    private InterWeb interweb;
    private int imageFileId; // profile image
    private int organisationId;
    private String username;
    private String email;
    private String password; // md5 hash

    private int gender;
    private Date dateofbirth;
    private String address;
    private String profession;
    private String additionalInformation;
    private String interest;
    private String phone;
    private Date registrationDate;

    private boolean destroyed = false;

    private Group activeGroup;
    private int activeGroupId;

    private boolean admin;
    private boolean moderator;

    private String iwKey;
    private String iwSecret;

    // caches
    private List<Course> courses;
    private List<Resource> resources;
    private List<Group> groups;
    private LinkedList<Group> writeAbleGroups;
    private String imageUrl;

    private TimeZone timeZone = TimeZone.getTimeZone("Europe/Berlin");
    private Date lastLoginDate = null;

    @Deprecated
    public User(ResultSet rs) throws SQLException
    {
	super();
	this.id = rs.getInt("user_id");
	this.username = rs.getString("username");
	this.email = rs.getString("email");
	this.password = rs.getString("password");
	this.organisationId = rs.getInt("organisation_id");
	this.activeGroupId = rs.getInt("active_group_id");
	this.imageFileId = rs.getInt("image_file_id");

	this.gender = rs.getInt("gender");
	this.dateofbirth = rs.getDate("dateofbirth");
	this.address = rs.getString("address");
	this.profession = rs.getString("profession");
	this.additionalInformation = rs.getString("additionalinformation");
	this.interest = rs.getString("interest");
	this.phone = rs.getString("phone");
	this.registrationDate = rs.getDate("registration_date");

	this.admin = rs.getInt("is_admin") == 1;
	this.moderator = rs.getInt("is_moderator") == 1;

	this.iwKey = rs.getString("iw_token");
	this.iwSecret = rs.getString("iw_secret");
	this.timeZone = TimeZone.getTimeZone("Europe/Berlin");
    }

    public User()
    {
    }

    public void clearCaches()
    {
	courses = null;
	resources = null;
	groups = null;
	writeAbleGroups = null;
    }

    public void onDestroy()
    {
	if(destroyed)
	    return;
	destroyed = true;
    }

    public boolean isLoggedInInterweb()
    {
	return !(getInterweb().getIWToken() == null);
    }

    public List<Course> getCourses() throws SQLException
    {
	if(courses != null)
	    return courses;

	courses = Learnweb.getInstance().getCourseManager().getCoursesByUserId(id);

	return courses;
    }

    public String getAdditionalInformation()
    {
	return additionalInformation;
    }

    public String getAddress()
    {
	return address;
    }

    public List<Comment> getComments() throws SQLException
    {
	return Learnweb.getInstance().getResourceManager().getCommentsByUserId(this.getId());
    }

    public Date getDateofbirth()
    {
	return dateofbirth;
    }

    public String getEmail()
    {
	return email;
    }

    @Override
    public int getId()
    {
	return id;
    }

    /**
     * Returns User.GENDER_MALE, User.GENDER_FEMALE or 0 if not set
     * 
     * @return
     */
    public int getGender()
    {
	return gender;
    }

    public String getInterest()
    {
	return interest;
    }

    public InterWeb getInterweb()
    {
	if(null == interweb)
	{
	    interweb = Learnweb.getInstance().getInterweb().clone();

	    if(iwKey != null && iwSecret != null)
	    {
		this.interweb.setIWToken(new AuthCredentials(iwKey, iwSecret));
	    }
	}
	return interweb;
    }

    public String getPhone()
    {
	return phone;
    }

    public String getProfession()
    {
	return profession;
    }

    public Resource addResource(Resource resource) throws SQLException
    {
	Learnweb learnweb = Learnweb.getInstance();
	resource = learnweb.getResourceManager().addResource(resource, this);

	learnweb.getArchiveUrlManager().addResourceToArchive(resource);

	if(null != resources)
	    resources.add(resource);

	return resource;
    }

    public void deleteResource(Resource resource) throws SQLException
    {
	if(null != resources)
	    resources.remove(resource);

	Learnweb.getInstance().getResourceManager().deleteResource(resource.getId());
    }

    public List<Resource> getResources() throws SQLException
    {
	if(null == resources)
	    resources = Learnweb.getInstance().getResourceManager().getResourcesByUserId(this.getId());

	return resources;
    }

    public List<Resource> getRatedResources() throws SQLException
    {
	return Learnweb.getInstance().getResourceManager().getRatedResourcesByUserId(this.getId());
    }

    public List<Tag> getTags() throws Exception
    {
	return Learnweb.getInstance().getResourceManager().getTagsByUserId(this.getId());
    }

    public String getUsername()
    {
	return username;
    }

    public void setAdditionalinformation(String additionalinformation)
    {
	this.additionalInformation = additionalinformation;
    }

    public void setAddress(String address)
    {
	this.address = address;
    }

    public void setDateofbirth(Date dateofbirth)
    {
	this.dateofbirth = dateofbirth;
    }

    public void setEmail(String email)
    {
	this.email = email;
    }

    public void setGender(int gender)
    {
	this.gender = gender;
    }

    public void setInterest(String interest)
    {
	this.interest = interest;
    }

    public void setPhone(String phone)
    {
	this.phone = phone;
    }

    public void setProfession(String profession)
    {
	this.profession = profession;
    }

    public void setUsername(String username)
    {
	this.username = username;
    }

    public void setOrganisationId(int organisationId)
    {
	this.organisationId = organisationId;
    }

    public int getOrganisationId()
    {
	return organisationId;
    }

    public Organisation getOrganisation() throws SQLException
    {
	return Learnweb.getInstance().getOrganisationManager().getOrganisationById(organisationId);
    }

    @Override
    public boolean equals(Object obj)
    {
	if(null == obj)
	    return false;

	if(obj.getClass() != this.getClass())
	    return false;

	return ((User) obj).getId() == this.getId();
    }

    /**
     * returns the groups the user is member off
     * 
     * @return
     * @throws SQLException
     */
    public List<Group> getGroups() throws SQLException
    {
	if(null == groups)
	    groups = Learnweb.getInstance().getGroupManager().getGroupsByUserId(id);

	return groups;
    }

    /**
     * returns the groups the user can add resources to
     * 
     * @return
     * @throws SQLException
     */
    public List<Group> getWriteAbleGroups() throws SQLException
    {
	if(null == writeAbleGroups)
	{
	    writeAbleGroups = new LinkedList<Group>();
	    getGroups();
	    for(Group group : groups)
	    {
		if(!group.isRestrictionOnlyLeaderCanAddResources() || group.isLeader(this))
		    writeAbleGroups.add(group);
	    }
	}
	return writeAbleGroups;
    }

    public void joinGroup(int groupId) throws Exception
    {
	joinGroup(Learnweb.getInstance().getGroupManager().getGroupById(groupId));
    }

    public void joinGroup(Group group) throws Exception
    {
	Learnweb.getInstance().getGroupManager().addUserToGroup(this, group);

	groups = null; // force reload
	writeAbleGroups = null; // force reload

	group.clearCaches();

	setActiveGroup(group);
    }

    public void leaveGroup(Group group) throws Exception
    {
	Learnweb.getInstance().getGroupManager().removeUserFromGroup(this, group);

	groups = null; // force reload
	writeAbleGroups = null;

	group.clearCaches(); // removeMember(this);

	if(activeGroup == group)
	    setActiveGroup(null);
    }

    public void deleteGroup(Group group) throws Exception
    {
	Learnweb.getInstance().getGroupManager().deleteGroup(group.getId());

	if(null != groups)
	    groups.remove(group);

	if(activeGroup == group)
	    setActiveGroup(null);
    }

    @Override
    public int compareTo(User o)
    {
	return getUsername().compareTo(o.getUsername());
    }

    /**
     * Defines the group, the user ist currently working on
     * 
     * @param group
     * @throws SQLException
     */
    public void setActiveGroup(Group group) throws SQLException
    {
	setActiveGroup(null == group ? 0 : group.getId());

	this.activeGroup = group;
    }

    /**
     * Defines the group, the user ist currently working on
     * 
     * @param group
     * @throws SQLException
     */
    public void setActiveGroup(int groupId) throws SQLException
    {
	this.activeGroup = null; // force to reload group
	this.activeGroupId = groupId;
    }

    /**
     * Returns the group, which the user is currently working on
     * 
     * @return may be null if not yet set
     * @throws SQLException
     */
    public Group getActiveGroup() throws SQLException
    {
	if(null == activeGroup && activeGroupId != 0)
	    activeGroup = Learnweb.getInstance().getGroupManager().getGroupById(activeGroupId);
	return activeGroup;
    }

    /**
     * Returns the id of the group, which the user is currently working on
     * 
     * @return may be null if not yet set
     * @throws SQLException
     */
    public int getActiveGroupId()
    {
	return activeGroupId;
    }

    public void setImage(InputStream inputStream) throws SQLException, IOException, IllegalArgumentException
    {
	FileManager fileManager = Learnweb.getInstance().getFileManager();

	// process image
	Image img = new Image(inputStream);
	Image thumbnail = img.getResizedToSquare(100, 0.05);

	// save image file
	File file = new File();
	file.setName("user_icon.png");
	file.setMimeType("image/png");
	file = fileManager.save(file, thumbnail.getInputStream());
	thumbnail.dispose();
	inputStream.close();

	imageFileId = file.getId();
	imageUrl = file.getUrl();

	Learnweb.getInstance().getUserManager().save(this);
    }

    public boolean isAdmin()
    {
	return admin;
    }

    public void setAdmin(boolean admin)
    {
	this.admin = admin;
    }

    public boolean isModerator()
    {
	return moderator;
    }

    public void setModerator(boolean moderator)
    {
	this.moderator = moderator;
    }

    /**
     * returns the url of the users image
     * or a default image if no image has been added
     * 
     * @return
     * @throws SQLException
     */
    public String getImage() throws SQLException
    {
	if(imageUrl == null)
	{
	    File file = Learnweb.getInstance().getFileManager().getFileById(imageFileId);

	    if(null == file)
		imageUrl = UtilBean.getLearnwebBean().getContextUrl() + "/resources/image/no_profile.jpg";
	    else
		imageUrl = file.getUrl();
	}

	return imageUrl;
    }

    public void setId(int id)
    {
	this.id = id;
    }

    /**
     * key of the interweb token
     * 
     * @return
     */
    public String getInterwebKey()
    {
	return iwKey;
    }

    /**
     * key of the interweb token
     * 
     * @param iwKey
     */
    public void setInterwebKey(String iwKey)
    {
	this.iwKey = iwKey;
	this.interweb = null; // force reload
    }

    /**
     * secret of the interweb token
     * 
     * @return
     */
    public String getInterwebSecret()
    {
	return iwSecret;
    }

    /**
     * secret of the interweb token
     * 
     * @param iwSecret
     */
    public void setInterwebSecret(String iwSecret)
    {
	this.iwSecret = iwSecret;
	this.interweb = null; // force reload
    }

    public void setInterwebToken(AuthCredentials auth)
    {
	getInterweb(); // make sure interweb is loaded

	this.interweb.setIWToken(auth);
	if(null == auth)
	{
	    this.iwKey = null;
	    this.iwSecret = null;
	}
	else
	{
	    this.iwKey = auth.getKey();
	    this.iwSecret = auth.getSecret();
	}
    }

    public int getImageFileId()
    {
	return imageFileId;
    }

    public void setImageFileId(int imageFileId)
    {
	this.imageFileId = imageFileId;
    }

    public Date getRegistrationDate()
    {
	return registrationDate;
    }

    public void setRegistrationDate(Date registrationDate)
    {
	this.registrationDate = registrationDate;
    }

    public String getPassword()
    {
	return password;
    }

    /**
     * If the password is not encrypted (plain text) set isEncrypted to false
     * 
     * @param password
     * @param isEncrypted
     */
    public void setPassword(String password, boolean isEncrypted)
    {
	if(!isEncrypted)
	    password = MD5.hash(password);
	this.password = password;
    }

    public TimeZone getTimeZone()
    {
	return timeZone;
    }

    public void setTimeZone(TimeZone timeZone)
    {
	this.timeZone = timeZone;
    }

    public Date getLastLoginDate() throws SQLException
    {
	if(null == lastLoginDate)
	{
	    lastLoginDate = Learnweb.getInstance().getUserManager().getLastLoginDate(id);
	}
	return lastLoginDate;
    }
}
