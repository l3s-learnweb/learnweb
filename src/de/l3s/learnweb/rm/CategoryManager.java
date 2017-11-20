package de.l3s.learnweb.rm;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;

public class CategoryManager
{
    private final static Logger log = Logger.getLogger(CategoryManager.class);

    private final static String COLUMNS = "cat_top_id, cat_mid_id, cat_bot_id";
    private final static String COLUMNS_TOP = "cat_top_id, cat_top_name";
    private final static String COLUMNS_MID = "cat_mid_id, cat_mid_name, cat_top_id";
    private final static String COLUMNS_BOT = "cat_bot_id, cat_bot_name, cat_mid_id";
    private final static String COLUMNS_CATRESOURCE = "resource_id, cat_top_id, cat_mid_id, cat_bot_id";

    private Learnweb learnweb;

    public CategoryManager(Learnweb learnweb) throws SQLException
    {
        super();
        Properties properties = learnweb.getProperties();
        // int userCacheSize = Integer.parseInt(properties.getProperty("USER_CACHE"));

        this.learnweb = learnweb;
        /* this.cache = userCacheSize == 0 ? new DummyCache<User>() : new Cache<User>(userCacheSize);*/
    }

    //get category_resource for a given resource Id
    public List<CategoryResource> getCategoryResourcesByResourceId(int resourceId) throws SQLException
    {
        List<CategoryResource> cresources = new ArrayList<CategoryResource>();
        List<Category> categories = new LinkedList<Category>();
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS_CATRESOURCE + " FROM `lw_resource_category` WHERE resource_id = ?");
        select.setInt(1, resourceId);
        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            cresources.add(createCategoryResource(rs));
        }
        select.close();

        return cresources;
    }

    //get category list for a given resource ID 
    public List<Category> getCategoriesByResourceId(int resourceId) throws SQLException
    {
        List<Category> categories = new LinkedList<Category>();
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM `lw_resource_category` WHERE resource_id = ? ORDER BY cat_top_id");
        select.setInt(1, resourceId);
        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            categories.add(createCategory(rs));
        }
        select.close();

        return categories;
    }

    public List<String> getCategoryNamesByResourceId(int resourceId) throws SQLException
    {
        List<Category> categories = new LinkedList<Category>();
        List<String> cats = new LinkedList<String>();
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS + " FROM `lw_resource_category` WHERE resource_id = ? ORDER BY cat_top_id");
        select.setInt(1, resourceId);
        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            categories.add(createCategory(rs));
        }
        select.close();

        for(Category cat : categories)
        {
            String catname = cat.getCat();
            cats.add(catname);
        }

        return cats;
    }

    //get category ids given the full category name (top, middle, bottom). Given string needs to be parsed to identify top, middle and bottom with the delimiter "/"
    public List<Integer> getCategoryIdsByCategoryFullName(String fullcatName) throws SQLException
    {
        List<Integer> catids = new ArrayList<Integer>();
        int topId, midId, botId;
        String[] ids = fullcatName.split("/");

        //if the category has only top category e.g. Science
        if(ids.length == 3)
        {
            String topcatName = ids[0];
            String midcatName = "x";
            String botcatName = "x";
            topId = getCategoryTopByName(topcatName);
            midId = getCategoryMiddleByNameAndTopcatId(midcatName, topId);
            botId = getCategoryBottomByNameAndMidcatId(botcatName, midId);

            catids.add(topId);
            catids.add(midId);
            catids.add(botId);
        }
        //category has top and mid category e.g. Science/Biology
        else if(ids.length == 2)
        {
            String topcatName = ids[0];
            String midcatName = ids[1];
            String botcatName = "x";
            topId = getCategoryTopByName(topcatName);
            midId = getCategoryMiddleByNameAndTopcatId(midcatName, topId);
            botId = getCategoryBottomByNameAndMidcatId(botcatName, midId);

            catids.add(topId);
            catids.add(midId);
            catids.add(botId);
        }
        //category has top, mid and bottom category e.g. Science/Biology/Plant
        else if(ids.length == 3)
        {
            String topcatName = ids[0];
            String midcatName = ids[1];
            String botcatName = ids[2];
            topId = getCategoryTopByName(topcatName);
            midId = getCategoryMiddleByNameAndTopcatId(midcatName, topId);
            botId = getCategoryBottomByNameAndMidcatId(botcatName, midId);

            catids.add(topId);
            catids.add(midId);
            catids.add(botId);
        }
        else
        {
            new IllegalArgumentException("invalid full category name was given: " + fullcatName).printStackTrace();
        }

        return catids;
    }

    //get all top categories 
    public List<CategoryTop> getAllTopCategories() throws SQLException
    {
        List<CategoryTop> cattops = new LinkedList<CategoryTop>();
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS_TOP + " FROM `lw_rm_cattop` ORDER BY cat_top_name");
        ResultSet rs = select.executeQuery();
        while(rs.next())
        {
            cattops.add(createCategoryTop(rs));
        }
        select.close();

        return cattops;
    }

    /*do I also need to create queries to retrieve all middle and bottom categories without top or middle ids? */

    //get all middle categories of a given top category. remember to filter out "x"s when displaying
    public List<CategoryMiddle> getAllMiddleCategoriesByCattopID(int cattopId) throws SQLException
    {
        List<CategoryMiddle> catmids = new LinkedList<CategoryMiddle>();
        if(cattopId == 0)
            return null;
        else if(cattopId < 1)
            new IllegalArgumentException("invalid top category id was requested: " + cattopId).printStackTrace();

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS_MID + " FROM `lw_rm_catmid` WHERE cat_top_id = ?");
        select.setInt(1, cattopId);
        ResultSet rs = select.executeQuery();

        while(rs.next())
        {
            catmids.add(createCategoryMiddle(rs));
        }
        select.close();

        return catmids;
    }

    //get all bottom categories given a middle category
    public List<CategoryBottom> getAllBottomCategoriesByCatmidID(int catmidId) throws SQLException
    {
        List<CategoryBottom> catbots = new LinkedList<CategoryBottom>();
        if(catmidId == 0)
            return null;
        else if(catmidId < 1)
            new IllegalArgumentException("invalid mid category id was requested: " + catmidId).printStackTrace();

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS_BOT + " FROM `lw_rm_catbot` WHERE cat_mid_id = ?");
        select.setInt(1, catmidId);
        ResultSet rs = select.executeQuery();

        while(rs.next())
        {
            catbots.add(createCategoryBottom(rs));
        }
        select.close();

        return catbots;
    }

    //get bottom category given the id
    public CategoryBottom getCategoryBottomById(int catbotId) throws SQLException
    {
        CategoryBottom catbot = new CategoryBottom();
        if(catbotId == 0)
            return null;
        else if(catbotId < 1)
            new IllegalArgumentException("invalid bottom category id was requested: " + catbotId).printStackTrace();

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS_BOT + " FROM `lw_rm_catbot` WHERE cat_bot_id = ?");
        select.setInt(1, catbotId);
        ResultSet rs = select.executeQuery();

        if(!rs.next())
        {
            new IllegalArgumentException("invalid cat bottom id was requested: " + catbotId).printStackTrace();
            return null;
        }
        catbot = createCategoryBottom(rs);
        select.close();

        return catbot;
    }

    //get middle category given the id
    public CategoryMiddle getCategoryMiddleById(int catmidId) throws SQLException
    {
        CategoryMiddle catmid = new CategoryMiddle();
        if(catmidId == 0)
            return null;
        else if(catmidId < 1)
            new IllegalArgumentException("invalid middle category id was requested: " + catmidId).printStackTrace();

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS_MID + " FROM `lw_rm_catmid` WHERE cat_mid_id = ?");
        select.setInt(1, catmidId);
        ResultSet rs = select.executeQuery();

        if(!rs.next())
        {
            new IllegalArgumentException("invalid cat middle id was requested: " + catmidId).printStackTrace();
            return null;
        }
        catmid = createCategoryMiddle(rs);
        select.close();

        return catmid;
    }

    //get top category given the id 
    public CategoryTop getCategoryTopById(int cattopId) throws SQLException
    {
        CategoryTop cattop = new CategoryTop();
        if(cattopId == 0)
            return null;
        else if(cattopId < 1)
            new IllegalArgumentException("invalid top category id was requested: " + cattopId).printStackTrace();

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS_TOP + " FROM `lw_rm_cattop` WHERE cat_top_id = ?");
        select.setInt(1, cattopId);
        ResultSet rs = select.executeQuery();

        if(!rs.next())
        {
            new IllegalArgumentException("invalid cat top id was requested: " + cattopId).printStackTrace();
            return null;
        }
        cattop = createCategoryTop(rs);
        select.close();

        return cattop;
    }

    //get bottom category given the name and mid category Id. if it does not exist create one
    public int getCategoryBottomByNameAndMidcatId(String catbotName, int catmidId) throws SQLException
    {
        CategoryBottom catbot = new CategoryBottom();
        int catbotId;
        if(catbotName == null)
        {
            new IllegalArgumentException("invalid bottom category name was requested: " + catbotName).printStackTrace();
        }

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS_BOT + " FROM `lw_rm_catbot` WHERE cat_bot_name =? AND cat_mid_id = ?");
        select.setString(1, catbotName);
        select.setInt(2, catmidId);
        ResultSet rs = select.executeQuery();

        if(!rs.next())
        {
            //TO-DO: create the new bottom category
        }
        else
        {
            catbot = createCategoryBottom(rs);
            select.close();
        }

        return catbot.getId();
    }

    //get middle category given the name and top category Id (if it does not exist, this is an error because middle categories are fixed) 
    public int getCategoryMiddleByNameAndTopcatId(String catmidName, int cattopId) throws SQLException
    {
        CategoryMiddle catmid = new CategoryMiddle();
        int catmidId;

        if(catmidName == null)
        {
            new IllegalArgumentException("invalid middle category name was requested: " + catmidName).printStackTrace();
        }
        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS_MID + " FROM `lw_rm_catmid` WHERE cat_mid_name = ? AND cat_top_id = ?");
        select.setString(1, catmidName);
        select.setInt(2, cattopId);
        ResultSet rs = select.executeQuery();

        if(!rs.next())
        {
            log.debug("invalid cat middle name was requested: " + catmidName, new IllegalArgumentException());
            return -1;
        }
        catmid = createCategoryMiddle(rs);
        select.close();

        return catmid.getId();
    }

    //get top category given the name (if it does not exist, this is an error because top categories are fixed) 
    public int getCategoryTopByName(String cattopName) throws SQLException
    {
        CategoryTop cattop = new CategoryTop();
        //int cattopId;
        if(cattopName == null)
        {
            new IllegalArgumentException("invalid top category name was requested: " + cattopName).printStackTrace();
        }

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS_TOP + " FROM `lw_rm_cattop` WHERE cat_top_name = ?");
        select.setString(1, cattopName);
        ResultSet rs = select.executeQuery();

        if(!rs.next())
        {
            new IllegalArgumentException("invalid cat top name was requested: " + cattopName).printStackTrace();
            return -1;
        }
        cattop = createCategoryTop(rs);
        select.close();

        return cattop.getId();
    }

    //get bottom categoryId given the name
    public int getCategoryBottomByName(String catbotName) throws SQLException
    {
        CategoryBottom catbot = new CategoryBottom();
        if(catbotName == null)
        {
            new IllegalArgumentException("invalid bottom category name was requested: " + catbotName).printStackTrace();
        }

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS_BOT + " FROM `lw_rm_catbot` WHERE cat_bot_name = ?");
        select.setString(1, catbotName);
        ResultSet rs = select.executeQuery();

        if(!rs.next())
        {
            new IllegalArgumentException("invalid cat bot name was requested: " + catbotName).printStackTrace();
            return -1;
        }
        catbot = createCategoryBottom(rs);
        select.close();

        return catbot.getId();
    }

    //get middle categoryId given the name 
    public int getCategoryMiddleByName(String catmidName) throws SQLException
    {
        CategoryMiddle catmid = new CategoryMiddle();
        //int catmidId;
        if(catmidName == null)
        {
            new IllegalArgumentException("invalid middle category name was requested: " + catmidName).printStackTrace();
        }

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS_MID + " FROM `lw_rm_catmid` WHERE cat_mid_name = ?");
        select.setString(1, catmidName);
        ResultSet rs = select.executeQuery();

        if(!rs.next())
        {
            new IllegalArgumentException("invalid cat mid name was requested: " + catmidName).printStackTrace();
            return -1;
        }
        catmid = createCategoryMiddle(rs);
        select.close();

        return catmid.getId();
    }

    //get top category given the name
    public CategoryTop getTopCategoryByName(String cattopName) throws SQLException
    {
        CategoryTop cattop = new CategoryTop();
        if(cattopName == null)
        {
            new IllegalArgumentException("invalid top category name was requested: " + cattopName).printStackTrace();
        }

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS_TOP + " FROM `lw_rm_cattop` WHERE cat_top_name = ?");
        select.setString(1, cattopName);
        ResultSet rs = select.executeQuery();

        if(!rs.next())
        {
            new IllegalArgumentException("invalid cat top name was requested: " + cattopName).printStackTrace();
        }
        cattop = createCategoryTop(rs);
        select.close();
        return cattop;
    }

    //get middle category given the name
    public CategoryMiddle getMiddleCategoryByName(String catmidName) throws SQLException
    {
        CategoryMiddle catmid = new CategoryMiddle();
        if(catmidName == null)
        {
            new IllegalArgumentException("invalid middle category name was requested: " + catmidName).printStackTrace();
        }

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS_MID + " FROM `lw_rm_catmid` WHERE cat_mid_name = ?");
        select.setString(1, catmidName);
        ResultSet rs = select.executeQuery();

        if(!rs.next())
        {
            new IllegalArgumentException("invalid cat mid name was requested: " + catmidName).printStackTrace();
        }

        catmid = createCategoryMiddle(rs);
        select.close();
        return catmid;
    }

    //get bottom category given the name 
    public CategoryBottom getBottomCategoryByName(String catbotName) throws SQLException
    {
        CategoryBottom catbot = new CategoryBottom();

        if(catbotName == null)
        {
            new IllegalArgumentException("invalid bottom category name was requested: " + catbotName).printStackTrace();
        }

        PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT " + COLUMNS_BOT + " FROM `lw_rm_catbot` WHERE cat_bot_name =?");
        select.setString(1, catbotName);
        ResultSet rs = select.executeQuery();

        if(!rs.next())
        {
            new IllegalArgumentException("invalid cat mid name was requested: " + catbotName).printStackTrace();
        }
        else
        {
            catbot = createCategoryBottom(rs);
            select.close();
        }
        return catbot;
    }

    //save new bottom category given the name and midcatId 
    public int saveNewBottomCategory(String catbotName, int catmidId) throws SQLException
    {
        int catbotId;
        PreparedStatement replace = learnweb.getConnection().prepareStatement("INSERT INTO `lw_rm_catbot` (`cat_bot_name`, `cat_mid_id`) VALUES (?, ?)");
        replace.setString(1, catbotName);
        replace.setInt(2, catmidId);
        replace.executeUpdate();

        //get the generated Id
        ResultSet rs = replace.getGeneratedKeys();
        if(!rs.next())
            throw new SQLException("database error: no id generated");
        catbotId = rs.getInt(1);
        replace.close();
        return catbotId;
    }

    //create category_resource
    @SuppressWarnings("unchecked")
    private CategoryResource createCategoryResource(ResultSet rs) throws SQLException
    {

        CategoryResource catresource = new CategoryResource();
        catresource.setResourceId(rs.getInt("resource_id"));
        catresource.setTopcatId(rs.getInt("cat_top_id"));
        catresource.setMidcatId(rs.getInt("cat_mid_id"));
        catresource.setBotcatId(rs.getInt("cat_bot_id"));

        return catresource;
    }

    //create category
    @SuppressWarnings("unchecked")
    private Category createCategory(ResultSet rs) throws SQLException
    {

        //retrive top, middle and bottom categories using their ids
        CategoryBottom catbot = getCategoryBottomById(rs.getInt("cat_bot_id"));
        CategoryMiddle catmid = getCategoryMiddleById(rs.getInt("cat_mid_id"));
        CategoryTop cattop = getCategoryTopById(rs.getInt("cat_top_id"));

        Category cat = new Category(cattop, catmid, catbot);

        return cat;
    }

    //create category bottom 
    @SuppressWarnings("unchecked")
    private CategoryBottom createCategoryBottom(ResultSet rs) throws SQLException
    {
        CategoryBottom catbot = new CategoryBottom();

        catbot.setId(rs.getInt("cat_bot_id"));
        catbot.setCatbot_name(rs.getString("cat_bot_name"));
        catbot.setCatmid_id(rs.getInt("cat_mid_id"));

        return catbot;
    }

    //create category middle
    @SuppressWarnings("unchecked")
    private CategoryMiddle createCategoryMiddle(ResultSet rs) throws SQLException
    {
        CategoryMiddle catmid = new CategoryMiddle();

        catmid.setId(rs.getInt("cat_mid_id"));
        catmid.setCatmid_name(rs.getString("cat_mid_name"));
        catmid.setCattop_id(rs.getInt("cat_top_id"));

        return catmid;
    }

    //create category top 
    @SuppressWarnings("unchecked")
    private CategoryTop createCategoryTop(ResultSet rs) throws SQLException
    {
        CategoryTop cattop = new CategoryTop();

        cattop.setId(rs.getInt("cat_top_id"));
        cattop.setCattop_name(rs.getString("cat_top_name"));

        return cattop;
    }
}
