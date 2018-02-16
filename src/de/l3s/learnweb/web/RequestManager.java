package de.l3s.learnweb.web;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.loginprotection.SimpleProtectionManager;

/**
 * Manages, stores and analyzes request data. Implements singleton since it has to be accessed by filter.
 *
 * @author Kate
 *
 */
public class RequestManager
{
    private final static Logger log = Logger.getLogger(SimpleProtectionManager.class);
    private final static Logger requestLog = Logger.getLogger("lw.requests");

    private final Learnweb learnweb;

    private Map<String, Date> banlist;
    private List<RequestData> requests;

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
        banlist = new HashMap<String, Date>();
        requests = new ArrayList<RequestData>();
    }

    /**
     * Loads banlists from the database. Should be called by init;
     */
    private void loadBanlist()
    {
        try
        {
            PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT * FROM lw_bans WHERE type='IP' AND bandate > now()");

            ResultSet rs = select.executeQuery();
            while(rs.next())
            {
                banlist.put(rs.getString("name"), rs.getDate("bandate"));
            }
            select.close();
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
    public void recordRequest(String ip, Date time, String url)
    {
        requests.add(new RequestData(ip, time, url));
        requestLog.info(ip + "," + DateFormatUtils.format(time, "dd/MM/yy HH:mm:SS") + "," + url);
    }

    /**
     * Removes requests that are older than one week from the memory.
     * Note: This doesn't erase them from the requests.csv, which saves every request.
     */
    public void cleanOldRequests()
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DATE, -7);

        Date threshold = cal.getTime();

        //TODO: Optimaze this, current algorithm is awful!
        for(RequestData req : requests)
        {
            if(req.time.before(threshold))
            {
                requests.remove(req);
            }
        }

    }

    /**
     * Writes the data on this day's requests.
     */
    public void compileReport()
    {
        //TODO: Oh boy
    }

}
