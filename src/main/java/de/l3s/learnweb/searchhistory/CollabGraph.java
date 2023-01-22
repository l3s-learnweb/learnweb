package de.l3s.learnweb.searchhistory;

import static com.google.common.math.Quantiles.median;
import static com.google.common.math.Quantiles.percentiles;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Create the visualisation graph of the top results for the current user, as well as the group it belongs to.
* @author Trung Tran
* */
public class CollabGraph implements Serializable {
    @Serial
    private static final long serialVersionUID = 1100213292212314798L;

    private transient List<Double> weightValues;

    /**
    * The Node class. Has the variables to be visualized
    * */
    public static class Node implements Serializable {
        @Serial
        private static final long serialVersionUID = 5501000537036189064L;
        private String uri;
        private String name;
        private int frequency;
        private transient List<String> users;
        private String user;
        transient Node parent;
        private final transient double weight;

        public Node(String name, String uri, String users, double weight) {
            this.name = name;
            this.uri = uri;
            this.users = Arrays.stream(users.split(",")).toList();
            this.frequency = 1;
            this.weight = weight;
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

        public List<String> getUsers() {
            return users;
        }

        public void setUsers(final List<String> users) {
            this.users = users;
        }

        public String getUser() {
            return user;
        }

        public void setUser() {
            this.user = String.join(",", users);
        }

        public String getUri() {
            return uri;
        }

        public void setUri(final String uri) {
            this.uri = uri;
        }

        public Node getParent() {
            return parent;
        }

        public void setParent(final Node parent) {
            this.parent = parent;
        }

        public double getWeight() {
            return weight;
        }
    }

    /**
    * The link class. Represents the link between entities.
    * */
    public static class Link implements Serializable {
        @Serial
        private static final long serialVersionUID = 843826821498615667L;
        private int source;
        private int target;

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

        public Link(int source, int target) {
            this.source = source;
            this.target = target;
        }
    }

    public static class Record implements Serializable {
        @Serial
        private static final long serialVersionUID = -474111258968809133L;
        public List<Node> nodes;
        public List<Link> links;

        public Record(final List<Node> nodes, final List<Link> links) {
            this.nodes = nodes;
            this.links = links;
        }
    }

    private int calculateFrequencyRatio(double weight) {
        double median = median().compute(weightValues);
        double q1 = percentiles().index(75).compute(weightValues);
        double q3 = percentiles().index(25).compute(weightValues);
        if (weight < q1) {
            return 1;
        } else if (weight >= q1 && weight < median) {
            return 2;
        } else if (weight >= median && weight < q3) {
            return 3;
        } else if (weight >= q3) {
            return 4;
        }
        throw new IllegalStateException("Unexpected value: " + true);
    }

    /**
     * @param nodes the nodes in the combined shared objects
     * @return the CollabGraph after merging nodes
     * */
    private CollabGraph removeDuplicatingNodesAndLinks(List<Node> nodes) {
        //Remove duplicating nodes by merging nodes with the same uri
        for (int i = 0; i < nodes.size(); i++) {
            if (!nodes.get(i).getUri().isEmpty()) {
                for (int j = i + 1; j < nodes.size(); j++) {
                    if (nodes.get(i).getUri().equals(nodes.get(j).getUri())) {
                        //Join the users and sessionId of the first node
                        List<String> userList = new ArrayList<>(nodes.get(i).getUsers());
                        userList.removeAll(nodes.get(j).getUsers());
                        userList.addAll(nodes.get(j).getUsers());
                        nodes.get(i).setUsers(userList);
                        //Remove the duplicating node
                        nodes.remove(j);
                        j--;
                    }
                }
                nodes.get(i).setUser();
            }
        }
        //Create the link after finaliziing the nodes
        List<Link> links = new ArrayList<>();
        for (int i = 0; i < nodes.size() - 1; i++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                List<String> commonUser = nodes.get(i).getUsers().stream().filter(
                    nodes.get(j).getUsers().stream().toList()::contains).toList();
                if (!commonUser.isEmpty()) {
                    if (nodes.get(j).getParent() == null || nodes.get(i).getWeight() == nodes.get(i).getParent().getWeight()) {
                        links.add(new Link(i, j));
                        nodes.get(j).setParent(nodes.get(i));
                    }
                }
            }
        }
        return new CollabGraph(nodes, links);
    }

    /**
    * Create the collaborative Graph (collabGraph) file + visualisation based on the shared objects as inputs.
    * @param    sharedObjects   the list of Shared Objects in Json form
    * @return   the collabGraph object to be visualized by annotation.js
    * */
    public CollabGraph createCollabGraph(List<JsonSharedObject> sharedObjects) {
        Record calculatedRecord = new Record(new ArrayList<>(), new ArrayList<>());
        if (sharedObjects.isEmpty()) {
            return null;
        }
        for (JsonSharedObject sharedObject : sharedObjects) {
            //Add all new entities
            for (JsonSharedObject.Link link : sharedObject.getLinks()) {
                calculatedRecord.links.add(new Link(link.getSource() + calculatedRecord.nodes.size(),
                    link.getTarget() + calculatedRecord.nodes.size()));
            }
            for (JsonSharedObject.Entity node : sharedObject.getEntities()) {
                calculatedRecord.nodes.add(new Node(node.getQuery(), node.getUri(), sharedObject.getUser().getName(), node.getWeight()));
            }
            //Add links, modify it to be logical with current nodes of collabGraph
        }
        CollabGraph graph = removeDuplicatingNodesAndLinks(calculatedRecord.nodes);
        for (Node node : graph.record.nodes) {
            node.setFrequency(node.getUsers().size());
        }
        return graph;
    }

    /**
     * Create the user's single graph, which shows the top entities in 3 sources: profile, group, session (query, web and snippets).
     * @param sharedObject the sharedObject json file gotten from Pkg
     * @return the collabGraph object to be visualized by annotation.js
     * */
    public CollabGraph createSingleGraph(JsonSharedObject sharedObject) {
        Record calculatedRecord = new Record(new ArrayList<>(), new ArrayList<>());
        weightValues = new ArrayList<>();
        for (JsonSharedObject.Entity node : sharedObject.getEntities()) {
            calculatedRecord.nodes.add(new Node(node.getQuery(), node.getUri(), node.getType(), node.getWeight()));
            weightValues.add(node.getWeight());
        }
        Collections.sort(weightValues);
        for (Node node : calculatedRecord.nodes) {
            node.setFrequency(calculateFrequencyRatio(node.getWeight()));
        }
        for (JsonSharedObject.Link link : sharedObject.getLinks()) {
            calculatedRecord.links.add(new Link(link.getSource(), link.getTarget()));
        }
        return removeDuplicatingNodesAndLinks(calculatedRecord.nodes);
    }

    public Record record;

    CollabGraph(final List<Node> nodes, final List<Link> links) {
        this.record = new Record(nodes, links);
    }
}
