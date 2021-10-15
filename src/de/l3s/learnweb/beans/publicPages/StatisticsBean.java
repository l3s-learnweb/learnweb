package de.l3s.learnweb.beans.publicPages;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.Handle;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.util.SqlHelper;

@Named
@RequestScoped
public class StatisticsBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 8540469716342151138L;
    private static final Logger log = LogManager.getLogger(StatisticsBean.class);

    private List<SimpleEntry<LocalDateTime, Integer>> activeUsersPerMonth;
    private List<SimpleEntry<String, Integer>> resourcesPerSource;
    private final Map<String, Number> generalStatistics = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        try (Handle handle = getLearnweb().openJdbiHandle()) {
            Long users = handle.select("SELECT count(*) FROM lw_user WHERE deleted = 0").mapTo(Long.class).one();
            Long groups = handle.select("SELECT count(*) FROM lw_group WHERE deleted = 0").mapTo(Long.class).one();
            Long resources = handle.select("SELECT count(*) FROM lw_resource WHERE deleted = 0").mapTo(Long.class).one();
            Long courses = handle.select("SELECT count(*) FROM lw_course").mapTo(Long.class).one();

            Long ratedResourcesCount = handle.select("SELECT (SELECT count(DISTINCT resource_id) FROM lw_resource_rating) + (SELECT count(DISTINCT resource_id) FROM lw_resource_thumb)").mapTo(Long.class).one();
            Long taggedResourcesCount = handle.select("SELECT count(DISTINCT resource_id) FROM lw_resource_tag").mapTo(Long.class).one();
            Long commentedResourcesCount = handle.select("SELECT count(DISTINCT resource_id) FROM lw_comment").mapTo(Long.class).one();

            Long rateCount = handle.select("SELECT (SELECT count(*) FROM lw_resource_rating) + (SELECT count(*) FROM lw_resource_thumb)").mapTo(Long.class).one();
            Long tagCount = handle.select("SELECT count(*) FROM lw_resource_tag").mapTo(Long.class).one();
            Long commentCount = handle.select("SELECT count(*) FROM lw_comment").mapTo(Long.class).one();

            Double ratedResourcesAverage = (double) rateCount / (double) ratedResourcesCount;
            Double taggedResourcesAverage = (double) tagCount / (double) taggedResourcesCount;
            Double commentedResourcesAverage = (double) commentCount / (double) commentedResourcesCount;

            generalStatistics.put("users", users);
            generalStatistics.put("groupsTitle", groups);
            generalStatistics.put("courses", courses);
            generalStatistics.put("resources", resources);
            generalStatistics.put("number_of_rated_resources", ratedResourcesCount);
            generalStatistics.put("number_of_tagged_resources", taggedResourcesCount);
            generalStatistics.put("number_of_commented_resources", commentedResourcesCount);
            generalStatistics.put("number_of_rates", rateCount);
            generalStatistics.put("number_of_tags", tagCount);
            generalStatistics.put("number_of_comments", commentCount);
            generalStatistics.put("average_number_of_rates_per_rated_resource", ratedResourcesAverage);
            generalStatistics.put("average_number_of_tags_per_tagged_resource", taggedResourcesAverage);
            generalStatistics.put("average_number_of_comments_per_commented_resource", commentedResourcesAverage);

            activeUsersPerMonth = handle.select("SELECT created_at, count(distinct user_id) as count FROM lw_user_log "
                + "WHERE action = 9 and created_at > DATE_SUB(NOW(), INTERVAL 390 day) GROUP BY year(created_at) ,month(created_at) "
                + "ORDER BY year(created_at) DESC,month(created_at) DESC LIMIT 13")
                .map((rs, ctx) -> new SimpleEntry<>(SqlHelper.getLocalDateTime(rs.getTimestamp(1)), rs.getInt(2)))
                .list();

            resourcesPerSource = handle.select("SELECT service, count(*) FROM lw_resource WHERE deleted = 0 GROUP BY service ORDER BY count( * ) DESC")
                .map((rs, ctx) -> {
                    String serviceName = rs.getString(1);
                    ResourceService service = ResourceService.valueOf(serviceName);

                    serviceName = service.getLabel();
                    if (service.isCrawled()) {
                        serviceName += " *";
                    }

                    return new AbstractMap.SimpleEntry<>(serviceName, rs.getInt(2));
                }).list();
        }
    }

    public List<SimpleEntry<LocalDateTime, Integer>> getActiveUsersPerMonth() {
        return activeUsersPerMonth;
    }

    public List<SimpleEntry<String, Integer>> getResourcesPerSource() {
        return resourcesPerSource;
    }

    public Map<String, Number> getGeneralStatistics() {
        return generalStatistics;
    }
}
