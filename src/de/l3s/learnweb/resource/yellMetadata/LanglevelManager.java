package de.l3s.learnweb.resource.yellMetadata;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;

public class LanglevelManager
{

    private final static Logger log = Logger.getLogger(LanglevelManager.class);

    private final static String COLUMNS = "langlevel_id, langlevel_name";
    private Learnweb learnweb;

    public LanglevelManager(Learnweb learnweb) throws SQLException
    {
        this.learnweb = learnweb;
    }

    /**
     * returns a list of all langlevels with a given resource id
     *
     * @return
     * @throws SQLException
     */

    public List<Langlevel> getLanglevelsByResourceId(int resourceId) throws SQLException
    {
        List<Langlevel> langlevels = new LinkedList<Langlevel>();
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM `lw_rm_langlevel` JOIN lw_resource_langlevel USING(langlevel_id) WHERE resource_id = ? ORDER BY langlevel_id");
        select.setInt(1, resourceId);
        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            langlevels.add(createLanglevel(rs));
        }
        select.close();

        return langlevels;
    }

    public List<String> getLanglevelNamesByResourceId(int resourceId) throws SQLException
    {
        List<String> langlevels = new LinkedList<String>();
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM `lw_rm_langlevel` JOIN lw_resource_langlevel USING(langlevel_id) WHERE resource_id = ? ORDER BY langlevel_id");
        select.setInt(1, resourceId);
        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            langlevels.add(rs.getString("langlevel_name"));
        }
        select.close();

        return langlevels;
    }

    /**
     * returns a list of all langlevels
     *
     * @return
     * @throws SQLException
     */

    public List<Langlevel> getLanglevels() throws SQLException
    {
        List<Langlevel> langlevels = new LinkedList<Langlevel>();
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM `lw_rm_langlevel` ORDER BY langlevel_name");
        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            langlevels.add(createLanglevel(rs));
        }
        select.close();

        return langlevels;
    }

    public Langlevel getLanglevel(int langlevelId) throws SQLException
    {
        Langlevel langlevel = new Langlevel();
        if(langlevelId == 0)
            return null;
        else if(langlevelId < 1)
            new IllegalArgumentException("invalid langlevel id was requested: " + langlevelId).printStackTrace();

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM `lw_rm_langlevel` WHERE langlevel_id = ?");
        select.setInt(1, langlevelId);
        ResultSet rs = select.executeQuery();

        if(!rs.next())
        {
            new IllegalArgumentException("invalid langlevel id was requested: " + langlevelId).printStackTrace();
            return null;
        }
        langlevel = createLanglevel(rs);
        select.close();

        log.debug("Get langlevel " + langlevel.getLanglevel_name() + " from db");

        return langlevel;
    }

    public int getLanglevelIdByLanglevelname(String langlevelname) throws SQLException
    {
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT langlevel_id FROM `lw_rm_langlevel` WHERE langlevel_name = ?");
        select.setString(1, langlevelname);
        ResultSet rs = select.executeQuery();

        if(!rs.next())
        {
            //log.warn("invalid langlevel name was requested: " + langlevelname);
            return -1;
        }
        int langlevelId = rs.getInt("langlevel_id");
        select.close();

        return langlevelId;
    }

    private Langlevel createLanglevel(ResultSet rs) throws SQLException
    {
        Langlevel langlevel = new Langlevel();
        langlevel.setId(rs.getInt("langlevel_id"));
        langlevel.setLanglevel_name(rs.getString("langlevel_name"));

        return langlevel;
    }
}
