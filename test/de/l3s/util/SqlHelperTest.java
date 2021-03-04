package de.l3s.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SqlHelperTest {

    @Test
    void generateInsertReplaceQuery() {
        String query = SqlHelper.generateInsertReplaceQuery("lw_forum_topic", new String[] {"topic_id", "group_id", "title", "user_id", "created_at"});
        assertEquals("INSERT INTO lw_forum_topic (topic_id,group_id,title,user_id,created_at) VALUES (?,?,?,?,?) ON DUPLICATE KEY "
            + "UPDATE group_id=VALUES(group_id),title=VALUES(title),user_id=VALUES(user_id),created_at=VALUES(created_at)", query);
    }

    @Test
    void generateInsertQuery() {
        String query = SqlHelper.generateInsertQuery("lw_forum_topic", new String[] {"topic_id", "group_id", "title", "user_id", "created_at"});
        assertEquals("INSERT INTO lw_forum_topic (topic_id,group_id,title,user_id,created_at) VALUES (?,?,?,?,?)", query);
    }

    @Test
    void generateUpdateQuery() {
        String query = SqlHelper.generateUpdateQuery("lw_forum_topic", "topic_id", new String[] {"group_id", "title", "user_id", "created_at"});
        assertEquals("UPDATE lw_forum_topic SET group_id=?,title=?,user_id=?,created_at=? WHERE topic_id=?", query);
    }
}
