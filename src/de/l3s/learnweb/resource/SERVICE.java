package de.l3s.learnweb.resource;

public enum SERVICE // when adding more services remember to update the service column of learnweb_large.sl_query
{
    bing("Bing"), // Does not support filtering by date
    flickr("Flickr"),
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

    SERVICE(String label)
    {
        this.label = label;
    }

    /**
     * Same as SERVICE.valueOf(), but removes spaces, dashes and case insensitive.
     */
    public static SERVICE parse(String value) throws IllegalArgumentException {
        return valueOf(value.toLowerCase().replaceAll("[ -]", ""));
    }

    public boolean isLearnwebSource()
    {
        switch(this)
        {
        case bing:
        case flickr:
        case youtube:
        case vimeo:
        case ipernity:
        case slideshare:
            return false;
        default:
            return true;
        }
    }

    public String getLabel()
    {
        return label;
    }

    @Override
    public String toString()
    {
        return label;
    }
}
