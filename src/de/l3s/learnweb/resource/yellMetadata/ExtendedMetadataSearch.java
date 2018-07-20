package de.l3s.learnweb.resource.yellMetadata;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.resource.AbstractPaginator;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDecorator;
import de.l3s.learnweb.resource.ResourceManager;
import de.l3s.learnweb.user.User;

public class ExtendedMetadataSearch extends ApplicationBean implements Serializable
{

    //populate search filter options (from MetadataSearchFilter.java) 
    //set selected values for each metadata search filter 

    private static final long serialVersionUID = 3635253409650813764L;
    private final static Logger log = Logger.getLogger(ExtendedMetadataSearch.class);

    private ExtendedMetadataSearchFilters mFilters = new ExtendedMetadataSearchFilters();
    private FilterPaginator filterPaginator;

    private Integer resultsPerPage = 8;
    private Integer resultsPerGroup = 2;

    private String sorting;
    private String groupField = "";

    protected long totalResults = -1;
    private int userId;
    private List<ResourceDecorator> results = new LinkedList<>();

    public ExtendedMetadataSearch(User user)
    {

        this.userId = user == null ? 0 : user.getId();

    }

    public FilterPaginator getUnFilteredResults(List<Resource> groupResources)
    {
        FilterPaginator ufPaginator = new FilterPaginator(this);
        setTotalResults(groupResources.size());
        this.results = convertToFinalResults(groupResources);
        return ufPaginator;
    }

    public FilterPaginator getCatFilterResults(List<Resource> gResources, String catName, String catLevel) throws SQLException
    {

        FilterPaginator cPaginator = new FilterPaginator(this);

        List<Resource> cFinalResults = new ArrayList<>();

        if(gResources.size() > 0)
        {
            if(catLevel.equalsIgnoreCase("0"))
            {
                cFinalResults = gResources;
            }
            else
            {
                for(Resource gResource : gResources)
                {
                    String result = filterByCategory(gResource.getId(), catName, catLevel);

                    if(result.equalsIgnoreCase("yes"))
                    {
                        cFinalResults.add(gResource);
                    }
                }
            }
        }

        setTotalResults(cFinalResults.size());

        this.results = convertToFinalResults(cFinalResults);

        return cPaginator;
    }

    public String filterByCategory(int resourceId, String cname, String stype) throws SQLException
    {

        String result = "no";
        CategoryManager cm = Learnweb.getInstance().getCategoryManager();

        List<CategoryResource> cr = cm.getCategoryResourcesByResourceId(resourceId);

        if(cr.size() > 0)
        {
            if(stype.equals("1"))
            {
                int topcatId = cm.getCategoryTopByName(cname);
                for(CategoryResource aCr : cr)
                {
                    if(topcatId == aCr.getTopcatId())
                    {
                        result = "yes";
                    }
                }
            }
            else if(stype.equals("2"))
            {
                int midcat_id = cm.getCategoryMiddleByName(cname);
                for(CategoryResource aCr : cr)
                {
                    if(midcat_id == aCr.getMidcatId())
                    {
                        result = "yes";
                    }
                }
            }
            else
            {
                int botcat_id = cm.getCategoryBottomByName(cname);
                for(CategoryResource aCr : cr)
                {
                    if(botcat_id == aCr.getBotcatId())
                    {
                        result = "yes";
                    }
                }
            }
        }

        return result;
    }

    public FilterPaginator getFilterResults(int groupId, int folderId, ExtendedMetadataSearchFilters emFilters, User user)
    {
        FilterPaginator fPaginator = new FilterPaginator(this);

        //first get resources by groupId
        List<Resource> filterResults = filterResultsByGroupId(groupId);

        //filter by author if not null
        if(emFilters.getFilterAuthors().length > 0)
        {
            filterResults = filterResultsByAuthors(emFilters.getFilterAuthors(), filterResults);

            String sAuthors = "";
            for(int i = 0; i < emFilters.getFilterAuthors().length; i++)
            {
                sAuthors += emFilters.getFilterAuthors()[i] + ";";
            }

            log(Action.group_metadata_search, groupId, 0, "filterByAuthors: " + sAuthors);
        }

        //filter by language if not null
        if(emFilters.getFilterLangs().length > 0)
        {
            filterResults = filterResultsByLanguages(emFilters.getFilterLangs(), filterResults);

            String sLangs = "";
            for(int i = 0; i < emFilters.getFilterLangs().length; i++)
            {
                sLangs += emFilters.getFilterLangs()[i] + ";";
            }

            log(Action.group_metadata_search, groupId, 0, "filterByLangs: " + sLangs);
        }

        //filter by level if not null
        if(emFilters.getFilterLevels().length > 0)
        {
            try
            {
                filterResults = filterResultsByLevels(emFilters.getFilterLevels(), filterResults);

                String sLevels = "";
                for(int i = 0; i < emFilters.getFilterLevels().length; i++)
                {
                    sLevels += emFilters.getFilterLevels()[i] + ";";
                }

                log(Action.group_metadata_search, groupId, 0, "filterByLangLevels: " + sLevels);
            }
            catch(SQLException e)
            {
                // TODO Auto-generated catch block
                log.debug("filtering by levels failed");
            }
        }

        //filter by media source if not null
        if((emFilters.getFilterSources().length > 0) && (emFilters.getFilterSources() != null))
        {
            filterResults = filterResultsByMsources(emFilters.getFilterSources(), filterResults);

            String smSources = "";
            for(int i = 0; i < emFilters.getFilterSources().length; i++)
            {
                smSources += emFilters.getFilterSources()[i] + ";";
            }

            log(Action.group_metadata_search, groupId, 0, "filterByMediaSources: " + smSources);
        }

        //filter by media type if not null
        if(emFilters.getFilterMtypes().length > 0)

        {
            filterResults = filterResultsByMtypes(emFilters.getFilterMtypes(), filterResults);

            String smTypes = "";
            for(int i = 0; i < emFilters.getFilterMtypes().length; i++)
            {
                smTypes += emFilters.getFilterMtypes()[i] + ";";
            }

            log(Action.group_metadata_search, groupId, 0, "filterByMediaTypes: " + smTypes);
        }

        //filter by audience if not null
        if(emFilters.getFilterTargets().length > 0)
        {
            try
            {
                filterResults = filterResultsByTargets(emFilters.getFilterTargets(), filterResults);

                String sTargets = "";
                for(int i = 0; i < emFilters.getFilterTargets().length; i++)
                {
                    sTargets += emFilters.getFilterTargets()[i] + ";";
                }

                log(Action.group_metadata_search, groupId, 0, "filterByAudiences: " + sTargets);
            }
            catch(SQLException e)
            {
                // TODO Auto-generated catch block
                log.debug("filtering by targets failed");
            }
        }

        //filter by purpose if not null
        if(emFilters.getFilterPurposes().length > 0)
        {
            try
            {
                filterResults = filterResultsByPurposes(emFilters.getFilterPurposes(), filterResults);

                String sPurposes = "";
                for(int i = 0; i < emFilters.getFilterPurposes().length; i++)
                {
                    sPurposes += emFilters.getFilterPurposes()[i] + ";";
                }

                log(Action.group_metadata_search, groupId, 0, "filterByPurposes: " + sPurposes);
            }
            catch(SQLException e)
            {
                // TODO Auto-generated catch block
                log.debug("filtering by purposes failed");
            }
        }

        //convert the final filterResults into list of resource decorator
        setTotalResults(filterResults.size());
        this.results = convertToFinalResults(filterResults);

        return fPaginator;

    }

    public List<ResourceDecorator> convertToFinalResults(List<Resource> fResults)
    {
        //we change the list of resources to list of resource decorator... 
        List<ResourceDecorator> temp = new LinkedList<>();
        for(Resource fResult : fResults)
        {
            ResourceDecorator rd = new ResourceDecorator(fResult);
            temp.add(rd);
        }

        return temp;
    }

    public List<Resource> filterResultsByGroupId(int groupId)
    {
        ResourceManager rsm = Learnweb.getInstance().getResourceManager();
        List<Resource> fResults = new ArrayList<>();
        if(groupId > 0)
        {
            try
            {
                fResults = rsm.getResourcesByGroupId(groupId);
            }
            catch(SQLException e)
            {
                // TODO Auto-generated catch block
                log.fatal("getting resources by given groupId failed: " + groupId);
            }
        }
        return fResults;
    }

    public List<Resource> filterResultsByAuthors(String[] authors, List<Resource> workingCopy)
    {
        List<Resource> fResults = new ArrayList<>();

        for(Resource r : workingCopy)
        {
            for(final String author : authors)
            {
                if(r.getAuthor() != null)
                {
                    if(r.getAuthor().equalsIgnoreCase(author.toLowerCase()))
                    {
                        fResults.add(r);
                    }
                }
            }
        }
        return fResults;
    }

    public List<Resource> filterResultsByLanguages(String[] langs, List<Resource> workingCopy)
    {
        List<Resource> fResults = new ArrayList<>();

        for(Resource r : workingCopy)
        {
            for(final String lang : langs)
            {
                if(r.getLanguage() != null)
                {
                    if(r.getLanguage().equalsIgnoreCase(lang))
                    {
                        fResults.add(r);
                    }
                }
            }
        }

        return fResults;
    }

    public List<Resource> filterResultsByMsources(String[] msources, List<Resource> workingCopy)
    {
        List<Resource> fResults = new ArrayList<>();

        for(Resource r : workingCopy)
        {
            for(final String msource : msources)
            {
                if(r.getMsource() != null)
                {
                    if(r.getMsource().equalsIgnoreCase(msource.toLowerCase()))
                    {
                        fResults.add(r);
                    }
                }
            }
        }
        return fResults;
    }

    public List<Resource> filterResultsByMtypes(String[] mtypes, List<Resource> workingCopy)
    {
        List<Resource> fResults = new ArrayList<>();

        for(Resource r : workingCopy)
        {
            for(final String mtype : mtypes)
            {
                if(r.getMtype() != null)
                {
                    String[] rtypes = r.getMtype().split(",");
                    for(int k = 0; k < rtypes.length; k++)
                    {
                        if(rtypes[k].trim().equalsIgnoreCase(mtype.toLowerCase()))
                        {
                            fResults.add(r);
                        }
                    }
                }
            }
        }
        return fResults;
    }

    public List<Resource> filterResultsByLevels(String[] levels, List<Resource> workingCopy) throws SQLException
    {
        List<Resource> fResults = new ArrayList<>();

        for(int i = 0; i < workingCopy.size(); i++)
        {
            Resource r = workingCopy.get(i);

            for(int j = 0; j < levels.length; j++)
            {
                List<String> rlevels;
                if(r.getExtendedMetadata().getLevels() != null)
                {
                    rlevels = r.getExtendedMetadata().getLevels();
                    for(int k = 0; k < rlevels.size(); k++)
                    {
                        if(rlevels.get(k).equalsIgnoreCase(levels[j]))
                        {
                            fResults.add(r);
                        }
                    }
                }
            }
        }
        return fResults;
    }

    public List<Resource> filterResultsByTargets(String[] targets, List<Resource> workingCopy) throws SQLException
    {
        List<Resource> fResults = new ArrayList<>();

        for(int i = 0; i < workingCopy.size(); i++)
        {
            Resource r = workingCopy.get(i);

            for(int j = 0; j < targets.length; j++)
            {
                List<String> rtargets;
                if(r.getExtendedMetadata().getTargets() != null)
                {
                    rtargets = r.getExtendedMetadata().getTargets();
                    for(int k = 0; k < rtargets.size(); k++)
                    {
                        if(rtargets.get(k).equalsIgnoreCase(targets[j].toLowerCase()))
                        {
                            fResults.add(r);
                        }
                    }
                }
            }
        }
        return fResults;
    }

    public List<Resource> filterResultsByPurposes(String[] purposes, List<Resource> workingCopy) throws SQLException
    {
        List<Resource> fResults = new ArrayList<>();

        for(int i = 0; i < workingCopy.size(); i++)
        {
            Resource r = workingCopy.get(i);

            for(int j = 0; j < purposes.length; j++)
            {
                List<String> rpurposes;
                if(r.getExtendedMetadata().getPurposes() != null)
                {
                    rpurposes = r.getExtendedMetadata().getPurposes();
                    for(int k = 0; k < rpurposes.size(); k++)
                    {
                        if(rpurposes.get(k).equalsIgnoreCase(purposes[j].toLowerCase()))
                        {
                            fResults.add(r);
                        }
                    }
                }
            }
        }
        return fResults;
    }

    public long getTotalResultCount()
    {
        if(totalResults == -1)
            log.warn("invalid state total results should not be -1");

        if(totalResults < 0)
        {
            return 0;
        }
        else
            return totalResults;
    }

    public static class FilterPaginator extends AbstractPaginator
    {
        private final static long serialVersionUID = 3823389610985272265L;

        private final ExtendedMetadataSearch filter;

        public FilterPaginator(ExtendedMetadataSearch filter)
        {
            super(filter.getResultsPerPage());
            this.filter = filter;

            log.debug("created new FilterPaginator");
        }

        @Override
        public boolean isEmpty()
        {
            if((int) filter.getTotalResultCount() == 0)
                return true;
            return false;
        }

        @Override
        public synchronized List<ResourceDecorator> getCurrentPage()
        {
            if(getCurrentPageCache() != null)
                return getCurrentPageCache();

            List<ResourceDecorator> presults = filter.getResourcesByPage(getPageIndex() + 1);

            setTotalResults((int) filter.getTotalResultCount());

            setCurrentPageCache(presults);

            return presults;
        }

    }

    public List<ResourceDecorator> getResourcesByPage(int currentPage)
    {
        int startIndex;
        List<ResourceDecorator> presources = new LinkedList<>();
        List<ResourceDecorator> resources = this.results;

        if(currentPage == 1)
        {
            startIndex = 0;
        }
        else
        {
            startIndex = (currentPage - 1) * this.resultsPerPage;
        }

        int endIndex = startIndex + this.resultsPerPage;

        for(int i = 0; i < resources.size(); i++)
        {
            if((i > (startIndex - 1)) && (i < endIndex))
            {
                presources.add(resources.get(i));
            }
        }

        return presources;
    }

    public ExtendedMetadataSearchFilters getmFilters()
    {
        return mFilters;
    }

    public void setmFilters(ExtendedMetadataSearchFilters mFilters)
    {
        this.mFilters = mFilters;
    }

    public FilterPaginator getFilterPaginator()
    {
        return filterPaginator;
    }

    public void setFilterPaginator(FilterPaginator filterPaginator)
    {
        this.filterPaginator = filterPaginator;
    }

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

    public String getSorting()
    {
        return sorting;
    }

    public void setSorting(String sorting)
    {
        this.sorting = sorting;
    }

    public String getGroupField()
    {
        return groupField;
    }

    public void setGroupField(String groupField)
    {
        this.groupField = groupField;
    }

    public long getTotalResults()
    {
        return totalResults;
    }

    public void setTotalResults(int totalResults)
    {
        this.totalResults = totalResults;
    }

    public int getUserId()
    {
        return userId;
    }

    public void setUserId(int userId)
    {
        this.userId = userId;
    }

    public void setSort(String sort)
    {
        this.sorting = sort;
    }

    public List<ResourceDecorator> getResults()
    {
        return results;
    }

    public void setResults(List<ResourceDecorator> results)
    {
        this.results = results;
    }

}
