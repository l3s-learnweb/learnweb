package de.l3s.learnweb.resource.glossary;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import de.l3s.learnweb.resource.File;
import de.l3s.util.StringHelper;

public class GlossaryTableView implements Serializable {
    @Serial
    private static final long serialVersionUID = -757320545292668593L;

    private GlossaryEntry entry;
    private GlossaryTerm term;

    private transient Map<String, Pattern> highlightQueries; // filter queries by field, "fulltext" is the global filter; the rows are recreated on each load
    private transient Map<String, String> htmlCache; // JSF evaluates the values several times per request

    public GlossaryTableView() {
        // required by Serializable
    }

    public GlossaryTableView(GlossaryEntry entry, GlossaryTerm term) {
        this.entry = entry;
        this.term = term;
    }

    /**
     * @param highlightQueries the queries of the filters by field (see {@link StringHelper#compileHighlightQuery}), "fulltext" is the global filter
     */
    public GlossaryTableView(GlossaryEntry entry, GlossaryTerm term, Map<String, Pattern> highlightQueries) {
        this.entry = entry;
        this.term = term;
        this.highlightQueries = highlightQueries;
    }

    /**
     * @param field the filter field of the value, its column filter takes precedence over the global filter
     * @return the escaped value as HTML, with the filter query highlighted if available
     */
    private String highlight(String field, String value) {
        if (value == null) {
            return "";
        }

        return cachedHtml(field, () -> {
            Pattern query = highlightQueries == null ? null : highlightQueries.getOrDefault(field, highlightQueries.get("fulltext"));
            return query == null ? StringHelper.escapeHtml(value) : StringHelper.highlightQuery(value, query);
        });
    }

    private String cachedHtml(String key, Supplier<String> html) {
        if (htmlCache == null) {
            htmlCache = new HashMap<>();
        }
        return htmlCache.computeIfAbsent(key, k -> html.get());
    }

    public int getEntryId() {
        return entry.getId();
    }

    public String getTopicOne() {
        return entry.getTopicOne();
    }

    public String getTopicOneHtml() {
        return highlight("topicOne", getTopicOne());
    }

    public String getTopicTwo() {
        return entry.getTopicTwo();
    }

    public String getTopicTwoHtml() {
        return highlight("topicTwo", getTopicTwo());
    }

    public String getTopicThree() {
        return entry.getTopicThree();
    }

    public String getTopicThreeHtml() {
        return highlight("topicThree", getTopicThree());
    }

    public String getDescription() {
        return entry.getDescription();
    }

    public String getDescriptionHtml() {
        return highlight("description", getDescription());
    }

    public List<File> getPictures() {
        return entry.getPictures();
    }

    public int getPicturesCount() {
        return entry.getPicturesCount();
    }

    public String getTerm() {
        return term.getTerm();
    }

    public String getTermHtml() {
        return highlight("term", getTerm());
    }

    public int getTermId() {
        return term.getId();
    }

    public Locale getLanguage() {
        return term.getLanguage();
    }

    public String getUses() {
        return StringUtils.join(term.getUses(), ", ");
    }

    public String getPronounciation() {
        return term.getPronounciation();
    }

    public String getAcronym() {
        return term.getAcronym();
    }

    public String getAcronymHtml() {
        return highlight("acronym", getAcronym());
    }

    public String getSource() {
        return term.getSource();
    }

    /**
     * The source column is filtered by a dropdown of the stored values, therefore only the global filter is highlighted.
     * The global filter matches the stored value, the label is bold as a whole if only the stored value matches (e.g. a translated label).
     *
     * @param label the displayed label of the source (see {@link GlossaryBean#getSourceLabel})
     * @return the escaped label as HTML, with the global filter query highlighted if available
     */
    public String getSourceHtml(String label) {
        if (label == null) {
            return "";
        }

        return cachedHtml("source:" + label, () -> {
            Pattern query = highlightQueries == null ? null : highlightQueries.get("fulltext");
            if (query == null) {
                return StringHelper.escapeHtml(label);
            }
            if (query.matcher(label).find()) {
                return StringHelper.highlightQuery(label, query);
            }
            if (getSource() != null && query.matcher(getSource()).find()) {
                return "<b>" + StringHelper.escapeHtml(label) + "</b>";
            }
            return StringHelper.escapeHtml(label);
        });
    }

    public String getPhraseology() {
        return term.getPhraseology();
    }

    public String getPhraseologyHtml() {
        return highlight("phraseology", getPhraseology());
    }

    public LocalDateTime getTimestamp() {
        return entry.getCreatedAt();
    }

    public GlossaryEntry getEntry() {
        return entry;
    }

    public String getFulltext() {
        return entry.getFulltext();
    }

    public String getTopics() {
        StringBuilder sb = new StringBuilder(getEntry().getTopicOne());

        if (StringUtils.isNotBlank(getEntry().getTopicTwo())) {
            sb.append(" - ");
            sb.append(getEntry().getTopicTwo());
        }

        if (StringUtils.isNotBlank(getEntry().getTopicThree())) {
            sb.append(" - ");
            sb.append(getEntry().getTopicThree());
        }

        return StringHelper.shortnString(sb.toString(), 20);
    }
}
