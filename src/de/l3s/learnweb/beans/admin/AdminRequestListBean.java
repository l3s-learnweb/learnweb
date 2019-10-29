package de.l3s.learnweb.beans.admin;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.web.AggregatedRequestData;
import de.l3s.learnweb.web.RequestData;

@Named
@RequestScoped
public class AdminRequestListBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -3469152668344315959L;
    private List<RequestData> requests;
    private List<RequestData> filteredRequests;

    private List<Map.Entry<String, Set<String>>> logins;
    private List<Map.Entry<String, Set<String>>> filteredLogins;

    private List<AggregatedRequestData> aggregatedRequests = null;
    private List<AggregatedRequestData> filteredAggregatedRequests = null;

    public AdminRequestListBean()
    {
        load();
    }

    private void load()
    {
        try
        {
            if(getUser() != null && getUser().isAdmin())
            {
                requests = new ArrayList<>(getLearnweb().getRequestManager().getRequests());
                logins = new ArrayList<>(getLearnweb().getRequestManager().getLogins().entrySet());
                aggregatedRequests = getLearnweb().getRequestManager().getAggregatedRequests();
                onUpdateAggregatedRequests();
            }
        }
        catch(Exception e)
        {
            addErrorMessage(e);
        }
    }

    public boolean filterByDate(Object value, Object filter, Locale locale)
    {
        DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        String strDate = df.format(((Date) value));
        return strDate.contains((String) filter);
    }

    public List<RequestData> getRequests()
    {
        return requests;
    }

    public List<Entry<String, Set<String>>> getLogins()
    {
        return logins;
    }

    public List<RequestData> getFilteredRequests()
    {
        return filteredRequests;
    }

    public void setFilteredRequests(List<RequestData> filteredRequests)
    {
        this.filteredRequests = filteredRequests;
    }

    public List<AggregatedRequestData> getAggregatedRequests()
    {
        return aggregatedRequests;
    }

    public void setAggregatedRequests(List<AggregatedRequestData> aggregatedRequests)
    {
        this.aggregatedRequests = aggregatedRequests;
    }

    public Date getAggrRequestsUpdated()
    {
        return getLearnweb().getRequestManager().getAggrRequestsUpdateTime();
    }

    public void onUpdateAggregatedRequests()
    {
        getLearnweb().getRequestManager().updateAggregatedRequests();
    }

    public List<Map.Entry<String, Set<String>>> getFilteredLogins()
    {
        return filteredLogins;
    }

    public void setFilteredLogins(List<Map.Entry<String, Set<String>>> filteredLogins)
    {
        this.filteredLogins = filteredLogins;
    }

    public List<AggregatedRequestData> getFilteredAggregatedRequests()
    {
        return filteredAggregatedRequests;
    }

    public void setFilteredAggregatedRequests(List<AggregatedRequestData> filteredAggregatedRequests)
    {
        this.filteredAggregatedRequests = filteredAggregatedRequests;
    }

}
