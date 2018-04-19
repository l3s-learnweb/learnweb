package de.l3s.learnweb.beans.admin;

import java.io.Serializable;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.loginprotection.AccessData;

@ManagedBean
@RequestScoped
public class AdminBanlistBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -5469152668344315959L;
    private List<AccessData> banlist;

    private String name;
    private int bantime;
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

        getLearnweb().getProtectionManager().ban(name, bantime, isIP);

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

    public List<AccessData> getBanlist()
    {
        return banlist;
    }

    public String getName()
    {
        return name;
    }

    public int getBantime()
    {
        return bantime;
    }

    public String getType()
    {
        return type;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setBantime(int bantime)
    {
        this.bantime = bantime;
    }

    public void setType(String type)
    {
        this.type = type;
    }

}
