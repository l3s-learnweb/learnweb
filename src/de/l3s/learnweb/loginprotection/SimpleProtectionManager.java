package de.l3s.learnweb.loginprotection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;

/**
 * This class manages brute force login protection. Uses singleton model.
 * <br>
 * Keeps a list of both IPs and usernames that have had multiple login attempts and bans them (temporarily or permamently until admin unblock).
 *
 * @author Kate
 *
 */
public class SimpleProtectionManager implements ProtectionManager
{
    private final static Logger log = Logger.getLogger(SimpleProtectionManager.class);
    private final Learnweb learnweb;

    //Two separate hashmaps that keep track of failed attempts and bantimes
    private Map<String, AccessData> usernameMap;
    private Map<String, AccessData> IPMap;

    public SimpleProtectionManager(Learnweb learnweb)
    {
        this.learnweb = learnweb;
        usernameMap = new ConcurrentHashMap<String, AccessData>();
        IPMap = new ConcurrentHashMap<String, AccessData>();
        loadBanLists();
    }

    /**
     * Loads banlists from the database. Should be called by every constructor.
     */
    private void loadBanLists()
    {
        try(PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT * FROM lw_bans"))
        {

            ResultSet rs = select.executeQuery();
            while(rs.next())
            {
                if(rs.getString("type").equals("user"))
                {
                    usernameMap.put(rs.getString("name"), new AccessData(1, rs.getTimestamp("bandate"), rs.getString("name")));
                }
                else if(rs.getString("type").equals("IP"))
                {
                    IPMap.put(rs.getString("name"), new AccessData(1, rs.getTimestamp("bandate"), rs.getString("name")));
                }
            }

        }
        catch(SQLException e)
        {
            log.error("Failed to load banlists. SQLException: ", e);
        }

        log.debug("Loaded banlists of usernames and IPs with " + usernameMap.size() + " and " + IPMap.size() + " entries respectively.");
    }

    /**
     * Returns current banlist. Includes both bans by IP and by username.
     */
    @Override
    public List<AccessData> getBanlist()
    {
        List<AccessData> banlist = new ArrayList<AccessData>();
        banlist.addAll(IPMap.values());
        banlist.addAll(usernameMap.values());
        return banlist;
    }

    @Override
    public AccessData getIPData(String IP)
    {
        return IPMap.get(IP);
    }

    @Override
    public AccessData getUsernameData(String username)
    {
        return usernameMap.get(username);
    }

    /**
     * Records a failed attempt to log in. If attempts to log in exceed some given amount, adjusts bantime:
     * <br>
     * At 50+ failed attempts username/IP gets a permaban that can only be lifted by admins. Or by being a vampire that can live for the next 400
     * years.
     * <br>
     * At 10+ failed attempts username/IP gets a 2-hour ban, incrementing by 2 for each failed attempt
     */
    @Override
    public void updateFailedAttempts(String IP, String username)
    {
        AccessData ipData = IPMap.get(IP);
        AccessData usernameData = usernameMap.get(username);

        if(ipData == null)
        {
            ipData = new AccessData(IP);
            IPMap.put(IP, ipData);
            ipData.setAttempts(ipData.getAttempts() + 1);
        }
        else
        {
            ipData.setAttempts(ipData.getAttempts() + 1);

            if(ipData.getAttempts() >= 10)
            {
                int bantime = 2 + 2 * ipData.getAttempts() % 10;
                ban(ipData, bantime, true);
                log.debug("Banned IP " + IP + " for " + bantime + " hours after " + ipData.getAttempts() + " failed login attempts");

            }
            else if(ipData.getAttempts() >= 50)
            {
                ipData.permaban();
                ban(ipData, -1, true);
                log.debug("Permabanned IP " + IP + " for excessive failed login attempts");
            }
        }

        if(usernameData == null)
        {
            usernameData = new AccessData(username);
            usernameMap.put(username, usernameData);
            usernameData.setAttempts(usernameData.getAttempts() + 1);

        }
        else
        {
            usernameData.setAttempts(usernameData.getAttempts() + 1);
            if(usernameData.getAttempts() >= 10)
            {
                int bantime = 2 + 2 * usernameData.getAttempts() % 10;
                ban(usernameData, bantime, false);
                log.debug("Banned username " + username + " for " + bantime + " hours after " + usernameData.getAttempts() + " failed login attempts");
            }
            else if(usernameData.getAttempts() >= 50)
            {
                usernameData.permaban();
                ban(usernameData, -1, false);
                log.debug("Permabanned username " + username + " for excessive failed login attempts");
            }
        }

    }

    /**
     * Bans selected IP or username for a given amount of time (or unlimited time) and updates the relevant database table.
     *
     * @param name IP that will be banned
     * @param bantime Duration of the ban (in hours). Negative bantime values equals permaban.
     * @param isIP Whether the given name is an IP (true) or username (false)
     * @param permaban Whether the ban is temporary or permament
     */
    @Override
    public void ban(AccessData accData, int bantime, boolean isIP)
    {
        if(bantime < 0)
        {
            accData.permaban();
        }
        else
        {
            accData.setBan(bantime);
        }

        try(PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT DELAYED INTO lw_bans (name, bandate, type) VALUES(?, ? ,?) ON DUPLICATE KEY UPDATE bandate=VALUES(bandate)"))
        {
            insert.setString(1, accData.getName());
            insert.setTimestamp(2, new java.sql.Timestamp(accData.getBanDate().getTime()));
            if(isIP)
            {
                insert.setString(3, "IP");
            }
            else
            {
                insert.setString(3, "user");
            }

            insert.execute();

            log.debug("Banned " + accData.getName() + " until " + accData.getBanDate());

            if(isIP)
            {
                learnweb.getRequestManager().addBan(accData.getName(), accData.getBanDate());
            }
        }
        catch(SQLException e)
        {
            log.error("Ban attempt failed. SQLException: ", e);
        }
    }

    @Override
    public void ban(String name, int bantime, boolean isIP)
    {
        AccessData accData;

        if(isIP)
        {
            accData = IPMap.get(name);
            if(accData == null)
            {
                accData = new AccessData(name);
                IPMap.put(name, accData);
            }
        }
        else
        {
            accData = usernameMap.get(name);
            if(accData == null)
            {
                accData = new AccessData(name);
                usernameMap.put(name, accData);
            }
        }

        ban(accData, bantime, isIP);

    }

    /**
     * Unbans a given name or IP. To be used by moderators\admins.
     *
     * @param name Name\Address that will be cleared of their sins
     */
    @Override
    public void unban(String name)
    {
        IPMap.remove(name);
        usernameMap.remove(name);

        try(PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM lw_bans WHERE name=?");)
        {
            delete.setString(1, name);
            delete.execute();
        }
        catch(SQLException e)
        {
            log.error("Ban removal attempt failed. SQLException: ", e);
        }

        log.debug("Unbanned " + name);
    }

    /**
     * Clears all and every ban. Use for debugging purposes only.
     */
    @Override
    public void clearBans()
    {
        IPMap.clear();
        usernameMap.clear();

        try(PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM lw_bans");)
        {
            delete.execute();
        }
        catch(SQLException e)
        {
            log.error("Ban clearing failed. SQLException: ", e);
        }

        log.debug("Banlist cleared.");
    }

    /**
     * Erases all banlist entries that have expired more than 3 days ago.
     */
    @Override
    public void cleanUpOutdatedBans()
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DATE, -7);

        try
        {
            PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM lw_bans WHERE bandate <= ?");
            delete.setTimestamp(1, new java.sql.Timestamp(cal.getTimeInMillis()));
            delete.execute();

        }
        catch(SQLException e)
        {
            log.error("Expired ban cleanup failed. SQLException: ", e);
        }

        IPMap.clear();
        usernameMap.clear();

        loadBanLists();

        log.debug("Older entries have been cleaned up from banlists.");

    }

}
