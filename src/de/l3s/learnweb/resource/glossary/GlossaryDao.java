package de.l3s.learnweb.resource.glossary;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.jdbi.v3.sqlobject.CreateSqlObject;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import de.l3s.learnweb.resource.FileDao;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDao;
import de.l3s.learnweb.resource.ResourceType;
import de.l3s.util.StringHelper;
import de.l3s.util.bean.BeanHelper;

public interface GlossaryDao extends SqlObject, Serializable {

    @CreateSqlObject
    ResourceDao getResourceDao();

    @CreateSqlObject
    FileDao getFileDao();

    @CreateSqlObject
    GlossaryEntryDao getGlossaryEntryDao();

    @CreateSqlObject
    GlossaryTermDao getGlossaryTermDao();

    @SqlUpdate("INSERT INTO lw_glossary_resource(resource_id, allowed_languages) VALUES (?, ?) ON DUPLICATE KEY UPDATE allowed_languages = VALUES(allowed_languages)")
    void insertGlossaryResource(int resourceId, String allowedLanguages);

    @SqlQuery("SELECT allowed_languages FROM lw_glossary_resource WHERE resource_id = ?")
    Optional<String> findGlossaryResourceAllowedLanguages(int resourceId);

    default Optional<GlossaryResource> findResourceById(int resourceId) {
        return convertToGlossaryResource(getResourceDao().findById(resourceId).orElse(null));
    }

    default Optional<GlossaryResource> convertToGlossaryResource(Resource resource) {
        if (resource == null) {
            return Optional.empty();
        }

        if (resource.getType() != ResourceType.glossary) {
            LogManager.getLogger(GlossaryDao.class)
                .error("Glossary resource requested but the resource is of type {}; {}", resource.getType(), BeanHelper.getRequestSummary());
            return Optional.empty();
        }

        return Optional.of((GlossaryResource) resource);
    }

    default List<GlossaryResource> findByOwnerIds(List<Integer> userIds) {
        List<Resource> resources = getResourceDao().findByOwnerIdsAndType(userIds, ResourceType.glossary);

        // convert to glossary resource but filter resources that could not be converted
        return resources.stream().map(this::convertToGlossaryResource).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
    }

    default void save(GlossaryResource resource) { // Called when resource is created in right-pane
        if (resource.isDeleted()) { //set resource and entries as deleted
            if (resource.getEntries() != null && !resource.getEntries().isEmpty()) {
                for (GlossaryEntry entry : resource.getEntries()) {
                    getGlossaryEntryDao().deleteSoft(entry, entry.getLastChangedByUserId());
                }
            }
            return;
        }

        insertGlossaryResource(resource.getId(), StringHelper.join(resource.getAllowedLanguages()));

        if (resource.isClonedButNotPersisted()) { // after a resource has been cloned we have to persist the cloned entries
            if (resource.getEntries() != null) {
                for (GlossaryEntry entry : resource.getEntries()) {
                    entry.setUserId(resource.getUserId()); //User ID of user who created the entry
                    entry.setDeleted(resource.isDeleted());
                    entry.setLastChangedByUserId(resource.getUserId());
                    entry.setResourceId(resource.getId());
                    entry.getTerms().forEach(term -> term.setId(0));
                    saveEntry(entry);
                }
            }

            resource.setClonedButNotPersisted(false); //Required to reset it if it was set prior to this.
        }
    }

    default void saveEntry(GlossaryEntry entry) {
        entry.setPicturesCount(entry.getPictures().size());
        getGlossaryEntryDao().save(entry);

        saveTerms(entry);
        getFileDao().insertGlossaryEntryFiles(entry, entry.getPictures());
    }

    default void saveTerms(GlossaryEntry entry) {
        for (GlossaryTerm term : entry.getTerms()) {
            if (term.getId() == 0) {
                term.setEntryId(entry.getId());
                term.setUserId(entry.getUserId());
            }

            term.setLastChangedByUserId(entry.getLastChangedByUserId());
            getGlossaryTermDao().save(term);
        }
        entry.getTerms().removeIf(GlossaryTerm::isDeleted);
    }
}
