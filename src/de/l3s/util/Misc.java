package de.l3s.util;

import java.util.Collection;
import java.util.Comparator;

import javax.faces.model.SelectItem;

import org.apache.commons.lang3.StringUtils;

public class Misc
{
    public static boolean nullOrEmpty(Collection collection) {
        return collection == null || collection.size() == 0;
    }

    public static void sleep(long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch(InterruptedException e)
        {
        }
    }

    /**
     *
     * @return Username and Ip/Hostname of the current system
     */
    public static String getSystemDescription()
    {
        String systemUser = null;
        String systemHostname = null;
        String systemHostAddress = null;
        try
        {
            systemUser = System.getProperty("user.name"); //platform independent

            java.net.InetAddress localMachine = java.net.InetAddress.getLocalHost();
            systemHostname = localMachine.getHostName();
            systemHostAddress = localMachine.getHostAddress();
        }
        catch(Throwable t)
        {
        }

        StringBuilder sb = new StringBuilder();

        if(StringUtils.isNotEmpty(systemUser))
            sb.append("user: ").append(systemUser).append("; ");

        if(StringUtils.isNotEmpty(systemHostname))
            sb.append("HostName: ").append(systemHostname).append("; ");

        if(StringUtils.isNotEmpty(systemHostAddress))
            sb.append("HostAddress: ").append(systemHostAddress).append(";");

        return sb.toString();
    }

    public final static Comparator<SelectItem> selectItemLabelComparator = Comparator.comparing(SelectItem::getLabel);

}
