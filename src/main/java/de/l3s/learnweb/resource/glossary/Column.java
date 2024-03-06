package de.l3s.learnweb.resource.glossary;

public enum Column {
    topicOne("glossary.first_topic"),
    topicTwo("glossary.second_topic"),
    topicThree("glossary.third_topic"),
    description("glossary.definition"),
    term("glossary.term"),
    language("language"),
    uses("glossary.use"),
    pronounciation("glossary.pronounciation"),
    acronym("glossary.acronym"),
    source("source"),
    phraseology("glossary.phraseology");

    private final String msgKey;

    Column(String msgKey) {
        this.msgKey = msgKey;
    }

    /**
     * The msgKey that shall be used to retrieve the columns translated name.
     */
    public String getMsgKey() {
        return msgKey;
    }

    /**
     * Convenience method to get the translated column name on the frontend through #{msg[Column.topicOne]}.
     */
    @Override
    public String toString() {
        return msgKey;
    }
}
