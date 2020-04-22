package de.l3s.learnweb.resource.search;

import java.io.Serializable;
import java.sql.SQLException;
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
 * total_hours_wasted_here = 48
 */
public class SearchFilters implements Serializable
{
    private static final long serialVersionUID = 8012567994091306088L;
    static final Logger log = LogManager.getLogger(SearchFilters.class);

    private Long totalResultsLearnweb = null;
    private Long totalResultsInterweb = null;
    private String stringFilters = null;
    private SearchMode configMode = SearchMode.text;
    private FilterType lastFilter = null;
    private int prevFilters = 0;
    private Map<FilterType, Object> configFilters = new EnumMap<>(FilterType.class);
    private Map<FilterType, List<Count>> availableResources = new HashMap<>();
    private boolean isFilterRemoved = false;
    private boolean canNotRequestLearnweb = false;
    private boolean canNotRequestInterweb = false;

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
        return new String[] { "location", "type", "groupId", "collector_s", "author_s", "coverage_s", "publisher_s", "tags_ss" };
    }

    public String[] getFacetQueries()
    {
        return new String[] { "{!key='type.other'}-type:text,video,image,pdf", "{!key='date.old'}timestamp:[* TO NOW-365DAY]", "{!key='date.y'}timestamp:[NOW-365DAY TO NOW]", "{!key='date.m'}timestamp:[NOW-30DAY TO NOW]", "{!key='date.w'}timestamp:[NOW-7DAY TO NOW]",
                "{!key='date.d'}timestamp:[NOW-1DAY TO NOW]" };
    }

    public void clean()
    {
        lastFilter = null;
        configFilters.clear();
        canNotRequestLearnweb = false;
        canNotRequestInterweb = false;
    }

    public void cleanAll()
    {
        lastFilter = null;
        configFilters.clear();
        availableResources.clear();
        canNotRequestLearnweb = false;
        canNotRequestInterweb = false;
        totalResultsLearnweb = null;
        totalResultsInterweb = null;
    }

    public void putResourceCounter(List<FacetField> ffs)
    {
        for(FacetField ff : ffs)
        {
            switch(ff.getName())
            {
                case "location":
                    putResourceCounter(FilterType.service, ff.getValues(), false);
                    break;
                case "type":
                    putResourceCounter(FilterType.type, ff.getValues(), false);
                    break;
                case "groupId":
                    putResourceCounter(FilterType.group, ff.getValues(), false);
                    break;
                case "collector_s":
                    putResourceCounter(FilterType.collector, ff.getValues(), false);
                    break;
                case "author_s":
                    putResourceCounter(FilterType.author, ff.getValues(), false);
                    break;
                case "coverage_s":
                    putResourceCounter(FilterType.coverage, ff.getValues(), false);
                    break;
                case "publisher_s":
                    putResourceCounter(FilterType.publisher, ff.getValues(), false);
                    break;
                case "tags_ss":
                    putResourceCounter(FilterType.tags, ff.getValues(), false);
                    break;
            }
        }
    }

    public void putResourceCounter(Map<String, Integer> map)
    {
        for(Map.Entry<String, Integer> entry : map.entrySet())
        {
            String key = entry.getKey();
            String[] tempNames = key.split("\\.");
            if(tempNames.length != 2)
            {
            }
            else if(tempNames[0].equals("date"))
            {
                Count c = new Count(new FacetField(tempNames[0]), tempNames[1], entry.getValue());
                putResourceCounter(FilterType.date, new ArrayList<>(Collections.singletonList(c)), true);
            }
            else if(tempNames[0].equals("type"))
            {
                Count c = new Count(new FacetField(tempNames[0]), tempNames[1], entry.getValue());
                putResourceCounter(FilterType.type, new ArrayList<>(Collections.singletonList(c)), true);
            }
        }
    }

    public void putResourceCounter(FilterType f, List<Count> counts, boolean merge)
    {
        if(lastFilter == null || lastFilter != f)
        {
            if(counts.size() <= 0 && availableResources.containsKey(f) && !merge)
            {
                availableResources.remove(f);
            }
            else if(merge && availableResources.containsKey(f) && !counts.isEmpty())
            {
                List<Count> c = availableResources.get(f);
                for(Count count : counts)
                {
                    if(c.contains(count))
                    {
                        c.set(c.indexOf(count), count);
                    }
                    else
                    {
                        c.add(count);
                    }
                }
                availableResources.put(f, c);
            }
            else if(!counts.isEmpty())
            {
                availableResources.put(f, counts);
            }
        }
    }

    public void setFiltersFromString(String filters)
    {
        if(filters == null || filters.isEmpty())
        {
            cleanAll();
            this.stringFilters = null;
        }
        else
        {
            clean();
            this.stringFilters = filters;
            String[] tempFilters = filters.split(",");

            isFilterRemoved = tempFilters.length < prevFilters;
            prevFilters = tempFilters.length;

            for(String filter : tempFilters)
            {
                String[] nameValue = filter.split(":");
                if(nameValue.length == 2)
                {
                    try
                    {
                        FilterType f = FilterType.valueOf(nameValue[0]);
                        String fValue = f.isEncodeBase64() ? StringHelper.decodeBase64(nameValue[1]) : nameValue[1];

                        switch(f)
                        {
                        case service:
                            setFilter(f, ResourceService.parse(fValue));
                            break;
                        case type:
                            setFilter(f, TYPE.valueOf(fValue));
                            break;
                        case date:
                            setFilter(f, DATE.valueOf(fValue));
                            break;
                        case group:
                        case collector:
                        case author:
                        case coverage:
                        case publisher:
                        case tags:
                            canNotRequestInterweb = true;
                            setFilter(f, fValue);
                            break;
                        case videoDuration:
                            setFilter(f, DURATION.valueOf(fValue));
                            break;
                        case imageSize:
                            setFilter(f, SIZE.valueOf(fValue));
                            break;
                        default:
                            setFilter(f, fValue);
                            break;
                        }
                    }
                    catch(IllegalArgumentException e)
                    {
                        log.error("Filter " + nameValue[0] + " and its value " + nameValue[1] + " ignored.");
                    }
                }
            }
        }
    }

    public String getFiltersString()
    {
        return stringFilters;
    }

    private String changeFilterInUrl(FilterType f, String value)
    {
        value = value == null ? null : (f.isEncodeBase64() ? StringHelper.encodeBase64(value.getBytes()) : value);

        if(configFilters.containsKey(f) && stringFilters != null)
        {
            String output = "";
            int startIndex = stringFilters.indexOf(f.name());
            int endIndex = stringFilters.indexOf(',', startIndex);

            if(startIndex != 0) // TODO changed if(startIndex != 0)
            {
                output += stringFilters.substring(0, startIndex - 1);
            }

            if(endIndex != -1)
            {
                output += stringFilters.substring(endIndex + 1);
            }

            return value == null ? (output.isEmpty() ? null : output) : (output.isEmpty() ? (f.name() + ":" + value) : (output + ',' + f.name() + ":" + value));
        }
        else if(value != null)
        {
            return stringFilters == null ? (f.name() + ":" + value) : (stringFilters + ',' + f.name() + ":" + value);
        }

        return stringFilters;
    }

    public List<Filter> getAvailableFilters()
    {
        FilterType[] empty = {};
        return this.getAvailableFilters(empty);
    }

    public List<Filter> getAvailableFilters(FilterType[] except)
    {
        List<Filter> list = new ArrayList<>();
        FilterType[] filters = ArrayUtils.removeElements(FilterType.getFilters(configMode), except);

        for(FilterType fs : filters)
        {
            boolean containsFilter = configFilters.containsKey(fs);
            Filter nf = new Filter(containsFilter ? fs.getItemName(configFilters.get(fs).toString()) : fs.toString(), fs.getLocaleAnyString(), changeFilterInUrl(fs, null), containsFilter);

            switch(fs)
            {
            case service:
                if(availableResources.containsKey(fs))
                {
                    for(Count c : availableResources.get(fs))
                    {
                        FilterOption fi = new FilterOption(fs.getItemName(c.getName()), changeFilterInUrl(fs, c.getName().toLowerCase()), c.getCount(), containsFilter && configFilters.get(fs).toString().equals(c.getName()));
                        nf.addOption(fi);
                    }
                }
                break;
            case type:
                if(availableResources.containsKey(fs))
                {
                    for(TYPE t : TYPE.values())
                    {
                        Long counter = null;
                        if(availableResources.containsKey(fs))
                        {
                            for(Count c : availableResources.get(fs))
                            {
                                if(c.getName().equals(t.name()))
                                {
                                    counter = c.getCount();
                                    break;
                                }
                            }

                            if(counter == null || counter == 0)
                            {
                                continue;
                            }
                        }
                        FilterOption fi = new FilterOption(t.toString(), changeFilterInUrl(fs, t.name()), counter, containsFilter && configFilters.get(fs).equals(t));
                        nf.addOption(fi);
                    }
                }
                break;
            case date:
                if(!configFilters.containsKey(FilterType.service) || !configFilters.get(FilterType.service).equals(ResourceService.bing) || !configFilters.get(FilterType.service).equals(ResourceService.vimeo))
                {
                    for(DATE d : DATE.values())
                    {
                        Long counter = null;
                        // TODO Dupe: duplicate code in same class
                        if(availableResources.containsKey(fs))
                        {
                            for(Count c : availableResources.get(fs))
                            {
                                if(c.getName().equals(d.name()))
                                {
                                    counter = c.getCount();
                                    break;
                                }
                            }

                            if(counter == null || counter == 0)
                            {
                                continue;
                            }
                        }
                        FilterOption fi = new FilterOption(d.toString(), changeFilterInUrl(fs, d.name()), counter, containsFilter && configFilters.get(fs).equals(d));
                        nf.addOption(fi);
                    }
                }
                break;
            case group:
            case collector:
            case author:
            case coverage:
            case publisher:
            case tags:
                if(availableResources.containsKey(fs))
                {
                    for(Count c : availableResources.get(fs))
                    {
                        if(c.getName().isEmpty() || c.getName().equals("\n") || c.getName().equals("0"))
                            continue;
                        FilterOption fi = new FilterOption(fs.getItemName(c.getName()), changeFilterInUrl(fs, c.getName()), c.getCount(), containsFilter && configFilters.get(fs).equals(c.getName()));
                        nf.addOption(fi);
                    }
                }
                break;
            case videoDuration:
                for(DURATION d : DURATION.values())
                {
                    FilterOption fi = new FilterOption(d.toString(),  changeFilterInUrl(fs, d.name()), null, containsFilter && configFilters.get(fs).equals(d));
                    nf.addOption(fi);
                }
                break;
            case imageSize:
                for(SIZE d : SIZE.values())
                {
                    FilterOption fi = new FilterOption(d.toString(), changeFilterInUrl(fs, d.name()), null, containsFilter && configFilters.get(fs).equals(d));
                    nf.addOption(fi);
                }
                break;
            case language:
                break;
            }

            list.add(nf);
        }
        return list;
    }

    public Long getTotalResources(FilterType fs, String fn)
    {
        if(availableResources.containsKey(fs))
        {
            for(Count c : availableResources.get(fs))
            {
                if(c.getName().equalsIgnoreCase(fn))
                {
                    return c.getCount();
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
        //String type = res.getResource().getType(); // text Image Video
        if(configFilters.containsKey(FilterType.imageSize) && res.getResource().getType() == ResourceType.image)
        {
            SIZE configSize = (SIZE) configFilters.get(FilterType.imageSize);
            int width = res.getThumbnail4().getWidth(), minWidth = configSize.getMinWidth(), maxWidth = configSize.getMaxWidth();

            if(minWidth > width || (maxWidth != 0 && width > maxWidth))
            {
                return false;
            }
        }

        if(configFilters.containsKey(FilterType.videoDuration) && res.getResource().getType() == ResourceType.video)
        {
            DURATION configDuration = (DURATION) configFilters.get(FilterType.videoDuration);
            int duration = res.getResource().getDuration();
            int minDuration = configDuration.getMinDuration();
            int maxDuration = configDuration.getMaxDuration();
            return minDuration <= duration && (maxDuration == 0 || duration <= maxDuration);
        }

        return true;
    }

    public void setFilter(FilterType f, Object o)
    {
        if(!isFilterRemoved)
        {
            this.lastFilter = f;
        }

        this.configFilters.put(f, o);
    }

    public String getServiceFilter()
    {
        if(configFilters.containsKey(FilterType.service))
        {
            return ((ResourceService) configFilters.get(FilterType.service)).name();
        }
        return null;
    }

    public String getTypeFilter()
    {
        if(configFilters.containsKey(FilterType.type))
        {
            return ((TYPE) configFilters.get(FilterType.type)).name();
        }
        return null;
    }

    public String getDateFromFilterAsString()
    {
        if(configFilters.containsKey(FilterType.date) && ((DATE) configFilters.get(FilterType.date)).getDateFrom() != null)
        {
            return configFilters.get(FilterType.date).toString();
        }
        return null;
    }

    public Date getDateFromFilter()
    {
        if(configFilters.containsKey(FilterType.date))
        {
            return ((DATE) configFilters.get(FilterType.date)).getDateFrom();
        }
        return null;
    }

    public String getDateToFilterAsString()
    {
        if(configFilters.containsKey(FilterType.date) && ((DATE) configFilters.get(FilterType.date)).getDateTo() != null)
        {
            return configFilters.get(FilterType.date).toString();
        }
        return null;
    }

    public Date getDateToFilter()
    {
        if(configFilters.containsKey(FilterType.date))
        {
            return ((DATE) configFilters.get(FilterType.date)).getDateTo();
        }
        return null;
    }

    public Date getDateToFilterDate()
    {
        return null;
    }

    public String getGroupFilter()
    {
        return (String) configFilters.get(FilterType.group);
    }

    public String getCollectorFilter()
    {
        return (String) configFilters.get(FilterType.collector);
    }

    public String getAuthorFilter()
    {
        return (String) configFilters.get(FilterType.author);
    }

    public String getCoverageFilter()
    {
        return (String) configFilters.get(FilterType.coverage);
    }

    public String getPublisherFilter()
    {
        return (String) configFilters.get(FilterType.publisher);
    }

    public String getTagsFilter()
    {
        return (String) configFilters.get(FilterType.tags);
    }

    public void setMode(SearchMode m)
    {
        if(m != configMode)
        {
            this.configMode = m;
            cleanAll();
        }
    }

    public SearchMode getMode()
    {
        return configMode;
    }

    public String getLanguageFilter()
    {
        return (String) configFilters.get(FilterType.language);
    }

    public void setLanguageFilter(String language)
    {
        setFilter(FilterType.language, language);
    }

    public boolean isFiltersEnabled()
    {
        return StringUtils.isNotBlank(stringFilters);
    }

    public boolean isLearnwebSearchEnabled()
    {
        if(configFilters.containsKey(FilterType.service))
        {
            return !((ResourceService) configFilters.get(FilterType.service)).isInterweb();
        }
        return !canNotRequestLearnweb;
    }

    public boolean isInterwebSearchEnabled()
    {
        if(configFilters.containsKey(FilterType.service))
        {
            return ((ResourceService) configFilters.get(FilterType.service)).isInterweb();
        }
        return !canNotRequestInterweb;
    }

    public Long getTotalResults()
    {
        long total = 0L;
        if(totalResultsInterweb != null)
        {
            total += totalResultsInterweb;
        }
        if(totalResultsLearnweb != null)
        {
            total += totalResultsLearnweb;
        }

        return total;
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
}
