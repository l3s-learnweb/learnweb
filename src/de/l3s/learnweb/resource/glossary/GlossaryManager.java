package de.l3s.learnweb.resource.glossary;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.Handle;
import org.omnifaces.util.Faces;

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

        try (Handle handle = learnweb.openHandle()) {
            handle.execute("REPLACE INTO `lw_glossary_resource`(`resource_id`, `allowed_languages`) VALUES (?, ?)",
                resource.getId(), StringHelper.join(resource.getAllowedLanguages()));
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

                // TODO @kemkes: check if this can be moved to glossaryBean
                try {
                    learnweb.getLogManager()
                        .log(learnweb.getUserManager().getUser(entry.getLastChangedByUserId()), Action.glossary_entry_add, glossaryResource.getGroupId(),
                            glossaryResource.getId(), Integer.toString(entry.getId()), Faces.getSessionId());
                } catch (Exception e) {
                    log.error("Error in logging " + Action.glossary_entry_add + " for entry ID: " + entry.getId() + " of resource: " + entry.getResourceId());
                }
            } else { // old entry updated
                updateEntry(entry);
            }
            saveTerms(entry, glossaryResource.getGroupId());
            entry.getTerms().removeIf(GlossaryTerm::isDeleted);

            // the glossary edit form uses a working copy (clone) therefore we have to replace the original entry
            // FIXME: this line produces ConcurrentModificationException
            glossaryResource.getEntries().removeIf(e -> e.getId() == entry.getId());
            glossaryResource.getEntries().add(entry);
        }
    }

    private void deleteEntry(GlossaryEntry entry) {
        try (Handle handle = learnweb.openHandle()) {
            handle.execute("UPDATE `lw_glossary_entry` SET `deleted` = ?, `last_changed_by_user_id` = ? WHERE `entry_id` = ?",
                true, entry.getLastChangedByUserId(), entry.getId());

            handle.execute("UPDATE `lw_glossary_term` SET `deleted` = ?, `last_changed_by_user_id` = ? WHERE `entry_id` = ?",
                true, entry.getLastChangedByUserId(), entry.getId());
        }
    }

    private void insertEntry(GlossaryEntry entry) {
        if (entry.getUserId() <= 0) {
            entry.setUserId(entry.getLastChangedByUserId()); // last change by userID == original user ID in insert
        }

        try (Handle handle = learnweb.openHandle()) {
            Integer entryId = handle.createUpdate("INSERT INTO `lw_glossary_entry`(`resource_id`, `original_entry_id`, `last_changed_by_user_id`, "
                + "`user_id`, `topic_one`, `topic_two`, `topic_three`, `description`, `description_pasted`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")
                .bind(0, entry.getResourceId())
                .bind(1, entry.getOriginalEntryId())
                .bind(2, entry.getLastChangedByUserId())
                .bind(3, entry.getUserId())
                .bind(4, entry.getTopicOne())
                .bind(5, entry.getTopicTwo())
                .bind(6, entry.getTopicThree())
                .bind(7, entry.getDescription())
                .bind(8, entry.isDescriptionPasted())
                .executeAndReturnGeneratedKeys().mapTo(Integer.class).one();

            entry.setId(entryId);
        }
    }

    private void updateEntry(GlossaryEntry entry) {
        try (Handle handle = learnweb.openHandle()) {
            handle.execute("UPDATE `lw_glossary_entry` SET `topic_one`=?,`topic_two`=?,`topic_three`=?,`description`=?,`description_pasted`=?, `last_changed_by_user_id`=? WHERE `entry_id`=?",
                entry.getTopicOne(), entry.getTopicTwo(), entry.getTopicThree(), entry.getDescription(), entry.isDescriptionPasted(), entry.getLastChangedByUserId(), entry.getId());
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

    private void updateTerms(GlossaryTerm term, GlossaryEntry entry) {
        try (Handle handle = learnweb.openHandle()) {
            handle.createUpdate("UPDATE `lw_glossary_term` SET `entry_id`=?, `deleted`=?, `term`=?, `language`=?, `uses`=?, `pronounciation`=?, `acronym`=?, "
                + "`source`=?, `phraseology`=?, `term_pasted`=?, `pronounciation_pasted`=?, `acronym_pasted`=?, `phraseology_pasted`=?, `last_changed_by_user_id`=? WHERE `term_id`=?")
                .bind(0, term.getEntryId())
                .bind(1, term.isDeleted())
                .bind(2, term.getTerm())
                .bind(3, term.getLanguage().toLanguageTag())
                .bind(4, String.join(","))
                .bind(5, term.getUses())
                .bind(6, term.getPronounciation())
                .bind(7, term.getAcronym())
                .bind(8, term.getSource())
                .bind(9, term.getPhraseology())
                .bind(10, term.isTermPasted())
                .bind(11, term.isPronounciationPasted())
                .bind(12, term.isAcronymPasted())
                .bind(13, term.isPhraseologyPasted())
                .bind(14, entry.getLastChangedByUserId())
                .bind(15, term.getId())
                .execute();
        }

        //set term
        term.setLastChangedByUserId(entry.getLastChangedByUserId());
    }

    public void insertTerms(GlossaryTerm term, GlossaryEntry entry, int groupId) throws SQLException {
        try (Handle handle = learnweb.openHandle()) {
            Integer termId = handle.createUpdate("INSERT INTO `lw_glossary_term`(`entry_id`, `original_term_id`, `last_changed_by_user_id`, `user_id`, "
                + "`term`, `language`, `uses`, `pronounciation`, `acronym`, `source`, `phraseology`, `term_pasted`, `pronounciation_pasted`, `acronym_pasted`, "
                + "`phraseology_pasted`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
                .bind(0, entry.getId())
                .bind(1, term.getOriginalTermId())
                .bind(2, entry.getLastChangedByUserId())
                .bind(3, entry.getUserId())
                .bind(4, term.getTerm())
                .bind(5, term.getLanguage().toLanguageTag())
                .bind(6, term.getUses() == null ? "" : String.join(","))
                .bind(7, term.getUses())
                .bind(8, term.getPronounciation())
                .bind(9, term.getAcronym())
                .bind(10, term.getSource())
                .bind(11, term.getPhraseology())
                .bind(12, term.isTermPasted())
                .bind(13, term.isPronounciationPasted())
                .bind(14, term.isAcronymPasted())
                .bind(15, term.isPhraseologyPasted())
                .executeAndReturnGeneratedKeys().mapTo(Integer.class).one();

            term.setId(termId);
            term.setLastChangedByUserId(entry.getLastChangedByUserId());
            term.setUserId(entry.getUserId());

            learnweb.getLogManager()
                .log(learnweb.getUserManager().getUser(entry.getLastChangedByUserId()), Action.glossary_term_add, groupId, entry.getResourceId(),
                    Integer.toString(entry.getId()), Faces.getSessionId());
        }
    }

    public List<GlossaryEntry> getGlossaryEntries(int resourceId) {
        try (Handle handle = learnweb.openHandle()) {
            return handle.select("SELECT * FROM `lw_glossary_entry` WHERE `resource_id` = ? and deleted = 0", resourceId).map((rs, ctx) -> {
                GlossaryEntry entry = new GlossaryEntry();
                entry.setDeleted(false);
                entry.setResourceId(resourceId);
                entry.setId(rs.getInt("entry_id"));
                entry.setOriginalEntryId(rs.getInt("original_entry_id"));
                entry.setUserId(rs.getInt("user_id"));
                entry.setTopicOne(rs.getString("topic_one"));
                entry.setTopicTwo(rs.getString("topic_two"));
                entry.setTopicThree(rs.getString("topic_three"));
                entry.setDescription(rs.getString("description"));
                entry.setDescriptionPasted(rs.getBoolean("description_pasted"));
                entry.setTimestamp(rs.getTimestamp("timestamp"));

                //get terms for given entry
                entry.setTerms(getGlossaryTerms(entry));
                return entry;
            }).list();
        }
    }

    public List<GlossaryTerm> getGlossaryTerms(GlossaryEntry entry) {
        StringBuilder fulltext = new StringBuilder();
        List<GlossaryTerm> entryTerms = new ArrayList<>();

        try (Handle handle = learnweb.openHandle()) {
            handle.select("SELECT * FROM `lw_glossary_term` WHERE `entry_id` = ? and `deleted` = 0", entry.getId()).map((rs, ctx) -> {
                GlossaryTerm term = new GlossaryTerm();
                term.setEntryId(entry.getId());
                term.setId(rs.getInt("term_id"));
                term.setUserId(rs.getInt("user_id"));
                term.setTerm(rs.getString("term"));
                term.setLanguage(Locale.forLanguageTag(rs.getString("language")));
                term.setUses(rs.getString("uses").isEmpty() ? new ArrayList<>() : Arrays.asList(rs.getString("uses").split(",")));
                term.setPronounciation(rs.getString("pronounciation"));
                term.setAcronym(rs.getString("acronym"));
                term.setSource(rs.getString("source"));
                term.setPhraseology(rs.getString("phraseology"));
                term.setTimestamp(rs.getTimestamp("timestamp"));
                term.setTermPasted(rs.getBoolean("term_pasted"));
                term.setPronounciationPasted(rs.getBoolean("pronounciation_pasted"));
                term.setAcronymPasted(rs.getBoolean("acronym_pasted"));
                term.setPhraseologyPasted(rs.getBoolean("phraseology_pasted"));

                // Append fulltext with all words from term
                fulltext.append(term.getTerm()).append(" ")
                    .append(term.getAcronym()).append(" ")
                    .append(term.getPronounciation()).append(" ")
                    .append(term.getSource()).append(" ")
                    .append(term.getPhraseology()).append(" ")
                    .append(term.getUses());

                entryTerms.add(term);
                return term;
            });
        }

        //Append words from entry
        fulltext.append(" ")
            .append(entry.getTopicOne()).append(" ")
            .append(entry.getTopicTwo()).append(" ")
            .append(entry.getTopicThree()).append(" ")
            .append(entry.getDescription());
        entry.setFulltext(fulltext.toString());

        return entryTerms;
    }

    public void delete(GlossaryResource resource) {
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
    public void loadGlossaryResource(GlossaryResource glossaryResource) {
        try (Handle handle = learnweb.openHandle()) {
            Optional<String> allowedLanguages = handle.createQuery("SELECT allowed_languages FROM `lw_glossary_resource` WHERE `resource_id`=?")
                .bind(0, glossaryResource.getId()).mapTo(String.class).findOne();

            if (allowedLanguages.isPresent()) {
                glossaryResource.setAllowedLanguages(StringHelper.splitLocales(allowedLanguages.get()));
            } else {
                log.error("Error loading glossary languages from database: {}", glossaryResource.getId(), new Exception());
                return;
            }
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

    public List<GlossaryResource> getGlossaryResourcesByUserId(List<Integer> userIds) throws SQLException {
        List<Resource> resources = learnweb.getResourceManager().getResourcesByUserIdAndType(userIds, ResourceType.glossary2);

        // convert to glossary resource but filter resources that could not be converted
        return resources.stream().map(this::getGlossaryResource).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
