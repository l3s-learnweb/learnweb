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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.Learnweb;

/**
 * Manages, stores and analyzes request data. Implements singleton since it has to be accessed by filter.
 *
 * @author Kate
 */
public class RequestManager {
    private static final Logger log = LogManager.getLogger(RequestManager.class);

    private final Learnweb learnweb;

    //Basic maps/list
    private final Queue<RequestData> requests;
    private final Map<String, Set<String>> logins;

    //Aggregated data info
    private List<AggregatedRequestData> aggregatedRequests;
    private Date aggrRequestsUpdated;

    public RequestManager(Learnweb learnweb) {
        this.learnweb = learnweb;
        logins = new ConcurrentHashMap<>();
        requests = new ConcurrentLinkedQueue<>();
        aggrRequestsUpdated = new Date(0);
        aggregatedRequests = new ArrayList<>();
    }

    /**
     * Adds a given request to the requests list.
     */
    public void recordRequest(String ip, String url) {
        Date time = new Date();
        requests.offer(new RequestData(ip, time, url));
    }

    /**
     * Records successful login into a Map(IP, Set(username)), thus matching every IP to usernames that were logged into from it.
     */
    public void recordLogin(String ip, String username) {
        Set<String> names = logins.computeIfAbsent(ip, k -> new HashSet<>());
        names.add(username);
    }

    /**
     * Gets the aggregated and fresh data on the given IP. Used for warning generation.
     *
     * @return All of the request info on certain IP.
     */
    public List<AggregatedRequestData> getRequestsByIP(String ip) {
        List<AggregatedRequestData> reqDataList = new ArrayList<>();

        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT * FROM lw_requests WHERE IP = ?")) {
            select.setString(1, ip);
            ResultSet rs = select.executeQuery();
            while (rs.next()) {
                reqDataList.add(new AggregatedRequestData(rs.getString("IP"), rs.getInt("requests"), rs.getInt("logins"), rs.getString("usernames"), rs.getTimestamp("time")));
            }
        } catch (SQLException e) {
            log.error("Failed to fetch aggregated results by IP. SQLException: ", e);
        }

        long recentRequests = requests.stream().filter(c -> ip.equals(c.getIp())).count();
        Set<String> log = logins.get(ip);
        int loginCount = 0;
        String usernames = "";

        if (log != null) {
            loginCount = log.size();
            usernames = log.toString();
        }

        reqDataList.add(new AggregatedRequestData(ip, (int) recentRequests, loginCount, usernames, new Date()));

        return reqDataList;
    }

    /**
     * Removes requests that are older than 1 hours from memory.
     */
    public void cleanOldRequests() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.HOUR, -1);

        Date threshold = cal.getTime();

        while (!requests.isEmpty() && requests.peek().getTime().before(threshold)) {
            logins.remove(requests.peek().getIp());
            requests.poll();
        }

    }

    /**
     * Logs the request data for the last hour into the database.
     */
    public void recordRequestsToDB() {
        Map<String, Long> requestsByIP = requests.stream().collect(Collectors.groupingBy(RequestData::getIp, Collectors.counting()));

        try (PreparedStatement insert = learnweb.getConnection().prepareStatement(
            "INSERT INTO lw_requests (IP, requests, logins, usernames, time) VALUES(?, ?, ?, ?, ?);")) {
            java.sql.Timestamp insertionTime = new java.sql.Timestamp(new Date().getTime());

            for (Entry<String, Long> entry : requestsByIP.entrySet()) {
                Set<String> log = logins.get(entry.getKey());

                int loginCount = 0;
                String usernames = "";
                if (log != null) {
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

        } catch (SQLException e) {
            log.error("Recording requests to DB failed. Data: {}. SQLException: ", requestsByIP, e);
        }

    }

    public Queue<RequestData> getRequests() {
        return requests;
    }

    public Map<String, Set<String>> getLogins() {
        return logins;
    }

    public List<AggregatedRequestData> getAggregatedRequests() {
        return aggregatedRequests;
    }

    /**
     * Loads the aggregated requests that happened after the last update.
     */
    public void updateAggregatedRequests() {
        try (PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT * FROM lw_requests WHERE time >= ?")) {
            select.setTimestamp(1, new java.sql.Timestamp(aggrRequestsUpdated.getTime()));
            ResultSet rs = select.executeQuery();
            while (rs.next()) {
                aggregatedRequests.add(new AggregatedRequestData(rs.getString("IP"), rs.getInt("requests"), rs.getInt("logins"), rs.getString("usernames"), rs.getTimestamp("time")));
            }
        } catch (SQLException e) {
            log.error("Failed to load AggregatedRequests. SQLException: ", e);
        }

        aggrRequestsUpdated = new Date();
    }

    /**
     * Clears the requests DB. Dev purposes only.
     */
    public void clearRequestsDB() {
        //noinspection SqlWithoutWhere
        try (PreparedStatement delete = learnweb.getConnection().prepareStatement("DELETE FROM lw_requests")) {
            delete.execute();
            aggregatedRequests = new ArrayList<>();
        } catch (SQLException e) {
            log.error("Failed to load ban lists. SQLException: ", e);
        }
    }

    public Date getAggrRequestsUpdateTime() {
        return aggrRequestsUpdated;
    }
}
