package de.l3s.learnweb.app;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.inject.Inject;

import org.jdbi.v3.core.Handle;
import org.omnifaces.cdi.Eager;

import de.l3s.interwebj.client.InterWeb;
import de.l3s.learnweb.resource.ResourceMetadataExtractor;
import de.l3s.learnweb.resource.ResourcePreviewMaker;
import de.l3s.learnweb.resource.search.solrClient.SolrClient;

/**
 * <p>
 * All in one singleton.
 *
 * Can be used to access most of beans without injecting them. Prefer injecting if possible.
 * </p>
 *
 * <h3> Annotation used in this class: </h3>
 * <ul>
 * <li> @Eager - means the class should be eagerly instantiated, even before application is actually started
 *          https://showcase.omnifaces.org/cdi/Eager
 * <li> @ApplicationScoped - something like a singleton, means that the class should be created once per application
 *          and always stored in memory, required to have @Produces methods in it
 *          https://docs.jboss.org/weld/reference/latest-3.1/en-US/html_single/#_built_in_scopes
 * <li> @Inject - tells the container to call that constructor when instantiating the bean.
 *          The container will inject other beans into the parameters of the constructor.
 *          https://docs.jboss.org/weld/reference/latest-3.1/en-US/html_single/#_getting_our_feet_wet
 * </ul>
 */
@Eager
@ApplicationScoped
@SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
public final class Learnweb {
    public static final String SALT_1 = "ff4a9ff19306ee0407cf69d592";
    public static final String SALT_2 = "3a129713cc1b33650816d61450";

    private static Learnweb learnweb;

    private final ConfigProvider configProvider;
    private final DaoProvider daoProvider;

    private final SolrClient solrClient;
    private final InterWeb interweb;

    private final ResourcePreviewMaker resourcePreviewMaker;
    private final ResourceMetadataExtractor resourceMetadataExtractor;

    @Inject
    public Learnweb(ConfigProvider configProvider, DaoProvider daoProvider) {
        this.configProvider = configProvider;
        this.daoProvider = daoProvider;

        solrClient = new SolrClient(configProvider.getProperty("solr_server_url"));
        interweb = new InterWeb(configProvider.getProperty("interwebj_api_url"), configProvider.getProperty("interwebj_api_key"),
            configProvider.getProperty("interwebj_api_secret"));

        resourcePreviewMaker = new ResourcePreviewMaker(daoProvider.getFileDao(), configProvider);
        resourceMetadataExtractor = new ResourceMetadataExtractor(solrClient);

        if (learnweb == null) {
            learnweb = this;
        } else {
            throw new DeploymentException("Learnweb is already created!");
        }
    }

    /**
     * Additional initialization procedures can be added here.
     *
     * Because of @Eager, FacesContext is not available here!
     */
    @PostConstruct
    public void init() {

    }

    /**
     * Shutdown/close procedures can be added here.
     */
    @PreDestroy
    public void destroy() {
        daoProvider.destroy();
    }

    public DaoProvider getDaoProvider() {
        return daoProvider;
    }

    public Handle openJdbiHandle() {
        return getDaoProvider().getJdbi().open();
    }

    public ConfigProvider getConfigProvider() {
        return configProvider;
    }

    public SolrClient getSolrClient() {
        return solrClient;
    }

    public InterWeb getInterweb() {
        return interweb;
    }

    public ResourcePreviewMaker getResourcePreviewMaker() {
        return resourcePreviewMaker;
    }

    public ResourceMetadataExtractor getResourceMetadataExtractor() {
        return resourceMetadataExtractor;
    }

    public static Learnweb getInstance() {
        if (null == learnweb) {
            throw new DeploymentException("Learnweb is not initialized correctly!");
        }
        return learnweb;
    }

    public static DaoProvider dao() {
        return getInstance().getDaoProvider();
    }

    public static ConfigProvider config() {
        return getInstance().getConfigProvider();
    }

    /**
     * This method should be used, if you want to create a Learnweb instance without Servlet context.
     */
    public static Learnweb createStatic() {
        ConfigProvider configProvider = new ConfigProvider(false);
        configProvider.setServerUrl("https://learnweb.l3s.uni-hannover.de", "/");

        DaoProvider daoProvider = new DaoProvider(configProvider);
        Learnweb learnweb = new Learnweb(configProvider, daoProvider);
        return learnweb;
    }
}
