package de.l3s.util;

import java.util.Arrays;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import de.l3s.learnweb.Learnweb;

public class BeanHelper
{
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
        if(request == null)
        {
            ExternalContext ext = FacesContext.getCurrentInstance().getExternalContext();
            request = (HttpServletRequest) ext.getRequest();
        }

        String ip = request.getHeader("X-FORWARDED-FOR");
        if(ip != null)
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
        String userAgent = null;
        Integer userId = null;
        String user = null;
        Map<String, String[]> parameters = null;

        try
        {
            if(request == null)
            {
                ExternalContext ext = FacesContext.getCurrentInstance().getExternalContext();
                request = (HttpServletRequest) ext.getRequest();
            }

            referrer = request.getHeader("referer");
            ip = getIp(request);

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
        return "page: " + url + "; user: " + user + "; ip: " + ip + "; referrer: " + referrer + "; userAgent: " + userAgent + "; parameters: " + printMap(parameters) + ";";
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
            sb.append(StringHelper.implode(Arrays.asList(entry.getValue()), "; "));
            sb.append("], ");
        }
        if(map.size() > 1) // remove last comma and whitespace
            sb.setLength(sb.length() - 2);
        sb.append("}");
        return sb.toString();
    }
}
