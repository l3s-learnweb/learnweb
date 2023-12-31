package de.l3s.learnweb.resource.glossary;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.result.LinkedHashMapRowReducer;
import org.jdbi.v3.core.result.RowView;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.KeyColumn;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.config.ValueColumn;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.UseRowReducer;

import de.l3s.learnweb.dashboard.glossary.GlossaryDescriptionSummary;
import de.l3s.util.SqlHelper;

@RegisterRowMapper(GlossaryEntryDao.GlossaryEntryMapper.class)
@RegisterRowMapper(GlossaryTermDao.GlossaryTermMapper.class)
public interface GlossaryEntryDao extends SqlObject, Serializable {

    @SqlQuery("SELECT *, (SELECT COUNT(*) FROM lw_glossary_entry_file f WHERE e.entry_id = f.entry_id) as pictures_count FROM lw_glossary_entry e WHERE entry_id = ?")
    Optional<GlossaryEntry> findById(int entryId);

    @SqlQuery("SELECT *, (SELECT COUNT(*) FROM lw_glossary_entry_file f WHERE e.entry_id = f.entry_id) as pictures_count FROM lw_glossary_entry e JOIN lw_glossary_term t USING (entry_id) WHERE e.resource_id = ? and e.deleted = 0")
    @UseRowReducer(GlossaryEntryTermReducer.class)
    List<GlossaryEntry> findByResourceId(int resourceId);

    default void deleteSoft(GlossaryEntry entry, int deletedByUserId) {
        getHandle().execute("UPDATE lw_glossary_entry SET deleted = 1, edit_user_id = ? WHERE entry_id = ?", deletedByUserId, entry);
        getHandle().execute("UPDATE lw_glossary_term SET deleted = 1, edit_user_id = ? WHERE entry_id = ?", deletedByUserId, entry);

        entry.setDeleted(true);
    }

    @SqlQuery("SELECT COUNT(distinct ge.entry_id) FROM lw_resource r JOIN lw_glossary_entry ge USING(resource_id) "
        + "WHERE ge.deleted != 1 AND r.deleted != 1 AND r.owner_user_id IN(<userIds>) AND ge.created_at BETWEEN :start AND :end")
    int countTotalEntries(@BindList("userIds") Collection<Integer> userIds, @Bind("start") LocalDate startDate, @Bind("end") LocalDate endDate);

    @SqlQuery("SELECT u.username, count(*) AS count FROM lw_resource r JOIN lw_user u ON u.user_id = r.owner_user_id JOIN lw_glossary_entry ge USING (resource_id) "
        + "WHERE ge.deleted != 1 AND r.deleted != 1 AND r.owner_user_id IN(<userIds>) AND ge.created_at BETWEEN :start AND :end GROUP BY u.username ORDER BY username")
    @KeyColumn("username")
    @ValueColumn("count")
    Map<String, Integer> countEntriesPerUser(@BindList("userIds") Collection<Integer> userIds, @Bind("start") LocalDate startDate, @Bind("end") LocalDate endDate);

    @RegisterRowMapper(GlossaryDescriptionSummaryMapper.class)
    @SqlQuery("SELECT entry_id, resource_id, user_id, description, description_pasted FROM lw_glossary_entry WHERE deleted != 1 AND user_id IN(<userIds>) AND created_at BETWEEN :start AND :end")
    List<GlossaryDescriptionSummary> countGlossaryDescriptionSummary(@BindList("userIds") Collection<Integer> userIds, @Bind("start") LocalDate startDate, @Bind("end") LocalDate endDate);

    default void save(GlossaryEntry entry) {
        if (entry.getUserId() == 0) {
            entry.setUserId(entry.getLastChangedByUserId()); // last change by userID == original user ID in insert
        }

        entry.setUpdatedAt(SqlHelper.now());
        if (entry.getCreatedAt() == null) {
            entry.setCreatedAt(entry.getUpdatedAt());
        }

        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("entry_id", SqlHelper.toNullable(entry.getId()));
        params.put("resource_id", entry.getResourceId());
        params.put("original_entry_id", SqlHelper.toNullable(entry.getOriginalEntryId()));
        params.put("edit_user_id", SqlHelper.toNullable(entry.getLastChangedByUserId()));
        params.put("user_id", SqlHelper.toNullable(entry.getUserId()));
        params.put("topic_one", entry.getTopicOne());
        params.put("topic_two", entry.getTopicTwo());
        params.put("topic_three", entry.getTopicThree());
        params.put("description", entry.getDescription());
        params.put("description_pasted", entry.isDescriptionPasted());
        params.put("imported", entry.isImported());
        params.put("updated_at", entry.getUpdatedAt());
        params.put("created_at", entry.getCreatedAt());

        Optional<Integer> entryId = SqlHelper.handleSave(getHandle(), "lw_glossary_entry", params)
            .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

        if (entryId.isPresent() && entryId.get() != 0) {
            entry.setId(entryId.get());
        }
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
            entry.setImported(rs.getBoolean("imported"));
            entry.setPicturesCount(rs.getInt("pictures_count"));
            entry.setUpdatedAt(SqlHelper.getLocalDateTime(rs.getTimestamp("updated_at")));
            entry.setCreatedAt(SqlHelper.getLocalDateTime(rs.getTimestamp("created_at")));
            return entry;
        }
    }

    class GlossaryEntryTermReducer implements LinkedHashMapRowReducer<Integer, GlossaryEntry> {
        @Override
        public void accumulate(Map<Integer, GlossaryEntry> map, RowView rowView) {
            int entryId = rowView.getColumn("entry_id", Integer.class);
            GlossaryEntry entry = map.computeIfAbsent(entryId, id -> rowView.getRow(GlossaryEntry.class));

            if (rowView.getColumn("term_id", Integer.class) != null) {
                entry.addTerm(rowView.getRow(GlossaryTerm.class));
            }
        }
    }

    class GlossaryDescriptionSummaryMapper implements RowMapper<GlossaryDescriptionSummary> {
        @Override
        public GlossaryDescriptionSummary map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            GlossaryDescriptionSummary result = new GlossaryDescriptionSummary();
            result.setEntryId(rs.getInt("entry_id"));
            result.setResourceId(rs.getInt("resource_id"));
            result.setUserId(rs.getInt("user_id"));
            result.setDescription(rs.getString("description"));
            result.setDescriptionPasted(rs.getBoolean("description_pasted"));
            return result;
        }
    }
}
