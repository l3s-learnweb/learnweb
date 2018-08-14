package de.l3s.learnweb.resource.glossaryNew;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.Resource;

/**
 * @author Rishita
 *
 */
public class GlossaryResource extends Resource implements Serializable
{
    /**
     *
     */
    private static final long serialVersionUID = 8388778401614338522L;
    private List<Locale> allowedLanguages = new LinkedList<>();
    private List<GlossaryEntry> entries = new LinkedList<>();
    private boolean deleted = false;

    //constructor does nothing
    public GlossaryResource()
    {

    }

    /**
     * copy constructor
     *
     * @param glossaryResource
     */
    public GlossaryResource(GlossaryResource otherGlossaryResource)
    {
        super(otherGlossaryResource);
        setAllowedLanguages(otherGlossaryResource.getAllowedLanguages());
        setEntries(otherGlossaryResource.getEntries());
        setDeleted(otherGlossaryResource.isDeleted());

    }

    public GlossaryResource(Resource resource) throws IllegalAccessException, InvocationTargetException
    {
        super(resource);
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
        // save normal resource fields
        super.save();

        // save GlossaryResource fields
        Learnweb.getInstance().getGlossaryManager().saveGlossaryResource(this);

        return this;
    }

    public List<Locale> getAllowedLanguages()
    {
        return allowedLanguages;
    }

    public void setAllowedLanguages(List<Locale> allowedLanguages)
    {
        this.allowedLanguages = allowedLanguages;
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

}
