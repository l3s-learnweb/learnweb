package de.l3s.learnweb.searchhistory;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

//Separate class for serializing the search history
public class JsonQuery implements Serializable {

    public class Node {
        private int id;
        private String query;
        private List<String> sessionId = new ArrayList();
        private int group;
        private int frequency;
        private String users;

        public Node(int id, String sessionId, String query, String users, int frequency) {
            this.id = id;
            this.sessionId.add(sessionId);
            this.query = query;
            this.group = group;
            this.users = users;
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
            this.users += this.users.contains(user) ? "" : new StringBuilder().append(", ").append(user).toString();
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

    private void setLink(int source, int target) {
        record.links.add(new Link(source, target));
    }

    public void processQuery(List<SearchSession> searchSession) {
        boolean existQuery = false;
        //Create nodes
        for (SearchSession session : searchSession)
            for (SearchQuery searchQuery : session.getQueries()) {
                existQuery = false;
                String searchKeyword = searchQuery.query().toLowerCase(Locale.ROOT);
                for (Node node : record.nodes)
                    if (node.containQuery(searchKeyword)) {
                        if (!node.getSessionId().contains(session.getSessionId())) {
                            node.upgradeNode(session.getSessionId(), session.getUser().getUsername());
                        }
                        node.increaseFrequency();
                        existQuery = true;
                        break;
                    }
                if (!existQuery) {
                    Node node = new Node(searchQuery.searchId(), session.getSessionId(), searchQuery.query(),
                        session.getUser().getUsername(), 1);
                    record.nodes.add(node);
                }
        }
        //Create links
        for (int i = 0; i < record.nodes.size() - 1; i++) {
            for (int j = i + 1; j < record.nodes.size(); j++) {
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
