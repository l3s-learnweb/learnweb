package de.l3s.learnweb.group;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.forum.ForumPostDao;
import de.l3s.learnweb.user.Organisation;
import de.l3s.learnweb.user.User;

@Named
@ViewScoped
public class GroupMembersBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = -235626744365943867L;
    //private static final Logger log = LogManager.getLogger(GroupMembersBean.class);

    private enum MembersView {
        grid,
        list
    }

    private int groupId;
    private Group group;

    private MembersView view = MembersView.grid;
    private List<User> members;

    private transient Map<Integer, Integer> postCounts;
    private transient Map<Integer, Integer> resourceCounts;

    public void onLoad() throws SQLException {
        User user = getUser();
        BeanAssert.authorized(user);

        group = getLearnweb().getGroupManager().getGroupById(groupId);
        BeanAssert.isFound(group);

        if (null != group) {
            group.setLastVisit(user);
        }
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public Group getGroup() {
        return group;
    }

    public boolean isMember() throws SQLException {
        User user = getUser();

        if (null == user) {
            return false;
        }

        if (null == group) {
            return false;
        }

        return group.isMember(user);
    }

    public List<User> getMembers() throws SQLException {
        if (null == members && group != null) {
            members = group.getMembers();
        }
        return members;
    }

    public boolean isUserDetailsHidden() {
        User user = getUser();
        if (user == null) {
            return false;
        }
        if (user.getOrganisation().getOption(Organisation.Option.Privacy_Anonymize_usernames)) {
            return true;
        }
        return false;
    }

    public MembersView getView() {
        return view;
    }

    public void setView(final MembersView view) {
        this.view = view;
    }

    public int getForumPostCounts(int userId) throws SQLException {
        if (postCounts == null) {
            postCounts = Learnweb.getInstance().getJdbi().withExtension(ForumPostDao.class, dao -> dao.getPostsCountPerUserByGroupId(groupId));
        }
        return postCounts.getOrDefault(userId, 0);
    }

    public int getResourcesCount(int userId) throws SQLException {
        if (resourceCounts == null) {
            resourceCounts = Learnweb.getInstance().getResourceManager().getResourceCountPerUserByGroup(groupId);
        }
        return resourceCounts.getOrDefault(userId, 0);
    }
}
