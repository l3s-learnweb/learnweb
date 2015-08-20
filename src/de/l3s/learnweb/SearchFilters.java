package de.l3s.learnweb;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;

import de.l3s.learnweb.Search.MODE;
import de.l3s.learnweb.beans.UtilBean;

public class SearchFilters implements Serializable
{
    private static final long serialVersionUID = 8012567994091306088L;
    final static Logger log = Logger.getLogger(SearchFilters.class);

    private String stringFilters = null;
    private MODE configMode;
    private Map<FILTERS, Object> configFilters = new EnumMap<>(FILTERS.class);
    private Map<FILTERS, List<Count>> availableResources = new HashMap<FILTERS, List<Count>>();
    private boolean canNotRequestLearnweb = false;
    private boolean canNotRequestInterweb = false;
    private boolean resourceCounterFlag = true; // Uses for collect counters from interweb and solr and merge them

    public enum SERVICE
    {
	Bing, // Not support filter by date
	Flickr,
	YouTube,
	Vimeo, // Not support filter by date
	Ipernity,
	ted, // stored in SOLR
	tedx, // stored in SOLR
	loro, // stored in SOLR
	yovisto, //  stored in SOLR
	learnweb, // stored in SOLR
	archiveit// stored in SOLR
	;

	public boolean isLearnwebSource()
	{
	    switch(this)
	    {
	    case Bing:
	    case Flickr:
	    case YouTube:
	    case Vimeo:
	    case Ipernity:
		return false;
	    default:
		return true;
	    }
	}

	@Override
	public String toString()
	{
	    return this.name();
	}
    };

    public enum DATE
    {
	d, // day
	w, // week (7 days)
	m, // month
	y // year
	;

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
	    default:
		return UtilBean.getLocaleMessage("any_time");
	    }
	}

	public Date getDate()
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
		return 300;
	    case m:
		return 1200;
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
		return 300;
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
	date,
	group,
	collector,
	author,
	imageSize,
	videoDuration,
	language;

	public static FILTERS[] getFilterByMode(MODE m)
	{
	    switch(m)
	    {
	    case web:
		return new FILTERS[] { FILTERS.service, FILTERS.date, FILTERS.group, FILTERS.collector, FILTERS.author };
	    case image:
		return new FILTERS[] { FILTERS.service, FILTERS.date, FILTERS.group, FILTERS.collector, FILTERS.author, FILTERS.imageSize };
	    case video:
		return new FILTERS[] { FILTERS.service, FILTERS.date, FILTERS.group, FILTERS.collector, FILTERS.author, FILTERS.videoDuration };
	    default:
		return FILTERS.values();
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
		return normalizeServiceName(item);
	    case group:
		return getGroupNameById(item);
	    default:
		return item;
	    }
	}
    };

    public void clean()
    {
	configFilters.clear();
	resourceCounterFlag = true;
	canNotRequestLearnweb = false;
	canNotRequestInterweb = false;
    }

    public void cleanAll()
    {
	configFilters.clear();
	availableResources.clear();
	resourceCounterFlag = true;
	canNotRequestLearnweb = false;
	canNotRequestInterweb = false;
    }

    public void putResourceCounter(List<FacetField> ffs)
    {
	for(FacetField ff : ffs)
	{
	    if(ff.getValues().size() <= 0)
	    {
		continue;
	    }
	    else if(ff.getName().equals("location"))
	    {
		putResourceCounter(FILTERS.service, ff.getValues());
	    }
	    else if(ff.getName().equals("groups"))
	    {
		putResourceCounter(FILTERS.group, ff.getValues());
	    }
	    else if(ff.getName().equals("collector_s"))
	    {
		putResourceCounter(FILTERS.collector, ff.getValues());
	    }
	    else if(ff.getName().equals("author_s"))
	    {
		putResourceCounter(FILTERS.author, ff.getValues());
	    }
	}
    }

    public void putResourceCounter(FILTERS f, List<Count> counts)
    {
	if(!configFilters.containsKey(f))
	{
	    if(f.equals(FILTERS.service))
	    {
		if(!resourceCounterFlag)
		{
		    List<Count> current = availableResources.get(f);
		    current.addAll(counts);
		    availableResources.put(f, current);
		    resourceCounterFlag = true;
		}
		else
		{
		    resourceCounterFlag = false;
		    availableResources.put(f, counts);
		}
	    }
	    else
	    {
		availableResources.put(f, counts);
	    }
	}
    }

    public void setFiltersFromString(String filters)
    {
	log.debug("Filters: " + filters);
	this.stringFilters = filters;

	if(filters == null)
	{
	    cleanAll();
	}
	else
	{
	    clean();
	    String[] tempFilters = filters.split(",");

	    for(String filter : tempFilters)
	    {
		String[] nameValue = filter.split(":");
		if(nameValue.length == 2)
		{
		    try
		    {
			FILTERS f = FILTERS.valueOf(nameValue[0]);

			switch(f)
			{
			case service:
			    configFilters.put(f, SERVICE.valueOf(nameValue[1]));
			    break;
			case date:
			    configFilters.put(f, DATE.valueOf(nameValue[1]));
			    break;
			case group:
			case collector:
			case author:
			    canNotRequestInterweb = true;
			    configFilters.put(f, nameValue[1]);
			    break;
			case videoDuration:
			    configFilters.put(f, DURATION.valueOf(nameValue[1]));
			    break;
			case imageSize:
			    configFilters.put(f, SIZE.valueOf(nameValue[1]));
			    break;
			default:
			    configFilters.put(f, nameValue[1]);
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
	if(configFilters.containsKey(f) && stringFilters != null)
	{
	    String output = "";
	    int startIndex = stringFilters.indexOf(f.name());
	    int endIndex = stringFilters.indexOf(',', startIndex);

	    if(startIndex != 0)
	    {
		output += stringFilters.substring(0, startIndex - 1);
	    }

	    if(endIndex != -1)
	    {
		output += stringFilters.substring(endIndex + 1);
	    }

	    return value == null ? (output.isEmpty() ? null : output) : (output.isEmpty() ? (f.name() + ":" + value) : (output + ',' + f.name() + ":" + value));

	    /*for(final Entry<FILTERS, Object> entry : configFilters.entrySet())
	    {
	    if(f.equals(entry.getKey()) && value != null)
	    {
	        output += entry.getKey().name().toLowerCase() + ":" + value + ",";
	    }
	    else
	    {
	        output += entry.getKey().name().toLowerCase() + ":" + entry.getValue() + ",";
	    }
	    }
	    return StringHelper.removeLastComma(output);*/
	}
	else if(value != null)
	{
	    return stringFilters == null ? (f.name() + ":" + value) : (stringFilters + ',' + f.name() + ":" + value);
	}

	return stringFilters;
    }

    public List<Filter> getAvailableFilters()
    {
	List<Filter> list = new ArrayList<Filter>();
	for(FILTERS fs : FILTERS.getFilterByMode(configMode))
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
			FilterItem fi = new FilterItem(fs.getItemName(c.getName()), c.getCount() > 0 ? Long.toString(c.getCount()) : null, changeFilterInUrl(fs, c.getName()), containsFilter && configFilters.get(fs).toString().equals(c.getName()));
			nf.addFilterItem(fi);
		    }
		}
		break;
	    case date:
		if(!configFilters.containsKey(FILTERS.service) || !configFilters.get(FILTERS.service).equals(SERVICE.Bing) || !configFilters.get(FILTERS.service).equals(SERVICE.Vimeo))
		{
		    for(DATE d : DATE.values())
		    {
			FilterItem fi = new FilterItem(d.toString(), null, changeFilterInUrl(fs, d.name()), containsFilter && configFilters.get(fs).equals(d));
			nf.addFilterItem(fi);
		    }
		}
		break;
	    case group:
	    case collector:
	    case author:
		if(availableResources.containsKey(fs))
		{
		    for(Count c : availableResources.get(fs))
		    {
			if(c.getName().isEmpty())
			    continue;
			FilterItem fi = new FilterItem(fs.getItemName(c.getName()), Long.toString(c.getCount()), changeFilterInUrl(fs, c.getName()), containsFilter && configFilters.get(fs).equals(c.getName()));
			nf.addFilterItem(fi);
		    }
		}
		break;
	    case videoDuration:
		for(DURATION d : DURATION.values())
		{
		    FilterItem fi = new FilterItem(d.toString(), null, changeFilterInUrl(fs, d.name()), containsFilter && configFilters.get(fs).equals(d));
		    nf.addFilterItem(fi);
		}
		break;
	    case imageSize:
		for(SIZE d : SIZE.values())
		{
		    FilterItem fi = new FilterItem(d.toString(), null, changeFilterInUrl(fs, d.name()), containsFilter && configFilters.get(fs).equals(d));
		    nf.addFilterItem(fi);
		}
		break;
	    case language:
		break;
	    }

	    if(nf.getItems().size() != 0)
	    {
		list.add(nf);
	    }
	}
	return list;
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
	if(configFilters.containsKey(FILTERS.imageSize) && res.getResource().getType().equals("Image"))
	{
	    SIZE configSize = (SIZE) configFilters.get(FILTERS.imageSize);
	    int width = res.getThumbnail4().getWidth(), minWidth = configSize.getMinWidth(), maxWidth = configSize.getMaxWidth();

	    if(minWidth > width || (maxWidth != 0 && width > maxWidth))
	    {
		return false;
	    }
	}

	if(configFilters.containsKey(FILTERS.videoDuration) && res.getResource().getType().equals("Video"))
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

    public String getServiceFilter()
    {
	if(configFilters.containsKey(FILTERS.service))
	{
	    return configFilters.get(FILTERS.service).toString();
	}
	return null;
    }

    public String getDateFromFilterAsString()
    {
	if(configFilters.containsKey(FILTERS.date))
	{
	    return configFilters.get(FILTERS.date).toString();
	}
	return null;
    }

    public Date getDateFromFilter()
    {
	if(configFilters.containsKey(FILTERS.date))
	{
	    return ((DATE) configFilters.get(FILTERS.date)).getDate();
	}
	return null;
    }

    public String getDateToFilter()
    {
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

    public void setMode(MODE m)
    {
	if(m != configMode)
	{
	    this.configMode = m;
	    cleanAll();
	}
    }

    public String getLanguageFilter()
    {
	return (String) configFilters.get(FILTERS.language);
    }

    public void setLanguageFilter(String language)
    {
	configFilters.put(FILTERS.language, language);
    }

    public boolean isLearnwebSearchEnabled()
    {
	if(configFilters.containsKey(FILTERS.service))
	{
	    return ((SERVICE) configFilters.get(FILTERS.service)).isLearnwebSource();
	}
	return !canNotRequestLearnweb;
    }

    public boolean isInterwebSearchEnabled()
    {
	if(configFilters.containsKey(FILTERS.service))
	{
	    return !((SERVICE) configFilters.get(FILTERS.service)).isLearnwebSource();
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
	    //e.printStackTrace();
	    return id;
	}
    }

    public static String normalizeServiceName(String name)
    {
	if(name == null || name.isEmpty())
	{
	    return null;
	}
	else if(name.equals("learnweb"))
	{
	    return "LearnWeb";
	}
	else if(name.equals("archiveit"))
	{
	    return "Archive-It";
	}
	else if(name.equals("loro"))
	{
	    return "Loro";
	}
	else if(name.equals("ted"))
	{
	    return "TED";
	}
	else if(name.equals("tedx"))
	{
	    return "TEDx";
	}
	else if(name.equals("yovisto"))
	{
	    return "Yovisto";
	}

	return name;
    }

    public static class Filter
    {
	private String name;
	private String anyText;
	private String anyUrl;
	private boolean active;
	private List<FilterItem> filterItems;

	public Filter(String name, String anyText, String anyUrl, boolean active)
	{
	    this.name = name;
	    this.anyText = anyText;
	    this.anyUrl = anyUrl;
	    this.active = active;
	    this.filterItems = new ArrayList<FilterItem>();
	}

	public String getName()
	{
	    return name;
	}

	public String getAnyText()
	{
	    return anyText;
	}

	public String getAnyUrl()
	{
	    return anyUrl;
	}

	public void addFilterItem(FilterItem i)
	{
	    filterItems.add(i);
	}

	public List<FilterItem> getItems()
	{
	    return filterItems;
	}

	public boolean isActive()
	{
	    return active;
	}
    }

    public static class FilterItem
    {
	private String name;
	private String counter;
	private String filterUrl;
	private boolean active;

	public FilterItem(String name, String counter, String filterUrl, boolean active)
	{
	    this.name = name;
	    this.counter = counter;
	    this.filterUrl = filterUrl;
	    this.active = active;
	}

	public String getName()
	{
	    return name;
	}

	public String getCounter()
	{
	    return counter;
	}

	public String getUrl()
	{
	    return filterUrl;
	}

	public boolean isActive()
	{
	    return active;
	}
    }
}
