package de.l3s.learnweb.resource.glossary;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.config.ValueColumn;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import de.l3s.learnweb.dashboard.glossary.GlossaryUserActivity;
import de.l3s.learnweb.dashboard.glossary.GlossaryUserTermsSummary;
import de.l3s.util.SqlHelper;

@RegisterRowMapper(GlossaryTermDao.GlossaryTermMapper.class)
public interface GlossaryTermDao extends SqlObject, Serializable {

    @SqlQuery("SELECT * FROM lw_glossary_term WHERE term_id = ?")
    Optional<GlossaryTerm> findById(String termId);

    @SqlQuery("SELECT * FROM lw_glossary_term WHERE entry_id = ? and deleted = 0")
    List<GlossaryTerm> findByEntryId(int entryId);

    @SqlQuery("SELECT COUNT(*) FROM lw_resource r JOIN lw_glossary_entry ge USING(resource_id) JOIN lw_glossary_term gt USING(entry_id) "
        + "WHERE ge.deleted != 1 AND r.deleted != 1 AND gt.deleted != 1 AND r.owner_user_id IN(<userIds>) AND ge.created_at BETWEEN :start AND :end")
    int countTotalTerms(@BindList("userIds") Collection<Integer> userIds, @Bind("start") LocalDate startDate, @Bind("end") LocalDate endDate);

    @SqlQuery("SELECT u.username, COUNT(distinct gt.term_id) AS count FROM lw_resource r JOIN lw_user u ON u.user_id = r.owner_user_id JOIN lw_glossary_entry ge "
        + "USING(resource_id) JOIN lw_glossary_term gt USING(entry_id) WHERE ge.deleted != 1 AND r.deleted != 1 AND gt.deleted != 1"
        + " AND r.owner_user_id IN(<userIds>) AND ge.created_at BETWEEN :start AND :end GROUP BY username ORDER BY username")
    @KeyColumn("username")
    @ValueColumn("count")
    Map<String, Integer> countTermsPerUser(@BindList("userIds") Collection<Integer> userIds, @Bind("start") LocalDate startDate, @Bind("end") LocalDate endDate);

    @SqlQuery("SELECT COUNT(distinct gt.source) FROM lw_resource r JOIN lw_glossary_entry ge USING(resource_id) JOIN lw_glossary_term gt USING(entry_id) "
        + "WHERE ge.deleted != 1 AND r.deleted != 1 AND gt.deleted != 1 AND r.owner_user_id IN(<userIds>) AND ge.created_at BETWEEN :start AND :end")
    int countTotalSources(@BindList("userIds") Collection<Integer> userIds, @Bind("start") LocalDate startDate, @Bind("end") LocalDate endDate);

    @SqlQuery("SELECT gt.source AS refs, COUNT(*) AS count FROM lw_resource r JOIN lw_glossary_entry ge USING(resource_id) JOIN lw_glossary_term gt USING(entry_id) "
        + "WHERE ge.deleted != 1 AND r.deleted != 1 AND gt.deleted != 1 AND r.owner_user_id IN(<userIds>) AND ge.created_at BETWEEN :start AND :end GROUP BY refs")
    @KeyColumn("refs")
    @ValueColumn("count")
    Map<String, Integer> countUsagePerSource(@BindList("userIds") Collection<Integer> userIds, @Bind("start") LocalDate startDate, @Bind("end") LocalDate endDate);

    @RegisterRowMapper(GlossaryUserTermsSummaryMapper.class)
    @SqlQuery("SELECT ge.user_id, COUNT(*) AS total_terms, COUNT(distinct entry_id) AS entries,COUNT( NULLIF( gt.term_pasted, 0 ) ) AS term_pasted, "
        + "COUNT( NULLIF( gt.pronounciation, '' ) ) AS pronounciation, COUNT( NULLIF( gt.pronounciation_pasted, 0 ) ) AS pronounciation_pasted, "
        + "COUNT( NULLIF( gt.acronym, '' ) ) AS acronym, COUNT( NULLIF( gt.acronym_pasted, 0 ) ) AS acronym_pasted, "
        + "COUNT( NULLIF( gt.phraseology, '' ) ) AS phraseology, COUNT( NULLIF( gt.phraseology_pasted, 0 ) ) AS phraseology_pasted, "
        + "COUNT( NULLIF( gt.uses, '' ) ) AS uses, COUNT( NULLIF( gt.source, '' ) ) AS source "
        + "FROM lw_resource r JOIN lw_glossary_entry ge USING(resource_id) JOIN lw_glossary_term gt USING(entry_id) "
        + "WHERE ge.deleted != 1 AND r.deleted != 1 AND gt.deleted != 1 AND ge.user_id IN(<userIds>) AND ge.created_at BETWEEN :start AND :end GROUP BY ge.user_id")
    List<GlossaryUserTermsSummary> countGlossaryUserTermsSummary(@BindList("userIds") Collection<Integer> userIds, @Bind("start") LocalDate startDate, @Bind("end") LocalDate endDate);

    @RegisterRowMapper(GlossaryUserActivityMapper.class)
    @SqlQuery("SELECT r.owner_user_id, count(distinct ge.entry_id) AS total_entries, count(*) AS total_terms, count(distinct gt.source) AS total_refs "
        + "FROM lw_resource r JOIN lw_glossary_entry ge USING(resource_id) JOIN lw_glossary_term gt USING(entry_id) "
        + "WHERE ge.deleted != 1 AND r.deleted != 1 AND gt.deleted != 1 AND r.owner_user_id IN(<userIds>) AND ge.created_at BETWEEN :start AND :end GROUP BY owner_user_id")
    List<GlossaryUserActivity> countGlossaryUserActivity(@BindList("userIds") Collection<Integer> userIds, @Bind("start") LocalDate startDate, @Bind("end") LocalDate endDate);

    default void save(GlossaryTerm term) {
        term.setUpdatedAt(SqlHelper.now());
        if (term.getCreatedAt() == null) {
            term.setCreatedAt(term.getUpdatedAt());
        }

        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("term_id", SqlHelper.toNullable(term.getId()));
        params.put("entry_id", term.getEntryId());
        params.put("original_term_id", SqlHelper.toNullable(term.getOriginalTermId()));
        params.put("edit_user_id", SqlHelper.toNullable(term.getLastChangedByUserId()));
        params.put("user_id", SqlHelper.toNullable(term.getUserId()));
        params.put("term", term.getTerm());
        params.put("language", term.getLanguage().toLanguageTag());
        params.put("uses", term.getUses() == null ? "" : String.join(",", term.getUses()));
        params.put("pronounciation", term.getPronounciation());
        params.put("acronym", term.getAcronym());
        params.put("source", term.getSource());
        params.put("phraseology", term.getPhraseology());
        params.put("term_pasted", term.isTermPasted());
        params.put("pronounciation_pasted", term.isPronounciationPasted());
        params.put("acronym_pasted", term.isAcronymPasted());
        params.put("phraseology_pasted", term.isPhraseologyPasted());
        params.put("updated_at", term.getUpdatedAt());
        params.put("created_at", term.getCreatedAt());

        Optional<Integer> termId = SqlHelper.handleSave(getHandle(), "lw_glossary_term", params)
            .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

        termId.ifPresent(term::setId);
    }

    class GlossaryTermMapper implements RowMapper<GlossaryTerm> {
        @Override
        public GlossaryTerm map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            GlossaryTerm term = new GlossaryTerm();
            term.setId(rs.getInt("term_id"));
            term.setEntryId(rs.getInt("entry_id"));
            term.setUserId(rs.getInt("user_id"));
            term.setTerm(rs.getString("term"));
            term.setOriginalTermId(rs.getInt("original_term_id"));
            term.setLanguage(Locale.forLanguageTag(rs.getString("language")));
            term.setUses(rs.getString("uses").isEmpty() ? new ArrayList<>() : Arrays.asList(rs.getString("uses").split(",")));
            term.setPronounciation(rs.getString("pronounciation"));
            term.setAcronym(rs.getString("acronym"));
            term.setSource(rs.getString("source"));
            term.setPhraseology(rs.getString("phraseology"));
            term.setTermPasted(rs.getBoolean("term_pasted"));
            term.setPronounciationPasted(rs.getBoolean("pronounciation_pasted"));
            term.setAcronymPasted(rs.getBoolean("acronym_pasted"));
            term.setPhraseologyPasted(rs.getBoolean("phraseology_pasted"));
            term.setUpdatedAt(SqlHelper.getLocalDateTime(rs.getTimestamp("updated_at")));
            term.setCreatedAt(SqlHelper.getLocalDateTime(rs.getTimestamp("created_at")));
            return term;
        }
    }

    class GlossaryUserTermsSummaryMapper implements RowMapper<GlossaryUserTermsSummary> {
        @Override
        public GlossaryUserTermsSummary map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            GlossaryUserTermsSummary result = new GlossaryUserTermsSummary();
            result.setUserId(rs.getInt("user_id"));
            result.setEntries(rs.getInt("entries"));
            result.setTerms(rs.getInt("total_terms"));
            result.setTermsPasted(rs.getInt("term_pasted"));
            result.setPronounciation(rs.getInt("pronounciation"));
            result.setPronounciationPasted(rs.getInt("pronounciation_pasted"));
            result.setAcronym(rs.getInt("acronym"));
            result.setAcronymPasted(rs.getInt("acronym_pasted"));
            result.setPhraseology(rs.getInt("phraseology"));
            result.setPhraseologyPasted(rs.getInt("phraseology_pasted"));
            result.setUses(rs.getInt("uses"));
            result.setSource(rs.getInt("source"));
            return result;
        }
    }

    class GlossaryUserActivityMapper implements RowMapper<GlossaryUserActivity> {
        @Override
        public GlossaryUserActivity map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            GlossaryUserActivity result = new GlossaryUserActivity(rs.getInt("owner_user_id"));
            result.setTotalGlossaries(rs.getInt("total_entries"));
            result.setTotalTerms(rs.getInt("total_terms"));
            result.setTotalReferences(rs.getInt("total_refs"));
            return result;
        }
    }
}
