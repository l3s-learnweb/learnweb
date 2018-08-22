package de.l3s.learnweb.resource.glossaryNew;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;

public class GlossaryManager
{
    private final static Logger log = Logger.getLogger(GlossaryManager.class);
    private final Learnweb learnweb;

    public GlossaryManager(Learnweb learnweb)
    {
        this.learnweb = learnweb;
    }

    public void saveGlossaryResource(GlossaryResource resource) throws SQLException //Called when resource is created in right-pane
    {
        if(resource.isDeleted()) //set resource and entries as deleted
        {
            delete(resource);
            return;
        }
        PreparedStatement insertGlossary = learnweb.getConnection().prepareStatement("INSERT INTO `lw_glossary_resource`(`resource_id`, `allowed_languages`) VALUES (?, ?)");
        insertGlossary.setInt(1, resource.getId());
        insertGlossary.setString(2, String.join(",", resource.getAllowedLanguages())); // TODO use StringHelper.join(resource.getAllowedLanguages())
        insertGlossary.executeQuery();

        if(resource.isClonedButNotPersisted()) // after a resource has been cloned we have to persist the cloned entries
            saveEntries(resource);
    }

    /**
     * To set new IDs for entries and terms
     *
     * @throws SQLException
     */
    private void saveEntries(GlossaryResource resource) throws SQLException
    {
        if(resource.getEntries() == null)
            return;

        for(GlossaryEntry entry : resource.getEntries())
        {
            entry.setDeleted(resource.isDeleted());
            entry.setId(-1);
            entry.setResourceId(resource.getId());
            entry.getTerms().forEach(term -> term.setId(-1));
            saveEntry(entry, resource.getUserId()); //TODO:: userId of user who copies or id of old user???
        }

        resource.setClonedButNotPersisted(false);
    }

    public void saveEntry(GlossaryEntry entry, int userId) throws SQLException
    {
        // TODO I think the userId parameter can be removed.
        // When an entry is deleted I would not set it to zero. The idea of the deleted flag was that we can undo deletions. But when you change the user_id this becomes harder.
        // When a glossary resource was cloned we could keep the createdByUserId and lastChangedByUserId as they are. When a resource is copied the new user becomes only owner of the resource. The entries keep their original creator. What do you think?

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
            // TODO Rishita: use INSERT INTO ON DUPLICATE UPDATE see Learnweb code mail from 04.05.2018 and SQL.getCreateStatement

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
            saveTerms(entry.getTerms(), userId);
        }

    }

    //  TODO entryId and userId are stored in the GlossaryTerm. No need to provide them separately
    //@Philipp: Entry ID for a new entry that is recently added is not stored in GlossaryTerm yet.
    // It is much cleaner to update the terms when its parent entry.id is updated; Fixed it myself
    public void saveTerms(List<GlossaryTerm> terms, int userId) throws SQLException
    {
        PreparedStatement termInsert = learnweb.getConnection().prepareStatement(
                "INSERT INTO `lw_glossary_term`(`entry_id`, `user_id`, `term`, `language`, `uses`, `pronounciation`, `acronym`, `source`, `phraseology`, `term_pasted`, `pronounciation_pasted`, `acronym_pasted`, `phraseology_pasted`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        PreparedStatement termUpdate = learnweb.getConnection().prepareStatement(
                "UPDATE `lw_glossary_term` SET `entry_id`=?, `deleted`=?, `term`=?, `language`=?, `uses`=?, `pronounciation`=?, `acronym`=?, `source`=?, `phraseology`=?, `term_pasted`=?, `pronounciation_pasted`=?, `acronym_pasted`=?, `phraseology_pasted`=? WHERE `term_id`=?");
        for(GlossaryTerm term : terms)
        {
            // TODO see line 80
            if(term.getId() < 0)//new term
            {
                termInsert.setInt(1, term.getEntryId());
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

                // TODO the generated id must be retrieved to update term.id here
            }
            else
            {
                termUpdate.setInt(1, term.getEntryId());
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

    public List<GlossaryEntry> getGlossaryEntries(int resourceId) throws SQLException
    {
        List<GlossaryEntry> entries = new LinkedList<>();

        try(PreparedStatement getEntries = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_glossary_entry` WHERE `resource_id`=? and deleted = ?");)
        {
            getEntries.setInt(1, resourceId);
            getEntries.setBoolean(2, false);
            ResultSet resultEntries = getEntries.executeQuery();
            while(resultEntries.next())
            {
                GlossaryEntry entry = new GlossaryEntry();
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
        }
        return entries;
    }

    public List<GlossaryTerm> getGlossaryTerms(int entryId) throws SQLException
    {
        List<GlossaryTerm> entryTerms = new LinkedList<>();
        try(PreparedStatement getTerms = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_glossary_term` WHERE `entry_id`=? and `deleted`=?");)
        {
            getTerms.setInt(1, entryId);
            getTerms.setBoolean(2, false);
            ResultSet terms = getTerms.executeQuery();
            while(terms.next())
            {
                GlossaryTerm term = new GlossaryTerm();
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

    public void delete(GlossaryResource resource) throws SQLException
    {
        if(resource.getEntries() != null || !resource.getEntries().isEmpty())
        {
            for(GlossaryEntry entry : resource.getEntries())
            {
                entry.setDeleted(true);
                saveEntry(entry, 0);
            }
        }
    }

    /**
     * loads glossary metadata into glossaryresource
     *
     * @param glossaryResource
     * @throws SQLException
     */
    public void loadGlossaryResource(GlossaryResource glossaryResource) throws SQLException
    {
        PreparedStatement getGlossary = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_glossary_resource` WHERE `resource_id`=?");
        getGlossary.setInt(1, glossaryResource.getId());
        ResultSet result = getGlossary.executeQuery();
        if(result.next())
        {
            glossaryResource.setAllowedLanguages(new ArrayList<String>(Arrays.asList(result.getString("allowed_languages").split(","))));
        }
        else
        {
            glossaryResource = null;
            log.error("Error in loading languages for glossary while loading glossary resource from Database"); // TODO add at least the resource id so that one has a chance to find the problematic resource
            return;
        }
        //Glossary Entries details
        List<GlossaryEntry> entries = getGlossaryEntries(glossaryResource.getId());
        glossaryResource.setEntries(entries);

    }
}
