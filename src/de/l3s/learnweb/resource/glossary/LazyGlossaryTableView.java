package de.l3s.learnweb.resource.glossary;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

public class LazyGlossaryTableView extends LazyDataModel<GlossaryTableView>
{
    private static final long serialVersionUID = 4388428278103454292L;
    private static final Logger log = Logger.getLogger(LazyGlossaryTableView.class);
    static final int PAGE_SIZE_MULTIPLICATOR = 20; // necessary to make the datatable show multiple rows per GlossaryEntry. I assume that no GlossaryEntry will have more than 20 GlossaryTerms

    private GlossaryResource glossaryResource;

    public LazyGlossaryTableView(GlossaryResource glossaryResource)
    {
        this.glossaryResource = glossaryResource;
        setRowCount(glossaryResource.getEntries().size() * PAGE_SIZE_MULTIPLICATOR);
    }

    /*
    @Override
    public GlossaryTableView getRowData(String rowKey)
    {
        for(GlossaryTableView GlossaryTableView : datasource)
        {
            if(Integer.valueOf(GlossaryTableView.getTermId()).equals(rowKey)) // TODO fix
            {
                return GlossaryTableView;
            }
        }
    
        return null;
    }
    
    @Override
    public Object getRowKey(GlossaryTableView GlossaryTableView)
    {
    
        return Integer.valueOf(GlossaryTableView.getTermId());
    }*/

    @Override
    public List<GlossaryTableView> load(int first,
            int pageSize,
            String sortField,
            SortOrder sortOrder,
            Map<String, Object> filters)
    {
        // create list of predicates for the given filters
        List<Predicate<GlossaryEntry>> allPredicates = new ArrayList<Predicate<GlossaryEntry>>();

        Map<String, String> simpleFilters = new HashMap<>(); // copies all non empty filters for fields of type String

        for(Entry<String, Object> entry : filters.entrySet())
        {
            String filterFieldOrginal = entry.getKey();
            Object filterValue = entry.getValue();

            if(null == filterFieldOrginal)
                continue;

            if(filterFieldOrginal.equals("language"))
            {
                @SuppressWarnings("unchecked")
                List<Locale> localesFilter = (List<Locale>) filterValue;

                allPredicates.add(e -> e.getTerms().stream().anyMatch(t -> localesFilter.contains(t.getLanguage())));
                continue;
            }
            else if(filterFieldOrginal.equals("globalFilter"))
            {
                filterFieldOrginal = "fulltext";
            }
            final String filterField = filterFieldOrginal;

            // ignore empty filter
            final String filterValueStr = String.valueOf(filterValue).toLowerCase();
            if(StringUtils.isBlank(filterValueStr))
                continue;

            simpleFilters.put(filterField, filterValueStr);

            switch(filterField) // TODO move fields to an ENUM rename topicOne to topic1 and so on
            {
            case "fulltext":
            case "description":
            case "topicOne":
            case "topicTwo":
            case "topicThree":
                log.debug("added filter for:" + filterField + " = " + filterValueStr);
                allPredicates.add(e -> e.get(filterField).toLowerCase().contains(filterValueStr));
                break;
            case "term":
            case "pronounciation":
            case "acronym":
            case "source":
            case "phraseology":
                log.debug("added filter for:" + filterField + " = " + filterValueStr);
                allPredicates.add(e -> e.getTerms().stream().anyMatch(t -> t.get(filterField).toLowerCase().contains(filterValueStr)));
                break;
            default:
                log.warn("unsupported filter:" + filterField);
            }
        }

        List<GlossaryEntry> data = glossaryResource.getEntries().stream()
                .filter(allPredicates.stream().reduce(x -> true, Predicate::and))
                .collect(Collectors.toList());

        // single column sort
        Collections.sort(data, new LazySorter(sortField, sortOrder));

        /*
        //multi sort
        if(sortMeta != null && !sortMeta.isEmpty())
        {
            for(SortMeta meta : sortMeta.values())
            {
                Collections.sort(data, new LazySorter(meta.getSortField(), meta.getSortOrder()));
            }
        }*/

        //paginate
        pageSize = pageSize / PAGE_SIZE_MULTIPLICATOR;
        first = (first == 0) ? 0 : first / PAGE_SIZE_MULTIPLICATOR;

        int dataSize = data.size();
        this.setRowCount(dataSize * PAGE_SIZE_MULTIPLICATOR);

        List<GlossaryEntry> page;

        if(dataSize > pageSize) //
        {
            try
            {
                page = data.subList(first, first + pageSize);
            }
            catch(IndexOutOfBoundsException e)
            {
                page = data.subList(first, first + (dataSize % pageSize));
            }
        }
        else
        {
            page = data;
        }

        // expand glossary entries; one row for term in each entry
        ArrayList<GlossaryTableView> tableView = new ArrayList<>(page.size() * 3);

        for(GlossaryEntry entry : page)
        {
            for(GlossaryTerm term : entry.getTerms())
            {
                tableView.add(new GlossaryTableView(entry, term, simpleFilters));
            }
        }
        return tableView;
    }

    public static class LazySorter implements Comparator<GlossaryEntry>
    {
        private String sortField;
        private SortOrder sortOrder;
        private Method fieldGetMethod;

        public LazySorter(String sortField, SortOrder sortOrder) throws SecurityException
        {
            this.sortField = sortField;
            this.sortOrder = sortOrder;

            try
            {
                fieldGetMethod = GlossaryEntry.class.getDeclaredMethod("get" + StringUtils.capitalize(sortField));
            }
            catch(Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public int compare(GlossaryEntry car1, GlossaryEntry car2)
        {
            if(car1 == null || car2 == null)
            {
                log.error("Sorting objects must not be null");
                return 0;
            }
            try
            {
                Object value1 = fieldGetMethod.invoke(car1);
                Object value2 = fieldGetMethod.invoke(car2);

                int value;

                if(value1 instanceof String)
                    value = String.CASE_INSENSITIVE_ORDER.compare((String) value1, (String) value2);
                else
                    value = ((Comparable) value1).compareTo(value2);

                return SortOrder.ASCENDING.equals(sortOrder) ? value : -1 * value;
            }
            catch(Exception e)
            {
                log.error("Sorting failed for field: " + sortField + " order: " + sortOrder, e);

                return 0;
            }
        }
    }
}
