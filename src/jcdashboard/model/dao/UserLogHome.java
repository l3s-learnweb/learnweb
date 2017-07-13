package jcdashboard.model.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Course;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.User;
import de.l3s.learnweb.UserManager;
import de.l3s.learnweb.beans.UserBean;
import de.l3s.learnweb.beans.UtilBean;
import jcdashboard.model.UsesTable;

// import org.apache.commons.logging.Log;
// import org.apache.commons.logging.LogFactory;

public class UserLogHome
{
    private static final Logger log = Logger.getLogger(UserLogHome.class);

    private Connection connect = null;

    private final Learnweb learnweb;

    public UserLogHome()
    {
        super();
        openConnection();
        learnweb = Learnweb.getInstance();
    }

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
                log.fatal("fatal sql error", e);
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
                log.fatal("fatal sql error", e);
            }
    }

    /**
     * Only used on http://localhost:8080/Learnweb-Tomcat/lw/admin/dashboard/dashboard.jsf
     * 
     * @return
     */
    public Map<String, Integer> actionPerDay()
    {
        Map<String, Integer> actperday = new TreeMap<String, Integer>();
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement("select DATE(timestamp) as day,count(*) as count from user_log where timestamp>'2017-03-02' and user_id<> 8963 group by day");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                actperday.put(rs.getString("day"), rs.getInt("count"));
            closeConnection();
        }
        catch(SQLException e)
        {
            log.fatal("fatal sql error", e);
        }
        return actperday;
    }

    /*
    public Map<String, Integer> actionCount()
    {
        Map<String, Integer> actperday = new TreeMap<String, Integer>();
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement("select action,count(*) as count from user_log where timestamp>'2017-03-02' and user_id<> 8963 group by action");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                actperday.put( rs.getString("action"), rs.getInt("count"));
            closeConnection();
        }
        catch(SQLException e)
        {
            log.fatal("fatal sql error", e);
        }
    
        return actperday;
    }
    */

    public Map<String, Integer> actionCount(String startdate, String enddate)
    {
        Map<String, Integer> actperday = new TreeMap<String, Integer>();
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement("select action,count(*) as count from user_log where timestamp>'" + startdate + "' and timestamp<'" + enddate + "' and user_id<> 8963 group by action");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                actperday.put(rs.getString("action"), rs.getInt("count"));
            closeConnection();
        }
        catch(SQLException e)
        {
            log.fatal("fatal sql error", e);
        }
        return actperday;
    }

    public Integer getTotalConcepts(String startdate, String enddate)
    {
        int result = 0;
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement(
                    "select count(distinct glossary_id) as count from resource_glossary rg, resource r where r.resource_id=rg.resource_id and rg.deleted<>1 and r.deleted<>1 and owner_user_id<> 8963 and timestamp>'" + startdate + "' and timestamp<'" + enddate + "'");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                result = rs.getInt("count");
            closeConnection();
        }
        catch(SQLException e)
        {
            log.fatal("fatal sql error", e);
        }
        return result;
    }

    public Integer getTotalTerms(String startdate, String enddate)
    {
        int result = 0;
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement(
                    "select count(*) as count from resource_glossary rg, resource_glossary_terms rgt where rg.glossary_id=rgt.glossary_id and resource_id IN (select resource_id from resource where owner_user_id<> 8963 and deleted<>1) and rgt.deleted <>1 and rg.deleted<>1 and rg.timestamp>'"
                            + startdate + "' and rg.timestamp<'" + enddate + "'");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                result = rs.getInt("count");
            closeConnection();

        }
        catch(SQLException e)
        {
            log.fatal("fatal sql error", e);
        }
        return result;
    }

    public Integer getTotalConcepts(Integer userId, String startdate, String enddate)
    {
        int result = 0;
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement(
                    "select count(distinct glossary_id) as count from resource_glossary rg, resource r where r.resource_id=rg.resource_id and rg.deleted<>1 and r.deleted<>1 and owner_user_id=" + userId + " and timestamp>'" + startdate + "' and timestamp<'" + enddate + "'");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                result = rs.getInt("count");
            closeConnection();

        }
        catch(SQLException e)
        {
            log.fatal("fatal sql error", e);
        }
        return result;
    }

    public Integer getTotalTerms(Integer userId, String startdate, String enddate)
    {
        int result = 0;
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement("select count(*) as count from resource_glossary rg, resource_glossary_terms rgt where rg.glossary_id=rgt.glossary_id and resource_id IN (select resource_id from resource where owner_user_id=" + userId
                    + " and deleted<>1) and rgt.deleted <>1 and rg.deleted<>1 and rg.timestamp>'" + startdate + "' and rg.timestamp<'" + enddate + "'");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                result = rs.getInt("count");
            // closeConnection();

        }
        catch(SQLException e)
        {
            log.fatal("fatal sql error", e);
        }
        return result;
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
                    actperday.put("EMPTY", rs.getInt("count"));
                else
                    actperday.put(rs.getString("refs"), rs.getInt("count"));

            closeConnection();
        }
        catch(SQLException e)
        {
            log.fatal("fatal sql error", e);
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
                    actperday.put("EMPTY", rs.getInt("count"));
                else
                    actperday.put(rs.getString("refs"), rs.getInt("count"));

            closeConnection();
        }
        catch(SQLException e)
        {
            log.fatal("fatal sql error", e);
        }
        return actperday;
    }

    public Map<String, Integer> glossarySource(Integer userId, String startdate, String enddate)
    {
        Map<String, Integer> actperday = new TreeMap<String, Integer>();
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement(
                    "select rgt.references as refs,count(*) as count from resource_glossary_terms rgt,resource_glossary rg, resource r where r.resource_id=rg.resource_id and rg.glossary_id=rgt.glossary_id and rg.deleted <>1 and r.deleted<>1 and rgt.deleted<>1 and owner_user_id="
                            + userId + " and rg.timestamp>'" + startdate + "' and rg.timestamp<'" + enddate + "' group by rgt.references");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                if(rs.getString("refs").trim().compareTo("") == 0)
                    actperday.put("EMPTY", rs.getInt("count"));
                else
                    actperday.put(rs.getString("refs"), rs.getInt("count"));

            closeConnection();
        }
        catch(SQLException e)
        {
            log.fatal("fatal sql error", e);
        }
        return actperday;
    }

    public Map<String, Integer> glossarySource(Integer userId)
    {
        Map<String, Integer> actperday = new TreeMap<String, Integer>();

        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement(
                    "select rgt.references as refs,count(*) as count from resource_glossary_terms rgt,resource_glossary rg, resource r where r.resource_id=rg.resource_id and rg.glossary_id=rgt.glossary_id and rg.deleted <>1 and r.deleted<>1 and rgt.deleted<>1 and owner_user_id="
                            + userId + " group by rgt.references");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                if(rs.getString("refs").trim().compareTo("") == 0)
                    actperday.put("EMPTY", rs.getInt("count"));
                else
                    actperday.put(rs.getString("refs"), rs.getInt("count"));

            closeConnection();
        }
        catch(SQLException e)
        {
            log.fatal("fatal sql error", e);
        }
        return actperday;
    }

    /*
     * never used
     
    public Map<String, Integer> userGlossary()
    {
        Map<String, Integer> actperday = new TreeMap<String, Integer>();
    
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement(
                    "select owner_user_id, count(distinct rgt.glossary_id) as count from resource_glossary rg, resource r, resource_glossary_terms rgt where r.resource_id=rg.resource_id and rg.glossary_id=rgt.glossary_id and rg.deleted <>1 and rg.resource_id IN (select resource_id from resource where owner_user_id<> 8963) and rg.deleted<>1 and rgt.deleted<>1 group by owner_user_id order by owner_user_id ");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                actperday.put( rs.getString("owner_user_id"), rs.getInt("count"));
    
            closeConnection();
        }
        catch(SQLException e)
        {
            log.fatal("fatal sql error", e);
        }
        return actperday;
    }
    */

    public Map<String, Integer> getUserGlossaryConceptCountByCourse(Course course, String startdate, String enddate)
    {
        Map<String, Integer> conceptsPerUser = new TreeMap<String, Integer>();

        try
        {
            /* Old query:
            PreparedStatement pstmt = openConnection().prepareStatement(
                    "select owner_user_id, count(distinct rgt.glossary_id) as count from resource_glossary rg, resource r, resource_glossary_terms rgt where r.resource_id=rg.resource_id and rg.glossary_id=rgt.glossary_id and rg.deleted <>1 and rg.resource_id IN (select resource_id from resource where owner_user_id<> 8963) and rg.deleted<>1 and rgt.deleted<>1 and rg.timestamp>'"
                            + startdate + "' and rg.timestamp<'" + enddate + "' group by owner_user_id order by owner_user_id ");
             */

            UserBean userBean = UtilBean.getUserBean();

            PreparedStatement pstmt = learnweb.getConnection().prepareStatement(
                    "SELECT user_id, username, count( * ) AS count FROM lw_user_course c JOIN lw_resource r ON c.user_id = r.owner_user_id JOIN lw_user USING (user_id) JOIN lw_resource_glossary rg USING (resource_id)  WHERE c.course_id =? AND rg.deleted !=1 AND r.deleted !=1 AND rg.timestamp > ? AND rg.timestamp < ? GROUP BY r.owner_user_id ORDER BY username");
            pstmt.setInt(1, course.getId());
            pstmt.setString(2, startdate);
            pstmt.setString(3, enddate);
            //log.debug(pstmt);

            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
            {
                conceptsPerUser.put(userBean.anonymizeUsername(rs.getInt("user_id"), rs.getString("username")), rs.getInt("count"));
            }
        }
        catch(SQLException e)
        {
            log.fatal("fatal sql error", e);
        }
        return conceptsPerUser;
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
                actperday.put(rs.getString("owner_user_id"), rs.getInt("count"));

            closeConnection();
        }
        catch(SQLException e)
        {
            log.fatal("fatal sql error", e);
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
                actperday.put(rs.getString("owner_user_id"), rs.getInt("count"));

            closeConnection();
        }
        catch(SQLException e)
        {
            log.fatal("fatal sql error", e);
        }
        return actperday;
    }

    public Map<String, Integer> actionPerDay(Integer userId)
    {
        Map<String, Integer> actperday = new TreeMap<String, Integer>();
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement("select DATE(timestamp) as day,count(*) as count from user_log where timestamp>'2017-03-02' and user_id<> 8963 and user_id=" + userId + " group by day");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                actperday.put(rs.getString("day"), rs.getInt("count"));

            closeConnection();
        }
        catch(SQLException e)
        {
            log.fatal("fatal sql error", e);
        }
        return actperday;
    }

    public Map<String, Integer> actionCount(Integer userId)
    {
        Map<String, Integer> actperday = new TreeMap<String, Integer>();
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement("select action,count(*) as count from user_log where timestamp>'2017-03-02' and user_id<> 8963 and user_id=" + userId + " group by action");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                actperday.put(rs.getString("action"), rs.getInt("count"));

            closeConnection();
        }
        catch(SQLException e)
        {
            log.fatal("fatal sql error", e);
        }
        return actperday;
    }

    public Map<String, Integer> actionCount(Integer userId, String startdate, String enddate)
    {
        Map<String, Integer> actperday = new TreeMap<String, Integer>();
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement("select action,count(*) as count from user_log where timestamp>'" + startdate + "' and timestamp<'" + enddate + "' and user_id<> 8963 and user_id=" + userId + " group by action");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                actperday.put(rs.getString("action"), rs.getInt("count"));

            closeConnection();
        }
        catch(SQLException e)
        {
            log.fatal("fatal sql error", e);
        }
        return actperday;
    }

    public Integer getTotalConcepts(Integer userId)
    {
        int result = 0;
        try
        {
            PreparedStatement pstmt = openConnection()
                    .prepareStatement("select count(distinct glossary_id) as count from resource_glossary rg, resource r where r.resource_id=rg.resource_id and rg.deleted<>1 and r.deleted<>1 and owner_user_id=" + userId + " and timestamp>'2017-03-02'");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                result = rs.getInt("count");
            closeConnection();

        }
        catch(SQLException e)
        {
            log.fatal("fatal sql error", e);
        }
        return result;
    }

    public Integer getTotalTerms(Integer userId)
    {
        int result = 0;
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement("select count(*) as count from resource_glossary rg, resource_glossary_terms rgt where rg.glossary_id=rgt.glossary_id and resource_id IN (select resource_id from resource where owner_user_id=" + userId
                    + " and deleted<>1) and rgt.deleted <>1 and rg.deleted<>1 and rg.timestamp>'2017-03-02'");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                result = rs.getInt("count");
            closeConnection();

        }
        catch(SQLException e)
        {
            log.fatal("fatal sql error", e);
        }
        return result;
    }

    public Integer getTotalSource(Integer userId)
    {
        int result = 0;
        try
        {
            PreparedStatement pstmt = openConnection()
                    .prepareStatement("select count(distinct rgt.references) as count from resource_glossary rg, resource_glossary_terms rgt where rg.glossary_id=rgt.glossary_id and resource_id IN (select resource_id from resource where owner_user_id=" + userId
                            + " and deleted<>1) and rgt.deleted <>1 and rg.deleted<>1 and rg.timestamp>'2017-03-02'");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                result = rs.getInt("count");
            closeConnection();

        }
        catch(SQLException e)
        {
            log.fatal("fatal sql error", e);
        }
        return result;
    }

    public Integer getTotalSource(Integer userId, String startdate, String enddate)
    {
        int result = 0;
        try
        {
            PreparedStatement pstmt = openConnection()
                    .prepareStatement("select count(distinct rgt.references) as count from resource_glossary rg, resource_glossary_terms rgt where rg.glossary_id=rgt.glossary_id and resource_id IN (select resource_id from resource where owner_user_id=" + userId
                            + " and deleted<>1) and rgt.deleted <>1 and rg.deleted<>1 and rg.timestamp>'" + startdate + "' and rg.timestamp<'" + enddate + "'");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                result = rs.getInt("count");
            closeConnection();

        }
        catch(SQLException e)
        {
            log.fatal("fatal sql error", e);
        }
        return result;
    }

    public Integer getTotalSourceNoempty(Integer userId, String startdate, String enddate)
    {
        int result = 0;
        try
        {
            PreparedStatement pstmt = openConnection()
                    .prepareStatement("select count(distinct rgt.references) as count from resource_glossary rg, resource_glossary_terms rgt where rg.glossary_id=rgt.glossary_id and resource_id IN (select resource_id from resource where owner_user_id=" + userId
                            + " and deleted<>1) and rgt.references<>'' and rgt.deleted <>1 and rg.deleted<>1 and rg.timestamp>'" + startdate + "' and rg.timestamp<'" + enddate + "'");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                result = rs.getInt("count");
            // closeConnection();

        }
        catch(SQLException e)
        {
            log.fatal("fatal sql error", e);
        }
        return result;
    }

    public Map<String, Integer[]> getSummary2(String startdate, String enddate)
    {
        int result = 0, result2 = 0;
        Map<String, Integer[]> summary = new TreeMap<String, Integer[]>();
        try
        {

            // getTotalConcepts
            PreparedStatement pstmt = openConnection().prepareStatement(
                    "select owner_user_id,count(distinct glossary_id) as count from resource_glossary rg, resource r where r.resource_id=rg.resource_id and rg.deleted<>1 and r.deleted<>1 and timestamp>'" + startdate + "' and timestamp<'" + enddate + "' group by owner_user_id ");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
            {
                result = rs.getInt("count");
                Integer[] list = new Integer[3];
                list[0] = result;
                summary.put(rs.getString("owner_user_id"), list);
            }

            // getTotalTerms
            pstmt = openConnection().prepareStatement(
                    "select owner_user_id, count(*) as count , count(distinct rgt.references) as count2 from resource_glossary rg, resource_glossary_terms rgt, resource r where rg.glossary_id=rgt.glossary_id and r.resource_id=rg.resource_id and r.deleted<>1 and rgt.deleted <>1 and rg.deleted<>1 and rg.timestamp>'"
                            + startdate + "' and rg.timestamp<'" + enddate + "' group by owner_user_id");
            rs = pstmt.executeQuery();
            while(rs.next())
            {
                result = rs.getInt("count");
                result2 = rs.getInt("count2");
                Integer[] list = summary.get(rs.getString("owner_user_id"));
                list[1] = result;
                list[2] = result2;
                summary.put(rs.getString("owner_user_id"), list);
            }

        }
        catch(SQLException e)
        {
            log.fatal("fatal sql error", e);
        }
        return summary;
    }

    public Map<String, Integer> getSummary(Integer userId, String startdate, String enddate)
    {
        int result = 0, result2 = 0;
        Map<String, Integer> summary = new TreeMap<String, Integer>();
        try
        {

            // getTotalConcepts
            PreparedStatement pstmt = openConnection().prepareStatement(
                    "select count(distinct glossary_id) as count from resource_glossary rg, resource r where r.resource_id=rg.resource_id and rg.deleted<>1 and r.deleted<>1 and owner_user_id=" + userId + " and timestamp>'" + startdate + "' and timestamp<'" + enddate + "'");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                result = rs.getInt("count");
            summary.put("concepts", result);

            // getTotalTerms
            pstmt = openConnection()
                    .prepareStatement("select count(*) as count , count(distinct rgt.references) as count2 from resource_glossary rg, resource_glossary_terms rgt where rg.glossary_id=rgt.glossary_id and resource_id IN (select resource_id from resource where owner_user_id="
                            + userId + " and deleted<>1) and rgt.deleted <>1 and rg.deleted<>1 and rg.timestamp>'" + startdate + "' and rg.timestamp<'" + enddate + "'");
            rs = pstmt.executeQuery();
            while(rs.next())
            {
                result = rs.getInt("count");
                result2 = rs.getInt("count2");
            }
            summary.put("terms", result);
            summary.put("sources", result2);

            // getTotalSourceNoempty
            /*pstmt = openConnection().prepareStatement("select count(distinct rgt.references) as count from resource_glossary rg, resource_glossary_terms rgt where rg.glossary_id=rgt.glossary_id and resource_id IN (select resource_id from resource where owner_user_id="+sid+" and deleted<>1) and rgt.references<>'' and rgt.deleted <>1 and rg.deleted<>1 and rg.timestamp>'"+startdate+"' and rg.timestamp<'"+enddate+"'");
            rs = pstmt.executeQuery();
            while (rs.next()) 
                result=rs.getString("count");
            summary.put("sources",Integer.parseInt(""+result));
            */
            // closeConnection();

        }
        catch(SQLException e)
        {
            log.fatal("fatal sql error", e);
        }
        return summary;
    }

    public List<String> descritpions(Integer userId)
    {
        List<String> actperday = new ArrayList<String>();
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement("select rg.description as descr from resource_glossary rg, resource r where r.resource_id=rg.resource_id and rg.deleted <>1 and r.deleted<>1 and owner_user_id=" + userId + " and timestamp>'2017-03-02';");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                actperday.add(rs.getString("descr"));

            closeConnection();
        }
        catch(SQLException e)
        {
            log.fatal("fatal sql error", e);
        }

        return actperday;
    }

    public List<String> descritpions(Integer userId, String startdate, String enddate)
    {
        List<String> actperday = new ArrayList<String>();
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement(
                    "select rg.description as descr from resource_glossary rg, resource r where r.resource_id=rg.resource_id and rg.deleted <>1 and r.deleted<>1 and owner_user_id=" + userId + " and timestamp>'" + startdate + "' and timestamp<'" + enddate + "';");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                actperday.add(rs.getString("descr"));

            closeConnection();
        }
        catch(SQLException e)
        {
            log.fatal("fatal sql error", e);
        }
        return actperday;
    }

    public UsesTable fields(Integer userId, String startdate, String enddate)
    {
        UsesTable ut = new UsesTable();
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement(
                    "SELECT r.owner_user_id as ouid, COUNT(*) as count, COUNT( NULLIF( pronounciation, '' ) ) as pronounciation,  COUNT( NULLIF( acronym, '' ) ) as acronym,  COUNT( NULLIF( phraseology, '' ) ) as phraseology,  COUNT( NULLIF( rgt.use, '' ) ) as uses , COUNT( NULLIF( rgt.references, '' ) ) as source FROM resource_glossary_terms rgt, resource_glossary rg, resource r  where r.resource_id=rg.resource_id and rg.glossary_id=rgt.glossary_id  and rg.deleted <>1 and r.deleted<>1 and rgt.deleted<>1 and owner_user_id="
                            + userId + " and rg.timestamp>'" + startdate + "' and rg.timestamp<'" + enddate + "' group by r.owner_user_id");
            ResultSet rs = pstmt.executeQuery();
            if(!rs.isBeforeFirst())
            {
                ut.setUserid(userId + "");
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
                    ut.setTotal(rs.getInt("count"));
                    ut.setPronounciation(rs.getInt("pronounciation"));
                    ut.setAcronym(rs.getInt("acronym"));
                    ut.setPhraseology(rs.getInt("phraseology"));
                    ut.setUses(rs.getInt("uses"));
                    ut.setSource(rs.getInt("source"));
                }
            }
            closeConnection();
        }
        catch(SQLException e)
        {
            log.fatal("fatal sql error", e);
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
                ut.setTotal(rs.getInt("count"));
                ut.setPronounciation(rs.getInt("pronounciation"));
                ut.setAcronym(rs.getInt("acronym"));
                ut.setPhraseology(rs.getInt("phraseology"));
                ut.setUses(rs.getInt("uses"));
                ut.setSource(rs.getInt("source"));
                uts.add(ut);
            }
            closeConnection();
        }
        catch(SQLException e)
        {
            log.fatal("fatal sql error", e);
        }

        return uts;
    }

    public Map<String, Integer> proxySources(Integer userId, String startdate, String enddate)
    {
        Map<String, Integer> actperday = new TreeMap<String, Integer>();
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement("select  REPLACE(REPLACE(SUBSTRING_INDEX(referer, '/', 3),'.waps.io',''),'.secure','') as domain, count(*) as count from proxy_log where user_id=" + userId + " and date>'" + startdate + "' and date<'"
                    + enddate + "' and status_code < 400 group by (domain) order by count desc");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                actperday.put(rs.getString("domain"), rs.getInt("count"));

            closeConnection();
        }
        catch(SQLException e)
        {
            log.fatal("fatal sql error", e);
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
                actperday.put(rs.getString("day"), rs.getInt("count"));

            closeConnection();
        }
        catch(SQLException e)
        {
            log.fatal("fatal sql error", e);
        }
        return actperday;
    }

    public Map<String, Integer> actionPerDay(Integer userId, String startdate, String enddate)
    {
        Map<String, Integer> actperday = new TreeMap<String, Integer>();
        try
        {
            PreparedStatement pstmt = openConnection().prepareStatement("select DATE(timestamp) as day,count(*) as count from user_log where timestamp>'" + startdate + "' and timestamp<'" + enddate + "' and user_id<> 8963 and user_id=" + userId + " group by day");
            ResultSet rs = pstmt.executeQuery();
            while(rs.next())
                actperday.put(rs.getString("day"), rs.getInt("count"));

            closeConnection();
        }
        catch(SQLException e)
        {
            log.fatal("fatal sql error", e);
        }
        return actperday;
    }

    public Collection<HashMap<String, Object>> getTrackerStatisticsPerUser(Course course, String startdate, String enddate) throws SQLException
    {
        if(course.getMemberCount() == 0) // no course members -> nothing to return
            return null;

        StringBuilder sb = new StringBuilder();
        for(User user : course.getMembers())
        {
            sb.append(',');
            sb.append(user.getId());
        }
        String userIds = sb.substring(1);

        UserManager userManager = learnweb.getUserManager();
        HashMap<Integer, HashMap<String, Object>> mergedStatistics = new HashMap<Integer, HashMap<String, Object>>();
        List<TrackerStatistic> learnwebStatistics = getTrackerUserStatistics(1, userIds, startdate, enddate);
        List<TrackerStatistic> proxyStatistics = getTrackerUserStatistics(2, userIds, startdate, enddate);

        // merge Learnweb and Proxy statistics into one table
        for(TrackerStatistic learnwebStatistic : learnwebStatistics)
        {
            HashMap<String, Object> mergedStatistic = new HashMap<>();
            mergedStatistic.put("user", userManager.getUser(learnwebStatistic.getUserId()));
            mergedStatistic.put("learnweb", learnwebStatistic);
            mergedStatistics.put(learnwebStatistic.getUserId(), mergedStatistic);
        }

        for(TrackerStatistic proxyStatistic : proxyStatistics)
        {
            HashMap<String, Object> mergedStatistic = mergedStatistics.get(proxyStatistic.getUserId());
            if(mergedStatistic == null)
            {
                mergedStatistic = new HashMap<>();
                mergedStatistic.put("user", userManager.getUser(proxyStatistic.getUserId()));
            }
            mergedStatistic.put("proxy", proxyStatistic);
            mergedStatistics.put(proxyStatistic.getUserId(), mergedStatistic);
        }

        // sum proxy and learnweb statistc
        for(HashMap<String, Object> entry : mergedStatistics.values())
        {
            TrackerStatistic proxyStatistic = (TrackerStatistic) entry.get("proxy");
            TrackerStatistic learnwebStatistic = (TrackerStatistic) entry.get("learnweb");

            TrackerStatistic summarizedStatistic;
            if(proxyStatistic != null && learnwebStatistic != null)
                summarizedStatistic = new TrackerStatistic(learnwebStatistic.getUserId(), learnwebStatistic.getTotalEvents() + proxyStatistic.getTotalEvents(), learnwebStatistic.getTimeStay() + proxyStatistic.getTimeStay(),
                        learnwebStatistic.getTimeActive() + proxyStatistic.getTimeActive(), learnwebStatistic.getClicks() + proxyStatistic.getClicks(), learnwebStatistic.getKeypresses() + proxyStatistic.getKeypresses());
            else if(proxyStatistic != null)
                summarizedStatistic = proxyStatistic;
            else if(learnwebStatistic != null)
                summarizedStatistic = learnwebStatistic;
            else
                throw new IllegalStateException();

            entry.put("summarized", summarizedStatistic);
        }
        return mergedStatistics.values();
    }

    /**
     * 
     * @param clientId 1 == Learnweb, 2 == Learnweb proxy
     * @param userIds comma separated list of user ids
     * @param startdate
     * @param enddate
     * @return
     * @throws SQLException
     */
    private List<TrackerStatistic> getTrackerUserStatistics(int clientId, String userIds, String startdate, String enddate) throws SQLException
    {
        List<TrackerStatistic> statistic = new LinkedList<TrackerStatistic>();

        PreparedStatement select = learnweb.getConnection().prepareStatement(
                "SELECT `external_user_id` as user_id, sum(`total_events`) as total_events, sum(`time_stay`) as time_stay, sum(`time_active`) as time_active, sum(`clicks`) as clicks, sum(`keypress`) as keypresses FROM tracker.`track` WHERE `external_client_id` = ? AND `external_user_id` IN ("
                        + userIds + ") AND created_at BETWEEN ? AND ? GROUP BY `external_user_id`");

        select.setString(1, Integer.toString(clientId));
        select.setString(2, startdate);
        select.setString(3, enddate);
        log.debug(select);
        ResultSet rs = select.executeQuery();

        while(rs.next())
        {
            statistic.add(new TrackerStatistic(rs.getInt("user_id"), rs.getInt("total_events"), rs.getInt("time_stay"), rs.getInt("time_active"), rs.getInt("clicks"), rs.getInt("keypresses")));
        }
        return statistic;
    }

    public static class TrackerStatistic
    {
        private int userId;
        private int totalEvents;
        private int timeStay;
        private int timeActive;
        private int clicks;
        private int keypresses;
        private long timeActiveInMinutes;
        private String timeActiveFormatted;
        private long timeStayInMinutes;
        private String timeStayFormatted;

        public TrackerStatistic(int userId, int totalEvents, int timeStay, int timeActive, int clicks, int keypresses)
        {
            super();
            this.userId = userId;
            this.totalEvents = totalEvents;
            this.timeStay = timeStay;
            this.timeActive = timeActive;
            this.clicks = clicks;
            this.keypresses = keypresses;

            Duration durationActive = Duration.ofMillis(timeActive);
            this.timeActiveInMinutes = durationActive.toMinutes();
            this.timeActiveFormatted = formatDuration(durationActive);

            Duration durationStay = Duration.ofMillis(timeStay);
            this.timeStayInMinutes = durationStay.toMinutes();
            this.timeStayFormatted = formatDuration(durationStay);
        }

        private static String formatDuration(Duration d)
        {
            long hours = d.toHours();
            long minutes = d.minusHours(hours).toMinutes();

            StringBuilder output = new StringBuilder();
            if(hours > 0)
            {
                output.append(hours);
                output.append("h ");
            }

            output.append(minutes);
            if(minutes < 10)
                output.append('0');
            output.append("m");

            return output.toString();
        }

        public int getUserId()
        {
            return userId;
        }

        public int getTotalEvents()
        {
            return totalEvents;
        }

        public int getTimeStay()
        {
            return timeStay;
        }

        public int getTimeActive()
        {
            return timeActive;
        }

        public int getClicks()
        {
            return clicks;
        }

        public int getKeypresses()
        {
            return keypresses;
        }

        public long getTimeActiveInMinutes()
        {
            return timeActiveInMinutes;
        }

        public String getTimeActiveFormatted()
        {
            return timeActiveFormatted;
        }

        public long getTimeStayInMinutes()
        {
            return timeStayInMinutes;
        }

        public String getTimeStayFormatted()
        {
            return timeStayFormatted;
        }

        @Override
        public String toString()
        {
            return "TrackerStatistic [userId=" + userId + ", totalEvents=" + totalEvents + ", timeStay=" + timeStay + ", timeActive=" + timeActive + ", clicks=" + clicks + ", keypresses=" + keypresses + "]";
        }

    }

    public static void main(String[] args) throws SQLException, ClassNotFoundException
    {
        Learnweb learnweb = Learnweb.createInstance("/Learnweb-Tomcat");

        UserLogHome ulh = new UserLogHome();
        Course course = learnweb.getCourseManager().getCourseById(1245);
        String startdate = "2017-03-02";
        String enddate = "2017-04-02";

        log.debug(ulh.getTrackerStatisticsPerUser(course, startdate, enddate));
    }

}
