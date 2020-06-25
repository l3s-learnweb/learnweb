package de.l3s.learnweb.dashboard.glossary;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import de.l3s.learnweb.Learnweb;

public class GlossaryDashboardManager {
    //private static final Logger log = LogManager.getLogger(GlossaryDashboardManager.class);

    private final Learnweb learnweb;

    public GlossaryDashboardManager(Learnweb learnweb) {
        this.learnweb = learnweb;
    }

    Integer getTotalConcepts(Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException {
        int result = 0;

        try (PreparedStatement select = learnweb.getConnection().prepareStatement(
            "SELECT COUNT(distinct ge.entry_id) AS total_concepts "
                + "FROM lw_resource r "
                + "JOIN lw_glossary_entry ge USING(resource_id) "
                + "WHERE ge.deleted != 1 AND r.deleted != 1 "
                + "AND r.owner_user_id IN(" + StringUtils.join(userIds, ",") + ") "
                + "AND ge.timestamp BETWEEN ? AND ?")) {
            select.setTimestamp(1, new Timestamp(startDate.getTime()));
            select.setTimestamp(2, new Timestamp(endDate.getTime()));
            ResultSet rs = select.executeQuery();

            while (rs.next()) {
                result = rs.getInt("total_concepts");
            }
        }

        return result;
    }

    Integer getTotalTerms(Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException {
        int result = 0;

        try (PreparedStatement select = learnweb.getConnection().prepareStatement(
            "SELECT COUNT(*) AS total_terms "
                + "FROM lw_resource r "
                + "JOIN lw_glossary_entry ge USING(resource_id) "
                + "JOIN lw_glossary_term gt USING(entry_id) "
                + "WHERE ge.deleted != 1 AND r.deleted != 1 AND gt.deleted != 1 "
                + "AND r.owner_user_id IN(" + StringUtils.join(userIds, ",") + ") "
                + "AND ge.timestamp BETWEEN ? AND ?")) {
            select.setTimestamp(1, new Timestamp(startDate.getTime()));
            select.setTimestamp(2, new Timestamp(endDate.getTime()));
            ResultSet rs = select.executeQuery();

            while (rs.next()) {
                result = rs.getInt("total_terms");
            }
        }

        return result;
    }

    Integer getTotalSources(Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException {
        int result = 0;

        try (PreparedStatement select = learnweb.getConnection().prepareStatement(
            "SELECT COUNT(distinct gt.source) AS total_sources "
                + "FROM lw_resource r "
                + "JOIN lw_glossary_entry ge USING(resource_id) "
                + "JOIN lw_glossary_term gt USING(entry_id) "
                + "WHERE ge.deleted != 1 AND r.deleted != 1 AND gt.deleted != 1 "
                + "AND r.owner_user_id IN(" + StringUtils.join(userIds, ",") + ") "
                + "AND ge.timestamp BETWEEN ? AND ?")) {
            select.setTimestamp(1, new Timestamp(startDate.getTime()));
            select.setTimestamp(2, new Timestamp(endDate.getTime()));
            ResultSet rs = select.executeQuery();

            while (rs.next()) {
                result = rs.getInt("total_sources");
            }
        }

        return result;
    }

    ArrayList<GlossaryUserTermsSummary> getGlossaryFieldSummeryPerUser(Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException {
        ArrayList<GlossaryUserTermsSummary> summeries = new ArrayList<>(userIds.size());

        try (PreparedStatement select = learnweb.getConnection().prepareStatement(
            "SELECT ge.user_id, "
                + "COUNT(*) AS total_terms, "
                + "COUNT(distinct entry_id) AS entries,"
                + "COUNT( NULLIF( gt.term_pasted, 0 ) ) AS term_pasted, "
                + "COUNT( NULLIF( gt.pronounciation, '' ) ) AS pronounciation, "
                + "COUNT( NULLIF( gt.pronounciation_pasted, 0 ) ) AS pronounciation_pasted, "
                + "COUNT( NULLIF( gt.acronym, '' ) ) AS acronym, "
                + "COUNT( NULLIF( gt.acronym_pasted, 0 ) ) AS acronym_pasted, "
                + "COUNT( NULLIF( gt.phraseology, '' ) ) AS phraseology, "
                + "COUNT( NULLIF( gt.phraseology_pasted, 0 ) ) AS phraseology_pasted, "
                + "COUNT( NULLIF( gt.uses, '' ) ) AS uses, "
                + "COUNT( NULLIF( gt.source, '' ) ) AS source "
                + "FROM lw_resource r "
                + "JOIN lw_glossary_entry ge USING(resource_id) "
                + "JOIN lw_glossary_term gt USING(entry_id) "
                + "WHERE ge.deleted != 1 AND r.deleted != 1 AND gt.deleted != 1 "
                + "AND ge.user_id IN(" + StringUtils.join(userIds, ",") + ") "
                + "AND ge.timestamp BETWEEN ? AND ? GROUP BY ge.user_id")) {
            select.setTimestamp(1, new Timestamp(startDate.getTime()));
            select.setTimestamp(2, new Timestamp(endDate.getTime()));
            ResultSet rs = select.executeQuery();

            while (rs.next()) {
                GlossaryUserTermsSummary fieldSummery = new GlossaryUserTermsSummary();
                fieldSummery.setUserId(rs.getInt("user_id"));
                fieldSummery.setEntries(rs.getInt("entries"));
                fieldSummery.setTerms(rs.getInt("total_terms"));
                fieldSummery.setTermsPasted(rs.getInt("term_pasted"));
                fieldSummery.setPronounciation(rs.getInt("pronounciation"));
                fieldSummery.setPronounciationPasted(rs.getInt("pronounciation_pasted"));
                fieldSummery.setAcronym(rs.getInt("acronym"));
                fieldSummery.setAcronymPasted(rs.getInt("acronym_pasted"));
                fieldSummery.setPhraseology(rs.getInt("phraseology"));
                fieldSummery.setPhraseologyPasted(rs.getInt("phraseology_pasted"));
                fieldSummery.setUses(rs.getInt("uses"));
                fieldSummery.setSource(rs.getInt("source"));

                summeries.add(fieldSummery);
            }
        }

        return summeries;
    }

    Map<String, Integer> getGlossaryConceptsCountPerUser(Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException {
        Map<String, Integer> conceptsPerUser = new TreeMap<>();

        try (PreparedStatement select = learnweb.getConnection().prepareStatement(
            "SELECT u.username, count(*) AS total_concepts " +
                "FROM lw_resource r " +
                "JOIN lw_user u ON u.user_id = r.owner_user_id " +
                "JOIN lw_glossary_entry ge USING (resource_id) " +
                "WHERE ge.deleted != 1 AND r.deleted != 1 " +
                "AND r.owner_user_id IN(" + StringUtils.join(userIds, ",") + ") " +
                "AND ge.timestamp BETWEEN ? AND ? GROUP BY u.username ORDER BY username")) {
            select.setTimestamp(1, new Timestamp(startDate.getTime()));
            select.setTimestamp(2, new Timestamp(endDate.getTime()));
            ResultSet rs = select.executeQuery();

            while (rs.next()) {
                conceptsPerUser.put(rs.getString("username"), rs.getInt("total_concepts"));
            }
        }

        return conceptsPerUser;
    }

    Map<String, Integer> getGlossarySourcesWithCounters(Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException {
        Map<String, Integer> countPerSource = new TreeMap<>();

        try (PreparedStatement select = learnweb.getConnection().prepareStatement(
            "SELECT gt.source AS refs, COUNT(*) AS total_terms "
                + "FROM lw_resource r "
                + "JOIN lw_glossary_entry ge USING(resource_id) "
                + "JOIN lw_glossary_term gt USING(entry_id) "
                + "WHERE ge.deleted != 1 AND r.deleted != 1 AND gt.deleted != 1 "
                + "AND r.owner_user_id IN(" + StringUtils.join(userIds, ",") + ") "
                + "AND ge.timestamp BETWEEN ? AND ? GROUP BY refs")) {
            select.setTimestamp(1, new Timestamp(startDate.getTime()));
            select.setTimestamp(2, new Timestamp(endDate.getTime()));
            ResultSet rs = select.executeQuery();

            while (rs.next()) {
                String source = rs.getString("refs");
                if (source == null || source.trim().isEmpty()) {
                    countPerSource.put("EMPTY", rs.getInt("total_terms"));
                } else {
                    countPerSource.put(source, rs.getInt("total_terms"));
                }
            }
        }

        return countPerSource;
    }

    Map<String, Integer> getGlossaryTermsCountPerUser(Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException {
        Map<String, Integer> countPerSource = new TreeMap<>();

        try (PreparedStatement select = learnweb.getConnection().prepareStatement(
            "SELECT u.username, COUNT(distinct gt.term_id) AS total_terms " +
                "FROM lw_resource r " +
                "JOIN lw_user u ON u.user_id = r.owner_user_id " +
                "JOIN lw_glossary_entry ge USING(resource_id) " +
                "JOIN lw_glossary_term gt USING(entry_id) " +
                "WHERE ge.deleted != 1 AND r.deleted != 1 AND gt.deleted != 1 " +
                "AND r.owner_user_id IN(" + StringUtils.join(userIds, ",") + ") " +
                "AND ge.timestamp BETWEEN ? AND ? group by username order by username")) {
            select.setTimestamp(1, new Timestamp(startDate.getTime()));
            select.setTimestamp(2, new Timestamp(endDate.getTime()));
            ResultSet rs = select.executeQuery();

            while (rs.next()) {
                countPerSource.put(rs.getString("username"), rs.getInt("total_terms"));
            }
        }

        return countPerSource;
    }

    Map<Integer, Integer> getActionsWithCounters(Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException {
        Map<Integer, Integer> countPerAction = new TreeMap<>();

        try (PreparedStatement select = learnweb.getConnection().prepareStatement(
            "SELECT action, COUNT(*) AS total_records FROM lw_user_log " +
                "WHERE user_id IN(" + StringUtils.join(userIds, ",") + ") " +
                "AND timestamp BETWEEN ? AND ? GROUP BY action")) {
            select.setTimestamp(1, new Timestamp(startDate.getTime()));
            select.setTimestamp(2, new Timestamp(endDate.getTime()));
            ResultSet rs = select.executeQuery();

            while (rs.next()) {
                countPerAction.put(rs.getInt("action"), rs.getInt("total_records"));
            }
        }

        return countPerAction;
    }

    Map<String, Integer> getActionsCountPerDay(Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException {
        // action name, count
        Map<String, Integer> actionsPerDay = new TreeMap<>();

        try (PreparedStatement select = learnweb.getConnection().prepareStatement(
            "SELECT DATE(timestamp) AS day, COUNT(*) AS total_records FROM lw_user_log " +
                "WHERE user_id IN(" + StringUtils.join(userIds, ",") + ") " +
                "AND timestamp BETWEEN ? AND ? GROUP BY day")) {
            select.setTimestamp(1, new Timestamp(startDate.getTime()));
            select.setTimestamp(2, new Timestamp(endDate.getTime()));
            ResultSet rs = select.executeQuery();

            while (rs.next()) {
                actionsPerDay.put(rs.getString("day"), rs.getInt("total_records"));
            }
        }

        return actionsPerDay;
    }

    ArrayList<String> getGlossaryDescriptions(Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException {
        ArrayList<String> descriptions = new ArrayList<>();

        try (PreparedStatement select = learnweb.getConnection().prepareStatement(
            "SELECT ge.description AS description " +
                "FROM lw_resource r " +
                "JOIN lw_glossary_entry ge USING(resource_id) " +
                "WHERE ge.deleted != 1 AND r.deleted != 1 " +
                "AND r.owner_user_id IN(" + StringUtils.join(userIds, ",") + ") " +
                "AND ge.timestamp BETWEEN ? AND ?")) {
            select.setTimestamp(1, new Timestamp(startDate.getTime()));
            select.setTimestamp(2, new Timestamp(endDate.getTime()));
            ResultSet rs = select.executeQuery();

            while (rs.next()) {
                descriptions.add(rs.getString("description"));
            }
        }

        return descriptions;
    }

    Map<Integer, GlossaryUserActivity> getGlossaryStatisticPerUser(Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException {
        Map<Integer, GlossaryUserActivity> statPerUser = new TreeMap<>();

        try (PreparedStatement select = learnweb.getConnection().prepareStatement(
            "select r.owner_user_id, count(distinct ge.entry_id) AS totalGlossary " +
                "FROM lw_resource r " +
                "JOIN lw_glossary_entry ge USING(resource_id) " +
                "WHERE ge.deleted != 1 AND r.deleted != 1 " +
                "AND r.owner_user_id IN(" + StringUtils.join(userIds, ",") + ") " +
                "AND ge.timestamp BETWEEN ? AND ? group by r.owner_user_id")) {
            select.setTimestamp(1, new Timestamp(startDate.getTime()));
            select.setTimestamp(2, new Timestamp(endDate.getTime()));
            ResultSet rs = select.executeQuery();

            while (rs.next()) {
                GlossaryUserActivity gs = new GlossaryUserActivity(rs.getInt("owner_user_id"));
                gs.setTotalGlossaries(rs.getInt("totalGlossary"));
                statPerUser.put(gs.getUserId(), gs);
            }
        }

        try (PreparedStatement select = learnweb.getConnection().prepareStatement(
            "SELECT r.owner_user_id, count(*) AS totalGlossaryTerms, count(distinct gt.source) AS totalReferences " +
                "FROM lw_resource r " +
                "JOIN lw_glossary_entry ge USING(resource_id) " +
                "JOIN lw_glossary_term gt USING(entry_id) " +
                "WHERE ge.deleted != 1 AND r.deleted != 1 AND gt.deleted != 1 " +
                "AND r.owner_user_id IN(" + StringUtils.join(userIds, ",") + ") " +
                "AND ge.timestamp BETWEEN ? AND ? group by owner_user_id")) {
            select.setTimestamp(1, new Timestamp(startDate.getTime()));
            select.setTimestamp(2, new Timestamp(endDate.getTime()));
            ResultSet rs = select.executeQuery();

            while (rs.next()) {
                GlossaryUserActivity gs = statPerUser.get(rs.getInt("owner_user_id"));
                gs.setTotalTerms(rs.getInt("totalGlossaryTerms"));
                gs.setTotalReferences(rs.getInt("totalReferences"));
                statPerUser.put(gs.getUserId(), gs);
            }
        }

        return statPerUser;
    }

    List<GlossaryEntryDescLang> getLangDescStatistic(Collection<Integer> userIds, Date startDate, Date endDate) throws SQLException {
        List<GlossaryEntryDescLang> descFieldDataList = new ArrayList<>();

        try (PreparedStatement select = learnweb.getConnection().prepareStatement(
            "SELECT entry_id, resource_id, user_id, description, description_pasted " +
                "FROM lw_glossary_entry " +
                "WHERE deleted != 1 AND user_id IN(" + StringUtils.join(userIds, ",") + ") " +
                "AND timestamp BETWEEN ? AND ?")) {
            select.setTimestamp(1, new Timestamp(startDate.getTime()));
            select.setTimestamp(2, new Timestamp(endDate.getTime()));
            ResultSet rs = select.executeQuery();

            while (rs.next()) {
                GlossaryEntryDescLang descFieldData = new GlossaryEntryDescLang();
                descFieldData.setEntryId(rs.getInt(1));
                descFieldData.setResourceId(rs.getInt(2));
                descFieldData.setUserId(rs.getInt(3));
                descFieldData.setDescription(rs.getString(4));
                descFieldData.setDescriptionPasted(rs.getBoolean(5));
                descFieldDataList.add(descFieldData);
            }
        }

        return descFieldDataList;
    }
}
