package de.l3s.learnweb.beans.admin;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    private Map<String, Set<String>> logins;
    private List<AggregatedRequestData> aggregatedRequests;

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
                logins = getLearnweb().getRequestManager().getLogins();
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
        if (filter != null)
        {
            DateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            String strDate = df.format(((Date) value));
            return strDate.contains((String) filter);
        }

        return true;
    }

    public List<RequestData> getRequests()
    {
        return requests;
    }

    public Map<String, Set<String>> getLogins()
    {
        return logins;
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

}
