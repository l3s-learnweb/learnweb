package de.l3s.learnweb.component;

import jakarta.faces.component.FacesComponent;

import org.primefaces.component.api.Widget;
import org.primefaces.component.menu.AbstractMenu;
import org.primefaces.model.menu.MenuModel;

@FacesComponent(createTag = true, tagName = "menu", namespace = "http://l3s.de/learnweb")
public class LearnwebMenu extends AbstractMenu implements Widget {
    public static final String COMPONENT_FAMILY = "de.l3s.learnweb.component";
    public static final String COMPONENT_TYPE = "de.l3s.learnweb.component.LearnwebMenu";
    public static final String DEFAULT_RENDERER = "de.l3s.learnweb.component.LearnwebMenuRenderer";

    public static final String CONTAINER_CLASS = "ui-lwmenu ui-widget";

    public static final String MENUITEM_CLASS = "ui-menuitem";
    public static final String MENUITEM_LINK_CLASS = "ui-menuitem-link";
    public static final String MENUITEM_TEXT_CLASS = "ui-menuitem-text";
    public static final String MENUITEM_ICON_CLASS = "ui-menuitem-icon";
    public static final String MENUITEM_LINK_WITH_ICON_CLASS = MENUITEM_LINK_CLASS + " ui-menuitem-link-hasicon";

    public static final String DESCENDANT_SUBMENU_CLASS = MENUITEM_CLASS + " ui-menu-parent";
    public static final String DESCENDANT_SUBMENU_ICON_CLASS = MENUITEM_ICON_CLASS + " ui-menuitem-icon-expand ui-icon ui-icon-carat-1-e";
    public static final String DESCENDANT_SUBMENU_LIST_CLASS = "ui-menu-list ui-helper-reset ui-helper-hidden";

    public enum PropertyKeys {
        widgetVar,
        model,
        style,
        styleClass
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

    @Override
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
}