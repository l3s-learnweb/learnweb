/**
 *
 */
package de.l3s.learnweb.resource.glossaryNew;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;

/**
 * Merger class for merging old glossary to new
 * Script will print a message upon ending.
 * Script will NOT create new resources(resource_id) while merging old glossary to new.
 * Script will retain old resource_id and generate new entries and terms which matches the schema of new glossary
 * Details like timestamp and user_id have been retained for all variables wherever possible
 * Script will notify with a message upon the end of merge
 *
 * @author Rishita
 *
 */
public class Merger
{

    /**
     *
     * @param args
     */

    public static void main(String[] args)
    {

        // Get * resource with type==glossary
        final Logger log = Logger.getLogger(Merger.class);
        Learnweb learnweb = null;
        try
        {
            learnweb = Learnweb.createInstance("http://learnweb.l3s.uni-hannover.de");
        }
        catch(ClassNotFoundException e1)
        {
            log.error("Error in initializing lw", e1);
            System.exit(0);
        }
        catch(SQLException e1)
        {
            log.error("Error in initializing lw", e1);
            System.exit(0);

        }
        int resourceId = 0;
        //TODO:: To test 1 conversion provide resource_id=xxxx in this sql query
        try(PreparedStatement ps = learnweb.getInstance().getConnection().prepareStatement("SELECT * FROM `lw_resource` WHERE `type`=\"glossary\" and `deleted`=0"))
        {
            ResultSet result = ps.executeQuery();
            while(result.next())
            {
                //Main resource details
                GlossaryResource g = new GlossaryResource();
                resourceId = result.getInt("resource_id");
                g.setId(result.getInt("resource_id"));
                g.setUserId(result.getInt("owner_user_id"));
                try(PreparedStatement lang = learnweb.getInstance().getConnection().prepareStatement("SELECT * FROM `lw_resource_glossary_main` WHERE `resource_id`=?"))
                {
                    lang.setInt(1, result.getInt("resource_id"));
                    ResultSet langResult = lang.executeQuery();
                    List<Locale> languages = new ArrayList<Locale>();
                    if(langResult.next())
                    {
                        String l1 = langResult.getString("language_one").substring(langResult.getString("language_one").lastIndexOf("-") + 1);
                        String l2 = langResult.getString("language_two").substring(langResult.getString("language_two").lastIndexOf("-") + 1);
                        languages.add(new Locale(l1.trim().toLowerCase()));
                        languages.add(new Locale(l2.trim().toLowerCase()));
                    }
                    g.setAllowedLanguages(languages);
                }
                catch(Exception e)
                {
                    log.error("Error in fetching language details for old glossary resource_id: " + resourceId, e);
                    System.exit(0);
                }
                //Get glossary ID to get entry details
                try(PreparedStatement glossaryId = learnweb.getInstance().getConnection().prepareStatement("SELECT * FROM `lw_resource_glossary` WHERE `resource_id`=?"))
                {
                    glossaryId.setInt(1, result.getInt("resource_id"));
                    ResultSet glossaries = glossaryId.executeQuery();
                    List<GlossaryEntry> entries = new ArrayList<GlossaryEntry>();
                    while(glossaries.next())
                    {
                        //Entry details
                        try(PreparedStatement entry = learnweb.getInstance().getConnection().prepareStatement("SELECT * FROM `lw_glossary_details` WHERE `glossary_id`=?"))
                        {
                            entry.setInt(1, glossaries.getInt("glossary_id"));
                            ResultSet entryDetails = entry.executeQuery();

                            while(entryDetails.next())
                            {
                                GlossaryEntry newEntry = new GlossaryEntry();
                                newEntry.setDescription(entryDetails.getString("description"));
                                newEntry.setTopicOne(entryDetails.getString("topic_1"));
                                newEntry.setTopicTwo(entryDetails.getString("topic_2"));
                                newEntry.setTopicThree(entryDetails.getString("topic_3"));
                                newEntry.setTimestamp(result.getTimestamp("resource_timestamp"));
                                newEntry.setUserId(g.getUserId());
                                newEntry.setLastChangedByUserId(g.getUserId());
                                newEntry.setResourceId(g.getId());

                                //Term details for every entry

                                try(PreparedStatement termDetails = learnweb.getInstance().getConnection().prepareStatement("SELECT * FROM `lw_resource_glossary_terms` WHERE `glossary_id`=?"))
                                {
                                    termDetails.setInt(1, glossaries.getInt("glossary_id"));
                                    ResultSet termResult = termDetails.executeQuery();
                                    while(termResult.next())
                                    {
                                        GlossaryTerm term = new GlossaryTerm();
                                        term.setDeleted(termResult.getBoolean("deleted"));
                                        term.setUserId(termResult.getInt("user_id"));
                                        term.setLastChangedByUserId(termResult.getInt("user_id"));
                                        term.setTerm(termResult.getString("term"));
                                        String termLang = termResult.getString("language");
                                        term.setLanguage(termLang.substring(termLang.lastIndexOf("-") + 1).trim().toLowerCase());
                                        term.setUses(Arrays.asList(termResult.getString("use").split(",")));
                                        term.setPronounciation(termResult.getString("pronounciation"));
                                        term.setAcronym(termResult.getString("acronym"));
                                        term.setSource(termResult.getString("references"));
                                        term.setPhraseology(termResult.getString("phraseology"));
                                        term.setTimestamp(termResult.getTimestamp("timestamp"));
                                        newEntry.addTerm(term);
                                    }
                                }
                                catch(Exception e)
                                {
                                    log.error("Error in fetching term details for resource_id:" + resourceId + " glossary_id:" + glossaries.getInt("glossary_id"), e);
                                    System.exit(0);
                                }
                                entries.add(newEntry);

                            }
                            g.setEntries(entries);
                        }
                        catch(Exception e)
                        {
                            log.error("error in fetching entry details for resource_id:" + resourceId + " old glossary_id:" + glossaries.getInt("glossary_id"), e);
                            System.exit(0);
                        }

                    }
                }
                catch(Exception e)
                {
                    log.error("Error in fetching glossaryIDs for old glossary resource_id:" + resourceId, e);
                    System.exit(0);
                }
                //Saving the resource
                try(PreparedStatement insertResource = learnweb.getInstance().getConnection().prepareStatement("INSERT INTO `lw_glossary_resource`(`resource_id`, `allowed_languages`) VALUES (?,?)"))
                {
                    insertResource.setInt(1, g.getId());
                    insertResource.setString(2, StringUtils.join(g.getAllowedLanguages(), ","));
                    insertResource.executeQuery();
                    insertResource.close();
                }
                catch(Exception e)
                {
                    log.error("Error in inserting new resource details: " + resourceId, e);
                    System.exit(0);
                }

                for(GlossaryEntry entry : g.getEntries())
                {
                    //Add entry

                    try(PreparedStatement addEntry = learnweb.getInstance().getConnection().prepareStatement(
                            "INSERT INTO `lw_glossary_entry`(`resource_id`, `original_entry_id`, `last_changed_by_user_id`, `user_id`, `deleted`, `topic_one`, `topic_two`, `topic_three`, `description`, `description_pasted`, `timestamp`) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                            PreparedStatement.RETURN_GENERATED_KEYS))
                    {
                        addEntry.setInt(1, g.getId());
                        addEntry.setInt(2, 0);
                        addEntry.setInt(3, entry.getLastChangedByUserId());
                        addEntry.setInt(4, g.getUserId());
                        addEntry.setBoolean(5, false);
                        addEntry.setString(6, entry.getTopicOne());
                        addEntry.setString(7, entry.getTopicTwo());
                        addEntry.setString(8, entry.getTopicThree());
                        addEntry.setString(9, entry.getDescription());
                        addEntry.setBoolean(10, false);
                        addEntry.setTimestamp(11, entry.getTimestamp());
                        addEntry.executeQuery();
                        ResultSet entryId = addEntry.getGeneratedKeys();
                        if(entryId.next())
                        {
                            entry.setId(entryId.getInt(1));
                        }

                        addEntry.close();
                    }
                    catch(Exception e)
                    {
                        log.error("Error in inserting entry for resource_id: " + resourceId, e);
                        System.exit(0);
                    }

                    //Add Terms
                    for(GlossaryTerm term : entry.getTerms())
                    {
                        try(PreparedStatement addTerms = learnweb.getInstance().getConnection().prepareStatement(
                                "INSERT INTO `lw_glossary_term`(`entry_id`, `original_term_id`, `last_changed_by_user_id`, `user_id`, `deleted`, `term`, `language`, `uses`, `pronounciation`, `acronym`, `source`, `phraseology`, `timestamp`, `term_pasted`, `pronounciation_pasted`, `acronym_pasted`, `phraseology_pasted`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"))
                        {
                            addTerms.setInt(1, entry.getId());
                            addTerms.setInt(2, 0);
                            addTerms.setInt(3, term.getLastChangedByUserId());
                            addTerms.setInt(4, term.getUserId());
                            addTerms.setBoolean(5, term.isDeleted());
                            addTerms.setString(6, term.getTerm());
                            addTerms.setString(7, term.getLanguage());
                            addTerms.setString(8, StringUtils.join(term.getUses(), ","));
                            addTerms.setString(9, term.getPronounciation());
                            addTerms.setString(10, term.getAcronym());
                            addTerms.setString(11, term.getSource());
                            addTerms.setString(12, term.getPhraseology());
                            addTerms.setTimestamp(13, term.getTimestamp());
                            addTerms.setBoolean(14, false);
                            addTerms.setBoolean(15, false);
                            addTerms.setBoolean(16, false);
                            addTerms.setBoolean(17, false);
                            addTerms.executeQuery();
                            addTerms.close();

                        }
                        catch(Exception e)
                        {
                            log.error("Error in inserting terms for resource_id: " + resourceId + " entry id: " + entry.getId(), e);
                            System.exit(0);
                        }
                    }
                }

                //Change from type glossary to glossary2 in lw_resource
                try(PreparedStatement changeGlossary = learnweb.getInstance().getConnection().prepareStatement("UPDATE `lw_resource` SET `type`=?, url=? WHERE `resource_id`=?"))
                {
                    String url = result.getString("url").replace("showGlossary.jsf", "glossary/glossary.jsf");
                    changeGlossary.setString(1, "glossary2");
                    changeGlossary.setString(2, url);
                    changeGlossary.setInt(3, g.getId());
                    changeGlossary.executeQuery();
                    changeGlossary.close();
                }
                catch(Exception e)
                {
                    log.error("Error in changing glossary resource type to new in main for resource_id: " + resourceId, e);
                }

            }
        }
        catch(Exception e)
        {
            log.error("Error in merging glossary resource_id: " + resourceId, e);
            System.exit(0);
        }
        System.out.println("Merge completed successfully");
        log.info("Merge completed successfully");
        System.exit(0);
        /*for all glossary resource, for all glossary_id{
        }
        GlossaryResource g = new GlossaryResource();
        //lw_glossary_resource
        1. resource_id from main resultset
        2. allowed_languages -> get language_one and language_two for lw_resource_glossary_main where resource_id is eq to r_id
        SAVE THIS HERE
        for all glossary_id{
        GlossaryEntry e = new GlossaryEntry();
        E DETAILS for lw_glossary_entry
        1. get topic1, topic2, topic3 and description from lw_glossary_details
        2. user_id from main result
        SAVE
        //lw_glossary_term
        1. get all terms details from lw_resource_glossary_terms where glossary_id is same
        2. for all terms(){
            GlossaryTerm t = new GlossaryTerm();
            add all details
            e.add(term)
            SAVE
        
        }
        
        }
        
        }*/

    }

}
