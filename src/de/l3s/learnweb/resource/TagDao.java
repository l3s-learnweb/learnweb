package de.l3s.learnweb.resource;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserDao;
import de.l3s.util.SqlHelper;

@RegisterRowMapper(TagDao.TagMapper.class)
public interface TagDao extends SqlObject, Serializable {

    @SqlQuery("SELECT * FROM lw_tag WHERE tag_id = ?")
    Optional<Tag> findById(int tagId);

    @SqlQuery("SELECT * FROM lw_tag WHERE name LIKE ? ORDER BY tag_id LIMIT 1")
    Optional<Tag> findByName(String name);

    @SqlQuery("SELECT DISTINCT t.* FROM lw_resource_tag JOIN lw_tag t USING(tag_id) JOIN lw_resource USING(resource_id) WHERE user_id = ? AND deleted = 0")
    List<Tag> findByUserId(int userId);

    default OwnerList<Tag, User> findByResourceId(int resourceId) {
        return getHandle().select("SELECT * FROM lw_resource_tag JOIN lw_tag USING(tag_id) JOIN lw_user USING(user_id) WHERE resource_id = ?", resourceId)
            .registerRowMapper(new UserDao.UserMapper())
            .registerRowMapper(new TagMapper())
            .reduceRows(new OwnerList<>(), (list, rowView) -> {
                Tag tag = rowView.getRow(Tag.class);
                User user = rowView.getRow(User.class);
                list.add(tag, user, rowView.getColumn("created_at", LocalDateTime.class));
                return list;
            });
    }

    @SqlUpdate("DELETE FROM lw_tag WHERE tag_id = ?")
    void delete(Tag tag);

    default void save(Tag tag) {
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("tag_id", SqlHelper.toNullable(tag.getId()));
        params.put("name", tag.getName());

        Optional<Integer> tagId = SqlHelper.handleSave(getHandle(), "lw_tag", params)
            .executeAndReturnGeneratedKeys().mapTo(Integer.class).findOne();

        if (tagId.isPresent() && tagId.get() != 0) {
            tag.setId(tagId.get());
        }
    }

    class TagMapper implements RowMapper<Tag> {
        @Override
        public Tag map(final ResultSet rs, final StatementContext ctx) throws SQLException {
            return new Tag(rs.getInt("tag_id"), rs.getString("name"));
        }
    }
}
