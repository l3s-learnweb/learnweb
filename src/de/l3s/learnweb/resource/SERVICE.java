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

    private SERVICE(String label)
    {
        this.label = label;
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

        /*
        switch(this)
        {
        case bing:
            return "Bing";
        case flickr:
            return "Flickr";
        case youtube:
            return "YouTube";
        case vimeo:
            return "Vimeo";
        case ipernity:
            return "Ipernity";
        case ted:
            return "TED";
        case tedx:
            return "TEDx";
        case loro:
            return "LORO";
        case yovisto:
            return "Yovisto";
        case learnweb:
            return "LearnWeb";
        case archiveit:
            return "Archive-It";
        case teded:
            return "TED-Ed";
        case factcheck:
            return "Fact-Check";
        case speechrepository:
            return "Speech-Repository";
        default:
            return this.name();
        }
        */
    }
}
