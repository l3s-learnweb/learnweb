package de.l3s.learnweb;

import java.util.List;

public class Folder
{
    private int folderId = -1;
    private int groupId = -1;
    private int parentFolderId;
    private String name;

    public List<Folder> getSubfolders()
    {
	return Learnweb.getInstance().getResourceManager().getFolders(groupId, folderId);
    }

    public Folder getParentFolder()
    {
	if(parentFolderId == 0)
	    return null;

	return Learnweb.getInstance().getResourceManager().getFolder(parentFolderId);
    }

    public int getFolderId()
    {
	return folderId;
    }

    public void setFolderId(int folderId)
    {
	this.folderId = folderId;
    }

    public int getGroupId()
    {
	return groupId;
    }

    public void setGroupId(int groupId)
    {
	this.groupId = groupId;
    }

    public int getParentFolderId()
    {
	return parentFolderId;
    }

    public void setParentFolderId(int parentFolderId)
    {
	this.parentFolderId = parentFolderId;
    }

    public String getName()
    {
	return name;
    }

    public void setName(String name)
    {
	this.name = name;
    }

}
