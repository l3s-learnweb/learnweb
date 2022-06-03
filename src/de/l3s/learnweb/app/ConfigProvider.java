package de.l3s.learnweb.app;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Servlets;

import de.l3s.util.UrlHelper;

@Named("config")
@ApplicationScoped
public class ConfigProvider implements Serializable {
    @Serial
    private static final long serialVersionUID = 8999792363825397979L;
    private static final Logger log = LogManager.getLogger(ConfigProvider.class);
    private static final String PROP_KEY_PREFIX = "learnweb_".toLowerCase(Locale.ROOT);

    /**
     * All the application configuration stored here.
     */
    private final Properties properties = new Properties();

    /**
     * A version of the application from pom.xml (extracted from web.xml, maven should put it there on build).
     */
    private String version;

    /**
     * Base URL of the application, contains schema and hostname without trailing slash.
     * Extracted from configuration, set manually or detected automatically.
     */
    private String baseUrl;

    /**
     * Context path is used to set cookies, and to generate absolute links.
     */
    private String contextPath;

    /**
     * Indicated whether the application is started in Servlet container with CDI or initialized manually.
     * E.g. {@code true} on Tomcat and {@code false} in tests or maintenance tasks.
     */
    private final boolean servlet;

    /**
     * Indicates whether the application is started in development mode according to jakarta.faces.PROJECT_STAGE in web.xml.
     * It is managed by Maven, {@code false} on build in `prod` profile, {@code true} otherwise. Always {@code false} when {@link #servlet} is {@code false}.
     */
    private Boolean development;

    /**
     * Used to "hide" the application content from customers during maintenance, can be changed via admin panel.
     */
    private boolean maintenance = false;

    private File fileManagerFolder;

    private transient String serverUrl;

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

            contextPath = Servlets.getContext().getContextPath();
            log.info("Found environment context: {}", contextPath);
            replaceVariablesContext(contextPath);
        } else {
            development = true;
            version = "dev";
        }

        // load server URL from config file or guess it
        String serverUrl = properties.getProperty("server_url");
        boolean autoServerUrl = serverUrl == null || "auto".equalsIgnoreCase(serverUrl);

        if (!autoServerUrl) {
            if (serverUrl.startsWith("http")) {
                setServerUrl(serverUrl, contextPath);
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
                if (propKey.startsWith(PROP_KEY_PREFIX)) {
                    propKey = propKey.substring(PROP_KEY_PREFIX.length());
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
                if (propKey.startsWith(PROP_KEY_PREFIX)) {
                    propKey = propKey.substring(PROP_KEY_PREFIX.length());
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

    private void replaceVariablesContext(String contextPath) {
        if (StringUtils.isNotBlank(contextPath)) {
            String prefix = contextPath.replace("/", "") + "_";
            for (String propKey : properties.stringPropertyNames()) {
                if (propKey.startsWith(prefix)) {
                    final String newKey = propKey.substring(prefix.length());
                    log.debug("Property {} replaced by {}", newKey, propKey);
                    properties.setProperty(newKey, properties.getProperty(propKey));
                }
            }
        } else {
            log.debug("No context path specified, skipping variable replacement");
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
     * In general, it is bad practice to use it inside application, except for generating absolute links, e.g. for emails.
     *
     * @return Returns the baseUrl + contextPath without trailing slash. For the default installation this is: https://learnweb.l3s.uni-hannover.de
     */
    public String getServerUrl() {
        if (serverUrl == null) {
            serverUrl = UrlHelper.removeTrailingSlash(getBaseUrl() + getContextPath());
        }
        return serverUrl;
    }

    public void setServerUrl(String baseUrl, String contextPath) {
        if (!isBaseUrlMissing()) {
            return; // ignore new serverUrl
        }

        baseUrl = UrlHelper.removeTrailingSlash(baseUrl);

        // enforce HTTPS on the production server
        if (baseUrl.startsWith("http://") && getPropertyBoolean("force_https")) {
            log.info("Forcing HTTPS schema.");
            baseUrl = "https://" + baseUrl.substring(7);
        }

        this.baseUrl = baseUrl;
        this.contextPath = StringUtils.isEmpty(contextPath) ? "/" : contextPath;
        log.info("Server url updated: {}", getServerUrl());
    }

    public boolean isBaseUrlMissing() {
        return baseUrl == null;
    }

    public String getBaseUrl() {
        if (baseUrl == null) {
            throw new DeploymentException("Server url requested but not set!");
        }
        return baseUrl;
    }

    public String getContextPath() {
        if (contextPath == null) {
            throw new DeploymentException("Context path requested but not set!");
        }
        return contextPath;
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

    /**
     * If started in development (also when no servlet context) or other test instance, do not schedule any jobs.
     */
    public boolean isRunScheduler() {
        return isServlet() && !isDevelopment() && "https://learnweb.l3s.uni-hannover.de".equals(getServerUrl());
    }

    public boolean isCollectSearchHistory() {
        return !isDevelopment() && "https://learnweb.l3s.uni-hannover.de".equals(getServerUrl()) || getPropertyBoolean("force_search_history");
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
