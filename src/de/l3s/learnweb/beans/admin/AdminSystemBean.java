package de.l3s.learnweb.beans.admin;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.inject.Named;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.exceptions.BeanAsserts;
import de.l3s.learnweb.user.User;

@Named
@RequestScoped
public class AdminSystemBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = 1354024417928664741L;
    //private static final Logger log = LogManager.getLogger(AdminSystemBean.class);
    private LinkedList<Object> databaseProcessList;
    private String memoryInfo;

    public AdminSystemBean() throws SQLException {
        User user = getUser();
        BeanAsserts.authorized(user);
        BeanAsserts.hasPermission(user.isAdmin());

        loadDatabaseProcessList();

        Runtime rt = Runtime.getRuntime();
        memoryInfo = "Total: " + (rt.totalMemory() / 1024 / 1024) + "mb - Free:" + (rt.freeMemory() / 1024 / 1024) + "mb - Max:" + (rt.maxMemory() / 1024 / 1024);

        // List<CacheStatistic> cacheSize = new ArrayList<>(6);
        // Learnweb lw = getLearnweb();
        // cacheSize.add(new CacheStatistic("Resources", lw.getResourceManager().getCacheSize()));
        // cacheSize.add(new CacheStatistic("Groups", lw.getGroupManager().getGroupCacheSize()));
        // cacheSize.add(new CacheStatistic("Users", lw.getUserManager().getCacheSize()));
        // cacheSize.add(new CacheStatistic("Folders", lw.getGroupManager().getFolderCacheSize()));
        // cacheSize.add(new CacheStatistic("Courses", lw.getCourseManager().getCacheSize()));
        // cacheSize.add(new CacheStatistic("Organisations", lw.getOrganisationManager().getCacheSize()));
    }

    public void onResetCaches() throws SQLException {
        getLearnweb().resetCaches();
    }

    private void loadDatabaseProcessList() throws SQLException {
        databaseProcessList = new LinkedList<>();
        ResultSet rs = getLearnweb().getConnection().createStatement().executeQuery("SHOW FULL PROCESSLIST");

        while (rs.next()) {
            DatabaseProcessStatistic ps = new DatabaseProcessStatistic();
            ps.setId(rs.getInt("Id"));
            ps.setUser(rs.getString("User"));
            ps.setHost(rs.getString("Host"));
            ps.setDb(rs.getString("db"));
            ps.setCommand(rs.getString("Command"));
            ps.setTime(rs.getString("Time"));
            ps.setState(rs.getString("State"));
            ps.setInfo(rs.getString("Info"));
            ps.setProgress(rs.getString("Progress"));

            databaseProcessList.add(ps);
        }
    }

    public String getMemoryInfo() {
        return memoryInfo;
    }

    public LinkedList<Object> getDatabaseProcessList() {
        return databaseProcessList;
    }

    public void onKillDatabaseProcess(int processId) throws SQLException {
        getLearnweb().getConnection().createStatement().executeUpdate("KILL " + processId);

        addMessage(FacesMessage.SEVERITY_INFO, "Killed process " + processId);

        loadDatabaseProcessList(); // update list
    }

    public void onClearCaches() {
        try {
            Learnweb learnweb = getLearnweb();
            learnweb.getOrganisationManager().resetCache();
            learnweb.getCourseManager().resetCache();
            learnweb.getResourceManager().resetCache();
            learnweb.getGroupManager().resetCache();
            learnweb.getUserManager().resetCache();
            learnweb.getFileManager().resetCache();
        } catch (SQLException e) {
            addErrorMessage(e);
        }

        addMessage(FacesMessage.SEVERITY_INFO, "Caches cleared");
    }

    public static class DatabaseProcessStatistic {
        private int id;
        private String user;
        private String host;
        private String db;
        private String command;
        private String time;
        private String state;
        private String info;
        private String progress;

        @Override
        public String toString() {
            return "ProcessStatistic [id=" + id + ", user=" + user + ", host=" + host + ", db=" + db + ", command=" + command
                + ", time=" + time + ", state=" + state + ", info=" + info + ", progress=" + progress + "]";
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getDb() {
            return db;
        }

        public void setDb(String db) {
            this.db = db;
        }

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getInfo() {
            return info;
        }

        public void setInfo(String info) {
            this.info = info;
        }

        public String getProgress() {
            return progress;
        }

        public void setProgress(String progress) {
            this.progress = progress;
        }
    }

    public static class CacheStatistic {
        private final String cache;
        private final int objects;

        public CacheStatistic(String cache, int objects) {
            this.cache = cache;
            this.objects = objects;
        }

        public String getCache() {
            return cache;
        }

        public int getObjects() {
            return objects;
        }
    }
}
