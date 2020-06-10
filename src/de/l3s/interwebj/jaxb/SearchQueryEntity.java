package de.l3s.interwebj.jaxb;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "query")
@XmlAccessorType(XmlAccessType.FIELD)
public class SearchQueryEntity
{

    @XmlAttribute(name = "id")
    private String id;
    @XmlAttribute(name = "link")
    private String link;

    @XmlElement(name = "q")
    private String query;
    @XmlElement(name = "date_from")
    private String dateFrom;
    @XmlElement(name = "date_till")
    private String dateTill;
    @XmlElement(name = "language")
    private String language;

    @XmlElementWrapper(name = "services")
    @XmlElement(name = "service")
    private Set<String> connectorNames;
    @XmlElementWrapper(name = "media_types")
    @XmlElement(name = "type")
    private Set<String> contentTypes;
    @XmlElementWrapper(name = "extras")
    @XmlElement(name = "extra")
    private Set<String> extras;

    @XmlElement(name = "page")
    private Integer page;
    @XmlElement(name = "per_page")
    private Integer perPage;
    @XmlElement(name = "search_in")
    private String searchScope;
    @XmlElement(name = "ranking")
    private String ranking;

    @XmlElement(name = "timeout")
    private Integer timeout;

    public SearchQueryEntity()
    {
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getLink()
    {
        return link;
    }

    public void setLink(String link)
    {
        this.link = link;
    }

    public String getQuery()
    {
        return query;
    }

    public void setQuery(String query)
    {
        this.query = query;
    }

    public String getDateFrom()
    {
        return dateFrom;
    }

    public void setDateFrom(String dateFrom)
    {
        this.dateFrom = dateFrom;
    }

    public String getDateTill()
    {
        return dateTill;
    }

    public void setDateTill(String dateTill)
    {
        this.dateTill = dateTill;
    }

    public String getLanguage()
    {
        return language;
    }

    public void setLanguage(String language)
    {
        this.language = language;
    }

    public Set<String> getConnectorNames()
    {
        return connectorNames;
    }

    public void setConnectorNames(Set<String> connectorNames)
    {
        this.connectorNames = connectorNames;
    }

    public Set<String> getContentTypes()
    {
        return contentTypes;
    }

    public void setContentTypes(Set<String> contentTypes)
    {
        this.contentTypes = contentTypes;
    }

    public Set<String> getExtras()
    {
        return extras;
    }

    public void setExtras(Set<String> extras)
    {
        this.extras = extras;
    }

    public Integer getPage()
    {
        return page;
    }

    public void setPage(Integer page)
    {
        this.page = page;
    }

    public Integer getPerPage()
    {
        return perPage;
    }

    public void setPerPage(Integer perPage)
    {
        this.perPage = perPage;
    }

    public String getSearchScope()
    {
        return searchScope;
    }

    public void setSearchScope(String searchScope)
    {
        this.searchScope = searchScope;
    }

    public String getRanking()
    {
        return ranking;
    }

    public void setRanking(String ranking)
    {
        this.ranking = ranking;
    }

    public Integer getTimeout()
    {
        return timeout;
    }

    public void setTimeout(Integer timeout)
    {
        this.timeout = timeout;
    }
}
