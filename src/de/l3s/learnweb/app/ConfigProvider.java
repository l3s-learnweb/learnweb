package de.l3s.learnweb.app;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.DeploymentException;
import javax.inject.Named;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.util.Faces;

import de.l3s.util.UrlHelper;

@Named("config")
@ApplicationScoped
public class ConfigProvider implements Serializable {
    private static final long serialVersionUID = 8999792363825397979L;
    private static final Logger log = LogManager.getLogger(ConfigProvider.class);

    /**
     * All the application configuration stored here.
     */
    private final Properties properties = new Properties();

    /**
     * An version of the application from pom.xml (extracted from web.xml, maven should put it there on build).
     */
    private String version;

    /**
     * A server url, extracted from configuration, set manually or detected automatically.
     * In general, it is bad practice to use it inside application, except for generating absolute links, e.g. for emails.
     */
    private String serverUrl;

    /**
     * Indicated whether the application is started in Servlet container with CDI or initialized manually.
     * E.g. {@code true} on Tomcat and {@code false} in tests or maintenance tasks.
     */
    private final boolean servlet;

    /**
     * Indicates whether the application is started in development mode according to javax.faces.PROJECT_STAGE in web.xml.
     * It is managed by Maven, {@code false} on build in `prod` profile, {@code true} otherwise. Always {@code false} when {@link #servlet} is {@code false}.
     */
    private Boolean development;

    /**
     * Used to "hide" the application content from customers during maintenance, can be changed via admin panel.
     */
    private boolean maintenance = false;

    private File fileManagerFolder;

    @Deprecated
    public ConfigProvider() {
        this(true);
    }

    public ConfigProvider(final boolean servlet) {
        loadProperties();
        loadEnvironmentVariables();

        this.servlet = servlet;
        if (servlet) {
            loadJndiVariables();
        } else {
            development = true;
            version = "dev";
        }

        // load server URL from config file or guess it
        String serverUrl = properties.getProperty("server_url");
        boolean autoServerUrl = serverUrl == null || "auto".equalsIgnoreCase(serverUrl);

        if (!autoServerUrl) {
            if (serverUrl.startsWith("http")) {
                setServerUrl(serverUrl);
            } else {
                throw new DeploymentException("Server url should include schema!");
            }
        }
    }

    private void loadProperties() {
        try {
            InputStream defaultProperties = getClass().getClassLoader().getResourceAsStream("de/l3s/learnweb/config/learnweb.properties");
            properties.load(defaultProperties);

            InputStream localProperties = getClass().getClassLoader().getResourceAsStream("de/l3s/learnweb/config/learnweb_local.properties");
            if (localProperties != null) {
                properties.load(localProperties);
                log.warn("Local properties loaded.");
            }
        } catch (IOException e) {
            log.error("Unable to load properties file(s)", e);
        }
    }


    /**
     * Because Tomcat always removes per-application context config file, we have to add context-prefix.
     * https://stackoverflow.com/questions/4032773/why-does-tomcat-replace-context-xml-on-redeploy
     */
    private void loadEnvironmentVariables() {
        try {
            Map<String, String> env = System.getenv();
            env.forEach((originalKey, propValue) -> {
                String propKey = originalKey.toLowerCase(Locale.ROOT);
                if (propKey.startsWith("learnweb_")) {
                    propKey = propKey.substring(9);
                    log.debug("Found environment variable {}: {} (original name {})", propKey, propValue, originalKey);
                    properties.setProperty(propKey, propValue);
                }
            });
        } catch (Exception e) {
            log.error("Unable to load environment variables", e);
        }
    }

    private void loadJndiVariables() {
        try {
            String namespace = "java:comp/env/";
            InitialContext ctx = new InitialContext();
            NamingEnumeration<NameClassPair> list = ctx.list(namespace);

            while (list.hasMore()) {
                NameClassPair next = list.next();
                String namespacedKey = namespace + next.getName();
                String propKey = next.getName().toLowerCase(Locale.ROOT);
                if (propKey.startsWith("learnweb_")) {
                    propKey = propKey.substring(9);
                    String propValue = ctx.lookup(namespacedKey).toString();
                    log.debug("Found JNDI variable {}: {} (original name {})", propKey, propValue, namespacedKey);
                    properties.setProperty(propKey, propValue);
                }
            }

            list.close();
            ctx.close();
        } catch (Exception e) {
            log.error("Unable to load JNDI variables", e);
        }
    }

    public Object setProperty(final String key, final String value) {
        return properties.setProperty(key, value);
    }

    public String getProperty(final String key) {
        return properties.getProperty(key);
    }

    public boolean getPropertyBoolean(final String key) {
        return "true".equalsIgnoreCase(properties.getProperty(key));
    }

    public String getVersion() {
        if (version == null) {
            version = Faces.getInitParameterOrDefault("project.version", "dev");
        }
        return version;
    }

    /**
     * @return Returns the servername + contextPath without trailing slash.
     * For the default installation this is: https://learnweb.l3s.uni-hannover.de
     */
    public String getServerUrl() {
        if (serverUrl == null) {
            throw new DeploymentException("Server url requested but not set!");
        }
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        if (this.serverUrl != null) {
            return; // ignore new serverUrl
        }

        serverUrl = UrlHelper.removeTrailingSlash(serverUrl);

        // enforce HTTPS on the production server
        if (serverUrl.startsWith("http://") && getPropertyBoolean("force_https")) {
            log.info("Forcing HTTPS schema.");
            serverUrl = "https://" + serverUrl.substring(7);
        }

        this.serverUrl = serverUrl;
        log.info("Server url updated: {}", serverUrl);
    }

    public boolean isServerUrlMissing() {
        return serverUrl == null;
    }

    public boolean isServlet() {
        return servlet;
    }

    public boolean isDevelopment() {
        if (development == null) {
            development = Faces.isDevelopment();
        }
        return development;
    }

    public boolean isMaintenance() {
        return maintenance;
    }

    public void setMaintenance(boolean maintenance) {
        this.maintenance = maintenance;
    }

    public File getFileManagerFolder() {
        if (fileManagerFolder == null) {
            fileManagerFolder = new File(properties.getProperty("file_manager_folder"));
            validateFolder(fileManagerFolder);
        }
        return fileManagerFolder;
    }

    private static void validateFolder(File folder) {
        if (!folder.exists()) {
            throw new DeploymentException("Folder '" + folder.getPath() + "' does not exist.");
        } else if (!folder.canRead()) {
            throw new DeploymentException("Can't read from folder '" + folder.getPath() + "'");
        } else if (!folder.canWrite()) {
            throw new DeploymentException("Can't write into folder '" + folder.getPath() + "'");
        }
    }
}
