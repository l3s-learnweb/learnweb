package de.l3s.learnweb.myfaces;

import java.util.List;

import javax.faces.application.Resource;
import javax.faces.context.FacesContext;

import org.apache.myfaces.application.ResourceHandlerImpl;
import org.apache.myfaces.resource.ContractResourceLoader;
import org.apache.myfaces.resource.ResourceCachedInfo;
import org.apache.myfaces.resource.ResourceHandlerCache;
import org.apache.myfaces.resource.ResourceLoader;
import org.apache.myfaces.resource.ResourceMeta;
import org.apache.myfaces.resource.ResourceValidationUtils;
import org.apache.myfaces.util.lang.Assert;

public class FixedMyFacesResourceHandler extends ResourceHandlerImpl {
    private ResourceHandlerCache resourceHandlerCache;

    @Override
    public Resource createResource(String resourceName, String libraryName, String contentType) {
        Assert.notNull(resourceName, "resourceName");

        Resource resource = null;

        if (resourceName.length() == 0) {
            return null;
        }

        if (resourceName.charAt(0) == '/') {
            // If resourceName starts with '/', remove that character because it
            // does not have any meaning (with and without should point to the
            // same resource).
            resourceName = resourceName.substring(1);
        }
        if (!ResourceValidationUtils.isValidResourceName(resourceName)) {
            return null;
        }
        if (libraryName != null && !ResourceValidationUtils.isValidLibraryName(libraryName, isAllowSlashesLibraryName())) {
            return null;
        }
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (contentType == null) {
            //Resolve contentType using ExternalContext.getMimeType
            contentType = facesContext.getExternalContext().getMimeType(resourceName);
        }

        final String localePrefix = getLocalePrefixForLocateResource(facesContext);
        final List<String> contracts = facesContext.getResourceLibraryContracts();
        String contractPreferred = getContractNameForLocateResource(facesContext);
        ResourceHandlerCache.ResourceValue resourceValue = null;

        // Check cache:
        //
        // Contracts are on top of everything, because it is a concept that defines
        // resources in a application scope concept. It means all resources in
        // /resources or /META-INF/resources can be overridden using a contract. Note
        // it also means resources under /META-INF/flows can also be overridden using
        // a contract.

        // Check first the preferred contract if any. If not found, try the remaining
        // contracts and finally if not found try to found a resource without a
        // contract name.
        if (contractPreferred != null) {
            resourceValue = getResourceLoaderCache().getResource(resourceName, libraryName, contentType, localePrefix, contractPreferred);
        }
        if (resourceValue == null && !contracts.isEmpty()) {
            // Try to get resource but try with a contract name
            for (String contract : contracts) {
                resourceValue = getResourceLoaderCache().getResource(resourceName, libraryName, contentType, localePrefix, contract);
                if (resourceValue != null) {
                    break;
                }
            }
        }
        // Only if no contract preferred try without it.
        if (resourceValue == null) {
            // Try to get resource without contract name
            resourceValue = getResourceLoaderCache().getResource(resourceName, libraryName, contentType, localePrefix);
        }

        if (resourceValue != null) {
            resource = new FixedMyFacesResource(resourceValue.getResourceMeta(), resourceValue.getResourceLoader(),
                getResourceHandlerSupport(), contentType, resourceValue.getCachedInfo() != null ? resourceValue.getCachedInfo().getURL() : null,
                resourceValue.getCachedInfo() != null ? resourceValue.getCachedInfo().getRequestPath() : null);
        } else {
            boolean resolved = false;
            // Try preferred contract first
            if (contractPreferred != null) {
                for (ContractResourceLoader loader : getResourceHandlerSupport().getContractResourceLoaders()) {
                    ResourceMeta resourceMeta = deriveResourceMeta(loader, resourceName, libraryName, localePrefix, contractPreferred);
                    if (resourceMeta != null) {
                        resource = new FixedMyFacesResource(resourceMeta, loader, getResourceHandlerSupport(), contentType);

                        // cache it
                        getResourceLoaderCache().putResource(resourceName, libraryName, contentType, localePrefix, contractPreferred, resourceMeta, loader, new ResourceCachedInfo(resource.getURL(), resource.getRequestPath()));
                        resolved = true;
                        break;
                    }
                }
            }
            if (!resolved && !contracts.isEmpty()) {
                for (ContractResourceLoader loader : getResourceHandlerSupport().getContractResourceLoaders()) {
                    for (String contract : contracts) {
                        ResourceMeta resourceMeta = deriveResourceMeta(loader, resourceName, libraryName, localePrefix, contract);
                        if (resourceMeta != null) {
                            resource = new FixedMyFacesResource(resourceMeta, loader, getResourceHandlerSupport(), contentType);

                            // cache it
                            getResourceLoaderCache().putResource(resourceName, libraryName, contentType, localePrefix, contract, resourceMeta, loader, new ResourceCachedInfo(resource.getURL(), resource.getRequestPath()));
                            resolved = true;
                            break;
                        }
                    }
                }
            }
            if (!resolved) {
                for (ResourceLoader loader : getResourceHandlerSupport().getResourceLoaders()) {
                    ResourceMeta resourceMeta = deriveResourceMeta(loader, resourceName, libraryName, localePrefix);

                    if (resourceMeta != null) {
                        resource = new FixedMyFacesResource(resourceMeta, loader, getResourceHandlerSupport(), contentType);

                        // cache it
                        getResourceLoaderCache().putResource(resourceName, libraryName, contentType, localePrefix, null, resourceMeta, loader, new ResourceCachedInfo(resource.getURL(), resource.getRequestPath()));
                        break;
                    }
                }
            }
        }
        return resource;
    }

    private ResourceHandlerCache getResourceLoaderCache() {
        if (this.resourceHandlerCache == null) {
            this.resourceHandlerCache = new ResourceHandlerCache();
        }

        return this.resourceHandlerCache;
    }
}
