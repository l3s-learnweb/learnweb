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
        officeServerUrl = StringUtils.removeEnd(getLearnweb().getConfigProvider().getProperty("onlyoffice_server_url"), "/");

        Resource resource = Beans.getInstance(ResourceDetailBean.class).getResource();
        fillInFileInfo(resource);
    }

    private void fillInFileInfo(Resource resource) {
        documentResourceId = resource.getId();
        documentFileType = FileUtility.getFileExtension(resource.getFileName());

        if (FileUtility.isSupportedFileType(documentFileType)) {
            documentUrl = resource.getFileUrl();
            documentType = FileUtility.getFileType(resource.getFileName());

            File mainFile = resource.getMainFile();
            if (mainFile != null) {
                documentFileId = mainFile.getId();
                documentKey = FileUtility.generateRevisionId(mainFile);
            }
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
        return getLearnweb().getConfigProvider().getServerUrl() + "/save?resourceId=" + documentResourceId + "&fileId=" + documentFileId;
    }

    public String getHistoryUrl() {
        return getLearnweb().getConfigProvider().getServerUrl() + "/history?resourceId=" + documentResourceId;
    }
}
