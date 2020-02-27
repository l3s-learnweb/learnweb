package de.l3s.learnweb.resource.office;

import java.io.IOException;
import java.sql.SQLException;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.google.gson.Gson;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.resource.office.history.model.History;
import de.l3s.learnweb.resource.office.history.model.HistoryData;
import de.l3s.learnweb.resource.office.history.model.HistoryInfo;

public class HistoryServlet extends HttpServlet
{

    private static final long serialVersionUID = -1782046122568142569L;

    private static final Logger logger = Logger.getLogger(HistoryServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        String resourceId = request.getParameter("resourceId");
        String json = getRefreshHistoryString(Integer.parseInt(resourceId));
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        String body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        Integer version = Integer.valueOf(request.getParameter("version"));
        Gson gson = new Gson();
        HistoryInfo info = gson.fromJson(body, HistoryInfo.class);
        HistoryData data = new HistoryData();
        if(version != null)
        {
            for(History history : info.getHistory())
            {
                if(version.equals(history.getVersion()))
                {
                    data = getHistoryDataForHistory(history);
                    break;
                }
            }
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(gson.toJson(data));
        }
    }

    private HistoryData getHistoryDataForHistory(History history)
    {
        try
        {
            return Learnweb.getInstance().getHistoryManager().getHistoryData(history.getId());
        }
        catch(SQLException e)
        {
            logger.error("HistoryData cannot be loaded for historyId = " + history.getId());
        }
        return null;
    }

    public String getRefreshHistoryString(Integer resourceId)
    {
        try
        {
            HistoryInfo info = Learnweb.getInstance().getHistoryManager().getHistoryInfo(resourceId);
            Gson gson = new Gson();
            return gson.toJson(info);
        }
        catch(SQLException e)
        {
            logger.error("History cannot be loaded for resourceId = " + resourceId);
        }
        return null;
    }

}
