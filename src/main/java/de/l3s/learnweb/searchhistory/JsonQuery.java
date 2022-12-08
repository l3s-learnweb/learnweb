package de.l3s.learnweb.searchhistory;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.stream.Collectors.*;
import static org.apache.commons.math3.util.Precision.round;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.searchhistory.dbpediaSpotlight.common.AnnotationUnit;
import de.l3s.learnweb.searchhistory.dbpediaSpotlight.common.ResourceItem;
import de.l3s.learnweb.searchhistory.dbpediaSpotlight.rest.SpotlightBean;
import de.l3s.learnweb.user.User;

/*
* Create a Json file of the search history from within a specific group.
* All search results from users will be annotated and the keywords listed as nodes using Dbpedia-spotlight.
* Nodes that have the same SessionId will be linked with each other.
* @author Trung Tran
* */
public class JsonQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1100213292212314798L;
    private static AnnotationUnit annotationUnit;

    public class Node {
        private String uri;
        private String query;
        private int frequency;
        private String users;
        private String sessionId;
        private double confidence;
        private int repetition;
        private double weight;

        //Node class. Receives the input from DB to be visualized
        public Node(String query, String uri, String users, double confidence, int repetition, String sessionId, double weight) {
            this.sessionId = sessionId;
            this.query = query;
            this.uri = uri;
            this.users = users;
            this.confidence = confidence;
            this.frequency = 1;
            this.repetition = repetition;
            this.weight = weight;
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

        public void setFrequency(int frequency) { this.frequency = frequency; }
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

        public void increaseFrequency(){
            this.frequency++;
        }

        public void combineUsers(String sessionId) {
            List<String> sessionIdSplit = Arrays.stream(sessionId.split(",")).toList();
            for (String id : sessionIdSplit)
                if (!this.sessionId.contains(id))
                {
                    if (this.sessionId.isEmpty()) this.sessionId = id;
                    else this.sessionId += "," + id;
                }
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

        public double getWeight() {
            return weight;
        }

        public void setWeight(final double weight) {
            this.weight = weight;
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
        @Serial
        private static final long serialVersionUID = -474111258968809133L;
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
            annotationCounts.add(new AnnotationCount(String.valueOf(id), resource.score(), resource.getSurfaceForm(), resource.getUri(), type,
                user, sessionId, keywords));
        }
        return annotationCounts;
    }

    private void AddNode(List<Node> nodes, String uri, String username, int frequency, double confidence, int repetition
        , String sessionId, double weight) {
            //Get the Node name as uri minus domain root - dbpedia.org/resource
            String nameQuery = uri.replaceAll("http://dbpedia.org/resource/", "")
                .replaceAll("_"," ");
            Node node = new Node(nameQuery, uri, username, confidence, repetition, sessionId, weight);
            if (!nodes.contains(node)) nodes.add(node);
    }

    //Calculate the weight to be connected from one node based on the algorithm
    private double calculateWeight(AnnotationCount annotationCount) {
        int days = (int) DAYS.between(LocalDateTime.now(), annotationCount.getCreatedAt());
        switch (annotationCount.getType()) {
            case "user": return 3 * Math.exp(-days);
            case "group": return 1 * Math.exp(-days);
            case "web": return 1.5 * Math.exp(-days);
            case "snippet_clicked": return 6 * Math.exp(-days);
            case "query": return 11 * Math.exp(-days);
            case "snippet_notClicked": return -Math.exp(-days);
            default: break;
        }
        return 0;
    }

    public Record removeDuplicatingNodesAndLinks(Record record) {
        Record calculatedRecord = record;
        //Remove duplicating nodes by merging nodes with the same uri
        for (int i = 0; i < calculatedRecord.nodes.size() - 1; i++) {
            if (!calculatedRecord.nodes.get(i).getUri().isEmpty())
                for (int j = i + 1; j < calculatedRecord.nodes.size(); j++) {
                    if (calculatedRecord.nodes.get(i).getUri().equals(calculatedRecord.nodes.get(j).getUri())) {
                        for (Link link : calculatedRecord.links) {
                            if (link.target == j) {
                                link.target = i;
                            }
                            else if (link.source == j) {
                                link.source = i;
                            }
                            if (link.source > j) link.source--;
                            if (link.target > j) link.target--;
                        }
                        List<String> userList = new ArrayList<>(Arrays.stream(calculatedRecord.nodes.get(i).getUsers().split(",")).toList());
                        userList.removeAll(Arrays.stream(calculatedRecord.nodes.get(j).getUsers().split(",")).toList());
                        userList.addAll(Arrays.stream(calculatedRecord.nodes.get(j).getUsers().split(",")).toList());
                        calculatedRecord.nodes.get(i).setUsers(userList.stream().collect(joining(",")));
                        calculatedRecord.nodes.get(i).combineUsers(calculatedRecord.nodes.get(j).getSessionId());
                        calculatedRecord.nodes.get(i).setRepetition(calculatedRecord.nodes.get(i).getRepetition() + calculatedRecord.nodes.get(j).getRepetition());
                        calculatedRecord.nodes.remove(j);
                        j--;
                        calculatedRecord.nodes.get(i).increaseFrequency();
                    }
                }
        }

        //Remove duplicating edges by merging edges with same sources & targets and vice versa
        //Set redundant sources of edges to -1
        for (int i = 0; i < calculatedRecord.links.size() - 1; i++) {
            if (calculatedRecord.links.get(i).source != -1)
                for (int j = i + 1; j < calculatedRecord.links.size(); j++) {
                    if ((calculatedRecord.links.get(i).source == calculatedRecord.links.get(j).source && calculatedRecord.links.get(i).target == calculatedRecord.links.get(j).target)
                        ||(calculatedRecord.links.get(i).source == calculatedRecord.links.get(j).target && calculatedRecord.links.get(i).target == calculatedRecord.links.get(j).source)) {
                        calculatedRecord.links.get(i).setWeight(calculatedRecord.links.get(i).getWeight() + calculatedRecord.links.get(j).getWeight());
                        //Why do I set the nodes???
                        // calculatedRecord.nodes.get(calculatedRecord.links.get(i).source).setWeight(
                        //     calculatedRecord.nodes.get(calculatedRecord.links.get(i).source).getWeight()
                        //     + calculatedRecord.nodes.get(calculatedRecord.links.get(j).source).getWeight()
                        // );
                        calculatedRecord.links.get(j).setSource(-1);
                    }
                }
        }
        //Remove edges with sources equal to -1
        calculatedRecord.links.removeIf(link -> link.getSource() == -1);
        return calculatedRecord;
    }

    //Implement Algorithm
    public List<JsonSharedObject> calculateTopEntries(List<AnnotationCount> annotationCounts, List<User> users,
        Group group, List<SearchSession> sessions, SearchHistoryDao searchHistoryDao, int numberTopEntities) throws IOException {

        //Preparation sort
        annotationCounts.sort(Comparator.comparing(AnnotationCount::getType));

        //Add default node. Any group that has only 1 node will be connected to default node
        AddNode(record.nodes, "default", "", 1, 0, 1, "", 0.0);

        //Create rdf graph model list
        List<RdfModel> rdfGraphs = new ArrayList<>();
        for (User user : users)
            rdfGraphs.add(new RdfModel(user, group, sessions));
        
        AnnotationCount tmp = new AnnotationCount();
        int currentIndex = 0;
        List<AnnotationCount> currentGroupAnnotation = new ArrayList<>();
        List<String> groupUsers = new ArrayList<>();
        List<Boolean> isUserActive = new ArrayList<>();

        for (User user : users) {
            groupUsers.add(user.getUsername());
            isUserActive.add(false);
        }
        //Create nodes and edges in (original) graph
        //TODO: complete the RDF graph
        for (AnnotationCount annotationCount : annotationCounts) {
            boolean isContain = false;

            for (String groupUser : groupUsers)
                for (String annotationUser : Arrays.stream(annotationCount.getUsers().split(",")).toList())
                    if (groupUser.equals(annotationUser)) isContain = true;

            if (!isContain) continue;

            double weight = calculateWeight(annotationCount);
            for (User user : users) {
                if (annotationCount.getType().contains("snippet") && annotationCount.getUsers().contains(user.getUsername())) {
                    rdfGraphs.get(users.indexOf(user)).addStatement("Snippet/" + annotationCount.getUri().replaceAll("http://dbpedia.org/resource/",
                        ""), "title", annotationCount.getSurfaceForm(), "literal");
                    rdfGraphs.get(users.indexOf(user)).addStatement("Snippet/" + annotationCount.getUri().replaceAll("http://dbpedia.org/resource/",
                        ""), "url", annotationCount.getUri(), "literal");
                    for (String session : annotationCount.getSessionId().split(",")) {
                        rdfGraphs.get(users.indexOf(user)).addStatement("SearchSession/" + session, "contains", "Snippet/" + annotationCount.getUri().replaceAll("http://dbpedia.org/resource/", ""), "resource");
                    }
                    for (SearchSession session : sessions) {
                        for (SearchQuery searchQuery : session.getQueries()) {
                            if (annotationCount.getId().contains(String.valueOf(searchQuery.searchId()))) {
                                rdfGraphs.get(users.indexOf(user)).addStatement("SearchQuery/" + searchQuery.query(), "generatesResult",
                                    "Snippet/" + annotationCount.getUri().replaceAll("http://dbpedia.org/resource/", ""), "resource");
                            }
                        }
                    }
                }

                if (annotationCount.getType().equals("web")) {
                    for (String userActive : annotationCount.getUsers().split(",")) isUserActive.set(groupUsers.indexOf(userActive), true);
                    if (annotationCount.getUsers().contains(user.getUsername())) {
                        rdfGraphs.get(users.indexOf(user)).addStatement("WebPage/" + annotationCount.getUri().replaceAll("http://dbpedia.org/resource/", ""), "title", annotationCount.getSurfaceForm(), "literal");
                        rdfGraphs.get(users.indexOf(user)).addStatement("WebPage/" + annotationCount.getUri().replaceAll("http://dbpedia.org/resource/", ""), "url", annotationCount.getUri(), "literal");
                        for (String session : annotationCount.getSessionId().split(",")) {
                            rdfGraphs.get(users.indexOf(user)).addStatement("SearchSession/" + session, "contains", "WebPage/" + annotationCount.getUri().replaceAll("http://dbpedia.org/resource/", ""), "resource");
                        }
                        for (SearchSession session : sessions) {
                            for (SearchQuery searchQuery : session.getQueries()) {
                                if (annotationCount.getId().contains(String.valueOf(searchQuery.searchId()))) {
                                    rdfGraphs.get(users.indexOf(user)).addStatement("SearchQuery/" + searchQuery.query(), "generatesResult", "WebPage/" + annotationCount.getUri().replaceAll("http://dbpedia.org/resource/", ""), "resource");
                                }
                            }
                        }
                    }
                }
            }

            AddNode(record.nodes, annotationCount.getUri(), annotationCount.getUsers(), 1, annotationCount.getConfidence()
                , annotationCount.getRepetition(), annotationCount.getSessionId(), weight);

            if (tmp.getType() != null && !tmp.getType().equals(annotationCount.getType())) {
                //Connect with default with this weight
                if (currentGroupAnnotation.size() == 1) {
                    setLink(0, currentIndex, weight);
                }
                currentGroupAnnotation = new ArrayList<>();
                currentIndex = record.nodes.size() - 1;
            } else {
                //Connect to all previous nodes in the list with weight
                for (int i = currentIndex; i < record.nodes.size() - 1; i++) {
                    if (i != 0) setLink(i, record.nodes.size() - 1, weight);
                }
            }
            currentGroupAnnotation.add(annotationCount);
            tmp = annotationCount;
        }

        if (currentGroupAnnotation.size() == 1)
            setLink(0, record.nodes.size() - 1, calculateWeight(tmp));

        //Remove duplications
        record = removeDuplicatingNodesAndLinks(record);

        HashMap<Integer, Double> results = new HashMap<>();
        //Calculate top entities from the formula:
        //Confidence(Node i) * sum(Confidence(Node j)) * Fsum(t)
        for (int i = 0; i < record.nodes.size(); i++) {
            double sumWeight = 0;
            double sumConfidence = 0;
            for (Link link : record.links) {
                if (link.source == i || link.target == i) {
                    sumWeight += link.weight;
                    sumConfidence += link.source == i ? (link.target == 0 ? 0 : record.nodes.get(link.target).getConfidence())
                        : (link.source == 0 ? 0 : record.nodes.get(link.source).getConfidence());
                }
            }
            results.put(i, record.nodes.get(i).getConfidence() * sumConfidence * sumWeight);
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

        for (User user : users) {
            for (Node node : record.nodes) {
                if (node.getUsers().contains(user.getUsername()))
                    rdfGraphs.get(users.indexOf(user)).addEntity(node.getUri(), node.getQuery(), node.getWeight(), node.getConfidence(), LocalDateTime.now());
            }
            String value = rdfGraphs.get(users.indexOf(user)).printModel(group.getTitle(), user.getUsername());
            if (searchHistoryDao.findRdfById(user.getId()).isEmpty())
                searchHistoryDao.insertRdf(user.getId(), value);
            else
                searchHistoryDao.updateRdf(value, user.getId());
        }
        //PREPARING COLLABGRAPH:

        List<Node> newNodes;
        List<Link> newLinks;
        //1 - List new nodes after users' top 3

        List<JsonSharedObject> sharedObjects = new ArrayList<>();
        for (String user : groupUsers) {
            int index = 0;
                if (isUserActive.get(groupUsers.indexOf(user))) {
                    newNodes = new ArrayList<>();
                    newLinks = new ArrayList<>();
                    for (Map.Entry<Integer, Double> entry : entries) {
                        if (record.nodes.get(entry.getKey()).users.contains(user)) {
                            //PSEUDO- after RDF completes this will change
                            Node chosenNode = new Node(record.nodes.get(entry.getKey()).getQuery(), record.nodes.get(entry.getKey()).getUri()
                                , user, record.nodes.get(entry.getKey()).getConfidence(), record.nodes.get(entry.getKey()).getRepetition(),
                                record.nodes.get(entry.getKey()).getSessionId(), entry.getValue());
                            newNodes.add(chosenNode);
                            index++;
                            if (index >= numberTopEntities) break;
                        }
                    }
                    List<Integer> nodesChild = new ArrayList<>();
                    List<Integer> indexImportantNodes = new ArrayList<>();
                    for (int i = 0; i < newNodes.size(); i++) {
                        nodesChild.add(0);
                        indexImportantNodes.add(i);
                    }
                    for (int i = 0; i < newNodes.size() - 1; i++) {
                        for (int j = i + 1; j < newNodes.size(); j++) {
                            Set<String> result = Arrays.stream(newNodes.get(i).getSessionId().split(",")).toList().stream()
                                .distinct()
                                .filter(Arrays.stream(newNodes.get(j).getSessionId().split(",")).toList()::contains)
                                .collect(toSet());
                            if (!result.isEmpty()) {
                                newLinks.add(new Link(i, j, 0));
                                nodesChild.set(i, nodesChild.get(i) + 1);
                                nodesChild.set(j, nodesChild.get(j) + 1);
                            }
                        }
                    }
                    final List<Node> finalNewNodes = newNodes;
                    indexImportantNodes.sort((o1, o2) -> {
                        if (Float.compare(nodesChild.get(o2), nodesChild.get(o1)) == 0) {
                            if (annotationCounts.stream().anyMatch(s -> Objects.equals(s.getUri(), finalNewNodes.get(o2).getUri())
                                && s.getType().equals("query"))) return 1;
                            if (annotationCounts.stream().anyMatch(s -> Objects.equals(s.getUri(), finalNewNodes.get(o1).getUri())
                                && s.getType().equals("query"))) return -1;
                            return 0;
                        }
                        else return Float.compare(nodesChild.get(o2), nodesChild.get(o1));
                    });
                    List<Node> calculatedNodes = new ArrayList<>();
                    List<Link> calculatedLinks = new ArrayList<>();

                    //Trimming the user's individual graph

                    calculatedNodes.add(newNodes.get(indexImportantNodes.get(0)));

                    for (int indexNode: indexImportantNodes) {
                        if (!calculatedNodes.contains(newNodes.get(indexNode)))
                            calculatedNodes.add(newNodes.get(indexNode));
                        for (Link link : newLinks) {
                            if (link.source == indexNode && !calculatedNodes.contains(newNodes.get(link.target))){
                                calculatedNodes.add(newNodes.get(link.target));
                                calculatedLinks.add(link);
                            }
                            else if (link.target == indexNode && !calculatedNodes.contains(newNodes.get(link.source))) {
                                calculatedNodes.add(newNodes.get(link.source));
                                calculatedLinks.add(link);
                            }
                        }
                    }
                    JsonSharedObject object = new JsonSharedObject();
                    for (Link link : calculatedLinks) {
                        object.getLinks().add(new JsonSharedObject.Link(link.source, link.target));
                    }
                    object.setUser(new JsonSharedObject.User(users.stream().filter(s -> s.getUsername().equals(user)).findFirst().get().getId(), user));
                    for (Node node: newNodes) {
                        object.getEntities().add(new JsonSharedObject.Entity(node.getUri(), node.getQuery(), node.getWeight()));
                    }
                    sharedObjects.add(object);
                }
        }

        return sharedObjects;
    }

    public JsonQuery createCollabGraph(List<JsonSharedObject> sharedObjects) {

        Record calculatedRecord = new Record(new ArrayList<>(), new ArrayList<>());
        for (JsonSharedObject sharedObject : sharedObjects) {
            //Add all new entities
            for (JsonSharedObject.Link link : sharedObject.getLinks()) {
                calculatedRecord.links.add(new Link(link.getSource() + calculatedRecord.nodes.size(),
                    link.getTarget() + calculatedRecord.nodes.size(), 0));
            }
            for (JsonSharedObject.Entity nodeToAdd : sharedObject.getEntities()) {
                calculatedRecord.nodes.add(new Node(nodeToAdd.getQuery(), nodeToAdd.getUri(), sharedObject.getUser().getName(), 0, 0, "", 0.0));
            }
            //Add links, modify it to be logical with current nodes of collabgraph
        }
        calculatedRecord = removeDuplicatingNodesAndLinks(calculatedRecord);

        return new JsonQuery(calculatedRecord.nodes, calculatedRecord.links);
    }

    public static void processQuery(String sessionId, int id, String username, String type, String content, SearchHistoryDao searchHistoryDao) throws Exception {
        List<AnnotationCount> annotationCounts = new ArrayList<>();
        SpotlightBean spotlight = new SpotlightBean();
        annotationUnit = spotlight.get(content);
        if (annotationUnit.getResources() != null) {
            annotationCounts = annotate(annotationUnit, id, type, username, sessionId, "");
        }
        annotationCounts.removeIf(annotationCount -> annotationCount.getConfidence() < 0.9);

        //Store this annotationCount into DB

        for (AnnotationCount annotationCount : annotationCounts) {
            annotationCount.setConfidence(round(annotationCount.getConfidence(),2));
            Optional<AnnotationCount> foundAnnotation = searchHistoryDao.findByUriAndType(annotationCount.getUri(), annotationCount.getType());
            if (foundAnnotation.isPresent()) {
                String session = foundAnnotation.get().getSessionId();
                if (!foundAnnotation.get().getSessionId().contains(annotationCount.getSessionId())) {
                    session += "," + annotationCount.getSessionId();
                }
                String users = foundAnnotation.get().getUsers();
                if (!foundAnnotation.get().getUsers().contains(annotationCount.getUsers())) {
                    users += "," + annotationCount.getUsers();
                }
                searchHistoryDao.updateQueryAnnotation(foundAnnotation.get().getRepetition() + 1, session, users,
                    annotationCount.getUri(), annotationCount.getType());
            }
            else {
                searchHistoryDao.insertQueryToAnnotation(annotationCount.getId(),
                    annotationCount.getType(), annotationCount.getUri(), annotationCount.getCreatedAt(), annotationCount.getSurfaceForm()
                    , annotationCount.getSessionId(), annotationCount.getUsers(), annotationCount.getConfidence(), annotationCount.getRepetition());
            }
        }
    }

    public Record record;

    public JsonQuery(final List<Node> nodes, final List<Link> links) {
        this.record = new Record(nodes, links);
    }
}
