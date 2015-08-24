package de.l3s.learnweb;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;

import org.hibernate.validator.constraints.NotEmpty;

import de.l3s.learnweb.beans.UtilBean;

@ManagedBean
public class ForumPost implements Serializable
{
    private static final long serialVersionUID = 4093915855537221830L;

    private int id = -1;
    private int userId;
    private int topicId;
    @NotEmpty
    private String text;
    private Date date;
    private int editCount;
    private Date lastEditDate;
    private int editUserId;
    private String category;
    private List<SelectItem> categories;

    // cached value
    private transient User user;
    private transient User editUser;

    public int getId()
    {
	return id;
    }

    public void setId(int postId)
    {
	this.id = postId;
    }

    public int getUserId()
    {
	return userId;
    }

    public void setUserId(int userId)
    {
	this.userId = userId;
    }

    public int getTopicId()
    {
	return topicId;
    }

    public void setTopicId(int topicId)
    {
	this.topicId = topicId;
    }

    public String getText()
    {
	return text;
    }

    public void setText(String text)
    {
	this.text = text;
    }

    public Date getDate()
    {
	return date;
    }

    public void setDate(Date date)
    {
	this.date = date;
	if(lastEditDate == null)
	    this.lastEditDate = date;
    }

    public int getEditCount()
    {
	return editCount;
    }

    public void setEditCount(int editCount)
    {
	this.editCount = editCount;
    }

    public Date getLastEditDate()
    {
	return lastEditDate;
    }

    public void setLastEditDate(Date lastEditDate)
    {
	this.lastEditDate = lastEditDate;
    }

    public int getEditUserId()
    {
	return editUserId;
    }

    public void setEditUserId(int editUserId)
    {
	this.editUserId = editUserId;
    }

    public User getUser() throws SQLException
    {
	if(user == null)
	{
	    user = Learnweb.getInstance().getUserManager().getUser(userId);
	}
	return user;
    }

    public User getEditUser() throws SQLException
    {
	if(editUser == null)
	{
	    editUser = Learnweb.getInstance().getUserManager().getUser(editUserId);
	}
	return editUser;
    }

    @PostConstruct
    public void init()
    {

	SelectItemGroup g1 = new SelectItemGroup(UtilBean.getLocaleMessage("Forum.cell.category.1"));
	g1.setSelectItems(new SelectItem[] { new SelectItem(UtilBean.getLocaleMessage("Forum.cell.category.1a")), new SelectItem(UtilBean.getLocaleMessage("Forum.cell.category.1b")), new SelectItem(UtilBean.getLocaleMessage("Forum.cell.category.1c")),
		new SelectItem(UtilBean.getLocaleMessage("Forum.cell.category.1d")) });

	SelectItemGroup g2 = new SelectItemGroup(UtilBean.getLocaleMessage("Forum.cell.category.2"));
	g2.setSelectItems(new SelectItem[] { new SelectItem(UtilBean.getLocaleMessage("Forum.cell.category.2a")), new SelectItem(UtilBean.getLocaleMessage("Forum.cell.category.2b")), new SelectItem(UtilBean.getLocaleMessage("Forum.cell.category.2c")),
		new SelectItem(UtilBean.getLocaleMessage("Forum.cell.category.2d")), new SelectItem(UtilBean.getLocaleMessage("Forum.cell.category.2e")), new SelectItem(UtilBean.getLocaleMessage("Forum.cell.category.2f")),
		new SelectItem(UtilBean.getLocaleMessage("Forum.cell.category.2g")), new SelectItem(UtilBean.getLocaleMessage("Forum.cell.category.2h")) });

	SelectItemGroup g3 = new SelectItemGroup(UtilBean.getLocaleMessage("Forum.cell.category.3"));
	g3.setSelectItems(new SelectItem[] { new SelectItem(UtilBean.getLocaleMessage("Forum.cell.category.3a")), new SelectItem(UtilBean.getLocaleMessage("Forum.cell.category.3b")), new SelectItem(UtilBean.getLocaleMessage("Forum.cell.category.3c")) });

	categories = new ArrayList<SelectItem>();
	categories.add(g1);
	categories.add(g2);
	categories.add(g3);
    }

    public String getCategory()
    {
	return category;
    }

    public void setCategory(String category)
    {
	this.category = category;
    }

    public List<SelectItem> getCategories()

    {
	init();
	return categories;
    }

    public void setCategories(List<SelectItem> categories)
    {
	this.categories = categories;
    }

    @Override
    public String toString()
    {
	return "ForumPost [id=" + id + ", userId=" + userId + ", topicId=" + topicId + ", text=" + text + ", date=" + date + ", editCount=" + editCount + ", lastEditDate=" + lastEditDate + ", editUserId=" + editUserId + "]";
    }

}
