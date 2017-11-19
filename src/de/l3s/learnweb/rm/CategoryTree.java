package de.l3s.learnweb.rm;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.Resource;

public class CategoryTree implements Serializable
{
    private static final long serialVersionUID = -453437192920100110L;
    private static final Logger log = Logger.getLogger(CategoryTree.class);

    private List<CategoryTop> uniqueTops = new ArrayList<CategoryTop>();
    private List<CategoryMiddle> uniqueMids = new ArrayList<CategoryMiddle>();
    private List<CategoryBottom> uniqueBots = new ArrayList<CategoryBottom>();
    private List<String> uTops = new ArrayList<String>();
    private List<String> uMids = new ArrayList<String>();
    private List<String> uBots = new ArrayList<String>();

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

        for(int i = 0; i < resources.size(); i++)
        {
            if(resources.get(i).getExtendedMetadata().getCategories() != null)
            {
                List<String> rcats = resources.get(i).getExtendedMetadata().getCategories();
                String texist = "false";
                String mexist = "false";
                String bexist = "false";

                for(int j = 0; j < rcats.size(); j++)
                {
                    String[] cat = rcats.get(j).split("/");

                    //prepare unique top categories
                    if(uTops.size() == 0)
                    {
                        uTops.add(cat[0]);
                    }
                    else
                    {
                        //check if it exists or not
                        for(int k = 0; k < uTops.size(); k++)
                        {
                            if(uTops.get(k).equals(cat[0]))
                            {
                                texist = "true";
                            }
                        }

                        if(texist.equalsIgnoreCase("false"))
                        {
                            uTops.add(cat[0]);
                        }
                    }

                    //prepare unique middle categories
                    if(uMids.size() == 0)
                    {
                        if(!(cat[1].equals("x")))
                        {
                            uMids.add(cat[1]);
                        }
                    }
                    else
                    {
                        //check if it exists or not
                        for(int k = 0; k < uMids.size(); k++)
                        {
                            if(uMids.get(k).equals(cat[1]))
                            {
                                mexist = "true";
                            }
                        }

                        if(mexist.equalsIgnoreCase("false"))
                        {
                            if(!(cat[1].equals("x")))
                            {
                                uMids.add(cat[1]);
                            }
                        }
                    }

                    //prepare unique bottom categories
                    if(uBots.size() == 0)
                    {
                        if(!(cat[2].equals("x")))
                        {
                            uBots.add(cat[2]);
                        }
                    }
                    else
                    {
                        //check if it exists or not 
                        for(int k = 0; k < uBots.size(); k++)
                        {
                            if(uBots.get(k).equals(cat[2]))
                            {
                                bexist = "true";
                            }
                        }

                        if(bexist.equalsIgnoreCase("false"))
                        {
                            if(!(cat[2].equals("x")))
                            {
                                uBots.add(cat[2]);
                            }
                        }
                    }
                }
            }
        }

        //using uTops, I get the list of uniqueTopcategories 
        for(int i = 0; i < uTops.size(); i++)
        {
            CategoryTop cattop = new CategoryTop();
            cattop = cm.getTopCategoryByName(uTops.get(i));
            if(cattop != null)
            {
                uniqueTops.add(cattop);
            }
            else
            {
                log.fatal("getting top category with given topcatname failed");
            }
        }

        //using uMids, I get the list of uniqueMidcategories 
        for(int i = 0; i < uMids.size(); i++)
        {
            CategoryMiddle catmid = new CategoryMiddle();
            catmid = cm.getMiddleCategoryByName(uMids.get(i));
            if(catmid != null)
            {
                uniqueMids.add(catmid);
            }
        }

        //using uBots, I get the list of uniqueBotcategories
        for(int i = 0; i < uBots.size(); i++)
        {
            CategoryBottom catbot = new CategoryBottom();
            catbot = cm.getBottomCategoryByName(uBots.get(i));
            if(catbot != null)
            {
                uniqueBots.add(catbot);
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
