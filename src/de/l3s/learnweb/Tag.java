package de.l3s.learnweb;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;


public class Tag implements Comparable<Tag>, Serializable{

	private static final long serialVersionUID = 7542445827379987188L;
	private int id;
	private String name;

	public Tag(int id, String name) 
	{
		this.id = id;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public int getId() {
		return id;
	}
	
	public List<Resource> getResources() throws SQLException
	{
		return Learnweb.getInstance().getResourceManager().getResourcesByTagId(id);
	}

	@Override
	public int compareTo(Tag tag) 
	{		
		return this.getName().compareTo(tag.getName());
	}

	public void setId(int id) {
		this.id = id;		
	}

}
