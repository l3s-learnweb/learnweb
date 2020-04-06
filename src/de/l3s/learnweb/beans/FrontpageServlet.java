package de.l3s.learnweb.beans;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import de.l3s.util.bean.BeanHelper;

/**
 * Redirects users to HTTPS if not run locally
 */
public class FrontpageServlet extends HttpServlet
{
    private static final long serialVersionUID = 7083477034183456614L;
    private static final Logger log = Logger.getLogger(FrontpageServlet.class);
    private static final String folder = "/lw/";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        // redirect to HTTPS if Learnweb is not run locally
        if(request.getScheme().equals("http") && request.getServerName().equals("learnweb.l3s.uni-hannover.de"))
        {
            response.sendRedirect("https://" + BeanHelper.getServerUrl(request).substring(7) + folder);
            return;
        }

        try
        {
            RequestDispatcher dispatcher = getServletContext().getRequestDispatcher(folder);
            dispatcher.forward(request, response);
        }
        catch(Exception e)
        {
            log.error("Unhandled error", e);
            response.setStatus(500);
        }
    }

}
