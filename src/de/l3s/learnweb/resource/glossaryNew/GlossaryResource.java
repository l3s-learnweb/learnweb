package de.l3s.learnweb.resource.glossaryNew;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.SERVICE;

/**
 * @author Rishita
 *
 */
public class GlossaryResource extends Resource implements Serializable
{
    private static final long serialVersionUID = 8388778401614338522L;

    private List<Locale> allowedLanguages = new ArrayList<>();
    private List<GlossaryEntry> entries = new LinkedList<>();
    private boolean deleted = false;
    private boolean clonedButNotSaved = false;

    //constructor does nothing
    public GlossaryResource()
    {
        this.setSource(SERVICE.learnweb);
        this.setLocation("Learnweb");
        this.setStorageType(Resource.LEARNWEB_RESOURCE);
        this.setType(Resource.ResourceType.glossary2);
    }

    /**
     * copy constructor
     *
     * @param glossaryResource
     */
    public GlossaryResource(GlossaryResource otherGlossaryResource)
    {
        super(otherGlossaryResource);
        setAllowedLanguages(new ArrayList<>(otherGlossaryResource.allowedLanguages));
        setDeleted(otherGlossaryResource.deleted);
        setClonedButNotPersisted(true);
        setEntries(new ArrayList<GlossaryEntry>(otherGlossaryResource.entries.size()));
        for(int i = 0; i < otherGlossaryResource.entries.size(); i++)
        {
            this.entries.add(i, otherGlossaryResource.entries.get(i).clone());
        }

    }

    public List<Locale> getAllowedLanguages()
    {
        return allowedLanguages;
    }

    public void setAllowedLanguages(List<Locale> allowedLanguages)
    {
        this.allowedLanguages = allowedLanguages;
    }

    @Override
    public GlossaryResource clone()
    {
        return new GlossaryResource(this);
    }

    @Override
    protected void postConstruct() throws SQLException
    {
        super.postConstruct();
        Learnweb.getInstance().getGlossaryManager().loadGlossaryResource(this);
    }

    @Override
    public Resource save() throws SQLException
    {
        Resource iconResource = Learnweb.getInstance().getResourceManager().getResource(200233);
        this.setThumbnail0(iconResource.getThumbnail0());
        this.setThumbnail1(iconResource.getThumbnail1());
        this.setThumbnail2(iconResource.getThumbnail2());
        this.setThumbnail3(iconResource.getThumbnail3());
        this.setThumbnail4(iconResource.getThumbnail4());
        this.setUrl(Learnweb.getInstance().getServerUrl() + "/lw/glossary/glossary.jsf?resource_id=" + this.getId());

        this.setUser(getUser()); // added by rishita to fix copy bug; does this make sense? TODO: test

        // save normal resource fields
        super.save();

        // save GlossaryResource fields
        Learnweb.getInstance().getGlossaryManager().saveGlossaryResource(this);

        return this;
    }

    public List<GlossaryEntry> getEntries()
    {
        return entries;
    }

    public void setEntries(List<GlossaryEntry> entries)
    {
        this.entries = entries;
    }

    @Override
    public boolean isDeleted()
    {
        return deleted;
    }

    @Override
    public void setDeleted(boolean deleted)
    {
        this.deleted = deleted;
    }

    public boolean isClonedButNotPersisted()
    {
        return clonedButNotSaved;
    }

    public void setClonedButNotPersisted(boolean cloned)
    {
        this.clonedButNotSaved = cloned;
    }

}
