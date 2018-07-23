package de.l3s.learnweb.resource.yellMetadata;

import java.io.Serializable;

// TODO: All this categories (top, mod, bot) should be replaced by something more abstract
//composite class of CategoryTop, CategoryMid, CategoryBot
public class Category implements Comparable<Category>, Serializable
{
    private static final long serialVersionUID = -5463021088765191168L;

    private CategoryTop catTop;
    private CategoryMiddle catMid;
    private CategoryBottom catBot;
    private String catName;

    public Category()
    {

    }

    public Category(CategoryTop catTop, CategoryMiddle catMid, CategoryBottom catBot)
    {
        this.catTop = catTop;
        this.catMid = catMid;
        this.catBot = catBot;
        this.catName = generateCatName();
    }

    public CategoryTop getCatTop()
    {
        return catTop;
    }

    public void setCatTop(CategoryTop catTop)
    {
        this.catTop = catTop;
        this.catName = null;
    }

    public CategoryMiddle getCatMid()
    {
        return catMid;
    }

    public void setCatMid(CategoryMiddle catMid)
    {
        this.catMid = catMid;
        this.catName = null;
    }

    public CategoryBottom getCatBot()
    {
        return catBot;
    }

    public void setCatBot(CategoryBottom catBot)
    {
        this.catBot = catBot;
        this.catName = null;
    }

    public String getCatName()
    {
        if(this.catName == null)
        {
            this.catName = generateCatName();
        }

        return this.catName;
    }

    private String generateCatName()
    {
        if(isCatExists(catTop))
        {
            if(isCatExists(catMid))
            {
                if(isCatExists(catBot))
                {
                    return catTop.getCatName() + "/" + catMid.getCatName() + "/" + catBot.getCatName();
                }

                return catTop.getCatName() + "/" + catMid.getCatName();
            }

            return catTop.getCatName();
        }

        return "x";
    }

    private static boolean isCatExists(CategoryInterface category)
    {
        return category != null && category.getCatName() != null && !category.getCatName().equals("x");
    }

    @Override
    public int compareTo(Category o)
    {
        return this.getCatName().compareTo(o.getCatName());
    }

}
