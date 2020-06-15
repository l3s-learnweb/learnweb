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
public class OfficeResourceBean extends ApplicationBean implements Serializable {
    private static final long serialVersionUID = -655001215017199006L;
    private static final Logger log = LogManager.getLogger(OfficeResourceBean.class);

    private Integer documentFileId = 0;
    private String documentType;
    private String documentFileType;
    private String documentUrl;
    private String documentKey;

    private String officeServerUrl;

    @PostConstruct
    public void init() {
        officeServerUrl = StringUtils.removeEnd(getLearnweb().getProperties().getProperty("FILES.DOCSERVICE.URL.CLIENT"), "/");

        Resource resource = Beans.getInstance(ResourceDetailBean.class).getResource();
        fillInFileInfo(resource);
    }

    private void fillInFileInfo(Resource resource) {
        documentFileType = FileUtility.getFileExtension(resource.getFileName());

        if (FileUtility.canBeViewed(documentFileType)) {
            documentUrl = resource.getFileUrl();
            documentType = FileUtility.getFileType(resource.getFileName());

            File mainFile = resource.getMainFile();
            if (mainFile != null) {
                documentFileId = mainFile.getId();
                documentKey = FileUtility.generateRevisionId(mainFile);
            }
        } else {
            log.error("Office type resource has not supported extension: {}", documentFileType);
            resource.setOnlineStatus(OnlineStatus.OFFLINE);
        }
    }

    public Integer getDocumentFileId() {
        return documentFileId;
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
        return getLearnweb().getServerUrl() + "/save";
    }

    public String getHistoryUrl() {
        return getLearnweb().getServerUrl() + "/history";
    }
}
