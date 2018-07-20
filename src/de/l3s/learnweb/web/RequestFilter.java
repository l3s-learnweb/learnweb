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

import com.google.common.net.InetAddresses;

import de.l3s.learnweb.Learnweb;
import de.l3s.util.BeanHelper;

/**
 * Logs incoming requests by IPs. Records IP, time and URL, then at the end of the day stores it into a log file.
 *
 * @author Kate
 *
 */
public class RequestFilter implements Filter
{
    private final static Logger log = Logger.getLogger(RequestFilter.class);

    private RequestManager requestManager;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        if(requestManager != null)
        {
            try
            {
                HttpServletRequest req = (HttpServletRequest) request;
                String ip = BeanHelper.getIp(req);

                if(!InetAddresses.isInetAddress(ip))
                {
                    log.error("Suspicious request: " + BeanHelper.getRequestSummary(req));
                    chain.doFilter(request, response);
                    return;
                }

                String url = req.getRequestURL().toString();

                requestManager.recordRequest(ip, url);
            }
            catch(Throwable e)
            {
                log.fatal("Request filter error: ", e);
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy()
    {
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        String context = filterConfig.getServletContext().getContextPath();
        //log.debug("Init RequestFilter; context = '" + context + "'");

        try
        {
            Learnweb learnweb = Learnweb.createInstance(context);
            requestManager = learnweb.getRequestManager();
        }
        catch(Exception e)
        {
            log.fatal("request filter not initialized ", e);
        }
    }

}
