package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.sql.SQLException;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

@ManagedBean
@RequestScoped
public class GroupOverviewBean extends GroupBean implements Serializable
{
	private static final long serialVersionUID = 3895774093840471673L;
	
	public void preRenderView() throws SQLException
	{
		
	}

}
