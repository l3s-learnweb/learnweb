package de.l3s.learnweb.rm.beans;

import java.io.Serializable;
import java.sql.SQLException;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Resource;
import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.beans.ResourceBean;
import de.l3s.learnweb.rm.ExtendedMetadata;

@ManagedBean
@ViewScoped
public class MetadataBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = 6827609597439580988L;
    private final static Logger log = Logger.getLogger(ResourceBean.class);
    private int id = 203447;
    private Resource resource;
    private ExtendedMetadata em;
    private String category = "empty";

    public void loadMetadata()
    {
        if(null != resource)
        {

            try
            {
                em = resource.getExtendedMetadata();
            }
            catch(SQLException e)
            {
                addMessage(FacesMessage.SEVERITY_FATAL, "failed to get extended metadata for given resource id");
                return;
            }
            category = em.getCategories().get(0);
            return;
        }

        if(id == 0) // no or invalid resource_id
        {
            // safeId is created to avoid NullPointException
            Integer safeId = getParameterInt("resource_id");

            if(safeId == null || safeId == 0)
            {
                addMessage(FacesMessage.SEVERITY_FATAL, "invalid or no resource_id parameter");
                return;
            }

            id = safeId;
        }

        try
        {
            resource = getLearnweb().getResourceManager().getResource(getId());
            try
            {
                em = resource.getExtendedMetadata();
            }
            catch(SQLException e)
            {
                addMessage(FacesMessage.SEVERITY_FATAL, "failed to get extended metadata for given resource id");
                return;
            }
            category = em.getCategories().get(0);

            if(null == resource)
            {
                addMessage(FacesMessage.SEVERITY_FATAL, "Invalid or no resource_id parameter. Maybe the resource was deleted");
                return;
            }
        }
        catch(Exception e)
        {
            log.error("can't load resource: " + id, e);
            addFatalMessage(e);
        }
    }

    public int getId()
    {
        return id;
    }

    public Resource getResource()
    {
        return resource;
    }

    public void setResource(Resource resource)
    {
        this.resource = resource;
    }

    public ExtendedMetadata getEm()
    {
        return em;
    }

    public void setEm(ExtendedMetadata em)
    {
        this.em = em;
    }

    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

}
