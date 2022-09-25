package de.l3s.learnweb.searchhistory;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.stream.Collectors.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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

    public class Node {
        private String uri;
        private String query;
        private int frequency;
        private String users;
        private String sessionId;
        private double confidence;
        private int repetition;

        //Node class. Receives the input from DB to be visualized
        public Node(String query, String uri, String users, double confidence, int repetition, String sessionId) {
            this.sessionId = sessionId;
            this.query = query;
            this.uri = uri;
            this.users = users;
            this.confidence = confidence;
            this.frequency = 1;
            this.repetition = repetition;
        }

        public String getQuery() {
            return query;
        }

        public void setQuery(final String query) {
            this.query = query;
        }

        public int getFrequency() {
            return frequency;
        }

        public String getUsers() {
            return users;
        }

        public void setUsers(final String users) {
            this.users = users;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(final String uri) {
            this.uri = uri;
        }

        public void increaseFrequency(String sessionId){
            this.frequency++;
            List<String> sessionIdSplit = Arrays.stream(sessionId.split(",")).toList();
            for (String id : sessionIdSplit)
                if (!this.sessionId.contains(id)) this.sessionId += "," + id;
        }

        public double getConfidence() {
            return confidence;
        }

        public void setConfidence(final double confidence) {
            this.confidence = confidence;
        }

        public int getRepetition() {
            return repetition;
        }

        public void setRepetition(final int repetition) {
            this.repetition = repetition;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(final String sessionId) {
            this.sessionId = sessionId;
        }
    }

    //Weighted links between created nodes.
    public class Link {
        public int source;
        public int target;

        public int getSource() {
            return source;
        }

        public void setSource(final int source) {
            this.source = source;
        }

        public int getTarget() {
            return target;
        }

        public void setTarget(final int target) {
            this.target = target;
        }

        public double getWeight() {
            return weight;
        }

        public void setWeight(final double weight) {
            this.weight = weight;
        }

        public double weight;

        public Link(int source, int target, double weight) {
            this.source = source;
            this.target = target;
            this.weight = weight;
        }
    }

    public class Metadata {
        public String id;
        public boolean isPrivate;
        public LocalDateTime createdAt;
    }

    public class Record implements Serializable {
        public List<Node> nodes;
        public List<Link> links;

        public Record(final List<Node> nodes, final List<Link> links) {
            this.nodes = nodes;
            this.links = links;
        }
    }

    private void setLink(int source, int target, double weight) {
        record.links.add(new Link(source, target, weight));
    }

    //Getting resources of dbpedia-spotlight results
    private static void annotate(List<AnnotationCount> annotationCounts, AnnotationUnit annotationUnit, int id, String type, List<String> users
        , String sessionId) {

        List<ResourceItem> resources = new ArrayList<>();

        //If annotating from webpages, only choose top 5 per webpage
        if (type.equals("web")) {
            Map<String, Long> uriPerType = annotationUnit.getResources().stream()
                .collect(groupingBy(ResourceItem::getUri, counting()));

            final List<ResourceItem> finalResources = resources;
            uriPerType.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
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
                //Similarity != confidence
                Optional<AnnotationCount> tmp = annotationCounts.stream().filter(s -> s.getUri().equals(resource.getUri()) && s.getType().equals(type))
                    .findAny();
                if (tmp.isPresent()) {
                    List<String> tmpList = new ArrayList<>(Arrays.stream(tmp.get().getUsers().split(",")).toList());
                    tmpList.removeAll(users);
                    tmpList.addAll(users);
                    tmp.get().setUsers(tmpList.stream().collect(joining(",")));
                    tmp.get().setRepetition(tmp.get().getRepetition() + 1);
                    if (!tmp.get().getSessionId().contains(sessionId)) tmp.get().addSessionId(sessionId);
                } else {
                    annotationCounts.add(new AnnotationCount(id, resource.score(), resource.getSurfaceForm(), resource.getUri(), type,
                        users.stream().collect(joining(",")), sessionId));
                }
            }
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
    private void AddNode(List<Node> nodes, String uri, String username, int frequency, double confidence, int repetition
        , String sessionId) {
            //Get the Node name as uri minus domain root - dbpedia.org/resource
            String nameQuery = uri.replaceAll("http://dbpedia.org/resource/", "")
                .replaceAll("_"," ");
            Node node = new Node(nameQuery, uri, username, confidence, repetition, sessionId);
            if (!nodes.contains(node)) nodes.add(node);
    }

    //Calculate the weight to be connected from one node based on the algorithm
    private double calculateWeight(AnnotationCount annotationCount) {
        int days = (int) DAYS.between(LocalDateTime.now(), annotationCount.getCreatedAt());
        switch (annotationCount.getType()) {
            case "user": return 3 * Math.exp(-days);
            case "group": return 4 * Math.exp(-days);
            case "web": return Math.exp(-days);
            case "snippet_clicked":
            case "query":
                return 2 * Math.exp(-days);
            case "snippet_notClicked": return -2 * Math.exp(-days);
            default: break;
        }
        return 0;
    }

    //Implement Algorithm
    public JsonQuery calculateTopEntries(List<AnnotationCount> annotationCounts) {

        List<AnnotationCount> currentGroupAnnotation = new ArrayList<>();
        //Preparation sort
        annotationCounts.sort(Comparator.comparing(AnnotationCount::getType));

        //Add default node. Any group that has only 1 node will be connected to default node
        AddNode(record.nodes, "default", "", 1, 0, 1, "");

        AnnotationCount tmp = new AnnotationCount();
        for (AnnotationCount annotationCount : annotationCounts) {
            double weight = calculateWeight(annotationCount);
            AddNode(record.nodes, annotationCount.getUri(), annotationCount.getUsers(), 1, annotationCount.getConfidence()
                , annotationCount.getRepetition(), annotationCount.getSessionId());
            if (tmp.getType() != null && !tmp.getType().equals(annotationCount.getType())) {
                //Connect with default with this weight
                if (currentGroupAnnotation.size() == 1) {
                    setLink(0, annotationCounts.indexOf(tmp) + 1, weight);
                }
                currentGroupAnnotation = new ArrayList<>();

            }
            else {
                //Connect to all previous nodes in the list with weight
                for (AnnotationCount annotation : currentGroupAnnotation) {
                    setLink(annotationCounts.indexOf(annotationCount) + 1, annotationCounts.indexOf(annotation) + 1, weight);
                }
            }
            currentGroupAnnotation.add(annotationCount);
            tmp = annotationCount;
        }

        if (currentGroupAnnotation.size() == 1)
            setLink(0, annotationCounts.indexOf(tmp) + 1, calculateWeight(tmp));

        //Remove duplications

        //Remove duplicating nodes by merging nodes with the same uri
        for (int i = 1; i < record.nodes.size() - 1; i++) {
            if (!record.nodes.get(i).getUri().isEmpty())
            for (int j = i + 1; j < record.nodes.size(); j++) {
                if (record.nodes.get(i).getUri().equals(record.nodes.get(j).getUri())) {
                    for (Link link : record.links) {
                        if (link.target == j) {
                            link.target = i;
                        }
                        else if (link.source == j) {
                            link.source = i;
                        }
                        if (link.source > j) link.source--;
                        if (link.target > j) link.target--;
                    }

                    record.nodes.get(i).increaseFrequency(record.nodes.get(j).getSessionId());
                    record.nodes.get(i).setRepetition(record.nodes.get(i).getRepetition() + record.nodes.get(j).getRepetition());
                    record.nodes.remove(j);
                    j--;
                }
            }
        }

        //Remove duplicating edges by merging edges with same sources & targets and vice versa
        //Set redundant sources of edges to -1
        for (int i = 0; i < record.links.size() - 1; i++) {
            if (record.links.get(i).source != -1)
            for (int j = i + 1; j < record.links.size(); j++) {
                if ((record.links.get(i).source == record.links.get(j).source && record.links.get(i).target == record.links.get(j).target)
                    ||(record.links.get(i).source == record.links.get(j).target && record.links.get(i).target == record.links.get(j).source)) {
                    record.links.get(i).setWeight(record.links.get(i).getWeight() + record.links.get(j).getWeight());
                    record.links.get(j).setSource(-1);
                }
            }
        }
        //Remove edges with sources equal to -1
        record.links.removeIf(link -> link.getSource() == -1);

        HashMap<Integer, Double> results = new HashMap<>();
        //Calculate top entities from the formula:
        //Confidence(Node i) * sum(Confidence(Node j)) * Fsum(t)
        for (int i = 0; i < record.nodes.size(); i++) {
            double sumWeight = 0;
            double sumConfidence = 0;
            for (Link link : record.links) {
                if (link.source == i || link.target == i) {
                    sumWeight += link.weight;
                    sumConfidence += link.source == i ? (link.target == 0 ? 0 : 0.5) : (link.source == 0 ? 0 : 0.5);
                }
            }
            results.put(i, 0.5 * sumConfidence * sumWeight);
        }
        //Sorting the entities by the results
        List<Map.Entry<Integer, Double>> entries
            = new ArrayList<>(results.entrySet());
        entries.sort(new Comparator<Map.Entry<Integer, Double>>() {
            @Override
            public int compare(final Map.Entry<Integer, Double> o1, final Map.Entry<Integer, Double> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });

        //PREPARING COLLABGRAPH:
        //1 - Get users
        //If we have group node -> the results are from group node, if not then we must take from every other nodes
        List<String> users;
        Optional<AnnotationCount> userRef = annotationCounts.stream().filter(s -> s.getType().equals("group")).findAny();
        if (userRef.isPresent()) users = Arrays.stream(userRef.get().getUsers().split(",")).toList();
        else {
            users = new ArrayList<>();
            for (AnnotationCount annotationCount : annotationCounts) {
                List<String> usersToAdd = Arrays.stream(annotationCount.getUsers().split(",")).toList();
                for (String user : usersToAdd)
                    if (!users.contains(user)) users.add(user);
            }
        }
        List<Node> newNodes = new ArrayList<>();
        List<Link> newLinks = new ArrayList<>();
        //2 - List new nodes after users' top 4
        for (String user : users) {
            int index = 0;
            for (Map.Entry<Integer, Double> entry : entries) {
                Node chosenNode = record.nodes.get(entry.getKey());
                if (chosenNode.users.contains(user)) {
                    if (!newNodes.contains(chosenNode)) newNodes.add(chosenNode);
                    index++;
                    if (index >= 4) break;
                }
            }
        }

        //3 - Visualize the new links
        for (int i = 0; i < newNodes.size() - 1; i++) {
            for (int j = i + 1; j < newNodes.size(); j++) {
                Set<String> result =  Arrays.stream(newNodes.get(i).getSessionId().split(",")).toList().stream()
                    .distinct()
                    .filter(newNodes.get(j).getSessionId()::contains)
                    .collect(toSet());
                if (!result.isEmpty()) {
                    newLinks.add(new Link(i, j, 0));
                }
            }
        }

        //4 - Remake spanning trees for all disjointing subgraphs
        List<Integer> parents = new ArrayList<>(newNodes.size());
        for (int i = 0; i < newNodes.size(); i++) parents.add(-2);
        List<Link> minimumLinks = new ArrayList<>();

        for (Node node: newNodes) {
            for (Link link : newLinks) {
                if (link.source == newNodes.indexOf(node) && parents.get(link.target) == -2) {
                     parents.set(link.target, link.source);
                     minimumLinks.add(link);
                }
                else if (link.target == newNodes.indexOf(node) && parents.get(link.source) == -2) {
                    parents.set(link.source, link.target);
                    minimumLinks.add(link);
                }
            }
            if (parents.get(newNodes.indexOf(node)) == -2)
                parents.set(newNodes.indexOf(node), -1);
        }

        return new JsonQuery(newNodes, minimumLinks);
    }

    public static void processQuery(List<SearchSession> searchSession, SearchHistoryDao searchHistoryDao, int selectedGroupId,
        UserDao userDao, GroupDao groupDao) throws Exception {
        SpotlightBean spotlight = new SpotlightBean();
        List<AnnotationCount> annotationCounts = new ArrayList<>();

        //Create nodes by retrieving information from DBPedia-spotlight
        //1 - Groups side
        Optional<Group> group = groupDao.findById(selectedGroupId);
        if (group.isPresent()) {
            annotationUnit = spotlight.get(group.get().getTitle());
            if (annotationUnit.getResources() != null) {
                annotate(annotationCounts, annotationUnit, selectedGroupId, "group", userDao.findByGroupId(selectedGroupId)
                    .stream().map(User::getUsername).collect(toList()),"");
            }
            if (group.get().getDescription() != null) {
                annotationUnit = spotlight.get(group.get().getDescription());
                if (annotationUnit.getResources() != null)
                    annotate(annotationCounts, annotationUnit, selectedGroupId, "group", userDao.findByGroupId(selectedGroupId)
                        .stream().map(User::getUsername).collect(toList()),"");
            }
        }

        for (SearchSession session : searchSession) {
            for (SearchQuery searchQuery : session.getQueries()) {
                //2- Users side
                Optional<User> user =  userDao.findById(session.getUserId());
                if (annotationCounts.stream().filter(s -> s.getId() == session.getUserId() && s.getType().equals("user")).findFirst().isEmpty()) {
                    String interest = user.get().getInterest();
                    if (interest != null) {
                        annotationUnit = spotlight.get(interest);
                        if (annotationUnit.getResources() != null)
                            annotate(annotationCounts, annotationUnit, session.getUserId(), "user", Arrays.asList((user.get().getUsername())),
                                session.getSessionId());
                    }
                }
                //3, 4 - Snippets side
                //Clicked and not clicked
                List<ResourceDecorator> snippets = searchHistoryDao.findSearchResultsByQuery(searchQuery, 32);
                for (ResourceDecorator snippet : snippets) {
                    annotationUnit = spotlight.get(snippet.getTitle());
                    if (annotationUnit.getResources() != null) {
                        annotate(annotationCounts, annotationUnit, searchQuery.searchId(), snippet.getClicked() ? "snippet_clicked" : "snippet_notClicked"
                            , Arrays.asList((user.get().getUsername())), session.getSessionId());
                    }
                }

                //If no search results clicked, only proceed the query as plain text
                annotationUnit = spotlight.get(searchQuery.query());
                if (annotationUnit.getResources() != null)
                    annotate(annotationCounts, annotationUnit, searchQuery.searchId(), "query"
                        , List.of((user.get().getUsername())), session.getSessionId());
                List<String> urlList = searchHistoryDao.findClickedUrl(searchQuery.query());
                //5 - webpages side
                //Get webpage from clicked snippets
                if (!urlList.isEmpty()) {
                    for (String url : urlList) {
                        Document doc = Jsoup.connect(url).timeout(10 * 1000).
                            userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.152 Safari/537.36")
                            .get();
                        annotationUnit = spotlight.get(filterWebsite(doc));
                        if (annotationUnit.getResources() == null) continue;
                        annotate(annotationCounts, annotationUnit, searchQuery.searchId(), "web"
                            , Arrays.asList((user.get().getUsername())), session.getSessionId());
                    }
                }
            }
        }

        //Filter the result list by dropping elements with score < 0.9
        annotationCounts.removeIf(annotationCount -> annotationCount.getConfidence() < 0.9);

        //Update DB for existing annotationCount (same uri + type), else add new tuple to DB
        for (AnnotationCount annotationCount : annotationCounts) {
            if (searchHistoryDao.findByUriAndType(annotationCount.getUri(), annotationCount.getType()).isPresent()) {
                searchHistoryDao.updateQueryAnnotation(annotationCount.getRepetition(), annotationCount.getUri()
                    , annotationCount.getType(), annotationCount.getSessionId());
            }
            else {
                searchHistoryDao.insertQueryToAnnotation(annotationCount.getId(),
                    annotationCount.getType(), annotationCount.getUri(), annotationCount.getCreatedAt(), annotationCount.getSurfaceForm()
                    , annotationCount.getSessionId(), annotationCount.getUsers(), annotationCount.getConfidence());
            }
        }
    }

    public Metadata metadata;
    public Record record;

    public JsonQuery(final List<Node> nodes, final List<Link> links) {
        this.record = new Record(nodes, links);
    }
}
