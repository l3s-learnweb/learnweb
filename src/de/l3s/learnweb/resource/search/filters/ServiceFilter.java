package de.l3s.learnweb.resource.search.filters;

import org.apache.solr.client.solrj.response.FacetField;

import de.l3s.learnweb.resource.ResourceService;

public class ServiceFilter extends Filter
{
    private static final long serialVersionUID = -465289044335451995L;
    
    private ResourceService service;

    public ServiceFilter(final FilterType type)
    {
        super(type);
    }

    @Override
    public void setActiveValue(final String activeValue)
    {
        super.setActiveValue(activeValue);
        if (activeValue != null)
            this.service = ResourceService.parse(activeValue);
    }

    @Override
    public void createOption(final FacetField.Count count)
    {
        String title = ResourceService.parse(count.getName()).toString();
        createOption(title, count.getName().toLowerCase(), count.getCount());
    }

    public ResourceService getService()
    {
        return service;
    }
}
