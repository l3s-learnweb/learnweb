package de.l3s.learnweb.myfaces;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.faces.application.ProjectStage;
import javax.faces.context.FacesContext;

import org.apache.myfaces.shared.resource.AliasResourceMetaImpl;
import org.apache.myfaces.shared.resource.ResourceHandlerSupport;
import org.apache.myfaces.shared.resource.ResourceImpl;
import org.apache.myfaces.shared.resource.ResourceLoader;
import org.apache.myfaces.shared.resource.ResourceLoaderUtils;
import org.apache.myfaces.shared.resource.ResourceMeta;

public class FixedMyFacesResource extends ResourceImpl
{
    public FixedMyFacesResource(final ResourceMeta resourceMeta, final ResourceLoader resourceLoader, final ResourceHandlerSupport support, final String contentType)
    {
        super(resourceMeta, resourceLoader, support, contentType);
    }

    public FixedMyFacesResource(final ResourceMeta resourceMeta, final ResourceLoader resourceLoader, final ResourceHandlerSupport support, final String contentType, final URL url, final String requestPath)
    {
        super(resourceMeta, resourceLoader, support, contentType, url, requestPath);
    }

    public Map<String, String> getResponseHeaders() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext.getApplication().getResourceHandler().isResourceRequest(facesContext)) {
            HashMap<String, String> headers = new HashMap<>();

            long lastModified;
            try {
                lastModified = getResourceLastModified(this.getURL());
            } catch (IOException var7) {
                lastModified = -1L;
            }

            if (this.couldResourceContainValueExpressions() && lastModified < getResourceHandlerSupport().getStartupTime()) {
                lastModified = getResourceHandlerSupport().getStartupTime();
            } else if (getResourceMeta() instanceof AliasResourceMetaImpl && lastModified < getResourceHandlerSupport().getStartupTime()) {
                lastModified = getResourceHandlerSupport().getStartupTime();
            }

            if (lastModified >= 0L) {
                headers.put("Last-Modified", ResourceLoaderUtils.formatDateHeader(lastModified));
                long expires;
                if (facesContext.isProjectStage(ProjectStage.Development)) {
                    expires = System.currentTimeMillis();
                } else {
                    expires = System.currentTimeMillis() + getResourceHandlerSupport().getMaxTimeExpires();
                }

                headers.put("Expires", ResourceLoaderUtils.formatDateHeader(expires));
            }

            return headers;
        } else {
            return Collections.emptyMap();
        }
    }

    public static long getResourceLastModified(URL url) throws IOException {
        if ("file".equals(url.getProtocol())) {
            String externalForm = URLDecoder.decode(url.toExternalForm(), StandardCharsets.UTF_8);
            File file = new File(externalForm.substring(5));
            return file.lastModified();
        } else {
            return ResourceLoaderUtils.getResourceLastModified(url.openConnection());
        }
    }

    private boolean couldResourceContainValueExpressions() {
        if (getResourceMeta().couldResourceContainValueExpressions()) {
            return true;
        } else {
            String contentType = this.getContentType();
            return "text/css".equals(contentType);
        }
    }
}
