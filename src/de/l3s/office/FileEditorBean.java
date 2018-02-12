package de.l3s.office;

import static de.l3s.office.FileUtility.canBeViewed;
import static de.l3s.office.FileUtility.getFileExtension;

import java.io.Serializable;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import de.l3s.learnweb.File;
import de.l3s.learnweb.File.TYPE;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.Resource.OnlineStatus;
import de.l3s.learnweb.beans.ApplicationBean;

@ManagedBean
@ViewScoped
public class FileEditorBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -655001215017199006L;

    private Resource resource;

    private File mainFile;

    private String filesExtension;

    private String fileType;

    private String fullFilesUrl;

    private String key;

    private final String onlyOfficeClientUrl;

    public FileEditorBean()
    {
        this.onlyOfficeClientUrl = getLearnweb().getProperties().getProperty("FILES.DOCSERVICE.URL.CLIENT");
    }

    public void fillInFileInfo(Resource resource)
    {
        if(!canBeViewed(getFileExtension(resource.getFileName())))
        {
            resource.setOnlineStatus(OnlineStatus.OFFLINE);
        }
        else
        {
            this.resource = resource;
            if(resource != null)
                mainFile = resource.getFile(TYPE.FILE_MAIN);
            setFullFilesUrl();
            setKey();
            setFileType(FileUtility.getFileType(resource.getFileName()));
            setFilesExtension(FileUtility.getFileExtension(resource.getFileName()).replace(".", ""));
        }
    }

    public void setKey()
    {
        if(mainFile != null)
            key = FileUtility.generateRevisionId(mainFile);
    }

    public String getCallbackUrl()
    {
        return getLearnweb().getSecureServerUrl() + "/save" + "?fileId=" + getMainFileId();
    }

    /*
    private String getServerUrl()
    {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        return getServerUrlWithoutContextPath() + request.getContextPath();
    }
    
    private String getServerUrlWithoutContextPath()
    {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
        return request.getScheme() + "://" + request.getServerName() + ':' + request.getServerPort();
    }
    */

    private String getMainFileId()
    {
        return mainFile != null ? Integer.toString(mainFile.getId()) : "";
    }

    public Resource getResource()
    {
        return resource;
    }

    public void setResource(Resource file)
    {
        this.resource = file;
    }

    public String getFilesExtension()
    {
        return filesExtension;
    }

    public void setFilesExtension(String filesExtension)
    {
        this.filesExtension = filesExtension;
    }

    public String getFileType()
    {
        return fileType;
    }

    public void setFileType(String fileType)
    {
        this.fileType = fileType;
    }

    public void setFullFilesUrl()
    {
        fullFilesUrl = resource.getFileUrl();
    }

    public String getFullFilesUrl()
    {
        return fullFilesUrl;
    }

    public String getKey()
    {
        return key;
    }

    public String getOnlyOfficeClientUrl()
    {
        return onlyOfficeClientUrl;
    }

}
