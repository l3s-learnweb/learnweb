package de.l3s.learnweb.beans;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Redirects users to the Learnweb or ArchiveWeb Frontpage depending of the used domain
 * 
 */
public class FrontpageServlet extends HttpServlet
{
    private final static long serialVersionUID = 7083477034183456614L;

    public FrontpageServlet()
    {
    }

    @Override
    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);
    }

    public static boolean isArchiveWebRequest(HttpServletRequest request)
    {
        String serverName = request.getServerName();

        return serverName.equals("archiveweb.l3s.uni-hannover.de") || request.getServerName().equals("archiveweb.dev");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        try
        {
            String url = request.getContextPath();

            if(isArchiveWebRequest(request))
                url += "/aw/";
            else
                url += "/lw/";

            response.sendRedirect(url);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            response.setStatus(500);
        }
    }

}
