package de.l3s.learnweb.beans.admin;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.MessagingException;

import org.omnifaces.util.Beans;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.web.Ban;
import de.l3s.learnweb.web.BounceManager;
import de.l3s.learnweb.web.Request;
import de.l3s.learnweb.web.RequestManager;

@Named
@ViewScoped
public class AdminBanlistBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = -4469152668344315959L;

    private String type = "user";
    private String name;
    private Integer banDays;
    private Integer banHours;
    private Integer banMinutes;
    private boolean permaban;

    private List<Request> suspiciousActivityList;

    @Inject
    private RequestManager requestManager;

    public void onManualBan() {
        boolean isIP = "ip".equalsIgnoreCase(type);

        if (permaban) {
            banDays = 36524; // A hundred years should be fair enough
        }

        banDays = Optional.ofNullable(banDays).orElse(0);
        banHours = Optional.ofNullable(banHours).orElse(0);
        banMinutes = Optional.ofNullable(banMinutes).orElse(0);

        requestManager.ban(name, banDays, banHours, banMinutes, isIP, null);
    }

    public void onUnban(String name) {
        requestManager.clearBan(name);
    }

    public void onDeleteOutdatedBans() {
        requestManager.clearOutdatedBans();
    }

    public void onRemoveSuspicious(String name) {
        requestManager.getSuspiciousRequests().removeIf(requestData -> name.equals(requestData.getIp()));
        suspiciousActivityList = null;
    }

    public List<Ban> getBanlist() {
        return requestManager.getBanlist();
    }

    public List<Request> getSuspiciousActivityList() {
        if (suspiciousActivityList == null) {
            try {
                Beans.getInstance(BounceManager.class).parseInbox();
            } catch (MessagingException | IOException e) {
                addErrorMessage(e);
            }

            requestManager.updateAggregatedRequests();
            suspiciousActivityList = requestManager.getSuspiciousRequests();
        }
        return suspiciousActivityList;

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getBanDays() {
        return banDays;
    }

    public void setBanDays(Integer banDays) {
        this.banDays = banDays;
    }

    public Integer getBanHours() {
        return banHours;
    }

    public void setBanHours(Integer banHours) {
        this.banHours = banHours;
    }

    public Integer getBanMinutes() {
        return banMinutes;
    }

    public void setBanMinutes(Integer banMinutes) {
        this.banMinutes = banMinutes;
    }

    public boolean isPermaban() {
        return permaban;
    }

    public void setPermaban(boolean permaban) {
        this.permaban = permaban;
    }
}
