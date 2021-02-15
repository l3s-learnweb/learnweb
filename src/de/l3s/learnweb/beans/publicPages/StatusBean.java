package de.l3s.learnweb.beans.publicPages;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

import org.jdbi.v3.core.Handle;
import org.omnifaces.util.Faces;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.beans.ApplicationBean;

@Named
@RequestScoped
public class StatusBean extends ApplicationBean {
    private final List<Service> services = new LinkedList<>();

    @PostConstruct
    public void init() {
        Learnweb learnweb = getLearnweb();
        services.add(new Service("Learnweb Tomcat", "ok", learnweb.getConfigProvider().getServerUrl(), "Obviously OK, otherwise this page would not be reachable"));

        // test learnweb database
        Service lwDb = new Service("Learnweb Database", "ok", learnweb.getConfigProvider().getProperty("mysql_url"), "");
        try (Handle handle = learnweb.openJdbiHandle()) {
            Integer dbUsers = handle.select("SELECT count(*) FROM lw_user").mapTo(Integer.class).one();

            if (dbUsers == null || dbUsers < 400) {
                lwDb.setStatus("error", "unexpected result from database");
            }
        } catch (Exception e) {
            lwDb.setStatus("error", e.getMessage());
        }
        services.add(lwDb);

        // very simple database integrity test
        Service lwDbIntegrity = new Service("Learnweb Database integrity", "ok", learnweb.getConfigProvider().getProperty("mysql_url"), "");
        try {
            if (learnweb.getDaoProvider().getResourceDao().findResourceRating(2811, 1684).isPresent()) {
                lwDbIntegrity.setStatus("warning", "unexpected result from database");
            }
        } catch (Exception e) {
            lwDbIntegrity.setStatus("error", e.getMessage());
        }
        services.add(lwDbIntegrity);
    }

    public String getVersion() {
        final String displayName = Faces.getServletContext().getServletContextName();

        int i = displayName.indexOf('-');
        if (i != -1) {
            return displayName.substring(i + 1).trim();
        }

        return displayName;
    }

    public String getProjectStage() {
        return Faces.getProjectStage().toString();
    }

    public List<Service> getServices() {
        return services;
    }

    public static class Service {
        private final String name;
        private String status;
        private final String url;
        private String comment;

        public Service(String name, String status, String url, String comment) {
            this.name = name;
            this.status = status;
            this.url = url;
            this.comment = comment;
        }

        public void setStatus(String status, String comment) {
            this.status = status;
            this.comment = comment;
        }

        public String getName() {
            return name;
        }

        public String getStatus() {
            return status;
        }

        public String getComment() {
            return comment;
        }

        public String getUrl() {
            return url;
        }
    }
}
