package de.l3s.learnweb.forum;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.group.Group;

class ForumManagerTest
{
    private Learnweb learnweb = Learnweb.createInstance();

    ForumManagerTest() throws SQLException, ClassNotFoundException {}

    @Test
    void getTopicsByGroup() throws SQLException
    {
        ArrayList<Integer> expected = new ArrayList<>(Arrays.asList(901, 924, 928, 929, 933, 898, 936, 939));
        List<ForumTopic> result = learnweb.getForumManager().getTopicsByGroup(1463);
        assertTrue(result.stream().allMatch(topic -> expected.contains(topic.getId())));
    }

    @Test
    void getTopicById() throws SQLException
    {
        ForumTopic expected = new ForumTopic();
        expected.setId(936);
        expected.setTitle("test123");
        expected.setLastPostId(9897);
        ForumTopic result = learnweb.getForumManager().getTopicById(expected.getId());
        assertEquals(expected.getId(), result.getId());
        assertEquals(expected.getTitle(), result.getTitle());
        assertEquals(expected.getLastPostId(), result.getLastPostId());
    }

    @Test
    void getPostsBy() throws SQLException
    {
        ArrayList<Integer> expected = new ArrayList<>(Arrays.asList(9898, 9913));
        List<ForumPost> result = learnweb.getForumManager().getPostsBy(939);
        assertTrue(result.stream().allMatch(group -> expected.contains(group.getId())));
    }

    @Test
    void getPostById() throws SQLException
    {
        ForumPost expected = new ForumPost();
        expected.setId(9913);
        expected.setText("<p>123123123</p>");
        ForumPost result = learnweb.getForumManager().getPostById(expected.getId());
        assertEquals(expected.getId(), result.getId());
        assertEquals(expected.getText(), result.getText());
    }

    @Test
    void getPostsByUser() throws SQLException
    {
        ArrayList<Integer> expected = new ArrayList<>(Arrays.asList(9697, 9698, 9699, 9700, 9701, 9705, 9707, 9846, 9847, 9848, 9850, 9851, 9852, 9853, 9854, 9855, 9857, 9862, 9863, 9864, 9865, 9866, 9867, 9868, 9869,
                9870, 9871, 9872, 9874, 9875, 9876, 9877, 9878, 9882, 9883, 9884, 9889, 9890, 9891, 9892, 9893, 9894, 9895, 9896, 9897, 9898, 9899, 9900, 9901, 9907, 9912, 9913, 9914, 9915));
        List<ForumPost> result = learnweb.getForumManager().getPostsByUser(12502);
        assertTrue(result.stream().allMatch(topic -> expected.contains(topic.getId())));
    }

    @Test
    void getPostCountByUser() throws SQLException
    {
        int expected = 54;
        int result = learnweb.getForumManager().getPostCountByUser(12502);
        assertEquals(expected, result);
    }

    @Test
    void saveTopic() throws SQLException
    {
        learnweb.getConnection().setAutoCommit(false);
        try
        {
            ForumTopic expected = learnweb.getForumManager().getTopicById(216);
            expected.setGroupId(1253);
            expected.setTitle("test");
            learnweb.getForumManager().save(expected);
            ForumTopic result = learnweb.getForumManager().getTopicById(216);
            assertEquals(expected.getId(), result.getId());
            assertEquals(expected.getTitle(), result.getTitle());
            assertEquals(expected.getGroupId(), result.getGroupId());
        }
        finally
        {
            learnweb.getConnection().rollback();
            learnweb.getConnection().close();
        }
    }

    @Test
    void deleteTopic() throws SQLException
    {
        learnweb.getConnection().setAutoCommit(false);
        try
        {
            ForumTopic topic = new ForumTopic();
            topic.setId(216);
            learnweb.getForumManager().deleteTopic(topic);
            ForumTopic result = learnweb.getForumManager().getTopicById(topic.getId());
            assertNull(result);
        }
        finally
        {
            learnweb.getConnection().rollback();
            learnweb.getConnection().close();
        }
    }

    @Test
    void savePost() throws SQLException
    {
        learnweb.getConnection().setAutoCommit(false);
        try
        {
            ForumPost expected = learnweb.getForumManager().getPostById(9697);
            expected.setText("test");
            expected.setEditCount(5);
            expected.setUserId(3);
            learnweb.getForumManager().save(expected);
            ForumPost result = learnweb.getForumManager().getPostById(9697);
            assertEquals(expected.getId(), result.getId());
            assertEquals(expected.getEditCount(), result.getEditCount());
            assertEquals(expected.getUserId(), result.getUserId());
        }
        finally
        {
            learnweb.getConnection().rollback();
            learnweb.getConnection().close();
        }
    }

    @Test
    void incViews() throws SQLException
    {
        learnweb.getConnection().setAutoCommit(false);
        try
        {
            int expected = 129;
            learnweb.getForumManager().incViews(386);
            ForumTopic result = learnweb.getForumManager().getTopicById(386);
            assertEquals(expected, result.getViews());
        }
        finally
        {
            learnweb.getConnection().rollback();
            learnweb.getConnection().close();
        }
    }

    @Test
    void deletePost() throws SQLException
    {
        learnweb.getConnection().setAutoCommit(false);
        try
        {
            ForumPost post = new ForumPost();
            post.setId(299);
            learnweb.getForumManager().deletePost(post);
            ForumTopic result = learnweb.getForumManager().getTopicById(post.getId());
            assertNull(result);
        }
        finally
        {
            learnweb.getConnection().rollback();
            learnweb.getConnection().close();
        }
    }
}