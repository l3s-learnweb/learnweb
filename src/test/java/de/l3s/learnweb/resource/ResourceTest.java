package de.l3s.learnweb.resource;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ResourceTest {
    @Test
    void setTitleKeepsPlainText() {
        Resource resource = new Resource();
        // the title is plain text, it must not be parsed as HTML (e.g. again when it is saved from the edit form)
        resource.setTitle("List<String> & Tom &amp; Jerry");
        assertEquals("List<String> & Tom &amp; Jerry", resource.getTitle());

        resource.setTitle("  DE >\n NL ");
        assertEquals("DE > NL", resource.getTitle());

        // the column is NOT NULL, a blank title must not become null
        resource.setTitle(" ");
        assertEquals("", resource.getTitle());
    }

    @Test
    void setSnippetSanitizesHtml() {
        ResourceDecorator decorator = new ResourceDecorator(new Resource());
        // snippets are rendered unescaped, the highlight is kept but scripts are removed
        decorator.setSnippet("<strong>Tom</strong> &amp; Jerry<img src=x onerror=alert(1)>");
        assertEquals("<strong>Tom</strong> &amp; Jerry", decorator.getSnippet());
    }
}
