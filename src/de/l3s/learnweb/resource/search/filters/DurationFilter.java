package de.l3s.learnweb.resource.search.filters;

import java.util.ArrayList;

public class DurationFilter extends Filter {
    private static final long serialVersionUID = 9120230856128443235L;

    @SuppressWarnings("StandardVariableNames")
    private enum DefaultValues {
        s,
        m,
        l
    }

    private DefaultValues value;
    private final ArrayList<FilterOption> defaultOptions = new ArrayList<>();

    public DurationFilter(final FilterType type) {
        super(type);

        for (DefaultValues duration : DefaultValues.values()) {
            defaultOptions.add(new FilterOption(null, duration.name(), null, false));
        }
    }

    @Override
    public ArrayList<FilterOption> getOptions() {
        return super.getOptions().isEmpty() ? defaultOptions : super.getOptions();
    }

    @Override
    public void setActiveValue(final String activeValue) {
        super.setActiveValue(activeValue);
        if (activeValue != null) {
            this.value = DefaultValues.valueOf(activeValue);
        }
    }

    public boolean isValid(int duration) {
        switch (value) {
            case s:
                return duration <= 240;
            case m:
                return duration > 240 && duration <= 1200;
            case l:
                return duration > 1200;
            default:
                return true;
        }
    }
}
