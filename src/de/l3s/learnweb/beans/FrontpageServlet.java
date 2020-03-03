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

/**
 * Redirects users to HTTPS if not run locally
 *
 */
public class FrontpageServlet extends HttpServlet
{
    private static final long serialVersionUID = 7083477034183456614L;
    private static final Logger log = Logger.getLogger(FrontpageServlet.class);

    public FrontpageServlet()
    {
    }

    @Override
    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {

        String contextPath = request.getContextPath();
        String folder = "/lw/";

        // redirect to HTTPS if Learnweb is not run locally
        if(!Learnweb.isInDevelopmentMode() && request.getScheme().equals("http"))
        {
            String server = "https://learnweb.l3s.uni-hannover.de";

            response.sendRedirect(server + contextPath + folder);
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
