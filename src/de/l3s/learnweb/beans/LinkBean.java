package de.l3s.learnweb.beans;

import java.io.IOException;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.l3s.learnweb.LogEntry.Action;

@ManagedBean
@RequestScoped
public class LinkBean extends ApplicationBean
{

    private String url;

    public LinkBean()
    {

    }

    public void redirect()
    {

    }

    public String getUrl()
    {
        FacesContext facesContext = getFacesContext();
        ExternalContext ext = facesContext.getExternalContext();
        HttpServletRequest servletRequest = (HttpServletRequest) ext.getRequest();
        HttpServletResponse servletResponse = (HttpServletResponse) ext.getResponse();

        url = servletRequest.getParameter("link");

        if(url != null && url != "")
        {
            try
            {
                servletResponse.sendRedirect(url);
                log(Action.open_link, 0, 0, url);

            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

}
