package de.l3s.learnweb.resource.glossary;

import java.io.Serial;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.ResourceType;
import de.l3s.util.StringHelper;

/**
 * @author Rishita
 */
public class GlossaryResource extends Resource {
    @Serial
    private static final long serialVersionUID = 8388778401614338522L;

    private ArrayList<Locale> allowedLanguages = new ArrayList<>();
    private LinkedList<GlossaryEntry> entries = new LinkedList<>();
    private boolean clonedButNotSaved = false;

    public GlossaryResource() {
        super(StorageType.LEARNWEB, ResourceType.glossary, ResourceService.learnweb);
    }

    /**
     * copy constructor.
     */
    protected GlossaryResource(GlossaryResource other) {
        super(other);

        this.allowedLanguages = new ArrayList<>(other.allowedLanguages);
        this.clonedButNotSaved = true;

        other.getEntries().forEach(entry -> this.entries.add(new GlossaryEntry(entry)));
    }

    @Override
    public GlossaryResource cloneResource() {
        return new GlossaryResource(this);
    }

    public ArrayList<Locale> getAllowedLanguages() {
        return allowedLanguages;
    }

    public void setAllowedLanguages(ArrayList<Locale> allowedLanguages) {
        this.allowedLanguages = allowedLanguages;
    }

    @Override
    protected void postConstruct() {
        super.postConstruct();

        Optional<String> allowedLanguages = Learnweb.dao().getGlossaryDao().findGlossaryResourceAllowedLanguages(getId());

        if (allowedLanguages.isPresent()) {
            setAllowedLanguages(StringHelper.splitLocales(allowedLanguages.get()));
        } else {
            LogManager.getLogger(GlossaryResource.class).error("Error loading glossary languages from database: {}", getId(), new Exception());
            return;
        }

        // Glossary Entries details
        List<GlossaryEntry> entries = Learnweb.dao().getGlossaryEntryDao().findByResourceId(getId());
        this.entries.addAll(entries);
    }

    @Override
    public Resource save() {
        super.save();

        // save GlossaryResource fields
        Learnweb.dao().getGlossaryDao().save(this);
        return this;
    }

    public LinkedList<GlossaryEntry> getEntries() {
        return entries;
    }

    public void setEntries(LinkedList<GlossaryEntry> entries) {
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
}
