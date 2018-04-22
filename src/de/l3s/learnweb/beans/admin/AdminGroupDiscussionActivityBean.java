package de.l3s.learnweb.beans.admin;

import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.l3s.learnweb.Group;
import de.l3s.learnweb.beans.ApplicationBean;

@ManagedBean
@RequestScoped
public class AdminGroupDiscussionActivityBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 6519388228766929819L;
    private static final Pattern datePattern = Pattern.compile("(\\d+\\D\\d+\\D\\d+).(\\d+\\D\\d+\\D\\d+)");
    private static final Pattern usernamePattern = Pattern.compile("acct:(.+)@");
    private static final Pattern groupIDPattern = Pattern.compile("hypothes.is\\/groups\\/(\\w*)");

    private int groupID;

    private List<AnnotationEntity> groupAnnotations;

    public AdminGroupDiscussionActivityBean()
    {
        load();
    }

    private void load()
    {
        setGroupAnnotations(new ArrayList<>());
        FacesContext context = FacesContext.getCurrentInstance();
        Map<String, String> paramMap = context.getExternalContext().getRequestParameterMap();

        setGroupID(Integer.parseInt(paramMap.get("groupID")));

        try
        {
            Group group = getLearnweb().getGroupManager().getGroupById(groupID);

            if(group == null)
            {
                return;
            }

            String hypothesisLink = group.getHypothesisLink();
            String hypothesisGroupID;

            Matcher matcher = groupIDPattern.matcher(hypothesisLink);
            if(matcher.find())
            {
                hypothesisGroupID = matcher.group(1);
            }
            else
            {
                return;
            }

            String token = group.getHypothesisToken();

            URIBuilder builder = new URIBuilder("https://hypothes.is/api/search");
            builder.setParameter("limit", "200").setParameter("group", hypothesisGroupID);

            HttpClient httpclient = HttpClientBuilder.create().build();
            HttpGet httpget = new HttpGet(builder.build());

            httpget.addHeader("Authorization", "Bearer " + token);

            HttpResponse response = httpclient.execute(httpget);

            HttpEntity entity = response.getEntity();
            String responseString = EntityUtils.toString(entity, "UTF-8");

            //Processing
            JSONObject jsonResponse = new JSONObject(responseString);
            JSONArray rows = jsonResponse.getJSONArray("rows");

            for(int i = 0; i < rows.length(); i++)
            {
                groupAnnotations.add(processJson(rows.getJSONObject(i)));
            }
        }
        catch(IOException | URISyntaxException | JSONException | ParseException | SQLException e)
        {
            addFatalMessage(e);
        }

    }

    private AnnotationEntity processJson(JSONObject row) throws JSONException, ParseException
    {
        String name = row.getString("user");
        Matcher matcher = usernamePattern.matcher(name);
        if(matcher.find())
        {
            name = matcher.group(1);
        }

        String url = row.getString("uri");
        String text = StringUtils.abbreviate(row.getString("text"), 100);

        String timeJSON = row.getString("created");
        String time;

        matcher = datePattern.matcher(timeJSON);
        if(matcher.find())
        {
            time = matcher.group(1) + " " + matcher.group(2);
        }
        else
        {
            time = timeJSON;
        }

        JSONArray target = row.getJSONArray("target");
        String snippet;
        System.out.print("Target length: " + target.length());

        if(target.length() > 1)
        {
            System.out.print("Sublist length: " + target.getJSONArray(1).length());
            snippet = target.getJSONArray(1).getJSONObject(3).getString("exact");
            snippet = StringUtils.abbreviate(snippet, 100);
        }
        else
        {
            snippet = "";
        }

        return new AnnotationEntity(name, text, snippet, url, time);
    }

    public List<AnnotationEntity> getGroupAnnotations()
    {
        return groupAnnotations;
    }

    public void setGroupAnnotations(List<AnnotationEntity> groupAnnotations)
    {
        this.groupAnnotations = groupAnnotations;
    }

    public int getGroupID()
    {
        return groupID;
    }

    public void setGroupID(int groupID)
    {
        this.groupID = groupID;
    }

    public class AnnotationEntity
    {
        String name;
        String annotation;
        String snippet;
        String url;
        String time;

        public AnnotationEntity(String name, String annotation, String snippet, String url, String time)
        {
            super();
            this.name = name;
            this.annotation = annotation;
            this.snippet = snippet;
            this.url = url;
            this.time = time;
        }

        public String getName()
        {
            return name;
        }

        public String getAnnotation()
        {
            return annotation;
        }

        public String getSnippet()
        {
            return snippet;
        }

        public String getUrl()
        {
            return url;
        }

        public String getTime()
        {
            return time;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public void setAnnotation(String annotation)
        {
            this.annotation = annotation;
        }

        public void setSnippet(String snippet)
        {
            this.snippet = snippet;
        }

        public void setUrl(String url)
        {
            this.url = url;
        }

        public void setTime(String time)
        {
            this.time = time;
        }
    }
}
