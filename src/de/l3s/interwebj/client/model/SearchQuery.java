package de.l3s.interwebj.client.model;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.google.gson.annotations.SerializedName;

public class SearchQuery implements Serializable {
    private static final long serialVersionUID = 1982483611243356485L;

    @SerializedName("id")
    private String id;
    @SerializedName("link")
    private String link;

    @SerializedName("q")
    private String query;
    @SerializedName("date_from")
    private String dateFrom;
    @SerializedName("date_till")
    private String dateTill;
    @SerializedName("language")
    private String language;

    @SerializedName("services")
    private List<String> services;
    @SerializedName("media_types")
    private List<String> contentTypes;

    @SerializedName("page")
    private Integer page;
    @SerializedName("per_page")
    private Integer perPage;
    @SerializedName("search_in")
    private String searchScopes;
    @SerializedName("ranking")
    private String ranking;

    @SerializedName("timeout")
    private Integer timeout;

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getLink() {
        return link;
    }

    public void setLink(final String link) {
        this.link = link;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(final String query) {
        this.query = query;
    }

    public String getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(final String dateFrom) {
        this.dateFrom = dateFrom;
    }

    public String getDateTill() {
        return dateTill;
    }

    public void setDateTill(final String dateTill) {
        this.dateTill = dateTill;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(final String language) {
        this.language = language;
    }

    public List<String> getServices() {
        return services;
    }

    public void setServices(final List<String> services) {
        this.services = services;
    }

    public List<String> getContentTypes() {
        return contentTypes;
    }

    public void setContentTypes(final List<String> contentTypes) {
        this.contentTypes = contentTypes;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(final Integer page) {
        this.page = page;
    }

    public Integer getPerPage() {
        return perPage;
    }

    public void setPerPage(final Integer perPage) {
        this.perPage = perPage;
    }

    public String getSearchScopes() {
        return searchScopes;
    }

    public void setSearchScopes(final String searchScopes) {
        this.searchScopes = searchScopes;
    }

    public String getRanking() {
        return ranking;
    }

    public void setRanking(final String ranking) {
        this.ranking = ranking;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(final Integer timeout) {
        this.timeout = timeout;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
            .append("id", id)
            .append("link", link)
            .append("query", query)
            .append("dateFrom", dateFrom)
            .append("dateTill", dateTill)
            .append("language", language)
            .append("services", services)
            .append("contentTypes", contentTypes)
            .append("page", page)
            .append("perPage", perPage)
            .append("searchScopes", searchScopes)
            .append("ranking", ranking)
            .append("timeout", timeout)
            .toString();
    }
}
