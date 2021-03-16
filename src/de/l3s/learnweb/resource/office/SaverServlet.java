package de.l3s.learnweb.resource.office;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

import de.l3s.learnweb.app.Learnweb;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.LogDao;
import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.resource.File.FileType;
import de.l3s.learnweb.resource.FileDao;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceDao;
import de.l3s.learnweb.resource.ResourcePreviewMaker;
import de.l3s.learnweb.resource.office.history.model.CallbackData;
import de.l3s.learnweb.resource.office.history.model.History;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserDao;
import de.l3s.util.UrlHelper;

@WebServlet(name = "saverServlet", description = "Servlet for saving edited office documents", urlPatterns = "/save", loadOnStartup = 4)
public class SaverServlet extends HttpServlet {
    private static final long serialVersionUID = 7296371511069054378L;
    private static final Logger log = LogManager.getLogger(SaverServlet.class);

    private static final String RESOURCE_ID = "resourceId";
    private static final String FILE_ID = "fileId";

    private static final String RESPONSE_OK = "{\"error\":0}";

    @Inject private LogDao logDao;
    @Inject private UserDao userDao;
    @Inject private FileDao fileDao;
    @Inject private ResourceDao resourceDao;
    @Inject private ResourceHistoryDao resourceHistoryDao;
    private ResourcePreviewMaker resourcePreviewMaker;

    @Override
    public void init() throws ServletException {
        this.resourcePreviewMaker = Learnweb.getInstance().getResourcePreviewMaker();
    }

    /**
     * Method called via callback to save edited resource.
     *
     * Requires `fileId` and `userId` parameters.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String body = null;
        Map<String, String[]> params = null;
        try {
            HttpSession session = request.getSession(true);
            body = IOUtils.toString(request.getReader());
            params = request.getParameterMap();

            Gson gson = new Gson();
            CallbackData callbackData = gson.fromJson(body, CallbackData.class);

            if (callbackData.getStatus() == 2) { // READY_FOR_SAVING
                int resourceId = Integer.parseInt(request.getParameter(RESOURCE_ID));
                int fileId = Integer.parseInt(request.getParameter(FILE_ID));

                processCallback(callbackData, resourceId, fileId, session.getId());
            }

            response.getWriter().write(RESPONSE_OK);
        } catch (NumberFormatException | IOException e) {
            log.error("Error processing callback from OnlyOffice: {} - {}", params, body, e);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void processCallback(CallbackData data, int resourceId, int fileId, String sessionId) throws IOException {
        // get the user who edited the document
        int userId = data.getUsers().get(0);
        User user = userDao.findByIdOrElseThrow(userId);

        if (fileId == 0) {
            saveNewDocument(data, resourceId, user, sessionId);
        } else {
            saveEditedDocument(data, resourceId, fileId, user, sessionId);
        }
    }

    private void saveNewDocument(CallbackData data, int resourceId, User user, String sessionId) throws IOException {
        Resource resource = resourceDao.findByIdOrElseThrow(resourceId);

        File file = new File(FileType.MAIN, resourceId, resource.getMainFile().getName(), resource.getMainFile().getMimeType());
        fileDao.save(file, UrlHelper.getInputStream(data.getUrl()));

        resource.addFile(file);
        resource.save();

        resourcePreviewMaker.processResource(resource);
        logDao.insert(user, Action.changing_office_resource, resource.getGroupId(), resource.getId(), null, sessionId);
    }

    private void saveEditedDocument(CallbackData data, int resourceId, int fileId, User user, String sessionId) throws IOException {
        // The idea of what is going here: we copy existing file, to a new file and than replace old file with new one
        // I'm not sure why it is necessary, but I guess to have permanent link to latest file (also to avoid reindex resource)
        File file = fileDao.findByIdOrElseThrow(fileId);
        File previousFile = new File(file);

        // save copy of existing file as a history file
        previousFile.setType(FileType.DOC_HISTORY);
        fileDao.save(previousFile, file.getInputStream());
        fileDao.save(file, UrlHelper.getInputStream(data.getUrl()));

        try {
            log.debug("Started history saving for resource {}", resourceId);
            data.getHistory().setUser(user.getId());
            data.getHistory().setFileId(file.getId());
            data.getHistory().setPrevFileId(previousFile.getId());
            saveDocumentHistory(data, resourceId, file);
            log.debug("History saved for resource {}", resourceId);
        } catch (IOException e) {
            log.error("Unable to store document history {}", resourceId, e);
        }

        Resource resource = resourceDao.findByIdOrElseThrow(resourceId);
        resourcePreviewMaker.processResource(resource); // create new thumbnails for the resource
        logDao.insert(user, Action.changing_office_resource, resource.getGroupId(), resource.getId(), null, sessionId);
    }

    private void saveDocumentHistory(CallbackData data, int resourceId, File file) throws IOException {
        File changesFile = new File(FileType.DOC_CHANGES, resourceId, "changes.zip", "application/zip");
        fileDao.save(changesFile, UrlHelper.getInputStream(data.getChangesUrl()));

        History history = data.getHistory();
        history.setResourceId(resourceId);
        history.setChangesFileId(changesFile.getId());
        history.setKey(FileUtility.generateRevisionId(file));
        resourceHistoryDao.save(history);
    }
}
