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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.util.Faces;

import de.l3s.util.UrlHelper;

@Named("config")
@ApplicationScoped
public class ConfigProvider implements Serializable {
    private static final long serialVersionUID = 8999792363825397979L;
    private static final Logger log = LogManager.getLogger(ConfigProvider.class);

    private final Properties properties = new Properties();

    private final boolean autoServerUrl;
    private String serverUrl;

    private File fileManagerFolder;
    private Boolean maintenanceMode = false;
    private Boolean developmentMode;

    public ConfigProvider() {
        loadProperties();
        loadEnvironmentVariables();

        // load server URL from config file or guess it
        String serverUrl = properties.getProperty("server_url");
        autoServerUrl = serverUrl == null || "auto".equalsIgnoreCase(serverUrl);

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

            InputStream testProperties = getClass().getClassLoader().getResourceAsStream("de/l3s/learnweb/config/learnweb_test.properties");
            if (testProperties != null) {
                properties.load(testProperties);
                log.warn("Test properties loaded.");
            }
        } catch (IOException e) {
            log.error("Unable to load properties file(s)", e);
        }
    }

    private void loadEnvironmentVariables() {
        try {
            Map<String, String> env = System.getenv();
            env.forEach((key, value) -> {
                if (key.startsWith("LEARNWEB_")) {
                    String propKey = key.substring(9).toLowerCase(Locale.ROOT);
                    log.debug("Found environment variable {}({}): {}", key, propKey, value);
                    properties.setProperty(propKey, value);
                }
            });
        } catch (Exception e) {
            log.error("Unable to load environment variables", e);
        }
    }

    public String getProperty(final String key) {
        return properties.getProperty(key);
    }

    public String getProperty(final String key, final String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * @return Returns the servername + contextPath. For the default installation this is: https://learnweb.l3s.uni-hannover.de
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
        if (serverUrl.startsWith("http://") && "true".equalsIgnoreCase(properties.getProperty("force_https", "false"))) {
            log.info("Forcing HTTPS schema.");
            serverUrl = "https://" + serverUrl.substring(7);
        }

        this.serverUrl = serverUrl;
        log.info("Server url updated: {}", serverUrl);
    }

    public boolean isRootEnvironment() {
        return "https://learnweb.l3s.uni-hannover.de".equals(getServerUrl());
    }

    public File getFileManagerFolder() {
        if (fileManagerFolder == null) {
            fileManagerFolder = new File(properties.getProperty("file_manager_folder"));
            validateFolder(fileManagerFolder);
        }
        return fileManagerFolder;
    }

    public boolean isMaintenanceMode() {
        return maintenanceMode;
    }

    public void setMaintenanceMode(boolean maintenanceMode) {
        this.maintenanceMode = maintenanceMode;
    }

    public boolean isDevelopmentMode() {
        if (developmentMode == null) {
            developmentMode = Faces.isDevelopment();
        }
        return developmentMode;
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
