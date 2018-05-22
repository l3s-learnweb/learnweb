package de.l3s.learnweb.resource.yellMetadata;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;

public class PurposeManager
{
    private final static Logger log = Logger.getLogger(PurposeManager.class);

    private final static String COLUMNS = "purpose_id, purpose_name";
    private Learnweb learnweb;

    public PurposeManager(Learnweb learnweb) throws SQLException
    {
        this.learnweb = learnweb;
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
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM `lw_rm_purpose` JOIN lw_resource_purpose USING(purpose_id) WHERE resource_id = ?  ORDER BY purpose_name");
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
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM `lw_rm_purpose` JOIN lw_resource_purpose USING(purpose_id) WHERE resource_id = ?  ORDER BY purpose_id");
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
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM `lw_rm_purpose` ORDER BY purpose_id");
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

        log.debug("Get purpose " + purpose.getName() + " from db");

        return purpose;
    }

    public Purpose addPurpose(String purposeName) throws SQLException
    {
        Purpose purpose = new Purpose();
        purpose.setName(purposeName);

        try(PreparedStatement replace = learnweb.getConnection().prepareStatement("INSERT INTO `lw_rm_purpose` (purpose_name) VALUES (?)", Statement.RETURN_GENERATED_KEYS);)
        {
            replace.setString(1, purposeName);
            replace.executeUpdate();

            // get the assigned id
            ResultSet rs = replace.getGeneratedKeys();
            if(!rs.next())
                throw new SQLException("database error: no id generated");
            purpose.setId(rs.getInt(1));

            return purpose;
        }
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

    private Purpose createPurpose(ResultSet rs) throws SQLException
    {
        Purpose purpose = new Purpose();
        purpose.setId(rs.getInt("purpose_id"));
        purpose.setName(rs.getString("purpose_name"));

        return purpose;
    }
}
