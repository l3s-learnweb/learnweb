package de.l3s.learnweb.beans.publicPages;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

import org.jdbi.v3.core.Handle;
import org.omnifaces.util.Faces;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.ApplicationBean;

@Named
@RequestScoped
public class StatusBean extends ApplicationBean {
    private Map<String, String> variables = new HashMap<>();
    private final List<Service> services = new LinkedList<>();

    public StatusBean() {
        fetchVariables();
        fetchServices();
    }

    private void fetchVariables() {
        variables = System.getenv().entrySet()
            .stream()
            .filter(map -> map.getKey().startsWith("LEARNWEB_"))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private void fetchServices() {
        Learnweb learnweb = getLearnweb();
        services.add(new Service("Learnweb Tomcat", "ok", learnweb.getServerUrl(), "Obviously OK, otherwise this page would not be reachable"));

        // test learnweb database
        Service lwDb = new Service("Learnweb Database", "ok", learnweb.getProperties().getProperty("mysql_url"), "");
        try (Handle handle = learnweb.openHandle()) {
            Integer dbUsers = handle.select("SELECT count(*) FROM lw_user").mapTo(Integer.class).one();

            if (dbUsers == null || dbUsers < 400) {
                lwDb.setStatus("error", "unexpected result from database");
            }
        } catch (Exception e) {
            lwDb.setStatus("error", e.getMessage());
        }
        services.add(lwDb);

        // very simple database integrity test
        Service lwDbIntegrity = new Service("Learnweb Database integrity", "ok", learnweb.getProperties().getProperty("FEDORA_SERVER_URL"), "");
        try {
            if (learnweb.getResourceManager().getResourceRateByUser(2811, 1684) == null) {
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

    public Map<String, String> getVariables() {
        return variables;
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
