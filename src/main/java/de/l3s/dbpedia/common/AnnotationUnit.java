package de.l3s.dbpedia.common;

import static de.l3s.dbpedia.common.Constants.*;
import static de.l3s.dbpedia.common.Prefixes.DBPEDIA_ONTOLOGY;
import static de.l3s.dbpedia.common.Prefixes.SCHEMA_ONTOLOGY;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class AnnotationUnit {
    @SerializedName("@text")
    private String text;

    @SerializedName("@confidence")
    private String confidence;

    @SerializedName("@support")
    private String support;

    @SerializedName("@types")
    private String types;

    @SerializedName("@sparql")
    private String sparql;

    @SerializedName("@policy")
    private String policy;

    @SerializedName("Resources")
    private List<ResourceItem> resources;

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(final String confidence) {
        this.confidence = confidence;
    }

    public String getSupport() {
        return support;
    }

    public void setSupport(final String support) {
        this.support = support;
    }

    public void setTypes(final String types) {
        this.types = types;
    }

    public String getSparql() {
        return sparql;
    }

    public void setSparql(final String sparql) {
        this.sparql = sparql;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(final String policy) {
        this.policy = policy;
    }

    public List<ResourceItem> getResources() {
        return resources;
    }

    public void setResources(final List<ResourceItem> resources) {
        this.resources = resources;
    }

    public Integer endIndex() {
        if (text != null) {
            return text.length();
        }
        return 0;
    }

    public String getTypes() {
        if (types != null && !types.isEmpty()) {
            return types.replace("Http", HTTP)
                .replace(DBPEDIA, DBPEDIA_ONTOLOGY)
                .replace(SCHEMA, SCHEMA_ONTOLOGY);
        }
        return types;
    }

    public Integer beginIndex() {
        return 1;
    }
}
