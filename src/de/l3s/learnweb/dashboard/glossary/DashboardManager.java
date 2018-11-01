package de.l3s.learnweb.dashboard.glossary;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.dashboard.glossary.GlossaryDashboardChartsFactory.*;
import de.l3s.util.StringHelper;

/**
 * Glossary is a resource, stored in lw_resource table.
 * Concepts is a "glossaries", stored in lw_resource_glossary(_copy) table.
 * TODO: update queries using the knowledge :'(
 */
public class DashboardManager
{
    private static final Logger log = Logger.getLogger(DashboardManager.class);
    private static DashboardManager instance;

    private final Learnweb learnweb;

    public static DashboardManager getInstance()
    {
        if(instance == null)
            instance = new DashboardManager();
        return instance;
    }

    private DashboardManager()
    {
        super();

        this.learnweb = Learnweb.getInstance();
    }

    public Integer getTotalConcepts(Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException
    {
        int result = 0;

        try(PreparedStatement select = learnweb.getConnection().prepareStatement(
                "SELECT COUNT(distinct rg.glossary_id) as count "
                        + "FROM lw_resource r "
                        + "JOIN lw_resource_glossary rg USING(resource_id) "
                        + "WHERE rg.deleted != 1 AND r.deleted != 1 "
                        + "AND r.owner_user_id IN(" + StringHelper.implodeInt(userIds, ",") + ") "
                        + "AND rg.timestamp BETWEEN ? AND ?"))
        {
            select.setTimestamp(1, new Timestamp(startDate.getTime()));
            select.setTimestamp(2, new Timestamp(endDate.getTime()));
            ResultSet rs = select.executeQuery();

            while(rs.next())
                result = rs.getInt("count");
        }

        return result;
    }

    public Integer getTotalTerms(Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException
    {
        int result = 0;

        try(PreparedStatement select = learnweb.getConnection().prepareStatement(
                "SELECT COUNT(*) as count "
                        + "FROM lw_resource r "
                        + "JOIN lw_resource_glossary rg USING(resource_id) "
                        + "JOIN lw_resource_glossary_terms rgt USING(glossary_id) "
                        + "WHERE rg.deleted != 1 AND r.deleted != 1 AND rgt.deleted != 1 "
                        + "AND r.owner_user_id IN(" + StringHelper.implodeInt(userIds, ",") + ") "
                        + "AND rg.timestamp BETWEEN ? AND ?"))
        {
            select.setTimestamp(1, new Timestamp(startDate.getTime()));
            select.setTimestamp(2, new Timestamp(endDate.getTime()));
            ResultSet rs = select.executeQuery();

            while(rs.next())
                result = rs.getInt("count");
        }

        return result;
    }

    public Integer getTotalSources(Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException
    {
        int result = 0;

        try(PreparedStatement select = learnweb.getConnection().prepareStatement(
                "SELECT COUNT(distinct rgt.references) as count "
                        + "FROM lw_resource r "
                        + "JOIN lw_resource_glossary rg USING(resource_id) "
                        + "JOIN lw_resource_glossary_terms rgt USING(glossary_id) "
                        + "WHERE rg.deleted != 1 AND r.deleted != 1 AND rgt.deleted != 1 "
                        + "AND r.owner_user_id IN(" + StringHelper.implodeInt(userIds, ",") + ") "
                        + "AND rg.timestamp BETWEEN ? AND ?"))
        {
            select.setTimestamp(1, new Timestamp(startDate.getTime()));
            select.setTimestamp(2, new Timestamp(endDate.getTime()));
            ResultSet rs = select.executeQuery();

            while(rs.next())
                result = rs.getInt("count");
        }

        return result;
    }

    public ArrayList<GlossaryFieldSummery> getGlossaryFieldSummeryPerUser(Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException
    {
        ArrayList<GlossaryFieldSummery> summeries = new ArrayList<>(userIds.size());

        try(PreparedStatement select = learnweb.getConnection().prepareStatement(
                "SELECT r.owner_user_id, "
                        + "COUNT(*) as count, COUNT( NULLIF( pronounciation, '' ) ) as pronounciation, "
                        + "COUNT( NULLIF( acronym, '' ) ) as acronym, "
                        + "COUNT( NULLIF( phraseology, '' ) ) as phraseology, "
                        + "COUNT( NULLIF( rgt.use, '' ) ) as uses, "
                        + "COUNT( NULLIF( rgt.references, '' ) ) as source "
                        + "FROM lw_resource r "
                        + "JOIN lw_resource_glossary rg USING(resource_id) "
                        + "JOIN lw_resource_glossary_terms rgt USING(glossary_id) "
                        + "WHERE rg.deleted != 1 AND r.deleted != 1 AND rgt.deleted != 1 "
                        + "AND r.owner_user_id IN(" + StringHelper.implodeInt(userIds, ",") + ") "
                        + "AND rg.timestamp BETWEEN ? AND ? GROUP BY r.owner_user_id"))
        {
            select.setTimestamp(1, new Timestamp(startDate.getTime()));
            select.setTimestamp(2, new Timestamp(endDate.getTime()));
            ResultSet rs = select.executeQuery();

            while(rs.next())
            {
                GlossaryFieldSummery fieldSummery = new GlossaryFieldSummery();
                fieldSummery.setUserId(rs.getInt("owner_user_id"));
                fieldSummery.setTotal(rs.getInt("count"));
                fieldSummery.setPronounciation(rs.getInt("pronounciation"));
                fieldSummery.setAcronym(rs.getInt("acronym"));
                fieldSummery.setPhraseology(rs.getInt("phraseology"));
                fieldSummery.setUses(rs.getInt("uses"));
                fieldSummery.setSource(rs.getInt("source"));

                summeries.add(fieldSummery);
            }
        }

        return summeries;
    }

    public Map<String, Integer> getGlossaryConceptsCountPerUser(Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException
    {
        Map<String, Integer> conceptsPerUser = new TreeMap<>();

        try(PreparedStatement select = learnweb.getConnection().prepareStatement(
                "SELECT u.username, count(*) AS count " +
                        "FROM lw_resource r " +
                        "JOIN lw_user u ON u.user_id = r.owner_user_id " +
                        "JOIN lw_resource_glossary rg USING (resource_id) " +
                        "WHERE rg.deleted != 1 AND r.deleted != 1 " +
                        "AND r.owner_user_id IN(" + StringHelper.implodeInt(userIds, ",") + ") " +
                        "AND rg.timestamp BETWEEN ? AND ? GROUP BY u.username ORDER BY username"))
        {
            select.setTimestamp(1, new Timestamp(startDate.getTime()));
            select.setTimestamp(2, new Timestamp(endDate.getTime()));
            ResultSet rs = select.executeQuery();

            while(rs.next())
                conceptsPerUser.put(rs.getString("username"), rs.getInt("count"));
        }

        return conceptsPerUser;
    }

    public Map<String, Integer> getGlossarySourcesWithCounters(Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException
    {
        Map<String, Integer> countPerSource = new TreeMap<>();

        try(PreparedStatement select = learnweb.getConnection().prepareStatement(
                "SELECT rgt.references as refs, COUNT(*) as count "
                        + "FROM lw_resource r "
                        + "JOIN lw_resource_glossary rg USING(resource_id) "
                        + "JOIN lw_resource_glossary_terms rgt USING(glossary_id) "
                        + "WHERE rg.deleted != 1 AND r.deleted != 1 AND rgt.deleted != 1 "
                        + "AND r.owner_user_id IN(" + StringHelper.implodeInt(userIds, ",") + ") "
                        + "AND rg.timestamp BETWEEN ? AND ? GROUP BY refs"))
        {
            select.setTimestamp(1, new Timestamp(startDate.getTime()));
            select.setTimestamp(2, new Timestamp(endDate.getTime()));
            ResultSet rs = select.executeQuery();

            while(rs.next())
            {
                String source = rs.getString("refs");
                if(source == null || source.trim().isEmpty())
                    countPerSource.put("EMPTY", rs.getInt("count"));
                else
                    countPerSource.put(source, rs.getInt("count"));
            }
        }

        return countPerSource;
    }

    public Map<String, Integer> getGlossaryTermsCountPerUser(Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException
    {
        Map<String, Integer> countPerSource = new TreeMap<>();

        try(PreparedStatement select = learnweb.getConnection().prepareStatement(
                "SELECT u.username, COUNT(distinct rgt.glossary_term_id) as count " +
                        "FROM lw_resource r " +
                        "JOIN lw_user u ON u.user_id = r.owner_user_id " +
                        "JOIN lw_resource_glossary rg USING(resource_id) " +
                        "JOIN lw_resource_glossary_terms rgt USING(glossary_id) " +
                        "WHERE rg.deleted != 1 AND r.deleted != 1 AND rgt.deleted != 1 " +
                        "AND r.owner_user_id IN(" + StringHelper.implodeInt(userIds, ",") + ") " +
                        "AND rg.timestamp BETWEEN ? AND ? group by username order by username"))
        {
            select.setTimestamp(1, new Timestamp(startDate.getTime()));
            select.setTimestamp(2, new Timestamp(endDate.getTime()));
            ResultSet rs = select.executeQuery();

            while(rs.next())
                countPerSource.put(rs.getString("username"), rs.getInt("count"));
        }

        return countPerSource;
    }

    public Map<Integer, Integer> getActionsWithCounters(Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException
    {
        Map<Integer, Integer> countPerAction = new TreeMap<>();

        try(PreparedStatement select = learnweb.getConnection().prepareStatement(
                "SELECT action, COUNT(*) AS count FROM lw_user_log " +
                        "WHERE user_id IN(" + StringHelper.implodeInt(userIds, ",") + ") " +
                        "AND timestamp BETWEEN ? AND ? GROUP BY action"))
        {
            select.setTimestamp(1, new Timestamp(startDate.getTime()));
            select.setTimestamp(2, new Timestamp(endDate.getTime()));
            ResultSet rs = select.executeQuery();

            while(rs.next())
                countPerAction.put(rs.getInt("action"), rs.getInt("count"));
        }

        return countPerAction;
    }

    public Map<String, Integer> getActionsCountPerDay(Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException
    {
        // action name, count
        Map<String, Integer> actionsPerDay = new TreeMap<>();

        try(PreparedStatement select = learnweb.getConnection().prepareStatement(
                "SELECT DATE(timestamp) as day, COUNT(*) AS count FROM lw_user_log " +
                        "WHERE user_id IN(" + StringHelper.implodeInt(userIds, ",") + ") " +
                        "AND timestamp BETWEEN ? AND ? GROUP BY day"))
        {
            select.setTimestamp(1, new Timestamp(startDate.getTime()));
            select.setTimestamp(2, new Timestamp(endDate.getTime()));
            ResultSet rs = select.executeQuery();

            while(rs.next())
                actionsPerDay.put(rs.getString("day"), rs.getInt("count"));
        }

        return actionsPerDay;
    }

    public ArrayList<String> getGlossaryDescriptions(Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException
    {
        ArrayList<String> descriptions = new ArrayList<>();
        return descriptions;

        /*try(PreparedStatement select = learnweb.getConnection().prepareStatement(
                "SELECT rg.description as description " +
                        "FROM lw_resource r " +
                        "JOIN lw_resource_glossary rg USING(resource_id) " +
                        "WHERE rg.deleted != 1 AND r.deleted != 1 " +
                        "AND r.owner_user_id IN(" + StringHelper.implodeInt(userIds, ",") + ") " +
                        "AND rg.timestamp BETWEEN ? AND ?"))
        {
            select.setTimestamp(1, new Timestamp(startDate.getTime()));
            select.setTimestamp(2, new Timestamp(endDate.getTime()));
            ResultSet rs = select.executeQuery();

            while(rs.next())
                descriptions.add(rs.getString("description"));
        }

        return descriptions;*/
    }

    public Map<Integer, GlossaryStatistic> getGlossaryStatisticPerUser(Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException
    {
        Map<Integer, GlossaryStatistic> statPerUser = new TreeMap<>();

        try(PreparedStatement select = learnweb.getConnection().prepareStatement(
                "select owner_user_id, count(distinct glossary_id) as totalGlossary " +
                        "FROM lw_resource r " +
                        "JOIN lw_resource_glossary rg USING(resource_id) " +
                        "WHERE rg.deleted != 1 AND r.deleted != 1 " +
                        "AND r.owner_user_id IN(" + StringHelper.implodeInt(userIds, ",") + ") " +
                        "AND rg.timestamp BETWEEN ? AND ? group by owner_user_id"))
        {
            select.setTimestamp(1, new Timestamp(startDate.getTime()));
            select.setTimestamp(2, new Timestamp(endDate.getTime()));
            ResultSet rs = select.executeQuery();

            while(rs.next())
            {
                GlossaryStatistic gs = new GlossaryStatistic(rs.getInt("owner_user_id"));
                gs.setTotalGlossaries(rs.getInt("totalGlossary"));
                statPerUser.put(gs.getUserId(), gs);
            }
        }

        try(PreparedStatement select = learnweb.getConnection().prepareStatement(
                "SELECT r.owner_user_id, count(*) as totalGlossaryTerms, count(distinct rgt.references) as totalReferences " +
                        "FROM lw_resource r " +
                        "JOIN lw_resource_glossary rg USING(resource_id) " +
                        "JOIN lw_resource_glossary_terms rgt USING(glossary_id) " +
                        "WHERE rg.deleted != 1 AND r.deleted != 1 AND rgt.deleted != 1 " +
                        "AND r.owner_user_id IN(" + StringHelper.implodeInt(userIds, ",") + ") " +
                        "AND rg.timestamp BETWEEN ? AND ? group by owner_user_id"))
        {
            select.setTimestamp(1, new Timestamp(startDate.getTime()));
            select.setTimestamp(2, new Timestamp(endDate.getTime()));
            ResultSet rs = select.executeQuery();

            while(rs.next())
            {
                GlossaryStatistic gs = statPerUser.get(rs.getInt("owner_user_id"));
                gs.setTotalTerms(rs.getInt("totalGlossaryTerms"));
                gs.setTotalReferences(rs.getInt("totalReferences"));
                statPerUser.put(gs.getUserId(), gs);
            }
        }

        return statPerUser;
    }

    public ArrayList<DescFieldData> getLangDescStatistic(Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException
    {
        ArrayList<DescFieldData> langDataList = new ArrayList<>();
        return langDataList;

        /*try(PreparedStatement select = learnweb.getConnection().prepareStatement(
                "SELECT rg.description as description, rg.glossary_id as glossary_id, r.language as language, r.owner_user_id as user_id " +
                        "FROM lw_resource r " +
                        "JOIN lw_resource_glossary rg USING(resource_id) " +
                        "WHERE rg.deleted != 1 AND r.deleted != 1 " +
                        "AND r.owner_user_id IN(" + StringHelper.implodeInt(userIds, ",") + ") " +
                        "AND rg.timestamp BETWEEN ? AND ?"))
        {
            select.setTimestamp(1, new Timestamp(startDate.getTime()));
            select.setTimestamp(2, new Timestamp(endDate.getTime()));
            ResultSet rs = select.executeQuery();

            while(rs.next())
            {
                DescFieldData descFieldData = new DescFieldData();
                descFieldData.setDescription(rs.getString("description"));
                descFieldData.setEntryId(rs.getInt("glossary_id"));
                descFieldData.setLang(rs.getString("language"));
                descFieldData.setUserId(rs.getInt("user_id"));
                langDataList.add(descFieldData);
            }
        }

        return langDataList;*/
    }

    public Map<String, Integer> getProxySourcesWithCounters(String trackerClientId, Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException
    {
        Map<String, Integer> countPerSource = new TreeMap<>();

        try(PreparedStatement select = learnweb.getConnection().prepareStatement(
                "SELECT REPLACE(REPLACE(website_domain, '.secure.waps.io', ''), '.waps.io', '') as domain, COUNT(*) as count "
                        + "FROM tracker.track "
                        + "WHERE status = 'PROCESSED' AND external_client_id = ? "
                        + "AND external_user_id IN(" + StringHelper.implodeInt(userIds, ",") + ") "
                        + "AND created_at BETWEEN ? AND ? group by (domain) order by count desc"))
        {
            select.setString(1, trackerClientId);
            select.setTimestamp(2, new Timestamp(startDate.getTime()));
            select.setTimestamp(3, new Timestamp(endDate.getTime()));
            ResultSet rs = select.executeQuery();

            while(rs.next())
            {
                String domain = rs.getString("domain");
                if(domain == null)
                {
                    log.warn("skip null domain");
                    continue;
                }
                countPerSource.put(domain, rs.getInt("count"));
            }
        }

        return countPerSource;
    }

    public LinkedList<TrackerStatistic> getTrackerStatistics(String trackerClientId, Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException
    {
        LinkedList<TrackerStatistic> statistic = new LinkedList<>();

        try(PreparedStatement select = learnweb.getConnection().prepareStatement(
                "SELECT external_user_id as user_id, sum(total_events) as total_events, sum(time_stay) as time_stay, sum(time_active) as time_active, sum(clicks) as clicks, sum(keypress) as keypresses "
                        + "FROM tracker.track "
                        + "WHERE status = 'PROCESSED' AND external_client_id = ? "
                        + "AND external_user_id IN(" + StringHelper.implodeInt(userIds, ",") + ") "
                        + "AND created_at BETWEEN ? AND ? group by external_user_id"))
        {
            select.setString(1, trackerClientId);
            select.setTimestamp(2, new Timestamp(startDate.getTime()));
            select.setTimestamp(3, new Timestamp(endDate.getTime()));
            ResultSet rs = select.executeQuery();

            while(rs.next())
            {
                TrackerStatistic trackerStatistic = new TrackerStatistic();
                trackerStatistic.setUserId(rs.getInt("user_id"));
                trackerStatistic.setTotalEvents(rs.getInt("total_events"));
                trackerStatistic.setTimeStay(rs.getInt("time_stay"));
                trackerStatistic.setTimeActive(rs.getInt("time_active"));
                trackerStatistic.setClicks(rs.getInt("clicks"));
                trackerStatistic.setKeyPresses(rs.getInt("keypresses"));
                statistic.add(trackerStatistic);
            }
        }

        return statistic;
    }
}