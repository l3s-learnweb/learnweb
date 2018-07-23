package de.l3s.learnweb.resource.yellMetadata;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.Resource;

public class CategoryTree implements Serializable
{
    private static final long serialVersionUID = -453437192920100110L;
    private static final Logger log = Logger.getLogger(CategoryTree.class);

    private List<CategoryTop> uniqueTops = new ArrayList<>();
    private List<CategoryMiddle> uniqueMids = new ArrayList<>();
    private List<CategoryBottom> uniqueBots = new ArrayList<>();
    private List<String> uTops = new ArrayList<>();
    private List<String> uMids = new ArrayList<>();
    private List<String> uBots = new ArrayList<>();

    public CategoryTree()
    {

    }

    public CategoryTree(List<Resource> resources)
    {
        if(resources.size() > 0)
        {
            try
            {
                populateCatTree(resources);
            }
            catch(SQLException e)
            {
                log.fatal("populating category tree failed");
            }
        }
    }

    private void populateCatTree(List<Resource> resources) throws SQLException
    {
        // using the given resources, i get unique top, middle, bottom categories
        // this method is called only when group resources are not empty 

        CategoryManager cm = Learnweb.getInstance().getCategoryManager();

        for(Resource resource : resources)
        {
            if(resource.getExtendedMetadata().getCategories() != null)
            {
                for(String category : resource.getExtendedMetadata().getCategories())
                {
                    String[] categoryParts = category.split("/");

                    //prepare unique top categories
                    if (categoryParts.length >= 1 && !categoryParts[0].equals("x")) {
                        if(uTops.size() == 0 || !uTops.contains(categoryParts[0]))
                        {
                            uTops.add(categoryParts[0]);
                        }
                    }

                    //prepare unique middle categories
                    if (categoryParts.length >= 2 && !categoryParts[1].equals("x")) {
                        if(uMids.size() == 0 || !uMids.contains(categoryParts[1]))
                        {
                            uMids.add(categoryParts[1]);
                        }
                    }

                    //prepare unique bottom categories
                    if (categoryParts.length >= 3 && !categoryParts[2].equals("x")) {
                        if(uBots.size() == 0 || !uBots.contains(categoryParts[2]))
                        {
                            uBots.add(categoryParts[2]);
                        }
                    }
                }
            }
        }

        //using uTops, I get the list of uniqueTopCategories
        for(String uTop : uTops)
        {
            CategoryTop catTop = cm.getTopCategoryByName(uTop);
            if(catTop != null)
            {
                uniqueTops.add(catTop);
            }
            else
            {
                log.fatal("getting top category with given topcatname failed");
            }
        }

        //using uMids, I get the list of uniqueMidCategories
        for(String uMid : uMids)
        {
            CategoryMiddle catMid = cm.getMiddleCategoryByName(uMid);
            if(catMid != null)
            {
                uniqueMids.add(catMid);
            }
        }

        //using uBots, I get the list of uniqueBotCategories
        for(String uBot : uBots)
        {
            CategoryBottom catBot = cm.getBottomCategoryByName(uBot);
            if(catBot != null)
            {
                uniqueBots.add(catBot);
            }
        }
    }

    public List<String> getuTops()
    {
        return uTops;
    }

    public void setuTops(List<String> uTops)
    {
        this.uTops = uTops;
    }

    public List<String> getuMids()
    {
        return uMids;
    }

    public void setuMids(List<String> uMids)
    {
        this.uMids = uMids;
    }

    public List<String> getuBots()
    {
        return uBots;
    }

    public void setuBots(List<String> uBots)
    {
        this.uBots = uBots;
    }

    public List<CategoryTop> getUniqueTops()
    {
        return uniqueTops;
    }

    public void setUniqueTops(List<CategoryTop> uniqueTops)
    {
        this.uniqueTops = uniqueTops;
    }

    public List<CategoryMiddle> getUniqueMids()
    {
        return uniqueMids;
    }

    public void setUniqueMids(List<CategoryMiddle> uniqueMids)
    {
        this.uniqueMids = uniqueMids;
    }

    public List<CategoryBottom> getUniqueBots()
    {
        return uniqueBots;
    }

    public void setUniqueBots(List<CategoryBottom> uniqueBots)
    {
        this.uniqueBots = uniqueBots;
    }

}
