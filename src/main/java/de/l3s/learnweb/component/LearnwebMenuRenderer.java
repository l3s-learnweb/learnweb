package de.l3s.learnweb.component;

import java.io.IOException;
import java.util.List;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;
import jakarta.faces.render.FacesRenderer;

import org.primefaces.component.menu.AbstractMenu;
import org.primefaces.component.menu.BaseMenuRenderer;
import org.primefaces.model.menu.DefaultSubMenu;
import org.primefaces.model.menu.MenuElement;
import org.primefaces.model.menu.MenuItem;
import org.primefaces.model.menu.Submenu;
import org.primefaces.util.WidgetBuilder;

@FacesRenderer(componentFamily = "de.l3s.learnweb.component", rendererType = "de.l3s.learnweb.component.LearnwebMenuRenderer")
public class LearnwebMenuRenderer extends BaseMenuRenderer {
    @Override
    protected void encodeScript(FacesContext context, AbstractMenu abstractMenu) throws IOException {
        LearnwebMenu menu = (LearnwebMenu) abstractMenu;

        WidgetBuilder wb = getWidgetBuilder(context);
        wb.init("LearnwebMenu", menu);
        wb.finish();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void encodeMarkup(FacesContext context, AbstractMenu abstractMenu) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        LearnwebMenu menu = (LearnwebMenu) abstractMenu;
        String clientId = menu.getClientId(context);
        String style = menu.getStyle();
        String styleClass = menu.getStyleClass();
        styleClass = styleClass == null ? LearnwebMenu.CONTAINER_CLASS : LearnwebMenu.CONTAINER_CLASS + " " + styleClass;

        writer.startElement("ul", menu);
        writer.writeAttribute("id", clientId, "id");
        writer.writeAttribute("class", styleClass, "styleClass");
        if (style != null) {
            writer.writeAttribute("style", style, "style");
        }
        writer.writeAttribute("role", "menu", null);

        if (menu.getElementsCount() > 0) {
            encodeRootMenuElements(context, menu, menu.getElements());
        }

        writer.endElement("ul");
    }

    private void encodeMenuItemWrapper(FacesContext context, AbstractMenu menu, MenuItem menuitem, String tabindex) throws IOException {
        ResponseWriter writer = context.getResponseWriter();

        String containerStyle = menuitem.getContainerStyle();
        String containerStyleClass = menuitem.getContainerStyleClass();
        containerStyleClass = (containerStyleClass == null) ? LearnwebMenu.MENUITEM_CLASS : LearnwebMenu.MENUITEM_CLASS + " " + containerStyleClass;

        writer.startElement("li", null);
        writer.writeAttribute("class", containerStyleClass, null);
        if (containerStyle != null) {
            writer.writeAttribute("style", containerStyle, null);
        }
        encodeMenuItem(context, menu, menuitem, tabindex);
        writer.endElement("li");
    }

    @SuppressWarnings("unchecked")
    private void encodePanelSubmenu(FacesContext context, LearnwebMenu menu, Submenu submenu) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        String style = submenu.getStyle();
        String styleClass = submenu.getStyleClass();
        styleClass = LearnwebMenu.DESCENDANT_SUBMENU_CLASS + " ui-menuitem-panel " + styleClass;
        styleClass = submenu.isExpanded() ? styleClass + " ui-state-expand" : styleClass;

        //wrapper
        writer.startElement("li", null);
        writer.writeAttribute("class", styleClass, null);
        if (style != null) {
            writer.writeAttribute("style", style, null);
        }

        //header
        writer.startElement("a", null);
        writer.writeAttribute("class", LearnwebMenu.MENUITEM_LINK_CLASS, null);
        writer.writeAttribute("role", "tab", null);
        writer.writeAttribute("tabindex", "0", null);
        if (!(submenu instanceof ActiveSubmenu) || submenu.isDisabled()) {
            writer.writeAttribute("href", "#", null);
            writer.writeAttribute("onclick", "return false;", null);
        } else {
            setAnchorAttributes(context, (ActiveSubmenu) submenu);
        }

        //icon
        writer.startElement("span", null);
        writer.writeAttribute("class", LearnwebMenu.DESCENDANT_SUBMENU_ICON_CLASS, null);
        writer.endElement("span");

        //icon
        writer.startElement("span", null);
        writer.writeAttribute("class", LearnwebMenu.MENUITEM_TEXT_CLASS, null);
        writer.writeText(submenu.getLabel(), null);
        writer.endElement("span");
        writer.endElement("a");

        //content
        if (submenu.getElementsCount() > 0) {
            writer.startElement("ul", null);
            writer.writeAttribute("class", LearnwebMenu.DESCENDANT_SUBMENU_LIST_CLASS, null);
            encodeMenuElements(context, menu, submenu.getElements());
            writer.endElement("ul");
        }

        writer.endElement("li"); //wrapper
    }

    @Override
    protected void encodeMenuItemContent(FacesContext context, AbstractMenu menu, MenuItem menuitem) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        String icon = menuitem.getIcon();
        Object value = menuitem.getValue();

        if (icon != null) {
            writer.startElement("span", null);
            writer.writeAttribute("class", LearnwebMenu.MENUITEM_ICON_CLASS + " " + icon, null);
            writer.endElement("span");
        }

        writer.startElement("span", null);
        writer.writeAttribute("class", LearnwebMenu.MENUITEM_TEXT_CLASS, null);

        if (value != null) {
            if (menuitem.isEscape()) {
                writer.writeText(value, "value");
            } else {
                writer.write(value.toString());
            }
        } else if (menuitem.shouldRenderChildren() && menuitem instanceof UIComponent component) {
            renderChildren(context, component);
        }

        writer.endElement("span");
    }

    private void setAnchorAttributes(FacesContext context, ActiveSubmenu activeSubMenu) throws IOException {
        ResponseWriter writer = context.getResponseWriter();

        if (activeSubMenu.getHref() != null || activeSubMenu.getOutcome() != null) { // GET
            String targetURL = getTargetURL(context, activeSubMenu);
            writer.writeAttribute("href", targetURL, null);

            if (activeSubMenu.getTarget() != null) {
                writer.writeAttribute("target", activeSubMenu.getTarget(), null);
            }
        } else { // POST
            writer.writeAttribute("href", "#", null);
            writer.writeAttribute("onclick", "return false;", null);
        }
    }

    @SuppressWarnings("unchecked")
    private void encodeDescendantSubmenu(FacesContext context, LearnwebMenu menu, Submenu submenu) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        String icon = submenu.getIcon();
        String style = submenu.getStyle();
        String styleClass = submenu.getStyleClass();
        styleClass = styleClass == null ? LearnwebMenu.DESCENDANT_SUBMENU_CLASS : LearnwebMenu.DESCENDANT_SUBMENU_CLASS + " " + styleClass;
        styleClass = submenu.getElementsCount() == 0 ? styleClass + " ui-state-empty" : styleClass;
        styleClass = submenu.isExpanded() ? styleClass + " ui-state-expand" : styleClass;
        boolean hasIcon = (icon != null);
        String linkClass = (hasIcon) ? LearnwebMenu.MENUITEM_LINK_WITH_ICON_CLASS : LearnwebMenu.MENUITEM_LINK_CLASS;

        writer.startElement("li", null);
        // writer.writeAttribute("id", submenu.getClientId(), null);
        writer.writeAttribute("class", styleClass, null);
        if (style != null) {
            writer.writeAttribute("style", style, null);
        }

        writer.startElement("a", null);
        writer.writeAttribute("class", linkClass, null);

        if ((submenu instanceof ActiveSubmenu) && !submenu.isDisabled()) {
            setAnchorAttributes(context, (ActiveSubmenu) submenu);
        }

        //toggle icon
        writer.startElement("span", null);
        writer.writeAttribute("class", LearnwebMenu.DESCENDANT_SUBMENU_ICON_CLASS, null);
        writer.endElement("span");

        //user icon
        if (hasIcon) {
            writer.startElement("span", null);
            writer.writeAttribute("class", LearnwebMenu.MENUITEM_ICON_CLASS + " " + icon, null);
            writer.endElement("span");
        }

        //submenu label
        writer.startElement("span", null);
        writer.writeAttribute("class", LearnwebMenu.MENUITEM_TEXT_CLASS, null);
        writer.writeText(submenu.getLabel(), null);
        writer.endElement("span");
        writer.endElement("a");

        //submenu children
        if (submenu.getElementsCount() > 0) {
            writer.startElement("ul", null);
            writer.writeAttribute("class", LearnwebMenu.DESCENDANT_SUBMENU_LIST_CLASS, null);
            encodeMenuElements(context, menu, submenu.getElements());
            writer.endElement("ul");
        }

        writer.endElement("li");
    }

    private void encodeRootMenuElements(final FacesContext context, final LearnwebMenu menu, final List<MenuElement> elements) throws IOException {
        for (MenuElement element : elements) {
            if (element.isRendered()) {
                if (element instanceof DefaultSubMenu) {
                    encodePanelSubmenu(context, menu, (Submenu) element);
                } else if (element instanceof Submenu) {
                    encodeDescendantSubmenu(context, menu, (Submenu) element);
                } else if (element instanceof MenuItem) {
                    encodeMenuItemWrapper(context, menu, (MenuItem) element, "-1");
                }
            }
        }
    }

    private void encodeMenuElements(final FacesContext context, final LearnwebMenu menu, final List<MenuElement> elements) throws IOException {
        for (MenuElement element : elements) {
            if (element.isRendered()) {
                if (element instanceof Submenu) {
                    encodeDescendantSubmenu(context, menu, (Submenu) element);
                } else if (element instanceof MenuItem) {
                    encodeMenuItemWrapper(context, menu, (MenuItem) element, "-1");
                }
            }
        }
    }
}
