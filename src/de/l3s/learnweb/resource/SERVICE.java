package de.l3s.learnweb.resource;

public enum SERVICE // when adding more services remember to update the service column of learnweb_large.sl_query
{
    bing, // Does not support filtering by date
    flickr,
    youtube,
    vimeo, // Does not support filtering by date
    ipernity,
    ted, // stored in SOLR
    tedx, // stored in SOLR
    loro, // stored in SOLR
    yovisto, //  stored in SOLR
    learnweb, // stored in SOLR
    archiveit, // stored in SOLR
    teded, // stored in SOLR
    factcheck, // stored in SOLR
    desktop, // only used as source
    internet, // only used as source
    slideshare;

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

    @Override
    public String toString()
    {
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
            return "Fact Check";
        default:
            return this.name();
        }
    }
}
