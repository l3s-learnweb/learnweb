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

@Named
@SessionScoped
public class PkgBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 9067603779789276428L;
    private Pkg pkg;
    @Inject
    private GroupDao groupDao;

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
        pkg.createPkg(groupId);
    }

    public void calculateGraph() throws InterruptedException {
        pkg.calculateSumWeight();
    }

    public List<JsonSharedObject> createSharedObject(int groupId, int numberEntities, boolean isAscending, String application) {
        return pkg.createSharedObject(groupId, numberEntities, isAscending, application);
    }

    public void updatePkg(AnnotationCount annotationCount, User user) {
        //pkg.updatePkg(annotationCount, user, );
    }

    public JsonSharedObject createSingleGraph(int userId, int groupId) {
        return pkg.createSingleGraph(userId, groupId);
    }
}
