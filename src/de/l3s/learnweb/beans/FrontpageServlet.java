package de.l3s.learnweb.beans;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.Learnweb.SERVICE;

/**
 * Redirects users to the Learnweb or ArchiveWeb Frontpage depending of the used domain
 *
 */
public class FrontpageServlet extends HttpServlet
{
    private final static long serialVersionUID = 7083477034183456614L;
    private final static Logger log = Logger.getLogger(FrontpageServlet.class);

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
        // forward request to homepage depending on Learnweb installation
        SERVICE service = null;
        try
        {
            Learnweb learnweb = Learnweb.getInstance();
            service = learnweb.getService();
        }
        catch(Exception e)
        {
            log.error("unhandled error", e);
        }

        boolean archiveWebRequest = isArchiveWebRequest(request);

        String folder;//request.getContextPath();

        if(service == SERVICE.AMA)
            folder = "/ama/";
        else if(archiveWebRequest)
            folder = "/aw/";
        else
            folder = "/lw/";

        // redirect to HTTPS; this is only a temporary solution until we redirect all requests to https
        if(!Learnweb.isInDevelopmentMode() && !archiveWebRequest && request.getScheme().equals("http"))
        {
            String server;

            switch(service)
            {
            case AMA:
                server = "https://network.ama-academy.eu";
            default:
                server = "https://learnweb.l3s.uni-hannover.de";
            }

            response.sendRedirect(server + folder);
            return;
        }

        try
        {
            RequestDispatcher dispatcher = getServletContext().getRequestDispatcher(folder);
            dispatcher.forward(request, response);
        }
        catch(Exception e)
        {
            log.error("unhandled error", e);
            response.setStatus(500);
        }
    }

}
