package de.l3s.learnweb.user.loginProtection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.web.AggregatedRequestData;
import de.l3s.learnweb.web.RequestManager;
import de.l3s.util.email.Mail;

/**
 * A fancier ProtectionManager that, rather than auto ban based on pure attempts, analyzes frequency of access on every nth failed attempt.
 *
 * @author Kate
 */
public class ProtectionManager {
    private static final Logger log = LogManager.getLogger(ProtectionManager.class);

    private static final int SUSPICIOUS_EMAIL_THRESHOLD = 30;
    private static final int ATTEMPTS_STEP = 50;
    private static final int CAPTCHA_THRESHOLD = 3;
    private static final int MINUTES_ANALYZED = 10;
    private static final int BAN_THRESHOLD = 100;

    private final Learnweb learnweb;

    private final Map<String, Ban> banlist;
    private final Set<String> whitelist;
    private final Queue<LoginAttemptData> attemptedLogins;
    private final List<AggregatedRequestData> suspiciousActivityList;
    private final BanDao banDao;

    private final String adminEmail;
    private int suspiciousAlertsCounter = 0;

    public ProtectionManager(Learnweb learnweb) {
        this.learnweb = learnweb;
        adminEmail = learnweb.getProperties().getProperty("ADMIN_MAIL");
        banlist = new ConcurrentHashMap<>();
        attemptedLogins = new ConcurrentLinkedQueue<>();
        suspiciousActivityList = new ArrayList<>();
        whitelist = new HashSet<>();
        loadBanLists();
        loadWhitelist();

        banDao = learnweb.getJdbi().onDemand(BanDao.class);
    }

    private void loadWhitelist() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("whitelist.txt")) {
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                try (Stream<String> stream = bufferedReader.lines()) {
                    stream.forEach(whitelist::add);
                }
            }
        } catch (IOException e) {
            log.error("Failed to load whitelist. IOException: ", e);
        }

        log.debug("Whitelist loaded. Entries: {}", whitelist.size());
    }

    /**
     * Loads ban lists from the database. Should be called by every constructor.
     */
    private void loadBanLists() {
        banDao.getBans().forEach(ban -> banlist.put(ban.getName(), ban));

        log.debug("Banlist loaded. Entries: {}", banlist.size());
    }

    public LocalDateTime getBannedUntil(String name) {
        Ban ban = banlist.get(name);
        if (ban == null) {
            return null;
        }
        return ban.getBannedUntil();
    }

    public boolean isBanned(String name) {
        LocalDateTime ipBan = getBannedUntil(name);
        return ipBan != null && ipBan.isAfter(LocalDateTime.now());
    }

    public boolean needsCaptcha(String name) {
        Ban ban = banlist.get(name);
        if (ban == null) {
            return false;
        }
        return ban.getAttempts() > CAPTCHA_THRESHOLD;
    }

    public void updateFailedAttempts(String ip, String username) {
        attemptedLogins.add(new LoginAttemptData(ip, username, false));

        Ban ipData = banlist.get(ip);
        Ban usernameData = banlist.get(username);

        if (ipData == null) {
            ipData = new Ban(ip);
            banlist.put(ip, ipData);
        }

        if (usernameData == null) {
            usernameData = new Ban(username);
            banlist.put(username, usernameData);
        }

        ipData.logAttempt();
        usernameData.logAttempt();

        if (ipData.getAllowedAttempts() < 0) {
            analyzeAccess(ipData, true);
        } else if (usernameData.getAllowedAttempts() < 0) {
            analyzeAccess(usernameData, false);
        }

    }

    /**
     * Checks whether the currently accessing IP either has a high request rate (over 300).
     */
    private void analyzeAccess(Ban ban, boolean isIP) {
        if (isIP && whitelist.contains(ban.getName()) && ban.getAttempts() < 300) {
            ban.setAllowedAttempts(ATTEMPTS_STEP);
            return;
        }

        LocalDateTime threshold = LocalDateTime.now().minusMinutes(MINUTES_ANALYZED);

        List<LoginAttemptData> list;
        if (isIP) {
            list = attemptedLogins.stream()
                .filter(x -> x.getIp().equals(ban.getName()) && x.getDateTime().isAfter(threshold))
                .collect(Collectors.toList());
        } else {
            list = attemptedLogins.stream()
                .filter(x -> x.getUsername().equals(ban.getName()) && x.getDateTime().isAfter(threshold))
                .collect(Collectors.toList());
        }

        if (list.size() > BAN_THRESHOLD) {
            flagSuspicious(ban, list);
        }

        ban.setAllowedAttempts(ATTEMPTS_STEP);
    }

    /**
     * Adds the suspicious acc data to the suspicious list and sends admin an email every 30 entries.
     */
    private void flagSuspicious(Ban ban, List<LoginAttemptData> list) {
        RequestManager rm = learnweb.getRequestManager();
        suspiciousActivityList.addAll(rm.getRequestsByIP(ban.getName()));

        if (suspiciousActivityList.size() > 30) {
            sendMail();
        }

        suspiciousAlertsCounter++;
        if (suspiciousActivityList.size() >= SUSPICIOUS_EMAIL_THRESHOLD && suspiciousAlertsCounter >= SUSPICIOUS_EMAIL_THRESHOLD) {
            sendMail();
        }
        suspiciousAlertsCounter = 0;

    }

    private void sendMail() {
        try {
            Mail message = new Mail();
            message.setSubject("[Learnweb] Suspicious activity alert");
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(adminEmail));

            StringBuilder content = new StringBuilder("Multiple accounts have been flagged as suspicious by Learnweb protection system. Please look at them closer at https://learnweb.l3s.uni-hannover.de/lw/admin/banlist.jsf.\n"
                + "Here are the ten most recent entries in the suspicious list: "
                + "\n\n");

            content.append("<table border=\"1\"><br>");

            List<AggregatedRequestData> suspiciousTemp;

            if (suspiciousActivityList.size() > 10) {
                suspiciousTemp = suspiciousActivityList.subList(0, 10);
            } else {
                suspiciousTemp = suspiciousActivityList;
            }

            for (AggregatedRequestData ard : suspiciousTemp) {
                content.append("<tr>");

                content.append("<td>");
                content.append(ard.getIp());
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
        } catch (MessagingException e) {
            log.error("Failed to send admin alert mail. Error: ", e);
        }
    }

    public void updateSuccessfulAttempts(String ip, String username) {
        Ban ipData = banlist.get(ip);
        Ban usernameData = banlist.get(username);

        if (ipData != null) {
            ipData.reset();
        }

        if (usernameData != null) {
            usernameData.reset();
        }

        attemptedLogins.add(new LoginAttemptData(ip, username, true));
    }

    public void clearBan(String name) {
        banDao.deleteBanByName(name);

        banlist.remove(name);
        log.debug("Unbanned {}", name);
    }

    public void clearOutdatedBans() {
        banDao.deleteOutdatedBans();

        banlist.clear();
        loadBanLists();

        log.debug("Older entries have been cleaned up from ban lists.");
    }

    public List<Ban> getBanlist() {
        return new ArrayList<>(banlist.values());
    }

    /**
     * Same as {@link #ban(String, String)}, but do not save the name to database.
     */
    public void tempBan(String ipAddr, String reason) {
        Ban ban = banlist.computeIfAbsent(ipAddr, Ban::new);

        ban.setType("temp");
        ban.ban(365, 0, 0);
        ban.setReason(reason);
    }

    /**
     * A default ban, to ban the given IP address for half a year.
     */
    public void ban(String ipAddr, String reason) {
        ban(ipAddr, 182, 0, 0, true, reason);
    }

    public void ban(String name, int banDays, int banHours, int banMinutes, boolean isIP, String reason) {
        Ban ban = banlist.computeIfAbsent(name, Ban::new);

        ban.setType(isIP ? "IP" : "user");
        ban.ban(banDays, banHours, banMinutes);
        ban.setReason(reason);

        banDao.save(ban);
        log.info("Banned {} until {}", ban.getName(), ban.getBannedUntil());
    }

    public void removeSuspicious(String name) {
        suspiciousActivityList.removeIf(requestData -> name.equals(requestData.getIp()));
    }

    public List<AggregatedRequestData> getSuspiciousActivityList() {
        return suspiciousActivityList;
    }
}
