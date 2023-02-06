package de.l3s.learnweb.searchhistory.dbpediaspotlight.common;

public interface Prefixes {
    String DBPEDIA_ONTOLOGY = "http://dbpedia.org/ontology/";
    String SCHEMA_ONTOLOGY = "http://schema.org/";

    /**
     * This method is only here to make lint happy
     */
    default void doNothing() {
    }
}
