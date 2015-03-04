package de.l3s.learnweb;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.ws.rs.core.Response.Status.Family;

import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONException;
import org.primefaces.json.JSONObject;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import de.l3s.util.StringHelper;

public class Entity implements Serializable
{
    private static final long serialVersionUID = -103883497340301020L;

    private String type;
    private Boolean supported = false;
    private String label;
    private String url;
    private String description;
    private String thumbnail;
    private String birthDate;
    private String birthPlace;
    private String children;
    private String fullName;
    private String alternativeNames;
    private String birthName;
    private String deathDate;
    private String deathPlace;
    private String spouse;
    private String homepage;
    private String foundedBy;
    private String foundingDate;
    private String foundingYear;
    private String launchDate;
    private String industry;
    private String companyType;
    private String areaServed;
    private String numberOfEmployees;
    private String capital;
    private String dialingCode;
    private String population;
    private String populationAsOf;
    private String languages;
    private String governmentType;
    private String nationalAnthem;
    private String currencies;
    private String country;
    private String area;
    private String governor;
    private String senators;
    private String state;
    private String timeZone;
    private JSONObject json;

    private final static String SERVER = "prometheus.kbs.uni-hannover.de:8890";

    public static void main(String[] args)
    {
	Entity ent = new Entity();
	ent.setUrl("http://dbpedia.org/resource/Barack_Obama");
	ent.setType("person");
	ent.extractInfo();

    }

    public void extractInfo()
    {
	Client client = Client.create();
	//WebResource resource = client.resource("http://prometheus.kbs.uni-hannover.de:8890/sparql?default-graph-uri=http%3A%2F%2F"+ SERVER +"&query=DESCRIBE+%3C" + url + "%3E&output=application%2Fmicrodata%2Bjson");
	url = url.replaceFirst("dbpedia.org", SERVER);
	WebResource resource = client.resource(url.replaceFirst("resource", "data") + ".json?timeout=1000");

	ClientResponse response = resource.get(ClientResponse.class);
	if(response.getClientResponseStatus().getFamily() == Family.SUCCESSFUL)
	{
	    try
	    {
		String strangeDBurl = url.substring(url.lastIndexOf('/') + 1);
		System.out.println("http://" + SERVER + "/resource/" + strangeDBurl);

		// only the last part of the url has to be url encoded
		strangeDBurl = "http://" + SERVER + "/resource/" + StringHelper.urlEncode(url.substring(url.lastIndexOf('/') + 1));

		json = new JSONObject(response.getEntity(String.class));
		json = json.getJSONObject(strangeDBurl);

		if(null == type)
		    return;

		if(type.equals("person"))
		{
		    thumbnail = getValue("http://" + SERVER + "/ontology/thumbnail");
		    birthDate = getValue("http://" + SERVER + "/ontology/birthDate");
		    birthPlace = getValue("http://" + SERVER + "/property/placeOfBirth");
		    children = getValue("http://" + SERVER + "/property/children");
		    fullName = getValue("http://" + SERVER + "/property/fullname");
		    alternativeNames = getValue("http://" + SERVER + "/property/alternativeNames");
		    birthName = getValue("http://" + SERVER + "/property/birthName");
		    deathDate = getValue("http://" + SERVER + "/ontology/deathDate");
		    deathPlace = getValue("http://" + SERVER + "/property/placeOfDeath");
		    spouse = getValue("http://" + SERVER + "/property/spouse");
		}
		if(type.equals("organization"))
		{
		    thumbnail = getValue("http://" + SERVER + "/ontology/thumbnail");
		    homepage = getValue("http://" + SERVER + "/property/homepage");
		    if(homepage.equals(""))
			homepage = getValue("http://xmlns.com/foaf/0.1/homepage");
		    foundedBy = getValue("http://" + SERVER + "/ontology/foundedBy");
		    foundingDate = getValue("http://" + SERVER + "/ontology/foundingDate");
		    if(foundingDate.equals(""))
			foundingYear = getValue("http://" + SERVER + "/ontology/foundingYear");
		    launchDate = getValue("http://" + SERVER + "/ontology/launchDate");
		    industry = getValue("http://" + SERVER + "/ontology/industry");
		    companyType = getValue("http://" + SERVER + "/property/companyType");
		    areaServed = getValue("http://" + SERVER + "/property/areaServed");
		    numberOfEmployees = getValue("http://" + SERVER + "/ontology/numberOfEmployees");
		}
		if(type.equals("place"))
		{
		    thumbnail = getValue("http://" + SERVER + "/ontology/thumbnail");
		    capital = getValue("http://" + SERVER + "/property/capital");
		    dialingCode = getValue("http://" + SERVER + "/property/callingCode");
		    population = getValue("http://" + SERVER + "/property/populationTotal");
		    populationAsOf = getValue("http://" + SERVER + "/property/populationAsOf");
		    languages = getValue("http://" + SERVER + "/property/languages");
		    governmentType = getValue("http://" + SERVER + "/property/governmentType");
		    nationalAnthem = getValue("http://" + SERVER + "/property/nationalAnthem");
		    currencies = getValue("http://" + SERVER + "/property/currency");
		    country = getValue("http://" + SERVER + "/ontology/country");
		    area = getValue("http://" + SERVER + "/property/areaTotalKm");
		    governor = getValue("http://" + SERVER + "/property/governor");
		    senators = getValue("http://" + SERVER + "/property/senators");
		    state = getValue("http://" + SERVER + "/ontology/isPartOf");
		    timeZone = getValue("http://" + SERVER + "/ontology/timeZone");
		}
	    }
	    catch(Exception e)
	    {
		e.printStackTrace();
	    }
	}
	else
	{
	    System.out.println("Error");
	}
    }

    public String getValue(String field)
    {

	String value = "";
	try
	{
	    String fieldValue = json.get(field).toString();

	    JSONArray jarray = new JSONArray(fieldValue);
	    for(int i = 0; i < jarray.length(); i++)
	    {
		JSONObject jo = jarray.getJSONObject(i);
		if(jo.getString("type").equals("uri") && !field.equals("http://" + SERVER + "/ontology/thumbnail"))
		{
		    if(field.equals("http://" + SERVER + "/property/children"))
			continue;

		    value = value + jo.getString("value").substring(jo.getString("value").lastIndexOf("/") + 1).replace("_", " ") + ", ";
		}
		else
		{
		    try
		    {
			String datatype = jo.getString("datatype");
			if(datatype.contains("date"))
			{
			    Date date = new SimpleDateFormat("yyyy-MM-dd").parse(jo.getString("value"));
			    value = new SimpleDateFormat("MMMM dd, yyyy").format(date) + ", ";
			}
			else if(datatype.contains("gYear"))
			{
			    value = jo.getString("value").substring(0, 6);
			}
			else
			{
			    throw new JSONException("Not Date format");
			}
		    }
		    catch(JSONException e)
		    {
			value = value + jo.getString("value") + ", ";
		    }
		    catch(Exception e)
		    {
			e.printStackTrace();
		    }
		}
	    }
	    value = value.substring(0, value.length() - 2);
	}
	catch(JSONException e)
	{
	    value = "";
	}

	if(value.trim().length() < 3)
	    return "";

	return value;
    }

    public String getLabel()
    {
	return label;
    }

    public void setLabel(String label)
    {
	this.label = label;
    }

    public String getUrl()
    {
	return url;
    }

    public void setUrl(String url)
    {
	this.url = url;
    }

    public String getDescription()
    {
	return description;
    }

    public void setDescription(String description)
    {
	this.description = description;
    }

    public String getType()
    {
	return type;
    }

    public void setType(String type)
    {
	this.type = type;
    }

    public String getThumbnail()
    {
	return thumbnail;
    }

    public void setThumbnail(String thumbnail)
    {
	this.thumbnail = thumbnail;
    }

    public Boolean getSupported()
    {
	return supported;
    }

    public void setSupported(Boolean supported)
    {
	this.supported = supported;
    }

    public String getBirthDate()
    {
	return birthDate;
    }

    public void setBirthDate(String birthDate)
    {
	this.birthDate = birthDate;
    }

    public String getBirthPlace()
    {
	return birthPlace;
    }

    public void setBirthPlace(String birthPlace)
    {
	this.birthPlace = birthPlace;
    }

    public String getChildren()
    {
	return children;
    }

    public void setChildren(String children)
    {
	this.children = children;
    }

    public String getFullName()
    {
	return fullName;
    }

    public void setFullName(String fullName)
    {
	this.fullName = fullName;
    }

    public String getAlternativeNames()
    {
	return alternativeNames;
    }

    public void setAlternativeNames(String alternativeNames)
    {
	this.alternativeNames = alternativeNames;
    }

    public String getBirthName()
    {
	return birthName;
    }

    public void setBirthName(String birthName)
    {
	this.birthName = birthName;
    }

    public String getDeathDate()
    {
	return deathDate;
    }

    public void setDeathDate(String deathDate)
    {
	this.deathDate = deathDate;
    }

    public String getDeathPlace()
    {
	return deathPlace;
    }

    public void setDeathPlace(String deathPlace)
    {
	this.deathPlace = deathPlace;
    }

    public String getSpouse()
    {
	return spouse;
    }

    public void setSpouse(String spouse)
    {
	this.spouse = spouse;
    }

    public String getHomepage()
    {
	return homepage;
    }

    public void setHomepage(String homepage)
    {
	this.homepage = homepage;
    }

    public String getFoundedBy()
    {
	return foundedBy;
    }

    public void setFoundedBy(String foundedBy)
    {
	this.foundedBy = foundedBy;
    }

    public String getFoundingDate()
    {
	return foundingDate;
    }

    public void setFoundingDate(String foundingDate)
    {
	this.foundingDate = foundingDate;
    }

    public String getFoundingYear()
    {
	return foundingYear;
    }

    public void setFoundingYear(String foundingYear)
    {
	this.foundingYear = foundingYear;
    }

    public String getLaunchDate()
    {
	return launchDate;
    }

    public void setLaunchDate(String launchDate)
    {
	this.launchDate = launchDate;
    }

    public String getIndustry()
    {
	return industry;
    }

    public void setIndustry(String industry)
    {
	this.industry = industry;
    }

    public String getCompanyType()
    {
	return companyType;
    }

    public void setCompanyType(String companyType)
    {
	this.companyType = companyType;
    }

    public String getAreaServed()
    {
	return areaServed;
    }

    public void setAreaServed(String areaServed)
    {
	this.areaServed = areaServed;
    }

    public String getNumberOfEmployees()
    {
	return numberOfEmployees;
    }

    public void setNumberOfEmployees(String numberOfEmployees)
    {
	this.numberOfEmployees = numberOfEmployees;
    }

    public JSONObject getJson()
    {
	return json;
    }

    public void setJson(JSONObject json)
    {
	this.json = json;
    }

    public String getCapital()
    {
	return capital;
    }

    public void setCapital(String capital)
    {
	this.capital = capital;
    }

    public String getDialingCode()
    {
	return dialingCode;
    }

    public void setDialingCode(String dialingCode)
    {
	this.dialingCode = dialingCode;
    }

    public String getPopulation()
    {
	return population;
    }

    public void setPopulation(String population)
    {
	this.population = population;
    }

    public String getLanguages()
    {
	return languages;
    }

    public void setLanguages(String languages)
    {
	this.languages = languages;
    }

    public String getGovernmentType()
    {
	return governmentType;
    }

    public void setGovernmentType(String governmentType)
    {
	this.governmentType = governmentType;
    }

    public String getNationalAnthem()
    {
	return nationalAnthem;
    }

    public void setNationalAnthem(String nationalAnthem)
    {
	this.nationalAnthem = nationalAnthem;
    }

    public String getCurrencies()
    {
	return currencies;
    }

    public void setCurrencies(String currencies)
    {
	this.currencies = currencies;
    }

    public String getCountry()
    {
	return country;
    }

    public void setCountry(String country)
    {
	this.country = country;
    }

    public String getArea()
    {
	return area;
    }

    public void setArea(String area)
    {
	this.area = area;
    }

    public String getGovernor()
    {
	return governor;
    }

    public void setGovernor(String governor)
    {
	this.governor = governor;
    }

    public String getSenators()
    {
	return senators;
    }

    public void setSenators(String senators)
    {
	this.senators = senators;
    }

    public String getState()
    {
	return state;
    }

    public void setState(String state)
    {
	this.state = state;
    }

    public String getTimeZone()
    {
	return timeZone;
    }

    public void setTimeZone(String timeZone)
    {
	this.timeZone = timeZone;
    }

    public String getPopulationAsOf()
    {
	return populationAsOf;
    }

    public void setPopulationAsOf(String populationAsOf)
    {
	this.populationAsOf = populationAsOf;
    }

}
