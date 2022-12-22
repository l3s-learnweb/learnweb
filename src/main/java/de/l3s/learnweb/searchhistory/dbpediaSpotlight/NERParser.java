package de.l3s.learnweb.searchhistory.dbpediaSpotlight;

import static de.l3s.learnweb.app.Learnweb.dao;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.math3.util.Precision.round;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.l3s.learnweb.searchhistory.AnnotationCount;
import de.l3s.learnweb.searchhistory.Pkg;
import de.l3s.learnweb.searchhistory.dbpediaSpotlight.common.AnnotationUnit;
import de.l3s.learnweb.searchhistory.dbpediaSpotlight.common.ResourceItem;
import de.l3s.learnweb.searchhistory.dbpediaSpotlight.rest.SpotlightBean;

/*
 * Specific class for calling Named Entity Recognition (NER)
 * @author Trung Tran
 * */
public final class NERParser {
    private static AnnotationUnit annotationUnit;

    /*
     * Getting resources of dbpedia-spotlight result
     * @param AnnotationUnit    the dbpedia-spotlight process
     * @param id    the source's id
     * @param type  the type of the annotation (user, group, web, snippet_clicked, snippet_notClicked, query)
     * @param user  the user's username
     * @param sessionId     the session id of the current annotation
     * @return  the list of annotationCount with its newly created values
     */
    private static List<AnnotationCount> annotate(AnnotationUnit annotationUnit, int id, String type, String user, String sessionId, String keywords) {
        List<ResourceItem> resources = new ArrayList<>();
        List<AnnotationCount> annotationCounts = new ArrayList<>();
        //If annotating from webpages, only choose top 5 per webpage
        if (type.equals("web")) {
            Map<String, Long> uriPerType = annotationUnit.getResources().stream()
                .collect(groupingBy(ResourceItem::getUri, counting()));

            final List<ResourceItem> finalResources = resources;
            uriPerType.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .forEach(r -> {
                    Optional<ResourceItem> resource =  annotationUnit.getResources().stream().filter(s -> s.getUri().equals(r.getKey())).findFirst();
                    if (resource.isPresent()) finalResources.add(resource.get());
                });
            resources = finalResources;
        }
        else {
            resources = annotationUnit.getResources();
        }
        for (ResourceItem resource : resources) {
            annotationCounts.add(new AnnotationCount(String.valueOf(id), resource.score(), resource.getSurfaceForm(), resource.getUri(), type,
                user, sessionId, keywords));
        }
        return annotationCounts;
    }

    /*
     * Main function of the class
     * @param sessionId     the session id of the current annotation
     * @param id    the id of the source (user, group)
     * @param username  the user's username
     * @param type  the type of the annotation (user, group, web, snippet_clicked, snippet_notClicked, query)
     * @param content   the content to be recognized
     * */
    public static void processQuery(String sessionId, int id, String username, String type, String content) throws Exception {
        if (content == null) return;
        List<AnnotationCount> annotationCounts = new ArrayList<>();
        SpotlightBean spotlight = new SpotlightBean();
        //Parse the content to dbpedia-spotlight
        annotationUnit = spotlight.get(content);
        //Parse the results to the function
        if (annotationUnit.getResources() != null) {
            annotationCounts = annotate(annotationUnit, id, type, username, sessionId, "");
        }
        //Remove all annotations with confidence < 0.9
        annotationCounts.removeIf(annotationCount -> annotationCount.getConfidence() < 0.9);


        //Insert inputStream into DB
        int userId = dao().getUserDao().findByUsername(username).get().getId();
        int inputId = dao().getSearchHistoryDao().insertInputStream(userId, type, content);
        //Store this annotationCount into DB
        for (AnnotationCount annotationCount : annotationCounts) {
            annotationCount.setConfidence(round(annotationCount.getConfidence(),2));
            Optional<AnnotationCount> foundAnnotation = dao().getSearchHistoryDao().findByUriAndType(annotationCount.getUri(), annotationCount.getType());
            //If already an annotationCount is found in DB, update its columns
            if (foundAnnotation.isPresent()) {
                //Update the sessionId
                String session = foundAnnotation.get().getSessionId();
                if (!session.contains(annotationCount.getSessionId())) {
                    session += "," + annotationCount.getSessionId();
                }
                //Update the User
                String users = foundAnnotation.get().getUsers();
                if (!users.contains(annotationCount.getUsers())) {
                    users += "," + annotationCount.getUsers();
                }
                //Update the InputStream
                String inputStreams = foundAnnotation.get().getInputStreams();
                if (!inputStreams.contains(String.valueOf(inputId))) {
                    inputStreams += "," + inputId;
                }
                //Update the repetition
                dao().getSearchHistoryDao().updateQueryAnnotation(foundAnnotation.get().getRepetition() + 1, session, users,
                    inputStreams, annotationCount.getUri(), annotationCount.getType());
            }
            //Insert directly new annotationCount into DB
            else {
                dao().getSearchHistoryDao().insertQueryToAnnotation(String.valueOf(inputId), annotationCount.getId(),
                    annotationCount.getType(), annotationCount.getUri(), annotationCount.getCreatedAt(), annotationCount.getSurfaceForm()
                    , annotationCount.getSessionId(), annotationCount.getUsers(), annotationCount.getConfidence(), annotationCount.getRepetition());
                Pkg.instance.updatePkg(annotationCount, dao().getUserDao().findByUsername(username).get());
            }
        }

    }

}
