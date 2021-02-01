package de.l3s.learnweb.beans.admin;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;

import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.mail.MessagingException;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.loginProtection.Ban;
import de.l3s.learnweb.web.AggregatedRequestData;
import de.l3s.util.email.BounceManager;

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

    private List<AggregatedRequestData> suspiciousActivityList;

    public void onManualBan() {
        boolean isIP = "ip".equalsIgnoreCase(type);

        if (permaban) {
            banDays = 36524; // A hundred years should be fair enough
        }

        banDays = Optional.ofNullable(banDays).orElse(0);
        banHours = Optional.ofNullable(banHours).orElse(0);
        banMinutes = Optional.ofNullable(banMinutes).orElse(0);

        getLearnweb().getProtectionManager().ban(name, banDays, banHours, banMinutes, isIP, null);
    }

    public void onUnban(String name) {
        getLearnweb().getProtectionManager().clearBan(name);
    }

    public void onDeleteOutdatedBans() {
        getLearnweb().getProtectionManager().clearOutdatedBans();
    }

    public void onRemoveSuspicious(String name) {
        getLearnweb().getProtectionManager().removeSuspicious(name);
        suspiciousActivityList = null;
    }

    public List<Ban> getBanlist() {
        return getLearnweb().getProtectionManager().getBanlist();
    }

    public List<AggregatedRequestData> getSuspiciousActivityList() {
        if (suspiciousActivityList == null) {
            try {
                new BounceManager(Learnweb.getInstance()).parseInbox();
            } catch (MessagingException | IOException e) {
                addErrorMessage(e);
            }

            getLearnweb().getRequestManager().updateAggregatedRequests();
            suspiciousActivityList = getLearnweb().getProtectionManager().getSuspiciousActivityList();
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
