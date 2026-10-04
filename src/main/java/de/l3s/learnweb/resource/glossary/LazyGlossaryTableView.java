package de.l3s.learnweb.resource.glossary;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortMeta;
import org.primefaces.model.SortOrder;

import de.l3s.util.StringHelper;

public class LazyGlossaryTableView extends LazyDataModel<GlossaryTableView> {
    @Serial
    private static final long serialVersionUID = 4388428278103454292L;
    private static final Logger log = LogManager.getLogger(LazyGlossaryTableView.class);

    /**
     * Necessary to make the datatable show multiple rows per GlossaryEntry.
     * I assume that no GlossaryEntry will have more than 20 GlossaryTerms.
     */
    static final int PAGE_SIZE_MULTIPLICATOR = 20;

    private final GlossaryResource glossaryResource;

    public LazyGlossaryTableView(GlossaryResource glossaryResource) {
        this.glossaryResource = glossaryResource;
    }

    @Override
    public int count(final Map<String, FilterMeta> map) {
        return 0;
    }

    @Override
    public List<GlossaryTableView> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
        // create list of predicates for the given filters
        List<Predicate<GlossaryEntry>> allPredicates = new ArrayList<>();

        Map<String, Pattern> highlightQueries = new HashMap<>(); // the patterns of all non empty filters for fields of type String

        for (FilterMeta meta : filterBy.values()) {
            String filterFieldOriginal = meta.getField();
            Object filterValue = meta.getFilterValue();

            if (null == filterFieldOriginal || filterValue == null) {
                //log.debug("skipped: " + meta);
                continue;
            }
            if (filterFieldOriginal.equals("language")) {
                @SuppressWarnings("unchecked")
                List<Locale> localesFilter = (List<Locale>) filterValue;

                allPredicates.add(e -> e.getTerms().stream().anyMatch(t -> localesFilter.contains(t.getLanguage())));
                continue;
            } else if (filterFieldOriginal.equals("globalFilter")) {
                filterFieldOriginal = "fulltext";
            }
            final String filterField = filterFieldOriginal;

            // ignore empty filter; the matching and highlighting are case-insensitive, lowercasing can change the length (e.g. for "İ")
            final String filterValueStr = String.valueOf(filterValue);
            if (StringUtils.isBlank(filterValueStr)) {
                continue;
            }

            highlightQueries.put(filterField, StringHelper.compileHighlightQuery(filterValueStr));

            switch (filterField) { // TODO @kemkes: move fields to an ENUM rename topicOne to topic1 and so on
                case "fulltext", "description", "topicOne", "topicTwo", "topicThree" ->
                    //log.debug("added filter for:" + filterField + " = " + filterValueStr);
                    allPredicates.add(e -> Strings.CI.contains(e.getField(filterField), filterValueStr));
                case "source" -> // selected from a dropdown of the stored values (GlossaryBean.getSourceFilterOptions)
                    allPredicates.add(e -> e.getTerms().stream().anyMatch(t -> filterValueStr.equalsIgnoreCase(t.getField(filterField))));
                case "term", "pronounciation", "acronym", "phraseology" ->
                    //log.debug("added filter for:" + filterField + " = " + filterValueStr);
                    allPredicates.add(e -> e.getTerms().stream().anyMatch(t -> Strings.CI.contains(t.getField(filterField), filterValueStr)));
                default -> log.error("unsupported filter:{}", filterField);
            }
        }

        List<GlossaryEntry> data = glossaryResource.getEntries().stream()
            .filter(allPredicates.stream().reduce(x -> true, Predicate::and))
            .collect(Collectors.toList()); // we can't use .toList() because it produces unmodifiable list

        // single column sort
        //Collections.sort(data, new LazySorter(field, order));

        //multi sort
        if (sortBy != null && !sortBy.isEmpty()) {
            for (SortMeta meta : sortBy.values()) {
                data.sort(new LazySorter(meta.getField(), meta.getOrder()));
            }
        } else {
            // default sort
            data.sort(new LazySorter("createdAt", SortOrder.DESCENDING));
        }

        //paginate
        pageSize /= PAGE_SIZE_MULTIPLICATOR;
        first = (first == 0) ? 0 : first / PAGE_SIZE_MULTIPLICATOR;

        int dataSize = data.size();
        this.setRowCount(dataSize * PAGE_SIZE_MULTIPLICATOR);

        List<GlossaryEntry> page;

        if (dataSize > pageSize) {
            try {
                page = data.subList(first, first + pageSize);
            } catch (IndexOutOfBoundsException e) {
                page = data.subList(first, first + (dataSize % pageSize));
            }
        } else {
            page = data;
        }

        // expand glossary entries; one row for term in each entry
        ArrayList<GlossaryTableView> tableView = new ArrayList<>(page.size() * 3);

        for (GlossaryEntry entry : page) {
            for (GlossaryTerm term : entry.getTerms()) {
                tableView.add(new GlossaryTableView(entry, term, highlightQueries));
            }
        }
        return tableView;
    }

    public static class LazySorter implements Comparator<GlossaryEntry>, Serializable {
        @Serial
        private static final long serialVersionUID = 899131076207451811L;

        private final String field;
        private final SortOrder order;

        private transient Method fieldGetMethod;

        public LazySorter(String field, SortOrder order) throws SecurityException {
            this.field = field;
            this.order = order;
        }

        private Method getFieldGetMethod() throws NoSuchMethodException {
            if (fieldGetMethod == null) {
                fieldGetMethod = GlossaryEntry.class.getDeclaredMethod("get" + StringUtils.capitalize(field));
            }
            return fieldGetMethod;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public int compare(GlossaryEntry entry1, GlossaryEntry entry2) {
            if (entry1 == null || entry2 == null) {
                log.error("Sorting objects must not be null");
                return 0;
            }
            try {
                Object value1 = getFieldGetMethod().invoke(entry1);
                Object value2 = getFieldGetMethod().invoke(entry2);

                int value;

                if (value1 instanceof String) {
                    value = String.CASE_INSENSITIVE_ORDER.compare((String) value1, (String) value2);
                } else {
                    value = ((Comparable) value1).compareTo(value2);
                }

                return SortOrder.ASCENDING == order ? value : -1 * value;
            } catch (Exception e) {
                log.error("Sorting failed for field: {} order: {}", field, order, e);

                return 0;
            }
        }
    }
}
