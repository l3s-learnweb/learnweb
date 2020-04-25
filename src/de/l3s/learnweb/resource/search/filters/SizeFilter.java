package de.l3s.learnweb.resource.search.filters;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.solr.client.solrj.response.FacetField;

public class SizeFilter extends Filter
{
    private static final long serialVersionUID = -3826334775933813398L;

    private enum SIZE_VALUES
    {
        small,
        medium,
        large,
        xlarge
    }

    private SIZE_VALUES value;
    private final ArrayList<FilterOption> defaultOptions = new ArrayList<>();


    public SizeFilter(final FilterType type)
    {
        super(type);
        
        for (SIZE_VALUES size : SIZE_VALUES.values())
            defaultOptions.add(new FilterOption(null, size.name(), null, false));
    }

    @Override
    public ArrayList<FilterOption> getOptions()
    {
        return super.getOptions().isEmpty() ? defaultOptions : super.getOptions();
    }

    @Override
    public void setActiveValue(final String activeValue)
    {
        super.setActiveValue(activeValue);
        if (activeValue != null)
            this.value = SIZE_VALUES.valueOf(activeValue);
    }

    public boolean isValid(int width)
    {
        switch(value)
        {
            case small:
                return width <= 150;
            case medium:
                return width > 150 && width <= 600;
            case large:
                return width > 600 && width <= 1200;
            case xlarge:
                return width > 1200;
            default:
                return true;
        }
    }
}
