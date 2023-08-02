package de.l3s.learnweb.searchhistory.dbpediaspotlight.rest;

import static org.apache.http.HttpHeaders.ACCEPT;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import com.google.gson.Gson;

import de.l3s.learnweb.searchhistory.dbpediaspotlight.common.AnnotationUnit;
import de.l3s.learnweb.searchhistory.dbpediaspotlight.common.Constants;
import de.l3s.learnweb.searchhistory.dbpediaspotlight.common.Prefixes;
import de.l3s.learnweb.searchhistory.dbpediaspotlight.common.ResourceItem;

public class SpotlightClient {
    private static final String URL = "https://api.dbpedia-spotlight.org/en/annotate";
    private final HttpClient client;
    private final HttpPost request;

    public SpotlightClient() {
        client = HttpClientBuilder.create().build();
        request = new HttpPost(URL);
        request.addHeader(ACCEPT, "application/json");
    }

    private AnnotationUnit get() throws IOException {
        Gson gson = new Gson();
        AnnotationUnit annotationUnit = gson.fromJson(getContent(), AnnotationUnit.class);
        fixPrefixes(annotationUnit.getResources());

        return annotationUnit;
    }

    public AnnotationUnit get(String text) throws IOException {
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("text", text));
        request.setEntity(new UrlEncodedFormEntity(params));
        return get();
    }

    public AnnotationUnit get(URL url) throws IOException {
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("url", url.toString()));
        request.setEntity(new UrlEncodedFormEntity(params));
        return get();
    }

    private String fixPrefixes(String value) {
        if (value != null && !value.isEmpty()) {
            return value.replace(Constants.HTTPBIG, Constants.HTTP)
                .replace(Constants.DBPEDIA, Prefixes.DBPEDIA_ONTOLOGY)
                .replace(Constants.SCHEMA, Prefixes.SCHEMA_ONTOLOGY);
        }
        return value;
    }

    private void fixPrefixes(ResourceItem resource) {
        resource.setTypes(fixPrefixes(resource.getTypes()));
    }

    private void fixPrefixes(List<ResourceItem> resources) {
        if (resources != null && !resources.isEmpty()) {
            resources.forEach(this::fixPrefixes);
        }
    }

    private String getContent() throws IOException {
        HttpResponse response = client.execute(request);
        StringBuilder result = new StringBuilder();
        try (BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8))) {
            String line;

            while ((line = rd.readLine()) != null) {
                result.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return result.toString();
    }

}
