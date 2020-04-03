package de.l3s.learnweb.resource.office;

import static de.l3s.learnweb.resource.office.FileUtility.canBeViewed;
import static de.l3s.learnweb.resource.office.FileUtility.getFileExtension;

import java.io.Serializable;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.resource.File.TYPE;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.Resource.OnlineStatus;

@Named
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

    private String onlyOfficeClientUrl;

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
        if(null == onlyOfficeClientUrl)
            onlyOfficeClientUrl = getLearnweb().getProperties().getProperty("FILES.DOCSERVICE.URL.CLIENT");
        return onlyOfficeClientUrl;
    }

}
