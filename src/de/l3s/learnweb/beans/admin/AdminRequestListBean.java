package de.l3s.learnweb.beans.admin;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.web.AggregatedRequestData;
import de.l3s.learnweb.web.RequestData;

@ManagedBean
@RequestScoped
public class AdminRequestListBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -5469152668344315959L;
    private List<RequestData> requests;
    private List<RequestData> filteredRequests;
    private List<Map.Entry<String, Set<String>>> logins;

    private List<AggregatedRequestData> aggregatedRequests = null;

    public AdminRequestListBean()
    {
        load();
    }

    private void load()
    {
        try
        {
            if(getUser().isAdmin() || getUser().isModerator())
            {
                requests = getLearnweb().getRequestManager().getRequests();
                logins = new ArrayList<Map.Entry<String, Set<String>>>(getLearnweb().getRequestManager().getLogins().entrySet());
                aggregatedRequests = getLearnweb().getRequestManager().getAggrRequests();
            }
        }
        catch(Exception e)
        {
            addFatalMessage(e);
        }
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

    public void onRecordRequests()
    {
        getLearnweb().getRequestManager().recordRequestsToDB();
    }

    public void onClearRequestsDB()
    {
        getLearnweb().getRequestManager().clearRequestsDB();
    }

}
