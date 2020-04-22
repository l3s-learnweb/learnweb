package de.l3s.learnweb.resource.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;

import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.resource.ResourceDecorator;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.ResourceType;
import de.l3s.learnweb.resource.search.filters.Filter;
import de.l3s.learnweb.resource.search.filters.FilterOption;
import de.l3s.learnweb.resource.search.filters.FilterType;
import de.l3s.util.StringHelper;

/**
 * Dear maintainer:
 * Once you are done trying to 'optimize' this routine, and have realized what a terrible mistake that was,
 * please increment the following counter as a warning to the next guy:
 *
 * total_hours_wasted_here = 50
 */
public class SearchFilters implements Serializable
{
    private static final long serialVersionUID = 8012567994091306088L;
    static final Logger log = LogManager.getLogger(SearchFilters.class);

    private boolean isFilterRemoved = false;
    private FilterType lastFilter = null;
    private int prevFilters = 0;

    private String stringFilters = null;
    private SearchMode searchMode = SearchMode.text;
    private Map<FilterType, Object> currentFilters = new EnumMap<>(FilterType.class);

    private Map<FilterType, List<Count>> availableResources = new HashMap<>();
    private boolean canNotRequestLearnweb = false;
    private boolean canNotRequestInterweb = false;
    private Long totalResultsLearnweb = null;
    private Long totalResultsInterweb = null;

    public enum TYPE
    {
        text,
        image,
        video,
        pdf,
        other;

        @Override
        public String toString()
        {
            return UtilBean.getLocaleMessage(this.name());
        }
    }

    public enum DATE
    {
        d, // day
        w, // week (7 days)
        m, // month
        y, // year
        old;

        @Override
        public String toString()
        {
            switch(this)
            {
            case d:
                return UtilBean.getLocaleMessage("past_24_hours");
            case w:
                return UtilBean.getLocaleMessage("past_week");
            case m:
                return UtilBean.getLocaleMessage("past_month");
            case y:
                return UtilBean.getLocaleMessage("past_year");
            case old:
                return UtilBean.getLocaleMessage("older_than_year");
            default:
                return UtilBean.getLocaleMessage("any_time");
            }
        }

        public Date getDateFrom()
        {
            Calendar cal = Calendar.getInstance();
            switch(this)
            {
            case d:
                cal.add(Calendar.DATE, -1);
                break;
            case w:
                cal.add(Calendar.DATE, -7);
                break;
            case m:
                cal.add(Calendar.MONTH, -1);
                break;
            case y:
                cal.add(Calendar.YEAR, -1);
                break;
            case old:
                return null;
            default:
                break;
            }

            return cal.getTime();
        }

        public Date getDateTo()
        {
            Calendar cal = Calendar.getInstance();
            switch(this)
            {
            case old:
                cal.add(Calendar.YEAR, -1);
                break;
            default:
                return null;
            }

            return cal.getTime();
        }
    }

    public enum SIZE
    {
        small,
        medium,
        large,
        extraLarge;

        @Override
        public String toString()
        {
            switch(this)
            {
            case small:
                return UtilBean.getLocaleMessage("small");
            case medium:
                return UtilBean.getLocaleMessage("medium");
            case large:
                return UtilBean.getLocaleMessage("large");
            case extraLarge:
                return UtilBean.getLocaleMessage("extra_large");
            default:
                return UtilBean.getLocaleMessage("any_size");
            }
        }

        public int getMaxWidth()
        {
            switch(this)
            {
            case small:
                return 150;
            case medium:
                return 600;
            case large:
                return 1200;
            case extraLarge:
                return 0;
            default:
                return 0;
            }
        }

        public int getMinWidth()
        {
            switch(this)
            {
            case small:
                return 0;
            case medium:
                return 150;
            case large:
                return 600;
            case extraLarge:
                return 1200;
            default:
                return 0;
            }
        }
    }

    public enum DURATION
    {
        s,
        m,
        l;

        @Override
        public String toString()
        {
            switch(this)
            {
            case s:
                return UtilBean.getLocaleMessage("short");
            case m:
                return UtilBean.getLocaleMessage("medium");
            case l:
                return UtilBean.getLocaleMessage("long");
            default:
                return UtilBean.getLocaleMessage("any_duration");
            }
        }

        public int getMaxDuration()
        {
            switch(this)
            {
            case s:
                return 240; // 4 min
            case m:
                return 1200; // 20 min
            case l:
                return 0;
            default:
                return 0;
            }
        }

        public int getMinDuration()
        {
            switch(this)
            {
            case s:
                return 0;
            case m:
                return 240;
            case l:
                return 1200;
            default:
                return 0;
            }
        }
    }

    public String[] getFacetFields()
    {
        return new String[]{ "location", "type", "groupId", "collector_s", "author_s", "coverage_s", "publisher_s", "tags_ss" };
    }

    public String[] getFacetQueries()
    {
        return new String[]{ "{!key='type.other'}-type:text,video,image,pdf", "{!key='date.old'}timestamp:[* TO NOW-365DAY]", "{!key='date.y'}timestamp:[NOW-365DAY TO NOW]", "{!key='date.m'}timestamp:[NOW-30DAY TO NOW]", "{!key='date.w'}timestamp:[NOW-7DAY TO NOW]",
                "{!key='date.d'}timestamp:[NOW-1DAY TO NOW]" };
    }

    public void clean()
    {
        lastFilter = null;
        currentFilters.clear();
        canNotRequestLearnweb = false;
        canNotRequestInterweb = false;
    }

    public void cleanAll()
    {
        lastFilter = null;
        currentFilters.clear();
        availableResources.clear();
        canNotRequestLearnweb = false;
        canNotRequestInterweb = false;
        totalResultsLearnweb = null;
        totalResultsInterweb = null;
    }

    public void putResourceCounter(List<FacetField> facetFields)
    {
        for(FacetField facetField : facetFields)
        {
            switch(facetField.getName())
            {
                case "location":
                    putResourceCounter(FilterType.service, facetField.getValues(), false);
                    break;
                case "type":
                    putResourceCounter(FilterType.type, facetField.getValues(), false);
                    break;
                case "groupId":
                    putResourceCounter(FilterType.group, facetField.getValues(), false);
                    break;
                case "collector_s":
                    putResourceCounter(FilterType.collector, facetField.getValues(), false);
                    break;
                case "author_s":
                    putResourceCounter(FilterType.author, facetField.getValues(), false);
                    break;
                case "coverage_s":
                    putResourceCounter(FilterType.coverage, facetField.getValues(), false);
                    break;
                case "publisher_s":
                    putResourceCounter(FilterType.publisher, facetField.getValues(), false);
                    break;
                case "tags_ss":
                    putResourceCounter(FilterType.tags, facetField.getValues(), false);
                    break;
            }
        }
    }

    public void putResourceCounter(Map<String, Integer> facetQueries)
    {
        for(Map.Entry<String, Integer> facetQuery : facetQueries.entrySet())
        {
            String[] tempNames = facetQuery.getKey().split("\\.");
            if(tempNames.length != 2) continue;

            Count count = new Count(new FacetField(tempNames[0]), tempNames[1], facetQuery.getValue());
            if("date".equals(tempNames[0]))
                putResourceCounter(FilterType.date, Collections.singletonList(count), true);
            else if("type".equals(tempNames[0]))
                putResourceCounter(FilterType.type, Collections.singletonList(count), true);
        }
    }

    public void putResourceCounter(FilterType filterType, List<Count> counts, boolean merge)
    {
        if(lastFilter == null || lastFilter != filterType)
        {
            if(counts.isEmpty() && availableResources.containsKey(filterType) && !merge)
            {
                availableResources.remove(filterType);
            }
            else if(merge && availableResources.containsKey(filterType) && !counts.isEmpty())
            {
                List<Count> availableCount = availableResources.get(filterType);
                for(Count count : counts)
                {
                    if(availableCount.contains(count))
                    {
                        availableCount.set(availableCount.indexOf(count), count);
                    }
                    else
                    {
                        availableCount.add(count);
                    }
                }
                availableResources.put(filterType, availableCount);
            }
            else if(!counts.isEmpty())
            {
                availableResources.put(filterType, counts);
            }
        }
    }

    public String getFiltersString()
    {
        return stringFilters;
    }

    /**
     * This method set filters from a string, e.g. url.
     * Example string: filter:foo,filter2:bar,filter3:tar
     */
    public void setFilters(String filters)
    {
        if(StringUtils.isEmpty(filters))
        {
            cleanAll();
            this.stringFilters = null;
            return;
        }

        clean();
        this.stringFilters = filters;
        String[] splitFilters = filters.split(",");

        isFilterRemoved = splitFilters.length < prevFilters;
        prevFilters = splitFilters.length;

        for(String splitFilter : splitFilters)
        {
            try
            {
                String[] nameValue = splitFilter.split(":");
                if(nameValue.length != 2) continue; // something wrong, skip it

                FilterType filter = FilterType.valueOf(nameValue[0]);
                String value = filter.isEncodeBase64() ? StringHelper.decodeBase64(nameValue[1]) : nameValue[1];
                setRawFilter(filter, value);
            }
            catch(Exception e)
            {
                log.error("Filter '" + splitFilter + "' can not be processed and was ignored.", e);
            }
        }
    }

    private void setRawFilter(FilterType filter, String value)
    {
        switch(filter)
        {
            case service:
                setFilter(filter, ResourceService.parse(value));
                break;
            case type:
                setFilter(filter, TYPE.valueOf(value));
                break;
            case date:
                setFilter(filter, DATE.valueOf(value));
                break;
            case group:
            case collector:
            case author:
            case coverage:
            case publisher:
            case tags:
                canNotRequestInterweb = true;
                setFilter(filter, value);
                break;
            case videoDuration:
                setFilter(filter, DURATION.valueOf(value));
                break;
            case imageSize:
                setFilter(filter, SIZE.valueOf(value));
                break;
            default:
                setFilter(filter, value);
                break;
        }
    }

    private String changeFilterInUrl(FilterType filter, String value)
    {
        StringBuilder output = new StringBuilder();
        if(currentFilters.containsKey(filter) && stringFilters != null)
        {
            int startIndex = stringFilters.indexOf(filter.name());
            int endIndex = stringFilters.indexOf(',', startIndex);

            if(startIndex != 0) output.append(stringFilters, 0, startIndex - 1);
            if(endIndex != -1) output.append(stringFilters, endIndex + 1, stringFilters.length());
        }
        else if(stringFilters != null)
        {
            output.append(stringFilters);
        }

        if (value != null)
        {
            value = filter.isEncodeBase64() ? StringHelper.encodeBase64(value.getBytes()) : value;
            if (output.length() != 0) output.append(",");
            output.append(filter.name()).append(":").append(value);
        }

        return output.length() != 0 ? output.toString() : null;
    }

    public List<Filter> getAvailableFilters()
    {
        FilterType[] empty = {};
        return this.getAvailableFilters(empty);
    }

    public List<Filter> getAvailableFilters(final FilterType[] except)
    {
        List<Filter> results = new ArrayList<>();
        FilterType[] filterTypes = ArrayUtils.removeElements(FilterType.getFilters(searchMode), except);

        for(FilterType filterType : filterTypes)
        {
            boolean isActive = currentFilters.containsKey(filterType);
            final String filterName = isActive ? filterType.getItemName(currentFilters.get(filterType).toString()) : filterType.toString();
            Filter filter = new Filter(filterName, filterType.getLocaleAnyString(), changeFilterInUrl(filterType, null), isActive);

            switch(filterType)
            {
                case service:
                    if(availableResources.containsKey(filterType))
                    {
                        for(Count count : availableResources.get(filterType))
                        {
                            FilterOption fi = new FilterOption(filterType.getItemName(count.getName()), changeFilterInUrl(filterType, count.getName().toLowerCase()), count.getCount(), isActive && currentFilters.get(filterType).toString().equals(count.getName()));
                            filter.addOption(fi);
                        }
                    }
                    break;
                case type:
                    if(availableResources.containsKey(filterType))
                    {
                        for(TYPE type : TYPE.values())
                        {
                            Long counter = null;
                            if(availableResources.containsKey(filterType))
                            {
                                for(Count count : availableResources.get(filterType))
                                {
                                    if(count.getName().equals(type.name()))
                                    {
                                        counter = count.getCount();
                                        break;
                                    }
                                }

                                if(counter == null || counter == 0)
                                {
                                    continue;
                                }
                            }
                            FilterOption fi = new FilterOption(type.toString(), changeFilterInUrl(filterType, type.name()), counter, isActive && currentFilters.get(filterType) == type);
                            filter.addOption(fi);
                        }
                    }
                    break;
                case date:
                    if(!currentFilters.containsKey(FilterType.service) || currentFilters.get(FilterType.service) != ResourceService.bing || currentFilters.get(FilterType.service) != ResourceService.vimeo)
                    {
                        for(DATE date : DATE.values())
                        {
                            Long counter = null;
                            if(availableResources.containsKey(filterType))
                            {
                                for(Count count : availableResources.get(filterType))
                                {
                                    if(count.getName().equals(date.name()))
                                    {
                                        counter = count.getCount();
                                        break;
                                    }
                                }

                                if(counter == null || counter == 0)
                                {
                                    continue;
                                }
                            }
                            FilterOption fi = new FilterOption(date.toString(), changeFilterInUrl(filterType, date.name()), counter, isActive && currentFilters.get(filterType) == date);
                            filter.addOption(fi);
                        }
                    }
                    break;
                case group:
                case collector:
                case author:
                case coverage:
                case publisher:
                case tags:
                    if(availableResources.containsKey(filterType))
                    {
                        for(Count count : availableResources.get(filterType))
                        {
                            if(count.getName().isEmpty() || count.getName().equals("\n") || count.getName().equals("0"))
                                continue;
                            FilterOption fi = new FilterOption(filterType.getItemName(count.getName()), changeFilterInUrl(filterType, count.getName()), count.getCount(), isActive && currentFilters.get(filterType).equals(count.getName()));
                            filter.addOption(fi);
                        }
                    }
                    break;
                case videoDuration:
                    for(DURATION duration : DURATION.values())
                    {
                        FilterOption fi = new FilterOption(duration.toString(), changeFilterInUrl(filterType, duration.name()), null, isActive && currentFilters.get(filterType) == duration);
                        filter.addOption(fi);
                    }
                    break;
                case imageSize:
                    for(SIZE size : SIZE.values())
                    {
                        FilterOption fi = new FilterOption(size.toString(), changeFilterInUrl(filterType, size.name()), null, isActive && currentFilters.get(filterType) == size);
                        filter.addOption(fi);
                    }
                    break;
                case language:
                    break;
            }

            results.add(filter);
        }
        return results;
    }

    public Long getTotalResources(FilterType filter, String value)
    {
        if(availableResources.containsKey(filter))
        {
            for(Count count : availableResources.get(filter))
            {
                if(count.getName().equalsIgnoreCase(value))
                {
                    return count.getCount();
                }
            }
        }

        return 0L;
    }

    /**
     * Check filters like image width and video duration
     *
     * @param res
     * @return boolean
     */
    public boolean checkAfterLoadFilters(ResourceDecorator res)
    {
        if(res.getResource().getType() == ResourceType.image && currentFilters.containsKey(FilterType.imageSize))
        {
            SIZE configSize = (SIZE) currentFilters.get(FilterType.imageSize);
            int width = res.getThumbnail4().getWidth();
            int minWidth = configSize.getMinWidth();
            int maxWidth = configSize.getMaxWidth();
            return minWidth <= width && (maxWidth == 0 || width <= maxWidth);
        }

        if(res.getResource().getType() == ResourceType.video && currentFilters.containsKey(FilterType.videoDuration))
        {
            DURATION configDuration = (DURATION) currentFilters.get(FilterType.videoDuration);
            int duration = res.getResource().getDuration();
            int minDuration = configDuration.getMinDuration();
            int maxDuration = configDuration.getMaxDuration();
            return minDuration <= duration && (maxDuration == 0 || duration <= maxDuration);
        }

        return true;
    }

    public void setFilter(FilterType filter, Object o)
    {
        if(!isFilterRemoved)
        {
            this.lastFilter = filter;
        }

        this.currentFilters.put(filter, o);
    }

    public void setMode(SearchMode mode)
    {
        if(mode != searchMode)
        {
            this.searchMode = mode;
            cleanAll();
        }
    }

    public SearchMode getMode()
    {
        return searchMode;
    }

    public boolean isFiltersEnabled()
    {
        return StringUtils.isNotBlank(stringFilters);
    }

    public boolean isLearnwebSearchEnabled()
    {
        if(currentFilters.containsKey(FilterType.service))
        {
            return !((ResourceService) currentFilters.get(FilterType.service)).isInterweb();
        }
        return !canNotRequestLearnweb;
    }

    public boolean isInterwebSearchEnabled()
    {
        if(currentFilters.containsKey(FilterType.service))
        {
            return ((ResourceService) currentFilters.get(FilterType.service)).isInterweb();
        }
        return !canNotRequestInterweb;
    }

    public long getTotalResults()
    {
        long results = 0L;
        if(totalResultsInterweb != null)
        {
            results += totalResultsInterweb;
        }
        if(totalResultsLearnweb != null)
        {
            results += totalResultsLearnweb;
        }

        return results;
    }

    public Long getTotalResultsLearnweb()
    {
        return totalResultsLearnweb;
    }

    public void setTotalResultsLearnweb(Long totalResultsLearnweb)
    {
        this.totalResultsLearnweb = totalResultsLearnweb;
    }

    public Long getTotalResultsInterweb()
    {
        return totalResultsInterweb;
    }

    public void setTotalResultsInterweb(Long totalResultsInterweb)
    {
        this.totalResultsInterweb = totalResultsInterweb;
    }

    /* ------------------- GETTERS FOR EACH FILTER ------------------- */

    public ResourceService getServiceFilter()
    {
        return (ResourceService) currentFilters.get(FilterType.service);
    }

    public TYPE getTypeFilter()
    {
        return (TYPE) currentFilters.get(FilterType.type);
    }

    public DATE getDateFilter()
    {
        return (DATE) currentFilters.get(FilterType.date);
    }

    public String getGroupFilter()
    {
        return (String) currentFilters.get(FilterType.group);
    }

    public String getCollectorFilter()
    {
        return (String) currentFilters.get(FilterType.collector);
    }

    public String getAuthorFilter()
    {
        return (String) currentFilters.get(FilterType.author);
    }

    public String getCoverageFilter()
    {
        return (String) currentFilters.get(FilterType.coverage);
    }

    public String getPublisherFilter()
    {
        return (String) currentFilters.get(FilterType.publisher);
    }

    public String getTagsFilter()
    {
        return (String) currentFilters.get(FilterType.tags);
    }

    public SIZE getImageSizeFilter()
    {
        return (SIZE) currentFilters.get(FilterType.imageSize);
    }

    public DURATION getVideoDurationFilter()
    {
        return (DURATION) currentFilters.get(FilterType.videoDuration);
    }

    public String getLanguageFilter()
    {
        return (String) currentFilters.get(FilterType.language);
    }
}
