package de.l3s.learnweb;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

@Deprecated
public class glossaryfixes
{
    //TODO:: DELETE THIS CLASS
    private static Connection dbConnection;

    public static Connection getConnection() throws SQLException
    {
        checkConnection();

        return dbConnection;
    }

    private static long lastCheck = 0L;

    private static void checkConnection() throws SQLException
    {
        // exit if last check was two or less seconds ago
        if(lastCheck > System.currentTimeMillis() - 2000)
            return;

        if(!dbConnection.isValid(1))
        {

            try
            {
                dbConnection.close();
            }
            catch(SQLException e)
            {
            }

            connect();
        }

        lastCheck = System.currentTimeMillis();
    }

    private static void connect() throws SQLException
    {
        // ?useUnicode=true
        dbConnection = DriverManager.getConnection("jdbc:mysql://localhost/learnweb_main" + "?log=false", "learnweb", "***REMOVED***");
        dbConnection.createStatement().execute("SET @@SQL_MODE = REPLACE(@@SQL_MODE, 'ONLY_FULL_GROUP_BY', '')");

        //pstmtLog = dbConnection.prepareStatement("INSERT DELAYED INTO `lw_user_log` (`user_id`, `session_id`, `action`, `target_id`, `params`, `group_id`, timestamp, execution_time, client_version) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 2)");

    }

    private static Learnweb lw;

    public static void main(String[] args)
    {
        try
        {
            try
            {
                lw = Learnweb.createInstance("http://learnweb.l3s.uni-hannover.de");
            }
            catch(ClassNotFoundException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            //connect();

            PreparedStatement ps = lw.getConnection().prepareStatement("SELECT * FROM `lw_resource_glossary` where resource_id != 0");
            ResultSet rs = ps.executeQuery();
            while(rs.next())
            {
                int resourceId = rs.getInt("resource_id");
                boolean deleted = rs.getBoolean("deleted");
                int glossaryId = rs.getInt("glossary_id");
                String topicOne = rs.getString("topic_1");
                String topicTwo = rs.getString("topic_2");
                String topicThree = rs.getString("topic_3");
                String description = rs.getString("description");
                Timestamp timestamp = rs.getTimestamp("timestamp");
                int newGlossaryId = 0;

                PreparedStatement ins = lw.getConnection().prepareStatement("INSERT INTO `lw_glossary_details`(`topic_1`, `topic_2`, `topic_3`, `description`) VALUES (?,?,?,?)", PreparedStatement.RETURN_GENERATED_KEYS);
                ins.setString(1, topicOne);
                ins.setString(2, topicTwo);
                ins.setString(3, topicThree);
                ins.setString(4, description);
                ins.executeUpdate();
                ResultSet gloss = ins.getGeneratedKeys();
                gloss.next();
                newGlossaryId = gloss.getInt(1);

                ins = lw.getConnection().prepareStatement("INSERT INTO `lw_resource_glossary_copy`(`deleted`, `resource_id`, `glossary_id`, `timestamp`) VALUES (?,?,?,?)");
                ins.setBoolean(1, deleted);
                ins.setInt(2, resourceId);
                ins.setInt(3, newGlossaryId);
                ins.setTimestamp(4, timestamp);
                ins.executeQuery();

                PreparedStatement term = lw.getConnection().prepareStatement("SELECT * FROM `lw_resource_glossary_terms` WHERE `glossary_id`=?");
                term.setInt(1, glossaryId);
                ResultSet terms = term.executeQuery();
                while(terms.next())
                {
                    boolean deletedT = terms.getBoolean("deleted");

                    int userId = terms.getInt("user_id");
                    String termValue = terms.getString("term");
                    String use = terms.getString("use");
                    String pronounciation = terms.getString("pronounciation");
                    String acronym = terms.getString("acronym");
                    String references = terms.getString("references");
                    String phraseology = terms.getString("phraseology");
                    String language = terms.getString("language");
                    Timestamp timestampT = terms.getTimestamp("timestamp");
                    PreparedStatement insTerm = lw.getConnection().prepareStatement(
                            "INSERT INTO `lw_resource_glossary_terms_copy`(`deleted`, `user_id`, `glossary_id`, `term`, `use`, `pronounciation`, `acronym`, `references`, `phraseology`, `language`, `timestamp`) VALUES (?,?,?,?,?,?,?,?,?,?,?)");

                    insTerm.setBoolean(1, deletedT);
                    insTerm.setInt(2, userId);
                    insTerm.setInt(3, newGlossaryId);
                    insTerm.setString(4, termValue);
                    insTerm.setString(5, use);
                    insTerm.setString(6, pronounciation);
                    insTerm.setString(7, acronym);
                    insTerm.setString(8, references);
                    insTerm.setString(9, phraseology);
                    insTerm.setString(10, language);
                    insTerm.setTimestamp(11, timestampT);
                    insTerm.executeQuery();
                }

            }

        }
        catch(SQLException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println("Ended");
    }

}
