package de.l3s.learnweb.resource.glossary;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;

import de.l3s.util.SqlHelper;

@RegisterRowMapper(GlossaryEntryDao.GlossaryEntryMapper.class)
@RegisterRowMapper(GlossaryTermDao.GlossaryTermMapper.class)
public interface GlossaryEntryDao extends SqlObject {
    @SqlQuery("SELECT * FROM lw_glossary_entry e JOIN lw_glossary_term t USING (entry_id) WHERE entry_id = ?")
    Optional<GlossaryEntry> findById(String entryId);

    @SqlQuery("SELECT * FROM lw_glossary_entry e JOIN lw_glossary_term t USING (entry_id) WHERE resource_id = ? and e.deleted = 0")
    @UseRowReducer(GlossaryEntryTermReducer.class)
    List<GlossaryEntry> findByResourceId(int resourceId);

    default void deleteSoft(int entryId, int deletedByUserId) {
        getHandle().execute("UPDATE lw_glossary_entry SET deleted = 1, last_changed_by_user_id = ? WHERE entry_id = ?", deletedByUserId, entryId);
        getHandle().execute("UPDATE lw_glossary_term SET deleted = 1, last_changed_by_user_id = ? WHERE entry_id = ?", deletedByUserId, entryId);
    }

    default void save(GlossaryEntry entry) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("entry_id", entry.getId() < 1 ? null : entry.getId());
        params.put("resource_id", entry.getResourceId());
        params.put("original_entry_id", entry.getOriginalEntryId());
        params.put("last_changed_by_user_id", entry.getLastChangedByUserId());
        params.put("user_id", entry.getUserId());
        params.put("topic_one", entry.getTopicOne());
        params.put("topic_two", entry.getTopicTwo());
        params.put("topic_three", entry.getTopicThree());
        params.put("description", entry.getDescription());
        params.put("description_pasted", entry.isDescriptionPasted());

        Optional<Integer> entryId = SqlHelper.generateInsertQuery(getHandle(), "lw_glossary_entry", params)
            .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

        entryId.ifPresent(entry::setId);
    }

    class GlossaryEntryMapper implements RowMapper<GlossaryEntry> {
        @Override
        public GlossaryEntry map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            GlossaryEntry entry = new GlossaryEntry();
            entry.setDeleted(false);
            entry.setResourceId(rs.getInt("resource_id"));
            entry.setId(rs.getInt("entry_id"));
            entry.setOriginalEntryId(rs.getInt("original_entry_id"));
            entry.setUserId(rs.getInt("user_id"));
            entry.setTopicOne(rs.getString("topic_one"));
            entry.setTopicTwo(rs.getString("topic_two"));
            entry.setTopicThree(rs.getString("topic_three"));
            entry.setDescription(rs.getString("description"));
            entry.setDescriptionPasted(rs.getBoolean("description_pasted"));
            entry.setTimestamp(rs.getTimestamp("timestamp"));
            return entry;
        }
    }

    class GlossaryEntryTermReducer implements LinkedHashMapRowReducer<Integer, GlossaryEntry> {
        @Override
        public void accumulate(Map<Integer, GlossaryEntry> map, RowView rowView) {
            int entryId = rowView.getColumn("entry_id", Integer.class);
            GlossaryEntry entry = map.computeIfAbsent(entryId, id -> rowView.getRow(GlossaryEntry.class));

            if (rowView.getColumn("term_id", Integer.class) != null) {
                entry.getTerms().add(rowView.getRow(GlossaryTerm.class));
            }
        }
    }
}
