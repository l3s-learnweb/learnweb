package de.l3s.learnweb.searchhistory;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.group.GroupDao;
import de.l3s.learnweb.resource.ResourceDecorator;
import de.l3s.learnweb.searchhistory.dbpediaSpotlight.common.AnnotationUnit;
import de.l3s.learnweb.searchhistory.dbpediaSpotlight.common.ResourceItem;
import de.l3s.learnweb.searchhistory.dbpediaSpotlight.rest.SpotlightBean;
import de.l3s.learnweb.user.User;
import de.l3s.learnweb.user.UserDao;

/*
* Create a Json file of the search history from within a specific group.
* All search results from users will be annotated and the keywords listed as nodes using Dbpedia-spotlight.
* Nodes that have the same SessionId will be linked with each other.
* @author Trung Tran
* */
public class JsonQuery implements Serializable {

    private static AnnotationUnit annotationUnit;
    private List<AnnotationCount> annotationCounts = new ArrayList<>();

    public class Node {
        private int id;
        private String query;
        private List<String> sessionId = new ArrayList();
        private int group;
        private int frequency;
        private String users;
        private List<String> userContainers;

        public Node(int id, String sessionId, String query, String user, int frequency) {
            this.id = id;
            this.sessionId.add(sessionId);
            this.query = query;
            userContainers = new ArrayList<>();
            userContainers.add(user);
            this.users = user;
            this.frequency = frequency;
        }

        public boolean containQuery(String query) {
            return this.query.toLowerCase(Locale.ROOT).equals(query);
        }

        public void increaseFrequency() {
            frequency++;
        }

        public List<String> getSessionId() {
            return sessionId;
        }

        public void upgradeNode(String sessionId, String user) {
            this.sessionId.add(sessionId);
            if (!userContainers.contains(user)) userContainers.add(user);
        }

        private void setUsers() {
            this.users = userContainers.stream().sorted().collect(Collectors.joining(", "));
        }
    }

    public class Link {
        public int source;
        public int target;

        public Link(int source, int target) {
            this.source = source;
            this.target = target;
        }
    }

    public class Metadata {
        public String id;
        public boolean isPrivate;
        public LocalDateTime createdAt;
    }

    public class Record implements Serializable {
        public List<Node> nodes = new ArrayList<>();
        public List<Link> links = new ArrayList<>();
    }

    private Node containQuery(String query) {
        return (record.nodes.stream().filter(s -> s.query.equals(query)).findAny().orElse(null));
    }

    private void setLink(int source, int target) {
        record.links.add(new Link(source, target));
    }

    private Map<String, Long> annotate(AnnotationUnit annotationUnit, int id, String type) {
        for (ResourceItem resource : annotationUnit.getResources()) {
            annotationCounts.add(new AnnotationCount(id, resource.score(), resource.getSurfaceForm(), resource.getUri(), type));
        }
        Map<String, Long> uriPerType = annotationUnit.getResources().stream()
            .collect(groupingBy(ResourceItem::getUri, counting()));
        //Sort the Map of uri descending of number of occurrences
        uriPerType.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed());
        return uriPerType;
    }

    private void AddNode(Map<String, Long> uriPerType, String sessionId, String username, int searchId) {
        for (String query : uriPerType.keySet()) {
            //Get the Node name as uri minus domain root - dbpedia.org/resource
            String nameQuery = query.replaceAll("http://dbpedia.org/resource/", "")
                .replaceAll("_"," ");
            Node chosenNode = containQuery(nameQuery);
            //If there exists node with same name then upgrade it - else create a new one
            if (chosenNode != null) {
                if (!chosenNode.getSessionId().contains(sessionId)) {
                    chosenNode.upgradeNode(sessionId, username);
                }
                chosenNode.increaseFrequency();
                continue;
            }
            Node node = new Node(searchId, sessionId, nameQuery,
                username, 1);
            record.nodes.add(node);
        }
    }

    public void processQuery(List<SearchSession> searchSession, SearchHistoryDao searchHistoryDao, int selectedGroupId,
        UserDao userDao, GroupDao groupDao) throws Exception {
        SpotlightBean spotlight = new SpotlightBean();
        annotationCounts = new ArrayList<>();

        //Create nodes by retrieving information from DBPedia-spotlight
        //Groups side
        Optional<Group> group = groupDao.findById(selectedGroupId);
        if (group.isPresent()) {
            annotationUnit = spotlight.get(group.get().getTitle());
            if (annotationUnit.getResources() != null) {
                annotate(annotationUnit, selectedGroupId, "group");
            }
            if (group.get().getDescription() != null) {
                annotationUnit = spotlight.get(group.get().getDescription());
                if (annotationUnit.getResources() != null)
                    annotate(annotationUnit, selectedGroupId, "group");
            }
        }
        //Search query + search results sides
        for (SearchSession session : searchSession) {
            for (SearchQuery searchQuery : session.getQueries()) {

                //Users side
                Optional<User> user =  userDao.findById(session.getUserId());
                if (annotationCounts.stream().filter(s -> s.getId() == session.getUserId() && s.getType().equals("user")).findFirst().isEmpty()) {
                    String interest = user.get().getInterest();
                    if (interest != null) {
                        annotationUnit = spotlight.get(interest);
                        if (annotationUnit.getResources() != null)
                            AddNode(annotate(annotationUnit, session.getUserId(), "user"),
                                session.getSessionId(), session.getUser().getUsername(), searchQuery.searchId());
                    }
                }
                //Snippets side
                Map<String, Long> snippets = searchHistoryDao.findSearchResultsByQuery(searchQuery, 5).stream()
                    .collect(groupingBy(ResourceDecorator::getDescription, counting()));
                for (String snippet : snippets.keySet()) {
                    annotationUnit = spotlight.get(snippet);
                    if (annotationUnit.getResources() != null) annotate(annotationUnit, searchQuery.searchId(), "snippet");
                }

                //If no search results clicked, only proceed the query as plain text
                annotationUnit = spotlight.get(searchQuery.query());
                if (annotationUnit.getResources() != null)
                    AddNode(annotate(annotationUnit, searchQuery.searchId(), "query"), session.getSessionId(), session.getUser().getUsername(), searchQuery.searchId());
                List<String> urlList = searchHistoryDao.findClickedUrl(searchQuery.query());
                //Get webpage from clicked snippets
                if (!urlList.isEmpty()) {
                    for (String url : urlList) {
                        Document doc = Jsoup.parse(url, String.valueOf(3 * 1000));
                        String textFromUrl = doc.text();
                        annotationUnit = spotlight.get(textFromUrl);
                        if (annotationUnit.getResources() == null) continue;
                            annotate(annotationUnit, searchQuery.searchId(), "web");
                    }
                }
            }
        }
        //Insert/update into DB
        //Filter the result list by dropping elements with score < 0.9 and grouping elements with the same uri
        annotationCounts.removeIf(annotationCount -> annotationCount.getSimilarityScore() < 0.9);
        List<AnnotationCount> newAnnotationCounts = new ArrayList<>();
        //Duplications will be counted as 1 tuple and increase the frequency.
        for (AnnotationCount annotationCount : annotationCounts) {
            Optional<AnnotationCount> tmp = newAnnotationCounts.stream().filter(s -> s.getUri().equals(annotationCount.getUri())).findFirst();
            if (tmp.isEmpty()) {
                newAnnotationCounts.add(annotationCount);
            }
            else if (tmp.get().getType().equals(annotationCount.getType())){
                tmp.get().setFrequency(tmp.get().getFrequency() + 1);
            } else {
                newAnnotationCounts.add(annotationCount);
            }
        }
        //Add to DB
        for (AnnotationCount annotationCount : newAnnotationCounts) {
            if (searchHistoryDao.findByUri(annotationCount.getUri()).isPresent()) {
                searchHistoryDao.updateQueryAnnotation(annotationCount.getFrequency(), annotationCount.getUri());
            }
            else {
                searchHistoryDao.insertQueryToAnnotation(annotationCount.getId(),
                    annotationCount.getType(), annotationCount.getUri(), annotationCount.getFrequency(), annotationCount.getSurfaceForm()
                    , annotationCount.getSimilarityScore());
            }
        }
        //Sort the search keywords' users
        for (Node node : record.nodes) {
            node.setUsers();
        }

        //Create links
        for (int i = 0; i < record.nodes.size() - 1; i++) {
            for (int j = i + 1; j < record.nodes.size(); j++) {
                //Check from map if Node i and node j has the same SessionId, then create a link from them.
                Set<String> result = record.nodes.get(i).getSessionId().stream()
                    .distinct()
                    .filter(record.nodes.get(j).getSessionId()::contains)
                    .collect(Collectors.toSet());
                if (!result.isEmpty()) {
                    setLink(i, j);
                }
            }
        }
    }

    public Metadata metadata;
    public Record record = new Record();
}
