package de.l3s.learnweb.beans.publicPages;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.util.database.Sql;

@Named
@RequestScoped
public class StatisticsBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = 8540469716342151138L;

    private final List<SimpleEntry<LocalDate, Integer>> activeUsersPerMonth;
    private final List<SimpleEntry<String, Number>> resourcesPerSource;
    private final Map<String, Number> generalStatistics = new LinkedHashMap<>();

    public StatisticsBean() throws SQLException {
        Long users = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_user WHERE deleted = 0");
        Long groups = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_group WHERE deleted = 0");
        Long resources = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_resource WHERE deleted = 0");
        Long courses = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_course");

        Long ratedResourcesCount = (Long) Sql.getSingleResult("SELECT (SELECT count(DISTINCT resource_id) FROM `lw_resource_rating`) + (SELECT count(DISTINCT resource_id) FROM `lw_thumb`)");
        Long taggedResourcesCount = (Long) Sql.getSingleResult("SELECT count(DISTINCT resource_id) FROM lw_resource_tag");
        Long commentedResourcesCount = (Long) Sql.getSingleResult("SELECT count(DISTINCT resource_id) FROM lw_comment");

        Long rateCount = (Long) Sql.getSingleResult("SELECT (SELECT count(*) FROM `lw_resource_rating`) + (SELECT count(*) FROM `lw_thumb`)");
        Long tagCount = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_resource_tag");
        Long commentCount = (Long) Sql.getSingleResult("SELECT count(*) FROM lw_comment");

        Double ratedResourcesAverage = (double) rateCount / (double) ratedResourcesCount;
        Double taggedResourcesAverage = (double) tagCount / (double) taggedResourcesCount;
        Double commentedResourcesAverage = (double) commentCount / (double) commentedResourcesCount;

        generalStatistics.put("users", users);
        generalStatistics.put("groupsTitle", groups);
        generalStatistics.put("courses", courses);
        generalStatistics.put("resources", resources);
        generalStatistics.put("number_of_rated_resources", ratedResourcesCount);
        generalStatistics.put("number_of_tagged_resources", taggedResourcesCount);
        generalStatistics.put("number_of_commented_resources", commentedResourcesCount);
        generalStatistics.put("number_of_rates", rateCount);
        generalStatistics.put("number_of_tags", tagCount);
        generalStatistics.put("number_of_comments", commentCount);
        generalStatistics.put("average_number_of_rates_per_rated_resource", ratedResourcesAverage);
        generalStatistics.put("average_number_of_tags_per_tagged_resource", taggedResourcesAverage);
        generalStatistics.put("average_number_of_comments_per_commented_resource", commentedResourcesAverage);

        //averageSessionTime = (BigDecimal) Sql.getSingleResult("SELECT avg(diff) / 60 FROM (SELECT count(*) as t, UNIX_TIMESTAMP(max(timestamp)) -  UNIX_TIMESTAMP(min(timestamp)) AS diff FROM `lw_user_log` GROUP BY session_id) AS DE");
        activeUsersPerMonth = calcActiveUsersPerMonth();

        HashSet<String> highlightedEntries = new HashSet<>();
        highlightedEntries.add("Archive-It");
        highlightedEntries.add("Yovisto");
        highlightedEntries.add("LORO");
        highlightedEntries.add("TEDx");
        highlightedEntries.add("TED");
        highlightedEntries.add("TED-Ed");
        highlightedEntries.add("FactCheck");
        highlightedEntries.add("speechrepository");

        resourcesPerSource = getEntriesForQuery("SELECT source, count(*) FROM lw_resource WHERE deleted = 0 GROUP BY source ORDER BY count( * ) DESC", highlightedEntries);
    }

    private List<SimpleEntry<String, Number>> getEntriesForQuery(String query, HashSet<String> highlightedEntries) throws SQLException {
        LinkedList<SimpleEntry<String, Number>> results = new LinkedList<>();
        ResultSet rs = getLearnweb().getConnection().createStatement().executeQuery(query);
        while (rs.next()) {
            String key = rs.getString(1);
            if (highlightedEntries != null && highlightedEntries.contains(key)) {
                key += " *";
            }

            SimpleEntry<String, Number> row = new AbstractMap.SimpleEntry<>(key, rs.getInt(2));
            results.add(row);
        }
        return results;
    }

    private LinkedList<SimpleEntry<LocalDate, Integer>> calcActiveUsersPerMonth() throws SQLException {
        LinkedList<SimpleEntry<LocalDate, Integer>> results = new LinkedList<>();
        ResultSet rs = getLearnweb().getConnection().createStatement().executeQuery(
            "SELECT timestamp, count(distinct user_id) as active_users FROM `lw_user_log` WHERE `action` = 9 and timestamp > DATE_SUB(NOW(), INTERVAL 390 day) group by year(timestamp) ,month(timestamp) ORDER BY  year(timestamp) DESC,month(timestamp) DESC LIMIT 13");
        while (rs.next()) {
            LocalDate date = rs.getObject(1, LocalDate.class);

            SimpleEntry<LocalDate, Integer> row = new AbstractMap.SimpleEntry<>(date, rs.getInt(2));
            results.add(row);
        }
        return results;
    }

    public List<SimpleEntry<LocalDate, Integer>> getActiveUsersPerMonth() {
        return activeUsersPerMonth;
    }

    public List<SimpleEntry<String, Number>> getResourcesPerSource() {
        return resourcesPerSource;
    }

    public Map<String, Number> getGeneralStatistics() {
        return generalStatistics;
    }
}
