package de.l3s.learnweb.beans.admin;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import javax.mail.MessagingException;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.user.loginProtection.AccessData;
import de.l3s.learnweb.web.AggregatedRequestData;
import de.l3s.util.email.BounceManager;

@Named
@SessionScoped
public class AdminBanlistBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -5469152668344315959L;
    private List<AccessData> banlist;
    private List<AggregatedRequestData> suspiciousActivityList;

    private String name;
    private int banDays;
    private int banHours;
    private int banMinutes;
    private boolean permaban;

    private String type;

    public AdminBanlistBean()
    {
        load();
    }

    private void load()
    {
        try
        {
            if(getUser().isAdmin() || getUser().isModerator())
            {
                banlist = getLearnweb().getProtectionManager().getBanlist();

                getLearnweb().getRequestManager().updateAggregatedRequests();
                suspiciousActivityList = getLearnweb().getProtectionManager().getSuspiciousActivityList();
            }
        }
        catch(Exception e)
        {
            addFatalMessage(e);
        }
    }

    public void onManualBan()
    {
        boolean isIP;
        if("ip".equals(type))
        {
            isIP = true;
        }
        else if("user".equals(type))
        {
            isIP = false;
        }
        else
        {
            return;
        }

        if(permaban)
        {
            getLearnweb().getProtectionManager().permaban(name, isIP);
        }
        else
        {
            getLearnweb().getProtectionManager().ban(name, banDays, banHours, banMinutes, isIP);
        }

        load();
    }

    public void onUnban(String name)
    {
        getLearnweb().getProtectionManager().unban(name);
        load();
    }

    public void onClearBanlist()
    {
        getLearnweb().getProtectionManager().clearBans();
        load();
    }

    public void onDeleteOutdatedBans()
    {
        getLearnweb().getProtectionManager().cleanUpOutdatedBans();
        load();
    }

    public void onRemoveSuspicious(String name)
    {
        getLearnweb().getProtectionManager().removeSuspicious(name);
        load();
    }

    public List<AccessData> getBanlist()
    {
        return banlist;
    }

    public String getName()
    {
        return name;
    }

    public String getType()
    {
        return type;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public List<AggregatedRequestData> getSuspiciousActivityList()
    {

        //TODO: AAAAAAAAA
        try
        {
            new BounceManager(Learnweb.getInstance()).parseInbox();
        }
        catch(MessagingException | IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return suspiciousActivityList;

    }

    public void setSuspiciousActivityList(List<AggregatedRequestData> suspiciousActivityList)
    {
        this.suspiciousActivityList = suspiciousActivityList;
    }

    public int getBanDays()
    {
        return banDays;
    }

    public void setBanDays(int banDays)
    {
        this.banDays = banDays;
    }

    public int getBanHours()
    {
        return banHours;
    }

    public void setBanHours(int banHours)
    {
        this.banHours = banHours;
    }

    public int getBanMinutes()
    {
        return banMinutes;
    }

    public void setBanMinutes(int banMinutes)
    {
        this.banMinutes = banMinutes;
    }

    public boolean isPermaban()
    {
        return permaban;
    }

    public void setPermaban(boolean permaban)
    {
        this.permaban = permaban;
    }

}
