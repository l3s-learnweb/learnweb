package de.l3s.learnweb.component;

import org.primefaces.component.menu.AbstractMenu;
import org.primefaces.component.menu.BaseMenuRenderer;
import org.primefaces.model.menu.MenuElement;
import org.primefaces.model.menu.MenuItem;
import org.primefaces.model.menu.Submenu;
import org.primefaces.util.WidgetBuilder;

import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import java.io.IOException;
import java.util.List;

public class LearnwebMenuRenderer extends BaseMenuRenderer
{
    @Override
    protected void encodeScript(FacesContext context, AbstractMenu abstractMenu) throws IOException
    {
        LearnwebMenu menu = (LearnwebMenu) abstractMenu;
        String clientId = menu.getClientId(context);
        WidgetBuilder wb = getWidgetBuilder(context);
        wb.init("LearnwebMenu", menu.resolveWidgetVar(), clientId)
                .attr("stateful", menu.isStateful())
                .attr("multiple", menu.isMultiple());
        wb.finish();
    }

    @Override
    protected void encodeMarkup(FacesContext context, AbstractMenu abstractMenu) throws IOException
    {
        ResponseWriter writer = context.getResponseWriter();
        LearnwebMenu menu = (LearnwebMenu) abstractMenu;
        String clientId = menu.getClientId(context);
        String style = menu.getStyle();
        String styleClass = menu.getStyleClass();
        styleClass = styleClass == null ? LearnwebMenu.CONTAINER_CLASS : LearnwebMenu.CONTAINER_CLASS + " " + styleClass;

        writer.startElement("ul", menu);
        writer.writeAttribute("id", clientId, "id");
        writer.writeAttribute("class", styleClass, "styleClass");
        if(style != null)
        {
            writer.writeAttribute("style", style, "style");
        }
        writer.writeAttribute("role", "menu", null);

        if(menu.getElementsCount() > 0)
        {
            encodeMenuElements(context, menu, menu.getElements());
        }

        writer.endElement("ul");
    }

    private void encodeMenuItemWrapper(FacesContext context, AbstractMenu menu, MenuItem menuitem, String tabindex) throws IOException
    {
        ResponseWriter writer = context.getResponseWriter();

        String containerStyle = menuitem.getContainerStyle();
        String containerStyleClass = menuitem.getContainerStyleClass();
        containerStyleClass = (containerStyleClass == null) ? LearnwebMenu.MENUITEM_CLASS : LearnwebMenu.MENUITEM_CLASS + " " + containerStyleClass;

        writer.startElement("li", null);
        writer.writeAttribute("class", containerStyleClass, null);
        if(containerStyle != null)
        {
            writer.writeAttribute("style", containerStyle, null);
        }
        encodeMenuItem(context, menu, menuitem, tabindex);
        writer.endElement("li");
    }

    private void encodeRootSubmenu(FacesContext context, LearnwebMenu menu, Submenu submenu) throws IOException
    {
        ResponseWriter writer = context.getResponseWriter();
        String style = submenu.getStyle();
        String styleClass = submenu.getStyleClass();
        styleClass = styleClass == null ? LearnwebMenu.PANEL_CLASS : LearnwebMenu.PANEL_CLASS + " " + styleClass;
        boolean expanded = submenu.isExpanded();
        String headerClass = expanded ? LearnwebMenu.ACTIVE_HEADER_CLASS : LearnwebMenu.INACTIVE_HEADER_CLASS;
        String headerIconClass = expanded ? LearnwebMenu.ACTIVE_TAB_HEADER_ICON_CLASS : LearnwebMenu.INACTIVE_TAB_HEADER_ICON_CLASS;
        String contentClass = expanded ? LearnwebMenu.ACTIVE_ROOT_SUBMENU_CONTENT : LearnwebMenu.INACTIVE_ROOT_SUBMENU_CONTENT;

        //wrapper
        writer.startElement("li", null);
        writer.writeAttribute("class", styleClass, null);
        if(style != null)
        {
            writer.writeAttribute("style", style, null);
        }

        //header
        writer.startElement("div", null);
        writer.writeAttribute("class", headerClass, null);
        writer.writeAttribute("role", "tab", null);
        writer.writeAttribute("tabindex", "0", null);

        //icon
        writer.startElement("span", null);
        writer.writeAttribute("class", headerIconClass, null);
        writer.endElement("span");

        writer.startElement("a", null);
        writer.writeAttribute("tabindex", "-1", null);
        if(!(submenu instanceof ActiveSubMenu) || submenu.isDisabled())
        {
            writer.writeAttribute("href", "#", null);
            writer.writeAttribute("onclick", "return false;", null);
        }
        else
        {
            setAnchorAttributes(context, (ActiveSubMenu) submenu);
        }
        writer.writeText(submenu.getLabel(), null);
        writer.endElement("a");

        writer.endElement("div");

        //content
        writer.startElement("div", null);
        writer.writeAttribute("class", contentClass, null);
        writer.writeAttribute("role", "tabpanel", null);
        writer.writeAttribute("id", menu.getClientId(context) + "_" + submenu.getId(), null);
        writer.writeAttribute("tabindex", "0", null);

        if(submenu.getElementsCount() > 0)
        {
            writer.startElement("ul", null);
            writer.writeAttribute("class", LearnwebMenu.LIST_CLASS, null);
            encodeMenuElements(context, menu, submenu.getElements());
            writer.endElement("ul");
        }

        writer.endElement("div");   //content

        writer.endElement("li");   //wrapper
    }

    private void setAnchorAttributes(FacesContext context, ActiveSubMenu activeSubMenu) throws IOException
    {
        ResponseWriter writer = context.getResponseWriter();
        setConfirmationScript(context, activeSubMenu);
        String onclick = activeSubMenu.getOnclick();

        //GET
        if(activeSubMenu.getUrl() != null || activeSubMenu.getOutcome() != null)
        {
            String targetURL = getTargetURL(context, activeSubMenu);
            writer.writeAttribute("href", targetURL, null);

            if(activeSubMenu.getTarget() != null)
            {
                writer.writeAttribute("target", activeSubMenu.getTarget(), null);
            }
        }
        //POST
        else
        {
            writer.writeAttribute("href", "#", null);
            writer.writeAttribute("onclick", "return false;", null);
        }

        if(onclick != null)
        {
            if(activeSubMenu.requiresConfirmation())
            {
                writer.writeAttribute("data-pfconfirmcommand", onclick, null);
                writer.writeAttribute("onclick", activeSubMenu.getConfirmationScript(), "onclick");
            }
            else
            {
                writer.writeAttribute("onclick", onclick, null);
            }
        }
    }

    private void encodeDescendantSubmenu(FacesContext context, LearnwebMenu menu, Submenu submenu) throws IOException
    {
        ResponseWriter writer = context.getResponseWriter();
        String icon = submenu.getIcon();
        String style = submenu.getStyle();
        String styleClass = submenu.getStyleClass();
        styleClass = styleClass == null ? LearnwebMenu.DESCENDANT_SUBMENU_CLASS : LearnwebMenu.DESCENDANT_SUBMENU_CLASS + " " + styleClass;
        boolean expanded = submenu.isExpanded();
        String toggleIconClass = expanded ? LearnwebMenu.DESCENDANT_SUBMENU_EXPANDED_ICON_CLASS : LearnwebMenu.DESCENDANT_SUBMENU_COLLAPSED_ICON_CLASS;
        String listClass = expanded ? LearnwebMenu.DESCENDANT_SUBMENU_EXPANDED_LIST_CLASS : LearnwebMenu.DESCENDANT_SUBMENU_COLLAPSED_LIST_CLASS;
        boolean hasIcon = (icon != null);
        String linkClass = (hasIcon) ? LearnwebMenu.MENUITEM_LINK_WITH_ICON_CLASS : LearnwebMenu.MENUITEM_LINK_CLASS;

        writer.startElement("li", null);
        writer.writeAttribute("id", submenu.getClientId(), null);
        writer.writeAttribute("class", styleClass, null);
        if(style != null)
        {
            writer.writeAttribute("style", style, null);
        }

        writer.startElement("a", null);
        writer.writeAttribute("class", linkClass, null);

        if((submenu instanceof ActiveSubMenu) && !submenu.isDisabled())
        {
            setAnchorAttributes(context, (ActiveSubMenu) submenu);
        }

        //toggle icon
        writer.startElement("span", null);
        writer.writeAttribute("class", toggleIconClass, null);
        writer.endElement("span");

        //user icon
        if(hasIcon)
        {
            writer.startElement("span", null);
            writer.writeAttribute("class", "ui-icon " + icon, null);
            writer.endElement("span");
        }

        //submenu label
        writer.startElement("span", null);
        writer.writeAttribute("class", LearnwebMenu.MENUITEM_TEXT_CLASS, null);
        writer.writeText(submenu.getLabel(), null);
        writer.endElement("span");

        writer.endElement("a");

        //submenu children
        if(submenu.getElementsCount() > 0)
        {
            writer.startElement("ul", null);
            writer.writeAttribute("class", listClass, null);
            encodeMenuElements(context, menu, submenu.getElements());
            writer.endElement("ul");
        }

        writer.endElement("li");
    }

    private void encodeMenuElements(final FacesContext context, final LearnwebMenu menu, final List<MenuElement> elements) throws IOException
    {
        for(MenuElement element : elements)
        {
            if(element.isRendered())
            {
                if(element instanceof Submenu)
                {
                    encodeDescendantSubmenu(context, menu, (Submenu) element);
                }
                else if(element instanceof MenuItem)
                {
                    encodeMenuItemWrapper(context, menu, (MenuItem) element, "-1");
                }
            }
        }
    }
}
