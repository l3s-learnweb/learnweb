package de.l3s.learnweb.component;

import org.primefaces.component.api.UIOutcomeTarget;
import org.primefaces.model.menu.MenuElement;
import org.primefaces.model.menu.Submenu;

import javax.faces.component.UIComponent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ActiveSubMenu implements Submenu, UIOutcomeTarget, Serializable
{
    private static final long serialVersionUID = -6646980546205592030L;

    private String id;
    private String style;
    private String styleClass;
    private String icon;
    private String iconPos;
    private String label;
    private String href;
    private String target;
    private boolean disabled;
    private boolean rendered = true;
    private boolean expanded = false;
    private String outcome;
    private boolean includeViewParams;
    private String fragment;
    private boolean disableClientWindow;
    private Map<String, List<String>> params;
    private transient List<MenuElement> elements;

    public ActiveSubMenu()
    {
        this.elements = new ArrayList<>();
    }

    public String getId()
    {
        return this.id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getStyle()
    {
        return this.style;
    }

    public void setStyle(String style)
    {
        this.style = style;
    }

    public String getStyleClass()
    {
        return this.styleClass;
    }

    public void setStyleClass(String styleClass)
    {
        this.styleClass = styleClass;
    }

    public String getIcon()
    {
        return this.icon;
    }

    public void setIcon(String icon)
    {
        this.icon = icon;
    }

    public String getIconPos()
    {
        return iconPos;
    }

    public void setIconPos(final String iconPos)
    {
        this.iconPos = iconPos;
    }

    public String getLabel()
    {
        return this.label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public String getHref()
    {
        return href;
    }

    public void setHref(final String href)
    {
        this.href = href;
    }

    public String getTarget()
    {
        return target;
    }

    public void setTarget(final String target)
    {
        this.target = target;
    }

    public boolean isDisabled()
    {
        return this.disabled;
    }

    public void setDisabled(boolean disabled)
    {
        this.disabled = disabled;
    }

    public List<MenuElement> getElements()
    {
        return this.elements;
    }

    public void setElements(List<MenuElement> elements)
    {
        this.elements = elements;
    }

    @Override
    public int getElementsCount()
    {
        return elements == null ? 0 : elements.size();
    }

    public boolean isRendered()
    {
        return this.rendered;
    }

    public void setRendered(boolean rendered)
    {
        this.rendered = rendered;
    }

    public boolean isExpanded()
    {
        return this.expanded;
    }

    public void setExpanded(boolean expanded)
    {
        this.expanded = expanded;
    }

    public Object getParent()
    {
        return null;
    }

    public String getClientId()
    {
        return this.id;
    }

    public String getOutcome()
    {
        return outcome;
    }

    public void setOutcome(final String outcome)
    {
        this.outcome = outcome;
    }

    public boolean isIncludeViewParams()
    {
        return includeViewParams;
    }

    public void setIncludeViewParams(final boolean includeViewParams)
    {
        this.includeViewParams = includeViewParams;
    }

    public String getFragment()
    {
        return fragment;
    }

    public void setFragment(final String fragment)
    {
        this.fragment = fragment;
    }

    public boolean isDisableClientWindow()
    {
        return disableClientWindow;
    }

    public void setDisableClientWindow(final boolean disableClientWindow)
    {
        this.disableClientWindow = disableClientWindow;
    }

    public Map<String, List<String>> getParams()
    {
        return params;
    }

    public void setParams(final Map<String, List<String>> params)
    {
        this.params = params;
    }

    public void setParam(String key, Object value)
    {
        if(value == null)
        {
            throw new IllegalArgumentException("value cannot be null");
        }
        else
        {
            if(this.params == null)
            {
                this.params = new LinkedHashMap<>();
            }

            if(!this.params.containsKey(key))
            {
                this.params.put(key, new ArrayList<>());
            }

            this.params.get(key).add(value.toString());
        }
    }

    public List<UIComponent> getChildren()
    {
        return Collections.emptyList();
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private ActiveSubMenu subMenu;

        private Builder()
        {
            this.subMenu = new ActiveSubMenu();
        }

        public Builder id(String id)
        {
            this.subMenu.setId(id);
            return this;
        }

        public Builder style(String style)
        {
            this.subMenu.setStyle(style);
            return this;
        }

        public Builder styleClass(String styleClass)
        {
            this.subMenu.setStyleClass(styleClass);
            return this;
        }

        public Builder icon(String icon)
        {
            this.subMenu.setIcon(icon);
            return this;
        }

        public Builder iconPos(String iconPos)
        {
            this.subMenu.setIconPos(iconPos);
            return this;
        }

        public Builder label(String label)
        {
            this.subMenu.setLabel(label);
            return this;
        }

        public Builder url(String url)
        {
            this.subMenu.setHref(url);
            return this;
        }

        public Builder target(String target)
        {
            this.subMenu.setTarget(target);
            return this;
        }

        public Builder disabled(boolean disabled)
        {
            this.subMenu.setDisabled(disabled);
            return this;
        }

        public Builder elements(List<MenuElement> elements)
        {
            this.subMenu.setElements(elements);
            return this;
        }

        public Builder addElement(MenuElement element)
        {
            this.subMenu.getElements().add(element);
            return this;
        }

        public Builder rendered(boolean rendered)
        {
            this.subMenu.setRendered(rendered);
            return this;
        }

        public Builder expanded(boolean expanded)
        {
            this.subMenu.setExpanded(expanded);
            return this;
        }

        public Builder outcome(String outcome)
        {
            this.subMenu.setOutcome(outcome);
            return this;
        }

        public Builder includeViewParams(boolean includeViewParams)
        {
            this.subMenu.setIncludeViewParams(includeViewParams);
            return this;
        }

        public Builder fragment(String fragment)
        {
            this.subMenu.setFragment(fragment);
            return this;
        }

        public Builder params(Map<String, List<String>> params)
        {
            this.subMenu.setParams(params);
            return this;
        }

        public Builder disableClientWindow(boolean disableClientWindow)
        {
            this.subMenu.setDisableClientWindow(disableClientWindow);
            return this;
        }

        public ActiveSubMenu build()
        {
            return this.subMenu;
        }
    }
}
