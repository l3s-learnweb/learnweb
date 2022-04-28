package de.l3s.learnweb.searchhistory.dbpediaSpotlight;

import java.net.URL;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import de.l3s.learnweb.searchhistory.dbpediaSpotlight.common.AnnotationUnit;
import de.l3s.learnweb.searchhistory.dbpediaSpotlight.common.ResourceItem;
import de.l3s.learnweb.searchhistory.dbpediaSpotlight.rest.SpotlightBean;

public class Example {
    public static void main(String[] args) throws Exception {
        SpotlightBean spotlight = new SpotlightBean();

        URL url = new URL("https://en.wikipedia.org/wiki/Cat");
        Document doc = Jsoup.parse(url, 3 * 1000);
        String text = doc.text();

        AnnotationUnit annotationUnit = spotlight.get(text);
        print(annotationUnit);
    }

    private static void print(AnnotationUnit annotationUnit) {
        AtomicInteger counter = new AtomicInteger(0);

        long total = annotationUnit.getResources().stream().parallel().count();

        Comparator<ResourceItem> compareByUri = Comparator
            .comparing(ResourceItem::getUri);

        Set<String> nameSet = new HashSet<>();

        if (annotationUnit != null) {
            System.out.println("Total: " + total);

            List<ResourceItem> resourceItemsList = annotationUnit.getResources().stream()
                .filter(r -> nameSet.add(r.getSurfaceForm()))
                .sorted(compareByUri)
                .collect(Collectors.toList());

            for (ResourceItem item : resourceItemsList) {
                System.out.println("URI: " + item.getUri() + ", surface form: " + item.getSurfaceForm() + ", similarity score: " + item.getSimilarityScore());
                counter.incrementAndGet();
            }

            System.out.println("Total after filtering out: " + counter);
        }

    }
}
