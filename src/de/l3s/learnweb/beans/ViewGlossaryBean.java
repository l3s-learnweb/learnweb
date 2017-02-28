/*package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.l3s.glossary.GlossaryItems;
import de.l3s.learnweb.GlossariesManager;
import de.l3s.learnwebBeans.ApplicationBean;

@ManagedBean
@ViewScoped
public class ViewGlossaryBean extends ApplicationBean implements Serializable
{
    private final static Logger log = Logger.getLogger(ViewGlossaryBean.class);
    private static final long serialVersionUID = -3927594222612462194L;
    private int resourceId;
    private List<GlossaryItems> items = new ArrayList<GlossaryItems>();
    private List<GlossaryItems> fileteredItems = new ArrayList<GlossaryItems>();

    GlossariesManager gl;

    public void preRenderView()
    {
        if(isAjaxRequest())
            return;

        if(resourceId > 0)
        {
            getGlossaryItems(resourceId);
            setFileteredItems(getItems());

        }
    }

    private void getGlossaryItems(int id)
    {
        gl = getLearnweb().getGlossariesManager();
        items = gl.getGlossaryItems(id);
    }

    public int getResourceId()
    {
        return resourceId;
    }

    public void setResourceId(int resourceId)
    {
        this.resourceId = resourceId;
    }

    public List<GlossaryItems> getItems()
    {
        return items;
    }

    public void setItems(List<GlossaryItems> items)
    {
        this.items = items;
    }

    public List<GlossaryItems> getFileteredItems()
    {
        return fileteredItems;
    }

    public void setFileteredItems(List<GlossaryItems> fileteredItems)
    {
        this.fileteredItems = fileteredItems;
    }
}
*/
