package de.l3s.learnweb.component;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import jakarta.faces.component.UIComponent;

import org.primefaces.component.api.UIOutcomeTarget;
import org.primefaces.model.menu.MenuElement;
import org.primefaces.model.menu.Submenu;

public class ActiveSubmenu implements Submenu, UIOutcomeTarget, Serializable {
    @Serial
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
    private HashMap<String, List<String>> params;
    private ArrayList<MenuElement> elements;

    public ActiveSubmenu() {
        this.elements = new ArrayList<>();
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getStyle() {
        return this.style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    @Override
    public String getStyleClass() {
        return this.styleClass;
    }

    public void setStyleClass(String styleClass) {
        this.styleClass = styleClass;
    }

    @Override
    public String getIcon() {
        return this.icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getIconPos() {
        return iconPos;
    }

    public void setIconPos(final String iconPos) {
        this.iconPos = iconPos;
    }

    @Override
    public String getLabel() {
        return this.label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String getHref() {
        return href;
    }

    public void setHref(final String href) {
        this.href = href;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(final String target) {
        this.target = target;
    }

    @Override
    public boolean isDisabled() {
        return this.disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    @Override
    public ArrayList<MenuElement> getElements() {
        return this.elements;
    }

    public void setElements(ArrayList<MenuElement> elements) {
        this.elements = elements;
    }

    @Override
    public int getElementsCount() {
        return elements == null ? 0 : elements.size();
    }

    @Override
    public boolean isRendered() {
        return this.rendered;
    }

    public void setRendered(boolean rendered) {
        this.rendered = rendered;
    }

    @Override
    public boolean isExpanded() {
        return this.expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    @Override
    public Object getParent() {
        return null;
    }

    @Override
    public String getClientId() {
        return this.id;
    }

    @Override
    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(final String outcome) {
        this.outcome = outcome;
    }

    @Override
    public boolean isIncludeViewParams() {
        return includeViewParams;
    }

    public void setIncludeViewParams(final boolean includeViewParams) {
        this.includeViewParams = includeViewParams;
    }

    @Override
    public String getFragment() {
        return fragment;
    }

    public void setFragment(final String fragment) {
        this.fragment = fragment;
    }

    @Override
    public boolean isDisableClientWindow() {
        return disableClientWindow;
    }

    public void setDisableClientWindow(final boolean disableClientWindow) {
        this.disableClientWindow = disableClientWindow;
    }

    @Override
    public HashMap<String, List<String>> getParams() {
        return params;
    }

    public void setParams(final HashMap<String, List<String>> params) {
        this.params = params;
    }

    public void setParam(String key, Object value) {
        if (value == null) {
            throw new IllegalArgumentException("value cannot be null");
        } else {
            if (this.params == null) {
                this.params = new LinkedHashMap<>();
            }

            this.params.computeIfAbsent(key, k -> new ArrayList<>()).add(value.toString());
        }
    }

    @Override
    public List<UIComponent> getChildren() {
        return Collections.emptyList();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final ActiveSubmenu submenu;

        private Builder() {
            this.submenu = new ActiveSubmenu();
        }

        public Builder id(String id) {
            this.submenu.setId(id);
            return this;
        }

        public Builder style(String style) {
            this.submenu.setStyle(style);
            return this;
        }

        public Builder styleClass(String styleClass) {
            this.submenu.setStyleClass(styleClass);
            return this;
        }

        public Builder icon(String icon) {
            this.submenu.setIcon(icon);
            return this;
        }

        public Builder iconPos(String iconPos) {
            this.submenu.setIconPos(iconPos);
            return this;
        }

        public Builder label(String label) {
            this.submenu.setLabel(label);
            return this;
        }

        public Builder url(String url) {
            this.submenu.setHref(url);
            return this;
        }

        public Builder target(String target) {
            this.submenu.setTarget(target);
            return this;
        }

        public Builder disabled(boolean disabled) {
            this.submenu.setDisabled(disabled);
            return this;
        }

        public Builder elements(ArrayList<MenuElement> elements) {
            this.submenu.setElements(elements);
            return this;
        }

        public Builder addElement(MenuElement element) {
            this.submenu.getElements().add(element);
            return this;
        }

        public Builder rendered(boolean rendered) {
            this.submenu.setRendered(rendered);
            return this;
        }

        public Builder expanded(boolean expanded) {
            this.submenu.setExpanded(expanded);
            return this;
        }

        public Builder outcome(String outcome) {
            this.submenu.setOutcome(outcome);
            return this;
        }

        public Builder includeViewParams(boolean includeViewParams) {
            this.submenu.setIncludeViewParams(includeViewParams);
            return this;
        }

        public Builder fragment(String fragment) {
            this.submenu.setFragment(fragment);
            return this;
        }

        public Builder params(HashMap<String, List<String>> params) {
            this.submenu.setParams(params);
            return this;
        }

        public Builder disableClientWindow(boolean disableClientWindow) {
            this.submenu.setDisableClientWindow(disableClientWindow);
            return this;
        }

        public ActiveSubmenu build() {
            return this.submenu;
        }
    }
}
