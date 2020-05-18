package de.l3s.learnweb.myfaces;

import java.io.IOException;
import java.net.URL;

import javax.faces.context.FacesContext;

import org.apache.myfaces.config.MyfacesConfig;
import org.apache.myfaces.resource.ResourceHandlerSupport;
import org.apache.myfaces.resource.ResourceImpl;
import org.apache.myfaces.resource.ResourceLoader;
import org.apache.myfaces.resource.ResourceLoaderUtils;
import org.apache.myfaces.resource.ResourceMeta;

public class FixedMyFacesResource extends ResourceImpl {
    public FixedMyFacesResource(final ResourceMeta resourceMeta, final ResourceLoader resourceLoader, final ResourceHandlerSupport support, final String contentType) {
        super(resourceMeta, resourceLoader, support, contentType);
    }

    public FixedMyFacesResource(final ResourceMeta resourceMeta, final ResourceLoader resourceLoader, final ResourceHandlerSupport support, final String contentType, final URL url, final String requestPath) {
        super(resourceMeta, resourceLoader, support, contentType, url, requestPath);
    }

    protected long getLastModified(FacesContext facesContext) {
        if (MyfacesConfig.getCurrentInstance(facesContext).isResourceCacheLastModified()) {
            Long lastModified = getResourceMeta().getLastModified();
            if (lastModified == null) {
                try {
                    lastModified = getResourceLastModified(this.getURL());
                } catch (IOException e) {
                    lastModified = -1L;
                }

                getResourceMeta().setLastModified(lastModified);
            }

            return lastModified;
        }

        try {
            return getResourceLastModified(this.getURL());
        } catch (IOException e) {
            return -1;
        }
    }

    private static long getResourceLastModified(URL url) throws IOException {
        return ResourceLoaderUtils.getResourceLastModified(url.openConnection());
    }
}
