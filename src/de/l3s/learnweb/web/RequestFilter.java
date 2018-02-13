package de.l3s.learnweb.web;

import java.io.IOException;
import java.util.Date;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

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
        HttpServletRequest req = (HttpServletRequest) request;

        String IP = req.getHeader("X-FORWARDED-FOR");
        if(IP == null)
        {
            IP = request.getRemoteAddr();
        }

        if(RequestManager.instance().checkBanned(IP))
        {
            return;
        }

        String url = req.getRequestURL().toString();
        Date now = new Date();

        RequestManager.instance().recordRequest(IP, now, url);

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
