package de.l3s.learnweb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DAO for the Organisation class.
 * Because there are only a few organisations we keep them all in memory
 * 
 * @author Philipp
 * 
 */
public class OrganisationManager
{

    // if you change this, you have to change the constructor of Organisation too
    private final static String COLUMNS = "organisation_id, title, logo, welcome_page, welcome_message, options_field1";

    private Learnweb learnweb;
    private Map<Integer, Organisation> cache;

    public OrganisationManager(Learnweb learnweb) throws SQLException
    {
	super();
	this.learnweb = learnweb;
	this.cache = Collections.synchronizedMap(new LinkedHashMap<Integer, Organisation>(30));
	this.resetCache();
    }

    public void resetCache() throws SQLException
    {
	cache.clear();

	// load all organisations into cache
	Statement select = learnweb.getConnection().createStatement();
	ResultSet rs = select.executeQuery("SELECT " + COLUMNS + " FROM lw_organisation ORDER BY title");
	while(rs.next())
	    cache.put(rs.getInt("organisation_id"), new Organisation(rs));
	select.close();
    }

    /**
     * Returns a list of all Organisations
     * 
     * @return The collection is unmodifiable
     */
    public Collection<Organisation> getOrganisationsAll()
    {
	return Collections.unmodifiableCollection(cache.values());
    }

    /**
     * Get an Organisation by her id
     * 
     * @param id
     * @return null if not found
     */
    public Organisation getOrganisationById(int id)
    {
	return cache.get(id);
    }

    /**
     * Get an Organisation by her title
     * 
     * @param title
     * @return null if not found
     */
    public Organisation getOrganisationByTitle(String title)
    {
	for(Organisation org : cache.values())
	{
	    if(org.getTitle().equalsIgnoreCase(title))
		return org;
	}

	return null;
    }

    /**
     * Saves the organisation to the database.
     * If the organisation is not yet stored at the database, a new record will be created and the returned organisation contains the new id.
     * 
     * @param organisation
     * @return
     * @throws SQLException
     */
    public Organisation save(Organisation organisation) throws SQLException
    {
	if(organisation.getId() < 0) // the organisation is not yet stored at the database 
	{ // we have to get a new id from the groupmanager
	    Group group = new Group();
	    group.setTitle(organisation.getTitle());
	    group.setDescription("Organisation");
	    group.setSubgroupLabel("Courses");
	    learnweb.getGroupManager().save(group);
	    learnweb.getGroupManager().deleteGroup(group.getId());
	    organisation.setId(group.getId());

	    cache.put(organisation.getId(), organisation);
	}

	PreparedStatement replace = learnweb.getConnection().prepareStatement("REPLACE INTO `lw_organisation` (" + COLUMNS + ") VALUES (?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);

	if(organisation.getId() < 0) // the organisation is not yet stored at the database 
	    replace.setNull(1, java.sql.Types.INTEGER);
	else
	    replace.setInt(1, organisation.getId());
	replace.setString(2, organisation.getTitle());
	replace.setString(3, organisation.getLogo());
	replace.setString(4, organisation.getWelcomePage());
	replace.setString(5, organisation.getWelcomeMessage());
	replace.setLong(6, organisation.getOptions()[0]);
	replace.executeUpdate();

	if(organisation.getId() < 0) // get the assigned id
	{
	    ResultSet rs = replace.getGeneratedKeys();
	    if(!rs.next())
		throw new SQLException("database error: no id generated");
	    organisation.setId(rs.getInt(1));
	    organisation = cache.put(organisation.getId(), organisation); // add the new organisation to the cache
	}
	replace.close();

	return organisation;
    }

    /* 
     public static void main(String[] args) throws SQLException
     {
    Learnweb lw = Learnweb.getInstance();
    OrganisationManagerInterface om = lw.getOrganisationManager();

    PreparedStatement update1 = lw.getConnection().prepareStatement("update lw_course set organisation_id = ? where organisation_id = ?");
    PreparedStatement update2 = lw.getConnection().prepareStatement("update lw_user set organisation_id = ? where organisation_id = ?");
    PreparedStatement delete = lw.getConnection().prepareStatement("DELETE FROM `lw_organisation` WHERE organisation_id = ?");

    System.out.println("Edit organisation ids");
    for(Organisation o : om.getOrganisationsAll())
    {
        int oldId = o.getId();
        o.setId(-1);
        om.save(o);
        int newId = o.getId();
        System.out.println("old id: " + oldId + " - new id: " + newId);

        update1.setInt(1, newId);
        update1.setInt(2, oldId);
        update1.executeUpdate();

        update2.setInt(1, newId);
        update2.setInt(2, oldId);
        update2.executeUpdate();

        delete.setInt(1, oldId);
        delete.executeUpdate();
    }
    update1.close();
    update2.close();

    update1 = lw.getConnection().prepareStatement("update lw_user_course set course_id = ? where course_id = ?");
    update2 = lw.getConnection().prepareStatement("update lw_group set course_id = ? where course_id = ?");
    delete = lw.getConnection().prepareStatement("DELETE FROM `lw_course` WHERE course_id = ?");

    CourseManager cm = lw.getCourseManager();
    System.out.println("Edit course ids");
    for(Course c : cm.getCoursesAll())
    {
        int oldId = c.getId();
        c.setId(-1);
        cm.save(c);
        int newId = c.getId();
        System.out.println("old id: " + oldId + " - new id: " + newId);

        update1.setInt(1, newId);
        update1.setInt(2, oldId);
        update1.executeUpdate();

        update2.setInt(1, newId);
        update2.setInt(2, oldId);
        update2.executeUpdate();

        delete.setInt(1, oldId);
        delete.executeUpdate();
    }
    update1.close();
    update2.close();
    delete.close();
     }
     */
}
