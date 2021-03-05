package de.l3s.learnweb.resource;

/**
 * List of possible resource types.
 *
 * Used in database ENUMs: lw_resource.type
 */
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
}
