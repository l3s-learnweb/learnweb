package de.l3s.learnweb.beans.admin;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.web.Request;
import de.l3s.learnweb.web.RequestManager;

@Named
@RequestScoped
public class AdminRequestListBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = -3469152668344315959L;

    private List<Request> requests;
    private Map<String, Set<String>> logins;
    private List<Request> aggregatedRequests;

    @Inject
    private RequestManager requestManager;

    @PostConstruct
    public void init() {
        BeanAssert.authorized(isLoggedIn());
        BeanAssert.hasPermission(getUser().isAdmin());

        requests = new ArrayList<>(requestManager.getRequests());
        logins = requestManager.getLogins();
        aggregatedRequests = requestManager.getAggregatedRequests();
        onUpdateAggregatedRequests();
    }

    public List<Request> getRequests() {
        return requests;
    }

    public Map<String, Set<String>> getLogins() {
        return logins;
    }

    public List<Request> getAggregatedRequests() {
        return aggregatedRequests;
    }

    public void setAggregatedRequests(List<Request> aggregatedRequests) {
        this.aggregatedRequests = aggregatedRequests;
    }

    public LocalDateTime getAggrRequestsUpdated() {
        return requestManager.getAggrRequestsUpdated();
    }

    public void onUpdateAggregatedRequests() {
        requestManager.updateAggregatedRequests();
    }

}
