package de.l3s.learnweb.rm;

import java.io.Serializable;

import org.apache.log4j.Logger;

public class ExtendedMetadataSearchFilters implements Serializable
{
    private static final long serialVersionUID = 9091606684801652063L;
    final static Logger log = Logger.getLogger(ExtendedMetadataSearchFilters.class);

    private Integer resultsPerPage = 8;
    private Integer resultsPerGroup = 2;
    private String[] filterAuthors;
    private String[] filterMtypes;
    private String[] filterSources;
    private String[] filterTargets;
    private String[] filterPurposes;
    private String[] filterLangs;
    private String[] filterLevels;

    public ExtendedMetadataSearchFilters()
    {

    }

    //setters and getters

    public Integer getResultsPerPage()
    {
        return resultsPerPage;
    }

    public void setResultsPerPage(Integer resultsPerPage)
    {
        this.resultsPerPage = resultsPerPage;
    }

    public Integer getResultsPerGroup()
    {
        return resultsPerGroup;
    }

    public void setResultsPerGroup(Integer resultsPerGroup)
    {
        this.resultsPerGroup = resultsPerGroup;
    }

    public String[] getFilterAuthors()
    {
        return filterAuthors;
    }

    public void setFilterAuthors(String[] filterAuthors)
    {
        this.filterAuthors = filterAuthors;
    }

    public String[] getFilterMtypes()
    {
        return filterMtypes;
    }

    public void setFilterMtypes(String[] filterMtypes)
    {
        this.filterMtypes = filterMtypes;
    }

    public String[] getFilterSources()
    {
        return filterSources;
    }

    public void setFilterSources(String[] filterSources)
    {
        this.filterSources = filterSources;
    }

    public String[] getFilterTargets()
    {
        return filterTargets;
    }

    public void setFilterTargets(String[] filterTargets)
    {
        this.filterTargets = filterTargets;
    }

    public String[] getFilterPurposes()
    {
        return filterPurposes;
    }

    public void setFilterPurposes(String[] filterPurposes)
    {
        this.filterPurposes = filterPurposes;
    }

    public String[] getFilterLangs()
    {
        return filterLangs;
    }

    public void setFilterLangs(String[] filterLangs)
    {
        this.filterLangs = filterLangs;
    }

    public String[] getFilterLevels()
    {
        return filterLevels;
    }

    public void setFilterLevels(String[] filterLevels)
    {
        this.filterLevels = filterLevels;
    }

    //reset all filters to null. called when reset button is clicked 

    public void resetFilters()
    {
        this.filterAuthors = null;
        this.filterMtypes = null;
        this.filterSources = null;
        this.filterTargets = null;
        this.filterPurposes = null;
        this.filterLangs = null;
        this.filterLevels = null;
    }

}
