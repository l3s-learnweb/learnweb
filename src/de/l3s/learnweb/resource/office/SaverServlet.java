package de.l3s.learnweb.resource.office;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import com.google.gson.Gson;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.resource.File.TYPE;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.office.history.model.History;
import de.l3s.learnweb.resource.office.history.model.CallbackData;
import de.l3s.learnweb.user.User;

/**
 * SaverServlet Class
 *
 * @web.servlet name="saverServlet" display-name="SaverServlet"
 *              description="Servlet for saving edited documents"
 * @web.servlet-mapping url-pattern="/save"
 */
public class SaverServlet extends HttpServlet
{
    private static final long serialVersionUID = 7296371511069054378L;
    private static final Logger log = Logger.getLogger(SaverServlet.class);

    private static final String FILE_ID = "fileId";
    private static final String USER_ID = "userId";

    private static final String ERROR_0 = "{\"error\":0}";

    /**
     * Method called via callback to save edited resource
     *
     * Requires `fileId` and `userId` parameters.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        try
        {
            HttpSession session = request.getSession(true);
            int fileId = Integer.parseInt(request.getParameter(FILE_ID));
            int userId = Integer.parseInt(request.getParameter(USER_ID));
            String body = IOUtils.toString(request.getReader());

            Gson gson = new Gson();
            CallbackData callbackData = gson.fromJson(body, CallbackData.class);

            log.debug("Document " + fileId + " status: " + callbackData.getStatus());
            if(callbackData.getStatus() == 2) // READY_FOR_SAVING
                saveEditedDocument(callbackData, fileId, userId, session.getId());

            response.getWriter().write(ERROR_0);
        }
        catch(NumberFormatException | IOException | SQLException e)
        {
            log.error("Error processing callback from OnlyOffice", e);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void saveEditedDocument(CallbackData data, int fileId, int userId, String sessionId) throws SQLException, IOException
    {
        Learnweb learnweb = Learnweb.getInstance();
        User user = learnweb.getUserManager().getUser(userId);

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
            log.debug("Started history saving for resource " + file.getResourceId());
            data.getHistory().setUser(userId);
            data.getHistory().setFileId(file.getId());
            data.getHistory().setPrevFileId(previousFile.getId());
            saveDocumentHistory(learnweb, data, file);
            log.debug("History saved for resource " + file.getResourceId());
        }
        catch(IOException | SQLException e)
        {
            log.error("Unable to store document history " + file.getResourceId(), e);
        }

        Resource resource = learnweb.getResourceManager().getResource(file.getResourceId());
        learnweb.getResourcePreviewMaker().processResource(resource); // create new thumbnails for the resource
        learnweb.getLogManager().log(user, Action.changing_office_resource, resource.getGroupId(), resource.getId(), null, sessionId);
    }

    private void saveDocumentHistory(Learnweb learnweb, CallbackData data, File file) throws IOException, SQLException
    {
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

    private InputStream getInputStream(String strUrl) throws IOException
    {
        URL url = new URL(strUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        return connection.getInputStream();
    }
}
