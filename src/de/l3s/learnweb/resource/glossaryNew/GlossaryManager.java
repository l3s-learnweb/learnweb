package de.l3s.learnweb.resource.glossaryNew;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.Resource;

public class GlossaryManager
{
    private final static Logger log = Logger.getLogger(GlossaryManager.class);
    private final Learnweb learnweb;

    public GlossaryManager(Learnweb learnweb)
    {
        this.learnweb = learnweb;
    }

    public void createGlossaryResource(Resource resource) throws SQLException //Called when resource is created in right-pane
    {
        PreparedStatement insertGlossary = learnweb.getConnection().prepareStatement("INSERT INTO `lw_glossary_resource`(`resource_id`, `allowed_languages`) VALUES (?, ?)");
        insertGlossary.setInt(1, resource.getId());
        insertGlossary.setString(2, String.join(",", resource.getGlossaryLanguages()));
        insertGlossary.executeQuery();

    }

    public void saveEntry(GlossaryEntry entry, int userId) throws SQLException
    {
        if(entry.isDeleted())
        {
            PreparedStatement deleteEntry = learnweb.getConnection().prepareStatement("UPDATE `lw_glossary_entry` SET `deleted`=? WHERE `entry_id`=?");
            deleteEntry.setBoolean(1, true);
            deleteEntry.setInt(2, entry.getId());
            deleteEntry.executeQuery();

            PreparedStatement deleteTerms = learnweb.getConnection().prepareStatement("UPDATE `lw_glossary_term` SET `deleted`=? WHERE `entry_id`=?");
            deleteTerms.setBoolean(1, true);
            deleteTerms.setInt(2, entry.getId());
            deleteTerms.executeQuery();
        }
        else
        {
            if(entry.getId() < 0) // new entry
            {
                PreparedStatement insertEntry = learnweb.getConnection().prepareStatement("INSERT INTO `lw_glossary_entry`(`resource_id`, `user_id`, `topic_one`, `topic_two`, `topic_three`, `description`, `description_pasted`) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        PreparedStatement.RETURN_GENERATED_KEYS);
                insertEntry.setInt(1, entry.getResourceId());
                insertEntry.setInt(2, userId);
                insertEntry.setString(3, entry.getTopicOne());
                insertEntry.setString(4, entry.getTopicTwo());
                insertEntry.setString(5, entry.getTopicThree());
                insertEntry.setString(6, entry.getDescription());
                insertEntry.setBoolean(7, entry.isDescriptionPasted());
                insertEntry.executeQuery();
                ResultSet entryInserted = insertEntry.getGeneratedKeys();
                entryInserted.next();
                entry.setId(entryInserted.getInt(1));

            }
            else //old entry updated
            {

                PreparedStatement updateEntry = learnweb.getConnection().prepareStatement("UPDATE `lw_glossary_entry` SET `topic_one`=?,`topic_two`=?,`topic_three`=?,`description`=?,`description_pasted`=? WHERE `entry_id`=?");
                updateEntry.setString(1, entry.getTopicOne());
                updateEntry.setString(2, entry.getTopicTwo());
                updateEntry.setString(3, entry.getTopicThree());
                updateEntry.setString(4, entry.getDescription());
                updateEntry.setBoolean(5, entry.isDescriptionPasted());
                updateEntry.setInt(6, entry.getId());
                updateEntry.executeUpdate();
            }
            saveTerms(entry.getTerms(), entry.getId(), userId);
        }

    }

    public void saveTerms(List<GlossaryTerm> terms, int entryId, int userId) throws SQLException
    {
        PreparedStatement termInsert = learnweb.getConnection().prepareStatement(
                "INSERT INTO `lw_glossary_term`(`entry_id`, `user_id`, `term`, `language`, `uses`, `pronounciation`, `acronym`, `source`, `phraseology`, `term_pasted`, `pronounciation_pasted`, `acronym_pasted`, `phraseology_pasted`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        PreparedStatement termUpdate = learnweb.getConnection().prepareStatement(
                "UPDATE `lw_glossary_term` SET `entry_id`=?, `deleted`=?, `term`=?, `language`=?, `uses`=?, `pronounciation`=?, `acronym`=?, `source`=?, `phraseology`=?, `term_pasted`=?, `pronounciation_pasted`=?, `acronym_pasted`=?, `phraseology_pasted`=? WHERE `term_id`=?");
        for(GlossaryTerm term : terms)
        {
            if(term.getId() < 0)//new term
            {
                termInsert.setInt(1, entryId);
                termInsert.setInt(2, userId);
                termInsert.setString(3, term.getTerm());
                termInsert.setString(4, term.getLanguage());
                termInsert.setString(5, String.join(",", term.getUses()));
                termInsert.setString(6, term.getPronounciation());
                termInsert.setString(7, term.getAcronym());
                termInsert.setString(8, term.getSource());
                termInsert.setString(9, term.getPhraseology());
                termInsert.setBoolean(10, term.isTermPasted());
                termInsert.setBoolean(11, term.isPronounciationPasted());
                termInsert.setBoolean(12, term.isAcronymPasted());
                termInsert.setBoolean(13, term.isPhraseologyPasted());
                termInsert.addBatch();
            }
            else
            {
                termUpdate.setInt(1, entryId);
                termUpdate.setBoolean(2, term.isDeleted());
                termUpdate.setString(3, term.getTerm());
                termUpdate.setString(4, term.getLanguage());
                termUpdate.setString(5, String.join(",", term.getUses()));
                termUpdate.setString(6, term.getPronounciation());
                termUpdate.setString(7, term.getAcronym());
                termUpdate.setString(8, term.getSource());
                termUpdate.setString(9, term.getPhraseology());
                termUpdate.setBoolean(10, term.isTermPasted());
                termUpdate.setBoolean(11, term.isPronounciationPasted());
                termUpdate.setBoolean(12, term.isAcronymPasted());
                termUpdate.setBoolean(13, term.isPhraseologyPasted());
                termUpdate.setInt(14, term.getId());
                termUpdate.addBatch();
            }
            termInsert.executeBatch();
            termUpdate.executeBatch();
        }
    }

    public void copyGlossary(int oldResourceId, int newResourceId, int userId) throws SQLException
    {
        GlossaryResource newResource = getGlossaryResource(oldResourceId);
        newResource.getEntries().forEach(entry -> entry.setId(-1));
        newResource.getEntries().forEach(entry -> entry.getTerms().forEach(term -> term.setId(-1)));
        newResource.setId(newResourceId);
        createGlossaryResource(newResource);
        for(GlossaryEntry entry : newResource.getEntries())
        {
            saveEntry(entry, userId);
        }
    }

    public GlossaryResource getGlossaryResource(int resourceId) throws SQLException
    {
        log.info("getting glossary resource");
        GlossaryResource glossary = new GlossaryResource();
        glossary.setId(resourceId);
        PreparedStatement getGlossary = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_glossary_resource` WHERE `resource_id`=?");
        getGlossary.setInt(1, resourceId);
        ResultSet result = getGlossary.executeQuery();
        if(result.next())
        {
            glossary.setAllowedLanguages(convertStringToLocale(result.getString("allowed_languages")));
        }
        //Glossary Entries details
        List<GlossaryEntry> entries = getGlossaryEntries(resourceId);
        glossary.setEntries(entries);
        return glossary;

    }

    public List<GlossaryEntry> getGlossaryEntries(int resourceId) throws SQLException
    {
        PreparedStatement getEntries = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_glossary_entry` WHERE `resource_id`=? and deleted = ?");
        getEntries.setInt(1, resourceId);
        getEntries.setBoolean(2, false);
        ResultSet resultEntries = getEntries.executeQuery();
        List<GlossaryEntry> entries = new LinkedList<>();
        while(resultEntries.next())
        {
            GlossaryEntry entry = new GlossaryEntry();
            entry.setDeleted(false);
            entry.setResourceId(resourceId);
            entry.setId(resultEntries.getInt("entry_id"));
            entry.setUserId(resultEntries.getInt("user_id"));
            entry.setTopicOne(resultEntries.getString("topic_one"));
            entry.setTopicTwo(resultEntries.getString("topic_two"));
            entry.setTopicThree(resultEntries.getString("topic_three"));
            entry.setDescription(resultEntries.getString("description"));
            entry.setDescriptionPasted(resultEntries.getBoolean("description_pasted"));

            //get terms for given entry
            entry.setTerms(getGlossaryTerms(entry.getId()));

            entries.add(entry);
        }
        return entries;
    }

    public List<GlossaryTerm> getGlossaryTerms(int entryId) throws SQLException
    {
        List<GlossaryTerm> entryTerms = new LinkedList<>();
        PreparedStatement getTerms = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_glossary_term` WHERE `entry_id`=? and `deleted`=?");
        getTerms.setInt(1, entryId);
        getTerms.setBoolean(2, false);
        ResultSet terms = getTerms.executeQuery();
        while(terms.next())
        {
            GlossaryTerm term = new GlossaryTerm();
            term.setDeleted(false);
            term.setEntryId(entryId);
            term.setId(terms.getInt("term_id"));
            term.setUserId(terms.getInt("user_id"));
            term.setTerm(terms.getString("term"));
            term.setLanguage(terms.getString("language"));
            term.setUses(Arrays.asList(terms.getString("uses").split(",")));
            term.setPronounciation(terms.getString("pronounciation"));
            term.setAcronym(terms.getString("acronym"));
            term.setSource(terms.getString("source"));
            term.setPhraseology(terms.getString("phraseology"));
            term.setTimestamp(terms.getTimestamp("timestamp"));
            term.setTermPasted(terms.getBoolean("term_pasted"));
            term.setPronounciationPasted(terms.getBoolean("pronounciation_pasted"));
            term.setAcronymPasted(terms.getBoolean("acronym_pasted"));
            term.setPhraseologyPasted(terms.getBoolean("phraseology_pasted"));
            entryTerms.add(term);

        }
        return entryTerms;
    }

    public ArrayList<GlossaryTableView> convertToGlossaryTableView(GlossaryResource glossaryResource2)
    {
        ArrayList<GlossaryTableView> tableView = new ArrayList<GlossaryTableView>();
        for(GlossaryEntry entry : glossaryResource2.getEntries())
        {
            for(GlossaryTerm term : entry.getTerms())
            {
                tableView.add(new GlossaryTableView(entry, term));
            }
        }
        return tableView;
    }

    public List<Locale> convertStringToLocale(String string)
    {
        List<String> languages = Arrays.asList(string.split(","));
        List<Locale> convertedLanguages = new LinkedList<>();
        for(String lang : languages)
        {
            convertedLanguages.add(new Locale(lang));
        }
        return convertedLanguages;
    }

    public void delete(int resourceId) throws SQLException
    {
        PreparedStatement deleteGlossary = learnweb.getConnection().prepareStatement("UPDATE `lw_glossary_resource` SET `deleted`=? WHERE `resource_id`=?");
        deleteGlossary.setBoolean(1, true);
        deleteGlossary.setInt(2, resourceId);
        deleteGlossary.executeQuery();

    }
}
