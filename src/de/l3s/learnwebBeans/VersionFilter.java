package de.l3s.learnwebBeans;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import de.l3s.learnweb.beans.UtilBean;

public class VersionFilter implements Filter
{

    @Override
    public void init(FilterConfig config) throws ServletException
    {

    }

    private static boolean isArchiveWebRequest(HttpServletRequest request)
    {
	String serverName = request.getServerName();

	return serverName.equals("archiveweb.l3s.uni-hannover.de") || request.getServerName().equals("learnweb.dev");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
	if(request instanceof HttpServletRequest)
	{
	    HttpServletRequest httpRequest = ((HttpServletRequest) request);

	    String contextPath = httpRequest.getContextPath();
	    String requestURI = httpRequest.getRequestURI();

	    System.out.println(contextPath + " - " + requestURI);

	    if(requestURI.length() - contextPath.length() == 1 && requestURI.endsWith("/"))
	    {
		System.out.println("is context root");

		if(isArchiveWebRequest(httpRequest))
		    contextPath += "/aw/";
		else
		    contextPath += "/lw/";

		request.getRequestDispatcher(contextPath).forward(request, response);
		return;
	    }

	    UserBean userBean = UtilBean.getUserBean();

	    if(userBean.getActiveCourseId() == 0 && isArchiveWebRequest(httpRequest)) // session is not initialized
	    {
		userBean.setActiveCourseId(891);
		System.out.println("set course");
	    }

	    //response.sendRedirect(url);

	    /*
	       if (requestURI.startsWith("/Check_License/Dir_My_App/")) {
	            String toReplace = requestURI.substring(requestURI.indexOf("/Dir_My_App"), requestURI.lastIndexOf("/") + 1);
	            String newURI = requestURI.replace(toReplace, "?Contact_Id=");
	            req.getRequestDispatcher(newURI).forward(req, res);
	        } else {
	            chain.doFilter(req, res);
	        }
	       */
	}

	chain.doFilter(request, response);
    }

    @Override
    public void destroy()
    {
    }
}
