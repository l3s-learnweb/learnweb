package de.l3s.learnweb.hserver.filters;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang3.StringUtils;

@Provider
public class CorsFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        String origin = request.getHeaderString("origin");
        String corsHeaders = request.getHeaderString("access-control-request-headers");
        response.getHeaders().add("Access-Control-Allow-Origin", StringUtils.isNotBlank(origin) ? origin : "*");
        response.getHeaders().add("Access-Control-Allow-Headers", StringUtils.isNotBlank(corsHeaders) ? corsHeaders : "*");
        response.getHeaders().add("Access-Control-Allow-Credentials", "true");
        response.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
    }
}
