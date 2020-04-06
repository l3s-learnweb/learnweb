package de.l3s.util.bean;

import java.util.Arrays;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import de.l3s.learnweb.Learnweb;
import de.l3s.util.StringHelper;

public class BeanHelper
{
    public static ExternalContext getExternalContext()
    {
        FacesContext fc = FacesContext.getCurrentInstance();
        return fc.getExternalContext();
    }

    public static HttpServletRequest getRequest()
    {
        return (HttpServletRequest) getExternalContext().getRequest();
    }
    
    public static String getServerUrl()
    {
        try
        {
            return getServerUrl(getRequest());
        }
        catch(Exception e)
        {
            // Can't get server url. This is only expected in console mode
            return "https://learnweb.l3s.uni-hannover.de";
        }
    }

    /**
     * @return example http://learnweb.l3s.uni-hannover.de or http://localhost:8080/Learnweb-Tomcat
     */
    public static String getServerUrl(HttpServletRequest request)
    {
        if(request.getServerPort() == 80 || request.getServerPort() == 443)
            return request.getScheme() + "://" + request.getServerName() + request.getContextPath();
        else
            return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
    }

    /**
     * Returns the remote address of the current request.
     * If available it returns the IP provided through the X-FORWARDED-FOR header
     *
     * @return
     */
    public static String getIp()
    {
        return getIp(null);
    }

    /**
     * Returns the remote address of the current request.
     * If available it returns the IP provided through the X-FORWARDED-FOR header
     *
     * @param request
     * @return
     */
    public static String getIp(HttpServletRequest request)
    {
        return getIp(request, false);
    }

    /**
     *
     * @param request
     * @param ignoreForwardHeader
     * @return
     */
    public static String getIp(HttpServletRequest request, boolean ignoreForwardHeader)
    {
        if(request == null)
        {
            request = getRequest();
        }

        String ip = request.getHeader("X-FORWARDED-FOR");
        if(ip != null && !ignoreForwardHeader)
        {
            // the x forward header can contain all the ips of all rely proxies
            String[] ips = ip.split(",");
            ip = ips[0].trim();
        }

        if(ip == null)
            ip = request.getRemoteAddr();

        return ip;
    }

    /**
     *
     * @return some attributes of the current http request like url, referrer, ip etc.
     */
    public static String getRequestSummary()
    {
        return getRequestSummary(null);
    }

    /**
     *
     * @param request
     * @return some attributes of a request like url, referrer, ip etc.
     */
    public static String getRequestSummary(HttpServletRequest request)
    {
        String url = null;
        String referrer = null;
        String ip = null;
        String ipForwardedForHeader = null;
        String userAgent = null;
        Integer userId = null;
        String user = null;
        Map<String, String[]> parameters = null;

        try
        {
            if(request == null)
            {
                request = getRequest();
            }

            referrer = request.getHeader("referer");
            ip = getIp(request, true);

            ipForwardedForHeader = request.getHeader("X-FORWARDED-FOR");

            userAgent = request.getHeader("User-Agent");
            url = request.getRequestURL().toString();
            if(request.getQueryString() != null)
                url += '?' + request.getQueryString();

            HttpSession session = request.getSession(false);
            if(session != null)
                userId = (Integer) session.getAttribute("learnweb_user_id");

            if(userId == null)
                user = "not logged in";
            else
            {
                Learnweb learnweb = Learnweb.getInstance();
                if(learnweb != null)
                    user = learnweb.getUserManager().getUser(userId).toString();
            }

            parameters = request.getParameterMap();
        }
        catch(Throwable t)
        {
            // ignore
        }
        return "page: " + url + "; user: " + user + "; ip: " + ip + "; ipHeader: " + ipForwardedForHeader + "; referrer: " + referrer + "; userAgent: " + userAgent + "; parameters: " + printMap(parameters) + ";";
    }

    /**
     *
     * @return the request URI + query string
     */
    public static String getRequestURI()
    {
        String uri = null;

        try
        {
            HttpServletRequest request = getRequest();

            uri = request.getRequestURI();
            if(request.getQueryString() != null)
                uri += '?' + request.getQueryString();
        }
        catch(Throwable t)
        {
            // ignore
        }
        return uri;
    }

    private static String printMap(Map<String, String[]> map)
    {
        if(null == map)
            return "";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for(Map.Entry<String, String[]> entry : map.entrySet())
        {
            sb.append(entry.getKey());
            sb.append("=[");
            if(entry.getKey().contains("password"))
                sb.append("XXX replaced XXX");
            else
                sb.append(StringHelper.implode(Arrays.asList(entry.getValue()), "; "));
            sb.append("], ");
        }
        if(map.size() > 1) // remove last comma and whitespace
            sb.setLength(sb.length() - 2);
        sb.append("}");
        return sb.toString();
    }
}
