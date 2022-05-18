package de.l3s.learnweb.searchhistory.dbpediaSpotlight;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

import java.net.URL;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import de.l3s.learnweb.searchhistory.dbpediaSpotlight.common.AnnotationUnit;
import de.l3s.learnweb.searchhistory.dbpediaSpotlight.common.ResourceItem;
import de.l3s.learnweb.searchhistory.dbpediaSpotlight.rest.SpotlightBean;

public class Parser {
    private static AnnotationUnit annotationUnit;

    public static void main(String[] args) throws Exception {
        SpotlightBean spotlight = new SpotlightBean();

        /**
         * Classify by text
         */
        String text = "Berlin is the capital of Germany Berlin";
        annotationUnit = spotlight.get(text);
        print(annotationUnit);

        /**
         * Classify by URL
         */
        URL url = new URL("https://en.wikipedia.org/wiki/Cat");
        Document doc = Jsoup.parse(url, 3 * 1000);
        String textFromUrl = doc.text();
        annotationUnit = spotlight.get(textFromUrl);
        print(annotationUnit);
    }

    private static void print(AnnotationUnit annotationUnit) {
        Set<String> nameSet = new HashSet<>();

        AtomicInteger counter = new AtomicInteger(0);

        long total = annotationUnit.getResources().stream().parallel().count();

        Comparator<ResourceItem> compareByURI = Comparator
            .comparing(ResourceItem::getUri);
        Comparator<ResourceItem> compareBySimScore = Comparator
            .comparing(ResourceItem::getSimilarityScore);
        Comparator<ResourceItem> compareByUriThenSurfaceForm = Comparator
            .comparing(ResourceItem::getUri).thenComparing(ResourceItem::getSurfaceForm);

        Predicate<ResourceItem> filterBySimScore = r -> Double.parseDouble(r.getSimilarityScore()) >= 0.9;
        Predicate<ResourceItem> filterBySurfaceForm = r -> nameSet.add(r.getSurfaceForm());
        Predicate<ResourceItem> filterByURI = r -> nameSet.add(r.getUri());

        System.out.println("Total: " + total);

        /**
         * Show retrieved list from DBpedia Spotlight with sorting out by URI
         */
        /*List<ResourceItem> resourceItemsList = annotationUnit.getResources().stream()
            .sorted(compareByUriThenSurfaceForm)
            .collect(Collectors.toList());

        for (ResourceItem item : resourceItemsList) {
            System.out.println("URI: " + item.getUri() + ", surface form: " + item.getSurfaceForm() + ", similarity score: " + item.getSimilarityScore());
            counter.incrementAndGet();
        }*/

        /**
         * Show retrieved list from DBpedia Spotlight with filtering - one surface form with the best similarity score
         */
        /*List<ResourceItem> resourceItemsList = annotationUnit.getResources().stream()
            .filter(r -> nameSet.add(r.getSurfaceForm()))
            .sorted(compareByUri)
            .collect(Collectors.toList());

        for (ResourceItem item : resourceItemsList) {
            System.out.println("URI: " + item.getUri() + ", surface form: " + item.getSurfaceForm() + ", similarity score: " + item.getSimilarityScore());
            counter.incrementAndGet();
        }*/

        /**
         * Show retrieved list from DBpedia Spotlight by filtering similarity score >= 0.85 && one surface form with the best similarity score
         */
        /*List<ResourceItem> resourceItemsList = annotationUnit.getResources().stream()
            .filter(filterBySimScore.and(filterByURI))
            .sorted(compareByURI)
            .collect(Collectors.toList());

        for (ResourceItem item : resourceItemsList) {
            System.out.println("URI: " + item.getUri() + ", surface form: " + item.getSurfaceForm() + ", similarity score: " + item.getSimilarityScore());
            counter.incrementAndGet();
        }

        System.out.println("Total after filtering out: " + counter);*/

        /**
         * Number of occurrences per URI
         */
        Map<String, Long> uriPerType = annotationUnit.getResources().stream()
            .collect(groupingBy(ResourceItem::getUri, counting()));

        uriPerType.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .forEach(r -> System.out.println("URI: " + r.getKey() + ", occurrences: " + r.getValue()));
    }
}
