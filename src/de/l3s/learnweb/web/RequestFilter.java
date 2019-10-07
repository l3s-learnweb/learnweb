package de.l3s.learnweb.web;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.google.common.net.InetAddresses;

import de.l3s.learnweb.Learnweb;
import de.l3s.learnweb.user.loginProtection.ProtectionManager;
import de.l3s.util.bean.BeanHelper;

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
    private ProtectionManager protectionManager;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
    {
        request.setCharacterEncoding("UTF-8");
        if(requestManager != null && protectionManager != null)
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

                if(protectionManager.isBanned(ip))
                {
                    String path = req.getRequestURI().substring(req.getContextPath().length());

                    // block requests except for some special pages and folders
                    if(!path.equals("/lw/error-blocked.jsf") && !path.startsWith("/javax.faces.resource/") && !path.startsWith("/resources/"))
                    {
                        HttpServletResponse httpResponse = (HttpServletResponse) response;
                        httpResponse.sendRedirect(request.getServletContext().getContextPath() + "/lw/error-blocked.jsf");
                        httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        return;
                    }
                }
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

        try
        {
            Learnweb learnweb = Learnweb.createInstance(context);
            requestManager = learnweb.getRequestManager();
            protectionManager = learnweb.getProtectionManager();
        }
        catch(Exception e)
        {
            log.fatal("request filter not initialized ", e);
        }
    }

}
