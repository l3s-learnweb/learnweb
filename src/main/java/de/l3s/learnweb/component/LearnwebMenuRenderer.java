package de.l3s.learnweb.component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.faces.FacesException;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;
import jakarta.faces.render.FacesRenderer;

import org.primefaces.component.api.AjaxSource;
import org.primefaces.component.api.UIOutcomeTarget;
import org.primefaces.component.menu.AbstractMenu;
import org.primefaces.component.menu.BaseMenuRenderer;
import org.primefaces.component.menuitem.UIMenuItem;
import org.primefaces.component.submenu.UISubmenu;
import org.primefaces.context.PrimeRequestContext;
import org.primefaces.expression.SearchExpressionUtils;
import org.primefaces.model.menu.MenuElement;
import org.primefaces.model.menu.MenuItem;
import org.primefaces.model.menu.Separator;
import org.primefaces.model.menu.Submenu;
import org.primefaces.util.AjaxRequestBuilder;
import org.primefaces.util.ComponentTraversalUtils;
import org.primefaces.util.WidgetBuilder;

@FacesRenderer(componentFamily = "de.l3s.learnweb.component", rendererType = "de.l3s.learnweb.component.LearnwebMenuRenderer")
public class LearnwebMenuRenderer extends BaseMenuRenderer {

    @Override
    protected void encodeMarkup(FacesContext context, AbstractMenu abstractMenu) throws IOException {
        LearnwebMenu menu = (LearnwebMenu) abstractMenu;
        ResponseWriter writer = context.getResponseWriter();
        String style = menu.getStyle();
        String styleClass = menu.getStyleClass();
        styleClass = (styleClass == null) ? "layout-menu" : "layout-menu " + styleClass;

        writer.startElement("ul", menu);
        writer.writeAttribute("id", menu.getClientId(context), "id");
        writer.writeAttribute("class", styleClass, "styleClass");
        if (style != null) {
            writer.writeAttribute("style", style, "style");
        }
        writer.writeAttribute("role", "menu", null);

        if (menu.getElementsCount() > 0) {
            encodeElements(context, menu, menu.getElements());
        }

        writer.endElement("ul");
    }

    protected void encodeElements(FacesContext context, AbstractMenu menu, List<MenuElement> elements) throws IOException {
        for (MenuElement element : elements) {
            encodeElement(context, menu, element);
        }
    }

    protected void encodeElement(FacesContext context, AbstractMenu menu, MenuElement element) throws IOException {
        ResponseWriter writer = context.getResponseWriter();

        if (element.isRendered()) {
            if (element instanceof MenuItem menuItem) {
                String menuItemClientId = (menuItem instanceof UIComponent) ? menuItem.getClientId() : menu.getClientId(context) + "_" + menuItem.getClientId();
                String containerStyle = menuItem.getContainerStyle();
                String containerStyleClass = menuItem.getContainerStyleClass();

                writer.startElement("li", null);
                writer.writeAttribute("id", menuItemClientId, null);
                writer.writeAttribute("role", "menuitem", null);

                if (containerStyle != null) {
                    writer.writeAttribute("style", containerStyle, null);
                }
                if (containerStyleClass != null) {
                    writer.writeAttribute("class", containerStyleClass, null);
                }

                encodeMenuItem(context, menu, menuItem);

                writer.endElement("li");
            } else if (element instanceof Submenu submenu) {
                String submenuClientId = (submenu instanceof UIComponent) ? ((UIComponent) submenu).getClientId() : menu.getClientId(context) + "_" + submenu.getId();
                String style = submenu.getStyle();
                String styleClass = submenu.getStyleClass();
                boolean isExpanded = submenu.isExpanded();
                String defaultStyleClass = "ui-menuitem-submenu";
                styleClass = styleClass != null ? styleClass + " " + defaultStyleClass : defaultStyleClass;
                styleClass = isExpanded ? styleClass + " active-menuitem" : styleClass;

                writer.startElement("li", null);
                writer.writeAttribute("id", submenuClientId, null);
                writer.writeAttribute("role", "menuitem", null);
                writer.writeAttribute("class", styleClass, null);

                if (style != null) {
                    writer.writeAttribute("style", style, null);
                }

                encodeSubmenu(context, menu, submenu);

                writer.endElement("li");
            } else if (element instanceof Separator) {
                encodeSeparator(context, (Separator) element);
            }
        }
    }

    protected void encodeSubmenu(FacesContext context, AbstractMenu menu, Submenu submenu) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        String icon = submenu.getIcon();
        String label = submenu.getLabel();
        int childrenElementsCount = submenu.getElementsCount();
        boolean isExpanded = submenu.isExpanded();

        writer.startElement("a", null);

        if (submenu instanceof ActiveSubmenu activeSubmenu) {
            if (activeSubmenu.getHref() != null || activeSubmenu.getOutcome() != null) {
                String targetURL = getTargetURL(context, activeSubmenu);
                writer.writeAttribute("href", targetURL, null);

                if (activeSubmenu.getTarget() != null) {
                    writer.writeAttribute("target", activeSubmenu.getTarget(), null);
                }
            }
        } else {
            writer.writeAttribute("href", "#", null);
        }

        encodeItemIcon(context, icon);

        if (label != null) {
            writer.startElement("span", null);
            writer.writeText(label, null);
            writer.endElement("span");

            if (submenu instanceof UISubmenu uiSubmenu) {
                encodeBadge(context, uiSubmenu.getAttributes().get("badge"));
            }
            encodeToggleIcon(context, submenu, childrenElementsCount);
        }

        writer.endElement("a");

        //submenus and menuitems
        if (childrenElementsCount > 0) {
            writer.startElement("ul", null);
            writer.writeAttribute("role", "menu", null);

            if (isExpanded) {
                writer.writeAttribute("style", "display:block;", null);
            }

            encodeElements(context, menu, submenu.getElements());
            writer.endElement("ul");
        }
    }

    protected void encodeItemIcon(FacesContext context, String icon) throws IOException {
        if (icon != null) {
            ResponseWriter writer = context.getResponseWriter();

            writer.startElement("i", null);
            if (icon.contains("fa ")) {
                icon += " fa-fw";
            }

            writer.writeAttribute("class", icon + " layout-menuitem-icon", null);
            writer.endElement("i");
        }
    }

    protected void encodeToggleIcon(FacesContext context, Submenu submenu, int childrenElementsCount) throws IOException {
        if (childrenElementsCount > 0) {
            ResponseWriter writer = context.getResponseWriter();

            writer.startElement("i", null);
            writer.writeAttribute("class", "fas fa-angle-down layout-menuitem-toggler", null);
            writer.endElement("i");
        }
    }

    protected void encodeBadge(FacesContext context, Object value) throws IOException {
        if (value != null) {
            ResponseWriter writer = context.getResponseWriter();

            writer.startElement("span", null);
            writer.writeAttribute("class", "menuitem-badge", null);
            writer.writeText(value.toString(), null);
            writer.endElement("span");
        }
    }

    @Override
    protected void encodeSeparator(FacesContext context, Separator separator) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        String style = separator.getStyle();
        String styleClass = separator.getStyleClass();
        styleClass = styleClass == null ? "Separator" : "Separator " + styleClass;

        //title
        writer.startElement("li", null);
        writer.writeAttribute("class", styleClass, null);
        if (style != null) {
            writer.writeAttribute("style", style, null);
        }

        writer.endElement("li");
    }

    @Override
    protected void encodeMenuItem(FacesContext context, AbstractMenu menu, MenuItem menuitem) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        String title = menuitem.getTitle();
        boolean disabled = menuitem.isDisabled();
        // Object value = menuitem.getValue();
        String style = menuitem.getStyle();
        String styleClass = menuitem.getStyleClass();

        writer.startElement("a", null);
        if (title != null) {
            writer.writeAttribute("title", title, null);
        }
        if (style != null) {
            writer.writeAttribute("style", style, null);
        }
        if (styleClass != null) {
            writer.writeAttribute("class", styleClass, null);
        }

        if (disabled) {
            writer.writeAttribute("href", "#", null);
            writer.writeAttribute("onclick", "return false;", null);
        } else {
            String onclick = menuitem.getOnclick();

            //GET
            if (menuitem.getUrl() != null || menuitem.getOutcome() != null) {
                String targetURL = getTargetURL(context, (UIOutcomeTarget) menuitem);
                writer.writeAttribute("href", targetURL, null);

                if (menuitem.getTarget() != null) {
                    writer.writeAttribute("target", menuitem.getTarget(), null);
                }
            } else { //POST
                writer.writeAttribute("href", "#", null);

                UIComponent form = ComponentTraversalUtils.closestForm(menu);
                if (form == null) {
                    throw new FacesException("MenuItem must be inside a form element");
                }

                String command;
                if (menuitem.isDynamic()) {
                    String menuClientId = menu.getClientId(context);
                    Map<String, List<String>> params = menuitem.getParams();
                    if (params == null) {
                        params = new LinkedHashMap<>();
                    }
                    List<String> idParams = new ArrayList<>();
                    idParams.add(menuitem.getId());
                    params.put(menuClientId + "_menuid", idParams);

                    command = menuitem.isAjax() ? createAjaxRequest(context, menu, (AjaxSource) menuitem, form, params)
                        : buildNonAjaxRequest(context, menu, form, menuClientId, params, true);
                } else {
                    command = menuitem.isAjax() ? createAjaxRequest(context, (AjaxSource) menuitem, form)
                        : buildNonAjaxRequest(context, ((UIComponent) menuitem), form, ((UIComponent) menuitem).getClientId(context), true);
                }

                onclick = (onclick == null) ? command : onclick + ";" + command;
            }

            if (onclick != null) {
                writer.writeAttribute("onclick", onclick, null);
            }
        }

        encodeMenuItemContent(context, menu, menuitem);

        writer.endElement("a");
    }

    @Override
    protected void encodeMenuItemContent(FacesContext context, AbstractMenu menu, MenuItem menuitem) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        String icon = menuitem.getIcon();
        Object value = menuitem.getValue();

        if (menuitem instanceof UIMenuItem) {
            encodeBadge(context, ((UIMenuItem) menuitem).getAttributes().get("badge"));
        }
        encodeItemIcon(context, icon);

        if (value != null) {
            writer.startElement("span", null);
            writer.writeText(value, "value");
            writer.endElement("span");
        }
    }

    @Override
    protected void encodeScript(FacesContext context, AbstractMenu abstractMenu) throws IOException {
        LearnwebMenu menu = (LearnwebMenu) abstractMenu;
        WidgetBuilder wb = getWidgetBuilder(context);
        wb.init("LearnwebMenu", menu);
        wb.finish();
    }

    protected String createAjaxRequest(FacesContext context, AjaxSource source, UIComponent form) {
        UIComponent component = (UIComponent) source;
        String clientId = component.getClientId(context);

        AjaxRequestBuilder builder = PrimeRequestContext.getCurrentInstance(context).getAjaxRequestBuilder();

        builder.init()
            .source(clientId)
            .form(SearchExpressionUtils.resolveClientId(source.getForm(), component))
            .process(component, source.getProcess())
            .update(component, source.getUpdate())
            .async(source.isAsync())
            .global(source.isGlobal())
            .delay(source.getDelay())
            .timeout(source.getTimeout())
            .partialSubmit(source.isPartialSubmit(), source.isPartialSubmitSet(), source.getPartialSubmitFilter())
            .resetValues(source.isResetValues(), source.isResetValuesSet())
            .ignoreAutoUpdate(source.isIgnoreAutoUpdate())
            .onstart(source.getOnstart())
            .onerror(source.getOnerror())
            .onsuccess(source.getOnsuccess())
            .oncomplete(source.getOncomplete())
            .params(component);

        if (form != null) {
            builder.form(form.getClientId(context));
        }

        builder.preventDefault();

        return builder.build();
    }

    protected String createAjaxRequest(FacesContext context, AbstractMenu menu, AjaxSource source, UIComponent form,
        Map<String, List<String>> params) {

        String clientId = menu.getClientId(context);

        AjaxRequestBuilder builder = PrimeRequestContext.getCurrentInstance(context).getAjaxRequestBuilder();

        builder.init()
            .source(clientId)
            .process(menu, source.getProcess())
            .update(menu, source.getUpdate())
            .async(source.isAsync())
            .global(source.isGlobal())
            .delay(source.getDelay())
            .timeout(source.getTimeout())
            .partialSubmit(source.isPartialSubmit(), source.isPartialSubmitSet(), source.getPartialSubmitFilter())
            .resetValues(source.isResetValues(), source.isResetValuesSet())
            .ignoreAutoUpdate(source.isIgnoreAutoUpdate())
            .onstart(source.getOnstart())
            .onerror(source.getOnerror())
            .onsuccess(source.getOnsuccess())
            .oncomplete(source.getOncomplete())
            .params(params);

        if (form != null) {
            builder.form(form.getClientId(context));
        }

        builder.preventDefault();

        return builder.build();
    }
}
