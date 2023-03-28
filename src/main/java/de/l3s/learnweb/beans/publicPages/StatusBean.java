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

        // test solr connection
        services.add(getSolrConnection());

        // very simple solr integrity test
        services.add(getSolrIntegrity());

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
            if (!getLearnweb().getDaoProvider().getOrganisationDao().findAll().isEmpty()) {
                return new Service("Database Integrity", true, null);
            } else {
                return new Service("Database Integrity", false, "No organisations found");
            }
        } catch (Exception e) {
            return new Service("Database Integrity", false, e.getMessage());
        }
    }

    private Service getSolrConnection() {
        try {
            if (getLearnweb().getSolrClient().getHttpSolrClient().ping().getStatus() == 0) {
                return new Service("Solr Connection", true, null);
            } else {
                return new Service("Solr Connection", false, "Unexpected status");
            }
        } catch (Exception e) {
            return new Service("Solr Connection", false, e.getMessage());
        }
    }

    private Service getSolrIntegrity() {
        try {
            if (getLearnweb().getSolrClient().countResources("*:*") >= 0) {
                return new Service("Solr Integrity", true, null);
            } else {
                return new Service("Solr Integrity", false, "Negative amount of resources o_O");
            }
        } catch (Exception e) {
            return new Service("Solr Integrity", false, e.getMessage());
        }
    }

    public List<Service> getServices() {
        return services;
    }

    public record Service(String name, boolean healthy, String description) {}
}
