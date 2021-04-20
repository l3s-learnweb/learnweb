package de.l3s.learnweb.resource.glossary;

public enum Column {
    topicOne("Glossary.first_topic"),
    topicTwo("Glossary.second_topic"),
    topicThree("Glossary.third_topic"),
    description("Glossary.Definition"),
    term("Glossary.term"),
    language("language"),
    uses("Glossary.use"),
    pronounciation("Glossary.pronounciation"),
    acronym("Glossary.acronym"),
    source("source"),
    phraseology("Glossary.phraseology");

    private final String msgKey;

    Column(String msgKey) {
        this.msgKey = msgKey;
    }

    /**
     * The msgKey that shall be used to retrieve the columns translated name
     */
    public String getMsgKey() {
        return msgKey;
    }

    /**
     * Convenience method to get the translated column name on the frontend through #{msg[Column.topicOne]}
     */
    @Override
    public String toString() {
        return msgKey;
    }
}
