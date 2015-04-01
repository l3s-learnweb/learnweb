package de.l3s.learnwebBeans;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import de.l3s.learnweb.Learnweb;

@ManagedBean
@RequestScoped
public class StatusBean extends ApplicationBean
{

    private List<Service> services = new LinkedList<StatusBean.Service>();

    public StatusBean()
    {
	services.add(new Service("Learnweb Tomcat", "ok", "", "offensichtlich ok, sonst wäre diese Seite nicht erreichbar"));

	Learnweb learnweb = getLearnweb();

	// test learnweb database
	String status = "ok";
	String comment = "";
	try
	{
	    Statement stmt = learnweb.getConnection().createStatement();
	    ResultSet rs = stmt.executeQuery("SELECT count(*) FROM lw_user");

	    if(!rs.next() || rs.getInt(1) < 400)
	    {
		status = "error";
		comment = "unexpected result from database";
	    }
	}
	catch(SQLException e)
	{
	    status = "error";
	    comment = e.getMessage();
	}
	services.add(new Service("Learnweb Database", status, learnweb.getProperties().getProperty("mysql_url"), comment));

	// test fedora 
	status = "ok";
	comment = "";

	try
	{
	    if(!learnweb.getResourceManager().isResourceRatedByUser(2811, 1684))
	    {
		status = "warning";
		comment = "unexpected result from fedora";
	    }
	}
	catch(Exception e)
	{
	    status = "error";
	    comment = e.getMessage();
	}
	services.add(new Service("Fedora", status, learnweb.getProperties().getProperty("FEDORA_SERVER_URL"), comment));

    }

    public List<Service> getServices()
    {
	return services;
    }

    public class Service
    {
	private String name;
	private String status;
	private String url;
	private String comment;

	public Service(String name, String status, String url, String comment)
	{
	    super();
	    this.name = name;
	    this.status = new String(status);
	    this.url = url;
	    this.comment = new String(comment);
	}

	public String getName()
	{
	    return name;
	}

	public String getStatus()
	{
	    return status;
	}

	public String getComment()
	{
	    return comment;
	}

	public String getUrl()
	{
	    return url;
	}

    }
}
