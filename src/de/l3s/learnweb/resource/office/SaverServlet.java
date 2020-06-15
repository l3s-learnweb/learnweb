package de.l3s.learnweb.resource.office;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.Map;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.resource.File.TYPE;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.office.history.model.CallbackData;
import de.l3s.learnweb.resource.office.history.model.History;
import de.l3s.learnweb.user.User;

@WebServlet(name = "saverServlet", description = "Servlet for saving edited office documents", urlPatterns = { "/save" }, loadOnStartup = 4)
public class SaverServlet extends HttpServlet {
    private static final long serialVersionUID = 7296371511069054378L;
    private static final Logger log = LogManager.getLogger(SaverServlet.class);

    private static final String RESOURCE_ID = "resourceId";
    private static final String FILE_ID = "fileId";

    private static final String RESPONSE_OK = "{\"error\":0}";

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
        } catch (NumberFormatException | IOException | SQLException e) {
            log.error("Error processing callback from OnlyOffice: {} - {}", params, body, e);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void processCallback(CallbackData data, int resourceId, int fileId, String sessionId) throws SQLException, IOException {
        Learnweb learnweb = Learnweb.getInstance();

        // get the user who edited the document
        int userId = data.getUsers().get(0);
        User user = learnweb.getUserManager().getUser(userId);

        if (fileId == 0) {
            saveNewDocument(learnweb, data, resourceId, user, sessionId);
        } else {
            saveEditedDocument(learnweb, data, fileId, user, sessionId);
        }
    }

    private void saveNewDocument(Learnweb learnweb, CallbackData data, int resourceId, User user, String sessionId) throws SQLException, IOException {
        Resource resource = learnweb.getResourceManager().getResource(resourceId);

        File file = new File();
        file.setResourceId(resource.getId());
        file.setType(TYPE.FILE_MAIN);
        file.setName(resource.getFileName());
        file.setMimeType(resource.getFormat());
        learnweb.getFileManager().save(file, getInputStream(data.getUrl()));

        resource.setUrl(file.getUrl());
        resource.save();

        learnweb.getResourcePreviewMaker().processResource(resource);
        learnweb.getLogManager().log(user, Action.changing_office_resource, resource.getGroupId(), resource.getId(), null, sessionId);
    }

    private void saveEditedDocument(Learnweb learnweb, CallbackData data, int fileId, User user, String sessionId) throws SQLException, IOException {
        // The idea of what is going here: we copy existing file, to a new file and than replace old file with new one
        // I'm not sure why it is necessary, but I guess to have permanent link to latest file (also to avoid reindex resource)
        File file = learnweb.getFileManager().getFileById(fileId);
        File previousFile = learnweb.getFileManager().copy(file);

        // save copy of existing file as a history file
        previousFile.setType(TYPE.HISTORY_FILE);
        learnweb.getFileManager().save(previousFile, file.getInputStream());

        file.setLastModified(null); // the correct value will be set on save
        learnweb.getFileManager().save(file, getInputStream(data.getUrl()));

        try {
            log.debug("Started history saving for resource {}", file.getResourceId());
            data.getHistory().setUser(user.getId());
            data.getHistory().setFileId(file.getId());
            data.getHistory().setPrevFileId(previousFile.getId());
            saveDocumentHistory(learnweb, data, file);
            log.debug("History saved for resource {}", file.getResourceId());
        } catch (IOException | SQLException e) {
            log.error("Unable to store document history {}", file.getResourceId(), e);
        }

        Resource resource = learnweb.getResourceManager().getResource(file.getResourceId());
        learnweb.getResourcePreviewMaker().processResource(resource); // create new thumbnails for the resource
        learnweb.getLogManager().log(user, Action.changing_office_resource, resource.getGroupId(), resource.getId(), null, sessionId);
    }

    private void saveDocumentHistory(Learnweb learnweb, CallbackData data, File file) throws IOException, SQLException {
        File changesFile = new File();
        changesFile.setResourceId(file.getResourceId());
        changesFile.setType(TYPE.CHANGES);
        changesFile.setName("changes.zip");
        changesFile.setMimeType("zip");
        learnweb.getFileManager().save(changesFile, getInputStream(data.getChangesUrl()));

        History history = data.getHistory();
        history.setResourceId(file.getResourceId());
        history.setChangesFileId(changesFile.getId());
        history.setKey(FileUtility.generateRevisionId(file));
        learnweb.getHistoryManager().saveHistory(history);
    }

    private InputStream getInputStream(String strUrl) throws IOException {
        URL url = new URL(strUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        return connection.getInputStream();
    }
}
