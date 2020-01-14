package de.l3s.learnweb.resource.search;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.*;

import de.l3s.learnweb.resource.ResourceType;
import de.l3s.learnweb.resource.search.filters.Filter;
import de.l3s.learnweb.resource.search.filters.FilterOption;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.resource.ResourceDecorator;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.util.StringHelper;

public class SearchFilters implements Serializable
{
    private static final long serialVersionUID = 8012567994091306088L;
    final static Logger log = Logger.getLogger(SearchFilters.class);

    private Long totalResultsLearnweb = null;
    private Long totalResultsInterweb = null;
    private String stringFilters = null;
    private SearchMode configMode = SearchMode.text;
    private FILTERS lastFilter = null;
    private int prevFilters = 0;
    private Map<FILTERS, Object> configFilters = new EnumMap<>(FILTERS.class);
    private Map<FILTERS, List<Count>> availableResources = new HashMap<>();
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

    public enum FILTERS
    {
        service,
        type,
        date,
        group,
        collector,
        author,
        coverage,
        publisher,
        tags,
        imageSize,
        videoDuration,
        language;

        public static FILTERS[] getFilterByMode(SearchMode m)
        {
            switch(m)
            {
            case text:
                return new FILTERS[] { service, date, group, collector, author, coverage, publisher, tags };
            case image:
                return new FILTERS[] { service, date, group, author, tags, imageSize };
            case video:
                return new FILTERS[] { service, date, group, author, tags, videoDuration };
            case group:
                return new FILTERS[] { service, type, date, collector, author, coverage, publisher, tags };
            default:
                return values();
            }
        }

        @Override
        public String toString()
        {
            switch(this)
            {
            case imageSize:
                return UtilBean.getLocaleMessage("size");
            case videoDuration:
                return UtilBean.getLocaleMessage("duration");
            default:
                return UtilBean.getLocaleMessage(this.name());
            }
        }

        public String getLocaleAnyString()
        {
            switch(this)
            {
            case date:
                return UtilBean.getLocaleMessage("any_time");
            case imageSize:
                return UtilBean.getLocaleMessage("any_size");
            case videoDuration:
                return UtilBean.getLocaleMessage("any_duration");
            default:
                return UtilBean.getLocaleMessage("any_" + this.name());
            }
        }

        public String getItemName(String item)
        {
            switch(this)
            {
            case service:
                return ResourceService.parse(item).toString();
            case group:
                return getGroupNameById(item);
            default:
                return item;
            }
        }

        public boolean isEncodeBase64()
        {
            switch(this)
            {
            case collector:
            case author:
            case coverage:
            case publisher:
            case tags:
                return true;
            default:
                return false;
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
                    putResourceCounter(FILTERS.service, ff.getValues(), false);
                    break;
                case "type":
                    putResourceCounter(FILTERS.type, ff.getValues(), false);
                    break;
                case "groupId":
                    putResourceCounter(FILTERS.group, ff.getValues(), false);
                    break;
                case "collector_s":
                    putResourceCounter(FILTERS.collector, ff.getValues(), false);
                    break;
                case "author_s":
                    putResourceCounter(FILTERS.author, ff.getValues(), false);
                    break;
                case "coverage_s":
                    putResourceCounter(FILTERS.coverage, ff.getValues(), false);
                    break;
                case "publisher_s":
                    putResourceCounter(FILTERS.publisher, ff.getValues(), false);
                    break;
                case "tags_ss":
                    putResourceCounter(FILTERS.tags, ff.getValues(), false);
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
                continue;
            }
            else if(tempNames[0].equals("date"))
            {
                Count c = new Count(new FacetField(tempNames[0]), tempNames[1], entry.getValue());
                putResourceCounter(FILTERS.date, new ArrayList<>(Collections.singletonList(c)), true);
            }
            else if(tempNames[0].equals("type"))
            {
                Count c = new Count(new FacetField(tempNames[0]), tempNames[1], entry.getValue());
                putResourceCounter(FILTERS.type, new ArrayList<>(Collections.singletonList(c)), true);
            }
        }
    }

    public void putResourceCounter(FILTERS f, List<Count> counts, boolean merge)
    {
        if(lastFilter == null || lastFilter != f)
        {
            if(counts.size() <= 0 && availableResources.containsKey(f) && !merge)
            {
                availableResources.remove(f);
            }
            else if(merge && availableResources.containsKey(f) && counts.size() > 0)
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
            else if(counts.size() > 0)
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
                        FILTERS f = FILTERS.valueOf(nameValue[0]);
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

    private String changeFilterInUrl(FILTERS f, String value)
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
        FILTERS[] empty = {};
        return this.getAvailableFilters(empty);
    }

    public List<Filter> getAvailableFilters(FILTERS[] except)
    {
        List<Filter> list = new ArrayList<>();
        FILTERS[] filters = ArrayUtils.removeElements(FILTERS.getFilterByMode(configMode), except);

        for(FILTERS fs : filters)
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
                if(!configFilters.containsKey(FILTERS.service) || !configFilters.get(FILTERS.service).equals(ResourceService.bing) || !configFilters.get(FILTERS.service).equals(ResourceService.vimeo))
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

    public Long getTotalResources(FILTERS fs, String fn)
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
        if(configFilters.containsKey(FILTERS.imageSize) && res.getResource().getType().equals(ResourceType.image))
        {
            SIZE configSize = (SIZE) configFilters.get(FILTERS.imageSize);
            int width = res.getThumbnail4().getWidth(), minWidth = configSize.getMinWidth(), maxWidth = configSize.getMaxWidth();

            if(minWidth > width || (maxWidth != 0 && width > maxWidth))
            {
                return false;
            }
        }

        if(configFilters.containsKey(FILTERS.videoDuration) && res.getResource().getType().equals(ResourceType.video))
        {
            DURATION configDuration = (DURATION) configFilters.get(FILTERS.videoDuration);
            int duration = res.getResource().getDuration(), minDuration = configDuration.getMinDuration(), maxDuration = configDuration.getMaxDuration();

            if(minDuration > duration || (maxDuration != 0 && duration > maxDuration))
            {
                return false;
            }
        }

        return true;
    }

    public void setFilter(FILTERS f, Object o)
    {
        if(!isFilterRemoved)
        {
            this.lastFilter = f;
        }

        this.configFilters.put(f, o);
    }

    public String getServiceFilter()
    {
        if(configFilters.containsKey(FILTERS.service))
        {
            return ((ResourceService) configFilters.get(FILTERS.service)).name();
        }
        return null;
    }

    public String getTypeFilter()
    {
        if(configFilters.containsKey(FILTERS.type))
        {
            return ((TYPE) configFilters.get(FILTERS.type)).name();
        }
        return null;
    }

    public String getDateFromFilterAsString()
    {
        if(configFilters.containsKey(FILTERS.date) && ((DATE) configFilters.get(FILTERS.date)).getDateFrom() != null)
        {
            return configFilters.get(FILTERS.date).toString();
        }
        return null;
    }

    public Date getDateFromFilter()
    {
        if(configFilters.containsKey(FILTERS.date))
        {
            return ((DATE) configFilters.get(FILTERS.date)).getDateFrom();
        }
        return null;
    }

    public String getDateToFilterAsString()
    {
        if(configFilters.containsKey(FILTERS.date) && ((DATE) configFilters.get(FILTERS.date)).getDateTo() != null)
        {
            return configFilters.get(FILTERS.date).toString();
        }
        return null;
    }

    public Date getDateToFilter()
    {
        if(configFilters.containsKey(FILTERS.date))
        {
            return ((DATE) configFilters.get(FILTERS.date)).getDateTo();
        }
        return null;
    }

    public Date getDateToFilterDate()
    {
        return null;
    }

    public String getGroupFilter()
    {
        return (String) configFilters.get(FILTERS.group);
    }

    public String getCollectorFilter()
    {
        return (String) configFilters.get(FILTERS.collector);
    }

    public String getAuthorFilter()
    {
        return (String) configFilters.get(FILTERS.author);
    }

    public String getCoverageFilter()
    {
        return (String) configFilters.get(FILTERS.coverage);
    }

    public String getPublisherFilter()
    {
        return (String) configFilters.get(FILTERS.publisher);
    }

    public String getTagsFilter()
    {
        return (String) configFilters.get(FILTERS.tags);
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
        return (String) configFilters.get(FILTERS.language);
    }

    public void setLanguageFilter(String language)
    {
        setFilter(FILTERS.language, language);
    }

    public boolean isFiltersEnabled()
    {
        return StringUtils.isNotBlank(stringFilters);
    }

    public boolean isLearnwebSearchEnabled()
    {
        if(configFilters.containsKey(FILTERS.service))
        {
            return !((ResourceService) configFilters.get(FILTERS.service)).isInterweb();
        }
        return !canNotRequestLearnweb;
    }

    public boolean isInterwebSearchEnabled()
    {
        if(configFilters.containsKey(FILTERS.service))
        {
            return ((ResourceService) configFilters.get(FILTERS.service)).isInterweb();
        }
        return !canNotRequestInterweb;
    }

    public static String getGroupNameById(String id)
    {
        try
        {
            Group group;
            group = Learnweb.getInstance().getGroupManager().getGroupById(Integer.parseInt(id));
            if(null == group)
                return "deleted";
            return group.getTitle();
        }
        catch(NumberFormatException | SQLException e)
        {
            return id;
        }
    }

    public Long getTotalResults()
    {
        Long total = 0L;
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
