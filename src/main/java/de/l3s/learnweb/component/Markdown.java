package de.l3s.learnweb.component;

import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.UIComponentBase;

@FacesComponent(createTag = true, tagName = "outputMarkdown", namespace = "http://l3s.de/learnweb")
public class Markdown extends UIComponentBase {
    public static final String COMPONENT_FAMILY = "de.l3s.learnweb.component";
    public static final String DEFAULT_RENDERER = "de.l3s.learnweb.component.MarkdownRenderer";

    public static final String STYLE_CLASS = "ui-text-markdown";

    public enum PropertyKeys {
        value,
        style,
        styleClass
    }

    public Markdown() {
        setRendererType(DEFAULT_RENDERER);
    }

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    public String getValue() {
        return (String) getStateHelper().eval(PropertyKeys.value, null);
    }

    public void setValue(String label) {
        getStateHelper().put(PropertyKeys.value, label);
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
