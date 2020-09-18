package de.l3s.learnweb.hserver;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import de.l3s.learnweb.hserver.filters.CorsFilter;
import de.l3s.learnweb.hserver.filters.DummyAuthFilter;

@ApplicationPath("/hserver")
public class HServerApplication extends ResourceConfig {
    public HServerApplication() {
        property(ServerProperties.WADL_FEATURE_DISABLE, true);

        register(CorsFilter.class);
        register(DummyAuthFilter.class);

        // auto scan for providers and endpoints
        packages("de.l3s.learnweb.hserver.resources");
    }
}
