package de.l3s.learnweb.beans.admin;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.group.GroupDao;

/**
 * Used to extract activities from http://hypothes.is.
 *
 * @author Kate
 */
@Named
@RequestScoped
public class AdminGroupDiscussionActivityBean extends ApplicationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = 6519388228766929819L;
    private static final Logger log = LogManager.getLogger(AdminGroupDiscussionActivityBean.class);

    private static final String REQUEST_URI = "https://hypothes.is/api/search?limit=200&group=";
    // hard coded to use token of hypothesis account kemkes@l3s.de; this account must join the hypothesis group
    private static final String REQUEST_AUTH_TOKEN = "***REMOVED***";

    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d+\\D\\d+\\D\\d+).(\\d+\\D\\d+\\D\\d+)");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("acct:(.+)@");
    private static final Pattern GROUP_ID_PATTERN = Pattern.compile("hypothes.is/groups/(\\w*)");

    private int groupId;
    private List<AnnotationEntity> groupAnnotations;

    @Inject
    private GroupDao groupDao;

    public void onLoad() {
        try {
            Group group = groupDao.findByIdOrElseThrow(groupId);
            String hypothesisLink = group.getHypothesisLink();
            Matcher matcher = GROUP_ID_PATTERN.matcher(hypothesisLink);
            String hypothesisGroup = matcher.group(1);
            BeanAssert.validate(hypothesisGroup != null, "The requested group has incorrect hypothes.is link");

            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(REQUEST_URI + hypothesisGroup))
                .header("Authorization", "Bearer " + REQUEST_AUTH_TOKEN)
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Processing
            JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray rows = jsonResponse.getAsJsonArray("rows");

            groupAnnotations = new ArrayList<>();
            for (JsonElement row : rows) {
                groupAnnotations.add(processJson(row.getAsJsonObject()));
            }
        } catch (IOException | JsonParseException | InterruptedException e) {
            addErrorMessage(e);
        }
    }

    private AnnotationEntity processJson(JsonObject row) {
        log.debug(row.toString());

        String name = row.get("user").getAsString();
        Matcher matcher = USERNAME_PATTERN.matcher(name);
        if (matcher.find()) {
            name = matcher.group(1);
        }

        String url = row.get("uri").getAsString();
        String text = StringUtils.abbreviate(row.get("text").getAsString(), 100);

        String timeJSON = row.get("created").getAsString();
        String time;

        matcher = DATE_PATTERN.matcher(timeJSON);
        if (matcher.find()) {
            time = matcher.group(1) + " " + matcher.group(2);
        } else {
            time = timeJSON;
        }

        JsonArray target = row.getAsJsonArray("target");
        String snippet;

        if (target.size() > 1) {
            snippet = target.get(1).getAsJsonArray().get(3).getAsJsonObject().get("exact").getAsString();
            snippet = StringUtils.abbreviate(snippet, 100);
        } else {
            snippet = "";
        }

        return new AnnotationEntity(name, text, snippet, url, time);
    }

    public List<AnnotationEntity> getGroupAnnotations() {
        return groupAnnotations;
    }

    public void setGroupAnnotations(List<AnnotationEntity> groupAnnotations) {
        this.groupAnnotations = groupAnnotations;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public static class AnnotationEntity implements Serializable {
        @Serial
        private static final long serialVersionUID = -5780824434073985590L;

        String name;
        String annotation;
        String snippet;
        String url;
        String time;

        public AnnotationEntity(String name, String annotation, String snippet, String url, String time) {
            this.name = name;
            this.annotation = annotation;
            this.snippet = snippet;
            this.url = url;
            this.time = time;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAnnotation() {
            return annotation;
        }

        public void setAnnotation(String annotation) {
            this.annotation = annotation;
        }

        public String getSnippet() {
            return snippet;
        }

        public void setSnippet(String snippet) {
            this.snippet = snippet;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }
    }
}
