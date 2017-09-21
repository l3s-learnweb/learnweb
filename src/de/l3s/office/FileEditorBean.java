package de.l3s.office;

import java.io.Serializable;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import de.l3s.learnweb.File;
import de.l3s.learnweb.File.TYPE;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.beans.ApplicationBean;

@ManagedBean
@ViewScoped
public class FileEditorBean extends ApplicationBean implements Serializable
{
    private static final long serialVersionUID = -655001215017199006L;

    private Resource file;

    private File mainFile;

    private String filesExtension;

    private String fileType;

    private String fullFilesUrl;

    private String key;

    public FileEditorBean()
    {

    }

    public void fillInFileInfo(Resource resource)
    {
        file = resource;
        if(file != null)
            mainFile = file.getFile(TYPE.FILE_MAIN);
        setFullFilesUrl();
        setKey();
        setFileType(FileUtility.getFileType(file.getFileName()));
        setFilesExtension(FileUtility.getFileExtension(resource.getFileName()).replace(".", ""));
    }

    public void setKey()
    {
        key = FileUtility.generateRevisionId(FileUtility.getInfoForKey(mainFile));
    }

    public String getCallbackUrl()
    {
        return getServerUrl() + "/save" + "?fileId=" + getMainFileId();
    }

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

    private String getMainFileId()
    {
        return mainFile != null ? Integer.toString(mainFile.getId()) : "";
    }

    public Resource getFile()
    {
        return file;
    }

    public void setFile(Resource file)
    {
        this.file = file;
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
        String url = file.getFileUrl().contains(getServerUrl()) ? file.getFileUrl() : getServerUrlWithoutContextPath() + file.getFileUrl();
        fullFilesUrl = url;
    }

    public String getFullFilesUrl()
    {
        return fullFilesUrl;
    }

    public String getKey()
    {
        return key;
    }

}
