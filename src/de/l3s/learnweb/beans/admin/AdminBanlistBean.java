package de.l3s.learnweb.beans.admin;

import java.io.IOException;
import java.io.Serializable;
import java.time.Duration;
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

    private String addr;
    private Integer banDays;
    private Integer banHours;
    private Integer banMinutes;
    private boolean permaban;

    private List<Request> suspiciousActivityList;

    @Inject
    private RequestManager requestManager;

    public void onManualBan() {
        if (permaban) {
            banDays = 36524; // A hundred years should be fair enough
        }

        banDays = Optional.ofNullable(banDays).orElse(0);
        banHours = Optional.ofNullable(banHours).orElse(0);
        banMinutes = Optional.ofNullable(banMinutes).orElse(0);

        requestManager.ban(addr, "manual ban", Duration.ofDays(banDays).plusHours(banHours).plusMinutes(banMinutes));
    }

    public void onUnban(String addr) {
        requestManager.clearBan(addr);
    }

    public void onDeleteOutdatedBans() {
        requestManager.clearOutdatedBans();
    }

    public void onRemoveSuspicious(String addr) {
        requestManager.getSuspiciousRequests().removeIf(requestData -> addr.equals(requestData.getIp()));
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

    public String getAddr() {
        return addr;
    }

    public void setAddr(final String addr) {
        this.addr = addr;
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
