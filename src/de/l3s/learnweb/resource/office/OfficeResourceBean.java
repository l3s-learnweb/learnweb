package de.l3s.learnweb.resource.office;

import java.io.Serializable;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.util.Beans;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.Resource.OnlineStatus;
import de.l3s.learnweb.resource.ResourceDetailBean;

@Named
@ViewScoped
public class OfficeResourceBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = -655001215017199006L;
    private static final Logger log = LogManager.getLogger(OfficeResourceBean.class);

    private int documentResourceId = 0;
    private int documentFileId = 0;
    private String documentType;
    private String documentFileType;
    private String documentUrl;
    private String documentKey;

    private String officeServerUrl;

    @PostConstruct
    public void init() {
        officeServerUrl = config().getProperty("onlyoffice_server_url");

        Resource resource = Beans.getInstance(ResourceDetailBean.class).getResource();
        fillInFileInfo(resource);
    }

    private void fillInFileInfo(Resource resource) {
        final File documentFile = resource.getMainFile();
        documentResourceId = resource.getId();
        documentFileType = FileUtility.getFileExtension(documentFile.getName());

        if (FileUtility.isSupportedFileType(documentFileType)) {
            documentUrl = documentFile.getAbsoluteUrl();
            documentType = FileUtility.getFileType(documentFile.getName());
            documentFileId = documentFile.getId();
            documentKey = FileUtility.generateRevisionId(documentFile);
        } else {
            log.error("Office type resource has unsupported extension: {}", documentFileType);
            resource.setOnlineStatus(OnlineStatus.OFFLINE);
        }
    }

    public String getDocumentFileType() {
        return documentFileType;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getDocumentUrl() {
        return documentUrl;
    }

    public String getDocumentKey() {
        return documentKey;
    }

    public String getOfficeServerUrl() {
        return officeServerUrl;
    }

    public String getCallbackUrl() {
        return config().getServerUrl() + "/save?resourceId=" + documentResourceId + "&fileId=" + documentFileId;
    }

    public String getHistoryUrl() {
        return config().getServerUrl() + "/history?resourceId=" + documentResourceId;
    }
}
