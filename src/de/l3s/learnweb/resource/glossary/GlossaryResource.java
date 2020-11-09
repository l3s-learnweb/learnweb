package de.l3s.learnweb.resource.glossary;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.ResourceType;

/**
 * @author Rishita
 */
public class GlossaryResource extends Resource {
    private static final long serialVersionUID = 8388778401614338522L;

    private List<Locale> allowedLanguages = new ArrayList<>();
    private List<GlossaryEntry> entries = new LinkedList<>();
    private boolean clonedButNotSaved = false;

    public GlossaryResource() {
        this.setSource(ResourceService.learnweb);
        this.setLocation("Learnweb");
        this.setStorageType(Resource.LEARNWEB_RESOURCE);
        this.setType(ResourceType.glossary2);
    }

    /**
     * copy constructor.
     */
    public GlossaryResource(GlossaryResource other) {
        super(other);
        setAllowedLanguages(new ArrayList<>(other.allowedLanguages));
        setClonedButNotPersisted(true);

        setEntries(new ArrayList<>(other.entries.size()));
        for (int i = 0, len = other.entries.size(); i < len; i++) {
            this.entries.add(i, other.entries.get(i).clone());
        }
    }

    public List<Locale> getAllowedLanguages() {
        return allowedLanguages;
    }

    public void setAllowedLanguages(List<Locale> allowedLanguages) {
        this.allowedLanguages = allowedLanguages;
    }

    @Override
    public GlossaryResource clone() {
        return new GlossaryResource(this);
    }

    @Override
    protected void postConstruct() throws SQLException {
        super.postConstruct();
        Learnweb.getInstance().getGlossaryManager().loadGlossaryResource(this);
    }

    @Override
    public Resource save() throws SQLException {
        Resource iconResource = Learnweb.getInstance().getResourceManager().getResource(200233); // TODO @astappiev: find a better image, load it from resource folder
        this.setThumbnail0(iconResource.getThumbnail0());
        this.setThumbnail1(iconResource.getThumbnail1());
        this.setThumbnail2(iconResource.getThumbnail2());
        this.setThumbnail3(iconResource.getThumbnail3());
        this.setThumbnail4(iconResource.getThumbnail4());

        this.setUser(getUser()); // added by Rishita to fix copy bug; does this make sense? TODO @astappiev: test

        // save normal resource fields
        super.save();

        // save GlossaryResource fields
        Learnweb.getInstance().getGlossaryManager().saveGlossaryResource(this);

        return this;
    }

    public List<GlossaryEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<GlossaryEntry> entries) {
        this.entries = entries;
    }

    public boolean isClonedButNotPersisted() {
        return clonedButNotSaved;
    }

    public void setClonedButNotPersisted(boolean cloned) {
        this.clonedButNotSaved = cloned;
    }

    /**
     * @return a flat table representation of the tree like glossary structure
     */
    public List<GlossaryTableView> getGlossaryTableView() {
        ArrayList<GlossaryTableView> tableView = new ArrayList<>(getEntries().size());

        for (GlossaryEntry entry : getEntries()) {
            for (GlossaryTerm term : entry.getTerms()) {
                tableView.add(new GlossaryTableView(entry, term));
            }
        }
        return tableView;
    }

    @Override
    public String getUrl() {
        return Learnweb.getInstance().getServerUrl() + "/lw/resource.jsf?resource_id=" + getId();
    }
}
