package de.l3s.learnweb.resource.search.filters;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.UtilBean;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.search.SearchMode;

import java.sql.SQLException;

public enum FilterType
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

    public static FilterType[] getFilters(SearchMode searchMode)
    {
        switch(searchMode)
        {
            case text:
                return new FilterType[]{ service, date, group, collector, author, coverage, publisher, tags };
            case image:
                return new FilterType[]{ service, date, group, author, tags, imageSize };
            case video:
                return new FilterType[]{ service, date, group, author, tags, videoDuration };
            case group:
                return new FilterType[]{ service, type, date, collector, author, coverage, publisher, tags };
            default:
                return values();
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
                return UtilBean.getLocaleMessage("any_" + name());
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

    private static String getGroupNameById(String groupId)
    {
        try
        {
            Group group = Learnweb.getInstance().getGroupManager().getGroupById(Integer.parseInt(groupId));
            if(null == group)
                return "deleted";
            return group.getTitle();
        }
        catch(NumberFormatException | SQLException e)
        {
            return groupId;
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
                return UtilBean.getLocaleMessage(name());
        }
    }
}
