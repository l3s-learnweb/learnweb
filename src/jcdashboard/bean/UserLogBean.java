package jcdashboard.bean;

import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import jcdashboard.model.dao.UserLogHome;

@ManagedBean(name = "userlog")
@ViewScoped
public class UserLogBean
{
    Map<String, Integer> mappa;

    public Map<String, Integer> getMappa()
    {
        UserLogHome ulh = new UserLogHome();
        mappa = ulh.actionPerDay();
        for(String k : mappa.keySet())
        {
            System.out.println(k + "------------" + mappa.get(k));
        }
        return mappa;
    }

    /*
    public void setMappa(Map<String, Integer> mappa)
    {
        this.mappa = mappa;
    }
    */
}
