package de.l3s.learnweb.resource.search.filters;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;

import org.apache.solr.client.solrj.response.FacetField;

public class DateFilter extends Filter
{
    private static final long serialVersionUID = 560235287682777075L;

    private enum DATE_VALUES
    {
        day,
        week,
        month,
        year,
        older
    }

    private DATE_VALUES value;
    private final ArrayList<FilterOption> defaultOptions = new ArrayList<>();

    public DateFilter(final FilterType type)
    {
        super(type);

        for (DATE_VALUES date : DATE_VALUES.values())
            defaultOptions.add(new FilterOption(null, date.name(), null, false));
    }

    @Override
    public ArrayList<FilterOption> getOptions()
    {
        return super.getOptions().isEmpty() ? defaultOptions : super.getOptions();
    }

    @Override
    public void createOption(final FacetField.Count count)
    {
        createOption(null, count.getName(), count.getCount());
    }

    @Override
    public void setActiveValue(final String activeValue)
    {
        super.setActiveValue(activeValue);
        if (activeValue != null)
            this.value = DATE_VALUES.valueOf(activeValue);
    }

    public Instant getDateFrom()
    {
        switch(value)
        {
            case day:
                return ZonedDateTime.now().minusDays(1).toInstant();
            case week:
                return ZonedDateTime.now().minusWeeks(1).toInstant();
            case month:
                return ZonedDateTime.now().minusMonths(1).toInstant();
            case year:
                return ZonedDateTime.now().minusYears(1).toInstant();
            default:
                return null;
        }
    }

    public Instant getDateTo()
    {
        switch(value)
        {
            case older:
                return ZonedDateTime.now().minusYears(1).toInstant();
            default:
                return null;
        }
    }
}
