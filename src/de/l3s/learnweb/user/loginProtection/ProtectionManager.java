package de.l3s.learnweb.user.loginProtection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.web.AggregatedRequestData;
import de.l3s.learnweb.web.RequestManager;
import de.l3s.util.email.Mail;

/**
 * A fancier ProtectionManager that, rather than auto ban based on pure attempts, analyzes frequency of access on every nth failed attempt.
 *
 * @author Kate
 *
 */
public class ProtectionManager
{
    private final static Logger log = Logger.getLogger(ProtectionManager.class);
    private final Learnweb learnweb;

    private Map<String, AccessData> accessMap;
    private Set<String> whitelist;
    private Queue<LoginAttemptData> attemptedLogins;
    List<AggregatedRequestData> suspiciousActivityList;

    private final String adminEmail;
    private int suspiciousAlertsCounter = 0;
    private final static int SUSPICIOUS_EMAIL_THRESHOLD = 30;

    private final static int ATTEMPTS_STEP = 50;
    private final static int CAPTCHA_THRESHOLD = 3;
    private final static int MINUTES_ANALYZED = 10;
    private final static int BAN_THRESHOLD = 100;

    public ProtectionManager(Learnweb learnweb)
    {
        this.learnweb = learnweb;
        adminEmail = learnweb.getProperties().getProperty("ADMIN_MAIL");
        accessMap = new ConcurrentHashMap<>();
        attemptedLogins = new ConcurrentLinkedQueue<>();
        suspiciousActivityList = new ArrayList<>();
        whitelist = new HashSet<>();
        loadBanLists();
        loadWhitelist();

    }

    private void loadWhitelist()
    {
        try(InputStream inputStream = getClass().getClassLoader().getResourceAsStream("whitelist.txt"))
        {
            Stream<String> stream = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines();
            stream.forEach(whitelist::add);
        }
        catch(IOException e)
        {
            log.error("Failed to load whitelist. IOException: ", e);
        }

        log.debug("Whitelist loaded. Entries: " + whitelist.size());
    }

    /**
     * Loads ban lists from the database. Should be called by every constructor.
     */
    private void loadBanLists()
    {
        try(PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT * FROM lw_bans"))
        {
            ResultSet rs = select.executeQuery();
            while(rs.next())
            {
                accessMap.put(rs.getString("name"), new AccessData(rs.getString("name"), rs.getInt("attempts"), rs.getTimestamp("bandate"), rs.getTimestamp("bannedon")));
            }

        }
        catch(SQLException e)
        {
            log.error("Failed to load ban lists. SQLException: ", e);
        }

        log.debug("Banlist loaded. Entries: " + accessMap.size());
    }

    public Date getBannedUntil(String name)
    {
        AccessData ad = accessMap.get(name);
        if(ad == null)
        {
            return null;
        }
        return ad.getBanDate();
    }

    public boolean isBanned(String ip)
    {
        Date ipBan = getBannedUntil(ip);
        return ipBan != null && ipBan.after(new Date());
    }

    public boolean needsCaptcha(String name)
    {
        AccessData ad = accessMap.get(name);
        if(ad == null)
        {
            return false;
        }
        return ad.getAttempts() > CAPTCHA_THRESHOLD;
    }

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
     * Checks whether the currently accessing IP either has a high request rate (over 300)
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
            list = attemptedLogins.stream().filter(x -> x.getIP().equals(ad.getName()) && x.getTimestamp().after(threshold.getTime())).collect(Collectors.toList());
        }
        else
        {
            list = attemptedLogins.stream().filter(x -> x.getUsername().equals(ad.getName()) && x.getTimestamp().after(threshold.getTime())).collect(Collectors.toList());
        }

        if(list.size() > BAN_THRESHOLD)
        {
            flagSuspicious(ad, list);
        }

        ad.setAllowedAttempts(ATTEMPTS_STEP);

    }

    /**
     * Adds the suspicious acc data to the suspicious list and sends admin an email every 30 entries
     */
    private void flagSuspicious(AccessData ad, List<LoginAttemptData> list)
    {
        RequestManager rm = learnweb.getRequestManager();
        suspiciousActivityList.addAll(rm.getRequestsByIP(ad.getName()));

        if(suspiciousActivityList.size() > 30)
        {
            sendMail();
        }

        suspiciousAlertsCounter++;
        if(suspiciousActivityList.size() >= SUSPICIOUS_EMAIL_THRESHOLD && suspiciousAlertsCounter >= SUSPICIOUS_EMAIL_THRESHOLD)
        {
            sendMail();
        }
        suspiciousAlertsCounter = 0;

    }

    private void sendMail()
    {
        try
        {
            Mail message = new Mail();
            message.setSubject("[Learnweb] Suspicious activity alert");
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(adminEmail));

            StringBuilder content = new StringBuilder("Multiple accounts have been flagged as suspicious by Learnweb protection system. Please look at them closer at http://learnweb.l3s.uni-hannover.de/lw/admin/banlist.jsf.\n"
                    + "Here are the ten most recent entries in the suspicious list: "
                    + "\n\n");

            content.append("<table border=\"1\"><br>");

            List<AggregatedRequestData> suspiciousTemp;

            if(suspiciousActivityList.size() > 10)
            {
                suspiciousTemp = suspiciousActivityList.subList(0, 10);
            }
            else
            {
                suspiciousTemp = suspiciousActivityList;
            }

            for(AggregatedRequestData ard : suspiciousTemp)
            {
                content.append("<tr>");

                content.append("<td>");
                content.append(ard.getIP());
                content.append("</td>");

                content.append("<td>");
                content.append(ard.getRequests());
                content.append("</td>");

                content.append("<td>");
                content.append(ard.getTime());
                content.append("</td>");

                content.append("</tr>");

            }

            content.append("</table><br>");

            content.append("Total entries requiring your attention: ").append(suspiciousActivityList.size());

            message.setHTML(content.toString());
            message.sendMail();
        }
        catch(MessagingException e)
        {
            log.error("Failed to send admin alert mail. Error: ", e);
        }
    }

    public void updateSuccessfulAttempts(String IP, String username)
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

    public void unban(String name)
    {
        accessMap.remove(name);

        try(PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM lw_bans WHERE name=?"))
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

    public void clearBans()
    {
        accessMap.clear();

        //noinspection SqlWithoutWhere
        try(PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM lw_bans"))
        {
            delete.execute();
        }
        catch(SQLException e)
        {
            log.error("Ban clearing failed. SQLException: ", e);
        }

        log.debug("Banlist cleared.");

    }

    public void cleanUpOutdatedBans()
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DATE, -7);

        try(PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM lw_bans WHERE bandate <= ?"))
        {
            delete.setTimestamp(1, new java.sql.Timestamp(cal.getTimeInMillis()));
            delete.execute();
        }
        catch(SQLException e)
        {
            log.error("Expired ban cleanup failed. SQLException: ", e);
        }

        accessMap.clear();
        loadBanLists();

        log.debug("Older entries have been cleaned up from ban lists.");

    }

    public List<AccessData> getBanlist()
    {
        return new ArrayList<>(accessMap.values());
    }

    public void ban(String name, int banDays, int banHours, int banMinutes, boolean isIP)
    {
        AccessData accData = accessMap.computeIfAbsent(name, AccessData::new);

        accData.setBan(banDays, banHours, banMinutes);

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

        }
        catch(SQLException e)
        {
            log.error("Ban attempt failed. SQLException: ", e);
        }

    }

    public void removeSuspicious(String name)
    {
        suspiciousActivityList.removeIf(s -> name.equals(s.getIP()));
    }

    public List<AggregatedRequestData> getSuspiciousActivityList()
    {
        return suspiciousActivityList;
    }

    public void permaban(String name, boolean isIP)
    {
        AccessData accData = accessMap.computeIfAbsent(name, AccessData::new);

        accData.permaban();

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

            log.debug("Banned " + accData.getName() + " permanently. They have been very naughty.");

        }
        catch(SQLException e)
        {
            log.error("Ban attempt failed. SQLException: ", e);
        }

    }

}
