package de.l3s.learnweb.rm;

import java.io.Serializable;
import java.util.List;

import org.apache.log4j.Logger;

public class ExtendedMetadataSearchFilters implements Serializable
{
    private static final long serialVersionUID = 9091606684801652063L;
    final static Logger log = Logger.getLogger(ExtendedMetadataSearchFilters.class);

    private List<String> authors;
    private List<String> mtypes;
    private List<String> sources;
    private List<String> targets;
    private List<String> purposes;
    private List<String> langs;
    private List<String> levels;

    public ExtendedMetadataSearchFilters()
    {

    }

    public List<String> getAuthors()
    {
        return authors;
    }

    public void setAuthors(List<String> authors)
    {
        this.authors = authors;
    }

    public List<String> getMtypes()
    {
        return mtypes;
    }

    public void setMtypes(List<String> mtypes)
    {
        this.mtypes = mtypes;
    }

    public List<String> getSources()
    {
        return sources;
    }

    public void setSources(List<String> sources)
    {
        this.sources = sources;
    }

    public List<String> getTargets()
    {
        return targets;
    }

    public void setTargets(List<String> targets)
    {
        this.targets = targets;
    }

    public List<String> getPurposes()
    {
        return purposes;
    }

    public void setPurposes(List<String> purposes)
    {
        this.purposes = purposes;
    }

    public List<String> getLangs()
    {
        return langs;
    }

    public void setLangs(List<String> langs)
    {
        this.langs = langs;
    }

    public List<String> getLevels()
    {
        return levels;
    }

    public void setLevels(List<String> levels)
    {
        this.levels = levels;
    }

}
