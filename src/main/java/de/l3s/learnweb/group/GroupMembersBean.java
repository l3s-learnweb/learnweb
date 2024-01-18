package de.l3s.learnweb.group;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.forum.ForumPostDao;
import de.l3s.learnweb.resource.ResourceDao;
import de.l3s.learnweb.user.Organisation;
import de.l3s.learnweb.user.User;

@Named
@ViewScoped
public class GroupMembersBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = -235626744365943867L;
    //private static final Logger log = LogManager.getLogger(GroupMembersBean.class);

    private enum MembersView {
        grid,
        list
    }

    private int groupId;
    private Group group;
    private MembersView view = MembersView.grid;

    private transient List<User> members;
    private transient Map<Integer, Integer> postCounts;
    private transient Map<Integer, Integer> resourceCounts;

    @Inject
    private GroupDao groupDao;

    @Inject
    private ResourceDao resourceDao;

    @Inject
    private ForumPostDao forumPostDao;

    public void onLoad() {
        User user = getUser();
        BeanAssert.authorized(user);

        group = groupDao.findByIdOrElseThrow(groupId);

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

    public boolean isMember() {
        User user = getUser();

        if (null == user) {
            return false;
        }

        if (null == group) {
            return false;
        }

        return group.isMember(user);
    }

    public List<User> getMembers() {
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

    public int getForumPostCounts(int userId) {
        if (postCounts == null) {
            postCounts = forumPostDao.countPerUserByGroupId(groupId);
        }
        return postCounts.getOrDefault(userId, 0);
    }

    public int getResourcesCount(int userId) {
        if (resourceCounts == null) {
            resourceCounts = resourceDao.countPerUserByGroupId(groupId);
        }
        return resourceCounts.getOrDefault(userId, 0);
    }
}
