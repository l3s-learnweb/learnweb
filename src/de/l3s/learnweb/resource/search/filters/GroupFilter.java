package de.l3s.learnweb.resource.search.filters;

import java.sql.SQLException;

import org.apache.solr.client.solrj.response.FacetField;

import de.l3s.learnweb.Learnweb;
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
            Group group = Learnweb.getInstance().getGroupManager().getGroupById(Integer.parseInt(groupId));
            return null == group ? "deleted" : group.getTitle();
        } catch (NumberFormatException | SQLException e) {
            return groupId;
        }
    }
}
