package de.l3s.learnweb.dashboard;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.*;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.User;
import de.l3s.util.StringHelper;

public class DashboardManager
{
    private static final Logger log = Logger.getLogger(DashboardManager.class);
    private static DashboardManager instance;

    private final Learnweb learnweb;

    public static DashboardManager getInstance(Learnweb learnweb)
    {
        if(instance == null)
            instance = new DashboardManager(learnweb);
        return instance;
    }

    private DashboardManager(Learnweb learnweb)
    {
        super();

        this.learnweb = learnweb;
    }

    public Integer getTotalConcepts(Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException
    {
        int result = 0;

        try (PreparedStatement select = learnweb.getConnection().prepareStatement(
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

        try (PreparedStatement select = learnweb.getConnection().prepareStatement(
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

        try (PreparedStatement select = learnweb.getConnection().prepareStatement(
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

    public List<GlossaryFieldSummery> getGlossaryFieldSummeryPerUser(Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException
    {
        List<GlossaryFieldSummery> summeries = new ArrayList<>(userIds.size());

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
                        + "AND rg.timestamp BETWEEN ? AND ?"))
        {
            select.setTimestamp(1, new Timestamp(startDate.getTime()));
            select.setTimestamp(2, new Timestamp(endDate.getTime()));
            ResultSet rs = select.executeQuery();

            while(rs.next()) {
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
                "SELECT r.owner_user_id, COUNT(distinct rgt.glossary_term_id) as count " +
                        "FROM lw_resource r " +
                        "JOIN lw_resource_glossary rg USING(resource_id) " +
                        "JOIN lw_resource_glossary_terms rgt USING(glossary_id) " +
                        "WHERE rg.deleted != 1 AND r.deleted != 1 AND rgt.deleted != 1 " +
                        "AND r.owner_user_id IN(" + StringHelper.implodeInt(userIds, ",") + ") " +
                        "AND rg.timestamp BETWEEN ? AND ? group by owner_user_id order by owner_user_id"))
        {
            select.setTimestamp(1, new Timestamp(startDate.getTime()));
            select.setTimestamp(2, new Timestamp(endDate.getTime()));
            ResultSet rs = select.executeQuery();

            while(rs.next())
                countPerSource.put(rs.getString("owner_user_id"), rs.getInt("count"));
        }

        return countPerSource;
    }

    public Map<String, Integer> getActionsWithCounters(Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException
    {
        // action name, count
        Map<String, Integer> countPerAction = new TreeMap<>();

        try(PreparedStatement select = learnweb.getConnection().prepareStatement(
                "SELECT le.action, COUNT(*) AS count FROM lw_user_log l " +
                        "JOIN learnweb_logs.log_actions le ON l.action = le.action_id " +
                        "WHERE l.user_id IN(" + StringHelper.implodeInt(userIds, ",") + ") " +
                        "AND l.timestamp BETWEEN ? AND ? GROUP BY le.action"))
        {
            select.setTimestamp(1, new Timestamp(startDate.getTime()));
            select.setTimestamp(2, new Timestamp(endDate.getTime()));
            ResultSet rs = select.executeQuery();

            while(rs.next())
                countPerAction.put(rs.getString("action"), rs.getInt("count"));
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

    public List<String> getGlossaryDescriptions(Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException
    {
        List<String> descriptions = new ArrayList<>();

        try(PreparedStatement select = learnweb.getConnection().prepareStatement(
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

        return descriptions;
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
                GlossaryStatistic gs = new GlossaryStatistic();
                gs.setTotalGlossaries(rs.getInt("totalGlossary"));
                statPerUser.put(rs.getInt("owner_user_id"), gs);
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
                statPerUser.put(rs.getInt("owner_user_id"), gs);
            }
        }

        return statPerUser;
    }

    public Map<String, Integer> getProxySourcesWithCounters(String trackerClientId, Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException
    {
        Map<String, Integer> countPerSource = new TreeMap<>();

        try(PreparedStatement select = learnweb.getConnection().prepareStatement(
                "SELECT REPLACE(REPLACE(SUBSTRING_INDEX(url, '/', 3),'.waps.io',''),'.secure','') as domain, COUNT(*) as count "
                        + "FROM tracker.track "
                        + "WHERE external_client_id = ? "
                        + "AND external_user_id IN(" + StringHelper.implodeInt(userIds, ",") + ") "
                        + "AND created_at BETWEEN ? AND ? group by (domain) order by count desc"))
        {
            select.setString(1, trackerClientId);
            select.setTimestamp(2, new Timestamp(startDate.getTime()));
            select.setTimestamp(3, new Timestamp(endDate.getTime()));
            ResultSet rs = select.executeQuery();

            while(rs.next())
                countPerSource.put(rs.getString("domain"), rs.getInt("count"));
        }

        return countPerSource;
    }

    public List<TrackerStatistic> getTrackerStatistics(String trackerClientId, Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException
    {
        List<TrackerStatistic> statistic = new LinkedList<>();

        try(PreparedStatement select = learnweb.getConnection().prepareStatement(
                "SELECT external_user_id as user_id, sum(total_events) as total_events, sum(time_stay) as time_stay, sum(time_active) as time_active, sum(clicks) as clicks, sum(keypress) as keypresses "
                        + "FROM tracker.track "
                        + "WHERE external_client_id = ? "
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

    public static class GlossaryStatistic
    {
        private int userId = -1;
        private int totalGlossaries = 0;
        private int totalTerms = 0;
        private int totalReferences = 0;

        public int getUserId()
        {
            return userId;
        }

        public void setUserId(int userId)
        {
            this.userId = userId;
        }

        public int getTotalGlossaries()
        {
            return totalGlossaries;
        }

        public void setTotalGlossaries(int totalGlossaries)
        {
            this.totalGlossaries = totalGlossaries;
        }

        public int getTotalTerms()
        {
            return totalTerms;
        }

        public void setTotalTerms(int totalTerms)
        {
            this.totalTerms = totalTerms;
        }

        public int getTotalReferences()
        {
            return totalReferences;
        }

        public void setTotalReferences(int totalReferences)
        {
            this.totalReferences = totalReferences;
        }
    }

    public static class TrackerStatistic
    {
        private int userId;
        private int totalEvents;
        private int timeStay;
        private int timeActive;
        private int clicks;
        private int keyPresses;

        private long timeActiveInMinutes;
        private String timeActiveFormatted;
        private long timeStayInMinutes;
        private String timeStayFormatted;

        public int getUserId()
        {
            return userId;
        }

        public void setUserId(int userId)
        {
            this.userId = userId;
        }

        public int getTotalEvents()
        {
            return totalEvents;
        }

        public void setTotalEvents(int totalEvents)
        {
            this.totalEvents = totalEvents;
        }

        public int getTimeStay()
        {
            return timeStay;
        }

        public void setTimeStay(int timeStay)
        {
            this.timeStay = timeStay;

            Duration durationStay = Duration.ofMillis(timeStay);
            this.timeStayInMinutes = durationStay.toMinutes();
            this.timeStayFormatted = StringHelper.formatDuration(durationStay);
        }

        public int getTimeActive()
        {
            return timeActive;
        }

        public void setTimeActive(int timeActive)
        {
            this.timeActive = timeActive;

            Duration durationActive = Duration.ofMillis(timeActive);
            this.timeActiveInMinutes = durationActive.toMinutes();
            this.timeActiveFormatted = StringHelper.formatDuration(durationActive);
        }

        public int getClicks()
        {
            return clicks;
        }

        public void setClicks(int clicks)
        {
            this.clicks = clicks;
        }

        public int getKeyPresses()
        {
            return keyPresses;
        }

        public void setKeyPresses(int keyPresses)
        {
            this.keyPresses = keyPresses;
        }

        public long getTimeActiveInMinutes()
        {
            return timeActiveInMinutes;
        }

        public void setTimeActiveInMinutes(long timeActiveInMinutes)
        {
            this.timeActiveInMinutes = timeActiveInMinutes;
        }

        public String getTimeActiveFormatted()
        {
            return timeActiveFormatted;
        }

        public void setTimeActiveFormatted(String timeActiveFormatted)
        {
            this.timeActiveFormatted = timeActiveFormatted;
        }

        public long getTimeStayInMinutes()
        {
            return timeStayInMinutes;
        }

        public void setTimeStayInMinutes(long timeStayInMinutes)
        {
            this.timeStayInMinutes = timeStayInMinutes;
        }

        public String getTimeStayFormatted()
        {
            return timeStayFormatted;
        }

        public void setTimeStayFormatted(String timeStayFormatted)
        {
            this.timeStayFormatted = timeStayFormatted;
        }
    }

    public static class GlossaryFieldSummery implements Serializable
    {
        private static final long serialVersionUID = -4378112533840640208L;
        int userId;
        int total;
        int pronounciation;
        int acronym;
        int phraseology;
        int uses;
        int source;

        private transient User user;

        public float getAvg()
        {
            return ((float) (pronounciation + acronym + phraseology + uses + source) / (total * 5));
        }

        public User getUser()
        {
            if(null == user)
            {
                try
                {
                    user = Learnweb.getInstance().getUserManager().getUser(userId);
                }
                catch(SQLException e)
                {
                    log.fatal("can't get user: " + userId, e);
                }
            }
            return user;
        }

        public int getUserid()
        {
            return userId;
        }

        private void setUserId(int userid)
        {
            this.userId = userid;
        }

        public int getTotal()
        {
            return total;
        }

        private void setTotal(int total)
        {
            this.total = total;
        }

        public int getPronounciation()
        {
            return pronounciation;
        }

        private void setPronounciation(int pronounciation)
        {
            this.pronounciation = pronounciation;
        }

        public int getAcronym()
        {
            return acronym;
        }

        private void setAcronym(int acronym)
        {
            this.acronym = acronym;
        }

        public int getPhraseology()
        {
            return phraseology;
        }

        private void setPhraseology(int phraseology)
        {
            this.phraseology = phraseology;
        }

        public int getUses()
        {
            return uses;
        }

        private void setUses(int uses)
        {
            this.uses = uses;
        }

        public int getSource()
        {
            return source;
        }

        private void setSource(int source)
        {
            this.source = source;
        }
    }
}
