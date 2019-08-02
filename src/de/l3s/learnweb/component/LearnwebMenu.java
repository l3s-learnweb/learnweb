package de.l3s.learnweb.component;

import org.primefaces.component.api.Widget;
import org.primefaces.component.menu.AbstractMenu;
import org.primefaces.util.ComponentUtils;

import javax.faces.component.FacesComponent;

@FacesComponent(createTag = true, tagName = "menu", namespace = "http://l3s.de/learnweb")
public class LearnwebMenu extends AbstractMenu implements Widget
{
    public static final String COMPONENT_FAMILY = "de.l3s.learnweb.component";
    public static final String COMPONENT_TYPE = "de.l3s.learnweb.component.LearnwebMenu";
    public static final String DEFAULT_RENDERER = "de.l3s.learnweb.component.LearnwebMenuRenderer";

    public static final String CONTAINER_CLASS = "ui-lwmenu ui-widget";

    public static final String PANEL_CLASS = "ui-lwmenu-panel";
    public static final String PANEL_HEADER_CLASS = "ui-menuitem-link ui-lwmenu-panel-header";
    public static final String PANEL_HEADER_ICON_CLASS = "ui-menuitem-icon fa fa-fw fa-angle-right";
    public static final String PANEL_HEADER_TEXT_CLASS = "ui-menuitem-text";
    public static final String PANEL_CONTENT_CLASS = "ui-lwmenu-panel-content ui-helper-hidden";

    public static final String DESCENDANT_SUBMENU_CLASS = "ui-menuitem ui-menu-parent";
    public static final String DESCENDANT_SUBMENU_ICON_CLASS = "ui-lwmenu-icon fa fa-fw fa-angle-right";
    public static final String DESCENDANT_SUBMENU_LIST_CLASS = "ui-menu-list ui-helper-reset ui-helper-hidden";

    public static final String MENUITEM_CLASS = "ui-menuitem";
    public static final String MENUITEM_TEXT_CLASS = "ui-menuitem-text";
    public static final String MENUITEM_ICON_CLASS = "ui-menuitem-icon";
    public static final String MENUITEM_LINK_WITH_ICON_CLASS = "ui-menuitem-link ui-menuitem-link-hasicon";

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
