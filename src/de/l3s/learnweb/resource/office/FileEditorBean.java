package de.l3s.learnweb.resource.office;

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
        filesExtension = FileUtility.getFileExtension(resource.getFileName());

        if(FileUtility.canBeViewed(filesExtension))
        {
            this.resource = resource;

            mainFile = resource.getFile(TYPE.FILE_MAIN);
            fullFilesUrl = resource.getFileUrl();
            fileType = FileUtility.getFileType(resource.getFileName());
            filesExtension = FileUtility.getFileExtension(resource.getFileName());
            if(mainFile != null) key = FileUtility.generateRevisionId(mainFile);
        }
        else
        {
            resource.setOnlineStatus(OnlineStatus.OFFLINE);
        }
    }

    public Resource getResource()
    {
        return resource;
    }

    public String getFilesExtension()
    {
        return filesExtension;
    }

    public String getFileType()
    {
        return fileType;
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
        {
            onlyOfficeClientUrl = getLearnweb().getProperties().getProperty("FILES.DOCSERVICE.URL.CLIENT");
        }
        return onlyOfficeClientUrl;
    }

    public String getCallbackUrl()
    {
        final String fileId = mainFile != null ? Integer.toString(mainFile.getId()) : "";
        return getLearnweb().getSecureServerUrl() + "/save?fileId=" + fileId;
    }

    public String getHistoryUrl()
    {
        return getLearnweb().getSecureServerUrl() + "/history";
    }
}
