package de.l3s.learnweb.resource;

import java.util.regex.Pattern;

/**
 * List of possible resource sources.
 *
 * Used in database ENUMs: lw_resource.source
 */
public enum ResourceService { // when adding more services remember to update the service column of learnweb_large.sl_query
    bing("Bing"), // Does not support filtering by date
    flickr("Flickr"),
    giphy("GIPHY"), // The uppercase name is required
    youtube("YouTube"),
    vimeo("Vimeo"), // Does not support filtering by date
    ipernity("Ipernity"),
    ted("TED"), // stored in SOLR
    tedx("TEDx"), // stored in SOLR
    loro("LORO"), // stored in SOLR
    yovisto("Yovisto"), //  stored in SOLR
    learnweb("LearnWeb"), // stored in SOLR
    archiveit("Archive-It"), // stored in SOLR
    teded("TED-Ed"), // stored in SOLR
    factcheck("Fact Check"), // stored in SOLR
    desktop("Desktop"), // only used as source
    internet("Internet"), // only used as source
    slideshare("SlideShare"),
    speechrepository("Speech Repository");

    private final String label;

    ResourceService(String label) {
        this.label = label;
    }

    public boolean isInterweb() {
        switch (this) {
            case bing:
            case giphy:
            case flickr:
            case youtube:
            case vimeo:
            case ipernity:
            case slideshare:
                return true;
            default:
                return false;
        }
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    /**
     * Returns the the location were a resource is stored. Necessary because some external sources are indexed in our Solr instance.
     */
    public String getLocation() {
        switch (this) {
            case teded:
            case ted:
            case tedx:
            case yovisto:
            case archiveit:
            case factcheck:
                return getLabel();
            default:
                return "Learnweb";
        }
    }

    /**
     * Same as valueOf(), but removes spaces and dashes, also case insensitive.
     */
    public static ResourceService parse(String value) {
        return valueOf(Pattern.compile("[ -]").matcher(value.toLowerCase()).replaceAll(""));
    }
}
