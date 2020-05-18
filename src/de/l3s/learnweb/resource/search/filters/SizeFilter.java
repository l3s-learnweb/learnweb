package de.l3s.learnweb.resource.search.filters;

import java.util.ArrayList;

public class SizeFilter extends Filter {
    private static final long serialVersionUID = -3826334775933813398L;

    private enum DefaultValues {
        small,
        medium,
        large,
        xlarge
    }

    private DefaultValues value;
    private final ArrayList<FilterOption> defaultOptions = new ArrayList<>();

    public SizeFilter(final FilterType type) {
        super(type);

        for (DefaultValues size : DefaultValues.values()) {
            defaultOptions.add(new FilterOption(null, size.name(), null, false));
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

    public boolean isValid(int width) {
        switch (value) {
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
