package de.l3s.learnweb.resource.search.filters;

import java.io.Serial;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;

import org.apache.solr.client.solrj.response.FacetField;

public class DateFilter extends Filter {
    @Serial
    private static final long serialVersionUID = 560235287682777075L;

    private enum DefaultValues {
        day,
        week,
        month,
        year,
        older
    }

    private DefaultValues value;
    private final ArrayList<FilterOption> defaultOptions = new ArrayList<>();

    public DateFilter(final FilterType type) {
        super(type);

        for (DefaultValues date : DefaultValues.values()) {
            defaultOptions.add(new FilterOption(null, date.name(), null, false));
        }
    }

    @Override
    public ArrayList<FilterOption> getOptions() {
        return super.getOptions().isEmpty() ? defaultOptions : super.getOptions();
    }

    @Override
    public void createOption(final FacetField.Count count) {
        createOption(null, count.getName(), count.getCount());
    }

    @Override
    public void setActiveValue(final String activeValue) {
        super.setActiveValue(activeValue);
        if (activeValue != null) {
            this.value = DefaultValues.valueOf(activeValue);
        }
    }

    public Instant getDateFrom() {
        return switch (value) {
            case day -> ZonedDateTime.now().minusDays(1).toInstant();
            case week -> ZonedDateTime.now().minusWeeks(1).toInstant();
            case month -> ZonedDateTime.now().minusMonths(1).toInstant();
            case year -> ZonedDateTime.now().minusYears(1).toInstant();
            default -> null;
        };
    }

    public Instant getDateTo() {
        return switch (value) {
            case older -> ZonedDateTime.now().minusYears(1).toInstant();
            default -> null;
        };
    }
}
