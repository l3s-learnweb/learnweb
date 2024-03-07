package de.l3s.learnweb.searchhistory.dbpediaspotlight;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.apache.commons.math3.util.Precision;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import de.l3s.learnweb.searchhistory.PKGraphDao;
import de.l3s.learnweb.searchhistory.RecognisedEntity;
import de.l3s.learnweb.searchhistory.dbpediaspotlight.common.AnnotationUnit;
import de.l3s.learnweb.searchhistory.dbpediaspotlight.common.ResourceItem;
import de.l3s.learnweb.searchhistory.dbpediaspotlight.rest.SpotlightClient;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserBean;

@Dependent
public class DbpediaSpotlightService implements Serializable {
    @Serial
    private static final long serialVersionUID = -1169917559922779411L;
    private static final double spotlightRequiredConfidence = 0.9;
    private transient AnnotationUnit annotationUnit;

    @Inject
    private PKGraphDao pkGraphDao;

    @Inject
    private UserBean userBean;

    /**
     * Getting entities from dbpedia-spotlight result.
     *
     * @param annotationUnit the dbpedia-spotlight process
     * @param type the type of the annotation (user, group, web, snippet_clicked, snippet_not_clicked, query)
     * @return the list of annotationCount with its newly created values
     */
    private static List<RecognisedEntity> annotate(AnnotationUnit annotationUnit, String type) {
        List<ResourceItem> resources = new ArrayList<>();
        List<RecognisedEntity> recognisedEntities = new ArrayList<>();

        //If annotating from webpages, only choose top 5 per webpage
        if ("web".equals(type)) {
            Map<String, Long> uriPerType = annotationUnit.getResources().stream()
                .collect(Collectors.groupingBy(ResourceItem::getUri, Collectors.counting()));

            final List<ResourceItem> finalResources = resources;
            uriPerType.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(r -> {
                    Optional<ResourceItem> resource =  annotationUnit.getResources().stream().filter(s -> s.getUri().equals(r.getKey())).findFirst();
                    resource.ifPresent(finalResources::add);
                });
        } else {
            resources = annotationUnit.getResources();
        }

        for (ResourceItem resource : resources) {
            recognisedEntities.add(new RecognisedEntity(resource.score(), resource.getSurfaceForm(), resource.getUri(), type, 0, null));
        }
        return recognisedEntities;
    }

    /**
     * Filter the input website to extract only the important text from html page.
     * Further development can focus on extracting from div tags with "content" in id.
     * @param webDoc the input html document
     * @return the filtered text
     * */
    private String filterWebsite(Document webDoc) {
        StringBuilder newWebText = new StringBuilder();
        List<String> tagLists = Arrays.asList("title", "p", "h1", "h2", "span");
        for (String tag : tagLists) {
            Elements elements = webDoc.select(tag);
            for (Element e : elements) {
                String text = e.ownText();
                newWebText.append(text).append(" ");
            }
        }
        return newWebText.toString();
    }

    /**
     * Creates a stream and extracts the entities from it using dbpedia-spotlight.
     *
     * @param user the user who generated the stream
     * @param type the type of the stream (user, group, web, snippet_clicked, snippet_not_clicked, query)
     * @param objectId the id of the source (user, group, searchId)
     * @param content the content to be recognized
     */
    public List<RecognisedEntity> storeStreamAndExtractEntities(User user, String type, int objectId, String content) throws Exception {
        // Insert inputStream into DB
        int inputId = pkGraphDao.insertInputStream(user.getId(), type, objectId, content);

        List<RecognisedEntity> recognisedEntities = new ArrayList<>();
        SpotlightClient spotlight = new SpotlightClient();

        //Parse the content to dbpedia-spotlight
        if (type.equals("web")) {
            Document doc = Jsoup.connect(content).timeout(10 * 1000).ignoreHttpErrors(true)
                .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.152 Safari/537.36")
                .get();
            content = filterWebsite(doc);
        }
        annotationUnit = spotlight.get(content);

        //Parse the results to the function
        if (annotationUnit.getResources() != null) {
            recognisedEntities = annotate(annotationUnit, type);
        }

        //Remove all annotations with confidence < 0.9
        recognisedEntities.removeIf(recognisedEntity -> recognisedEntity.getConfidence() < spotlightRequiredConfidence);
        recognisedEntities.forEach(entity -> entity.setInputStreams(String.valueOf(inputId)));
        return recognisedEntities;
    }

    /**
     * Main function of the class.
     * @param sessionId the session id of the current annotation
     * @param user the user's username
     * */
    public int storeEntities(String sessionId, User user, List<RecognisedEntity> entities) {
        if (entities.isEmpty()) {
            return 0;
        }

        int streamId = Integer.parseInt(entities.stream().findFirst().get().getInputStreams());

        //Store this annotationCount into DB
        for (RecognisedEntity entity : entities) {
            entity.setUserId(user.getId());
            entity.setSessionId(sessionId);

            //Round the confidence
            entity.setConfidence(Precision.round(entity.getConfidence(), 2));

            Optional<RecognisedEntity> foundAnnotation = pkGraphDao.findEntityByUriAndType(entity.getUri(), entity.getType(), entity.getUserId());
            //If already an annotationCount is found in DB, update its columns
            if (foundAnnotation.isEmpty()) {
                pkGraphDao.saveEntity(entity);
            } else {
                //Update the sessionId
                String session = foundAnnotation.get().getSessionId();
                if (!session.contains(entity.getSessionId())) {
                    session += "," + entity.getSessionId();
                }

                //Update the InputId
                String input = foundAnnotation.get().getInputStreams();
                if (!input.contains(entity.getInputStreams())) {
                    input += "," + entity.getInputStreams();
                }

                entity.setUriId(foundAnnotation.get().getUriId());
                entity.setSessionId(session);
                entity.setInputStreams(input);
                entity.setCreatedAt(foundAnnotation.get().getCreatedAt());
                pkGraphDao.saveEntity(entity);
            }

            if (user.getId() == userBean.getUser().getId()) {
                userBean.getUserPkg().updatePkg(entity);
                userBean.getUserPkg().updateRdfModel(entity, user, sessionId);
            }
        }
        return streamId;
    }
}
