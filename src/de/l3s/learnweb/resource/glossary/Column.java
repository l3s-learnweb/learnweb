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

    private final String localeName;

    Column(String localeName) {
        this.localeName = localeName;
    }

    public String getLocaleName() {
        return localeName;
    }

    @Override
    public String toString() {
        return localeName;
    }
}
