package de.l3s.learnweb.resource.office;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.util.Beans;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.resource.File.TYPE;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.Resource.OnlineStatus;
import de.l3s.learnweb.resource.ResourceDetailBean;

@Named
@ViewScoped
public class OfficeResourceBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -655001215017199006L;
    private static final Logger log = LogManager.getLogger(OfficeResourceBean.class);

    private File mainFile;
    private String filesExtension;
    private String fileType;
    private String fullFilesUrl;
    private String key;

    private String onlyOfficeClientUrl;
    private String documentServerUrl;

    @PostConstruct
    public void init()
    {
        onlyOfficeClientUrl = getLearnweb().getProperties().getProperty("FILES.DOCSERVICE.URL.CLIENT");
        documentServerUrl = getLearnweb().getProperties().getProperty("DOCUMENT_SERVER_URL");

        if (StringUtils.isBlank(documentServerUrl) || "auto".equalsIgnoreCase(documentServerUrl))
            documentServerUrl = getLearnweb().getServerUrl();

        Resource resource = Beans.getInstance(ResourceDetailBean.class).getResource();
        fillInFileInfo(resource);
    }

    private void fillInFileInfo(Resource resource)
    {
        filesExtension = FileUtility.getFileExtension(resource.getFileName());

        if(FileUtility.canBeViewed(filesExtension))
        {
            mainFile = resource.getFile(TYPE.FILE_MAIN);
            fullFilesUrl = resource.getFileUrl();
            fileType = FileUtility.getFileType(resource.getFileName());
            filesExtension = FileUtility.getFileExtension(resource.getFileName());
            if(mainFile != null) key = FileUtility.generateRevisionId(mainFile);
        }
        else
        {
            log.error("Office type resource has not supported extension.");
            resource.setOnlineStatus(OnlineStatus.OFFLINE);
        }
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
        return onlyOfficeClientUrl;
    }

    public String getCallbackUrl()
    {
        final String fileId = mainFile != null ? Integer.toString(mainFile.getId()) : "";
        return documentServerUrl + "/save?fileId=" + fileId;
    }

    public String getHistoryUrl()
    {
        return documentServerUrl + "/history";
    }
}
