package de.l3s.learnweb.searchhistory;

import static de.l3s.learnweb.app.Learnweb.dao;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.math3.util.Precision;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.user.User;

/**
 * <p>This is the main calculation class for annotation. Based on what user searches for, it will create a Personal
 * knowledge graph (PKG) system for all users in the same group. The system can gather important entities that each
 * user has looked into to show the interests of members in group, as well as creating recommendations for others.</p>
 * <h3>Basic structure of Pkg class:</h3>
 * <ul>
 *     <li>A graph represented by list of nodes and edges</li>
 *     <li>A list of recognized entities retrieved from Database</li>
 * </ul>
 * <p>The workflow is conceptually described as follows:</p>
 * =====================================================================================================================
 * <p>1 - createPkg() initializes the Pkg, gets all recognized entities from DB and makes them as nodes. </p>
 * <p>2 - Which query the user searches for, clicks on which links and vice versa, will be fed from
 * SearchBean.commandOnResourceClick() and SearchBean.destroy() to dbpedia-spotlight api in class NERParser. </p>
 * <p>3 - class NERParser will annotate the input content with its source (from group description, user profile or search),
 * return as a list of recognized entities and store it in DB (learnweb_annotations.annotationCount). The input is also
 * stored in another DB table.</p>
 * <p>4 - Pkg then updates by receiving those entities as nodes - with function updatePkg().</p>
 * <p>5 - Shared objects will be the product of Pkg system. Before creating shared objects, Pkg will merge all duplicating
 * nodes and edges, then calculates every node based on their connections.</p>
 * <p>6 - Depending on user's purpose, Pkg then exports the shared objects for that purpose, e.g. top 3 entities with the
 * highest weights for the collaborative graph, or 5 for recommender system etc.</p>
 *
* */
public final class Pkg {
    private List<Node> nodes;
    private List<Link> links;
    private transient List<AnnotationCount> annotationCounts;
    private List<User> users;
    private transient HashMap<Integer, Double> results;
    private static final Pattern PATTERN = Pattern.compile("http://dbpedia.org/resource/");

    public static final Pkg instance = new Pkg(new ArrayList<>(), new ArrayList<>());
    private List<RdfModel> rdfGraphs;
    /**
     * The Node class. Has all values of an entity
     * */

    public static class Node {
        private transient int id;
        private String uri;
        private String name;
        private int frequency;
        private String users;
        private transient String sessionId;
        private transient double confidence;
        private transient double weight;
        private transient LocalDateTime date;
        private String type;

        //Node class. Receives the input from DB to be visualized
        public Node(int id, String name, String uri, String users, double weight, double confidence, String sessionId, String type, LocalDateTime date) {
            this.id = id;
            this.sessionId = sessionId;
            this.name = name;
            this.uri = uri;
            this.users = users;
            this.confidence = confidence;
            this.frequency = 1;
            this.type = type;
            this.date = date;
            this.weight = weight;
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

        public void setFrequency(int frequency) {
            this.frequency = frequency;
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

        public String getType() {
            return type;
        }

        public void setType(final String type) {
            this.type = type;
        }

        public LocalDateTime getDate() {
            return date;
        }

        public void setDate(final LocalDateTime date) {
            this.date = date;
        }

        public double getWeight() {
            return weight;
        }

        public void setWeight(final double weight) {
            this.weight = weight;
        }
    }

    /**
     * The link class. Represents the weighted link between two entities
     * */
    public static class Link {
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

    private void setLink(int source, int target, double weight) {
        links.add(new Link(source, target, weight));
    }

    /**
     * Add this node into the list of nodes.
     * @param id the new node's id from DB
     * @param uri    the new node's uri
     * @param username   the new node's username string. Multiple usernames is divided by commas.
     * @param confidence the new node's confidence
     * @param sessionId  the new node's session id
     * @param weight the weight of the new node (can actually be excluded in future updates)
     * @param type the type of the new node.
     * @param date the created time of the node.
     * */
    private void addNode(int id, String uri, String username, double confidence, double weight, String sessionId, String type, LocalDateTime date) {
        //Get the Node name as uri minus domain root - dbpedia.org/resource
        String nameQuery = PATTERN.matcher(uri).replaceAll("")
            .replaceAll("_", " ");
        Node node = new Node(id, nameQuery, uri, username, weight, confidence, sessionId, type, date);
        if (!nodes.contains(node)) {
            nodes.add(node);
        }
    }

    /**
    * Add one RDF statement to the user's RDF graph.
     * @param subject the statement's subject
     * @param pre the statement's predicate
     * @param object the statement's object
     * @param mode either "literal" or "resource"
     * @param user the user whom RDF Graph gets this statement added
    * */
    private void addRdfStatement(String subject, String pre, String object, String mode, User user) {
        rdfGraphs.get(users.indexOf(user)).addStatement(subject, pre, object, mode);
    }

    /**
     * Calculate the weight to be connected from a node with the function values based on the algorithm.
     * @param   date  the date of the entity's creation
     * @param   type  the type of this entity
     * @return  the weight of this entity, based on its group type and how many days since the input into DB
     * */
    private double calculateWeight(LocalDateTime date, String type) {
        int days = (int) ChronoUnit.DAYS.between(LocalDateTime.now(), date);
        switch (type) {
            case "user" -> {
                return 3 * Math.exp(days);
            }
            case "group" -> {
                return 0.5 * Math.exp(days);
            }
            case "web" -> {
                return 1 * Math.exp(days);
            }
            case "snippet_clicked" -> {
                return 4 * Math.exp(days);
            }
            case "query" -> {
                return 12 * Math.exp(days);
            }
            case "snippet_not_clicked" -> {
                return -0.6 * Math.exp(days);
            }
            default -> {
            }
        }
        return 0;
    }

    /**
     * <p>Merge the duplicating nodes (same uri) and their corresponding links.
     * Two nodes with the same uri are called duplicates, so the function will remove one of
     * the two duplicates in each pair of nodes. The remaining gets its usernames, types and session id
     * combined from both of them. </p>
     * <p>The function then creates the links between each of these nodes. Based on which types they have in common,
     * the link between them will get their weight calculated based on the formula in calculateWeight(date, type).
     * The nodes that have no connections will then be connected to the DEFAULT node.</p>
     * */
    private void removeDuplicatingNodesAndLinks() {
        //Remove duplicating nodes by merging nodes with the same uri
        for (int i = 0; i < nodes.size() - 1; i++) {
            if (!nodes.get(i).getUri().isEmpty()) {
                for (int j = i + 1; j < nodes.size(); j++) {
                    if (nodes.get(i).getUri().equals(nodes.get(j).getUri())) {
                        //Join the users and sessionId of the first node
                        List<String> userList = new ArrayList<>(Arrays.stream(nodes.get(i).getUsers().split(",")).toList());
                        List<String> types = new ArrayList<>(Arrays.stream(nodes.get(i).getType().split(",")).toList());
                        userList.removeAll(Arrays.stream(nodes.get(j).getUsers().split(",")).toList());
                        userList.addAll(Arrays.stream(nodes.get(j).getUsers().split(",")).toList());
                        types.removeAll(Arrays.stream(nodes.get(j).getType().split(",")).toList());
                        types.addAll(Arrays.stream(nodes.get(j).getType().split(",")).toList());
                        nodes.get(i).setUsers(String.join(",", userList));
                        nodes.get(i).combineUsers(nodes.get(j).getSessionId());
                        nodes.get(i).setType(String.join(",", types));
                        //Remove the duplicating node
                        nodes.remove(j);
                        j--;
                        nodes.get(i).increaseFrequency();
                    }
                }
            }
        }
        for (int i = 1; i < nodes.size() - 1; i++) {
            boolean isUnique = true;
            for (int j = i + 1; j < nodes.size(); j++) {
                List<String> commonTypes = Arrays.stream(nodes.get(i).getType().split(",")).filter(
                    Arrays.stream(nodes.get(j).getType().split(",")).toList()::contains).toList();
                if (!commonTypes.isEmpty()) {
                    double weight = 0;
                    isUnique = false;
                    for (String s : commonTypes) {
                        weight += calculateWeight(nodes.get(i).getDate(), s);
                    }
                    setLink(i, j, weight);
                }
            }
            if (isUnique) {
                setLink(0, i, calculateWeight(nodes.get(i).getDate(), nodes.get(i).getType()));
            }
        }
    }

    /**
     * Initializes the PKG for all users in the specific group.
     * @param    groupId     the id of the group
     * */
    public void createPkg(int groupId) {
        long startTime = System.nanoTime();
        System.out.println("Creating Pkg, estimate time: ");
        this.annotationCounts = dao().getSearchHistoryDao().findAllAnnotationCounts();
        nodes = new ArrayList<>();
        links = new ArrayList<>();
        //Find the users in this group
        users = dao().getUserDao().findByGroupId(groupId);
        //Add default node. Any group that has only 1 node will be connected to default node
        addNode(0, "default", "", 1, 0.0, "", "", null);

        //Initialize rdf graph model list
        rdfGraphs = new ArrayList<>();
        for (User user : users) {
            rdfGraphs.add(new RdfModel(user));
            if (groupId != 0) {
                Group group = dao().getGroupDao().findById(groupId).get();
                rdfGraphs.get(rdfGraphs.size() - 1).addGroup(user, group);
            }
        }
        //Create nodes and edges in (original) graph
        for (AnnotationCount annotationCount : this.annotationCounts) {
            for (User user : users) {
                if (annotationCount.getUsers().matches(".*\\b" + Pattern.quote(user.getUsername()) + "\\b.*")) {
                    updatePkg(annotationCount, user);
                }
            }
        }
        long endTime = System.nanoTime();
        System.out.println(endTime - startTime);
        removeDuplicatingNodesAndLinks();
    }

    /** Add this recognized entity into the node list as a Node.
     * Then add RDF-statement to this user's RDF graph based on the parameters from the entity.
     * @param annotationCount The recognized entity to be added to the PKG
     * @param user the current user
    * */
    public void updatePkg(AnnotationCount annotationCount, User user) {
        addNode(annotationCount.getUriId(), annotationCount.getUri(), annotationCount.getUsers(), annotationCount.getConfidence(),
            Precision.round(calculateWeight(annotationCount.getCreatedAt(), annotationCount.getType()), 2), annotationCount.getSessionId(),
            annotationCount.getType(), annotationCount.getCreatedAt());

        //----------------------------------Rdf-insert-model--------------------------------------
        Pattern keywordPattern = Pattern.compile("<b>" + "(.*?)" + "</b>");
        Pattern headlinePattern = Pattern.compile("<title>" + "(.*?)" + "</title>");
        List<SearchSession> sessions = dao().getSearchHistoryDao().findSessionsByUserId(user.getId());
        for (String session : annotationCount.getSessionId().split(",")) {
            for (SearchSession searchSession : sessions) {
                if (searchSession.getSessionId().equals(session)) {
                    addRdfStatement("educor:User/" + user.getId(), "educor:generatesLogs", "SearchSession/" + session, "resource", user);
                    addRdfStatement("SearchSession/" + session, "schema:startTime",
                        searchSession.getStartTimestamp().format(DateTimeFormatter.ISO_DATE), "literal", user);
                    addRdfStatement("SearchSession/" + session, "schema:endTime",
                        searchSession.getEndTimestamp().format(DateTimeFormatter.ISO_DATE), "literal", user);
                    for (SearchQuery query : searchSession.getQueries()) {
                        addRdfStatement("SearchQuery/" + query.searchId(),
                            "schema:dateCreated", query.timestamp().format(DateTimeFormatter.ISO_DATE), "literal", user);
                        if ("query".equals(annotationCount.getType())) {
                            addRdfStatement("SearchSession/" + searchSession.getSessionId(), "contains",
                                "SearchQuery/" + query.searchId(), "resource", user);
                            addRdfStatement("SearchQuery/" + query.searchId(), "query",
                                query.query(), "literal", user);
                        }
                    }
                    if (annotationCount.getType().contains("snippet")) {
                        addRdfStatement("Snippet/" + annotationCount.getUriId(), "schema:title", annotationCount.getSurfaceForm(), "literal", user);
                        addRdfStatement("Snippet/" + annotationCount.getUriId(), "schema:url", annotationCount.getUri(), "literal", user);
                        addRdfStatement("SearchSession/" + session, "contains", "Snippet/" + annotationCount.getUriId(), "resource", user);
                        for (int searchId : dao().getSearchHistoryDao().findSearchIdByResult(annotationCount.getUriId())) {
                            addRdfStatement("SearchQuery/" + searchId,
                                "generatesResult", "Snippet/" + annotationCount.getUriId(), "resource", user);
                        }

                    }

                    if ("web".equals(annotationCount.getType())) {
                        addRdfStatement("schema:WebPage/" + annotationCount.getUriId(), "schema:title", annotationCount.getSurfaceForm(), "literal", user);
                        addRdfStatement("schema:WebPage/" + annotationCount.getUriId(), "schema:url", annotationCount.getUri(), "literal", user);
                        addRdfStatement("SearchSession/" + session, "contains", "schema:WebPage/" + annotationCount.getUriId(), "resource", user);
                        for (int searchId : dao().getSearchHistoryDao().findSearchIdByResult(annotationCount.getUriId())) {
                            addRdfStatement("SearchQuery/" + searchId,
                                "generatesResult", "WebPage/" + annotationCount.getUriId(), "resource", user);
                        }

                    }
                    for (String inputId : annotationCount.getInputStreams().split(",")) {
                        for (InputStreamRdf inputStream : dao().getSearchHistoryDao().findInputContentById(Integer.parseInt(inputId))) {
                            //Add createsInputStream statement based on the entities' type
                            if (inputStream.getUserId() == user.getId()) {
                                Group group = dao().getGroupDao().findByUserId(user.getId()).get(0);
                                if ("profile".equals(annotationCount.getType())) {
                                    addRdfStatement("educor:UserProfile/" + user.getId(), "createsInputStream",
                                        "InputStream/" + inputId, "resource", user);
                                } else if ("group".equals(annotationCount.getType())) {
                                    addRdfStatement("foaf:Group/" + group.getId(), "createsInputStream",
                                        "InputStream/" + inputId, "resource", user);
                                } else {
                                    addRdfStatement("SearchSession/" + session, "createsInputStream",
                                        "InputStream/" + inputId, "resource", user);
                                }
                                addRdfStatement("InputStream/" + inputStream.getId(), "schema:text", inputStream.getContent(), "literal", user);
                                addRdfStatement("InputStream/" + inputStream.getId(), "schema:dateCreated",
                                    inputStream.getDateCreated().toString(), "literal", user);
                                addRdfStatement("RecognizedEntities/" + PATTERN.matcher(annotationCount.getUri())
                                    .replaceAll(""), "processes", "InputStream/" + inputStream.getId(), "resource", user);
                                if ("web".equals(annotationCount.getType())) {
                                    Matcher matcher = keywordPattern.matcher(inputStream.getContent());
                                    while (matcher.find()) {
                                        addRdfStatement("WebPage/" + annotationCount.getUriId(), "keywords", matcher.group(1), "literal", user);
                                    }
                                    matcher = headlinePattern.matcher(inputStream.getContent());
                                    while (matcher.find()) {
                                        addRdfStatement("WebPage/" + annotationCount.getUriId(), "headline", matcher.group(1), "literal", user);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        //--------------------------------RDF-Insert-Model-End----------------------------------
    }

    /**
     * Calculate the sum_weight of each node with the formula of NEA.
     */
    public void calculateSumWeight() {
        results = new HashMap<>();
        for (int i = 1; i < nodes.size() - 1; i++) {
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

    /** Create a single graph of the current user, which contains node from 3 different sources (user, group and session).
     * @param userId the current user id
     * @return the shared object of a single graph
     * */
    public JsonSharedObject createSingleGraph(int userId, int groupId) {
        Optional<User> optUser = dao().getUserDao().findById(userId);
        if (optUser.isEmpty() || !dao().getUserDao().isActiveUser(userId, groupId)) {
            return null;
        }
        JsonSharedObject object = new JsonSharedObject("singleGraph", false);
        List<Node> newNodes = new ArrayList<>();
        List<Link> newLinks = new ArrayList<>();

        Map<String, String> typeMap = new HashMap<>();
        //HARDCODED lines - need alternatives
        typeMap.put("profile", "user");
        typeMap.put("group", "group");
        typeMap.put("snippet_not_clicked", "session");
        typeMap.put("snippet_clicked", "session");
        typeMap.put("query", "session");
        typeMap.put("web", "session");

        Map<String, Integer> occurrences = new HashMap<>();
        occurrences.put("user", 0);
        occurrences.put("group", 0);
        occurrences.put("session", 0);

        //Find occurrences of each source as nodes - if the total of one source exceeds 10 then stop
        for (Map.Entry<Integer, Double> entry : results.entrySet()) {
            Node node = nodes.get(entry.getKey());
            for (Map.Entry<String, String> type : typeMap.entrySet()) {
                if (node.getUsers().matches(".*\\b" + Pattern.quote(optUser.get().getUsername()) + "\\b.*")
                    && occurrences.get(type.getValue()) < 10 && node.getType().contains(type.getKey())) {
                    String nodeType = type.getValue();
                    Node chosenNode = new Node(node.getId(), node.getName(), node.getUri(), "User", entry.getValue(),
                        node.getConfidence(), node.getSessionId(), nodeType, node.getDate());
                    if (newNodes.stream().noneMatch(s -> s.getType().equals(chosenNode.getType()) && s.getUri().equals(chosenNode.getUri()))) {
                        newNodes.add(chosenNode);
                        occurrences.put(type.getValue(), occurrences.get(type.getValue()) + 1);
                    }
                }
            }
        }
        //Create links
        for (int i = 0; i < newNodes.size() - 1; i++) {
            for (int j = i + 1; j < newNodes.size(); j++) {
                Node node1 = newNodes.get(i);
                Node node2 = newNodes.get(j);
                if (Arrays.stream(node1.getType().split(",")).anyMatch(node2.getType()::contains)) {
                    newLinks.add(new Link(i, j, 0));
                }
            }
        }
        //Add this new graph (newLinks, newNodes) into a shared object, so that it can be stored later on
        object.getLinks().addAll(newLinks.stream().map(l -> new JsonSharedObject.Link(l.source, l.target)).toList());
        object.getEntities().addAll(newNodes.stream().map(n -> new JsonSharedObject.Entity(n.getUri(), n.getName(), n.getWeight(),
            n.getType(), n.getId())).toList());
        return object;
    }

    /**
     * Create shared objects based on the result of pkg graph calculation.
     * @param groupId   The group id
     * @param numberEntities   how many entities per user the shared Object will show
     * @param isAscending show if the sharedObject will get the result from top or bottom
     * @return   the list of shared object in Json form
     * */
    public List<JsonSharedObject> createSharedObject(int groupId, int numberEntities, boolean isAscending, String application) {
        //Initialization
        //The list to be returned
        List<JsonSharedObject> sharedObjects = new ArrayList<>();
        List<Node> newNodes;
        List<Link> newLinks;
        if (results == null) {
            return null;
        }
        //Sort the calculated results to get entities' ranking
        List<Map.Entry<Integer, Double>> entries
            = new ArrayList<>(results.entrySet());
        if (isAscending) {
            entries.sort(Map.Entry.comparingByValue());
        } else {
            entries.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));
        }
        //List new nodes after users' top 3
        for (User user : users) {
            int index = 0;
            //Choose only the active users to create the shared object
            if (dao().getUserDao().isActiveUser(user.getId(), groupId)) {
                newNodes = new ArrayList<>();
                newLinks = new ArrayList<>();
                for (Map.Entry<Integer, Double> entry : entries) {
                    //Find from the top of the results numberTopEntities entities, break after reaching the number
                    if (nodes.get(entry.getKey()).users.matches(".*\\b" + Pattern.quote(user.getUsername()) + "\\b.*")) {
                        Node chosenNode = new Node(nodes.get(entry.getKey()).getId(), nodes.get(entry.getKey()).getName(), nodes.get(entry.getKey()).getUri(),
                            user.getUsername(), entry.getValue(), nodes.get(entry.getKey()).getConfidence(),
                            nodes.get(entry.getKey()).getSessionId(), nodes.get(entry.getKey()).getType(), nodes.get(entry.getKey()).getDate());
                        newNodes.add(chosenNode);
                        index++;
                        if (index >= numberEntities) {
                            break;
                        }
                    }
                }

                //Links initialization
                for (int i = 0; i < newNodes.size() - 1; i++) {
                    for (int j = i + 1; j < newNodes.size(); j++) {
                        Set<String> result = Arrays.stream(newNodes.get(i).getSessionId().split(",")).toList().stream()
                            .distinct()
                            .filter(Arrays.stream(newNodes.get(j).getSessionId().split(",")).toList()::contains)
                            .collect(Collectors.toSet());
                        if (!result.isEmpty()) {
                            newLinks.add(new Link(i, j, 0));
                        }
                    }
                }

                //Create the sharedObject
                JsonSharedObject object = new JsonSharedObject(application, false);
                for (Link link : newLinks) {
                    object.getLinks().add(new JsonSharedObject.Link(link.source, link.target));
                }
                object.setUser(new JsonSharedObject.User(users.stream().filter(s -> s.getUsername().equals(user.getUsername()))
                    .findFirst().get().getId(), user.getUsername()));
                for (Node node : newNodes) {
                    object.getEntities().add(new JsonSharedObject.Entity(node.getUri(), node.getName(), node.getWeight(), node.getType(), node.getId()));
                }
                sharedObjects.add(object);
            }
        }

        Gson gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new SearchHistoryBean.LocalDateTimeAdapter().nullSafe())
            .create();
        //Export shared object to DB
        for (JsonSharedObject sharedObject : sharedObjects) {
            int sharedObjectId;
            List<JsonSharedObject> obj = dao().getSearchHistoryDao().findObjectByIdAndType(groupId, sharedObject.getUser().getId(), application);
            if (obj.isEmpty()) {
                sharedObjectId = dao().getSearchHistoryDao().insertSharedObject(sharedObject.getUser().getId(), groupId, application,
                    gson.toJson(sharedObject));
            } else {
                dao().getSearchHistoryDao().updateSharedObject(gson.toJson(sharedObject), LocalDateTime.now(), sharedObject.getUser().getId(), groupId, application);
                sharedObjectId = obj.get(0).getId();
            }
            //--------------------RDF---------------------------
            int userIndex = users.stream().mapToInt(users::indexOf).filter(i -> users.get(i).getUsername().equals(sharedObject.getUser().getName())
            ).findFirst().getAsInt();
            rdfGraphs.get(userIndex).addStatement("SharedObject/" + sharedObjectId, "schema:dateCreated",
                LocalDateTime.now().format(DateTimeFormatter.ISO_DATE), "literal");
            rdfGraphs.get(userIndex).addStatement("SharedObject/" + sharedObjectId, "schema:application", sharedObject.getApplication(), "literal");
            rdfGraphs.get(userIndex).addStatement("SharedObject/" + sharedObjectId, "schema:text", gson.toJson(sharedObject), "literal");
            for (JsonSharedObject.Entity entity: sharedObject.getEntities()) {
                rdfGraphs.get(userIndex).addStatement("SharedObject/" + sharedObjectId, "dependsOn", "RecognizedEntities/" + entity.getId(), "resource");
                Optional<AnnotationCount> annotationObj = annotationCounts.stream().filter(s -> s.getUriId() == entity.getId()).findFirst();
                if (annotationObj.isPresent()) {
                    for (String inputId : annotationObj.get().getInputStreams().split(",")) {
                        rdfGraphs.get(userIndex).addStatement("SharedObject/" + sharedObjectId, "dependsOn", "InputStream/" + inputId, "resource");
                    }
                }
            }
            //--------------------End RDF ----------------------
        }

        //Add the entities after calculation to Rdf List
        Group group = dao().getGroupDao().findByIdOrElseThrow(groupId);
        for (User user : users) {
            for (Node node : nodes) {
                if (node.getUsers().matches(".*\\b" + Pattern.quote(user.getUsername()) + "\\b.*")) {
                    rdfGraphs.get(users.indexOf(user)).addEntity(PATTERN.matcher(node.getUri()).replaceAll(""),
                        node.getUri(), node.getName(), node.getWeight(), node.getConfidence(), LocalDateTime.now());
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
