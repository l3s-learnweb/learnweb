package de.l3s.learnweb.beans.admin;

import java.io.Serial;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.Handle;
import org.primefaces.model.dashboard.DashboardModel;
import org.primefaces.model.dashboard.DefaultDashboardModel;
import org.primefaces.model.dashboard.DefaultDashboardWidget;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.exceptions.BadRequestHttpException;
import de.l3s.learnweb.exceptions.HttpException;
import de.l3s.learnweb.group.FolderDao;
import de.l3s.learnweb.group.GroupDao;
import de.l3s.learnweb.i18n.MessagesBundle;
import de.l3s.learnweb.i18n.bundles.SizedResourceBundle;
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
    private transient List<LocaleObject> localeObjects;
    private transient Integer totalResources;
    private transient Integer indexedResources;
    private transient Integer reindexProgress;

    public AdminSystemBean() {
        model = new DefaultDashboardModel();
        model.addWidget(new DefaultDashboardWidget("memory", "col-12 col-lg-6 col-xl-4"));
        model.addWidget(new DefaultDashboardWidget("solr_index", "col-12 col-lg-6 col-xl-4"));
        model.addWidget(new DefaultDashboardWidget("maintenance", "col-12 col-lg-6 col-xl-4"));
        model.addWidget(new DefaultDashboardWidget("cache", "col-12 col-lg-6 col-xl-4"));
        model.addWidget(new DefaultDashboardWidget("i18n", "col-12 col-lg-6 col-xl-4"));
        model.addWidget(new DefaultDashboardWidget("error_handling", "col-12 col-lg-6 col-xl-4"));
        model.addWidget(new DefaultDashboardWidget("db_connections", "col-12"));
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
        } catch (Exception e) {
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

    public void clearLocales() {
        MessagesBundle.clearLocaleCache();

        addGrowl(FacesMessage.SEVERITY_INFO, "Locales cleared");
        localeObjects = null;
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

    public List<LocaleObject> getLocaleObjects() {
        if (localeObjects == null) {
            localeObjects = new ArrayList<>();
            MessagesBundle.getLocaleCache().forEach(entry -> {
                String locale = entry.getValue().getLocale().toString();
                int size = entry.getValue().keySet().size();
                if (locale.isEmpty()) {
                    locale = "[default]";
                }
                if (entry.getValue() instanceof SizedResourceBundle bundle) {
                    size = bundle.size();
                }
                localeObjects.add(new LocaleObject(entry.getKey().toString(), locale, size));
            });
        }
        return localeObjects;
    }

    public record DatabaseProcess(int id, String user, String host, String db, String command, String time, String state, String info, String progress) {}

    public record CacheObject(String name, int size, long sizeSecondary) {}

    public record LocaleObject(String locale, String bundleLocale, int bundleSize) {}

    public record RuntimeInfo(String totalMemory, String freeMemory, String maxMemory) {
        private RuntimeInfo(Runtime runtime) {
            this(
                FileUtils.byteCountToDisplaySize(runtime.totalMemory()),
                FileUtils.byteCountToDisplaySize(runtime.freeMemory()),
                FileUtils.byteCountToDisplaySize(runtime.maxMemory())
            );
        }
    }

    @SuppressWarnings("ProhibitedExceptionThrown")
    public void throwRuntimeException() {
        throw new RuntimeException("peek-a-boo");
    }

    public void throwSQLException() throws SQLException {
        throw new SQLException("DB fail");
    }

    public void throwHttpException() throws HttpException {
        throw new BadRequestHttpException("Demo error");
    }
}
