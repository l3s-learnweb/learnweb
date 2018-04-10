package de.l3s.learnweb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.glossary.GlossaryItems;
import de.l3s.glossary.LanguageItem;

public class GlossaryManager
{
    private final static Logger log = Logger.getLogger(GlossaryManager.class);
    private Learnweb learnweb;

    public GlossaryManager(Learnweb learnweb)
    {
        this.learnweb = learnweb;
    }

    //Insert 1st Language and 2nd Language terms related to Glossary entry
    public void InsertTerms(GlossaryEntry e) // TODO java naming conventions
    {
        //TODO:: change table names
        String InsertTerms = "INSERT INTO `lw_resource_glossary_terms_copy`(`glossary_id`, `term`, `use`, `pronounciation`, `acronym`, `references`, `phraseology`, `language`, `deleted`, `user_id`) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement preparedStmnt = null;

        try
        {
            String[] languagePair = getLanguagePairs(e.getResourceId());

            for(LanguageItem t : e.getFirstLanguageItems())
            {
                preparedStmnt = learnweb.getConnection().prepareStatement(InsertTerms); // TODO define outside loop
                preparedStmnt.setInt(1, e.getGlossaryId());
                preparedStmnt.setString(2, t.getValue());
                preparedStmnt.setString(3, String.join(", ", t.getSelectedUses()));
                preparedStmnt.setString(4, t.getPronounciation());
                preparedStmnt.setString(5, t.getAcronym());
                preparedStmnt.setString(6, t.getReferences());
                preparedStmnt.setString(7, t.getPhraseology());
                preparedStmnt.setString(8, languagePair[0]);
                preparedStmnt.setInt(9, 0);
                preparedStmnt.setInt(10, e.getUserId());
                preparedStmnt.executeQuery();

            }
            // TODO :: Combine both?? YES!!! use languaeitem as parameter of this method and call it once for the first and once for the second langauge
            for(LanguageItem t : e.getSecondLanguageItems())
            {
                preparedStmnt = learnweb.getConnection().prepareStatement(InsertTerms); // TODO define outside loop
                preparedStmnt.setInt(1, e.getGlossaryId());
                preparedStmnt.setString(2, t.getValue());
                preparedStmnt.setString(3, String.join(", ", t.getSelectedUses()));
                preparedStmnt.setString(4, t.getPronounciation());
                preparedStmnt.setString(5, t.getAcronym());
                preparedStmnt.setString(6, t.getReferences());
                preparedStmnt.setString(7, t.getPhraseology());
                preparedStmnt.setString(8, languagePair[1]);
                preparedStmnt.setInt(9, 0);
                preparedStmnt.setInt(10, e.getUser().getId());
                preparedStmnt.executeQuery();

            }
        }
        catch(SQLException e1)
        {
            log.error(e1);
        }
    }

    public boolean checkIfExists(int resourceId)
    {
        boolean deleted = false;
        String checkIfGlossaryDeleted = "SELECT `deleted` FROM `lw_resource` WHERE `resource_id`=?";

        try
        {
            PreparedStatement ps = learnweb.getConnection().prepareStatement(checkIfGlossaryDeleted);
            ps.setInt(1, resourceId);
            ResultSet result = ps.executeQuery();
            if(result.next())
                deleted = result.getBoolean("deleted");

        }
        catch(SQLException e)
        {
            log.error("Error in fetching deleted for glossary resource: " + resourceId, e);
        }
        return deleted;
    }

    public void copyGlossary(int oldResourceId, int newResourceId)
    {

        PreparedStatement ps = null;
        try
        {
            //copy main languages
            String getOldLanguagePair = "SELECT * FROM `lw_resource_glossary_main` WHERE `resource_id`=?";
            ps = learnweb.getConnection().prepareStatement(getOldLanguagePair);
            ps.setInt(1, oldResourceId);
            ResultSet langPair = ps.executeQuery();
            if(langPair.next())
            {
                String insertLang = "INSERT INTO `lw_resource_glossary_main`(`resource_id`, `language_one`, `language_two`) VALUES (?,?,?)";
                ps = learnweb.getConnection().prepareStatement(insertLang);
                ps.setInt(1, newResourceId);
                ps.setString(2, langPair.getString("language_one"));
                ps.setString(3, langPair.getString("language_two"));
                ps.executeUpdate();
            }
            //Get glossary IDs
            ps = null;
            ps = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_resource_glossary_copy` WHERE resource_id=?");
            ps.setInt(1, oldResourceId);
            ResultSet oldGlossary = ps.executeQuery();
            while(oldGlossary.next())
            {
                //Get glossary details
                int oldGlossId = oldGlossary.getInt("glossary_id");
                int newGlossId = 0;
                ps = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_glossary_details` WHERE `glossary_id`=?");
                ps.setInt(1, oldGlossId);
                ResultSet oldData = ps.executeQuery();
                if(oldData.next())
                {
                    //insert glossary details
                    ps = learnweb.getConnection().prepareStatement("INSERT INTO `lw_glossary_details`( `topic_1`, `topic_2`, `topic_3`, `description`) VALUES (?,?,?,?)", PreparedStatement.RETURN_GENERATED_KEYS);

                    ps.setString(1, oldData.getString("topic_1"));
                    ps.setString(2, oldData.getString("topic_2"));
                    ps.setString(3, oldData.getString("topic_3"));
                    ps.setString(4, oldData.getString("description"));
                    ps.executeUpdate();
                    ResultSet newGlossary = ps.getGeneratedKeys();
                    newGlossary.next();
                    newGlossId = newGlossary.getInt(1); //new glossary id for a copied resource
                    //insert glossary ID and resource ID
                    ps = learnweb.getConnection().prepareStatement("INSERT INTO `lw_resource_glossary_copy`(`deleted`, `resource_id`, `glossary_id`) VALUES (?,?,?)");
                    ps.setBoolean(1, oldGlossary.getBoolean("deleted"));
                    ps.setInt(2, newResourceId);
                    ps.setInt(3, newGlossId);
                    ps.executeUpdate();

                }
                //insert terms
                if(newGlossId > 0)
                {
                    ps = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_resource_glossary_terms_copy` WHERE `glossary_id`=?");
                    ps.setInt(1, oldGlossId);
                    ResultSet terms = ps.executeQuery();
                    while(terms.next())
                    {
                        PreparedStatement preparedStmnt = learnweb.getConnection()
                                .prepareStatement("INSERT INTO `lw_resource_glossary_terms_copy`(`glossary_id`, `term`, `use`, `pronounciation`, `acronym`, `references`, `phraseology`, `language`, `deleted`, `user_id`) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

                        preparedStmnt.setInt(1, newGlossId);
                        preparedStmnt.setString(2, terms.getString("term"));
                        preparedStmnt.setString(3, terms.getString("use"));
                        preparedStmnt.setString(4, terms.getString("pronounciation"));
                        preparedStmnt.setString(5, terms.getString("acronym"));
                        preparedStmnt.setString(6, terms.getString("references"));
                        preparedStmnt.setString(7, terms.getString("phraseology"));
                        preparedStmnt.setString(8, terms.getString("language"));
                        preparedStmnt.setBoolean(9, terms.getBoolean("deleted"));
                        preparedStmnt.setInt(10, terms.getInt("user_id"));
                        preparedStmnt.executeQuery();

                    }
                }
            }
        }
        catch(SQLException e1)
        {

            log.error("Error in copying glossary entry for resource id: " + newResourceId + "from old resource: " + oldResourceId, e1);
        }
    }

    //Insert main glossary Entry for a resource id
    public boolean addToDatabase(GlossaryEntry e)
    {
        boolean executeResult = true;
        if(e.getGlossaryId() == 0)
        {

            PreparedStatement preparedStmnt = null;

            try
            {

                //TODO:: Change the table names from _copy
                //Add glossary details and generate glossary ID
                preparedStmnt = learnweb.getConnection().prepareStatement("INSERT INTO `lw_glossary_details`(`topic_1`, `topic_2`, `topic_3`, `description`) VALUES (?,?,?,?)", PreparedStatement.RETURN_GENERATED_KEYS);
                preparedStmnt.setString(1, e.getSelectedTopicOne());
                preparedStmnt.setString(2, e.getSelectedTopicTwo());
                preparedStmnt.setString(3, e.getSelectedTopicThree());
                preparedStmnt.setString(4, e.getDescription());
                preparedStmnt.executeUpdate();
                ResultSet glossEntry = preparedStmnt.getGeneratedKeys();
                glossEntry.next();
                e.setGlossaryId(glossEntry.getInt(1));

                //Add glossary ID and resource ID
                preparedStmnt = learnweb.getConnection().prepareStatement("INSERT INTO `lw_resource_glossary_copy`( `resource_id`, `glossary_id`) VALUES (?, ?)");
                preparedStmnt.setInt(1, e.getResourceId());
                preparedStmnt.setInt(2, e.getGlossaryId());
                preparedStmnt.executeQuery();

                //Insert terms
                InsertTerms(e);

            }
            catch(SQLException e1)
            {
                executeResult = false;
                log.error("Error in adding glossary entry for resource id: " + e.getResourceId(), e1);
            }

            return executeResult;

        }
        else if(e.getGlossaryId() > 0)
        {
            int glossIden = e.getGlossaryId();
            String updateStmnt = "UPDATE `lw_glossary_details` SET `topic_1`= ? ,`topic_2`= ?,`topic_3`= ?,`description`= ? WHERE glossary_id = " + Integer.toString(glossIden); // TODO set glossary id through pstmt
            try
            {
                PreparedStatement pstmnt = learnweb.getConnection().prepareStatement(updateStmnt);
                pstmnt.setString(1, e.getSelectedTopicOne());
                pstmnt.setString(2, e.getSelectedTopicTwo());
                pstmnt.setString(3, e.getSelectedTopicThree());
                pstmnt.setString(4, e.getDescription());
                pstmnt.executeQuery();

                List<LanguageItem> newUkItems = new ArrayList<LanguageItem>(e.getFirstLanguageItems());

                List<LanguageItem> newItItems = new ArrayList<LanguageItem>(e.getSecondLanguageItems());
                pstmnt = null;
                pstmnt = learnweb.getConnection().prepareStatement("SELECT glossary_term_id FROM lw_resource_glossary_terms_copy WHERE glossary_id = " + Integer.toString(glossIden));
                ResultSet rs = pstmnt.executeQuery();
                while(rs.next())
                {
                    boolean deleteTerm = false;
                    for(LanguageItem u : e.getFirstLanguageItems())
                    {
                        if(rs.getInt("glossary_term_id") == u.getTermId())
                        {
                            deleteTerm = true;
                            break;
                        }
                    }
                    if(deleteTerm == false)
                    {
                        for(LanguageItem iItems : e.getSecondLanguageItems())
                        {
                            if(rs.getInt("glossary_term_id") == iItems.getTermId())
                            {
                                deleteTerm = true;
                                break;
                            }
                        }
                    }
                    if(deleteTerm != true)
                    {
                        String delete = "UPDATE `lw_resource_glossary_terms_copy` SET `deleted`= ? WHERE glossary_term_id = ? ";

                        PreparedStatement pd = learnweb.getConnection().prepareStatement(delete);
                        pd.setInt(1, 1);
                        pd.setInt(2, rs.getInt("glossary_term_id"));
                        pd.executeUpdate();
                    }
                }

                for(LanguageItem u : e.getFirstLanguageItems())
                {
                    PreparedStatement preparedStmnt = null;
                    if(u.getTermId() > 0)
                    {
                        preparedStmnt = learnweb.getConnection().prepareStatement("UPDATE `lw_resource_glossary_terms_copy` SET `term`= ? ,`use`= ? ,`pronounciation`= ? ,`acronym`= ? ,`references`= ? ,`phraseology`= ? ,`language`= ?  WHERE `glossary_term_id` = ?");

                        preparedStmnt.setString(1, u.getValue());
                        String use = String.join(", ", u.getSelectedUses());
                        preparedStmnt.setString(2, use);
                        preparedStmnt.setString(3, u.getPronounciation());
                        preparedStmnt.setString(4, u.getAcronym());
                        preparedStmnt.setString(5, u.getReferences());
                        preparedStmnt.setString(6, u.getPhraseology());
                        preparedStmnt.setString(7, u.getLanguage().toString());
                        preparedStmnt.setInt(8, u.getTermId());
                        preparedStmnt.executeQuery();

                        newUkItems.remove(u);

                    }
                }
                //TODO:: Combine both?
                for(LanguageItem u : e.getSecondLanguageItems())
                {
                    PreparedStatement preparedStmnt = null;
                    if(u.getTermId() > 0)
                    {
                        preparedStmnt = learnweb.getConnection().prepareStatement("UPDATE `lw_resource_glossary_terms_copy` SET `term`= ? ,`use`= ? ,`pronounciation`= ? ,`acronym`= ? ,`references`= ? ,`phraseology`= ? ,`language`= ?  WHERE `glossary_term_id` = ?");

                        preparedStmnt.setString(1, u.getValue());
                        String use = String.join(", ", u.getSelectedUses());
                        preparedStmnt.setString(2, use);
                        preparedStmnt.setString(3, u.getPronounciation());
                        preparedStmnt.setString(4, u.getAcronym());
                        preparedStmnt.setString(5, u.getReferences());
                        preparedStmnt.setString(6, u.getPhraseology());
                        preparedStmnt.setString(7, u.getLanguage().toString());
                        preparedStmnt.setInt(8, u.getTermId());
                        preparedStmnt.executeQuery();
                        newItItems.remove(u);

                    }
                }
                e.setSecondLanguageItems(newItItems);
                e.setFirstLanguageItems(newUkItems);
                InsertTerms(e);
            }
            catch(SQLException e1)
            {
                executeResult = false;
                log.error(e1);
            }

            return executeResult;

        }
        return false;

    }

    public int getEntryCount(int resourceId)
    {
        //TODO:: change table name
        int entryCount = 0;
        String getCount = "SELECT count(*) as entry FROM `lw_resource_glossary_copy` WHERE `resource_id`=? and deleted=0";
        try
        {
            PreparedStatement ps = learnweb.getConnection().prepareStatement(getCount);
            ps.setInt(1, resourceId);
            ResultSet result = ps.executeQuery();
            if(result.next())
                entryCount = result.getInt("entry");
        }
        catch(SQLException e)
        {
            log.error("Error in fetching entry count for glossary with resource id: " + resourceId, e);
        }

        return entryCount;
    }

    public void deleteFromDb(int glossId)
    {
        //TODO:: change table names
        String deleteGlossItem = "UPDATE `lw_resource_glossary_copy` SET `deleted`= ? WHERE glossary_id = ? ";

        try
        {

            PreparedStatement terms = learnweb.getConnection().prepareStatement("UPDATE `lw_resource_glossary_terms_copy` SET `deleted`= ? WHERE glossary_id = ? ");
            terms.setInt(1, 1);
            terms.setInt(2, glossId);
            terms.executeUpdate();

            PreparedStatement gloss = learnweb.getConnection().prepareStatement(deleteGlossItem);
            gloss.setInt(1, 1);
            gloss.setInt(2, glossId);
            gloss.executeUpdate();

        }
        catch(SQLException e)
        {
            log.error(e);
        }

    }

    public void setLanguagePairs(int resourceId, String primaryLang, String secondaryLang) throws SQLException
    {
        String insertLanguagePairs = "INSERT INTO `lw_resource_glossary_main`(`resource_id`, `language_one`, `language_two`) VALUES (?,?,?)";
        PreparedStatement ps = learnweb.getConnection().prepareStatement(insertLanguagePairs);
        ps.setInt(1, resourceId);
        ps.setString(2, primaryLang);
        ps.setString(3, secondaryLang);
        ps.executeUpdate();
    }

    public String[] getLanguagePairs(int resourceId) throws SQLException
    {
        PreparedStatement ps = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_resource_glossary_main` WHERE `resource_id`=?");
        ps.setInt(1, resourceId);
        ResultSet result = ps.executeQuery();

        String langPair[] = new String[2];
        if(result.next())
        {
            langPair[0] = result.getString("language_one"); // primary Language
            langPair[1] = result.getString("language_two"); // secondary Language
        }
        ps.close();

        return langPair;
    }

    public List<GlossaryItems> getGlossaryItems(int id)
    {
        List<GlossaryItems> items = new ArrayList<GlossaryItems>();

        //TODO:: change db names
        PreparedStatement preparedStmnt = null;
        ResultSet resultID = null;
        String primaryLanguage = "";
        String secondaryLanguage = "";

        try
        {
            String[] langPair = getLanguagePairs(id);

            primaryLanguage = langPair[0];
            secondaryLanguage = langPair[1];
            String mainDetails = "SELECT * FROM `lw_resource_glossary_copy` WHERE `resource_id` = ? AND `deleted`= ?";
            preparedStmnt = learnweb.getConnection().prepareStatement(mainDetails);
            preparedStmnt.setInt(1, id);
            preparedStmnt.setInt(2, 0);
            resultID = preparedStmnt.executeQuery();

            while(resultID.next())
            {

                List<LanguageItem> finalList = new ArrayList<LanguageItem>();

                int glossaryId = resultID.getInt("glossary_id");
                //get Glossary details
                PreparedStatement pstmnt = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_glossary_details` WHERE `glossary_id`=?"); // TODO define outside loop
                pstmnt.setInt(1, glossaryId);
                ResultSet result = pstmnt.executeQuery();
                result.next();
                //fetch terms
                PreparedStatement ps = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_resource_glossary_terms_copy` WHERE `glossary_id` = ? AND `deleted`= ? order by(`language`)"); // TODO define outside loop
                ps.setInt(1, glossaryId);
                ps.setInt(2, 0);
                ResultSet termResults = ps.executeQuery();
                ResultSet termTime = ps.executeQuery(); //for timestamp
                Timestamp latestTimestamp = getLatestTimestamp(glossaryId, resultID.getTimestamp("timestamp"), termTime);
                String primaryLangTerm = null;
                while(termResults.next())
                {

                    GlossaryItems gloss = new GlossaryItems();

                    gloss.setGlossId(glossaryId);

                    gloss.setTopic_1(result.getString("topic_1"));
                    gloss.setTopic_2(result.getString("topic_2"));
                    gloss.setTopic_3(result.getString("topic_3"));
                    gloss.setDescription(result.getString("description"));
                    gloss.setAcronym(termResults.getString("acronym"));
                    gloss.setValue(termResults.getString("term"));
                    gloss.setPhraseology(termResults.getString("phraseology"));
                    gloss.setPronounciation(termResults.getString("pronounciation"));
                    gloss.setReferences(termResults.getString("references"));
                    gloss.setTermId(termResults.getInt("glossary_term_id"));
                    gloss.setSelectedUses(termResults.getString("use"));

                    gloss.setDate(latestTimestamp);

                    if(termResults.getString("language").equals(primaryLanguage))
                    {
                        gloss.setLanguage(primaryLanguage);
                        primaryLangTerm = termResults.getString("term"); // for ordering terms
                    }
                    else
                        gloss.setLanguage(secondaryLanguage);
                    gloss.setPrimaryLanguageTerm(primaryLangTerm);

                    LanguageItem it = new LanguageItem(); // TODO "it" name
                    gloss.setPrimaryLanguage(it.getEnum(primaryLanguage));
                    gloss.setSecondaryLanguage(it.getEnum(secondaryLanguage));
                    it.setAcronym(termResults.getString("acronym"));
                    it.setValue(termResults.getString("term"));
                    it.setPhraseology(termResults.getString("phraseology"));
                    it.setPronounciation(termResults.getString("pronounciation"));
                    it.setReferences(termResults.getString("references"));
                    it.setTermId(termResults.getInt("glossary_term_id"));
                    List<String> setUse = new ArrayList<String>(); // TODO improve
                    if(termResults.getString("use").contains(","))
                        setUse = Arrays.asList(termResults.getString("use").split(", "));
                    else
                        setUse.add(termResults.getString("use").trim());
                    it.setSelectedUses(setUse);
                    if(termResults.getString("language").equals(primaryLanguage)) // TODO improve
                        it.setLanguage(it.getEnum(primaryLanguage));
                    else
                        it.setLanguage(it.getEnum(secondaryLanguage));
                    finalList.add(it);
                    gloss.setFinalItems(finalList);
                    items.add(gloss);

                }

            }

        }
        catch(SQLException e)
        {
            log.error(e);
        }
        return items;
    }

    private Timestamp getLatestTimestamp(int glossaryId, Timestamp timestamp, ResultSet termTime)
    {
        Timestamp finalTime = timestamp;
        try
        {
            while(termTime.next())
            {
                if(finalTime.before(termTime.getTimestamp("timestamp")))
                    finalTime = termTime.getTimestamp("timestamp");
            }
        }
        catch(SQLException e)
        {
            log.error(e);
        }

        return finalTime;
    }

}
