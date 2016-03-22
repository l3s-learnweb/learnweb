package de.l3s.learnwebAdminBeans;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import org.apache.log4j.Logger;

import de.l3s.learnweb.User;
import de.l3s.learnwebBeans.ApplicationBean;

@ManagedBean
@RequestScoped
public class AdminSystemBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 1354024417928664741L;
    private static final Logger log = Logger.getLogger(AdminSystemBean.class);
    private LinkedList<Object> databaseProcessList;

    public AdminSystemBean() throws SQLException
    {
	User user = getUser();

	if(null == user || !user.isAdmin()) // not logged in
	    return;

	loadDatabaseProcessList();
    }

    private void loadDatabaseProcessList() throws SQLException
    {
	databaseProcessList = new LinkedList<>();
	ResultSet rs = getLearnweb().getConnection().createStatement().executeQuery("SHOW FULL PROCESSLIST");

	while(rs.next())
	{
	    DatabaseProcessStatistic ps = new DatabaseProcessStatistic();
	    ps.setId(rs.getInt("Id"));
	    ps.setUser(rs.getString("User"));
	    ps.setHost(rs.getString("Host"));
	    ps.setDb(rs.getString("db"));
	    ps.setCommand(rs.getString("Command"));
	    ps.setTime(rs.getString("Time"));
	    ps.setState(rs.getString("State"));
	    ps.setInfo(rs.getString("Info"));
	    ps.setProgress(rs.getString("Progress"));

	    databaseProcessList.add(ps);
	}
    }

    public LinkedList<Object> getDatabaseProcessList()
    {
	return databaseProcessList;
    }

    public void onKillDatabaseProcess(int processId) throws SQLException
    {
	getLearnweb().getConnection().createStatement().executeUpdate("KILL " + processId);

	addMessage(FacesMessage.SEVERITY_INFO, "Killed process " + processId);

	loadDatabaseProcessList(); // update list
    }

    public static class DatabaseProcessStatistic
    {
	private int id;
	private String user;
	private String host;
	private String db;
	private String command;
	private String time;
	private String state;
	private String info;
	private String progress;

	@Override
	public String toString()
	{
	    return "ProcessStatistic [id=" + id + ", user=" + user + ", host=" + host + ", db=" + db + ", command=" + command + ", time=" + time + ", state=" + state + ", info=" + info + ", progress=" + progress + "]";
	}

	public int getId()
	{
	    return id;
	}

	public void setId(int id)
	{
	    this.id = id;
	}

	public String getUser()
	{
	    return user;
	}

	public void setUser(String user)
	{
	    this.user = user;
	}

	public String getHost()
	{
	    return host;
	}

	public void setHost(String host)
	{
	    this.host = host;
	}

	public String getDb()
	{
	    return db;
	}

	public void setDb(String db)
	{
	    this.db = db;
	}

	public String getCommand()
	{
	    return command;
	}

	public void setCommand(String command)
	{
	    this.command = command;
	}

	public String getTime()
	{
	    return time;
	}

	public void setTime(String time)
	{
	    this.time = time;
	}

	public String getState()
	{
	    return state;
	}

	public void setState(String state)
	{
	    this.state = state;
	}

	public String getInfo()
	{
	    return info;
	}

	public void setInfo(String info)
	{
	    this.info = info;
	}

	public String getProgress()
	{
	    return progress;
	}

	public void setProgress(String progress)
	{
	    this.progress = progress;
	}
    }
}
