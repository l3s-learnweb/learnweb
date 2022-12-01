package de.l3s.mail.message;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import de.l3s.learnweb.LanguageBundle;

public abstract class Element {

    private String inlineStyle;
    private String styleClass;

    public Element inlineStyle(String style) {
        this.inlineStyle = style;
        return this;
    }

    public Element styleClass(String styleClass) {
        this.styleClass = styleClass;
        return this;
    }

    /**
     * Builds an HTML representation of this element and appends it to the given StringBuilder.
     */
    protected abstract void buildHtml(StringBuilder sb, LanguageBundle msg);

    /**
     * Builds a plain text representation of this element and appends it to the given StringBuilder.
     */
    protected abstract void buildPlainText(StringBuilder sb, LanguageBundle msg);

    /**
     * Helper method which stringifies list of attributes for the tag.
     */
    protected StringBuilder buildAttributes(final Map<String, String> attributes) {
        HashMap<String, String> finalAttributes = new HashMap<>();
        if (attributes != null && !attributes.isEmpty()) {
            finalAttributes.putAll(attributes);
        }

        finalAttributes.put("style", inlineStyle);
        finalAttributes.put("class", styleClass);

        StringBuilder sb = new StringBuilder();
        finalAttributes.forEach((attr, value) -> {
            if (StringUtils.isNoneBlank(attr, value)) {
                sb.append(' ').append(attr).append('=').append('"').append(value).append('"');
            }
        });
        return sb;
    }

    /**
     * Helper method which stringifies only the {@code style} and {@code class} attributes for the tag.
     */
    protected StringBuilder buildAttributes() {
        return buildAttributes(null);
    }

    public String getInlineStyle() {
        return inlineStyle;
    }

    public String getStyleClass() {
        return styleClass;
    }
}
