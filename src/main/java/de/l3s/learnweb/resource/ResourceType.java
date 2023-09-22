package de.l3s.learnweb.resource;

import de.l3s.interweb.core.search.ContentType;

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
    survey, // SurveyResource
    glossary; // GlossaryResource

    @Override
    public String toString() {
        return switch (this) {
            case text -> "Document";
            default -> super.toString();
        };
    }

    public static ResourceType fromContentType(final ContentType type) {
        return switch (type) {
            case video -> video;
            case audio -> audio;
            case image -> image;
            case presentation -> presentation;
            default -> website;
        };
    }
}
