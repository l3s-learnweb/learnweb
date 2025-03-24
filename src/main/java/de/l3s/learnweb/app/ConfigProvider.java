package de.l3s.learnweb.app;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.util.Faces;
import org.omnifaces.util.Servlets;

import de.l3s.util.UrlHelper;
import de.l3s.util.bean.BeanHelper;
import io.sentry.Sentry;

@Named("config")
@ApplicationScoped
public class ConfigProvider implements Serializable {
    @Serial
    private static final long serialVersionUID = 8999792363825397979L;
    private static final Logger log = LogManager.getLogger(ConfigProvider.class);
    private static final String PROP_KEY_PREFIX = "learnweb_";

    /**
     * All the application configuration stored here.
     */
    private final Properties properties = new Properties();

    /**
     * A version of the application from pom.xml (extracted from web.xml, maven should put it there on build).
     */
    private String version;

    /**
     * An environment of the application, based on the configuration used.
     */
    private String environment;

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
    private boolean servlet;

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

    @SuppressWarnings("this-escape")
    public ConfigProvider(final boolean servlet) {
        loadProperties();
        loadEnvironmentVariables();

        this.servlet = servlet;
        if (servlet) {
            try {
                loadJndiVariables();

                contextPath = Servlets.getContext().getContextPath();
                log.info("Found servlet context: '{}'", contextPath);
                replaceVariablesContext(contextPath);
            } catch (NamingException e) {
                log.error("Unable to load JNDI variables", e);
                this.servlet = false;
            }
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
                log.info("Server URL set to {} (from config)", serverUrl);
            } else {
                throw new DeploymentException("Server url should include schema!");
            }
        }

        if (StringUtils.isNotEmpty(properties.getProperty("sentry_dsn"))) {
            Sentry.init(options -> {
                options.setDsn(properties.getProperty("sentry_dsn"));
                options.setEnvironment(getEnvironment());
                options.addInAppInclude("de.l3s");
                if (!isDevelopment()) {
                    options.setRelease("learnweb@" + getVersion());
                }
                log.info("Sentry initialized with environment '{}' and release '{}'", options.getEnvironment(), options.getRelease());
            });
        }
    }

    private void loadProperties() {
        try (InputStream defaultProperties = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            Properties prop = new Properties();
            prop.load(defaultProperties);
            for (String propKey : prop.stringPropertyNames()) {
                setProperty(propKey, prop.getProperty(propKey));
            }
        } catch (IOException e) {
            log.error("Unable to load application properties", e);
        }

        try (InputStream localProperties = getClass().getClassLoader().getResourceAsStream(".env")) {
            if (localProperties != null) {
                Properties prop = new Properties();
                prop.load(localProperties);
                for (String propKey : prop.stringPropertyNames()) {
                    setProperty(propKey, prop.getProperty(propKey));
                }

                environment = "local";
                log.info(".env properties loaded.");
            }
        } catch (IOException e) {
            log.error("Unable to load .env properties", e);
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
                if (properties.containsKey(propKey)) {
                    log.debug("Environment variable override '{}' with value: {}", propKey, propValue);
                    setProperty(propKey, propValue);
                } else if (propKey.startsWith(PROP_KEY_PREFIX)) {
                    propKey = propKey.substring(PROP_KEY_PREFIX.length());
                    log.debug("Found environment variable {}: {} (original name {})", propKey, propValue, originalKey);
                    setProperty(propKey, propValue);
                }
            });
        } catch (Exception e) {
            log.error("Unable to load environment variables", e);
        }
    }

    private void loadJndiVariables() throws NamingException {
        String namespace = "java:comp/env/";
        InitialContext ctx = new InitialContext();
        NamingEnumeration<NameClassPair> list = ctx.list(namespace);

        while (list.hasMore()) {
            NameClassPair next = list.next();
            String propKey = next.getName().toLowerCase(Locale.ROOT);
            String namespacedKey = namespace + next.getName();
            String propValue = ctx.lookup(namespacedKey).toString();

            if (properties.containsKey(propKey)) {
                log.debug("JNDI variable override '{}' with value: {}", propKey, propValue);
                setProperty(propKey, propValue);
            } else if (propKey.startsWith(PROP_KEY_PREFIX)) {
                propKey = propKey.substring(PROP_KEY_PREFIX.length());
                log.debug("Found JNDI variable {}: {} (original name {})", propKey, propValue, namespacedKey);
                setProperty(propKey, propValue);
            }
        }

        list.close();
        ctx.close();
    }

    private void replaceVariablesContext(String contextPath) {
        if (StringUtils.isNotBlank(contextPath)) {
            String prefix = contextPath.replace("/", "") + "_";
            for (String propKey : properties.stringPropertyNames()) {
                if (propKey.startsWith(prefix)) {
                    final String newKey = propKey.substring(prefix.length());
                    log.debug("Property {} replaced by {}", newKey, propKey);
                    setProperty(newKey, properties.getProperty(propKey));
                }
            }
        } else {
            log.debug("No context path specified, skipping variable replacement");
        }
    }

    public Object setProperty(final String key, final String value) {
        if (StringUtils.isBlank(value)) {
            return properties.remove(key);
        } else {
            return properties.setProperty(key.toLowerCase(Locale.ROOT), value);
        }
    }

    public String getProperty(final String key) {
        return properties.getProperty(key);
    }

    public String getProperty(final String key, final String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public boolean getPropertyBoolean(final String key) {
        return Boolean.parseBoolean(properties.getProperty(key));
    }

    public boolean getPropertyBoolean(final String key, final boolean defaultValue) {
        return Boolean.parseBoolean(properties.getProperty(key, String.valueOf(defaultValue)));
    }

    public String getEnvironment() {
        if (environment == null) {
            if (!isDevelopment()) {
                if ("/".equals(contextPath)) {
                    return "production";
                } else if ("/dev".equals(contextPath)) {
                    return "development";
                }
            }
            environment = "local";
        }
        return environment;
    }

    public String getVersion() {
        if (version == null) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("META-INF/maven/de.l3s/learnweb/pom.properties")) {
                Properties properties = new Properties();
                properties.load(is);

                version = properties.getProperty("version");
                log.info("Learnweb version: {}", version);
            } catch (Exception e) {
                development = true;
            }
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
            try {
                development = Faces.isDevelopment();
            } catch (Exception e) {
                log.error("Unable to determine development mode", e);
                development = true;
            }
        }
        return development;
    }

    public String getAppName() {
        return getProperty("app_name", "Learnweb");
    }

    public String getSupportEmail() {
        return getProperty("app_support_email", "learnweb-support@l3s.de");
    }

    public String getCaptchaType() {
        if (StringUtils.isAnyEmpty(getCaptchaPublicKey(), getCaptchaPrivateKey())) {
            return null;
        }
        if (getCaptchaPublicKey().length() == 40) {
            return "g-recaptcha";
        }
        if (getCaptchaPublicKey().length() == 36) {
            return "h-captcha";
        }
        return null;
    }

    public String getCaptchaPublicKey() {
        return getProperty("captcha_public_key");
    }

    public String getCaptchaPrivateKey() {
        return getProperty("captcha_private_key");
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

    public List<Locale> getSupportedLocales() {
        return BeanHelper.getSupportedLocales();
    }

    public Set<Locale> getSupportedGlossaryLocales() {
        return BeanHelper.getSupportedGlossaryLocales();
    }
}
