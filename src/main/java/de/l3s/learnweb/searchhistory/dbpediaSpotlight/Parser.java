package de.l3s.learnweb.searchhistory.dbpediaSpotlight;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

import java.net.URL;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import de.l3s.learnweb.searchhistory.dbpediaSpotlight.common.AnnotationUnit;
import de.l3s.learnweb.searchhistory.dbpediaSpotlight.common.ResourceItem;
import de.l3s.learnweb.searchhistory.dbpediaSpotlight.rest.SpotlightBean;

public class Parser {
    private static AnnotationUnit annotationUnit;

    public static void main(String[] args) throws Exception {
        SpotlightBean spotlight = new SpotlightBean();

        //Classify by plain text
        String text = "Berlin is the capital of Germany Berlin";
        annotationUnit = spotlight.get(text);
        print(annotationUnit);

        //Classify by URL
        URL url = new URL("https://en.wikipedia.org/wiki/Cat");
        Document doc = Jsoup.parse(url, 3 * 1000);
        String textFromUrl = doc.text();
        annotationUnit = spotlight.get(textFromUrl);
        print(annotationUnit);
    }

    /**
     * Print number of occurrences per URI
     *
     * @author Tetiana Tolmachova
     */
    private static void print(AnnotationUnit annotationUnit) {
        long total = annotationUnit.getResources().stream().parallel().count();
        System.out.println("Total: " + total);

        Map<String, Long> uriPerType = annotationUnit.getResources().stream()
            .collect(groupingBy(ResourceItem::getUri, counting()));

        uriPerType.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .forEach(r -> System.out.println("URI: " + r.getKey() + ", occurrences: " + r.getValue()));
    }
}
