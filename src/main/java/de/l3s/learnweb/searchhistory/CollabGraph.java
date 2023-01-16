package de.l3s.learnweb.searchhistory;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

/*
* @author Trung Tran
* */
public class CollabGraph implements Serializable {

    @Serial
    private static final long serialVersionUID = 1100213292212314798L;
    private static final Pattern PATTERN = Pattern.compile("http://dbpedia.org/resource/");
    private transient HashMap<Integer, Double> results;

    /*
    * The Node class. Has all values of an entity
    * */
    public class Node {
        private String uri;
        private String name;
        private int frequency;
        private String users;

        //Node class. Receives the input from DB to be visualized
        public Node(String name, String uri, String users) {
            this.name = name;
            this.uri = uri;
            this.users = users;
            this.frequency = 1;
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
    }

    /*
    * The link class. Represents the weighted link between two entities
    * */
    public class Link {
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

    private void removeDuplicatingNodesAndLinks(List<Node> nodes, List<Link> links) {
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
                        //Remove the duplicating node
                        nodes.remove(j);
                        j--;
                        nodes.get(i).increaseFrequency();
                    }
                }
            }
        }
    }

    /**
    * Create the collaborative Graph (collabGraph) file + visualisation based on the shared objects as inputs
    * @param    sharedObjects   the list of Shared Objects in Json form
    * @return   the collabGraph object to be visualized by annotation.js
    * */
    public CollabGraph createCollabGraph(List<JsonSharedObject> sharedObjects) {
        Record calculatedRecord = new Record(new ArrayList<>(), new ArrayList<>());
        if (sharedObjects.isEmpty()) return null;
        for (JsonSharedObject sharedObject : sharedObjects) {
            //Add all new entities
            for (JsonSharedObject.Link link : sharedObject.getLinks()) {
                calculatedRecord.links.add(new Link(link.getSource() + calculatedRecord.nodes.size(),
                    link.getTarget() + calculatedRecord.nodes.size()));
            }
            for (JsonSharedObject.Entity node : sharedObject.getEntities()) {
                calculatedRecord.nodes.add(new Node(node.getQuery(), node.getUri(), sharedObject.getUser().getName()));
            }
            //Add links, modify it to be logical with current nodes of collabGraph
        }

        removeDuplicatingNodesAndLinks(calculatedRecord.nodes, calculatedRecord.links);

        return new CollabGraph(calculatedRecord.nodes, calculatedRecord.links);
    }

    /**
     * Create the user's single graph, which shows the top entities in 3 sources: profile, group, session (query, web and snippets)
     * @param sharedObject the sharedObject json file gotten from Pkg
     * @return the collabGraph object to be visualized by annotation.js
     * */
    public CollabGraph createSingleGraph(JsonSharedObject sharedObject) {
        Record calculatedRecord = new Record(new ArrayList<>(), new ArrayList<>());
        for (JsonSharedObject.Entity node : sharedObject.getEntities()) {
            calculatedRecord.nodes.add(new Node(node.getQuery(), node.getUri(), node.getType()));
        }
        for (JsonSharedObject.Link link : sharedObject.getLinks()) {
            calculatedRecord.links.add(new Link(link.getSource(), link.getTarget()));
        }
        return new CollabGraph(calculatedRecord.nodes, calculatedRecord.links);
    }

    public Record record;

    CollabGraph(final List<Node> nodes, final List<Link> links) {
        this.record = new Record(nodes, links);
    }
}