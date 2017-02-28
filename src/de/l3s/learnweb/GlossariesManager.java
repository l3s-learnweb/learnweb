package de.l3s.learnweb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.glossary.GlossaryItems;
import de.l3s.glossary.LanguageItem;
import de.l3s.learnweb.solrClient.SolrClient;

public class GlossariesManager
{
    private final static Logger log = Logger.getLogger(GlossariesManager.class);
    private Learnweb learnweb;

    public GlossariesManager(Learnweb learnweb)
    {
        this.learnweb = learnweb;
    }

    //Insert Italian and Uk terms related to Glossary entry
    public void InsertTerms(GlossaryEntry e)
    {
        String InsertTerms = "INSERT INTO `lw_resource_glossary_terms`(`glossary_id`, `term`, `use`, `pronounciation`, `acronym`, `references`, `phraseology`, `language`) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement preparedStmnt = null;
        try
        {
            preparedStmnt = Learnweb.getInstance().getConnection().prepareStatement(InsertTerms);
        }
        catch(SQLException e1)
        {
            log.error(e1);
        }
        try
        {
            for(LanguageItem t : e.getUkItems())
            {
                preparedStmnt = Learnweb.getInstance().getConnection().prepareStatement(InsertTerms);
                preparedStmnt.setInt(1, e.getGlossaryId());
                preparedStmnt.setString(2, t.getValue());
                preparedStmnt.setString(3, String.join(", ", t.getSelectedUses()));
                preparedStmnt.setString(4, t.getPronounciation());
                preparedStmnt.setString(5, t.getAcronym());
                preparedStmnt.setString(6, t.getReferences());
                preparedStmnt.setString(7, t.getPhraseology());
                preparedStmnt.setString(8, "English-uk");
                preparedStmnt.executeQuery();

            }
            for(LanguageItem t : e.getItalianItems())
            {
                preparedStmnt = Learnweb.getInstance().getConnection().prepareStatement(InsertTerms);
                preparedStmnt.setInt(1, e.getGlossaryId());
                preparedStmnt.setString(2, t.getValue());
                preparedStmnt.setString(3, String.join(", ", t.getSelectedUses()));
                preparedStmnt.setString(4, t.getPronounciation());
                preparedStmnt.setString(5, t.getAcronym());
                preparedStmnt.setString(6, t.getReferences());
                preparedStmnt.setString(7, t.getPhraseology());
                preparedStmnt.setString(8, "Italian-it");
                preparedStmnt.executeQuery();

            }
        }
        catch(SQLException e1)
        {
            log.error(e1);
        }
    }

    //Insert main glossary Entry for a resource id
    public Resource addToDatabase(GlossaryEntry e)
    {
        if(e.getGlossaryId() == 0)
        {
            String InsertGlossary = "INSERT INTO `lw_resource_glossary`(`resource_id`, `topic_1`, `topic_2`, `topic_3`, `description`) VALUES ( ?, ?, ?, ?, ?)";

            PreparedStatement preparedStmnt = null;

            try
            {
                preparedStmnt = Learnweb.getInstance().getConnection().prepareStatement(InsertGlossary, PreparedStatement.RETURN_GENERATED_KEYS);

                preparedStmnt.setInt(1, e.getResourceId());
                preparedStmnt.setString(2, e.getSelectedTopicOne());
                preparedStmnt.setString(3, e.getSelectedTopicTwo());
                preparedStmnt.setString(4, e.getSelectedTopicThree());
                preparedStmnt.setString(5, e.getDescription());
                preparedStmnt.executeQuery();
                ResultSet keys = preparedStmnt.getGeneratedKeys();
                keys.next();
                int glossaryId = keys.getInt(1);
                e.setGlossaryId(glossaryId);
                InsertTerms(e);
            }
            catch(SQLException e1)
            {

                log.error("Error in adding glossary entry for resource id: " + e.getResourceId(), e1);
            }

            //            ResourcePreviewMaker rpm = learnweb.getResourcePreviewMaker();
            SolrClient solr = learnweb.getSolrClient();
            ResourceManager resourceManager = learnweb.getResourceManager();
            //           int learnwebResourceId = 0;
            Resource glossItem = new Resource();
            try
            {
                glossItem = resourceManager.getResource(e.getResourceId());
            }
            catch(SQLException e1)
            {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            return glossItem;

        }
        else if(e.getGlossaryId() > 0)
        {
            int glossIden = e.getGlossaryId();
            String updateStmnt = "UPDATE `lw_resource_glossary` SET `topic_1`= ? ,`topic_2`= ?,`topic_3`= ?,`description`= ? WHERE glossary_id = " + Integer.toString(glossIden);
            try
            {
                PreparedStatement pstmnt = Learnweb.getInstance().getConnection().prepareStatement(updateStmnt);
                pstmnt.setString(1, e.getSelectedTopicOne());
                pstmnt.setString(2, e.getSelectedTopicTwo());
                pstmnt.setString(3, e.getSelectedTopicThree());
                pstmnt.setString(4, e.getDescription());
                pstmnt.executeQuery();
                String deleteTerms = "SELECT glossary_term_id FROM lw_resource_glossary_terms WHERE glossary_id = " + Integer.toString(glossIden);
                String updateTerms = "UPDATE `lw_resource_glossary_terms` SET `term`= ? ,`use`= ? ,`pronounciation`= ? ,`acronym`= ? ,`references`= ? ,`phraseology`= ? ,`language`= ?  WHERE `glossary_term_id` = ?";
                List<LanguageItem> newUkItems = new ArrayList<LanguageItem>(e.getUkItems());

                List<LanguageItem> newItItems = new ArrayList<LanguageItem>(e.getItalianItems());
                pstmnt = null;
                pstmnt = Learnweb.getInstance().getConnection().prepareStatement(deleteTerms);
                ResultSet rs = pstmnt.executeQuery();
                while(rs.next())
                {
                    boolean deleteTerm = false;
                    for(LanguageItem u : e.getUkItems())
                    {
                        if(rs.getInt("glossary_term_id") == u.getTermId())
                        {
                            deleteTerm = true;
                            break;
                        }
                    }
                    if(deleteTerm == false)
                    {
                        for(LanguageItem iItems : e.getItalianItems())
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
                        String delete = "DELETE FROM `lw_resource_glossary_terms` WHERE glossary_term_id = " + Integer.toString(rs.getInt("glossary_term_id"));
                        PreparedStatement pd = Learnweb.getInstance().getConnection().prepareStatement(delete);
                        pd.executeUpdate();
                    }
                }

                for(LanguageItem u : e.getUkItems())
                {
                    PreparedStatement preparedStmnt = null;
                    if(u.getTermId() > 0)
                    {
                        preparedStmnt = Learnweb.getInstance().getConnection().prepareStatement(updateTerms);

                        preparedStmnt.setString(1, u.getValue());
                        String use = String.join(", ", u.getSelectedUses());
                        preparedStmnt.setString(2, use);
                        preparedStmnt.setString(3, u.getPronounciation());
                        preparedStmnt.setString(4, u.getAcronym());
                        preparedStmnt.setString(5, u.getReferences());
                        preparedStmnt.setString(6, u.getPhraseology());
                        preparedStmnt.setString(7, "English-uk");
                        preparedStmnt.setInt(8, u.getTermId());
                        preparedStmnt.executeQuery();

                        newUkItems.remove(u);

                    }
                }
                for(LanguageItem u : e.getItalianItems())
                {
                    PreparedStatement preparedStmnt = null;
                    if(u.getTermId() > 0)
                    {
                        preparedStmnt = Learnweb.getInstance().getConnection().prepareStatement(updateTerms);

                        preparedStmnt.setString(1, u.getValue());
                        String use = String.join(", ", u.getSelectedUses());
                        preparedStmnt.setString(2, use);
                        preparedStmnt.setString(3, u.getPronounciation());
                        preparedStmnt.setString(4, u.getAcronym());
                        preparedStmnt.setString(5, u.getReferences());
                        preparedStmnt.setString(6, u.getPhraseology());
                        preparedStmnt.setString(7, "Italian-it");
                        preparedStmnt.setInt(8, u.getTermId());
                        preparedStmnt.executeQuery();
                        newItItems.remove(u);

                    }
                }
                e.setItalianItems(newItItems);
                e.setUkItems(newUkItems);
                InsertTerms(e);
            }
            catch(SQLException e1)
            {
                log.error(e1);
            }
            SolrClient solr = learnweb.getSolrClient();
            ResourceManager resourceManager = learnweb.getResourceManager();
            Resource glossItem = new Resource();
            try
            {
                glossItem = resourceManager.getResource(e.getResourceId());
            }
            catch(SQLException e1)
            {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            return glossItem;

        }
        return null;

    }

    public void deleteFromDb(int glossId)
    {
        String deleteTerms = "DELETE FROM `lw_resource_glossary_terms` WHERE glossary_id = " + Integer.toString(glossId);
        String deleteGlossItem = "DELETE FROM `lw_resource_glossary` WHERE glossary_id = " + Integer.toString(glossId);
        try
        {
            PreparedStatement Terms = Learnweb.getInstance().getConnection().prepareStatement(deleteTerms);
            Terms.executeUpdate();
            PreparedStatement gloss = Learnweb.getInstance().getConnection().prepareStatement(deleteGlossItem);
            gloss.executeUpdate();
        }
        catch(SQLException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public List<GlossaryItems> getGlossaryItems(int id)
    {
        List<GlossaryItems> items = new ArrayList<GlossaryItems>();
        String mainDetails = "SELECT * FROM `lw_resource_glossary` WHERE `resource_id` = ?";
        String termDetails = "SELECT * FROM `lw_resource_glossary_terms` WHERE `glossary_id` = ?";
        PreparedStatement preparedStmnt = null;
        ResultSet result = null;

        try
        {
            preparedStmnt = Learnweb.getInstance().getConnection().prepareStatement(mainDetails);
            preparedStmnt.setInt(1, id);
            result = preparedStmnt.executeQuery();
            while(result.next())
            {
                List<LanguageItem> finalList = new ArrayList<LanguageItem>();

                int glossaryId = result.getInt("glossary_id");
                PreparedStatement ps = Learnweb.getInstance().getConnection().prepareStatement(termDetails);
                ps.setInt(1, glossaryId);
                ResultSet termResults = ps.executeQuery();

                while(termResults.next())
                {

                    GlossaryItems gloss = new GlossaryItems();
                    gloss.setGlossId(result.getInt("glossary_id"));
                    gloss.setGlossIdString("_" + Integer.toString(result.getInt("glossary_id")));

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

                    if(termResults.getString("language").contains("uk"))
                        gloss.setLanguage("English");
                    else
                        gloss.setLanguage("Italian");

                    LanguageItem uk = new LanguageItem();
                    uk.setAcronym(termResults.getString("acronym"));
                    uk.setValue(termResults.getString("term"));
                    uk.setPhraseology(termResults.getString("phraseology"));
                    uk.setPronounciation(termResults.getString("pronounciation"));
                    uk.setReferences(termResults.getString("references"));
                    uk.setTermId(termResults.getInt("glossary_term_id"));
                    List<String> setUse = new ArrayList<String>();
                    if(termResults.getString("use").contains(","))
                        setUse = Arrays.asList(termResults.getString("use").split(", "));
                    else
                        setUse.add(termResults.getString("use").trim());
                    uk.setSelectedUses(setUse);
                    if(termResults.getString("language").contains("uk"))
                        uk.setLanguage("English");
                    else
                        uk.setLanguage("Italian");
                    finalList.add(uk);
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

}
