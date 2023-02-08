package de.l3s.learnweb.searchhistory;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.group.GroupDao;
import de.l3s.learnweb.user.User;

@SessionScoped
@Named
public class PkgBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 9067603779789276428L;
    private transient Pkg pkg;
    @Inject
    private GroupDao groupDao;
    @Inject
    private SearchHistoryDao searchHistoryDao;

    @PostConstruct
    public void init() {
        User user = getUser();
        List<Group> groups = groupDao.findByUserId(user.getId());
        int groupId = 0;
        if (!groups.isEmpty()) {
            groupId = groups.get(0).getId();
        }
        pkg = new Pkg(new ArrayList<>(), new ArrayList<>());
        //Find the users in this group
        pkg.createPkg(user, groupId);
    }

    public void trimPkg() {
        pkg.removeDuplicatingNodesAndLinks();
        pkg.calculateSumWeight();
    }

    public void createSharedObject(int groupId, int numberEntities, boolean isAscending, String application) {
        pkg.createSharedObject(getUser(), groupId, numberEntities, isAscending, application);
    }

    public void updatePkg(AnnotationCount annotationCount, User user) {
        pkg.updatePkg(annotationCount, user, searchHistoryDao.findSessionsByUserId(user.getId()));
    }

    public JsonSharedObject createSingleGraph(int userId, int groupId) {
        return pkg.createSingleGraph(userId, groupId);
    }

    /**
     *  * A blank function to trigger init() from PkgBean.
     * */
    public void doSomething() {}

}
