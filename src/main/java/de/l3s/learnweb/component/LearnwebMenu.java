package de.l3s.learnweb.component;

import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UINamingContainer;
import jakarta.faces.component.UIOutput;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.AbortProcessingException;
import jakarta.faces.event.ComponentSystemEvent;
import jakarta.faces.event.ComponentSystemEventListener;
import jakarta.faces.event.ListenerFor;
import jakarta.faces.event.PostAddToViewEvent;

import org.primefaces.component.api.Widget;
import org.primefaces.component.menu.AbstractMenu;
import org.primefaces.model.menu.MenuModel;

@FacesComponent(createTag = true, tagName = "menu", namespace = "http://l3s.de/learnweb")
@ListenerFor(sourceClass = LearnwebMenu.class, systemEventClass = PostAddToViewEvent.class)
public final class LearnwebMenu extends AbstractMenu implements Widget, ComponentSystemEventListener {

    public static final String COMPONENT_TYPE = "de.l3s.learnweb.component.LearnwebMenu";
    public static final String COMPONENT_FAMILY = "de.l3s.learnweb.component";
    private static final String DEFAULT_RENDERER = "de.l3s.learnweb.component.LearnwebMenuRenderer";
    private static final String[] REQUIRED_RESOURCES = {"components.css", "jquery/jquery.js", "jquery/jquery-plugins.js", "core.js"};

    protected enum PropertyKeys {
        widgetVar, model, style, styleClass
    }

    public LearnwebMenu() {
        setRendererType(DEFAULT_RENDERER);
    }

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    public String getWidgetVar() {
        return (String) getStateHelper().eval(PropertyKeys.widgetVar, null);
    }

    public void setWidgetVar(String widgetVar) {
        getStateHelper().put(PropertyKeys.widgetVar, widgetVar);
    }

    public MenuModel getModel() {
        return (MenuModel) getStateHelper().eval(PropertyKeys.model, null);
    }

    public void setModel(MenuModel model) {
        getStateHelper().put(PropertyKeys.model, model);
    }

    public String getStyle() {
        return (String) getStateHelper().eval(PropertyKeys.style, null);
    }

    public void setStyle(String style) {
        getStateHelper().put(PropertyKeys.style, style);
    }

    public String getStyleClass() {
        return (String) getStateHelper().eval(PropertyKeys.styleClass, null);
    }

    public void setStyleClass(String styleClass) {
        getStateHelper().put(PropertyKeys.styleClass, styleClass);
    }

    public String resolveWidgetVar() {
        FacesContext context = getFacesContext();
        String userWidgetVar = (String) getAttributes().get("widgetVar");

        if (userWidgetVar != null) {
            return userWidgetVar;
        } else {
            return "widget_" + getClientId(context).replaceAll("-|" + UINamingContainer.getSeparatorChar(context), "_");
        }
    }

    @Override
    public void processEvent(ComponentSystemEvent event) throws AbortProcessingException {
        if (event instanceof PostAddToViewEvent) {
            FacesContext context = getFacesContext();
            UIViewRoot root = context.getViewRoot();

            for (String res : REQUIRED_RESOURCES) {
                UIComponent component = context.getApplication().createComponent(UIOutput.COMPONENT_TYPE);
                if (res.endsWith("css")) {
                    component.setRendererType("jakarta.faces.resource.Stylesheet");
                } else if (res.endsWith("js")) {
                    component.setRendererType("jakarta.faces.resource.Script");
                }

                component.getAttributes().put("library", "primefaces");
                component.getAttributes().put("name", res);

                root.addComponentResource(context, component);
            }
        }
    }
}
