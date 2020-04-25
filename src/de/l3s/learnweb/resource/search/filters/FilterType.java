package de.l3s.learnweb.resource.search.filters;

import org.apache.logging.log4j.LogManager;

import de.l3s.learnweb.resource.search.SearchMode;

public enum FilterType
{
    service(ServiceFilter.class),
    type(TypeFilter.class),
    date(DateFilter.class),
    group(GroupFilter.class),
    collector(Filter.class),
    author(Filter.class),
    coverage(Filter.class),
    publisher(Filter.class),
    tags(Filter.class),
    size(SizeFilter.class),
    duration(DurationFilter.class),
    language(Filter.class);

    private final Class<? extends Filter> filterClass;

    FilterType(final Class<? extends Filter> filterClass)
    {
        this.filterClass = filterClass;
    }

    public Filter createFilter()
    {
        try
        {
            return filterClass.getConstructor(FilterType.class).newInstance(this);
        }
        catch(ReflectiveOperationException e)
        {
            LogManager.getLogger(FilterType.class).error("Can't create a filter for type {}", this, e);
            return new Filter(this);
        }
    }

    public static FilterType[] getFilters(SearchMode searchMode)
    {
        switch(searchMode)
        {
            case text:
                return new FilterType[]{ service, date, group, collector, author, coverage, publisher, tags };
            case image:
                return new FilterType[]{ service, date, group, author, tags, size };
            case video:
                return new FilterType[]{ service, date, group, author, tags, duration };
            case group:
                return new FilterType[]{ service, type, date, collector, author, coverage, publisher, tags };
            default:
                return values();
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
