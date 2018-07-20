package de.l3s.learnweb.resource.yellMetadata;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;

public class AudienceManager
{
    private final static Logger log = Logger.getLogger(AudienceManager.class);

    private final static String COLUMNS = "audience_id, audience_name";
    private Learnweb learnweb;

    public AudienceManager(Learnweb learnweb) throws SQLException
    {
        this.learnweb = learnweb;
    }

    /**
     * returns a list of all audiences with a given resource id
     *
     * @return
     * @throws SQLException
     */

    public List<Audience> getAudiencesByResourceId(int resourceId) throws SQLException
    {
        List<Audience> audiences = new LinkedList<>();
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM `lw_rm_audience` JOIN lw_resource_audience USING(audience_id) WHERE resource_id = ? ORDER BY audience_id");
        select.setInt(1, resourceId);
        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            audiences.add(createAudience(rs));
        }
        select.close();

        return audiences;
    }

    public List<String> getAudienceNamesByResourceId(int resourceId) throws SQLException
    {
        List<String> audiences = new LinkedList<>();
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM `lw_rm_audience` JOIN lw_resource_audience USING(audience_id) WHERE resource_id = ? ORDER BY audience_id");
        select.setInt(1, resourceId);
        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            audiences.add(rs.getString("audience_name"));
        }
        select.close();

        return audiences;
    }

    /**
     * returns a list of all audiences
     *
     * @return
     * @throws SQLException
     */

    public List<Audience> getAudiences() throws SQLException
    {
        List<Audience> audiences = new LinkedList<>();
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM `lw_rm_audience` ORDER BY audience_id");
        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            audiences.add(createAudience(rs));
        }
        select.close();

        return audiences;
    }

    public Audience getAudience(int audienceId) throws SQLException
    {
        if(audienceId == 0)
            return null;
        else if(audienceId < 1)
            new IllegalArgumentException("invalid user id was requested: " + audienceId).printStackTrace();

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM `lw_rm_audience` WHERE audience_id = ?");
        select.setInt(1, audienceId);
        ResultSet rs = select.executeQuery();

        if(!rs.next())
        {
            new IllegalArgumentException("invalid audience id was requested: " + audienceId).printStackTrace();
            return null; //throw new IllegalArgumentException("invalid audience id");
        }
        Audience audience = createAudience(rs);
        select.close();

        log.debug("Get audience " + audience.getAudience_name() + " from db");

        return audience;
    }

    public int getAudienceIdByAudienceName(String audienceName) throws SQLException
    {
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT audience_id FROM `lw_rm_audience` WHERE audience_name = ?");
        select.setString(1, audienceName);
        ResultSet rs = select.executeQuery();

        if(!rs.next())
        {
            //log.warn("invalid audience name was requested: " + audiencename);
            return -1;
        }
        int audienceId = rs.getInt("audience_id");
        select.close();

        return audienceId;
    }

    private Audience createAudience(ResultSet rs) throws SQLException
    {
        Audience audience = new Audience();
        audience.setId(rs.getInt("audience_id"));
        audience.setAudience_name(rs.getString("audience_name"));

        return audience;
    }

}
