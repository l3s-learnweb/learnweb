package de.l3s.util;

import java.net.InetAddress;
import java.util.Comparator;

import jakarta.faces.model.SelectItem;

import org.apache.commons.lang3.StringUtils;

public final class Misc {
    public static final Comparator<SelectItem> SELECT_ITEM_LABEL_COMPARATOR = Comparator.comparing(SelectItem::getLabel);

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * @return Username and Ip/Hostname of the current system
     */
    public static String getSystemDescription() {
        String systemUser = null;
        String systemHostname = null;
        String systemHostAddress = null;
        try {
            systemUser = System.getProperty("user.name"); // platform independent

            InetAddress localMachine = InetAddress.getLocalHost();
            systemHostname = localMachine.getHostName();
            systemHostAddress = localMachine.getHostAddress();
        } catch (Throwable ignored) {
        }

        StringBuilder sb = new StringBuilder();

        if (StringUtils.isNotEmpty(systemUser)) {
            sb.append("user: ").append(systemUser).append("; ");
        }

        if (StringUtils.isNotEmpty(systemHostname)) {
            sb.append("HostName: ").append(systemHostname).append("; ");
        }

        if (StringUtils.isNotEmpty(systemHostAddress)) {
            sb.append("HostAddress: ").append(systemHostAddress).append(";");
        }

        return sb.toString();
    }

}
