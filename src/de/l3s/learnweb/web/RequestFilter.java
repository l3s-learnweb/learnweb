package de.l3s.learnweb.web;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

/**
 * Logs incoming requests by IPs. Records IP, time and URL, then at the end of the day stores it into a log file.
 *
 * @author Kate
 *
 */
public class RequestFilter implements Filter
{

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        try
        {
            HttpServletRequest req = (HttpServletRequest) request;

            String IP = req.getHeader("X-FORWARDED-FOR");
            if(IP == null)
            {
                IP = request.getRemoteAddr();
            }

            final RequestManager requestManager = RequestManager.instance();
            //TEMPORARILY DISABLED
            //            if(requestManager.checkBanned(IP))
            //            {
            //                return;
            //            }

            String url = req.getRequestURL().toString();

            requestManager.recordRequest(IP, url);
        }
        catch(Throwable e) // makes sure that an error in request manager doesn't block the system
        {
            Logger.getLogger(RequestFilter.class).fatal("request filter error", e);
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy()
    {
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException
    {
    }

}
