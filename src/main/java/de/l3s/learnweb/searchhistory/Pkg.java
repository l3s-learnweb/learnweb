package de.l3s.learnweb.searchhistory;

import static de.l3s.learnweb.app.Learnweb.dao;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.stream.Collectors.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.user.User;

/**
* Main calculation class for annotation. Create a static Pkg list.
* For communication with other applications: collabGraph and Recommender system
* */
public class Pkg {

    private transient List<AnnotationCount> annotationCounts;

    private List<User> users;
    private transient HashMap<Integer, Double> results;
    private static final Pattern PATTERN = Pattern.compile("http://dbpedia.org/resource/");

    public static Pkg instance = new Pkg(new ArrayList<>(), new ArrayList<>());

    private List<RdfModel> rdfGraphs;
    /**
     * The Node class. Has all values of an entity
     * */
    public class Node {
        private transient int id;
        private String uri;
        private String name;
        private int frequency;
        private String users;
        private transient String sessionId;
        private transient double confidence;
        private transient double weight;

        private String type;

        //Node class. Receives the input from DB to be visualized
        public Node(int id, String name, String uri, String users, double confidence, String sessionId, double weight, String type) {
            this.id = id;
            this.sessionId = sessionId;
            this.name = name;
            this.uri = uri;
            this.users = users;
            this.confidence = confidence;
            this.frequency = 1;
            this.weight = weight;
            this.type = type;
        }

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public int getFrequency() {
            return frequency;
        }

        public void setFrequency(int frequency) {this.frequency = frequency;}

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

        public void increaseFrequency() {
            this.frequency++;
        }

        public void combineUsers(String sessionId) {
            List<String> sessionIdSplit = Arrays.stream(sessionId.split(",")).toList();
            for (String id : sessionIdSplit) {
                if (!this.sessionId.contains(id)) {
                    if (this.sessionId.isEmpty()) {
                        this.sessionId = id;
                    } else {
                        this.sessionId += "," + id;
                    }
                }
            }
        }

        public double getConfidence() {
            return confidence;
        }

        public void setConfidence(final double confidence) {
            this.confidence = confidence;
        }

        public String getSessionId() {
            return sessionId;
        }

        public double getWeight() {
            return weight;
        }

        public void setWeight(final double weight) {
            this.weight = weight;
        }

        public String getType() {
            return type;
        }

        public void setType(final String type) {
            this.type = type;
        }
    }

    /**
     * The link class. Represents the weighted link between two entities
     * */
    public class Link {
        private int source;
        private int target;
        private transient double weight;
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

        public Link(int source, int target, double weight) {
            this.source = source;
            this.target = target;
            this.weight = weight;
        }
    }

    private List<Node> nodes;
    private List<Link> links;

    private void setLink(int source, int target, double weight) {
        links.add(new Pkg.Link(source, target, weight));
    }

    /**
     * Add this node into nodes List
     * @param uri    the new node's uri
     * @param username   the new node's username
     * @param confidence the new node's confidence
     * @param sessionId  the new nodes' session id
     * @param weight     the new node's weight
     * */
    private void AddNode(int id, String uri, String username, double confidence, String sessionId, double weight, String type) {
        //Get the Node name as uri minus domain root - dbpedia.org/resource
        String nameQuery = PATTERN.matcher(uri).replaceAll("")
            .replaceAll("_", " ");
        Node node = new Node(id, nameQuery, uri, username, confidence, sessionId, weight, type);
        if (!nodes.contains(node)) {
            nodes.add(node);
        }
    }

    /**
     * Calculate the weight to be connected from a node with the function values based on the algorithm
     * @param   annotationCount  the new entity
     * @return  the weight of this entity, based on its group type and how many days since the input into DB
     * */
    private double calculateWeight(AnnotationCount annotationCount) {
        int days = (int) DAYS.between(LocalDateTime.now(), annotationCount.getCreatedAt());
        switch (annotationCount.getType()) {
            case "user" -> {
                return 3 * Math.exp(-days);
            }
            case "group" -> {
                return 0.5 * Math.exp(-days);
            }
            case "web" -> {
                return 1 * Math.exp(-days);
            }
            case "snippet_clicked" -> {
                return 5 * Math.exp(-days);
            }
            case "query" -> {
                return 11 * Math.exp(-days);
            }
            case "snippet_notClicked" -> {
                return -Math.exp(-days);
            }
            default -> {
            }
        }
        return 0;
    }

    /**
     * Remove the duplicating nodes (same uri) and their corresponding links
     * The duplicating nodes will be removed first, with their links' sources and targets changed into the first node's values
     * Then the duplicating links will be removed
     * */
    private void removeDuplicatingNodesAndLinks() {
        //Remove duplicating nodes by merging nodes with the same uri
        for (int i = 0; i < nodes.size() - 1; i++) {
            if (!nodes.get(i).getUri().isEmpty()) {
                for (int j = i + 1; j < nodes.size(); j++) {
                    if (nodes.get(i).getUri().equals(nodes.get(j).getUri())) {
                        for (Link link : links) {
                            if (link.target == j) {
                                link.target = i;
                            } else if (link.source == j) {
                                link.source = i;
                            }
                            if (link.source > j) {
                                link.source--;
                            }
                            if (link.target > j) {
                                link.target--;
                            }
                        }
                        //Join the users and sessionId of the first node
                        List<String> userList = new ArrayList<>(Arrays.stream(nodes.get(i).getUsers().split(",")).toList());
                        userList.removeAll(Arrays.stream(nodes.get(j).getUsers().split(",")).toList());
                        userList.addAll(Arrays.stream(nodes.get(j).getUsers().split(",")).toList());
                        nodes.get(i).setUsers(String.join(",", userList));
                        nodes.get(i).combineUsers(nodes.get(j).getSessionId());
                        //Remove the duplicating node
                        nodes.remove(j);
                        j--;
                        nodes.get(i).increaseFrequency();
                    }
                }
            }
        }

        //Remove duplicating edges by merging edges with same sources & targets and vice versa
        //Set redundant sources of edges to -1
        for (int i = 0; i < links.size() - 1; i++) {
            if (links.get(i).source != -1) {
                for (int j = i + 1; j < links.size(); j++) {
                    if ((links.get(i).source == links.get(j).source && links.get(i).target == links.get(j).target)
                        || (links.get(i).source == links.get(j).target && links.get(i).target == links.get(j).source)) {
                        links.get(i).setWeight(links.get(i).getWeight() + links.get(j).getWeight());
                        links.get(j).setSource(-1);
                    }
                }
            }
        }
        //Remove edges with sources equal to -1
        links.removeIf(link -> link.getSource() == -1);
    }

    /**
     * Create the PKG for all users in the specific group
     * @param    groupId     the id of the group
     * @return   a List of Shared Object in Json form
     * */
    public void createPkg(int groupId) throws IOException {

        this.annotationCounts = dao().getSearchHistoryDao().findAllAnnotationCounts();
        //Find the search sessions in this group
        List<SearchSession> sessions = dao().getSearchHistoryDao().findSessionsByGroupId(groupId);
        //Find the users in this group
        users = dao().getUserDao().findByGroupId(groupId);
        //Find the group
        Group group = dao().getGroupDao().findByIdOrElseThrow(groupId);
        //Add default node. Any group that has only 1 node will be connected to default node
        AddNode(0,"default", "", 1, "", 0.0, "");

        //Initialize rdf graph model list
        rdfGraphs = new ArrayList<>();
        for (User user : users) {
            rdfGraphs.add(new RdfModel(user, group, sessions));
        }
        //Create nodes and edges in (original) graph
        //TODO: complete the RDF graph
        for (AnnotationCount annotationCount : this.annotationCounts) {
            for (User user : users) {
                if (annotationCount.getUsers().contains(user.getUsername())) {
                    updatePkg(annotationCount, user);
                }
            }
        }
    }

    /**
    * @param annotationCount
    * */
    public void updatePkg(AnnotationCount annotationCount, User user) {
        double weight = calculateWeight(annotationCount);
        AddNode(annotationCount.getUriId(), annotationCount.getUri(), annotationCount.getUsers(), annotationCount.getConfidence()
            , annotationCount.getSessionId(), weight, annotationCount.getType());
        for (int i = 1; i < nodes.size() - 1; i++) {
            if (nodes.get(i).getType().equals(annotationCount.getType())) {
                setLink(i, nodes.size() - 1, weight);
            }
        }

//----------------------------------Rdf-insert-model--------------------------------------
        if (annotationCount.getType().contains("snippet")) {
            rdfGraphs.get(users.indexOf(user)).addStatement("Snippet/" + PATTERN.matcher(annotationCount.getUri())
                .replaceAll(""), "title", annotationCount.getSurfaceForm(), "literal");
            rdfGraphs.get(users.indexOf(user)).addStatement("Snippet/" + PATTERN.matcher(annotationCount.getUri())
                .replaceAll(""), "url", annotationCount.getUri(), "literal");
            for (String session : annotationCount.getSessionId().split(",")) {
                rdfGraphs.get(users.indexOf(user)).
                    addStatement("SearchSession/" + session, "contains", "Snippet/" +
                        PATTERN.matcher(annotationCount.getUri()).replaceAll(""), "resource");
                for (SearchQuery searchQuery : dao().getSearchHistoryDao().findQueriesBySessionId(session)) {
                    if (annotationCount.getId().contains(String.valueOf(searchQuery.searchId()))) {
                        rdfGraphs.get(users.indexOf(user)).addStatement("SearchQuery/" + searchQuery.query(), "generatesResult",
                            "Snippet/" + PATTERN.matcher(annotationCount.getUri()).replaceAll(""), "resource");
                    }
                }
            }
        }

        if ("web".equals(annotationCount.getType())) {
            rdfGraphs.get(users.indexOf(user)).addStatement("WebPage/" + PATTERN.matcher(annotationCount.getUri())
                .replaceAll(""), "title", annotationCount.getSurfaceForm(), "literal");
            rdfGraphs.get(users.indexOf(user)).addStatement("WebPage/" + PATTERN.matcher(annotationCount.getUri())
                .replaceAll(""), "url", annotationCount.getUri(), "literal");
            for (String session : annotationCount.getSessionId().split(",")) {
                rdfGraphs.get(users.indexOf(user)).addStatement("SearchSession/" + session, "contains", "WebPage/"
                    + PATTERN.matcher(annotationCount.getUri()).replaceAll(""), "resource");
                for (SearchQuery searchQuery : dao().getSearchHistoryDao().findQueriesBySessionId(session)) {
                    if (annotationCount.getId().contains(String.valueOf(searchQuery.searchId()))) {
                        rdfGraphs.get(users.indexOf(user)).addStatement("SearchQuery/" +
                            searchQuery.query(), "generatesResult", "WebPage/" +
                            PATTERN.matcher(annotationCount.getUri()).replaceAll(""), "resource");
                    }
                }
            }
        }
        for (String inputStreamId : annotationCount.getInputStreams().split(",")) {
            Optional<String> inputStream = dao().getSearchHistoryDao().findInputStreamById(Integer.parseInt(inputStreamId));
            if (inputStream.isPresent()) {
                rdfGraphs.get(users.indexOf(user)).
                    addStatement("InputStream/" + inputStreamId, "text", inputStream.get(), "literal");
            }
        }
    }

    /**
     * Calculate the sum_weight of each node with the formula of NEA
     */
    private void calculateSumWeight() {
        //Calculate top entities from the formula:
        //Confidence(Node i) * sum(Confidence(Node j)) * Fsum(t)
        for (int i = 1; i < nodes.size(); i++) {
            double sumWeight = 0;
            double sumConfidence = 0;
            for (Link link : links) {
                if (link.source == i || link.target == i) {
                    sumWeight += link.weight;
                    sumConfidence += link.source == i ? (link.target == 0 ? 0 : nodes.get(link.target).getConfidence())
                        : (link.source == 0 ? 0 : nodes.get(link.source).getConfidence());
                }
            }
            results.put(i, nodes.get(i).getConfidence() * sumConfidence * sumWeight);
        }
    }

    /**
     * Create shared objects based on the result of pkg graph calculation
     * @param groupId   The group id
     * @param numberEntities   how many entities per user the shared Object will show
     * @param isAscending show if the sharedObject will get the result from top or bottom
     * @return   the list of shared object in Json form
     * */
    public List<JsonSharedObject> createSharedObject(int groupId, int numberEntities, boolean isAscending) {
        //Initialization
        results = new HashMap<>();

        //Remove duplications
        removeDuplicatingNodesAndLinks();

        calculateSumWeight();
        //The list to be returned
        List<JsonSharedObject> sharedObjects = new ArrayList<>();
        List<Node> newNodes;
        List<Link> newLinks;
        //Get from DB the active users
        List<User> activeUsers = dao().getUserDao().findActiveUsers();
        //Sort the calculated results to get entities' ranking
        List<Map.Entry<Integer, Double>> entries
            = new ArrayList<>(results.entrySet());
        if (!isAscending) entries.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));
        else entries.sort(Map.Entry.comparingByValue());

        //List new nodes after users' top 3
        for (User user : users) {
            int index = 0;
            //Choose only the active users to create the shared object
            if (activeUsers.contains(user)) {
                newNodes = new ArrayList<>();
                newLinks = new ArrayList<>();
                for (Map.Entry<Integer, Double> entry : entries) {
                    //Find from the top of the results numberTopEntities entities, break after reaching the number
                    if (nodes.get(entry.getKey()).users.contains(user.getUsername())) {
                        Node chosenNode = new Node(nodes.get(entry.getKey()).getId(), nodes.get(entry.getKey()).getName(), nodes.get(entry.getKey()).getUri()
                            , user.getUsername(), nodes.get(entry.getKey()).getConfidence(),
                            nodes.get(entry.getKey()).getSessionId(), entry.getValue(), nodes.get(entry.getKey()).getType());
                        newNodes.add(chosenNode);
                        index++;
                        if (index >= numberEntities) {
                            break;
                        }
                    }
                }
                //Retrieve "important" nodes - nodes that appear directly in the query will be displayed with more connections
                //Initialization
                List<Integer> nodesChild = new ArrayList<>();
                List<Integer> indexImportantNodes = new ArrayList<>();
                for (int i = 0; i < newNodes.size(); i++) {
                    nodesChild.add(0);
                    indexImportantNodes.add(i);
                }
                //Links initialization
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
                //Sort the graph with more important nodes coming in first rank
                final List<Node> finalNewNodes = newNodes;
                indexImportantNodes.sort((o1, o2) -> {
                    if (Float.compare(nodesChild.get(o2), nodesChild.get(o1)) == 0) {
                        if (annotationCounts.stream().anyMatch(s -> Objects.equals(s.getUri(), finalNewNodes.get(o2).getUri())
                            && "query".equals(s.getType()))) {
                            return 1;
                        }
                        if (annotationCounts.stream().anyMatch(s -> Objects.equals(s.getUri(), finalNewNodes.get(o1).getUri())
                            && "query".equals(s.getType()))) {
                            return -1;
                        }
                        return 0;
                    } else {
                        return Float.compare(nodesChild.get(o2), nodesChild.get(o1));
                    }
                });

                List<Node> calculatedNodes = new ArrayList<>();
                List<Link> calculatedLinks = new ArrayList<>();

                //Trimming the user's individual graph

                calculatedNodes.add(newNodes.get(indexImportantNodes.get(0)));

                for (int indexNode : indexImportantNodes) {
                    if (!calculatedNodes.contains(newNodes.get(indexNode))) {
                        calculatedNodes.add(newNodes.get(indexNode));
                    }
                    for (Link link : newLinks) {
                        if (link.source == indexNode && !calculatedNodes.contains(newNodes.get(link.target))) {
                            calculatedNodes.add(newNodes.get(link.target));
                            calculatedLinks.add(link);
                        } else if (link.target == indexNode && !calculatedNodes.contains(newNodes.get(link.source))) {
                            calculatedNodes.add(newNodes.get(link.source));
                            calculatedLinks.add(link);
                        }
                    }
                }

                //Create the sharedObject
                JsonSharedObject object = new JsonSharedObject();
                for (Link link : calculatedLinks) {
                    object.getLinks().add(new JsonSharedObject.Link(link.source, link.target));
                }
                object.setUser(new JsonSharedObject.User(users.stream().filter(s -> s.getUsername().equals(user.getUsername()))
                    .findFirst().get().getId(), user.getUsername()));
                for (Node node : newNodes) {
                    object.getEntities().add(new JsonSharedObject.Entity(node.getUri(), node.getName(), node.getWeight()));
                }
                sharedObjects.add(object);

                rdfGraphs.get(users.indexOf(user)).addStatement("SharedObject/" + user.getUsername(), "dateCreated",
                    LocalDateTime.now().format(DateTimeFormatter.ISO_DATE), "literal");
            }
        }

        //Add the entities after calculation to Rdf List
        Group group = dao().getGroupDao().findByIdOrElseThrow(groupId);
        for (User user : users) {
            //Final rdf touch - insert RecognizedEntities
            for (Node node : nodes) {
                if (node.getUsers().contains(user.getUsername())) {
                    rdfGraphs.get(users.indexOf(user)).addEntity(node.getUri(), node.getName(), node.getWeight(), node.getConfidence(), LocalDateTime.now());
                    Optional<AnnotationCount> annotationCount = dao().getSearchHistoryDao().findAnnotationById(node.getId());
                    if (annotationCount.isPresent()) {
                        for (String inputId : annotationCount.get().getInputStreams().split(",")) {
                            rdfGraphs.get(users.indexOf(user)).
                                addStatement("RecognizedEntities/" + node.getName(), "processes", "InputStream/" + inputId, "resource");
                        }
                    }
                }
            }
            //Print the Rdf graphs both to DB and local directories as files
            String value = rdfGraphs.get(users.indexOf(user)).printModel();
            if (dao().getSearchHistoryDao().findRdfById(user.getId()).isEmpty()) {
                dao().getSearchHistoryDao().insertRdf(user.getId(), group.getId(), value);
            } else {
                dao().getSearchHistoryDao().updateRdf(value, user.getId());
            }
        }

        return sharedObjects;
    }

    private Pkg(List<Node> nodes, List<Link> links) {
        this.nodes = nodes;
        this.links = links;
    }
}
