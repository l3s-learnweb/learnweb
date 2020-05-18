package de.l3s.learnweb.resource.search.filters;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringJoiner;

import org.apache.solr.client.solrj.response.FacetField;

/**
 * Default Solr Filter, all options are dynamic values.
 */
public class Filter implements Serializable {
    private static final long serialVersionUID = 4814395595067137441L;

    private final FilterType type;
    private String activeValue;
    private final ArrayList<FilterOption> options = new ArrayList<>();

    public Filter(final FilterType type) {
        this.type = type;
    }

    public FilterType getType() {
        return type;
    }

    public String getTitle() {
        if (!isActive()) {
            return null;
        }

        for (FilterOption option : options) {
            if (option.isActive()) {
                return option.getTitle();
            }
        }

        return null;
    }

    public boolean isActive() {
        return activeValue != null;
    }

    public String getActiveValue() {
        return activeValue;
    }

    public void setActiveValue(final String activeValue) {
        // remove old active option
        if (this.activeValue != null && !this.activeValue.equals(activeValue)) {
            FilterOption option = findOption(this.activeValue);
            if (option != null) {
                option.setActive(false);
            }
        }

        // set new active option
        if (activeValue != null) {
            FilterOption option = findOption(activeValue);
            if (option != null) {
                option.setActive(true);
            }
        }

        this.activeValue = activeValue;
    }

    public boolean isDisabled() {
        return options.isEmpty();
    }

    public ArrayList<FilterOption> getOptions() {
        return options;
    }

    public FilterOption findOption(String value) {
        if (!options.isEmpty()) {
            for (FilterOption option : options) {
                if (option.getValue().equals(value)) {
                    return option;
                }
            }
        }

        return null;
    }

    public void createOption(final Collection<FacetField.Count> counts, boolean merge) {
        if (!merge) {
            options.clear();
        }

        for (FacetField.Count count : counts) {
            createOption(count);
        }
    }

    public void createOption(final FacetField.Count count) {
        createOption(count.getName(), count.getName(), count.getCount());
    }

    public void createOption(final String title, final String value, final Long count) {
        addOption(new FilterOption(title, value, count, activeValue != null && activeValue.equals(value)));
    }

    private void addOption(FilterOption option) {
        int index = options.indexOf(option);
        if (index == -1) {
            options.add(option);
        } else {
            options.set(index, option);
        }
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Filter.class.getSimpleName() + "(" + type + ")[", "]")
            .add("activeValue='" + activeValue + "'")
            .add("options=" + options)
            .toString();
    }
}
