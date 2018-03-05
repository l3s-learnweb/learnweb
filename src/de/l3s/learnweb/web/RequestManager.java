package de.l3s.learnweb.web;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;

/**
 * Manages, stores and analyzes request data. Implements singleton since it has to be accessed by filter.
 *
 * @author Kate
 *
 */
public class RequestManager
{
    private final static Logger log = Logger.getLogger(RequestManager.class);

    private final Learnweb learnweb;

    //Basic maps/list
    private final Map<String, Date> banlist;
    private Queue<RequestData> requests;
    private final Map<String, Set<String>> logins;

    //Aggregated data info
    private List<AggregatedRequestData> aggregatedRequests = null;
    private Date aggrRequestsUpdated;

    private static RequestManager instance = null;

    public static RequestManager init(Learnweb learnweb)
    {
        if(instance == null)
        {
            instance = new RequestManager(learnweb);
            instance.loadBanlist();
        }

        return instance;
    }

    public static RequestManager instance()
    {
        return instance;
    }

    private RequestManager(Learnweb learnweb)
    {
        this.learnweb = learnweb;
        banlist = new ConcurrentHashMap<String, Date>();
        logins = new ConcurrentHashMap<String, Set<String>>();
        requests = new ConcurrentLinkedQueue<RequestData>();
        aggrRequestsUpdated = new Date(0);
        aggregatedRequests = new ArrayList<AggregatedRequestData>();
    }

    /**
     * Loads banlists from the database. Should be called by init;
     */
    private void loadBanlist()
    {
        try(PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT * FROM lw_bans WHERE type = 'IP' AND bandate > now()");)
        {
            ResultSet rs = select.executeQuery();
            while(rs.next())
            {
                banlist.put(rs.getString("name"), rs.getTimestamp("bandate"));
            }
        }
        catch(SQLException e)
        {
            log.error("Failed to load banlist. SQLException: ", e);
        }

        log.debug("Loaded IP banlist");
    }

    /**
     * Checks whether certain IP is banned.
     *
     * @return true if bantime of given IP is after today, false if it has already expired or
     */
    public boolean checkBanned(String ip)
    {
        Date bandate = banlist.get(ip);
        if(bandate != null)
        {
            return bandate.after(new Date());
        }
        else
        {
            return false;
        }
    }

    /**
     * Adds a given request to the requests list
     */
    public void recordRequest(String ip, String url)
    {
        Date time = new Date();
        requests.offer(new RequestData(ip, time, url));
    }

    /**
     * Records successful login into a Map(IP, Set(username)), thus matching every IP to usernames that were logged into from it.
     */
    public void recordLogin(String ip, String username)
    {
        Set<String> names = logins.get(ip);
        if(names == null)
        {
            names = new HashSet<String>();
            logins.put(ip, names);
        }

        names.add(username);
    }

    /**
     * Adds some fresh bans to the IP banlist.
     */
    public void addBan(String ip, Date bandate)
    {
        banlist.put(ip, bandate);
    }

    /**
     * Removes requests that are older than 1 hours from memory
     */
    public void cleanOldRequests()
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.HOUR, -1);

        Date threshold = cal.getTime();

        while(requests.peek().getTime().before(threshold))
        {
            logins.remove(requests.peek().getIP());
            requests.poll();
        }

    }

    /**
     * Logs the request data for the last hour into the database
     */
    public void recordRequestsToDB()
    {
        Map<String, Long> requestsByIP = requests.stream().collect(Collectors.groupingBy(RequestData::getIP, Collectors.counting()));

        try(PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT INTO lw_requests (IP, requests, logins, usernames, time) VALUES(?, ?, ?, ?, ?);"))
        {
            java.sql.Timestamp insertionTime = new java.sql.Timestamp(new Date().getTime());

            for(Entry<String, Long> entry : requestsByIP.entrySet())
            {
                Set<String> log = logins.get(entry.getKey());

                int loginCount = 0;
                String usernames = "";
                if(log != null)
                {
                    loginCount = log.size();
                    usernames = log.toString();
                }

                insert.setString(1, entry.getKey());
                insert.setInt(2, entry.getValue().intValue());
                insert.setInt(3, loginCount);
                insert.setString(4, usernames);
                insert.setTimestamp(5, insertionTime);

                insert.addBatch();
            }

            insert.executeBatch();

        }
        catch(SQLException e)
        {
            log.error("Recording requests to DB failed. SQLException: ", e);
        }

    }

    public Queue<RequestData> getRequests()
    {
        return requests;
    }

    public Map<String, Set<String>> getLogins()
    {
        return logins;
    }

    public List<AggregatedRequestData> getAggregatedRequests()
    {
        return aggregatedRequests;
    }

    /**
     * Loads the aggregated requests that happened after the last update
     */
    public void updateAggregatedRequests()
    {
        try(PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT * FROM lw_requests WHERE time >= ?"))
        {
            select.setTimestamp(1, new java.sql.Timestamp(aggrRequestsUpdated.getTime()));
            ResultSet rs = select.executeQuery();
            while(rs.next())
            {
                aggregatedRequests.add(new AggregatedRequestData(rs.getString("IP"), rs.getInt("requests"), rs.getInt("logins"), rs.getString("usernames"), rs.getTimestamp("time")));
            }
        }
        catch(SQLException e)
        {
            log.error("Failed to load AggregatedRequests. SQLException: ", e);
        }

        aggrRequestsUpdated = new Date();
    }

    /**
     * Clears the requests DB. Dev purposes only.
     */
    public void clearRequestsDB()
    {
        try(PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM lw_requests"))
        {
            delete.execute();
            aggregatedRequests = new ArrayList<AggregatedRequestData>();
        }
        catch(SQLException e)
        {
            log.error("Failed to load banlists. SQLException: ", e);
        }
    }

    public List<AggregatedRequestData> getAggrRequests()
    {
        return aggregatedRequests;
    }

    public Date getAggrRequestsUpdateTime()
    {
        return aggrRequestsUpdated;
    }

}
