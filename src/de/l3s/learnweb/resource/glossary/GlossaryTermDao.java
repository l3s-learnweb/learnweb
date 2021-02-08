package de.l3s.learnweb.resource.glossary;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import de.l3s.util.SqlHelper;

@RegisterRowMapper(GlossaryTermDao.GlossaryTermMapper.class)
public interface GlossaryTermDao extends SqlObject {
    @SqlQuery("SELECT * FROM lw_glossary_term WHERE term_id = ?")
    Optional<GlossaryTerm> findById(String termId);

    @SqlQuery("SELECT * FROM lw_glossary_term WHERE entry_id = ? and deleted = 0")
    List<GlossaryTerm> findByEntryId(int entryId);

    default void save(GlossaryTerm term) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("term_id", term.getId() < 1 ? null : term.getId());
        params.put("entry_id", term.getEntryId());
        params.put("original_term_id", term.getOriginalTermId());
        params.put("last_changed_by_user_id", term.getLastChangedByUserId());
        params.put("user_id", term.getUserId());
        params.put("term", term.getTerm());
        params.put("language", term.getLanguage().toLanguageTag());
        params.put("uses", term.getUses() == null ? "" : String.join(","));
        params.put("pronounciation", term.getUses());
        params.put("acronym", term.getPronounciation());
        params.put("source", term.getAcronym());
        params.put("phraseology", term.getSource());
        params.put("term_pasted", term.getPhraseology());
        params.put("pronounciation_pasted", term.isTermPasted());
        params.put("lw_glossary_term", term.isPronounciationPasted());
        params.put("acronym_pasted", term.isAcronymPasted());
        params.put("phraseology_pasted", term.isPhraseologyPasted());

        Optional<Integer> termId = SqlHelper.generateInsertQuery(getHandle(), "lw_glossary_term", params)
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
            term.setLanguage(Locale.forLanguageTag(rs.getString("language")));
            term.setUses(rs.getString("uses").isEmpty() ? new ArrayList<>() : Arrays.asList(rs.getString("uses").split(",")));
            term.setPronounciation(rs.getString("pronounciation"));
            term.setAcronym(rs.getString("acronym"));
            term.setSource(rs.getString("source"));
            term.setPhraseology(rs.getString("phraseology"));
            term.setTimestamp(rs.getTimestamp("timestamp"));
            term.setTermPasted(rs.getBoolean("term_pasted"));
            term.setPronounciationPasted(rs.getBoolean("pronounciation_pasted"));
            term.setAcronymPasted(rs.getBoolean("acronym_pasted"));
            term.setPhraseologyPasted(rs.getBoolean("phraseology_pasted"));
            return term;
        }
    }
}
