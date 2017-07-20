package de.l3s.learnweb.rm;

import java.io.Serializable;

//composite class of CategoryTop, CategoryMid, CategoryBot

public class Category implements Comparable<Category>, Serializable
{
    private static final long serialVersionUID = -5463021088765191168L;
    CategoryTop cattop = new CategoryTop();
    CategoryMiddle catmid = new CategoryMiddle();
    CategoryBottom catbot = new CategoryBottom();
    String cat;

    public Category()
    {

    }

    public Category(CategoryTop cattop, CategoryMiddle catmid, CategoryBottom catbot)
    {
        this.cattop = cattop;
        this.catmid = catmid;
        this.catbot = catbot;
        if((catbot.getCatbot_name() != "x") && (catmid.getCatmid_name() != "x"))
        {
            this.cat = cattop.getCattop_name() + "/" + catmid.getCatmid_name() + "/" + catbot.getCatbot_name();
        }
        else if((catmid.getCatmid_name() != "x") && (catbot.getCatbot_name() == "x"))
        {
            this.cat = cattop.getCattop_name() + "/" + catmid.getCatmid_name();
        }
        else if((catmid.getCatmid_name() == "x") && (catbot.getCatbot_name() == "x"))
        {
            this.cat = cattop.getCattop_name();
        }
    }

    public CategoryTop getCattop()
    {
        return cattop;
    }

    public void setCattop(CategoryTop cattop)
    {
        this.cattop = cattop;
    }

    public CategoryMiddle getCatmid()
    {
        return catmid;
    }

    public void setCatmid(CategoryMiddle catmid)
    {
        this.catmid = catmid;
    }

    public CategoryBottom getCatbot()
    {
        return catbot;
    }

    public void setCatbot(CategoryBottom catbot)
    {
        this.catbot = catbot;
    }

    public String getCat()
    {
        if((catbot.getCatbot_name() != "x") && (catmid.getCatmid_name() != "x"))
        {
            this.cat = cattop.getCattop_name() + "/" + catmid.getCatmid_name() + "/" + catbot.getCatbot_name();
        }
        else if((catmid.getCatmid_name() != "x") && (catbot.getCatbot_name() == "x"))
        {
            this.cat = cattop.getCattop_name() + "/" + catmid.getCatmid_name();
        }
        else if((catmid.getCatmid_name() == "x") && (catbot.getCatbot_name() == "x"))
        {
            this.cat = cattop.getCattop_name();
        }

        return this.cat;
    }

    public void setCat(String cat)
    {
        this.cat = cat;
    }

    @Override
    public int compareTo(Category o)
    {
        return this.getCat().compareTo(o.getCat());
    }

}
