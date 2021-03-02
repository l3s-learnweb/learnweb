package de.l3s.learnweb.resource;

public enum ResourceType {
    text,
    video,
    image,
    audio,
    pdf,
    website,
    spreadsheet,
    presentation,
    document,
    file, // applications, archives, etc

    // learnweb types
    survey,
    glossary;

    @Override
    public String toString() {
        switch (this) {
            case text:
                return "Document";
            default:
                return super.toString();
        }
    }

    /**
     * Same as ResourceType.valueOf(), but case insensitive.
     */
    public static ResourceType parse(String value) {
        return valueOf(value.toLowerCase());
    }
}
