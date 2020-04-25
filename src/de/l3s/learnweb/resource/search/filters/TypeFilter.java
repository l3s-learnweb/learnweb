package de.l3s.learnweb.resource.search.filters;

import org.apache.solr.client.solrj.response.FacetField;

public class TypeFilter extends Filter
{
    private static final long serialVersionUID = -1088922664871138641L;

    public TypeFilter(final FilterType type)
    {
        super(type);
    }

    @Override
    public void createOption(final FacetField.Count count)
    {
        createOption(null, count.getName(), count.getCount());
    }
}
