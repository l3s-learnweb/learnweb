package de.l3s.learnweb.component;

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;
import jakarta.faces.render.FacesRenderer;
import jakarta.faces.render.Renderer;

import org.apache.commons.lang3.StringUtils;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import net.sf.extjwnl.util.cache.LRUCache;

@FacesRenderer(componentFamily = Markdown.COMPONENT_FAMILY, rendererType = Markdown.DEFAULT_RENDERER)
public class MarkdownRenderer extends Renderer<Markdown> {

    private static final Parser parser = Parser.builder().build();
    private static final HtmlRenderer renderer = HtmlRenderer.builder().build();
    private static final LRUCache<Integer, String> cache = new LRUCache<>(100);

    @Override
    public void encodeEnd(FacesContext context, Markdown component) throws IOException {
        String styleClass = Stream.of(Markdown.STYLE_CLASS, component.getStyleClass())
                .filter(s -> s != null && !s.isEmpty()).collect(Collectors.joining(" "));

        ResponseWriter writer = context.getResponseWriter();
        writer.startElement("div", component);
        writer.writeAttribute("id", component.getClientId(context), "id");
        writer.writeAttribute("class",  styleClass, "styleClass");
        if (StringUtils.isNotEmpty(component.getStyle())) {
            writer.writeAttribute("style", component.getStyle(), "style");
        }
        writer.write(renderMarkdown(component.getValue()));
        writer.endElement("div");
    }

    private String renderMarkdown(final String text) {
        return cache.computeIfAbsent(text.hashCode(), key -> {
            org.commonmark.node.Node paragraphs = parser.parse(text);
            return renderer.render(paragraphs);
        });
    }
}
