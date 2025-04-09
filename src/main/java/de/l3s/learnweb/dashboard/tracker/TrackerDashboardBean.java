package de.l3s.learnweb.dashboard.tracker;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.l3s.learnweb.app.ConfigProvider;
import de.l3s.learnweb.dashboard.CommonDashboardUserBean;

@Named
@ViewScoped
public class TrackerDashboardBean extends CommonDashboardUserBean implements Serializable {
    private static final Logger log = LogManager.getLogger(TrackerDashboardBean.class);

    @Serial
    private static final long serialVersionUID = 3640317272542005280L;

    private transient Map<String, Integer> proxySources;
    private transient List<TrackerUserActivity> statistics;

    @Inject
    private ConfigProvider config;

    @Inject
    private ObjectMapper objectMapper;

    @Override
    public void onLoad() {
        super.onLoad();

        if (StringUtils.isAnyEmpty(config.getProperty("integration_tracker_url"), config.getProperty("integration_tracker_apikey"), config.getProperty("integration_tracker_secret"))) {
            throw new DeploymentException("Proxy is not configured. Please contact the administrator.");
        }
    }

    @Override
    public void cleanAndUpdateStoredData() {
        statistics = null;
        proxySources = null;
    }

    public List<TrackerUserActivity> getStatistics() {
        if (statistics == null) {
            try {
                String response = sendApiRequest("/api/v2/statistics/users", getSelectedUsersIds(), startDate, endDate);
                this.statistics = objectMapper.readValue(response, new TypeReference<>() {});
            } catch (IOException | InterruptedException e) {
                log.error("Error fetching proxy sources: ", e);
            }
        }
        return statistics;
    }

    public Map<String, Integer> getProxySources() {
        if (proxySources == null) {
            try {
                String response = sendApiRequest("/api/v2/statistics/domains", getSelectedUsersIds(), startDate, endDate);
                proxySources = objectMapper.readValue(response, new TypeReference<>() {});
            } catch (IOException | InterruptedException e) {
                log.error("Error fetching proxy sources: ", e);
            }
        }
        return proxySources;
    }

    private String sendApiRequest(String endpoint, List<Integer> userIds, LocalDate startDate, LocalDate endDate)
        throws IOException, InterruptedException {
        String queryParams = buildQueryParameters(userIds, startDate, endDate);
        String url = config.getProperty("integration_tracker_url") + endpoint + queryParams;

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Apikey " + config.getProperty("integration_tracker_apikey"))
            .header("X-Secret-Key", config.getProperty("integration_tracker_secret"))
            .GET().build();

        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            log.error("API request failed with status code {}: {}", response.statusCode(), response.body());
            throw new IOException("API request failed with status code: " + response.statusCode());
        }

        return response.body();
    }

    private String buildQueryParameters(List<Integer> userIds, LocalDate startDate, LocalDate endDate) {
        StringBuilder params = new StringBuilder("?");
        boolean hasParams = false;

        if (userIds != null && !userIds.isEmpty()) {
            params.append("user_ids=").append(String.join(",", userIds.stream().map(String::valueOf).toList()));
            hasParams = true;
        }

        if (startDate != null) {
            if (hasParams) params.append("&");
            params.append("start_date=").append(startDate);
            hasParams = true;
        }

        if (endDate != null) {
            if (hasParams) params.append("&");
            params.append("end_date=").append(endDate);
        }

        return params.toString();
    }
}
