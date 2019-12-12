package de.l3s.learnweb.resource.search.filters;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Filter implements Serializable
{
    private static final long serialVersionUID = 4814395595067137441L;

    private final String title;
    private final String anyTitle;
    private final String anyValue;
    private boolean active;
    private List<FilterOption> options;

    public Filter(final String title, final String anyTitle, final String anyValue, final boolean active)
    {
        this.title = title;
        this.anyTitle = anyTitle;
        this.anyValue = anyValue;
        this.active = active;
        this.options = new ArrayList<>();
    }

    public String getTitle()
    {
        return title;
    }

    public String getAnyTitle()
    {
        return anyTitle;
    }

    public String getAnyValue()
    {
        return anyValue;
    }

    public boolean isActive()
    {
        return active;
    }

    public void setActive(boolean active)
    {
        this.active = active;
    }

    public void addOption(FilterOption i)
    {
        options.add(i);
    }

    public List<FilterOption> getOptions()
    {
        return options;
    }

    public void setOptions(final List<FilterOption> options)
    {
        this.options = options;
    }

    public boolean isDisabled()
    {
        return options.isEmpty();
    }
}
