package de.l3s.learnweb.beans;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Resource;
import de.l3s.learnweb.Submission;
import de.l3s.learnweb.beans.GroupDetailBean.RPAction;
import de.l3s.office.FileEditorBean;
import de.l3s.util.StringHelper;

/**
 *
 * @author Philipp
 *
 */
@ManagedBean
@ViewScoped
public class SubmissionModeratorBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -2494182373382483709L;
    private final static Logger log = Logger.getLogger(SubmissionModeratorBean.class);

    private int submissionId = -1;

    // required to show resources in right panel
    private Resource clickedResource;
    private RPAction rightPanelAction;
    @ManagedProperty(value = "#{resourceDetailBean}")
    private ResourceDetailBean resourceDetailBean;
    @ManagedProperty(value = "#{fileEditorBean}")
    private FileEditorBean fileEditorBean;
    private Submission submission;

    public void onLoad()
    {
        try
        {
            submission = getLearnweb().getSubmissionManager().getSubmissionById(submissionId);

            if(null == submission)
            {
                addMessage(FacesMessage.SEVERITY_ERROR, "missing parameter");
                return;
            }
        }
        catch(SQLException e)
        {
            addFatalMessage(e);
        }

    }

    public int getSubmissionId()
    {
        return submissionId;
    }

    public void setSubmissionId(int submissionId)
    {
        this.submissionId = submissionId;
    }

    public Submission getSubmission()
    {
        return submission;
    }

    // methods required to show resources in right panel
    public void actionSelectGroupItem()
    {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();

        try
        {
            String itemType = params.get("itemType");
            int itemId = StringHelper.parseInt(params.get("itemId"), -1);

            if(itemType != null && itemType.equals("resource") && itemId > 0)
            {
                Resource resource = getLearnweb().getResourceManager().getResource(itemId);
                if(resource != null)
                {
                    this.setClickedResource(resource);
                    if((resource.getType().equals("Presentation") || resource.getType().equals("Text") || resource.getType().equals("Spreadsheet")) && resource.getStorageType() == 1)
                        getFileEditorBean().fillInFileInfo(resource);
                }
                else
                    throw new NullPointerException("Target resource does not exists");
            }

        }
        catch(NullPointerException | SQLException e)
        {
            log.error(e);
        }
    }

    public Resource getClickedResource()
    {
        return clickedResource;
    }

    public void setClickedResource(Resource resource)
    {
        clickedResource = resource;
        this.getResourceDetailBean().setClickedResource(clickedResource);
        this.rightPanelAction = RPAction.viewResource;
    }

    public void setFileEditorBean(FileEditorBean fileEditorBean)
    {
        this.fileEditorBean = fileEditorBean;
    }

    public RPAction getRightPanelAction()
    {
        return rightPanelAction;
    }

    public ResourceDetailBean getResourceDetailBean()
    {
        return resourceDetailBean;
    }

    public void setResourceDetailBean(ResourceDetailBean resourceDetailBean)
    {
        this.resourceDetailBean = resourceDetailBean;
    }

    public FileEditorBean getFileEditorBean()
    {
        return fileEditorBean;
    }

}
