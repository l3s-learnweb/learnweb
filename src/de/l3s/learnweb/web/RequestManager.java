package de.l3s.learnweb.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.util.email.Mail;

/**
 * Manages, stores and analyzes request data. Implements singleton since it has to be accessed by filter.
 * Protects login form by analysing frequency of access on every nth failed attempt.
 *
 * @author Kate
 */
@ApplicationScoped
public class RequestManager implements Serializable {
    private static final long serialVersionUID = 8152764483449652749L;
    private static final Logger log = LogManager.getLogger(RequestManager.class);

    private static final int SUSPICIOUS_EMAIL_THRESHOLD = 30;
    private static final int ATTEMPTS_STEP = 50;
    private static final int CAPTCHA_THRESHOLD = 3;
    private static final int MINUTES_ANALYZED = 10;
    private static final int BAN_THRESHOLD = 100;

    private String adminEmail;
    private int suspiciousAlertsCounter = 0;

    private final Set<String> whitelist = new HashSet<>();
    private final Map<String, Ban> banlist = new ConcurrentHashMap<>();

    // Basic maps/list
    private final Queue<Request> requests = new ConcurrentLinkedQueue<>();
    private final List<Request> suspiciousRequests = new ArrayList<>();
    private final Map<String, Set<String>> logins = new ConcurrentHashMap<>();
    private final Queue<LoginAttempt> attemptedLogins = new ConcurrentLinkedQueue<>();

    // Aggregated data info
    private List<Request> aggregatedRequests = new ArrayList<>();
    private LocalDateTime aggrRequestsUpdated = LocalDateTime.MIN;

    @Inject
    private BanDao banDao;

    @Inject
    private RequestDao requestDao;

    @PostConstruct
    public void init() {
        adminEmail = Learnweb.config().getProperty("admin_mail");

        loadBanLists();
        loadWhitelist();
    }

    private void loadWhitelist() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("whitelist.txt")) {
            if (inputStream != null) {
                try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    try (Stream<String> stream = bufferedReader.lines()) {
                        stream.forEach(whitelist::add);
                    }
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
        banDao.findAll().forEach(ban -> banlist.put(ban.getAddr(), ban));

        log.debug("Banlist loaded. Entries: {}", banlist.size());
    }

    /**
     * Adds a given request to the requests list.
     */
    public void recordRequest(String ip, String url) {
        requests.offer(new Request(ip, url));
    }

    /**
     * Records successful login into a Map(IP, Set(username)), thus matching every IP to usernames that were logged into from it.
     */
    public void recordLogin(String ip, String username) {
        Set<String> names = logins.computeIfAbsent(ip, key -> new HashSet<>());
        names.add(username);
    }

    /**
     * Gets the aggregated and fresh data on the given IP. Used for warning generation.
     *
     * @return All of the request info on certain IP.
     */
    public List<Request> getRequestsByIp(String ip) {
        List<Request> requests = requestDao.findByIp(ip);

        long recentRequests = this.requests.stream().filter(r -> ip.equals(r.getIp())).count();
        Set<String> logins = this.logins.get(ip);

        if (logins != null) {
            for (Request request : requests) {
                request.setRequests((int) recentRequests);
                request.setLoginCount(logins.size());
                request.setUsernames(logins.toString());
            }
        }

        return requests;
    }

    /**
     * Removes requests that are older than 1 hours from memory.
     */
    public void cleanOldRequests() {
        LocalDateTime hourAgo = LocalDateTime.now().minusHours(1);

        while (!requests.isEmpty() && requests.peek().getTime().isBefore(hourAgo)) {
            logins.remove(requests.peek().getIp());
            requests.poll();
        }
    }

    /**
     * Logs the request data for the last hour into the database.
     */
    public void flushRequests() {
        List<Request> requestsToSave = new ArrayList<>();

        Map<String, Long> requestsByIp = requests.stream().collect(Collectors.groupingBy(Request::getIp, Collectors.counting()));
        for (Map.Entry<String, Long> entry : requestsByIp.entrySet()) {
            Request request = new Request(entry.getKey(), null);
            request.setRequests(entry.getValue().intValue());

            Set<String> loginsOfIp = logins.get(entry.getKey());
            if (loginsOfIp != null) {
                request.setLoginCount(loginsOfIp.size());
                request.setUsernames(loginsOfIp.toString());
            }

            requestsToSave.add(request);
        }

        if (!requestsToSave.isEmpty()) {
            requestDao.save(requestsToSave);
        }
    }

    public Queue<Request> getRequests() {
        return requests;
    }

    public Map<String, Set<String>> getLogins() {
        return logins;
    }

    public List<Request> getAggregatedRequests() {
        return aggregatedRequests;
    }

    public List<Request> getSuspiciousRequests() {
        return suspiciousRequests;
    }

    /**
     * Loads the aggregated requests that happened after the last update.
     */
    public void updateAggregatedRequests() {
        aggregatedRequests.addAll(requestDao.findAfterDate(aggrRequestsUpdated));
        aggrRequestsUpdated = LocalDateTime.now();
    }

    /**
     * Clears the requests DB. Dev purposes only.
     */
    public void clearRequests() {
        requestDao.deleteAll();
        aggregatedRequests = new ArrayList<>();
    }

    public LocalDateTime getAggrRequestsUpdated() {
        return aggrRequestsUpdated;
    }

    public boolean isBanned(String addr) {
        Ban ban = banlist.get(addr);
        return ban != null && ban.getExpires() != null && ban.getExpires().isAfter(LocalDateTime.now());
    }

    public boolean isCaptchaRequired(String addr) {
        Ban ban = banlist.get(addr);
        if (ban == null) {
            return false;
        }
        return ban.getAttempts() > CAPTCHA_THRESHOLD;
    }

    public void updateFailedAttempts(String ip, String username) {
        attemptedLogins.add(new LoginAttempt(ip, username, false));

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
        if (isIP && whitelist.contains(ban.getAddr()) && ban.getAttempts() < 300) {
            ban.setAllowedAttempts(ATTEMPTS_STEP);
            return;
        }

        LocalDateTime threshold = LocalDateTime.now().minusMinutes(MINUTES_ANALYZED);

        List<LoginAttempt> list;
        if (isIP) {
            list = attemptedLogins.stream()
                .filter(x -> x.getIp().equals(ban.getAddr()) && x.getDateTime().isAfter(threshold))
                .collect(Collectors.toList());
        } else {
            list = attemptedLogins.stream()
                .filter(x -> x.getUsername().equals(ban.getAddr()) && x.getDateTime().isAfter(threshold))
                .collect(Collectors.toList());
        }

        if (list.size() > BAN_THRESHOLD) {
            flagSuspicious(ban);
        }

        ban.setAllowedAttempts(ATTEMPTS_STEP);
    }

    /**
     * Adds the suspicious acc data to the suspicious list and sends admin an email every 30 entries.
     */
    private void flagSuspicious(Ban ban) {
        suspiciousRequests.addAll(getRequestsByIp(ban.getAddr()));

        if (suspiciousRequests.size() > 30) {
            sendMail();
        }

        suspiciousAlertsCounter++;
        if (suspiciousRequests.size() >= SUSPICIOUS_EMAIL_THRESHOLD && suspiciousAlertsCounter >= SUSPICIOUS_EMAIL_THRESHOLD) {
            sendMail();
            suspiciousAlertsCounter = 0;
        }
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

            List<Request> suspiciousTemp;

            if (suspiciousRequests.size() > 10) {
                suspiciousTemp = suspiciousRequests.subList(0, 10);
            } else {
                suspiciousTemp = suspiciousRequests;
            }

            for (Request ard : suspiciousTemp) {
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

            content.append("Total entries requiring your attention: ").append(suspiciousRequests.size());

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
            ipData.resetAttempts();
        }

        if (usernameData != null) {
            usernameData.resetAttempts();
        }

        attemptedLogins.add(new LoginAttempt(ip, username, true));
    }

    public void clearBan(String addr) {
        banDao.delete(addr);

        banlist.remove(addr);
        log.debug("Unbanned {}", addr);
    }

    public void clearOutdatedBans() {
        banDao.deleteOutdated();

        banlist.clear();
        loadBanLists();

        log.debug("Older entries have been cleaned up from ban lists.");
    }

    public List<Ban> getBanlist() {
        return new ArrayList<>(banlist.values());
    }

    /**
     * A default ban, to ban the given IP address for half a year.
     */
    public void ban(String addr, String reason) {
        ban(addr, reason, Duration.ofDays(183));
    }

    public void ban(String addr, String reason, Duration amount) {
        Ban ban = banlist.computeIfAbsent(addr, Ban::new);
        ban.setReason(reason);
        ban.setExpires(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).plus(amount));

        banDao.save(ban);
        log.info("Banned {} until {}", ban.getAddr(), ban.getExpires());
    }
}
