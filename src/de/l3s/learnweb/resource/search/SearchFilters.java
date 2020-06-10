package de.l3s.learnweb.resource.search;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;

import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.ResourceType;
import de.l3s.learnweb.resource.search.filters.DateFilter;
import de.l3s.learnweb.resource.search.filters.DurationFilter;
import de.l3s.learnweb.resource.search.filters.Filter;
import de.l3s.learnweb.resource.search.filters.FilterOption;
import de.l3s.learnweb.resource.search.filters.FilterType;
import de.l3s.learnweb.resource.search.filters.ServiceFilter;
import de.l3s.learnweb.resource.search.filters.SizeFilter;
import de.l3s.util.StringHelper;

/**
 * Dear maintainer:
 * Once you are done trying to 'optimize' this routine, and have realized what a terrible mistake that was,
 * please increment the following counter as a warning to the next guy:
 *
 * total_hours_wasted_here = 58
 */
public class SearchFilters implements Serializable {
    private static final long serialVersionUID = 8012567994091306088L;
    static final Logger log = LogManager.getLogger(SearchFilters.class);

    private SearchMode searchMode;
    private FilterType freezeFilter; // to show alternative options for last selected filter

    // Some filters are available only on certain provider
    private boolean learnwebSearchAvailable = true;
    private boolean interwebSearchAvailable = true;

    private long totalResultsLearnweb;
    private long totalResultsInterweb;

    private final EnumMap<FilterType, Filter> activeFilters = new EnumMap<>(FilterType.class);
    private final EnumMap<FilterType, Filter> availableFilters = new EnumMap<>(FilterType.class);

    public SearchFilters(SearchMode searchMode) {
        this.searchMode = searchMode;
    }

    public void reset() {
        freezeFilter = null;
        activeFilters.clear();
        learnwebSearchAvailable = true;
        interwebSearchAvailable = true;
        resetCounters();
    }

    public void resetCounters() {
        availableFilters.clear();
        totalResultsLearnweb = 0;
        totalResultsInterweb = 0;

        if (freezeFilter != null) {
            availableFilters.put(freezeFilter, activeFilters.get(freezeFilter));
        }
    }

    public String[] getFacetQueries() {
        return new String[] {
            "{!key='date.older'}timestamp:[* TO NOW-365DAY]",
            "{!key='date.year'}timestamp:[NOW-365DAY TO NOW]",
            "{!key='date.month'}timestamp:[NOW-30DAY TO NOW]",
            "{!key='date.week'}timestamp:[NOW-7DAY TO NOW]",
            "{!key='date.day'}timestamp:[NOW-1DAY TO NOW]"
        };
    }

    public String[] getFacetFields() {
        return new String[] {"location", "type", "groupId", "collector_s", "author_s", "coverage_s", "publisher_s",
            "language_level_ss", "yell_target_ss", "yell_purpose_ss", "tags_ss", "language"};
    }

    public void putResourceCounters(Map<String, Integer> facetQueries) {
        for (Map.Entry<String, Integer> facetQuery : facetQueries.entrySet()) {
            String[] tempNames = facetQuery.getKey().split("\\.");
            if (tempNames.length != 2 || facetQuery.getValue() == 0) {
                continue;
            }

            Count count = new Count(new FacetField(tempNames[0]), tempNames[1], facetQuery.getValue());
            if ("date".equals(tempNames[0])) {
                putResourceCounters(FilterType.date, Collections.singletonList(count), true);
            }
        }
    }

    public void putResourceCounters(List<FacetField> facetFields) {
        for (FacetField facetField : facetFields) {
            switch (facetField.getName()) {
                case "location":
                    putResourceCounters(FilterType.service, facetField.getValues(), false);
                    break;
                case "type":
                    putResourceCounters(FilterType.type, facetField.getValues(), false);
                    break;
                case "groupId":
                    putResourceCounters(FilterType.group, facetField.getValues(), false);
                    break;
                case "collector_s":
                    putResourceCounters(FilterType.collector, facetField.getValues(), false);
                    break;
                case "author_s":
                    putResourceCounters(FilterType.author, facetField.getValues(), false);
                    break;
                case "coverage_s":
                    putResourceCounters(FilterType.coverage, facetField.getValues(), false);
                    break;
                case "publisher_s":
                    putResourceCounters(FilterType.publisher, facetField.getValues(), false);
                    break;
                case "language_level_ss":
                    putResourceCounters(FilterType.language_level, facetField.getValues(), false);
                    break;
                case "yell_target_ss":
                    putResourceCounters(FilterType.yell_target, facetField.getValues(), false);
                    break;
                case "yell_purpose_ss":
                    putResourceCounters(FilterType.yell_purpose, facetField.getValues(), false);
                    break;
                case "tags_ss":
                    putResourceCounters(FilterType.tags, facetField.getValues(), false);
                    break;
                case "language":
                    putResourceCounters(FilterType.language, facetField.getValues(), false);
                    break;
                default:
                    log.error("Unknown facetField name {}", facetField);
            }
        }
    }

    public void putResourceCounters(FilterType filterType, Collection<Count> counts, boolean merge) {
        if (freezeFilter == null || freezeFilter != filterType) {
            if (counts.isEmpty() && availableFilters.containsKey(filterType) && !merge) {
                availableFilters.remove(filterType);
            } else if (!counts.isEmpty()) {
                Filter filter = availableFilters.computeIfAbsent(filterType, key -> activeFilters.getOrDefault(key, key.createFilter()));
                filter.createOption(counts, merge);
            }
        }
    }

    /**
     * This method set filters from a string, e.g. url.
     * Example string: filter:foo,filter2:bar,filter3:tar
     */
    public void setFilters(String filters) {
        reset();

        if (StringUtils.isEmpty(filters)) {
            return;
        }

        String[] splitFilters = filters.split(",");

        for (String splitFilter : splitFilters) {
            String[] nameValue = splitFilter.split(":");
            if (nameValue.length != 2) {
                continue; // something wrong, skip it
            }

            FilterType filter = FilterType.valueOf(nameValue[0]);
            String value = filter.isEncodeBase64() ? StringHelper.decodeBase64(nameValue[1]) : nameValue[1];
            setFilter(filter, value);
        }
    }

    public void setFilter(FilterType filterType, String value) {
        if (StringUtils.isBlank(value)) {
            Filter filter = activeFilters.remove(filterType);
            if (filter != null) {
                filter.setActiveValue(null);
                freezeFilter = null;
            }
            return;
        }

        try {
            Filter filter = activeFilters.computeIfAbsent(filterType, key -> availableFilters.computeIfAbsent(key, FilterType::createFilter));
            freezeFilter = filterType;
            filter.setActiveValue(value);
        } catch (IllegalArgumentException e) {
            log.error("Not valid value of {} filter attempt to set active: {}", filterType, value, e);
        }
    }

    public Collection<Filter> getAvailableFilters() {
        return this.getAvailableFilters(new FilterType[] {});
    }

    public Collection<Filter> getAvailableFilters(final FilterType[] excludeFilters) {
        ArrayList<Filter> results = new ArrayList<>();
        FilterType[] possibleFilters = ArrayUtils.removeElements(FilterType.getFilters(searchMode), excludeFilters);

        for (FilterType filterType : possibleFilters) {
            Filter filter = availableFilters.get(filterType);

            if (filterType == FilterType.date) {
                // skip date filter for services that not support it
                Filter sf = availableFilters.get(FilterType.service);
                if (sf != null && ResourceService.vimeo.name().equals(sf.getActiveValue())) {
                    continue;
                }
            }

            if (filter == null && (filterType == FilterType.duration || filterType == FilterType.size || filterType == FilterType.date)) {
                // some filters doesn't have facet fields, just init them
                filter = filterType.createFilter();
                availableFilters.put(filterType, filter);
            }

            if (filter != null) {
                results.add(filter);
            }
        }
        return results;
    }

    /**
     * Check filters applied after resources are loaded from provider.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean checkAfterLoadFilters(Resource res) {
        if (res.getType() == ResourceType.image && activeFilters.containsKey(FilterType.size)) {
            SizeFilter activeFilter = (SizeFilter) activeFilters.get(FilterType.size);
            return activeFilter.isValid(res.getLargestThumbnail().getWidth());
        }

        if (res.getType() == ResourceType.video && activeFilters.containsKey(FilterType.duration)) {
            DurationFilter activeFilter = (DurationFilter) activeFilters.get(FilterType.duration);
            return activeFilter.isValid(res.getDuration());
        }

        return true;
    }

    public SearchMode getSearchMode() {
        return searchMode;
    }

    public void setSearchMode(SearchMode mode) {
        if (mode != searchMode) {
            this.searchMode = mode;
            reset();
        }
    }

    public boolean isFiltersActive() {
        return !activeFilters.isEmpty();
    }

    public boolean isLearnwebSearchEnabled() {
        if (activeFilters.containsKey(FilterType.service)) {
            return !((ServiceFilter) activeFilters.get(FilterType.service)).getService().isInterweb();
        }
        return learnwebSearchAvailable;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isInterwebSearchEnabled() {
        if (activeFilters.containsKey(FilterType.service)) {
            return ((ServiceFilter) activeFilters.get(FilterType.service)).getService().isInterweb();
        }
        return interwebSearchAvailable;
    }

    public long getTotalResults() {
        return totalResultsLearnweb + totalResultsInterweb;
    }

    public long getTotalResults(FilterType filterType, String value) {
        Filter filter = availableFilters.get(filterType);
        if (filter != null) {
            FilterOption option = filter.findOption(value);
            if (option != null) {
                return option.getTotalResults();
            }
        }

        return 0;
    }

    public void setTotalResultsLearnweb(Long totalResultsLearnweb) {
        this.totalResultsLearnweb = totalResultsLearnweb;
    }

    public void setTotalResultsInterweb(Long totalResultsInterweb) {
        this.totalResultsInterweb = totalResultsInterweb;
    }

    public boolean isFilterActive(FilterType filterType) {
        return activeFilters.containsKey(filterType);
    }

    public Filter getFilter(FilterType filterType) {
        return activeFilters.get(filterType);
    }

    public String getFilterValue(FilterType filterType) {
        return activeFilters.get(filterType).getActiveValue();
    }

    /* --- Some special getters for Filters --- */

    public Instant getFilterDateFrom() {
        if (!activeFilters.containsKey(FilterType.date)) {
            return null;
        }

        return ((DateFilter) activeFilters.get(FilterType.date)).getDateFrom();
    }

    public Instant getFilterDateTo() {
        if (!activeFilters.containsKey(FilterType.date)) {
            return null;
        }

        return ((DateFilter) activeFilters.get(FilterType.date)).getDateTo();
    }
}
