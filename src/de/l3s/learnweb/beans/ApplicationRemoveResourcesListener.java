package de.l3s.learnweb.beans;

import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;

/**
 * Removes Primefaces CSS and JS files from selected pages
 *
 * @author Kemkes
 *
 */
public class ApplicationRemoveResourcesListener implements SystemEventListener
{
    private static final String HEAD = "head";

    @Override
    public void processEvent(SystemEvent event) throws AbortProcessingException
    {
        FacesContext context = FacesContext.getCurrentInstance();
        UIViewRoot viewRoot = context.getViewRoot();
        // iterate over all resources which are added to the HEAD
        int i = context.getViewRoot().getComponentResources(context, HEAD).size() - 1;
        // need to use this loop because we can't get a modifiable iterator
        while(i >= 0)
        {
            // Fetch current resource from included resources list
            UIComponent resource = viewRoot.getComponentResources(context, HEAD).get(i);
            //String resourceName = (String) resource.getAttributes().get("name");

            if("primefaces".equals(resource.getAttributes().get("library")))
            {
                // remove all primefaces libraries

                //viewRoot.removeComponentResource(context, resource, HEAD); <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< uncomment me to remove primefaces components
            }
            /*
            else if(resourceName.equals("main-template/queryInput.js"))
            {
            // replace queryInput.js with queryInput.combined.js which includes all necessary libraries
            resource.getAttributes().put("name", "main-template/queryInput.combined.js");
            }*/

            i--;
        }
    }

    @Override
    public boolean isListenerForSource(Object source)
    {
        return (source instanceof UIViewRoot);
    }
}
