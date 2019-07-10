package de.l3s.learnweb.component;

import org.primefaces.component.api.Widget;
import org.primefaces.component.menu.AbstractMenu;
import org.primefaces.util.ComponentUtils;

import javax.faces.component.FacesComponent;

@FacesComponent(createTag = true, tagName = "learnwebMenu", namespace = "http://learnweb.l3s.uni-hannover.de/components")
public class LearnwebMenu extends AbstractMenu implements Widget
{
    public static final String COMPONENT_FAMILY = "de.l3s.learnweb.component";
    public static final String COMPONENT_TYPE = "de.l3s.learnweb.component.LearnwebMenu";
    public static final String DEFAULT_RENDERER = "de.l3s.learnweb.component.LearnwebMenuRenderer";

    public static final String CONTAINER_CLASS = "ui-lwmenu ui-widget";

    public static final String PANEL_CLASS = "ui-lwmenu-panel";
    public static final String INACTIVE_HEADER_CLASS = "ui-lwmenu-panel-header ui-state-default ui-corner-all";
    public static final String ACTIVE_HEADER_CLASS = "ui-lwmenu-panel-header ui-state-default ui-state-active ui-corner-top";
    public static final String INACTIVE_ROOT_SUBMENU_CONTENT = "ui-lwmenu-panel-content ui-helper-hidden";
    public static final String ACTIVE_ROOT_SUBMENU_CONTENT = "ui-lwmenu-panel-content";

    public static final String INACTIVE_TAB_HEADER_ICON_CLASS = "ui-icon ui-icon-triangle-1-e";
    public static final String ACTIVE_TAB_HEADER_ICON_CLASS = "ui-icon ui-icon-triangle-1-s";
    public static final String DESCENDANT_SUBMENU_CLASS = "ui-menuitem ui-corner-all ui-menu-parent";
    public static final String DESCENDANT_SUBMENU_EXPANDED_ICON_CLASS = "ui-lwmenu-icon ui-icon ui-icon-triangle-1-s";
    public static final String DESCENDANT_SUBMENU_COLLAPSED_ICON_CLASS = "ui-lwmenu-icon ui-icon ui-icon-triangle-1-e";
    public static final String DESCENDANT_SUBMENU_EMPTY_ICON_CLASS = "ui-lwmenu-icon ui-icon ui-icon-empty";
    public static final String DESCENDANT_SUBMENU_EXPANDED_LIST_CLASS = "ui-menu-list ui-helper-reset";
    public static final String DESCENDANT_SUBMENU_COLLAPSED_LIST_CLASS = "ui-menu-list ui-helper-reset ui-helper-hidden";
    public static final String MENUITEM_CLASS = "ui-menuitem ui-corner-all";
    public static final String MENUITEM_LINK_WITH_ICON_CLASS = "ui-menuitem-link ui-menuitem-link-hasicon ui-corner-all";

    public LearnwebMenu()
    {
        setRendererType(DEFAULT_RENDERER);
    }

    public enum PropertyKeys
    {

        widgetVar,
        model,
        style,
        styleClass,
        multiple,
        stateful
    }

    @Override
    public String getFamily()
    {
        return COMPONENT_FAMILY;
    }

    public String getWidgetVar()
    {
        return (String) getStateHelper().eval(LearnwebMenu.PropertyKeys.widgetVar, null);
    }

    public void setWidgetVar(String widgetVar)
    {
        getStateHelper().put(LearnwebMenu.PropertyKeys.widgetVar, widgetVar);
    }

    @Override
    public org.primefaces.model.menu.MenuModel getModel()
    {
        return (org.primefaces.model.menu.MenuModel) getStateHelper().eval(LearnwebMenu.PropertyKeys.model, null);
    }

    public void setModel(org.primefaces.model.menu.MenuModel model)
    {
        getStateHelper().put(LearnwebMenu.PropertyKeys.model, model);
    }

    public String getStyle()
    {
        return (String) getStateHelper().eval(LearnwebMenu.PropertyKeys.style, null);
    }

    public void setStyle(String style)
    {
        getStateHelper().put(LearnwebMenu.PropertyKeys.style, style);
    }

    public String getStyleClass()
    {
        return (String) getStateHelper().eval(LearnwebMenu.PropertyKeys.styleClass, null);
    }

    public void setStyleClass(String styleClass)
    {
        getStateHelper().put(LearnwebMenu.PropertyKeys.styleClass, styleClass);
    }

    public boolean isMultiple()
    {
        return (Boolean) getStateHelper().eval(LearnwebMenu.PropertyKeys.multiple, true);
    }

    public void setMultiple(boolean multiple)
    {
        getStateHelper().put(LearnwebMenu.PropertyKeys.multiple, multiple);
    }

    public boolean isStateful()
    {
        return (Boolean) getStateHelper().eval(LearnwebMenu.PropertyKeys.stateful, true);
    }

    public void setStateful(boolean stateful)
    {
        getStateHelper().put(LearnwebMenu.PropertyKeys.stateful, stateful);
    }

    @Override
    public String resolveWidgetVar()
    {
        return ComponentUtils.resolveWidgetVar(getFacesContext(), this);
    }
}
