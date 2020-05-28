package de.l3s.learnweb.beans.admin;

import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.group.Group;

/**
 * Used to extract activities from http://hypothes.is.
 *
 * @author Kate
 */
@Named
@RequestScoped
public class AdminGroupDiscussionActivityBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = 6519388228766929819L;
    private static final Logger log = LogManager.getLogger(AdminGroupDiscussionActivityBean.class);

    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d+\\D\\d+\\D\\d+).(\\d+\\D\\d+\\D\\d+)");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("acct:(.+)@");
    private static final Pattern GROUP_ID_PATTERN = Pattern.compile("hypothes.is/groups/(\\w*)");

    private int groupID;
    private List<AnnotationEntity> groupAnnotations;

    public AdminGroupDiscussionActivityBean() {
        // TODO: perhaps @PostConstructor or viewAction should be used instead
        load();
    }

    private void load() {
        setGroupAnnotations(new ArrayList<>());
        FacesContext context = FacesContext.getCurrentInstance();
        Map<String, String> paramMap = context.getExternalContext().getRequestParameterMap();

        setGroupID(Integer.parseInt(paramMap.get("groupID")));

        try {
            Group group = getLearnweb().getGroupManager().getGroupById(groupID);

            if (group == null) {
                return;
            }

            String hypothesisLink = group.getHypothesisLink();
            String hypothesisGroupID;

            Matcher matcher = GROUP_ID_PATTERN.matcher(hypothesisLink);
            if (matcher.find()) {
                hypothesisGroupID = matcher.group(1);
            } else {
                return;
            }

            String token = "***REMOVED***"; // hard coded to use token of hypothesis account kemkes@l3s.de; this account must join the hypothesis group

            URIBuilder builder = new URIBuilder("https://hypothes.is/api/search");
            builder.setParameter("limit", "200").setParameter("group", hypothesisGroupID);

            HttpClient httpclient = HttpClientBuilder.create().build();
            HttpGet httpget = new HttpGet(builder.build());

            httpget.addHeader("Authorization", "Bearer " + token);

            HttpResponse response = httpclient.execute(httpget);

            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity, "UTF-8");

            // Processing
            JsonObject jsonResponse = JsonParser.parseString(responseString).getAsJsonObject();
            JsonArray rows = jsonResponse.getAsJsonArray("rows");

            for (JsonElement row : rows) {
                groupAnnotations.add(processJson(row.getAsJsonObject()));
            }
        } catch (IOException | URISyntaxException | JsonParseException | SQLException e) {
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

    public int getGroupID() {
        return groupID;
    }

    public void setGroupID(int groupID) {
        this.groupID = groupID;
    }

    public static class AnnotationEntity implements Serializable {
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
