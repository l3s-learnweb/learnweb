package de.l3s.learnweb.resource.yellMetadata;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.Resource;

//handles extended metadata search and display: similar to resource but only deal with extended metadata of resources.

public class ExtendedMetadataManager
{
    //private final static Logger log = Logger.getLogger(ExtendedMetadataManager.class);

    private final static String COLUMNS_AUTHOR = "author";
    private Learnweb learnweb;

    public ExtendedMetadataManager(Learnweb learnweb) throws SQLException
    {
        this.learnweb = learnweb;
    }

    //get the list of authors for the given list of resources: used for display in author filter
    public List<String> getAuthorsByResourceIds(List<Resource> resources) throws SQLException
    {
        List<String> authors = new LinkedList<>();
        //here iterate resources and do select for each resource
        for(Resource r : resources)
        {
            int resourceId = r.getId();

            PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS_AUTHOR + " FROM `lw_resource` WHERE resource_id = ? AND deleted = 0 ORDER BY author");
            select.setInt(1, resourceId);
            ResultSet rs = select.executeQuery();
            while(rs.next())
            { //if the author does not exist already
                String exist = "false";
                for(String a : authors) // super stupid containers() implementation
                {
                    if(a.equals(rs.getString("author")))
                    {
                        exist = "true";
                    }
                }

                //add the author only if it does not yet exist
                if(exist.equalsIgnoreCase("false"))
                {
                    authors.add(rs.getString("author"));
                }
            }
            select.close();
        }

        return authors;
    }

    //get the list of media sources for the given list of resources: used for display for source filter

    //get the list of audience for the given list of resources: used for display for target learner filter

    //get the list of language for the given list of resources: used for display for language filter

    //get the list of language levels for the given list of resources: used for display for level filter

    //get the list of purposes for the given list of resources: used for display for purpose filter

    //get the entended metadata for the given resource id
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ExtendedMetadata getMetadataByResourceId(int resourceId) throws SQLException
    {
        ExtendedMetadata eMetadata = new ExtendedMetadata();

        //get author? (not necessary) res.author
        //get language (maybe not necessary) res.language

        //get target learners
        List<String> tlearners = learnweb.getAudienceManager().getAudienceNamesByResourceId(resourceId);
        Map<String, Integer> tcount = sortMetadataValueCount(tlearners);
        eMetadata.setTargetCount(tcount);
        eMetadata.setTargets(tlearners);

        //get purposes
        List<String> purposes = learnweb.getPurposeManager().getPurposeNamesByResourceId(resourceId);
        Map<String, Integer> pcount = sortMetadataValueCount(purposes);
        eMetadata.setPurposeCount(pcount);
        eMetadata.setPurposes(purposes);

        //lang levels
        List<String> langlevels = learnweb.getLangLevelManager().getLangLevelNamesByResourceId(resourceId);
        Map<String, Integer> lcount = sortMetadataValueCount(langlevels);
        eMetadata.setLevelCount(lcount);
        eMetadata.setLevels(langlevels);

        StringBuilder lc = new StringBuilder();
        for(Map.Entry<String, Integer> entry : lcount.entrySet())
        {
            lc.append(entry.getKey()).append("[").append(entry.getValue().toString()).append("] ");
        }

        eMetadata.setlCount(lc.toString());
        return eMetadata;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Map sortMetadataValueCount(List<String> metadatas)
    {

        Map<String, Integer> sorted = new HashMap();
        List<String> tempNames = new ArrayList<>();
        int initCount = 1;

        for(String md : metadatas)
        {

            String exist = "no";
            //check if the name exists already
            for(String tempName : tempNames)
            {

                if(md.equalsIgnoreCase(tempName))
                {
                    exist = "yes";
                }
            }

            if(exist.equalsIgnoreCase("no"))
            {
                tempNames.add(md);
                sorted.put(md, initCount);
            }

            if(exist.equalsIgnoreCase("yes")) //if md already exists, only increase the count
            {
                sorted.put(md, sorted.get(md) + 1);

            }
        }

        return sorted;
    }

}
