package de.l3s.learnweb.resource;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.beans.BeanAssert;
import de.l3s.learnweb.exceptions.HttpException;
import de.l3s.learnweb.logging.Action;
import de.l3s.learnweb.logging.LogDao;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserBean;
import de.l3s.util.bean.BeanHelper;

/**
 * Servlet for Streaming Files to the Clients Browser.
 */
@WebServlet(name = "FileDownloadServlet", urlPatterns = "/file/*", loadOnStartup = 2)
public class FileDownloadServlet extends DownloadServlet {
    private static final long serialVersionUID = 7083477094183456614L;
    private static final Logger log = LogManager.getLogger(FileDownloadServlet.class);

    private static final String URL_PATTERN = "/file/";

    @Inject
    private FileDao fileDao;

    @Inject
    private ResourceDao resourceDao;

    @Inject
    private UserBean userBean;

    @Inject
    private LogDao logDao;

    /**
     * Process the actual request.
     *
     * @param request The request to be processed.
     * @param response The response to be created.
     * @param content Whether the request body should be written (GET) or not (HEAD).
     * @throws IOException If something fails at I/O level.
     */
    @Override
    protected void processRequest(HttpServletRequest request, HttpServletResponse response, boolean content) throws IOException {
        try {
            String requestURI = request.getRequestURI();
            // remove the servlet's urlPattern
            requestURI = requestURI.substring(requestURI.indexOf(URL_PATTERN) + URL_PATTERN.length());

            String[] partsURI = requestURI.split("/");
            if (partsURI.length != 3 || !NumberUtils.isCreatable(partsURI[0]) || !NumberUtils.isCreatable(partsURI[1]) || StringUtils.isBlank(partsURI[1])) {
                String referrer = request.getHeader("referer");

                // only log the error if the referrer is uni-hannover.de. Otherwise we have no chance to fix the link
                Level logLevel = StringUtils.contains(referrer, "uni-hannover.de") ? Level.ERROR : Level.WARN;
                log.log(logLevel, "Invalid download URL: {}; {}", requestURI, BeanHelper.getRequestSummary(request));

                throw new HttpException(HttpServletResponse.SC_BAD_REQUEST);
            }

            int resourceId = NumberUtils.toInt(partsURI[0]);
            int fileId = NumberUtils.toInt(partsURI[1]);
            String fileName = partsURI[2];

            // Check && retrieve file
            File file = fileDao.findByIdOrElseThrow(fileId);
            // Check && retrieve resource
            Resource resource = resourceDao.findByIdOrElseThrow(resourceId);
            // Get user from session
            User user = userBean.getUser();

            BeanAssert.validate(file.getName().equals(fileName)); // validate file name
            BeanAssert.validate(resource.getFiles().containsValue(file)); // validate the file belongs to the resource
            BeanAssert.hasPermission(resource.canViewResource(user)); // validate user has access to the resource

            if (file.getType() == File.FileType.MAIN) { // log download of main file
                if (null != user) {
                    HttpSession session = request.getSession(true);
                    logDao.insert(user, Action.downloading, resource.getGroupId(), resourceId, Integer.toString(file.getId()), session.getId());
                }
            }

            sendFile(request, response, file, content);
        } catch (HttpException e) {
            // Happens when file is not found or request URL is invalid, for humans
            response.sendError(e.getStatus());
        } catch (Exception e) {
            log.fatal("Unexpected error in download servlet {}", request.getRequestURI(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
