package de.l3s.learnweb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * DAO for the Organisation class.
 * Because there are only a few organisations we keep them all in memory
 * 
 * @author Philipp
 * 
 */
public class OrganisationManager
{
    public final static Logger log = Logger.getLogger(OrganisationManager.class);
    // if you change this, you have to change the constructor of Organisation too
    private final static String COLUMNS = "organisation_id, title, logo, welcome_page, welcome_message, default_search_text, default_search_image, default_search_video, default_language, options_field1";

    private Learnweb learnweb;
    private Map<Integer, Organisation> cache;

    protected OrganisationManager(Learnweb learnweb) throws SQLException
    {
        super();
        this.learnweb = learnweb;
        this.cache = Collections.synchronizedMap(new LinkedHashMap<Integer, Organisation>(30));
        this.resetCache();
    }

    public void resetCache() throws SQLException
    {
        cache.clear();

        // load all organizations into cache
        try(Statement select = learnweb.getConnection().createStatement())
        {
            ResultSet rs = select.executeQuery("SELECT " + COLUMNS + " FROM lw_organisation ORDER BY title");
            while(rs.next())
            {
                Organisation organisation = new Organisation(rs);
                cache.put(rs.getInt("organisation_id"), organisation);
            }
        }
    }

    /**
     * 
     * @return number of cached objects
     */
    public int getCacheSize()
    {
        return cache.size();
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
            learnweb.getGroupManager().save(group);
            learnweb.getGroupManager().deleteGroup(group);
            organisation.setId(group.getId());

            cache.put(organisation.getId(), organisation);
        }

        PreparedStatement replace = learnweb.getConnection().prepareStatement("REPLACE INTO `lw_organisation` (" + COLUMNS + ") VALUES (?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);

        if(organisation.getId() < 0) // the organisation is not yet stored at the database 
            replace.setNull(1, java.sql.Types.INTEGER);
        else
            replace.setInt(1, organisation.getId());
        replace.setString(2, organisation.getTitle());
        replace.setString(3, organisation.getLogo());
        replace.setString(4, organisation.getWelcomePage());
        replace.setString(5, organisation.getWelcomeMessage());
        replace.setString(6, organisation.getDefaultSearchServiceText().name());
        replace.setString(7, organisation.getDefaultSearchServiceImage().name());
        replace.setString(8, organisation.getDefaultSearchServiceVideo().name());
        replace.setString(9, organisation.getDefaultLanguage());
        replace.setLong(10, organisation.getOptions()[0]);
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
}
