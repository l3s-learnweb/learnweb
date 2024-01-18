package de.l3s.learnweb.resource.web;

import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.ResourceType;
import de.l3s.learnweb.resource.archive.ArchiveUrl;

public class WebResource extends Resource {
    @Serial
    private static final long serialVersionUID = 8066627820924157401L;
    private static final Logger log = LogManager.getLogger(WebResource.class);

    private transient LinkedHashSet<ArchiveUrl> archiveUrls; // To store the archived URLs

    public WebResource() {
        this(ResourceType.website, ResourceService.internet);
    }

    public WebResource(ResourceService service) {
        this(ResourceType.website, service);
    }

    public WebResource(ResourceType type, ResourceService service) {
        setStorageType(StorageType.WEB);
        setType(type);
        setService(service);
    }

    protected WebResource(final WebResource other) {
        super(other);

        setArchiveUrls(new LinkedHashSet<>(other.getArchiveUrls()));
    }

    @Override
    public WebResource cloneResource() {
        return new WebResource(this);
    }

    @Override
    protected void clearCaches() {
        super.clearCaches();

        archiveUrls = null;
    }

    @Override
    public Resource save() {
        super.save();

        if (CollectionUtils.isNotEmpty(getArchiveUrls())) {
            try {
                // To copy archive versions of a resource if it exists
                Learnweb.dao().getArchiveUrlDao().insertArchiveUrl(getId(), getArchiveUrls());
            } catch (Exception e) {
                log.error("Can't save archiveUrls", e);
            }
        }

        return this;
    }

    public LinkedHashSet<ArchiveUrl> getArchiveUrls() {
        if (archiveUrls == null && getId() != 0) {
            archiveUrls = new LinkedHashSet<>(Learnweb.dao().getArchiveUrlDao().findByResourceId(getId()));
            archiveUrls.addAll(Learnweb.dao().getWaybackUrlDao().findByUrl(getUrl()));
        }

        return archiveUrls;
    }

    public void setArchiveUrls(LinkedHashSet<ArchiveUrl> archiveUrls) {
        this.archiveUrls = archiveUrls;
    }

    public HashMap<Integer, List<ArchiveUrl>> getArchiveUrlsAsYears() {
        HashMap<Integer, List<ArchiveUrl>> versions = new LinkedHashMap<>();
        for (ArchiveUrl url : archiveUrls) {
            int year = url.timestamp().getYear();
            if (!versions.containsKey(year)) {
                versions.put(year, new ArrayList<>());
            }
            versions.get(year).add(url);
        }
        return versions;
    }

    public boolean isArchived() {
        return getArchiveUrls() != null && !archiveUrls.isEmpty();
    }

    public ArchiveUrl getFirstArchivedObject() {
        return archiveUrls.getFirst();
    }

    public ArchiveUrl getLastArchivedObject() {
        return archiveUrls.getLast();
    }
}
