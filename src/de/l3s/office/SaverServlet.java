package de.l3s.office;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.Date;
import java.util.Scanner;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.google.gson.Gson;

import de.l3s.learnweb.File;
import de.l3s.learnweb.File.TYPE;
import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.LogEntry.Action;
import de.l3s.learnweb.Resource;
import de.l3s.learnweb.User;
import de.l3s.office.history.model.Change;
import de.l3s.office.history.model.History;
import de.l3s.office.history.model.OfficeUser;
import de.l3s.office.history.model.SavingInfo;

/**
 * Servlet Class
 *
 * @web.servlet name="saverServlet" display-name="Simple SaverServlet"
 *              description="Servlet for saving edited documents"
 * @web.servlet-mapping url-pattern="/save"
 */

public class SaverServlet extends HttpServlet
{
    private static final long serialVersionUID = 7296371511069054378L;

    private static final String FILE_ID = "fileId";

    private static final String USER_ID = "userId";

    private final static Logger log = Logger.getLogger(SaverServlet.class);

    private static final String ERROR_0 = "{\"error\":0}";

    private static final String DELIMITER = "\\A";

    private static Learnweb learnweb;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException
    {
        HttpSession session = request.getSession(true);
        try(PrintWriter writer = response.getWriter())
        {
            String fileId = request.getParameter(FILE_ID);
            String userId = request.getParameter(USER_ID);
            try(Scanner scanner = new Scanner(request.getInputStream()))
            {
                scanner.useDelimiter(DELIMITER);
                String body = scanner.hasNext() ? scanner.next() : StringUtils.EMPTY;
                Gson gson = new Gson();
                SavingInfo savingInfo = gson.fromJson(body, SavingInfo.class);
                parseResponse(savingInfo, fileId, userId, session.getId());
            }
            writer.write(ERROR_0);
        }
        catch(IOException e)
        {
            log.error(e);
        }
    }

    private void parseResponse(SavingInfo info, String fileId, String userId, String sessionId)
    {
        try
        {
            log.info("Document " + fileId + " status : " + info.getStatus());
            if(info.getStatus() == DocumentStatus.READY_FOR_SAVING.getStatus())
            {
                learnweb = Learnweb.getInstance();
                File file = learnweb.getFileManager().getFileById(Integer.parseInt(fileId));
                File previousVersionFile = learnweb.getFileManager().copy(file);
                file.setLastModified(new Date());
                Resource resource = learnweb.getResourceManager().getResource(file.getResourceId());
                URL url = new URL(info.getUrl());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = connection.getInputStream();
                learnweb.getFileManager().save(previousVersionFile, file.getInputStream());
                learnweb.getFileManager().save(file, inputStream);
                resource.setUrl(file.getUrl());
                learnweb.getResourcePreviewMaker().processResource(resource);
                log.info("Started history saving for resourceId = " + file.getResourceId());
                previousVersionFile.setType(TYPE.HISTORY_FILE);
                log.info("History is saved resourceId = " + file.getResourceId());
                createResourceHistory(info, previousVersionFile, file.getResourceId(), Integer.parseInt(userId));
                User learnwebUser = new User();
                learnwebUser.setId(Integer.valueOf(userId));
                learnweb.log(learnwebUser, Action.changing_resource, resource.getGroupId(), resource.getId(), null, sessionId, (int) System.currentTimeMillis());
            }
        }
        catch(NumberFormatException e)
        {
            log.error(e);
        }
        catch(SQLException | IOException e)
        {
            log.error(e);
        }

    }

    private void createResourceHistory(SavingInfo info, File previosVersion, int resourceId, int userId) throws IOException, SQLException
    {
        History history = new History();
        history.setResourceId(resourceId);
        OfficeUser officeUser = new OfficeUser();
        officeUser.setId(userId);
        history.setUser(officeUser);
        history.setCreated(info.getLastSave().replace('T', ' ').substring(0, info.getLastSave().indexOf('.')));
        history.setPreviousVersionFileId(previosVersion.getId());
        history.setServerVersion(info.getHistory().getServerVersion());
        history.setKey(info.getKey());
        File changesFile = new File();
        changesFile.setType(TYPE.CHANGES);
        changesFile.setName("changes.zip");
        changesFile.setMimeType("zip");
        changesFile.setResourceId(resourceId);
        URL url = new URL(info.getChangesurl());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        InputStream inputStream = connection.getInputStream();
        learnweb.getFileManager().save(changesFile, inputStream);
        history.setChangesFileId(changesFile.getId());
        learnweb.getHistoryManager().saveHistory(history);
        for(Change change : info.getHistory().getChanges())
        {
            Change fileChange = new Change();
            OfficeUser user = new OfficeUser();
            user.setId(change.getUser().getId());
            fileChange.setUser(user);
            fileChange.setHistoryId(history.getId());
            fileChange.setCreated(change.getCreated());
            learnweb.getHistoryManager().saveChange(fileChange);
            history.addChange(fileChange);
        }
    }

}
