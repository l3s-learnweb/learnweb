package jcdashboard.model.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import jcdashboard.model.UsesTable;

// import org.apache.commons.logging.Log;
// import org.apache.commons.logging.LogFactory;

public class UserLogHome
{
    private static final Logger log = Logger.getLogger(UserLogHome.class);

    private Connection connect = null;

    private Connection openConnection()
    {

        if(connect == null)
        {
            String url = "jdbc:mysql://localhost/learnweb_logs";

            String username = "learnweb";
            String password = "***REMOVED***";

            try
            {

                Class.forName("org.mariadb.jdbc.Driver");

                connect = DriverManager.getConnection(url, username, password);
            }
            catch(SQLException ex)
            {
                System.out.println(ex.getMessage());
            }
            catch(ClassNotFoundException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return connect;

    }

    public void closeConnection()
    {
        if(connect != null)
            try
            {
                connect.close();
            }
            catch(SQLException e)
            {
                e.printStackTrace();
            }
    }

    public UserLogHome()
    {
        super();
        openConnection();
    }

    public Map<String, Integer> actionPerDay()
    {
        Map<String, Integer> actperday = new TreeMap<String, Integer>();
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement("select DATE(timestamp) as day,count(*) as count from user_log where timestamp>'2017-03-02' and user_id<> 8963 group by day");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                actperday.put("" + rs.getString("day"), Integer.parseInt("" + rs.getString("count")));
            closeConnection();
        }
        catch(NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        return actperday;
    }

    public Map<String, Integer> actionCount()
    {
        Map<String, Integer> actperday = new TreeMap<String, Integer>();
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement("select action,count(*) as count from user_log where timestamp>'2017-03-02' and user_id<> 8963 group by action");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                actperday.put("" + rs.getString("action"), Integer.parseInt("" + rs.getString("count")));
            closeConnection();
        }
        catch(NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }

        return actperday;
    }

    public Map<String, Integer> actionCount(String startdate, String enddate)
    {
        Map<String, Integer> actperday = new TreeMap<String, Integer>();
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement("select action,count(*) as count from user_log where timestamp>'" + startdate + "' and timestamp<'" + enddate + "' and user_id<> 8963 group by action");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                actperday.put("" + rs.getString("action"), Integer.parseInt("" + rs.getString("count")));
            closeConnection();
        }
        catch(NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        return actperday;
    }

    public Integer getTotalConcepts(String startdate, String enddate)
    {
        String result = "0";
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement(
                    "select count(distinct glossary_id) as count from resource_glossary rg, resource r where r.resource_id=rg.resource_id and rg.deleted<>1 and r.deleted<>1 and owner_user_id<> 8963 and timestamp>'" + startdate + "' and timestamp<'" + enddate + "'");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                result = rs.getString("count");
            closeConnection();

        }
        catch(NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        return Integer.parseInt("" + result);
    }

    public Integer getTotalTerms(String startdate, String enddate)
    {
        String result = "0";
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement(
                    "select count(*) as count from resource_glossary rg, resource_glossary_terms rgt where rg.glossary_id=rgt.glossary_id and resource_id IN (select resource_id from resource where owner_user_id<> 8963 and deleted<>1) and rgt.deleted <>1 and rg.deleted<>1 and rg.timestamp>'"
                            + startdate + "' and rg.timestamp<'" + enddate + "'");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                result = rs.getString("count");
            closeConnection();

        }
        catch(NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        return Integer.parseInt("" + result);
    }

    public Integer getTotalConcepts(Integer sid, String startdate, String enddate)
    {
        String result = "0";
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement(
                    "select count(distinct glossary_id) as count from resource_glossary rg, resource r where r.resource_id=rg.resource_id and rg.deleted<>1 and r.deleted<>1 and owner_user_id=" + sid + " and timestamp>'" + startdate + "' and timestamp<'" + enddate + "'");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                result = rs.getString("count");
            // closeConnection();

        }
        catch(NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        return Integer.parseInt("" + result);
    }

    public Integer getTotalTerms(Integer sid, String startdate, String enddate)
    {
        String result = "0";
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement("select count(*) as count from resource_glossary rg, resource_glossary_terms rgt where rg.glossary_id=rgt.glossary_id and resource_id IN (select resource_id from resource where owner_user_id=" + sid
                    + " and deleted<>1) and rgt.deleted <>1 and rg.deleted<>1 and rg.timestamp>'" + startdate + "' and rg.timestamp<'" + enddate + "'");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                result = rs.getString("count");
            // closeConnection();

        }
        catch(NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        return Integer.parseInt("" + result);
    }

    public Map<String, Integer> glossarySource()
    {
        Map<String, Integer> actperday = new TreeMap<String, Integer>();
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement(
                    "select rgt.references as refs,count(*) as count from resource_glossary_terms rgt,resource_glossary rg, resource r where r.resource_id=rg.resource_id and rg.glossary_id=rgt.glossary_id and rg.deleted <>1 and r.deleted<>1 and rgt.deleted<>1 group by rgt.references");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                if(rs.getString("refs").trim().compareTo("") == 0)
                    actperday.put("EMPTY", Integer.parseInt("" + rs.getString("count")));
                else
                    actperday.put("" + rs.getString("refs"), Integer.parseInt("" + rs.getString("count")));

            closeConnection();
        }
        catch(NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        return actperday;
    }

    public Map<String, Integer> glossarySource(String startdate, String enddate)
    {
        Map<String, Integer> actperday = new TreeMap<String, Integer>();
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement(
                    "select rgt.references as refs,count(*) as count from resource_glossary_terms rgt,resource_glossary rg, resource r where r.resource_id=rg.resource_id and rg.glossary_id=rgt.glossary_id and rg.deleted <>1 and r.deleted<>1 and rgt.deleted<>1 and rg.timestamp>'"
                            + startdate + "' and rg.timestamp<'" + enddate + "' group by rgt.references");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                if(rs.getString("refs").trim().compareTo("") == 0)
                    actperday.put("EMPTY", Integer.parseInt("" + rs.getString("count")));
                else
                    actperday.put("" + rs.getString("refs"), Integer.parseInt("" + rs.getString("count")));

            closeConnection();
        }
        catch(NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        return actperday;
    }

    public Map<String, Integer> glossarySource(Integer sid, String startdate, String enddate)
    {
        Map<String, Integer> actperday = new TreeMap<String, Integer>();
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement(
                    "select rgt.references as refs,count(*) as count from resource_glossary_terms rgt,resource_glossary rg, resource r where r.resource_id=rg.resource_id and rg.glossary_id=rgt.glossary_id and rg.deleted <>1 and r.deleted<>1 and rgt.deleted<>1 and owner_user_id="
                            + sid + " and rg.timestamp>'" + startdate + "' and rg.timestamp<'" + enddate + "' group by rgt.references");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                if(rs.getString("refs").trim().compareTo("") == 0)
                    actperday.put("EMPTY", Integer.parseInt("" + rs.getString("count")));
                else
                    actperday.put("" + rs.getString("refs"), Integer.parseInt("" + rs.getString("count")));

            closeConnection();
        }
        catch(NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        return actperday;
    }

    public Map<String, Integer> glossarySource(Integer sid)
    {
        Map<String, Integer> actperday = new TreeMap<String, Integer>();

        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement(
                    "select rgt.references as refs,count(*) as count from resource_glossary_terms rgt,resource_glossary rg, resource r where r.resource_id=rg.resource_id and rg.glossary_id=rgt.glossary_id and rg.deleted <>1 and r.deleted<>1 and rgt.deleted<>1 and owner_user_id="
                            + sid + " group by rgt.references");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                if(rs.getString("refs").trim().compareTo("") == 0)
                    actperday.put("EMPTY", Integer.parseInt("" + rs.getString("count")));
                else
                    actperday.put("" + rs.getString("refs"), Integer.parseInt("" + rs.getString("count")));

            closeConnection();
        }
        catch(NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        return actperday;
    }

    public Map<String, Integer> userGlossary()
    {
        Map<String, Integer> actperday = new TreeMap<String, Integer>();

        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement(
                    "select owner_user_id, count(distinct rgt.glossary_id) as count from resource_glossary rg, resource r, resource_glossary_terms rgt where r.resource_id=rg.resource_id and rg.glossary_id=rgt.glossary_id and rg.deleted <>1 and rg.resource_id IN (select resource_id from resource where owner_user_id<> 8963) and rg.deleted<>1 and rgt.deleted<>1 group by owner_user_id order by owner_user_id ");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                actperday.put("" + rs.getString("owner_user_id"), Integer.parseInt("" + rs.getString("count")));

            closeConnection();
        }
        catch(NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        return actperday;
    }

    public Map<String, Integer> userGlossary(String startdate, String enddate)
    {
        Map<String, Integer> actperday = new TreeMap<String, Integer>();

        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement(
                    "select owner_user_id, count(distinct rgt.glossary_id) as count from resource_glossary rg, resource r, resource_glossary_terms rgt where r.resource_id=rg.resource_id and rg.glossary_id=rgt.glossary_id and rg.deleted <>1 and rg.resource_id IN (select resource_id from resource where owner_user_id<> 8963) and rg.deleted<>1 and rgt.deleted<>1 and rg.timestamp>'"
                            + startdate + "' and rg.timestamp<'" + enddate + "' group by owner_user_id order by owner_user_id ");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                actperday.put("" + rs.getString("owner_user_id"), Integer.parseInt("" + rs.getString("count")));

            closeConnection();
        }
        catch(NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        return actperday;
    }

    public Map<String, Integer> userGlossaryTerm()
    {
        Map<String, Integer> actperday = new TreeMap<String, Integer>();

        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement(
                    "select owner_user_id, count(distinct rgt.glossary_term_id) as count from resource_glossary rg, resource r, resource_glossary_terms rgt where r.resource_id=rg.resource_id and rg.glossary_id=rgt.glossary_id and rg.deleted <>1 and rg.resource_id IN (select resource_id from resource where owner_user_id<> 8963) and rg.deleted<>1 and rgt.deleted<>1 group by owner_user_id order by owner_user_id ");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                actperday.put("" + rs.getString("owner_user_id"), Integer.parseInt("" + rs.getString("count")));

            closeConnection();
        }
        catch(NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        return actperday;
    }

    public Map<String, Integer> userGlossaryTerm(String startdate, String enddate)
    {
        Map<String, Integer> actperday = new TreeMap<String, Integer>();
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement(
                    "select owner_user_id, count(distinct rgt.glossary_term_id) as count from resource_glossary rg, resource r, resource_glossary_terms rgt where r.resource_id=rg.resource_id and rg.glossary_id=rgt.glossary_id and rg.deleted <>1 and rg.resource_id IN (select resource_id from resource where owner_user_id<> 8963) and rg.deleted<>1 and rgt.deleted<>1 and rg.timestamp>'"
                            + startdate + "' and rg.timestamp<'" + enddate + "' group by owner_user_id order by owner_user_id ");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                actperday.put("" + rs.getString("owner_user_id"), Integer.parseInt("" + rs.getString("count")));

            closeConnection();
        }
        catch(NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        return actperday;
    }

    public Map<String, Integer> actionPerDay(Integer sid)
    {
        Map<String, Integer> actperday = new TreeMap<String, Integer>();
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement("select DATE(timestamp) as day,count(*) as count from user_log where timestamp>'2017-03-02' and user_id<> 8963 and user_id=" + sid + " group by day");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                actperday.put("" + rs.getString("day"), Integer.parseInt("" + rs.getString("count")));

            closeConnection();
        }
        catch(NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        return actperday;
    }

    public Map<String, Integer> actionCount(Integer sid)
    {
        Map<String, Integer> actperday = new TreeMap<String, Integer>();
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement("select action,count(*) as count from user_log where timestamp>'2017-03-02' and user_id<> 8963 and user_id=" + sid + " group by action");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                actperday.put("" + rs.getString("action"), Integer.parseInt("" + rs.getString("count")));

            closeConnection();
        }
        catch(NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        return actperday;
    }

    public Map<String, Integer> actionCount(Integer sid, String startdate, String enddate)
    {
        Map<String, Integer> actperday = new TreeMap<String, Integer>();
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement("select action,count(*) as count from user_log where timestamp>'" + startdate + "' and timestamp<'" + enddate + "' and user_id<> 8963 and user_id=" + sid + " group by action");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                actperday.put("" + rs.getString("action"), Integer.parseInt("" + rs.getString("count")));

            closeConnection();
        }
        catch(NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        return actperday;
    }

    public Integer getTotalConcepts(Integer sid)
    {
        String result = "0";
        try
        {
            PreparedStatement pstmt = openConnection()
                    .prepareStatement("select count(distinct glossary_id) as count from resource_glossary rg, resource r where r.resource_id=rg.resource_id and rg.deleted<>1 and r.deleted<>1 and owner_user_id=" + sid + " and timestamp>'2017-03-02'");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                result = rs.getString("count");
            closeConnection();

        }
        catch(NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        return Integer.parseInt("" + result);
    }

    public Integer getTotalTerms(Integer sid)
    {
        String result = "0";
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement("select count(*) as count from resource_glossary rg, resource_glossary_terms rgt where rg.glossary_id=rgt.glossary_id and resource_id IN (select resource_id from resource where owner_user_id=" + sid
                    + " and deleted<>1) and rgt.deleted <>1 and rg.deleted<>1 and rg.timestamp>'2017-03-02'");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                result = rs.getString("count");
            closeConnection();

        }
        catch(NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        return Integer.parseInt("" + result);
    }

    public Integer getTotalSource(Integer sid)
    {
        String result = "0";
        try
        {
            PreparedStatement pstmt = openConnection()
                    .prepareStatement("select count(distinct rgt.references) as count from resource_glossary rg, resource_glossary_terms rgt where rg.glossary_id=rgt.glossary_id and resource_id IN (select resource_id from resource where owner_user_id=" + sid
                            + " and deleted<>1) and rgt.deleted <>1 and rg.deleted<>1 and rg.timestamp>'2017-03-02'");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                result = rs.getString("count");
            closeConnection();

        }
        catch(NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        return Integer.parseInt("" + result);
    }

    public Integer getTotalSource(Integer sid, String startdate, String enddate)
    {
        String result = "0";
        try
        {
            PreparedStatement pstmt = openConnection()
                    .prepareStatement("select count(distinct rgt.references) as count from resource_glossary rg, resource_glossary_terms rgt where rg.glossary_id=rgt.glossary_id and resource_id IN (select resource_id from resource where owner_user_id=" + sid
                            + " and deleted<>1) and rgt.deleted <>1 and rg.deleted<>1 and rg.timestamp>'" + startdate + "' and rg.timestamp<'" + enddate + "'");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                result = rs.getString("count");
            closeConnection();

        }
        catch(NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        return Integer.parseInt("" + result);
    }

    public Integer getTotalSourceNoempty(Integer sid, String startdate, String enddate)
    {
        String result = "0";
        try
        {
            PreparedStatement pstmt = openConnection()
                    .prepareStatement("select count(distinct rgt.references) as count from resource_glossary rg, resource_glossary_terms rgt where rg.glossary_id=rgt.glossary_id and resource_id IN (select resource_id from resource where owner_user_id=" + sid
                            + " and deleted<>1) and rgt.references<>'' and rgt.deleted <>1 and rg.deleted<>1 and rg.timestamp>'" + startdate + "' and rg.timestamp<'" + enddate + "'");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                result = rs.getString("count");
            // closeConnection();

        }
        catch(NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        return Integer.parseInt("" + result);
    }

    public Map<String, Integer[]> getSummary2(String startdate, String enddate)
    {
        String result = "0", result2 = "0";
        Map<String, Integer[]> summary = new TreeMap<String, Integer[]>();
        try
        {

            // getTotalConcepts
            PreparedStatement pstmt = openConnection().prepareStatement(
                    "select owner_user_id,count(distinct glossary_id) as count from resource_glossary rg, resource r where r.resource_id=rg.resource_id and rg.deleted<>1 and r.deleted<>1 and timestamp>'" + startdate + "' and timestamp<'" + enddate + "' group by owner_user_id ");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
            {
                result = rs.getString("count");
                Integer[] list = new Integer[3];
                list[0] = Integer.parseInt("" + result);
                summary.put(rs.getString("owner_user_id"), list);
            }

            // getTotalTerms
            pstmt = openConnection().prepareStatement(
                    "select owner_user_id, count(*) as count , count(distinct rgt.references) as count2 from resource_glossary rg, resource_glossary_terms rgt, resource r where rg.glossary_id=rgt.glossary_id and r.resource_id=rg.resource_id and r.deleted<>1 and rgt.deleted <>1 and rg.deleted<>1 and rg.timestamp>'"
                            + startdate + "' and rg.timestamp<'" + enddate + "' group by owner_user_id");
            rs = pstmt.executeQuery();
            while(rs.next())
            {
                result = rs.getString("count");
                result2 = rs.getString("count2");
                Integer[] list = summary.get(rs.getString("owner_user_id"));
                list[1] = Integer.parseInt("" + result);
                list[2] = Integer.parseInt("" + result2);
                summary.put(rs.getString("owner_user_id"), list);
            }

        }
        catch(NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        return summary;
    }

    public Map<String, Integer> getSummary(Integer sid, String startdate, String enddate)
    {
        String result = "0", result2 = "0";
        Map<String, Integer> summary = new TreeMap<String, Integer>();
        try
        {

            // getTotalConcepts
            PreparedStatement pstmt = openConnection().prepareStatement(
                    "select count(distinct glossary_id) as count from resource_glossary rg, resource r where r.resource_id=rg.resource_id and rg.deleted<>1 and r.deleted<>1 and owner_user_id=" + sid + " and timestamp>'" + startdate + "' and timestamp<'" + enddate + "'");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                result = rs.getString("count");
            summary.put("concepts", Integer.parseInt("" + result));

            // getTotalTerms
            pstmt = openConnection()
                    .prepareStatement("select count(*) as count , count(distinct rgt.references) as count2 from resource_glossary rg, resource_glossary_terms rgt where rg.glossary_id=rgt.glossary_id and resource_id IN (select resource_id from resource where owner_user_id=" + sid
                            + " and deleted<>1) and rgt.deleted <>1 and rg.deleted<>1 and rg.timestamp>'" + startdate + "' and rg.timestamp<'" + enddate + "'");
            rs = pstmt.executeQuery();
            while(rs.next())
            {
                result = rs.getString("count");
                result2 = rs.getString("count2");
            }
            summary.put("terms", Integer.parseInt("" + result));
            summary.put("sources", Integer.parseInt("" + result2));

            // getTotalSourceNoempty
            /*pstmt = openConnection().prepareStatement("select count(distinct rgt.references) as count from resource_glossary rg, resource_glossary_terms rgt where rg.glossary_id=rgt.glossary_id and resource_id IN (select resource_id from resource where owner_user_id="+sid+" and deleted<>1) and rgt.references<>'' and rgt.deleted <>1 and rg.deleted<>1 and rg.timestamp>'"+startdate+"' and rg.timestamp<'"+enddate+"'");
            rs = pstmt.executeQuery();
            while (rs.next()) 
            	result=rs.getString("count");
            summary.put("sources",Integer.parseInt(""+result));
            */
            // closeConnection();

        }
        catch(NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        return summary;
    }

    public List<String> descritpions(Integer sid)
    {
        List<String> actperday = new ArrayList<String>();
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement("select rg.description as descr from resource_glossary rg, resource r where r.resource_id=rg.resource_id and rg.deleted <>1 and r.deleted<>1 and owner_user_id=" + sid + " and timestamp>'2017-03-02';");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                actperday.add(rs.getString("descr"));

            closeConnection();
        }
        catch(NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }

        return actperday;
    }

    public List<String> descritpions(Integer sid, String startdate, String enddate)
    {
        List<String> actperday = new ArrayList<String>();
        try
        {
            PreparedStatement pstmt = openConnection()
                    .prepareStatement("select rg.description as descr from resource_glossary rg, resource r where r.resource_id=rg.resource_id and rg.deleted <>1 and r.deleted<>1 and owner_user_id=" + sid + " and timestamp>'" + startdate + "' and timestamp<'" + enddate + "';");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                actperday.add(rs.getString("descr"));

            closeConnection();
        }
        catch(NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        return actperday;
    }

    public UsesTable fields(Integer sid, String startdate, String enddate)
    {
        UsesTable ut = new UsesTable();
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement(
                    "SELECT r.owner_user_id as ouid, COUNT(*) as count, COUNT( NULLIF( pronounciation, '' ) ) as pronounciation,  COUNT( NULLIF( acronym, '' ) ) as acronym,  COUNT( NULLIF( phraseology, '' ) ) as phraseology,  COUNT( NULLIF( rgt.use, '' ) ) as uses , COUNT( NULLIF( rgt.references, '' ) ) as source FROM resource_glossary_terms rgt, resource_glossary rg, resource r  where r.resource_id=rg.resource_id and rg.glossary_id=rgt.glossary_id  and rg.deleted <>1 and r.deleted<>1 and rgt.deleted<>1 and owner_user_id="
                            + sid + " and rg.timestamp>'" + startdate + "' and rg.timestamp<'" + enddate + "' group by r.owner_user_id");
            ResultSet rs = pstmt.executeQuery();
            if(!rs.isBeforeFirst())
            {
                ut.setUserid("" + sid);
                ut.setTotal(0);
                ut.setPronounciation(0);
                ut.setAcronym(0);
                ut.setPhraseology(0);
                ut.setUses(0);
                ut.setSource(0);
            }
            else
            {
                while(rs.next())
                {
                    ut.setUserid(rs.getString("ouid"));
                    ut.setTotal(Integer.parseInt(rs.getString("count")));
                    ut.setPronounciation(Integer.parseInt(rs.getString("pronounciation")));
                    ut.setAcronym(Integer.parseInt(rs.getString("acronym")));
                    ut.setPhraseology(Integer.parseInt(rs.getString("phraseology")));
                    ut.setUses(Integer.parseInt(rs.getString("uses")));
                    ut.setSource(Integer.parseInt(rs.getString("source")));
                }
            }
            closeConnection();
        }
        catch(NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        return ut;
    }

    public ArrayList<UsesTable> fields()
    {
        ArrayList<UsesTable> uts = new ArrayList<UsesTable>();
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement(
                    "SELECT r.owner_user_id as ouid, COUNT(*) as count, COUNT( NULLIF( pronounciation, '' ) ) as pronounciation,  COUNT( NULLIF( acronym, '' ) ) as acronym,  COUNT( NULLIF( phraseology, '' ) ) as phraseology,  COUNT( NULLIF( rgt.use, '' ) ) as uses , COUNT( NULLIF( rgt.references, '' ) ) as source FROM resource_glossary_terms rgt, resource_glossary rg, resource r  where r.resource_id=rg.resource_id and rg.glossary_id=rgt.glossary_id  and rg.deleted <>1 and r.deleted<>1 and rgt.deleted<>1 and owner_user_id IN ('10113','10112','10150','10110','10410','10430')  group by r.owner_user_id");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
            {
                UsesTable ut = new UsesTable();
                ut.setUserid(rs.getString("ouid"));
                ut.setTotal(Integer.parseInt(rs.getString("count")));
                ut.setPronounciation(Integer.parseInt(rs.getString("pronounciation")));
                ut.setAcronym(Integer.parseInt(rs.getString("acronym")));
                ut.setPhraseology(Integer.parseInt(rs.getString("phraseology")));
                ut.setUses(Integer.parseInt(rs.getString("uses")));
                ut.setSource(Integer.parseInt(rs.getString("source")));
                uts.add(ut);
            }
            closeConnection();
        }
        catch(NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }

        return uts;
    }

    public Map<String, Integer> proxySources(Integer sid, String startdate, String enddate)
    {
        Map<String, Integer> actperday = new TreeMap<String, Integer>();
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement("select  REPLACE(REPLACE(SUBSTRING_INDEX(referer, '/', 3),'.waps.io',''),'.secure','') as domain, count(*) as count from proxy_log where user_id=" + sid + " and date>'" + startdate + "' and date<'" + enddate
                    + "' and status_code < 400 group by (domain) order by count desc");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                actperday.put("" + rs.getString("domain"), Integer.parseInt("" + rs.getString("count")));

            closeConnection();
        }
        catch(NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        return actperday;
    }

    public Map<String, Integer> actionPerDay(String startdate, String enddate)
    {
        Map<String, Integer> actperday = new TreeMap<String, Integer>();
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement("select DATE(timestamp) as day,count(*) as count from user_log where timestamp>'" + startdate + "' and timestamp<'" + enddate + "' and user_id<> 8963 group by day");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                actperday.put("" + rs.getString("day"), Integer.parseInt("" + rs.getString("count")));

            closeConnection();
        }
        catch(NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        return actperday;
    }

    public Map<String, Integer> actionPerDay(Integer sid, String startdate, String enddate)
    {
        Map<String, Integer> actperday = new TreeMap<String, Integer>();
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement("select DATE(timestamp) as day,count(*) as count from user_log where timestamp>'" + startdate + "' and timestamp<'" + enddate + "' and user_id<> 8963 and user_id=" + sid + " group by day");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                actperday.put("" + rs.getString("day"), Integer.parseInt("" + rs.getString("count")));

            closeConnection();
        }
        catch(NumberFormatException e)
        {
            e.printStackTrace();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
        return actperday;
    }

}
