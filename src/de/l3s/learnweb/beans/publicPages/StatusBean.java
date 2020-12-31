package de.l3s.learnweb.beans.publicPages;

import java.net.HttpURLConnection;
import java.util.LinkedList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

import org.jdbi.v3.core.Handle;
import org.omnifaces.util.Faces;

import de.l3s.learnweb.beans.ApplicationBean;

@Named
@RequestScoped
public class StatusBean extends ApplicationBean {
    private final List<Service> services = new LinkedList<>();

    @PostConstruct
    public void init() {
        // obviously OK, otherwise this page would not be reachable
        services.add(new Service("Tomcat Server", true, null));

        // test learnweb database
        services.add(getDatabaseConnection());

        // very simple database integrity test
        services.add(getDatabaseIntegrity());

        // if any not healthy, set status code to 503
        if (services.stream().anyMatch(service -> !service.healthy)) {
            Faces.setResponseStatus(HttpURLConnection.HTTP_UNAVAILABLE);
        }
    }

    private Service getDatabaseConnection() {
        try (Handle handle = getLearnweb().openJdbiHandle()) {
            handle.select("SELECT 1").mapTo(Integer.class).one();
            return new Service("Database Connection", true, null);
        } catch (Exception e) {
            return new Service("Database Connection", false, e.getMessage());
        }
    }

    private Service getDatabaseIntegrity() {
        try {
            getLearnweb().getDaoProvider().getOrganisationDao().findDefault(); // will fail if no default
            return new Service("Database Integrity", true, null);
        } catch (Exception e) {
            return new Service("Database Integrity", false, e.getMessage());
        }
    }

    public List<Service> getServices() {
        return services;
    }

    public static class Service {
        private final String name;
        private final boolean healthy;
        private final String description;

        public Service(String name, boolean healthy, String description) {
            this.name = name;
            this.healthy = healthy;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public boolean isHealthy() {
            return healthy;
        }

        public String getDescription() {
            return description;
        }
    }
}
