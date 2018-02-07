package de.l3s.learnweb.loginprotection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

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
        usernameMap = new HashMap<String, AccessData>();
        IPMap = new HashMap<String, AccessData>();
        loadBanLists();
    }

    /**
     * Loads banlists from the database. Should be called by every constructor.
     */
    private void loadBanLists()
    {
        try
        {
            PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT * FROM lw_bans");

            ResultSet rs = select.executeQuery();
            while(rs.next())
            {
                if(rs.getString("type").equals("user"))
                {
                    usernameMap.put(rs.getString("name"), new AccessData(1, rs.getDate("bandate")));
                }
                else if(rs.getString("type").equals("IP"))
                {
                    IPMap.put(rs.getString("name"), new AccessData(1, rs.getDate("bandate")));
                }
            }

        }
        catch(SQLException e)
        {
            log.error("Failed to load banlists. SQLException: ", e);
        }

        log.debug("Loaded banlists of usernames and IPs with " + usernameMap.size() + " and " + IPMap.size() + " entries respectively.");
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
            ipData = new AccessData();
            IPMap.put(IP, ipData);
        }

        if(usernameData == null)
        {
            usernameData = new AccessData();
            usernameMap.put(username, usernameData);
            return;
        }

        ipData.attempts++;
        usernameData.attempts++;

        if(ipData.attempts >= 50)
        {
            ipData.permaban();
            log.debug("Permabanned IP " + IP + " for excessive failed login attempts");
        }
        else if(ipData.attempts >= 10)
        {
            int bantime = 2 + 2 * ipData.attempts % 10;
            ban(ipData, IP, bantime, true);
            log.debug("Banned IP " + IP + " for " + bantime + " hours after " + ipData.attempts + " failed login attempts");

        }

        if(usernameData.attempts >= 50)
        {
            usernameData.permaban();
            log.debug("Permabanned username " + username + " for excessive failed login attempts");
        }
        else if(usernameData.attempts >= 10)
        {
            int bantime = 2 + 2 * usernameData.attempts % 10;
            ban(usernameData, username, bantime, false);
            log.debug("Banned username " + username + " for " + bantime + " hours after " + usernameData.attempts + " failed login attempts");
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
    public void ban(AccessData accData, String name, int bantime, boolean isIP)
    {
        if(bantime < 0)
        {
            accData.permaban();
        }
        else
        {
            accData.setBan(bantime);
        }

        try
        {
            PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT INTO lw_bans (name, bandate, type) VALUES(?, ? ,?) ON DUPLICATE KEY UPDATE bandate=VALUES(bandate)");
            insert.setString(1, name);
            insert.setDate(2, new java.sql.Date(accData.banDate.getTime()));
            if(isIP)
            {
                insert.setString(3, "IP");
            }
            else
            {
                insert.setString(3, "user");
            }

            insert.execute();
        }
        catch(SQLException e)
        {
            log.error("Ban attempt failed. SQLException: ", e);
        }
    }

    /**
     * Unbans a given name or IP. To be used by moderators.
     *
     * @param name Name\Address that will be cleared of their sins
     */
    @Override
    public void unban(String name)
    {
        IPMap.remove(name);
        usernameMap.remove(name);

        try
        {
            PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM lw_bans WHERE name=?");
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

        try
        {
            PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM lw_bans");
            delete.execute();

        }
        catch(SQLException e)
        {
            log.error("Ban clearing failed. SQLException: ", e);
        }

        log.debug("Banlist cleared.");
    }

}
