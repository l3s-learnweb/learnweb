package de.l3s.learnweb.component;

import org.primefaces.model.menu.DefaultMenuItem;
import org.primefaces.model.menu.MenuElement;
import org.primefaces.model.menu.Submenu;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ActiveSubMenu extends DefaultMenuItem implements Submenu, Serializable
{
    private static final long serialVersionUID = -6646980546205592030L;

    private List<MenuElement> elements;
    private boolean expanded = false;

    public ActiveSubMenu() {
        elements = new ArrayList<>();
    }

    public ActiveSubMenu(Object value) {
        super(value);
        elements = new ArrayList<>();
    }

    public ActiveSubMenu(Object value, String icon) {
        super(value, icon);
        elements = new ArrayList<>();
    }

    public ActiveSubMenu(Object value, String icon, String url) {
        super(value, icon, url);
        elements = new ArrayList<>();
    }

    @Override
    public List<MenuElement> getElements() {
        return elements;
    }

    public void setElements(List<MenuElement> elements) {
        this.elements = elements;
    }

    @Override
    public int getElementsCount() {
        return (elements == null) ? 0 : elements.size();
    }

    @Override
    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    @Override
    public String getLabel()
    {
        return getValue().toString();
    }

    @Override
    public Object getParent() {
        return null;
    }

    public void addElement(MenuElement element) {
        elements.add(element);
    }
}
