package de.l3s.learnweb.tasks;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceManager;

/**
 * Reeds through all undeleted resources and performs arbitrary tests
 *
 * @author Kemkes
 *
 */
public final class ConvertChloesResources
{
    private final static Logger log = Logger.getLogger(ConvertChloesResources.class);

    public static void main(String[] args) throws Exception
    {
        new ConvertChloesResources();
    }

    private Learnweb learnweb;
    private ResourceManager resourceManager;

    private ConvertChloesResources() throws SQLException, ClassNotFoundException
    {
        learnweb = Learnweb.createInstance(null);

        resourceManager = learnweb.getResourceManager();
        resourceManager.setReindexMode(true);

        convertLanguageLevel();
        convertPurposeOfUse();
        convertTargetLearner();

        learnweb.onDestroy();
    }

    private void saveMetadata(int resourceId, String key, String[] values) throws SQLException
    {
        log.debug("saving " + key + " for " + resourceId);
        Resource resource = resourceManager.getResource(resourceId);

        if (resource == null)
        {
            log.info("Resource is not exists!");
            return;
        }

        resource.getMetadataMultiValue().put(key, values);
        resource.save();
    }

    private void convertLanguageLevel() throws SQLException
    {
        log.debug("convertLanguageLevel");
        try(PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT resource_id, GROUP_CONCAT(DISTINCT b.langlevel_name) AS csvalues FROM lw_resource_langlevel a INNER JOIN lw_rm_langlevel b ON a.langlevel_id = b.langlevel_id GROUP BY resource_id"))
        {
            ResultSet rs = select.executeQuery();
            while(rs.next())
            {
                int resourceId = rs.getInt("resource_id");
                String[] values = rs.getString("csvalues").split(",");
                saveMetadata(resourceId, "language_level", values);
            }
            rs.close();
        }
    }

    private void convertPurposeOfUse() throws SQLException
    {
        log.debug("convertPurposeOfUse");
        try(PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT resource_id, GROUP_CONCAT(DISTINCT b.purpose_name) AS csvalues FROM lw_resource_purpose a INNER JOIN lw_rm_purpose b ON a.purpose_id = b.purpose_id GROUP BY resource_id"))
        {
            ResultSet rs = select.executeQuery();
            while(rs.next())
            {
                int resourceId = rs.getInt("resource_id");
                String[] values = rs.getString("csvalues").split(",");
                saveMetadata(resourceId, "yell_purpose", values);
            }
            rs.close();
        }
    }

    private void convertTargetLearner() throws SQLException
    {
        log.debug("convertTargetLearner");
        try(PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT resource_id, GROUP_CONCAT(DISTINCT b.audience_name) AS csvalues FROM lw_resource_audience a INNER JOIN lw_rm_audience b ON a.audience_id = b.audience_id GROUP BY resource_id"))
        {
            ResultSet rs = select.executeQuery();
            while(rs.next())
            {
                int resourceId = rs.getInt("resource_id");
                String[] values = rs.getString("csvalues").split(",");
                saveMetadata(resourceId, "yell_target", values);
            }
            rs.close();
        }
    }
}
