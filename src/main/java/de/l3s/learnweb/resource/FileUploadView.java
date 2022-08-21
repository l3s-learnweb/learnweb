package de.l3s.learnweb.resource;

import java.io.IOException;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.util.Beans;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;

import de.l3s.learnweb.beans.ApplicationBean;
import de.l3s.learnweb.resource.search.solrClient.FileInspector;

@Named
@RequestScoped
public class FileUploadView extends ApplicationBean {
    private static final Logger log = LogManager.getLogger(FileUploadView.class);

    @Inject
    private FileDao fileDao;

    public void handleFileUpload(FileUploadEvent event) {
        try {
            log.debug("Handle File upload");
            UploadedFile uploadedFile = event.getFile();

            log.debug("Getting the fileInfo from uploaded file...");
            FileInspector.FileInfo info = getLearnweb().getResourceMetadataExtractor().getFileInfo(uploadedFile.getInputStream(), uploadedFile.getFileName());

            log.debug("Saving the file...");
            File file = new File(File.FileType.MAIN, info.getFileName(), info.getMimeType());
            fileDao.save(file, uploadedFile.getInputStream());

            AddResourceBean addResourceBean = Beans.getInstance(AddResourceBean.class);
            Resource res = addResourceBean.getResource().cloneResource();
            res.addFile(file);

            log.debug("Extracting metadata from the file...");
            getLearnweb().getResourceMetadataExtractor().processFileResource(res, info);
            addResourceBean.addResource(res);

            log.debug("Creating thumbnails from the file...");
            Thread createThumbnailThread = new ResourcePreviewMaker.CreateThumbnailThread(res);
            createThumbnailThread.start();
            createThumbnailThread.join(1000);
        } catch (InterruptedException | IOException e) {
            addErrorMessage(e);
        }
    }
}
