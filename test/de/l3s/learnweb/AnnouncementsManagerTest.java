package de.l3s.learnweb;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class AnnouncementsManagerTest
{
    private Learnweb learnweb = Learnweb.createInstance();

    AnnouncementsManagerTest() throws SQLException, ClassNotFoundException {}


    @Test
    void getAnnouncementById() throws SQLException
    {
        Announcement expected = new Announcement();
        expected.setId(113);
        expected.setTitle("ICWL conference, Magdeburg, Germany");
        expected.setText("<p>MSc. Tetiana Tolmachova presented the full paper \"Visualizing Search History in Web Learning\" at the 18th International Conference on Web-based Learning (ICWL2019). The paper describes the LogCanvasTag interface for search history visualization, which supports users in re-constructing the semantic relationship between their search activities. Experimental results indicate that searching experience of both independent users and collaborative searching groups beneﬁt from this search history visualization.</p>");

        Announcement result = learnweb.getAnnouncementsManager().getAnnouncementById(expected.getId());

        assertEquals(expected.getId(), result.getId());
        assertEquals(expected.getTitle(), result.getTitle());
        assertEquals(expected.getText(), result.getText());
    }

    @Test
    void getAnnouncementsAll() throws SQLException
    {
        ArrayList<Integer> expected = new ArrayList<>(Arrays.asList(113, 114, 129, 131, 132));
        List<Announcement> result = learnweb.getAnnouncementsManager().getAnnouncementsAll();
        assertTrue(result.stream().allMatch(topic -> expected.contains(topic.getId())));
    }

    @Test
    void delete() throws SQLException
    {
        learnweb.getConnection().setAutoCommit(false);
        try
        {
            Announcement announcement = new Announcement();
            announcement.setId(216);
            learnweb.getAnnouncementsManager().delete((announcement));
            Announcement result = learnweb.getAnnouncementsManager().getAnnouncementById(announcement.getId());
            assertNull(result);
        }
        finally
        {
            learnweb.getConnection().rollback();
            learnweb.getConnection().close();
        }
    }

    @Test
    void insert() throws SQLException
    {
        learnweb.getConnection().setAutoCommit(false);
        try
        {
            Announcement expected = new Announcement();
            expected.setUserId(12502);
            expected.setHidden(false);
            expected.setTitle("Title");
            expected.setText("Text");
            expected.setDate(new Timestamp(System.currentTimeMillis()));

            learnweb.getAnnouncementsManager().save(expected);

            try(PreparedStatement select = learnweb.getConnection().prepareStatement("SELECT * FROM lw_news WHERE user_id = ? AND title = ? and message = ?"))
            {
                select.setInt(1, expected.getUserId());
                select.setString(2, expected.getTitle());
                select.setString(3, expected.getText());

                ResultSet rs = select.executeQuery();

                if(!rs.next())
                    fail();

                assertEquals(expected.getUserId(), rs.getInt("user_id"));
                assertEquals(expected.getTitle(), rs.getString("title"));
                assertEquals(expected.getText(), rs.getString("message"));
            }
        }
        finally
        {
            learnweb.getConnection().rollback();
            learnweb.getConnection().close();
        }
    }

    @Test
    void update() throws SQLException
    {
        learnweb.getConnection().setAutoCommit(false);
        try
        {
            Announcement expected = learnweb.getAnnouncementsManager().getAnnouncementById(113);
            expected.setHidden(false);
            expected.setTitle("Title");
            expected.setText("Text");
            expected.setDate(new Timestamp(System.currentTimeMillis()));

            learnweb.getAnnouncementsManager().save(expected);
            Announcement result = learnweb.getAnnouncementsManager().getAnnouncementById(113);

            assertEquals(expected.getId(), result.getId());
            assertEquals(expected.getTitle(), result.getTitle());
            assertEquals(expected.getText(), result.getText());
        }
        finally
        {
            learnweb.getConnection().rollback();
            learnweb.getConnection().close();
        }
    }

}