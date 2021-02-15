package de.l3s.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SqlHelperTest {

    @Test
    void generateInsertQuery() {
        String query = SqlHelper.generateInsertQuery("lw_forum_topic", "topic_id, group_id, topic_title, user_id, topic_time");
        assertEquals("INSERT INTO lw_forum_topic (topic_id,group_id,topic_title,user_id,topic_time) VALUES (?,?,?,?,?) ON DUPLICATE KEY "
            + "UPDATE group_id=VALUES(group_id),topic_title=VALUES(topic_title),user_id=VALUES(user_id),topic_time=VALUES(topic_time)", query);
    }
}