package de.l3s.learnweb.loginprotection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.loginprotection.entity.AccessData;
import de.l3s.learnweb.loginprotection.entity.LoginAttemptData;

/**
 * A fancier ProtectionManager that, rather than autoban based on pure attempts, analyzes frequency of access on every nth failed attempt.
 *
 * @author Kate
 *
 */
public class FrequencyProtectionManager implements ProtectionManager
{
    private final static Logger log = Logger.getLogger(FrequencyProtectionManager.class);
    private final Learnweb learnweb;

    private Map<String, AccessData> accessMap;
    private Set<String> whitelist;
    private Queue<LoginAttemptData> attemptedLogins;

    private final static int ATTEMPTS_STEP = 50;
    private final static int CAPTCHA_THRESHOLD = 3;
    private final static int MINUTES_ANALYZED = 10;
    private final static int BAN_THRESHOLD = 100;
    private final static int BAN_LENGTH = 5;

    public FrequencyProtectionManager(Learnweb learnweb)
    {
        this.learnweb = learnweb;
        accessMap = new ConcurrentHashMap<String, AccessData>();
        attemptedLogins = new ConcurrentLinkedQueue<LoginAttemptData>();
        whitelist = new HashSet<String>();
        loadBanLists();
        loadWhitelist();
    }

    private void loadWhitelist()
    {
        //TODO: This is a placeholder code, will be fixed\adjusted later
        try(Stream<String> stream = Files.lines(Paths.get("whitelist.txt")))
        {
            stream.forEach(whitelist::add);
        }
        catch(IOException e)
        {
            log.error("Failed to load whitelist. SQLException: ", e);
        }

        log.debug("Whitelist loaded. Entries: " + whitelist.size());
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
                accessMap.put(rs.getString("name"), new AccessData(rs.getString("name"), rs.getInt("attempts"), rs.getTimestamp("bandate"), rs.getDate("bannedon")));
            }

        }
        catch(SQLException e)
        {
            log.error("Failed to load banlists. SQLException: ", e);
        }

        log.debug("Banlist loaded. Entries: " + accessMap.size());
    }

    @Override
    public Date getBannedUntil(String name)
    {
        AccessData ad = accessMap.get(name);
        if(ad == null)
        {
            return null;
        }
        return ad.getBanDate();
    }

    @Override
    public boolean needsCaptcha(String name)
    {
        AccessData ad = accessMap.get(name);
        if(ad == null)
        {
            return false;
        }
        return ad.getAttempts() > CAPTCHA_THRESHOLD;
    }

    @Override
    public void updateFailedAttempts(String IP, String username)
    {
        attemptedLogins.add(new LoginAttemptData(IP, username, false, new Date()));

        AccessData ipData = accessMap.get(IP);
        AccessData usernameData = accessMap.get(username);

        if(ipData == null)
        {
            ipData = new AccessData(IP);
            accessMap.put(IP, ipData);
        }

        if(usernameData == null)
        {
            usernameData = new AccessData(username);
            accessMap.put(username, usernameData);
        }

        ipData.logAttempt();
        usernameData.logAttempt();

        if(ipData.getAllowedAttempts() < 0)
        {
            analyzeAccess(ipData, true);
        }
        else if(usernameData.getAllowedAttempts() < 0)
        {
            analyzeAccess(usernameData, false);
        }

    }

    /**
     * Checks whether the currenttly accessing IP either has a high request rate (over
     */
    private void analyzeAccess(AccessData ad, boolean isIP)
    {
        if(isIP && whitelist.contains(ad.getName()) && ad.getAttempts() < 300)
        {
            ad.setAllowedAttempts(ATTEMPTS_STEP);
            return;
        }

        Calendar threshold = Calendar.getInstance();
        threshold.setTime(new Date());
        threshold.add(Calendar.MINUTE, -MINUTES_ANALYZED);

        List<LoginAttemptData> list;

        if(isIP)
        {
            list = attemptedLogins.stream().filter(x -> x.getIP() == ad.getName() && x.getTimestamp().after(threshold.getTime())).collect(Collectors.toList());

        }
        else
        {
            list = attemptedLogins.stream().filter(x -> x.getUsername() == ad.getName() && x.getTimestamp().after(threshold.getTime())).collect(Collectors.toList());
        }

        if(list.size() > BAN_THRESHOLD)
        {
            int banMultiplier = ad.getAttempts() / ATTEMPTS_STEP;
            this.ban(ad, banMultiplier * BAN_LENGTH, isIP);
        }

        ad.setAllowedAttempts(ATTEMPTS_STEP);

    }

    @Override
    public void updateSuccessfuldAttempts(String IP, String username)
    {
        AccessData ipData = accessMap.get(IP);
        AccessData usernameData = accessMap.get(username);

        if(ipData != null)
        {
            ipData.reset();
        }

        if(usernameData != null)
        {
            usernameData.reset();
        }

        attemptedLogins.add(new LoginAttemptData(IP, username, true, new Date()));
    }

    @Override
    public void unban(String name)
    {
        accessMap.remove(name);

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

    @Override
    public void clearBans()
    {
        accessMap.clear();

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

        accData.setBannedOn(new Date());

        try(PreparedStatement insert = learnweb.getConnection().prepareStatement("INSERT DELAYED INTO lw_bans (name, bandate, bannedon, attempts, type) VALUES(?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE bandate=VALUES(bandate)"))
        {
            insert.setString(1, accData.getName());
            insert.setTimestamp(2, new java.sql.Timestamp(accData.getBanDate().getTime()));
            insert.setTimestamp(3, new java.sql.Timestamp(accData.getBannedOn().getTime()));
            insert.setInt(4, accData.getAttempts());

            if(isIP)
            {
                insert.setString(5, "IP");
            }
            else
            {
                insert.setString(5, "user");
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
        accData = accessMap.get(name);
        if(accData == null)
        {
            accData = new AccessData(name);
            accessMap.put(name, accData);
        }

        ban(accData, bantime, isIP);

    }

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

        accessMap.clear();
        loadBanLists();

        log.debug("Older entries have been cleaned up from banlists.");

    }

    @Override
    public List<AccessData> getBanlist()
    {
        List<AccessData> l = new ArrayList<AccessData>();
        l.addAll(accessMap.values());
        return l;
    }

}
