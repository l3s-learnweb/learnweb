package de.l3s.learnweb.searchhistory;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.user.User;

public class RdfModel {

    private static final Pattern COMPILE = Pattern.compile(" ");
    private static final Pattern PATTERN = Pattern.compile("\\<[^>]*>");
    private final Model model;
    private Model ontologyModel;
    private static final String prefixBase = "https://github.com/tibonto/PKGonto/";
    private static final String prefixSchema = "https://schema.org/";
    private final Group group;

    private void readOntologyModel() {
        ontologyModel = ModelFactory.createDefaultModel();
        ontologyModel.read("/ontology/pkgOnto.ttl","TTL");
    }

    public RdfModel(final User user, final Group group, final List<SearchSession> sessions) throws IOException{
        model = ModelFactory.createDefaultModel();
        model.setNsPrefix("schema", prefixSchema);
        this.group = group;
        readOntologyModel();

        model.createResource("SharedObject/Negativity_Exponential_Algorithm");
        addStatement("Group/" + group.getTitle(), "description", PATTERN.matcher(group.getDescription()).replaceAll(""), "literal");
        addStatement("Group/" + group.getTitle(), "name", group.getTitle(), "literal");
        addStatement("Group/" + group.getTitle(), "dateCreated", group.getCreatedAt().format(DateTimeFormatter.ISO_DATE), "literal");

        addStatement("User/" + user.getUsername(), "email", user.getEmail(), "literal");
        addStatement("User/" + user.getUsername(), "hasProfile", "UserProfile/" + user.getUsername(), "resource");
        addStatement("UserProfile/" + user.getUsername(), "username", user.getUsername(), "literal");
        addStatement("User/" + user.getUsername(), "memberOf", "Group/" + group.getTitle(), "resource");
        addStatement("User/" + user.getUsername(), "name", user.getUsername(), "literal");

        for (SearchSession session : sessions) {
            if (session.getUser().equals(user)) {
                addStatement("User/" + user.getUsername(), "generatesLogs", "SearchSession/" + session.getSessionId(), "resource");
                addStatement("SearchSession/" + session.getSessionId(), "startTime", session.getStartTimestamp().format(DateTimeFormatter.ISO_DATE), "literal");
                addStatement("SearchSession/" + session.getSessionId(), "endTime", session.getEndTimestamp().format(DateTimeFormatter.ISO_DATE), "literal");
                for (SearchQuery searchQuery : session.getQueries()) {
                    addStatement("SearchSession/" + session.getSessionId(), "contains", "SearchQuery/" + searchQuery.query(), "resource");
                    addStatement("SearchQuery/" + searchQuery.query(), "dateCreated", searchQuery.timestamp().format(DateTimeFormatter.ISO_DATE), "literal");
                }
            }
        }
    }

    public void addStatement(String sbj, String pre, String obj, String type) {
        Resource subject = model.createResource(prefixBase + COMPILE.matcher(sbj).replaceAll("_"));

        switch (type) {
            case "literal" -> {
                if (obj == null) obj = "";
                subject.addProperty(model.createProperty("schema:" + pre), obj);
            }
            case "resource" -> {
                Property predicate = model.createProperty("schema:" + pre);
                RDFNode object = model.createResource(prefixBase + COMPILE.matcher(obj).replaceAll("_"));
                Statement stmt = model.createStatement(subject, predicate, object);
                model.add(stmt);
            }
            default -> {
            }
        }
    }

    public void addEntity(int id, String uri, String surfaceForm, double weight, double score, LocalDateTime time) {
        if (Objects.equals(uri, "default")) return;
        addStatement("RecognizedEntities/" + id, "identifier", uri, "literal");
        addStatement("RecognizedEntities/" + id, "surfaceForm", surfaceForm, "literal");
        addStatement("RecognizedEntities/" + id, "weight", String.valueOf(weight), "literal");
        addStatement("RecognizedEntities/" + id, "confidenceScore", String.valueOf(score), "literal");
        addStatement("RecognizedEntities/" + id, "dateCreated", time.format(DateTimeFormatter.ISO_DATE), "literal");
    }

    public String printModel() {
        // list the statements in the Model
        StringWriter out = new StringWriter();
        model.write(out, "TTL", prefixBase);
        //model.write(writer, "TTL", prefixBase);
        return out.toString();
    }

    public Model getModel() {
        return model;
    }
}