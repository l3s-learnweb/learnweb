package de.l3s.learnweb.searchhistory;

import de.l3s.learnweb.resource.ResourceDecorator;

/**
 * A search result as it is stored in the search history, taken from the result when it was loaded.
 *
 * @param resourceId the id of the result if it is stored in Learnweb, then its other details aren't stored; 0 otherwise
 */
public record SearchHistoryResult(int rank, int resourceId, String url, String title, String description,
                                  String thumbnailUrl, int thumbnailHeight, int thumbnailWidth) {

    /**
     * The details are copied, because the search page keeps changing the resource, e.g. saves it when the user adds it to a group.
     */
    public static SearchHistoryResult of(ResourceDecorator result) {
        int resourceId = result.getResource().getId();
        if (resourceId != 0) { // reading the details of a stored resource may load its files
            return new SearchHistoryResult(result.getRank(), resourceId, null, null, null, null, 0, 0);
        }

        return new SearchHistoryResult(result.getRank(), 0, result.getUrl(), result.getTitle(), result.getDescription(),
            result.getThumbnailMedium(), result.getHeight(), result.getWidth());
    }
}
