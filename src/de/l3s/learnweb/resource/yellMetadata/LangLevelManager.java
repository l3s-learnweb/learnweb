package de.l3s.learnweb.resource.yellMetadata;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;

public class LangLevelManager
{

    private final static Logger log = Logger.getLogger(LangLevelManager.class);

    private final static String COLUMNS = "langlevel_id, langlevel_name";
    private Learnweb learnweb;

    public LangLevelManager(Learnweb learnweb) throws SQLException
    {
        this.learnweb = learnweb;
    }

    /**
     * returns a list of all lang levels with a given resource id
     *
     * @return
     * @throws SQLException
     */

    public List<LangLevel> getLangLevelsByResourceId(int resourceId) throws SQLException
    {
        List<LangLevel> langLevels = new LinkedList<LangLevel>();
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM `lw_rm_langlevel` JOIN lw_resource_langlevel USING(langlevel_id) WHERE resource_id = ? ORDER BY langlevel_id");
        select.setInt(1, resourceId);
        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            langLevels.add(createLangLevel(rs));
        }
        select.close();

        return langLevels;
    }

    public List<String> getLangLevelNamesByResourceId(int resourceId) throws SQLException
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
     * returns a list of all lang levels
     *
     * @return
     * @throws SQLException
     */

    public List<LangLevel> getLangLevels() throws SQLException
    {
        List<LangLevel> langLevels = new LinkedList<LangLevel>();
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM `lw_rm_langlevel` ORDER BY langlevel_name");
        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            langLevels.add(createLangLevel(rs));
        }
        select.close();

        return langLevels;
    }

    public LangLevel getLangLevel(int langlevelId) throws SQLException
    {
        LangLevel langLevel = new LangLevel();
        if(langlevelId == 0)
            return null;
        else if(langlevelId < 1)
            new IllegalArgumentException("invalid langLevel id was requested: " + langlevelId).printStackTrace();

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM `lw_rm_langlevel` WHERE langlevel_id = ?");
        select.setInt(1, langlevelId);
        ResultSet rs = select.executeQuery();

        if(!rs.next())
        {
            new IllegalArgumentException("invalid langLevel id was requested: " + langlevelId).printStackTrace();
            return null;
        }
        langLevel = createLangLevel(rs);
        select.close();

        log.debug("Get langLevel " + langLevel.getLangLevelName() + " from db");

        return langLevel;
    }

    public int getLangLevelIdByLangLevelName(String langLevelName) throws SQLException
    {
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT langlevel_id FROM `lw_rm_langlevel` WHERE langlevel_name = ?");
        select.setString(1, langLevelName);
        ResultSet rs = select.executeQuery();

        if(!rs.next())
        {
            //log.warn("invalid langlevel name was requested: " + langlevelname);
            return -1;
        }
        int langLevelId = rs.getInt("langlevel_id");
        select.close();

        return langLevelId;
    }

    private LangLevel createLangLevel(ResultSet rs) throws SQLException
    {
        LangLevel langLevel = new LangLevel();
        langLevel.setId(rs.getInt("langlevel_id"));
        langLevel.setLangLevelName(rs.getString("langlevel_name"));

        return langLevel;
    }
}
