package de.l3s.learnweb.resource.glossary;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceType;
import de.l3s.util.StringHelper;
import de.l3s.util.bean.BeanHelper;

public class GlossaryManager {
    private static final Logger log = LogManager.getLogger(GlossaryManager.class);
    private final Learnweb learnweb;

    public GlossaryManager(Learnweb learnweb) {
        this.learnweb = learnweb;
    }

    public void saveGlossaryResource(GlossaryResource resource) throws SQLException { // Called when resource is created in right-pane
        if (resource.isDeleted()) { //set resource and entries as deleted
            delete(resource);
            return;
        }
        try (PreparedStatement insertGlossary = learnweb.getConnection()
            .prepareStatement("REPLACE INTO `lw_glossary_resource`(`resource_id`, `allowed_languages`) VALUES (?, ?)")) {
            insertGlossary.setInt(1, resource.getId());
            insertGlossary.setString(2, StringHelper.join(resource.getAllowedLanguages()));
            insertGlossary.executeQuery();
        }
        if (resource.isClonedButNotPersisted()) { // after a resource has been cloned we have to persist the cloned entries
            saveEntries(resource);
        }
    }

    /**
     * To set new IDs for entries and terms which are copied.
     */
    private void saveEntries(GlossaryResource resource) throws SQLException {
        if (resource.getEntries() == null) {
            return;
        }

        for (GlossaryEntry entry : resource.getEntries()) {
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

    public void saveEntry(GlossaryEntry entry, GlossaryResource glossaryResource) throws SQLException {

        if (entry.isDeleted()) {
            deleteEntry(entry);
        } else {
            entry.setResourceId(glossaryResource.getId());
            entry.setTimestamp(new Timestamp(System.currentTimeMillis())); // update timestamp

            if (entry.getId() < 0) { // new entry
                insertEntry(entry);

                // TODO philipp: check if this can be moved to glossaryBean
                try {
                    HttpSession session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(true);
                    String sessionId = session != null ? session.getId() : null;
                    learnweb.getLogManager()
                        .log(learnweb.getUserManager().getUser(entry.getLastChangedByUserId()), Action.glossary_entry_add, glossaryResource.getGroupId(),
                            glossaryResource.getId(), Integer.toString(entry.getId()), sessionId);
                } catch (Exception e) {
                    log.error("Error in logging " + Action.glossary_entry_add + " for entry ID: " + entry.getId() + " of resource: " + entry.getResourceId());
                }
            } else { // old entry updated
                updateEntry(entry);
            }
            saveTerms(entry, glossaryResource.getGroupId());
            entry.getTerms().removeIf(GlossaryTerm::isDeleted);

            // the glossary edit form uses a working copy (clone) therefore we have to replace the original entry
            glossaryResource.getEntries().removeIf(e -> e.getId() == entry.getId());
            glossaryResource.getEntries().add(entry);
        }
    }

    private void deleteEntry(GlossaryEntry entry) throws SQLException {
        try (PreparedStatement deleteEntry = learnweb.getConnection()
            .prepareStatement("UPDATE `lw_glossary_entry` SET `deleted`=?, `last_changed_by_user_id`=? WHERE `entry_id`=?")) {
            deleteEntry.setBoolean(1, true);
            deleteEntry.setInt(2, entry.getLastChangedByUserId());
            deleteEntry.setInt(3, entry.getId());
            deleteEntry.executeQuery();
        }
        try (PreparedStatement deleteTerms = learnweb.getConnection()
            .prepareStatement("UPDATE `lw_glossary_term` SET `deleted`=?, `last_changed_by_user_id`=? WHERE `entry_id`=?")) {
            deleteTerms.setBoolean(1, true);
            deleteTerms.setInt(2, entry.getLastChangedByUserId());
            deleteTerms.setInt(3, entry.getId());
            deleteTerms.executeQuery();
        }
    }

    private void insertEntry(GlossaryEntry entry) throws SQLException {
        try (PreparedStatement insertEntry = learnweb.getConnection().prepareStatement(
            "INSERT INTO `lw_glossary_entry`(`resource_id`, `original_entry_id`, `last_changed_by_user_id`, `user_id`, `topic_one`, `topic_two`, `topic_three`, `description`, `description_pasted`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS)) {
            if (entry.getUserId() <= 0) {
                entry.setUserId(entry.getLastChangedByUserId()); // last change by userID == original user ID in insert
            }

            insertEntry.setInt(1, entry.getResourceId());
            insertEntry.setInt(2, entry.getOriginalEntryId());
            insertEntry.setInt(3, entry.getLastChangedByUserId());
            insertEntry.setInt(4, entry.getUserId());
            insertEntry.setString(5, entry.getTopicOne());
            insertEntry.setString(6, entry.getTopicTwo());
            insertEntry.setString(7, entry.getTopicThree());
            insertEntry.setString(8, entry.getDescription());
            insertEntry.setBoolean(9, entry.isDescriptionPasted());
            insertEntry.executeQuery();

            ResultSet entryInserted = insertEntry.getGeneratedKeys();
            entryInserted.next();
            entry.setId(entryInserted.getInt(1));
        }
    }

    private void updateEntry(GlossaryEntry entry) throws SQLException {
        try (PreparedStatement updateEntry = learnweb.getConnection().prepareStatement(
            "UPDATE `lw_glossary_entry` SET `topic_one`=?,`topic_two`=?,`topic_three`=?,`description`=?,`description_pasted`=?, `last_changed_by_user_id`=? WHERE `entry_id`=?")) {
            updateEntry.setString(1, entry.getTopicOne());
            updateEntry.setString(2, entry.getTopicTwo());
            updateEntry.setString(3, entry.getTopicThree());
            updateEntry.setString(4, entry.getDescription());
            updateEntry.setBoolean(5, entry.isDescriptionPasted());
            updateEntry.setInt(6, entry.getLastChangedByUserId());
            updateEntry.setInt(7, entry.getId());
            updateEntry.executeUpdate();
        }
    }

    public void saveTerms(GlossaryEntry entry, int groupId) throws SQLException {
        for (GlossaryTerm term : entry.getTerms()) {
            if (term.getId() < 0) {
                insertTerms(term, entry, groupId);
            } else {
                updateTerms(term, entry);
            }
        }
    }

    private void updateTerms(GlossaryTerm term, GlossaryEntry entry) throws SQLException {
        try (PreparedStatement termUpdate = learnweb.getConnection().prepareStatement(
            "UPDATE `lw_glossary_term` SET `entry_id`=?, `deleted`=?, `term`=?, `language`=?, `uses`=?, `pronounciation`=?, `acronym`=?, `source`=?, `phraseology`=?, `term_pasted`=?, `pronounciation_pasted`=?, `acronym_pasted`=?, `phraseology_pasted`=?, `last_changed_by_user_id`=? WHERE `term_id`=?")) {
            termUpdate.setInt(1, term.getEntryId());
            termUpdate.setBoolean(2, term.isDeleted());
            termUpdate.setString(3, term.getTerm());
            termUpdate.setString(4, term.getLanguage().toLanguageTag());
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

    public void insertTerms(GlossaryTerm term, GlossaryEntry entry, int groupId) throws SQLException {
        try (PreparedStatement termInsert = learnweb.getConnection().prepareStatement(
            "INSERT INTO `lw_glossary_term`(`entry_id`, `original_term_id`, `last_changed_by_user_id`, `user_id`, `term`, `language`, `uses`, `pronounciation`, `acronym`, `source`, `phraseology`, `term_pasted`, `pronounciation_pasted`, `acronym_pasted`, `phraseology_pasted`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS)) {
            termInsert.setInt(1, entry.getId());
            termInsert.setInt(2, term.getOriginalTermId());
            termInsert.setInt(3, entry.getLastChangedByUserId());
            termInsert.setInt(4, entry.getUserId());
            termInsert.setString(5, term.getTerm());
            termInsert.setString(6, term.getLanguage().toLanguageTag());
            termInsert.setString(7, term.getUses() == null ? "" : String.join(",", term.getUses()));
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

            HttpSession session = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(true);
            String sessionId = session != null ? session.getId() : null;
            learnweb.getLogManager()
                .log(learnweb.getUserManager().getUser(entry.getLastChangedByUserId()), Action.glossary_term_add, groupId, entry.getResourceId(),
                    Integer.toString(entry.getId()), sessionId);
        }
    }

    public List<GlossaryEntry> getGlossaryEntries(int resourceId) throws SQLException {
        List<GlossaryEntry> entries = new LinkedList<>();

        try (PreparedStatement getEntries = learnweb.getConnection()
            .prepareStatement("SELECT * FROM `lw_glossary_entry` WHERE `resource_id`=? and deleted = ?")) {
            getEntries.setInt(1, resourceId);
            getEntries.setBoolean(2, false);
            ResultSet resultEntries = getEntries.executeQuery();
            while (resultEntries.next()) {
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

    public List<GlossaryTerm> getGlossaryTerms(GlossaryEntry entry) throws SQLException {
        List<GlossaryTerm> entryTerms = new LinkedList<>();
        try (PreparedStatement getTerms = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_glossary_term` WHERE `entry_id`=? and `deleted`=?")) {
            StringBuilder fulltext = new StringBuilder();
            getTerms.setInt(1, entry.getId());
            getTerms.setBoolean(2, false);
            ResultSet terms = getTerms.executeQuery();
            while (terms.next()) {
                GlossaryTerm term = new GlossaryTerm();
                term.setEntryId(entry.getId());
                term.setId(terms.getInt("term_id"));
                term.setUserId(terms.getInt("user_id"));
                term.setTerm(terms.getString("term"));
                term.setLanguage(Locale.forLanguageTag(terms.getString("language")));
                term.setUses(terms.getString("uses").isEmpty() ? new ArrayList<>() : Arrays.asList(terms.getString("uses").split(",")));
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
                fulltext.append(term.getTerm()).append(" ")
                    .append(term.getAcronym()).append(" ")
                    .append(term.getPronounciation()).append(" ")
                    .append(term.getSource()).append(" ")
                    .append(term.getPhraseology()).append(" ")
                    .append(term.getUses());
                entryTerms.add(term);
            }
            //Append words from entry
            fulltext.append(" ")
                .append(entry.getTopicOne()).append(" ")
                .append(entry.getTopicTwo()).append(" ")
                .append(entry.getTopicThree()).append(" ")
                .append(entry.getDescription());
            entry.setFulltext(fulltext.toString());
        }
        return entryTerms;
    }

    public void delete(GlossaryResource resource) throws SQLException {
        if (resource.getEntries() != null && !resource.getEntries().isEmpty()) {
            for (GlossaryEntry entry : resource.getEntries()) {
                entry.setDeleted(true);
                deleteEntry(entry);
            }
        }
    }

    /**
     * loads glossary metadata into glossaryResource.
     */
    public void loadGlossaryResource(GlossaryResource glossaryResource) throws SQLException {
        PreparedStatement getGlossary = learnweb.getConnection().prepareStatement("SELECT * FROM `lw_glossary_resource` WHERE `resource_id`=?");
        getGlossary.setInt(1, glossaryResource.getId());
        ResultSet result = getGlossary.executeQuery();
        if (result.next()) {
            glossaryResource.setAllowedLanguages(StringHelper.splitLocales(result.getString("allowed_languages")));
        } else {
            log.error("Error loading glossary languages from database: {}", glossaryResource.getId(), new Exception());
            return;
        }
        //Glossary Entries details
        List<GlossaryEntry> entries = getGlossaryEntries(glossaryResource.getId());
        glossaryResource.setEntries(entries);
    }

    public GlossaryResource getGlossaryResource(int resourceId) throws SQLException {
        Resource resource = learnweb.getResourceManager().getResource(resourceId);
        return getGlossaryResource(resource);
    }

    public GlossaryResource getGlossaryResource(Resource resource) {
        if (resource == null) {
            return null;
        }

        if (resource.getType() != ResourceType.glossary2) {
            log.error("Glossary resource requested but the resource is of type " + resource.getType() + "; " + BeanHelper.getRequestSummary());
            return null;
        }

        return (GlossaryResource) resource;
    }
}
