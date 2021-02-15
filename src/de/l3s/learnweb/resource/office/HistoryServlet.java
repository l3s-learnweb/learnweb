package de.l3s.learnweb.resource.office;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

import de.l3s.learnweb.resource.office.history.model.History;
import de.l3s.learnweb.resource.office.history.model.HistoryData;
import de.l3s.learnweb.resource.office.history.model.HistoryInfo;

/**
 * Servlet for getting office document history.
 */
@WebServlet(name = "HistoryServlet", urlPatterns = "/history", loadOnStartup = 5)
public class HistoryServlet extends HttpServlet {
    private static final long serialVersionUID = -1782046122568142569L;
    private static final Logger log = LogManager.getLogger(HistoryServlet.class);

    @Inject
    private ResourceHistoryDao resourceHistoryDao;

    /**
     * Method called on `onRequestHistory` and returns object which will be forwarded to `docEditor.refreshHistory`.
     *
     * Requires `resourceId` parameter.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            int resourceId = Integer.parseInt(request.getParameter("resourceId"));

            List<History> histories = resourceHistoryDao.findByResourceId(resourceId);
            HistoryInfo info = new HistoryInfo(histories);

            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            Gson gson = new Gson();
            response.getWriter().write(gson.toJson(info));
        } catch (Exception e) {
            log.error("HistoryInfo cannot be loaded for resource {}", request.getParameter("resourceId"), e);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    /**
     * Method called on `onRequestHistoryData` and returns object which will be forwarded to `docEditor.setHistoryData`
     *
     * Requires `resourceId` and `version` parameters.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            int resourceId = Integer.parseInt(request.getParameter("resourceId"));
            int version = Integer.parseInt(request.getParameter("version"));

            Gson gson = new Gson();
            Optional<HistoryData> data = resourceHistoryDao.findByResourceIdAndVersion(resourceId, version);

            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(gson.toJson(data.orElseThrow()));
        } catch (Exception e) {
            log.error("HistoryData cannot be loaded for resource {}, version {}", request.getParameter("resourceId"), request.getParameter("version"), e);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }
}
