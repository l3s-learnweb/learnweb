package de.l3s.searchlogclient;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import de.l3s.learnweb.User;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.searchlogclient.jaxb.QueryLog;
import de.l3s.searchlogclient.jaxb.ResourceLog;
import de.l3s.searchlogclient.jaxb.ViewingTime;

@ManagedBean
@SessionScoped
public class SelectUsersBean extends ApplicationBean implements Serializable
{

    private static final long serialVersionUID = 5314158272712896434L;
    private int selectedUser;
    private String selectedTable;
    private boolean test_resource_click;
    private boolean test_resultset;
    private boolean test_user_q;
    private boolean test_viewing;

    private ArrayList<ResourceLog> logs = new ArrayList<ResourceLog>();
    private ArrayList<Resultset> resultset = new ArrayList<Resultset>();
    private ArrayList<QueryLog> querylog = new ArrayList<QueryLog>();
    private ArrayList<ViewingTime> timelog = new ArrayList<ViewingTime>();

    private Connection dbConnection;
    private final String mysql_url = "jdbc:mysql://mysql.l3s.uni-hannover.de/learnweb?useUnicode=true&amp;characterEncoding=UTF-8";
    private final String mysql_user = "zerr";
    private final String mysql_password = "***REMOVED***";

    public SelectUsersBean()
    {
        selectedUser = getUser().getId();

    }

    public int getSelectedUser()
    {
        return selectedUser;
    }

    public void setSelectedUser(int selectedUser)
    {
        this.selectedUser = selectedUser;
    }

    public String getSelectedTable()
    {
        return selectedTable;
    }

    public void setSelectedTable(String selectedTable)
    {
        this.selectedTable = selectedTable;
    }

    public List<User> users;
    public List<String> dbtables;

    public List<String> getDbtables()
    {

        dbtables = new LinkedList<String>();
        dbtables.add("action_on_resource");
        dbtables.add("resources");
        dbtables.add("user_queries");
        dbtables.add("viewing_time");
        return dbtables;
    }

    public List<User> getUsers()
    {

        selectedUser = getUser().getId();

        return users;
    }

    public void displayResults()
    {

        if(selectedTable.equals("action_on_resource"))
        {
            test_resource_click = true;
            test_resultset = false;
            test_user_q = false;
            test_viewing = false;
            selectResourceClick();

        }
        else if(selectedTable.equals("resources"))
        {
            test_resultset = true;
            test_resource_click = false;
            test_user_q = false;
            test_viewing = false;
            selectResultset();
        }
        else if(selectedTable.equals("user_queries"))
        {
            test_user_q = true;
            test_resource_click = false;
            test_resultset = false;
            test_viewing = false;
            selectUserqueries();
        }
        else
        {
            test_viewing = true;
            test_resource_click = false;
            test_resultset = false;
            test_user_q = false;
            selectViewingtime();
        }

    }

    public void selectResourceClick()
    {

        String select = "SELECT t1.resultsetid,t1.resourceid,t2.url,t1.timestamp,t1.action FROM action_on_resource t1 JOIN resources t2 ON t1.resourceid = t2.resourceid AND t1.resultsetid=t2.resultsetid where userid=?";
        int resourceid, resultsetid;
        String timestamp, action, url;
        logs = new ArrayList<ResourceLog>();
        ResourceLog resourcelog;
        try
        {
            Class.forName("org.mariadb.jdbc.Driver");
            dbConnection = DriverManager.getConnection(mysql_url, mysql_user, mysql_password);
            PreparedStatement dbselect = dbConnection.prepareStatement(select);

            dbselect.setInt(1, selectedUser);
            dbselect.executeQuery();
            ResultSet rs = dbselect.getResultSet();
            while(rs.next())
            {
                resultsetid = rs.getInt("resultsetid");
                resourceid = rs.getInt("resourceid");
                timestamp = rs.getTimestamp("timestamp").toString();
                action = rs.getString("action");
                url = rs.getString("url");
                resourcelog = new ResourceLog(selectedUser, resultsetid, resourceid, action, timestamp, url);
                logs.add(resourcelog);
            }
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        catch(ClassNotFoundException e)
        {
            e.printStackTrace();
        }

    }

    public void selectResultset()
    {
        String select = "SELECT resultsetid FROM user_queries where userid=?";
        int resultsetid;

        resultset = new ArrayList<Resultset>();
        Resultset oneresult;

        try
        {
            Class.forName("org.mariadb.jdbc.Driver");

            dbConnection = DriverManager.getConnection(mysql_url, mysql_user, mysql_password);

            PreparedStatement dbselect = dbConnection.prepareStatement(select);

            dbselect.setInt(1, selectedUser);
            dbselect.executeQuery();
            ResultSet rs = dbselect.getResultSet();
            while(rs.next())
            {

                resultsetid = rs.getInt("resultsetid");
                String selectresources = "SELECT t1.resultsetid,t1.resourceid,t1.url,t1.type,t1.source,t1.filename,t1.shortdescrp,t1.system,t1.system_id,t1.selected" + " FROM resources t1 where t1.resultsetid=?";
                PreparedStatement dbselectres = dbConnection.prepareStatement(selectresources);
                dbselectres.setInt(1, resultsetid);
                dbselectres.executeQuery();
                ResultSet res = dbselectres.getResultSet();
                while(res.next())
                {

                    oneresult = new Resultset(res.getInt("resultsetid"), res.getInt("resourceid"), res.getString("url"), res.getString("type"), res.getString("source"), res.getString("filename"), res.getString("shortdescrp"), res.getString("system"), res.getInt("system_id"),
                            res.getString("selected"));
                    resultset.add(oneresult);

                }
            }
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        catch(ClassNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    public void selectUserqueries()
    {
        String select = "SELECT * FROM user_queries where userid=?";

        querylog = new ArrayList<QueryLog>();
        QueryLog single_qlog;

        try
        {
            Class.forName("org.mariadb.jdbc.Driver");
            dbConnection = DriverManager.getConnection(mysql_url, mysql_user, mysql_password);

            PreparedStatement dbselect = dbConnection.prepareStatement(select);

            dbselect.setInt(1, selectedUser);
            dbselect.executeQuery();
            ResultSet rs = dbselect.getResultSet();
            while(rs.next())
            {

                single_qlog = new QueryLog(rs.getString("query"), rs.getString("search_type"), rs.getInt("userid"), rs.getInt("groupid"), rs.getString("sessionid"), rs.getString("timestamp"), rs.getInt("resultsetid"));
                querylog.add(single_qlog);
            }
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        catch(ClassNotFoundException e)
        {
            e.printStackTrace();
        }

    }

    public void selectViewingtime()
    {
        String select = "SELECT t1.resultsetid,t1.resourceid,t2.url,t1.starttime,t1.endtime FROM viewing_time t1 JOIN resources t2 ON t1.resourceid=t2.resourceid AND t1.resultsetid=t2.resultsetid";

        timelog = new ArrayList<ViewingTime>();
        ViewingTime single_timelog;

        try
        {
            Class.forName("org.mariadb.jdbc.Driver");
            dbConnection = DriverManager.getConnection(mysql_url, mysql_user, mysql_password);

            PreparedStatement dbselect = dbConnection.prepareStatement(select);
            dbselect.executeQuery();

            ResultSet rs = dbselect.getResultSet();
            while(rs.next())
            {

                single_timelog = new ViewingTime(rs.getInt("resultsetid"), rs.getString("url"), rs.getString("starttime"), rs.getString("endtime"));
                timelog.add(single_timelog);
            }
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        catch(ClassNotFoundException e)
        {
            e.printStackTrace();
        }

    }

    public boolean isTest_resource_click()
    {
        return test_resource_click;
    }

    public void setTest_resource_click(boolean test_resource_click)
    {
        this.test_resource_click = test_resource_click;
    }

    public boolean isTest_resultset()
    {
        return test_resultset;
    }

    public void setTest_resultset(boolean test_resultset)
    {
        this.test_resultset = test_resultset;
    }

    public boolean isTest_user_q()
    {
        return test_user_q;
    }

    public void setTest_user_q(boolean test_user_q)
    {
        this.test_user_q = test_user_q;
    }

    public boolean isTest_viewing()
    {
        return test_viewing;
    }

    public void setTest_viewing(boolean test_viewing)
    {
        this.test_viewing = test_viewing;
    }

    public ArrayList<ResourceLog> getLogs()
    {
        return logs;
    }

    public void setLogs(ArrayList<ResourceLog> logs)
    {
        this.logs = logs;
    }

    public ArrayList<Resultset> getResultset()
    {
        return resultset;
    }

    public void setResultset(ArrayList<Resultset> resultset)
    {
        this.resultset = resultset;
    }

    public ArrayList<QueryLog> getQuerylog()
    {
        return querylog;
    }

    public void setQuerylog(ArrayList<QueryLog> querylog)
    {
        this.querylog = querylog;
    }

    public ArrayList<ViewingTime> getTimelog()
    {
        return timelog;
    }

    public void setTimelog(ArrayList<ViewingTime> timelog)
    {
        this.timelog = timelog;
    }

}
