package de.l3s.learnweb.beans;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.solrClient.SolrSearch;

@ManagedBean(name = "advancedSearchBean")
@SessionScoped
public class AdvancedSearchBean
{
    final static Logger log = Logger.getLogger(SolrSearch.class);
    private String query = "";
    private String extractWord = "";
    private String anyWords = "";
    private String noneWords = "";
    private String language;
    private Map<String, String> languages;
    private Map<String, Map<String, String>> data = new HashMap<String, Map<String, String>>();
    private String country;
    private String city;
    private Map<String, String> countries;
    private Map<String, String> cities;
    private String facetFields[] = null;
    private String facetQueries[] = null;
    private QueryResponse result;

    private SolrQuery solrQuery;

    @PostConstruct
    public void init()
    {
        languages = new HashMap<String, String>();
        languages.put("German", "German");
        languages.put("English", "English");
        languages.put("Italian", "Italian");

        countries = new HashMap<String, String>();
        countries.put("USA", "USA");
        countries.put("Germany", "Germany");

        Map<String, String> map = new HashMap<String, String>();
        map.put("New York", "New York");
        map.put("San Francisco", "San Francisco");
        map.put("Denver", "Denver");
        data.put("USA", map);

        map = new HashMap<String, String>();
        map.put("Berlin", "Berlin");
        map.put("Munich", "Munich");
        map.put("Frankfurt", "Frankfurt");
        data.put("Germany", map);
    }

    public AdvancedSearchBean()
    {

    }

    public AdvancedSearchBean(String query, String region, String extractWord, String anyWord, String noneWord)
    {
        this.query = query;

        if(extractWord != "")
        {
            this.extractWord = extractWord;

        }
        if(anyWord != "")
        {
            this.anyWords = anyWord;
        }

        if(noneWord != "")
        {
            this.noneWords = noneWord;
        }
        String newquery = removeMyGroupQuery(query);
        if(!query.equals(newquery))
        {
            this.query = newquery;
        }
    }

    public String getExtractWord()
    {
        return extractWord;
    }

    public void setExtractWord(String extractWord)
    {
        this.extractWord = extractWord;
    }

    public String getQuery()
    {
        return query;
    }

    public void setQuery(String query)
    {
        this.query = query;
    }

    public String getAnyWords()
    {
        return anyWords;
    }

    public void setAnyWords(String anyWords)
    {
        this.anyWords = anyWords;
    }

    public String getNoneWords()
    {
        return noneWords;
    }

    public void setNoneWords(String noneWords)
    {
        this.noneWords = noneWords;
    }

    public String getLanguage()
    {
        return language;
    }

    public void setLanguage(String language)
    {
        this.language = language;
    }

    public Map<String, String> getLanguages()
    {
        return languages;
    }

    public void setLanguages(Map<String, String> languages)
    {
        this.languages = languages;
    }

    public String getCountry()
    {
        return country;
    }

    public void setCountry(String country)
    {
        this.country = country;
    }

    public String getCity()
    {
        return city;
    }

    public void setCity(String city)
    {
        this.city = city;
    }

    public Map<String, String> getCountries()
    {
        return countries;
    }

    public void setCountries(Map<String, String> countries)
    {
        this.countries = countries;
    }

    public Map<String, String> getCities()
    {
        return cities;
    }

    public void setCities(Map<String, String> cities)
    {
        this.cities = cities;
    }

    public QueryResponse getResult()
    {
        return result;
    }

    public void setResult(QueryResponse result)
    {
        this.result = result;
    }

    private String removeMyGroupQuery(String query)
    {
        String newquery = "";
        Pattern pattern = Pattern.compile("groups\\s*:\\s*my\\s*");
        Matcher matcher = pattern.matcher(query.toLowerCase());
        if(matcher.find())
        {
            int start = matcher.start();
            int end = matcher.end();
            if(start != 0)
                newquery = query.substring(0, start);
            newquery = newquery.concat(query.substring(end, query.length()));
            return newquery;
        }
        else
            return query;
    }

    public String advancedSearch()
    {

        int pageOffset;
        int pageSize;
        HttpSolrClient server = Learnweb.getInstance().getSolrClient().getSolrServer();
        //SolrInputDocument doc = new SolrInputDocument();
        solrQuery = new SolrQuery(query);
        solrQuery.setQuery(query);

        if(query.length() != 0)
            solrQuery.addField(query);

        if(language.length() != 0)
            solrQuery.addFilterQuery("language : " + language);

        if(0 != country.length())
            solrQuery.addFilterQuery("location : " + country);

        solrQuery.setHighlight(true);
        solrQuery.addHighlightField("title");
        solrQuery.addHighlightField("description");

        solrQuery.set("facet", "true");
        if(facetFields != null)
        {
            solrQuery.addFacetField(facetFields);
        }
        if(facetQueries != null && facetQueries.length > 0)
        {
            for(String query : facetQueries)
            {
                solrQuery.addFacetQuery(query);
            }
        }
        solrQuery.setFacetLimit(20); // TODO set to -1 to show all facets (implement "more" button on frontend)
        solrQuery.setFacetSort("count");
        solrQuery.setFacetMinCount(1);

        log.debug("solr query: " + solrQuery);

        //get solrServer
        server = Learnweb.getInstance().getSolrClient().getSolrServer();

        //solrQuery.setHighlight(true).setHighlightSimplePre("<span class='highlighter'").setHighlightSimplePost("</span>").setStart(pageOffset).setRows(pageSize);
        SolrDocumentList sdl;
        try
        {
            sdl = server.query(solrQuery).getResults();
            for(SolrDocument sd : sdl)
            {
                String id = (String) sd.getFieldValue("id");
            }
        }
        catch(IOException | SolrServerException e)
        {
            // TODO Auto-generated catch block
            log.error("unhandled error", e);
        }

        return "/lw/advancedSearch/search_result.xhtml?faces-redirect=true";
    }

    public void onCountryChange()
    {
        if(country != null && !country.equals(""))
            cities = data.get(country);
        else
            cities = new HashMap<String, String>();
    }
}
