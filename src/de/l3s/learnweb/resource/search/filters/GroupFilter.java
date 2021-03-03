package de.l3s.learnweb.resource.search.filters;

import org.apache.solr.client.solrj.response.FacetField;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.group.Group;

public class GroupFilter extends Filter {
    private static final long serialVersionUID = 7210708293516694728L;

    public GroupFilter(final FilterType type) {
        super(type);
    }

    @Override
    public void createOption(final FacetField.Count count) {
        String title = getOptionTitle(count.getName());
        createOption(title, count.getName(), count.getCount());
    }

    private static String getOptionTitle(String groupId) {
        try {
            return Learnweb.dao().getGroupDao().findById(Integer.parseInt(groupId)).map(Group::getTitle).orElse("deleted");
        } catch (NumberFormatException e) {
            return groupId;
        }
    }
}
