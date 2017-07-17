package de.l3s.learnweb.rm;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.Resource;

//handles extended metadata search and display: similar to resource but only deal with extended metadata of resources. 

public class ExtendedMetadataManager
{
    private final static Logger log = Logger.getLogger(ExtendedMetadataManager.class);

    private final static String COLUMNS_AUTHOR = "author";
    private Learnweb learnweb;
    private ExtendedMetadata extendedMetadata = new ExtendedMetadata();

    public ExtendedMetadataManager(Learnweb learnweb) throws SQLException
    {
        super();
        Properties properties = learnweb.getProperties();
        // int userCacheSize = Integer.parseInt(properties.getProperty("USER_CACHE"));

        this.learnweb = learnweb;
        /* this.cache = userCacheSize == 0 ? new DummyCache<User>() : new Cache<User>(userCacheSize);*/
    }

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

                authors.add(rs.getString("author"));
            }
            select.close();
        }

        return authors;
    }

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

        tlearners = Learnweb.getInstance().getAudienceManager().getAudienceNamesByResourceId(resourceId);
        tcount = sortMetadataValueCount(tlearners);
        eMetadata.setTargetCount(tcount);

        //get purposes
        List<String> purposes = new LinkedList<String>();
        Map<String, Integer> pcount = new HashMap();

        purposes = Learnweb.getInstance().getPurposeManager().getPurposeNamesByResourceId(resourceId);
        pcount = sortMetadataValueCount(purposes);
        eMetadata.setPurposeCount(pcount);

        //lang levels
        List<String> langlevels = new LinkedList<String>();
        Map<String, Integer> lcount = new HashMap();

        langlevels = Learnweb.getInstance().getLanglevelManager().getLanglevelNamesByResourceId(resourceId);
        lcount = sortMetadataValueCount(langlevels);
        eMetadata.setLevelCount(lcount);

        //get categories 
        List<String> categories = new LinkedList<String>();
        categories = Learnweb.getInstance().getCategoryManager().getCategoryNamesByResourceId(resourceId);
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

            //check if the name exists already
            String exist = "no";
            for(String tn : tempnames)
            {
                if(md == tn)
                {
                    exist = "yes";
                }
            }

            if(exist.equalsIgnoreCase("no"))
            {
                sorted.put(md, initcount);
                tempnames.add(md);
            }
            else //if md already exists, only increase the count
            {
                sorted.put(md, sorted.get(md) + 1);

            }

        }

        return sorted;
    }

}
