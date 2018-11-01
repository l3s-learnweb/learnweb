package de.l3s.learnweb.resource.glossaryNew;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.Resource.ResourceType;
import de.l3s.util.BeanHelper;
import de.l3s.util.StringHelper;

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
        try(PreparedStatement insertGlossary = learnweb.getConnection().prepareStatement("INSERT INTO `lw_glossary_resource`(`resource_id`, `allowed_languages`) VALUES (?, ?)"))
        {
            insertGlossary.setInt(1, resource.getId());
            insertGlossary.setString(2, StringHelper.join(resource.getAllowedLanguages()));
            insertGlossary.executeQuery();
        }
        if(resource.isClonedButNotPersisted()) // after a resource has been cloned we have to persist the cloned entries
            saveEntries(resource);
    }

    /**
     * To set new IDs for entries and terms which are copied
     *
     * @throws SQLException
     */
    private void saveEntries(GlossaryResource resource) throws SQLException
    {
        if(resource.getEntries() == null)
            return;

        for(GlossaryEntry entry : resource.getEntries())
        {
            entry.setUserId(resource.getUserId()); //User ID of user who created the entry
            entry.setDeleted(resource.isDeleted());
            entry.setLastChangedByUserId(resource.getUserId());
            entry.setId(-1);
            entry.setResourceId(resource.getId());
            entry.getTerms().forEach(term -> term.setId(-1));
            insertEntry(entry);
            saveTerms(entry, resource.getGroupId());
        }

        resource.setClonedButNotPersisted(false); //Required to reset it if it was set prior to this.
    }

    public void saveEntry(GlossaryEntry entryCloned, GlossaryResource glossaryResource) throws SQLException
    {
        entryCloned.setTimestamp(new Timestamp(System.currentTimeMillis())); // change timestamp of entry to latest timestamp
        if(entryCloned.isDeleted())
        {
            deleteEntry(entryCloned);
        }
        else
        {
            //Discuss with @Philipp
            // TODO Rishita: use INSERT INTO ON DUPLICATE UPDATE see Learnweb code mail from 04.05.2018 and SQL.getCreateStatement

            if(entryCloned.getId() < 0) // new entry
            {
                insertEntry(entryCloned);

                try
                {
                    HttpSession session;
                    String sessionId = (session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(true)) != null ? session.getId() : null;
                    learnweb.getLogManager().log(learnweb.getUserManager().getUser(entryCloned.getLastChangedByUserId()), Action.glossary_entry_add, glossaryResource.getGroupId(), glossaryResource.getId(), Integer.toString(entryCloned.getId()), sessionId);

                }
                catch(Exception e)
                {
                    log.error("Error in logging " + Action.glossary_entry_add + " for entry ID: " + entryCloned.getId() + " of resource: " + entryCloned.getResourceId());
                }

            }
            else //old entry updated
            {
                updateEntry(entryCloned);
            }
            saveTerms(entryCloned, glossaryResource.getGroupId());
            entryCloned.getTerms().removeIf(term -> term.isDeleted());
            //Replace or Add the entry corresponding to cloned entry
            glossaryResource.getEntries().removeIf(entry -> entry.getId() == entryCloned.getId());
            glossaryResource.getEntries().add(entryCloned);

        }

    }

    private void deleteEntry(GlossaryEntry entryCloned) throws SQLException
    {
        try(PreparedStatement deleteEntry = learnweb.getConnection().prepareStatement("UPDATE `lw_glossary_entry` SET `deleted`=?, `last_changed_by_user_id`=? WHERE `entry_id`=?");)
        {
            deleteEntry.setBoolean(1, true);
            deleteEntry.setInt(2, entryCloned.getLastChangedByUserId());
            deleteEntry.setInt(3, entryCloned.getId());
            deleteEntry.executeQuery();
        }
        try(PreparedStatement deleteTerms = learnweb.getConnection().prepareStatement("UPDATE `lw_glossary_term` SET `deleted`=?, `last_changed_by_user_id`=? WHERE `entry_id`=?");)
        {
            deleteTerms.setBoolean(1, true);
            deleteTerms.setInt(2, entryCloned.getLastChangedByUserId());
            deleteTerms.setInt(3, entryCloned.getId());
            deleteTerms.executeQuery();
        }

    }

    private void insertEntry(GlossaryEntry entryCloned) throws SQLException
    {

        //String ins = "INSERT INTO `lw_glossary_entry`(`entry_id`, `resource_id`, `original_entry_id`, `last_changed_by_user_id`, `user_id`, `topic_one`, `topic_two`, `topic_three`, `description`, `description_pasted`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE `last_changed_by_user_id`=VALUES(`last_changed_by_user_id`), `topic_one`=VALUES(`topic_one`), `topic_two`=VALUES(`topic_two`), `topic_three`=VALUES(`topic_three`), `description`=VALUES(`description`), `description_pasted`=VALUES(`description_pasted`)";
        try(PreparedStatement insertEntry = learnweb.getConnection().prepareStatement(
                "INSERT INTO `lw_glossary_entry`(`resource_id`, `original_entry_id`, `last_changed_by_user_id`, `user_id`, `topic_one`, `topic_two`, `topic_three`, `description`, `description_pasted`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                PreparedStatement.RETURN_GENERATED_KEYS);)
        {
            //PreparedStatement insertEntry = learnweb.getConnection().prepareStatement(
            //ins, PreparedStatement.RETURN_GENERATED_KEYS);
            if(entryCloned.getUserId() <= 0)
                entryCloned.setUserId(entryCloned.getLastChangedByUserId());//last change by userID == original user ID in insert
            /*insertEntry.setInt(1, entry.getId());
            insertEntry.setInt(2, entry.getResourceId());
            insertEntry.setInt(3, entry.getOriginalEntryId());
            insertEntry.setInt(4, entry.getLastChangedByUserId());
            insertEntry.setInt(5, entry.getUserId());
            insertEntry.setString(6, entry.getTopicOne());
            insertEntry.setString(7, entry.getTopicTwo());
            insertEntry.setString(8, entry.getTopicThree());
            insertEntry.setString(9, entry.getDescription());
            insertEntry.setBoolean(10, entry.isDescriptionPasted());
            insertEntry.executeUpdate();*/
            insertEntry.setInt(1, entryCloned.getResourceId());
            insertEntry.setInt(2, entryCloned.getOriginalEntryId());
            insertEntry.setInt(3, entryCloned.getLastChangedByUserId());
            insertEntry.setInt(4, entryCloned.getUserId());
            insertEntry.setString(5, entryCloned.getTopicOne());
            insertEntry.setString(6, entryCloned.getTopicTwo());
            insertEntry.setString(7, entryCloned.getTopicThree());
            insertEntry.setString(8, entryCloned.getDescription());
            insertEntry.setBoolean(9, entryCloned.isDescriptionPasted());
            insertEntry.executeQuery();
            ResultSet entryInserted = insertEntry.getGeneratedKeys();
            entryInserted.next();
            entryCloned.setId(entryInserted.getInt(1));
            /*if(entry.getId() < 1)
            {
            ResultSet entryInserted = insertEntry.getGeneratedKeys();
            entryInserted.next();
            entry.setId(entryInserted.getInt(1));
            glossaryResource.getEntries().add(entry);
            }*/
        }

    }

    private void updateEntry(GlossaryEntry entryCloned) throws SQLException
    {
        try(PreparedStatement updateEntry = learnweb.getConnection().prepareStatement("UPDATE `lw_glossary_entry` SET `topic_one`=?,`topic_two`=?,`topic_three`=?,`description`=?,`description_pasted`=?, `last_changed_by_user_id`=? WHERE `entry_id`=?");)
        {
            updateEntry.setString(1, entryCloned.getTopicOne());
            updateEntry.setString(2, entryCloned.getTopicTwo());
            updateEntry.setString(3, entryCloned.getTopicThree());
            updateEntry.setString(4, entryCloned.getDescription());
            updateEntry.setBoolean(5, entryCloned.isDescriptionPasted());
            updateEntry.setInt(6, entryCloned.getLastChangedByUserId());
            updateEntry.setInt(7, entryCloned.getId());
            updateEntry.executeUpdate();
        }
    }

    public void saveTerms(GlossaryEntry entry, int groupId) throws SQLException
    {

        for(GlossaryTerm term : entry.getTerms())
        {

            if(term.getId() < 0)
            {
                insertTerms(term, entry, groupId);
            }
            else
            {
                updateTerms(term, entry);
            }

        }

    }

    private void updateTerms(GlossaryTerm term, GlossaryEntry entry) throws SQLException
    {
        try(PreparedStatement termUpdate = learnweb.getConnection().prepareStatement(
                "UPDATE `lw_glossary_term` SET `entry_id`=?, `deleted`=?, `term`=?, `language`=?, `uses`=?, `pronounciation`=?, `acronym`=?, `source`=?, `phraseology`=?, `term_pasted`=?, `pronounciation_pasted`=?, `acronym_pasted`=?, `phraseology_pasted`=?, `last_changed_by_user_id`=? WHERE `term_id`=?");)
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
            termUpdate.setInt(14, entry.getLastChangedByUserId());
            termUpdate.setInt(15, term.getId());
            termUpdate.executeQuery();
            //set term
            term.setLastChangedByUserId(entry.getLastChangedByUserId());
        }

    }

    public void insertTerms(GlossaryTerm term, GlossaryEntry entry, int groupId) throws SQLException
    {
        try(PreparedStatement termInsert = learnweb.getConnection().prepareStatement(
                "INSERT INTO `lw_glossary_term`(`entry_id`, `original_term_id`, `last_changed_by_user_id`, `user_id`, `term`, `language`, `uses`, `pronounciation`, `acronym`, `source`, `phraseology`, `term_pasted`, `pronounciation_pasted`, `acronym_pasted`, `phraseology_pasted`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                PreparedStatement.RETURN_GENERATED_KEYS);)
        {
            termInsert.setInt(1, entry.getId());
            termInsert.setInt(2, term.getOriginalTermId());
            termInsert.setInt(3, entry.getLastChangedByUserId());
            termInsert.setInt(4, entry.getUserId());
            termInsert.setString(5, term.getTerm());
            termInsert.setString(6, term.getLanguage());
            termInsert.setString(7, String.join(",", term.getUses()));
            termInsert.setString(8, term.getPronounciation());
            termInsert.setString(9, term.getAcronym());
            termInsert.setString(10, term.getSource());
            termInsert.setString(11, term.getPhraseology());
            termInsert.setBoolean(12, term.isTermPasted());
            termInsert.setBoolean(13, term.isPronounciationPasted());
            termInsert.setBoolean(14, term.isAcronymPasted());
            termInsert.setBoolean(15, term.isPhraseologyPasted());
            termInsert.executeQuery();
            ResultSet termInserted = termInsert.getGeneratedKeys();
            termInserted.next();
            //set term values
            term.setId(termInserted.getInt(1));
            term.setLastChangedByUserId(entry.getLastChangedByUserId());
            term.setUserId(entry.getUserId());

            HttpSession session;
            String sessionId = (session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(true)) != null ? session.getId() : null;
            learnweb.getLogManager().log(learnweb.getUserManager().getUser(entry.getLastChangedByUserId()), Action.glossary_term_add, groupId, entry.getResourceId(), Integer.toString(entry.getId()), sessionId);
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
                entry.setDeleted(false);
                entry.setResourceId(resourceId);
                entry.setId(resultEntries.getInt("entry_id"));
                entry.setOriginalEntryId(resultEntries.getInt("original_entry_id"));
                entry.setUserId(resultEntries.getInt("user_id"));
                entry.setTopicOne(resultEntries.getString("topic_one"));
                entry.setTopicTwo(resultEntries.getString("topic_two"));
                entry.setTopicThree(resultEntries.getString("topic_three"));
                entry.setDescription(resultEntries.getString("description"));
                entry.setDescriptionPasted(resultEntries.getBoolean("description_pasted"));
                entry.setTimestamp(resultEntries.getTimestamp("timestamp"));

                //get terms for given entry
                entry.setTerms(getGlossaryTerms(entry));
                entries.add(entry);
            }
        }
        return entries;
    }

    public List<GlossaryTerm> getGlossaryTerms(GlossaryEntry entry) throws SQLException
    {
        List<GlossaryTerm> entryTerms = new LinkedList<>();
        try(PreparedStatement getTerms = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_glossary_term` WHERE `entry_id`=? and `deleted`=?");)
        {
            StringBuilder fulltext = new StringBuilder();
            getTerms.setInt(1, entry.getId());
            getTerms.setBoolean(2, false);
            ResultSet terms = getTerms.executeQuery();
            while(terms.next())
            {
                GlossaryTerm term = new GlossaryTerm();
                term.setEntryId(entry.getId());
                term.setId(terms.getInt("term_id"));
                term.setUserId(terms.getInt("user_id"));
                term.setTerm(terms.getString("term"));
                term.setLanguage(terms.getString("language"));
                term.setUses(terms.getString("uses").isEmpty() ? new ArrayList<String>() : Arrays.asList(terms.getString("uses").split(",")));
                term.setPronounciation(terms.getString("pronounciation"));
                term.setAcronym(terms.getString("acronym"));
                term.setSource(terms.getString("source"));
                term.setPhraseology(terms.getString("phraseology"));
                term.setTimestamp(terms.getTimestamp("timestamp"));
                term.setTermPasted(terms.getBoolean("term_pasted"));
                term.setPronounciationPasted(terms.getBoolean("pronounciation_pasted"));
                term.setAcronymPasted(terms.getBoolean("acronym_pasted"));
                term.setPhraseologyPasted(terms.getBoolean("phraseology_pasted"));

                //Append fulltext with all words from term
                fulltext.append(term.getTerm() + " " + term.getAcronym() + " " + term.getPronounciation() + " " + term.getSource() + " " + term.getPhraseology() + " " + term.getUses());
                entryTerms.add(term);
            }
            //Append words from entry
            fulltext.append(" " + entry.getTopicOne() + " " + entry.getTopicTwo() + " " + entry.getTopicThree() + " " + entry.getDescription());
            entry.setFulltext(fulltext.toString());
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
                deleteEntry(entry);
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
            glossaryResource.setAllowedLanguages(StringHelper.splitLocales(result.getString("allowed_languages")));
        }
        else
        {
            log.error("Error in loading languages for glossary while loading glossary resource from Database. Resource ID: " + glossaryResource.getId());
            glossaryResource = null;
            return;
        }
        //Glossary Entries details
        List<GlossaryEntry> entries = getGlossaryEntries(glossaryResource.getId());
        glossaryResource.setEntries(entries);

    }

    public GlossaryResource getGlossaryResource(int resourceId) throws SQLException
    {
        Resource resource = learnweb.getResourceManager().getResource(resourceId);

        if(resource == null)
            return null;

        if(resource.getType() != ResourceType.glossary2)
        {
            log.error("Glossary resource requested but the resource is of type " + resource.getType() + "; " + BeanHelper.getRequestSummary());
            return null;
        }

        return (GlossaryResource) resource;
    }
}
