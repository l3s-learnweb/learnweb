package de.l3s.learnweb.pkg;

import static de.l3s.learnweb.app.Learnweb.dao;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.math3.util.Precision;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.l3s.learnweb.group.Group;
import de.l3s.dbpedia.RecognisedEntity;
import de.l3s.learnweb.searchhistory.SearchHistoryBean;
import de.l3s.learnweb.searchhistory.SearchQuery;
import de.l3s.learnweb.searchhistory.SearchSession;
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
 */
public final class PKGraph {
    private static final Logger log = LogManager.getLogger(PKGraph.class);
    private static final Pattern PATTERN = Pattern.compile("http://dbpedia.org/resource/");

    private int userId;
    protected RdfModel rdfGraph;
    private List<Node> nodes = new ArrayList<>();
    private List<Link> links = new ArrayList<>();
    private transient List<RecognisedEntity> recognisedEntities;
    private transient HashMap<Integer, Double> results;

    private PKGraph() {
    }

    private void setLink(int source, int target, double weight) {
        links.add(new Link(source, target, weight));
    }

    /**
     * Add this node into the list of nodes.
     *
     * @param id the new node's id from DB
     * @param uri the new node's uri
     * @param userId the user's id.
     * @param confidence the new node's confidence
     * @param sessionId the new node's session id
     * @param weight the weight of the new node (can actually be excluded in future updates)
     * @param type the type of the new node.
     * @param date the created time of the node.
     */
    private void addNode(int id, String uri, int userId, double confidence, double weight, String sessionId, String type, LocalDateTime date) {
        //Get the Node name as uri minus domain root - dbpedia.org/resource
        String nameQuery = PATTERN.matcher(uri).replaceAll("").replaceAll("_", " ");

        Node node = new Node(id, nameQuery, uri, userId, weight, confidence, sessionId, type, date);
        if (!nodes.contains(node)) {
            nodes.add(node);
        }
    }

    /**
     * Add one RDF statement to the user's RDF graph.
     *
     * @param subject the statement's subject
     * @param pre the statement's predicate
     * @param object the statement's object
     * @param mode either "literal" or "resource"
     */
    public void addRdfStatement(String subject, String pre, String object, String mode) {
        rdfGraph.addStatement(subject, pre, object, mode);
    }

    private void addSearchSessionStatement(User user) {
        for (SearchSession session : dao().getSearchHistoryDao().findSessionsByUserId(user.getId())) {
            addRdfStatement("SearchSession/" + session.getSessionId(), "schema:startTime", session.getStartTimestamp().format(DateTimeFormatter.ISO_DATE), "literal");
            addRdfStatement("SearchSession/" + session.getSessionId(), "schema:endTime", session.getEndTimestamp().format(DateTimeFormatter.ISO_DATE), "literal");
            for (SearchQuery query : session.getQueries()) {
                addRdfStatement("SearchSession/" + session.getSessionId(), RdfModel.prefixBase + "contains", "SearchQuery/" + query.searchId(), "resource");
                addRdfStatement("SearchQuery/" + query.searchId(), RdfModel.prefixBase + "query", query.query(), "literal");
                addRdfStatement("SearchQuery/" + query.searchId(), "schema:dateCreated", query.timestamp().format(DateTimeFormatter.ISO_DATE), "literal");
            }
        }
    }

    /**
     * Calculate the weight to be connected from a node with the function values based on the algorithm.
     *
     * @param date the date of the entity's creation
     * @param type the type of this entity
     * @return the weight of this entity, based on its group type and how many days since the input into DB
     */
    private double calculateWeight(LocalDateTime date, String type) {
        int days = (int) ChronoUnit.DAYS.between(LocalDateTime.now(), date);
        switch (type) {
            case "user" -> {
                return 3 * Math.exp(days);
            }
            case "group" -> {
                return 1 * Math.exp(days);
            }
            case "web" -> {
                return 1 * Math.exp(days);
            }
            case "snippet_clicked" -> {
                return 4 * Math.exp(days);
            }
            case "query" -> {
                return 11 * Math.exp(days);
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
     */
    public void removeDuplicatingNodesAndLinks() {
        //Remove duplicating nodes by merging nodes with the same uri
        for (int i = 0; i < nodes.size() - 1; i++) {
            if (!nodes.get(i).getUri().isEmpty()) {
                for (int j = i + 1; j < nodes.size(); j++) {
                    if (nodes.get(i).getUri().equals(nodes.get(j).getUri())) {
                        //Join the users and sessionId of the first node
                        List<String> types = new ArrayList<>(Arrays.stream(nodes.get(i).getType().split(",")).toList());
                        types.removeAll(Arrays.stream(nodes.get(j).getType().split(",")).toList());
                        types.addAll(Arrays.stream(nodes.get(j).getType().split(",")).toList());

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

        // recalculate the weight of each node
        calculateSumWeight();
    }

    /**
     * Initializes the PKG for all users in the specific group.
     *
     * @param user the current User
     */
    public static PKGraph createPkg(User user) {
        PKGraph pkg = new PKGraph();
        pkg.userId = user.getId();
        // Get the entities from DB for this user
        pkg.recognisedEntities = dao().getCollabGraphDao().findEntityByUser(user.getId());

        if (pkg.recognisedEntities.size() == 1) {
            // Add the default node. Any group that has only 1 node will be connected to the default node
            pkg.addNode(0, "default", 0, 1, 0.0, "", "", null);
        }

        // New RDF-Model initialization
        pkg.rdfGraph = new RdfModel();
        // Initialize rdf graph model
        Optional<RdfObject> rdfObject = dao().getCollabGraphDao().findRdfById(user.getId());
        if (rdfObject.isPresent()) {
            pkg.rdfGraph.makeModelFromString(rdfObject.get().getRdfValue());
        } else {
            //If the user belongs to a group
            for (Group group : user.getGroups()) {
                pkg.rdfGraph.addGroup(user, group);
            }
        }

        // Add statements which search sessions are the subject
        pkg.addSearchSessionStatement(user);
        for (RecognisedEntity recognisedEntity :  pkg.recognisedEntities) {
            // Call the DB update here
            pkg.updatePkg(recognisedEntity);
        }

        pkg.removeDuplicatingNodesAndLinks();
        log.info("PKG created for user {}", user.getUsername());
        return pkg;
    }

    /**
     * Add RDF-statements to this user's RDF graph based on the parameters from the entity.
     * Calls after DBPedia-spotlight is used
     *
     * @param recognisedEntity the entity
     * @param user the current user
     * @param session the user's current search session
     */
    public void updateRdfModel(RecognisedEntity recognisedEntity, User user, String session) {
        //----------------------------------Rdf-insert-model--------------------------------------
        Pattern keywordPattern = Pattern.compile("<b>" + "(.*?)" + "</b>");
        Pattern headlinePattern = Pattern.compile("<title>" + "(.*?)" + "</title>");
        //Get the session id list from entity
        // List<Integer> searchIds = dao().getCollabGraphDao().findSearchIdByResult(annotationCount.getUriId());

        if (recognisedEntity.getType().contains("snippet")) {
            addRdfStatement("SearchSession/" + session, "contains", "Snippet/" + recognisedEntity.getUriId(), "resource");
        } else if ("web".equals(recognisedEntity.getType())) {
            addRdfStatement("SearchSession/" + session, "contains", "schema:WebPage/" + recognisedEntity.getUriId(), "resource");
        }
        addRdfStatement("educor:User/" + user.getId(), "educor:generatesLogs", "SearchSession/" + session, "resource");

        //Input stream statements
        if (recognisedEntity.getInputStreams() != null) {
            List<InputStreamRdf> inputStreamRdfs = dao().getCollabGraphDao().findInputContentById(recognisedEntity.getInputStreams());
            for (InputStreamRdf inputRdf : inputStreamRdfs) {
                //Add createsInputStream statement based on the entities' type
                if (inputRdf.getUserId() == user.getId()) {
                    if ("user".equals(recognisedEntity.getType())) {
                        addRdfStatement("educor:UserProfile/" + user.getId(), "createsInputStream", "InputStream/" + inputRdf.getId(), "resource");
                    } else if ("group".equals(recognisedEntity.getType())) {
                        addRdfStatement("foaf:Group/" + inputRdf.getObjectId(), "createsInputStream", "InputStream/" + inputRdf.getId(), "resource");
                    } else {
                        addRdfStatement("SearchSession/" + session, "createsInputStream", "InputStream/" + inputRdf.getUserId(), "resource");
                    }

                    addRdfStatement("InputStream/" + inputRdf.getId(), "schema:text", inputRdf.getContent(), "literal");
                    addRdfStatement("InputStream/" + inputRdf.getId(), "schema:dateCreated", inputRdf.getDateCreated().toString(), "literal");
                    addRdfStatement("RecognizedEntities/" + PATTERN.matcher(recognisedEntity.getUri()).replaceAll(""), "processes", "InputStream/" + inputRdf.getId(), "resource");
                    if ("web".equals(recognisedEntity.getType())) {
                        Matcher matcher = keywordPattern.matcher(inputRdf.getContent());
                        while (matcher.find()) {
                            addRdfStatement("WebPage/" + recognisedEntity.getUriId(), "keywords", matcher.group(1), "literal");
                        }
                        matcher = headlinePattern.matcher(inputRdf.getContent());
                        while (matcher.find()) {
                            addRdfStatement("WebPage/" + recognisedEntity.getUriId(), "headline", matcher.group(1), "literal");
                        }
                    }
                }
            }
        }
        //--------------------------------RDF-Insert-Model-End----------------------------------
    }

    /**
     * Add this recognized entity into the node list as a Node.
     *
     * @param entity The recognized entity to be added to the PKG
     */
    public void updatePkg(RecognisedEntity entity) {
        addNode(entity.getUriId(), entity.getUri(), entity.getUserId(), entity.getConfidence(),
            Precision.round(calculateWeight(entity.getCreatedAt(), entity.getType()), 3), entity.getSessionId(),
            entity.getType(), entity.getCreatedAt());
    }

    /**
     * Calculate the sum_weight of each node with the formula of NEA.
     */
    public void calculateSumWeight() {
        //latch.await();
        results = new HashMap<>();
        for (int i = 1; i < nodes.size() - 1; i++) {
            double weight = 0;
            double sumConfidence = 0;
            for (Link link : links) {
                if (link.source == i || link.target == i) {
                    weight += link.weight;
                    sumConfidence += link.source == i ? (link.target == 0 ? 0 : nodes.get(link.target).getConfidence()) : (link.source == 0 ? 0 : nodes.get(link.source).getConfidence());
                }
            }
            results.put(i, nodes.get(i).getConfidence() * sumConfidence * weight);
        }
    }

    /**
     * Create a shared object for the single graph of the current user, which contains nodes from 3 different sources (user, group and session).
     * Usually, each source will have a maximum of 10 nodes.
     *
     * @return the shared object of a single graph
     */
    public JsonSharedObject createSingleGraph() {
        if (!dao().getUserDao().isActiveUser(userId)) {
            return null;
        }

        JsonSharedObject object = new JsonSharedObject("singleGraph", false);
        List<Node> newNodes = new ArrayList<>();

        Map<String, String> typeMap = new HashMap<>();
        //HARDCODED lines - need alternatives
        typeMap.put("user", "user");
        typeMap.put("group", "group");
        typeMap.put("snippet_not_clicked", "session");
        typeMap.put("snippet_clicked", "session");
        typeMap.put("query", "session");
        typeMap.put("web", "session");

        Map<String, Integer> occurrences = new HashMap<>();
        occurrences.put("user", 0);
        occurrences.put("group", 0);
        occurrences.put("session", 0);

        List<Map.Entry<Integer, Double>> entries = new ArrayList<>(results.entrySet());
        entries.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));
        //Find occurrences of each source as nodes - if the total of one source exceeds 10 then stop
        for (Map.Entry<Integer, Double> entry : entries) {
            Node node = nodes.get(entry.getKey());
            for (Map.Entry<String, String> type : typeMap.entrySet()) {
                if (occurrences.get(type.getValue()) < 10 && node.getType().contains(type.getKey())) {
                    if (object.getEntities().stream().noneMatch(s -> s.getType().equals(type.getValue()) && s.getUri().equals(node.getUri()))) {
                        newNodes.add(node);
                        object.getEntities().add(new JsonSharedObject.Entity(node.getUri(), node.getName(), entry.getValue(), type.getValue(), node.getId()));
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
                    object.getLinks().add(new JsonSharedObject.Link(i, j));
                }
            }
        }
        //Add this new graph (newLinks, newNodes) into a shared object, so that it can be stored later on
        return object;
    }

    /**
     * Create a shared object based on the result of pkg graph calculation.
     *
     * @param numberPositive how many positive entities the shared Object will pass
     * @param numberNegative how many negative entities the shared Object will pass
     * @return the list of shared object in Json form
     */
    public JsonSharedObject prepareCollabRec(int numberPositive, int numberNegative) {
        calculateSumWeight();

        JsonSharedObject sharedObject = new JsonSharedObject("CollabRec", false);
        if (results == null || results.isEmpty()) {
            log.info("No weight calculated");
            return null;
        }

        List<Map.Entry<Integer, Double>> choosenEntries = new ArrayList<>();
        if (results.size() < numberPositive + numberNegative) {
            choosenEntries.addAll(results.entrySet());
        } else {
            Comparator<Map.Entry<Integer, Double>> cmp = Map.Entry.comparingByValue();
            List<Map.Entry<Integer, Double>> entries = new ArrayList<>(results.entrySet());
            entries.sort(cmp.reversed());

            int limit = 0;
            for (Map.Entry<Integer, Double> entry : entries) {
                choosenEntries.add(entry);
                if (++limit >= numberPositive) {
                    break;
                }
            }

            entries.sort(cmp);
            limit = 0;
            for (Map.Entry<Integer, Double> entry : entries) {
                choosenEntries.add(entry);
                if (++limit >= numberNegative) {
                    break;
                }
            }
        }

        for (Map.Entry<Integer, Double> entry : choosenEntries) {
            Node chosenNode = nodes.get(entry.getKey());
            sharedObject.getEntities().add(new JsonSharedObject.Entity(chosenNode.getUri(), chosenNode.getName(), entry.getValue(), chosenNode.getType(), chosenNode.getId()));
        }

        return sharedObject;
    }

    /**
     * Create a shared object based on the result of pkg graph calculation.
     *
     * @param user the current user
     * @param groupId The group id
     * @param numberEntities how many entities per user the shared Object will show
     * @param isAscending show if the sharedObject will get the result from top or bottom
     * @param application the application of this shared object (can be "recommendation", "collabGraph" or "negative" + "positive"
     * @return the list of shared object in Json form
     */
    public JsonSharedObject createSharedObject(User user, int groupId, int numberEntities, boolean isAscending, String application) {
        //Initialization
        //The list to be returned
        JsonSharedObject sharedObject = new JsonSharedObject(application, false);
        List<Node> newNodes;
        if (results == null) {
            return null;
        }

        //Sort the calculated results to get entities' ranking
        List<Map.Entry<Integer, Double>> entries = new ArrayList<>(results.entrySet());
        Comparator<Map.Entry<Integer, Double>> cmp = Map.Entry.comparingByValue();
        if (isAscending) {
            entries.sort(cmp);
        } else {
            entries.sort(cmp.reversed());
        }

        int index = 0;
        //Choose only the active users to create the shared object
        if (!dao().getUserDao().isActiveUser(user.getId())) {
            return null;
        } else {
            newNodes = new ArrayList<>();
            for (Map.Entry<Integer, Double> entry : entries) {
                //Find from the top of the results numberTopEntities entities, break after reaching the number
                Node chosenNode = nodes.get(entry.getKey());
                newNodes.add(chosenNode);
                sharedObject.getEntities().add(new JsonSharedObject.Entity(chosenNode.getUri(), chosenNode.getName(), entry.getValue(), chosenNode.getType(), chosenNode.getId()));
                index++;
                if (index >= numberEntities) {
                    break;
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
                        sharedObject.getLinks().add(new JsonSharedObject.Link(i, j));
                    }
                }
            }

            sharedObject.setUser(new JsonSharedObject.User(user.getId(), user.getUsername()));
        }

        Gson gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new SearchHistoryBean.LocalDateTimeAdapter().nullSafe()).create();

        //Export shared object to DB
        int sharedObjectId;
        List<JsonSharedObject> obj = dao().getCollabGraphDao().findObjectsByUserId(groupId, sharedObject.getUser().getId(), application);
        if (obj.isEmpty()) {
            sharedObjectId = dao().getCollabGraphDao().insertSharedObject(sharedObject.getUser().getId(), groupId, application, gson.toJson(sharedObject));
        } else {
            dao().getCollabGraphDao().updateSharedObject(gson.toJson(sharedObject), LocalDateTime.now(), sharedObject.getUser().getId(), groupId, application);
            sharedObjectId = obj.get(0).getId();
        }

        //--------------------RDF---------------------------
        rdfGraph.addStatement("SharedObject/" + sharedObjectId, "schema:dateCreated", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE), "literal");
        rdfGraph.addStatement("SharedObject/" + sharedObjectId, "schema:application", sharedObject.getApplication(), "literal");
        rdfGraph.addStatement("SharedObject/" + sharedObjectId, "schema:text", gson.toJson(sharedObject), "literal");
        for (JsonSharedObject.Entity entity : sharedObject.getEntities()) {
            rdfGraph.addStatement("SharedObject/" + sharedObjectId, "dependsOn", "RecognizedEntities/" + entity.getId(), "resource");

            Optional<RecognisedEntity> annotationObj = recognisedEntities.stream().filter(s -> s.getUriId() == entity.getId()).findFirst();
            if (annotationObj.isPresent()) {
                for (String inputId : annotationObj.get().getInputStreams().split(",")) {
                    rdfGraph.addStatement("SharedObject/" + sharedObjectId, "dependsOn", "InputStream/" + inputId, "resource");
                }
            }
        }

        //--------------------End RDF ----------------------

        //Add the entities after calculation to Rdf List
        Group group = dao().getGroupDao().findByIdOrElseThrow(groupId);
        for (Node node : nodes) {
            rdfGraph.addEntity(PATTERN.matcher(node.getUri()).replaceAll(""), node.getUri(), node.getName(), node.getWeight(), node.getConfidence(), node.getDate());
        }

        //Print the Rdf graphs both to DB and local directories as files
        String value = rdfGraph.printModel();
        if (dao().getCollabGraphDao().findRdfById(user.getId()).isEmpty()) {
            dao().getCollabGraphDao().insertRdf(user.getId(), group.getId(), value);
        } else {
            dao().getCollabGraphDao().updateRdf(value, user.getId());
        }
        return sharedObject;
    }

    public RdfModel getRdfGraph() {
        return rdfGraph;
    }

    /**
     * The Node class. Has all values of an entity
     */

    public static class Node {
        private transient int id;
        private String uri;
        private String name;
        private int frequency;
        private int userId;
        private transient String sessionId;
        private transient double confidence;
        private transient double weight;
        private transient LocalDateTime date;
        private String type;

        //Node class. Receives the input from DB to be visualized
        public Node(int id, String name, String uri, int userId, double weight, double confidence, String sessionId, String type, LocalDateTime date) {
            this.id = id;
            this.sessionId = sessionId;
            this.name = name;
            this.uri = uri;
            this.userId = userId;
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

        public int getUserId() {
            return userId;
        }

        public void setUserId(final int userId) {
            this.userId = userId;
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
     */
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
}
