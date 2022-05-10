package de.l3s.learnweb.beans.admin;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.jdbi.v3.core.Handle;
import org.primefaces.model.DashboardColumn;
import org.primefaces.model.DashboardModel;
import org.primefaces.model.DefaultDashboardColumn;
import org.primefaces.model.DefaultDashboardModel;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.group.FolderDao;
import de.l3s.learnweb.group.GroupDao;
import de.l3s.learnweb.resource.FileDao;
import de.l3s.learnweb.resource.ResourceDao;
import de.l3s.learnweb.user.CourseDao;
import de.l3s.learnweb.user.OrganisationDao;
import de.l3s.learnweb.user.UserDao;
import de.l3s.maintenance.resources.ReindexResources;

@Named
@ViewScoped
public class AdminSystemBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 1354024417928664741L;
    private static final Logger log = LogManager.getLogger(AdminSystemBean.class);

    private final DashboardModel model;

    private transient RuntimeInfo runtimeInfo;
    private transient List<DatabaseProcess> databaseProcesses;
    private transient List<CacheObject> cacheObjects;
    private transient Integer totalResources;
    private transient Integer indexedResources;
    private transient Integer reindexProgress;

    public AdminSystemBean() {
        model = new DefaultDashboardModel();

        DashboardColumn column1 = new DefaultDashboardColumn();
        column1.addWidget("memory");
        column1.addWidget("cache");
        model.addColumn(column1);

        DashboardColumn column2 = new DefaultDashboardColumn();
        column2.addWidget("maintenance");
        model.addColumn(column2);

        DashboardColumn column3 = new DefaultDashboardColumn();
        column3.addWidget("solrIndex");
        model.addColumn(column3);
    }

    public DashboardModel getModel() {
        return model;
    }

    public Integer getTotalResources() {
        if (totalResources == null) {
            totalResources = dao().getResourceDao().countUndeleted();
        }
        return totalResources;
    }

    public Integer getIndexedResources() {
        if (indexedResources == null) {
            indexedResources = Math.toIntExact(getLearnweb().getSolrClient().countResources("*:*"));
        }
        return indexedResources;
    }

    public RuntimeInfo getRuntimeInfo() {
        if (runtimeInfo == null) {
            runtimeInfo = new RuntimeInfo(Runtime.getRuntime());
        }
        return runtimeInfo;
    }

    public void terminateProcess(DatabaseProcess process) {
        try (Handle handle = getLearnweb().openJdbiHandle()) {
            handle.execute("KILL ?", process.id());
        }

        addGrowl(FacesMessage.SEVERITY_INFO, "Killed process " + process.id());
        databaseProcesses = null;
    }

    public void terminateAllProcesses() {
        try (Handle handle = getLearnweb().openJdbiHandle()) {
            for (DatabaseProcess process : databaseProcesses) {
                handle.execute("KILL ?", process.id());
            }
        }

        addGrowl(FacesMessage.SEVERITY_INFO, "All processes terminated");
        databaseProcesses = null;
    }

    public void reindexResources() {
        reindexProgress = 0;

        try {
            ReindexResources task = new ReindexResources(getLearnweb(), progress -> reindexProgress = progress);
            task.run(false);
        } catch (IOException | SolrServerException e) {
            log.error("Error reindexing resources", e);
        } finally {
            indexedResources = null;
            totalResources = null;
        }
    }

    public void onReindexComplete() {
        addGrowl(FacesMessage.SEVERITY_INFO, "Reindex completed");
    }

    public Integer getReindexProgress() {
        return reindexProgress;
    }

    public void clearAllCaches() {
        OrganisationDao.cache.clear();
        UserDao.cache.clear();
        CourseDao.cache.clear();
        GroupDao.cache.clear();
        FolderDao.cache.clear();
        ResourceDao.cache.clear();
        FileDao.cache.clear();

        addGrowl(FacesMessage.SEVERITY_INFO, "Caches cleared");
        cacheObjects = null;
    }

    public void onMaintenanceUpdate() {
        if (config().isMaintenance()) {
            addGrowl(FacesMessage.SEVERITY_WARN, "Maintenance enabled");
        } else {
            addGrowl(FacesMessage.SEVERITY_INFO, "Maintenance disabled");
        }
    }

    public List<DatabaseProcess> getDatabaseProcesses() {
        if (databaseProcesses == null) {
            try (Handle handle = getLearnweb().openJdbiHandle()) {
                databaseProcesses = handle.select("SHOW FULL PROCESSLIST").map((rs, ctx) -> {
                    DatabaseProcess ps = new DatabaseProcess(
                        rs.getInt("Id"),
                        rs.getString("User"),
                        rs.getString("Host"),
                        rs.getString("db"),
                        rs.getString("Command"),
                        rs.getString("Time"),
                        rs.getString("State"),
                        rs.getString("Info"),
                        rs.getString("Progress")
                    );
                    return ps;
                }).list();
            }
        }
        return databaseProcesses;
    }

    public List<CacheObject> getCacheObjects() {
        if (cacheObjects == null) {
            cacheObjects = new ArrayList<>();
            cacheObjects.add(new CacheObject("Resources", ResourceDao.cache.size(), ResourceDao.cache.sizeSecondaryCache()));
            cacheObjects.add(new CacheObject("Groups", GroupDao.cache.size(), GroupDao.cache.sizeSecondaryCache()));
            cacheObjects.add(new CacheObject("Users", UserDao.cache.size(), UserDao.cache.sizeSecondaryCache()));
            cacheObjects.add(new CacheObject("Folders", FolderDao.cache.size(), FolderDao.cache.sizeSecondaryCache()));
            cacheObjects.add(new CacheObject("Courses", CourseDao.cache.size(), CourseDao.cache.sizeSecondaryCache()));
            cacheObjects.add(new CacheObject("Organisations", OrganisationDao.cache.size(), OrganisationDao.cache.sizeSecondaryCache()));
            cacheObjects.add(new CacheObject("Files", FileDao.cache.size(), FileDao.cache.sizeSecondaryCache()));
        }
        return cacheObjects;
    }

    public record DatabaseProcess(int id, String user, String host, String db, String command, String time, String state, String info, String progress) {}

    public record CacheObject(String name, int size, long sizeSecondary) {}

    public record RuntimeInfo(String totalMemory, String freeMemory, String maxMemory) {
        private RuntimeInfo(Runtime runtime) {
            this(
                FileUtils.byteCountToDisplaySize(runtime.totalMemory()),
                FileUtils.byteCountToDisplaySize(runtime.freeMemory()),
                FileUtils.byteCountToDisplaySize(runtime.maxMemory())
            );
        }
    }
}
