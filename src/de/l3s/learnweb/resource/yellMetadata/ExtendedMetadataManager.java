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
        List<String> authors = new LinkedList<String>();
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
                    if(a == rs.getString("author"))
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
        //get mtype (maybe not necessary if added directly to resource) res.mtype
        //get msource (maybe not necessary if added directly to resource) res.msource
        //get language (maybe not necessary) res.language

        //get target learners
        List<String> tlearners = new LinkedList<String>();
        Map<String, Integer> tcount = new HashMap();

        tlearners = learnweb.getAudienceManager().getAudienceNamesByResourceId(resourceId);
        tcount = sortMetadataValueCount(tlearners);
        eMetadata.setTargetCount(tcount);
        eMetadata.setTargets(tlearners);

        //get purposes
        List<String> purposes = new LinkedList<String>();
        Map<String, Integer> pcount = new HashMap();

        purposes = learnweb.getPurposeManager().getPurposeNamesByResourceId(resourceId);
        pcount = sortMetadataValueCount(purposes);
        eMetadata.setPurposeCount(pcount);
        eMetadata.setPurposes(purposes);

        //lang levels
        List<String> langlevels = new LinkedList<String>();
        Map<String, Integer> lcount = new HashMap();

        langlevels = learnweb.getLanglevelManager().getLanglevelNamesByResourceId(resourceId);
        lcount = sortMetadataValueCount(langlevels);
        eMetadata.setLevelCount(lcount);
        eMetadata.setLevels(langlevels);

        String lc = "";
        for(Map.Entry<String, Integer> entry : lcount.entrySet())
        {
            lc += entry.getKey() + "[" + entry.getValue().toString() + "] ";
        }

        eMetadata.setlCount(lc);

        //get categories
        List<String> categories = new LinkedList<String>();
        categories = learnweb.getCategoryManager().getCategoryNamesByResourceId(resourceId);
        eMetadata.setCategories(categories);

        return eMetadata;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Map sortMetadataValueCount(List<String> metadatas)
    {

        Map<String, Integer> sorted = new HashMap();
        List<String> tempnames = new ArrayList<String>();
        int initcount = 1;

        for(String md : metadatas)
        {

            String exist = "no";
            //check if the name exists already
            for(int i = 0; i < tempnames.size(); i++)
            {

                if(md.equalsIgnoreCase(tempnames.get(i)))
                {
                    exist = "yes";
                }
            }

            if(exist.equalsIgnoreCase("no"))
            {
                tempnames.add(md);
                sorted.put(md, initcount);
            }

            if(exist.equalsIgnoreCase("yes")) //if md already exists, only increase the count
            {
                sorted.put(md, sorted.get(md) + 1);

            }
        }

        return sorted;
    }

}
