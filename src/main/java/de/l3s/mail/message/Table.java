package de.l3s.mail.message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.l3s.learnweb.LanguageBundle;

public class Table extends Element {
    private final java.util.List<TableRow> rows = new ArrayList<>();
    private final java.util.List<String> columnClass = new ArrayList<>();
    private final java.util.List<String> titleRowClass = new ArrayList<>();

    public Table addRow(Element... elements) {
        rows.add(new TableRow(elements));
        return this;
    }

    public Table addRow(String... elements) {
        rows.add(new TableRow(elements));
        return this;
    }

    public void setTitleClass(String... titleClass) {
        if (titleClass != null) {
            this.titleRowClass.addAll(Arrays.asList(titleClass));
        }
    }

    public void setColumnClass(String... colClass) {
        if (colClass != null) {
            this.columnClass.addAll(Arrays.asList(colClass));

        }
    }

    private void adjustClassSize(List<String> cssClass, List<Element> rowClass) {
        if (cssClass.size() >= rowClass.size()) {
            return;
        }
        for (int i = cssClass.size(); i < rowClass.size(); i++) {
            cssClass.add("");
        }
    }

    @Override
    protected void buildHtml(final StringBuilder sb, final LanguageBundle msg) {
        if (rows.isEmpty()) {
            return;
        }
        sb.append("<table").append(buildAttributes()).append(">");
        sb.append("<tr>");
        //Header
        List<Element> titleRow = rows.get(0).getElements();
        adjustClassSize(titleRowClass, titleRow);
        for (int i = 0; i < titleRow.size(); i++) {
            if (!titleRowClass.isEmpty() && (titleRowClass.get(i) != null && !titleRowClass.get(i).isEmpty())) {
                sb.append("<th class = \"").append(titleRowClass.get(i)).append("\">");
            } else {
                sb.append("<th>");
            }
            titleRow.get(i).buildHtml(sb, msg);
            sb.append("</th>");
        }
        sb.append("</tr>");
        //Element cells
        for (int i = 1; i < rows.size(); i++) {
            sb.append("<tr>");
            TableRow row = rows.get(i);
            adjustClassSize(columnClass, row.getElements());
            int column = 0;
            for (Element cell : row.getElements()) {
                if (!columnClass.isEmpty() && (columnClass.get(column) == null || columnClass.get(column).isEmpty())) {
                    sb.append("<td>");
                } else {
                    sb.append("<td class = \"").append(columnClass.get(column)).append("\">");
                }
                cell.buildHtml(sb, msg);
                sb.append("</td>");
                column++;
            }
            sb.append("</tr>");
        }
        sb.append("</table>");
    }

    @Override
    protected void buildPlainText(final StringBuilder sb, final LanguageBundle msg) {
        sb.append("\n");
        for (TableRow textRow : rows) {
            for (Element elem : textRow.getElements()) {
                elem.buildPlainText(sb, msg);
                sb.append("\t");
            }
            sb.setLength(sb.length() - 1);
            sb.append("\n");
        }
    }
}
