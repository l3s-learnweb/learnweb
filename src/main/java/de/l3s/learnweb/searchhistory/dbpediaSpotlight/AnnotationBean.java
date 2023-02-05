package de.l3s.learnweb.searchhistory.dbpediaspotlight;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import org.apache.commons.math3.util.Precision;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.omnifaces.util.Beans;

import de.l3s.learnweb.searchhistory.AnnotationCount;
import de.l3s.learnweb.searchhistory.PkgBean;
import de.l3s.learnweb.searchhistory.SearchHistoryDao;
import de.l3s.learnweb.searchhistory.dbpediaspotlight.common.AnnotationUnit;
import de.l3s.learnweb.searchhistory.dbpediaspotlight.common.ResourceItem;
import de.l3s.learnweb.searchhistory.dbpediaspotlight.rest.SpotlightBean;
import de.l3s.learnweb.user.UserDao;

@RequestScoped
public class AnnotationBean implements Serializable {
    @Serial
    private static final long serialVersionUID = -1169917559922779411L;
    private static AnnotationUnit annotationUnit;
    private transient PkgBean pkgBean;
    @Inject
    private UserDao userDao;
    @Inject
    private SearchHistoryDao searchHistoryDao;

    /**
     * Getting resources of dbpedia-spotlight result.
     * @param annotationUnit the dbpedia-spotlight process
     * @param id the source's id
     * @param type the type of the annotation (user, group, web, snippet_clicked, snippet_not_clicked, query)
     * @param user the user's username
     * @param sessionId the session id of the current annotation
     * @return the list of annotationCount with its newly created values
     */
    private static List<AnnotationCount> annotate(AnnotationUnit annotationUnit, int id, String type, String user, String sessionId) {
        List<ResourceItem> resources = new ArrayList<>();
        List<AnnotationCount> annotationCounts = new ArrayList<>();
        //If annotating from webpages, only choose top 10 per webpage
        if (type.equals("web")) {
            Map<String, Long> uriPerType = annotationUnit.getResources().stream()
                .collect(Collectors.groupingBy(ResourceItem::getUri, Collectors.counting()));

            final List<ResourceItem> finalResources = resources;
            uriPerType.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(r -> {
                    Optional<ResourceItem> resource =  annotationUnit.getResources().stream().filter(s -> s.getUri().equals(r.getKey())).findFirst();
                    if (resource.isPresent()) {
                        finalResources.add(resource.get());
                    }
                });
            resources = finalResources;
        } else {
            resources = annotationUnit.getResources();
        }
        for (ResourceItem resource : resources) {
            annotationCounts.add(new AnnotationCount(String.valueOf(id), resource.score(), resource.getSurfaceForm(), resource.getUri(), type,
                user, sessionId));
        }
        return annotationCounts;
    }

    private static String filterWebsite(Document webDoc) {
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
     * Main function of the class.
     * @param sessionId the session id of the current annotation
     * @param id the id of the source (user, group)
     * @param username the user's username
     * @param type the type of the annotation (user, group, web, snippet_clicked, snippet_not_clicked, query)
     * @param content the content to be recognized
     * */
    public void processQuery(String sessionId, int id, String username, String type, String content) throws Exception {
        if (content == null) {
            return;
        }
        List<AnnotationCount> annotationCounts = new ArrayList<>();
        SpotlightBean spotlight = new SpotlightBean();
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
            annotationCounts = annotate(annotationUnit, id, type, username, sessionId);
        }
        //Remove all annotations with confidence < 0.9
        annotationCounts.removeIf(annotationCount -> annotationCount.getConfidence() < 0.9);

        //Insert inputStream into DB
        int userId = userDao.findByUsername(username).get().getId();
        int inputId = searchHistoryDao.insertInputStream(userId, type, content);

        //Store this annotationCount into DB
        for (AnnotationCount annotationCount : annotationCounts) {
            //Round the confidence
            annotationCount.setConfidence(Precision.round(annotationCount.getConfidence(), 2));

            Optional<AnnotationCount> foundAnnotation = searchHistoryDao.findByUriAndType(annotationCount.getUri(), annotationCount.getType());
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
                //Update the InputId
                String input = foundAnnotation.get().getInputStreams();
                if (!input.contains(String.valueOf(inputId))) {
                    input += "," + inputId;
                }
                //Update the inputId
                //Update the repetition
                searchHistoryDao.updateQueryAnnotation(session, users, input,
                    annotationCount.getUri(), annotationCount.getType());
                if (!"user".equals(type) && !"profile".equals(type) && !searchHistoryDao.findSearchIdByResult(foundAnnotation.get().getUriId()).contains(id)) {
                    searchHistoryDao.insertQueryResult(id, foundAnnotation.get().getUriId());
                }
            } else {
                //Insert directly new annotationCount into DB
                annotationCount.setInputStreams(String.valueOf(inputId));
                int uriId = searchHistoryDao.insertQueryToAnnotation(annotationCount.getType(), annotationCount.getUri(), String.valueOf(inputId),
                    annotationCount.getCreatedAt(), annotationCount.getSurfaceForm(),
                    annotationCount.getSessionId(), annotationCount.getUsers(), annotationCount.getConfidence());
                if (!"user".equals(type) && !"profile".equals(type)) {
                    searchHistoryDao.insertQueryResult(id, uriId);
                }
                getPkgBean().updatePkg(annotationCount, userDao.findByUsername(username).get());
            }
        }
    }

    private PkgBean getPkgBean() {
        if (null == pkgBean) {
            pkgBean = Beans.getInstance(PkgBean.class);
        }
        return pkgBean;
    }
}
