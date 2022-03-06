package de.l3s.mail.message;

import java.util.ArrayList;
import java.util.Arrays;

class TableRow {
    private final java.util.List<Element> elements = new ArrayList<>();

    TableRow(Element... elements) {
        if (elements != null) {
            this.elements.addAll(Arrays.asList(elements));
        }
    }

    TableRow(String... elements) {
        if (elements != null) {
            for (String element : elements) {
                this.elements.add(new Text(element));
            }
        }
    }

    public java.util.List<Element> getElements() {
        return elements;
    }
}
