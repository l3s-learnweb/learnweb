package de.l3s.learnweb.rm;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;

public class PurposeManager
{
    private final static Logger log = Logger.getLogger(PurposeManager.class);

    private final static String COLUMNS = "purpose_id, purpose_name";
    private Learnweb learnweb;

    public PurposeManager(Learnweb learnweb) throws SQLException
    {
        super();
        Properties properties = learnweb.getProperties();
        // int userCacheSize = Integer.parseInt(properties.getProperty("USER_CACHE"));

        this.learnweb = learnweb;
        /* this.cache = userCacheSize == 0 ? new DummyCache<User>() : new Cache<User>(userCacheSize);*/
    }

    /**
     * returns a list of all purposes with a given resource id
     * 
     * @return
     * @throws SQLException
     */

    public List<Purpose> getPurposesByResourceId(int resourceId) throws SQLException
    {
        List<Purpose> purposes = new LinkedList<Purpose>();
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM `lw_rm_purpose` JOIN lw_resource_purpose USING(purpose_id) WHERE resource_id = ? AND deleted = 0 ORDER BY purpose_name");
        select.setInt(1, resourceId);
        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            purposes.add(createPurpose(rs));
        }
        select.close();

        return purposes;
    }

    public List<String> getPurposeNamesByResourceId(int resourceId) throws SQLException
    {
        List<String> purposes = new LinkedList<String>();
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM `lw_rm_purpose` JOIN lw_resource_purpose USING(purpose_id) WHERE resource_id = ? AND deleted = 0 ORDER BY purpose_name");
        select.setInt(1, resourceId);
        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            purposes.add(rs.getString("purpose_name"));
        }
        select.close();

        return purposes;
    }

    /**
     * returns a list of all langlevels
     * 
     * @return
     * @throws SQLException
     */

    public List<Purpose> getPurposes() throws SQLException
    {
        List<Purpose> purposes = new LinkedList<Purpose>();
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM `lw_rm_purpose` ORDER BY purpose_name");
        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            purposes.add(createPurpose(rs));
        }
        select.close();

        return purposes;
    }

    public Purpose getPurpose(int purposeId) throws SQLException
    {
        Purpose purpose = new Purpose();
        if(purposeId == 0)
            return null;
        else if(purposeId < 1)
            new IllegalArgumentException("invalid purpose id was requested: " + purposeId).printStackTrace();

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM `lw_rm_purpose` WHERE langlevel_id = ?");
        select.setInt(1, purposeId);
        ResultSet rs = select.executeQuery();

        if(!rs.next())
        {
            new IllegalArgumentException("invalid purpose id was requested: " + purposeId).printStackTrace();
            return null;
        }
        purpose = createPurpose(rs);
        select.close();

        log.debug("Get purpose " + purpose.getPurpose_name() + " from db");

        return purpose;
    }

    public int getPurposeIdByPurposename(String purposename) throws SQLException
    {
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT purpose_id FROM `lw_rm_purpose` WHERE purpose_name = ?");
        select.setString(1, purposename);
        ResultSet rs = select.executeQuery();

        if(!rs.next())
        {
            //log.warn("invalid purpose name was requested: " + purposename);
            return -1;
        }
        int purposeId = rs.getInt("purpose_id");
        select.close();

        return purposeId;
    }

    @SuppressWarnings("unchecked")
    private Purpose createPurpose(ResultSet rs) throws SQLException
    {
        int purposeId = rs.getInt("purpose_id");
        Purpose purpose = new Purpose();

        purpose.setId(rs.getInt("purpose_id"));
        purpose.setPurpose_name(rs.getString("purpose_name"));

        return purpose;
    }
}
